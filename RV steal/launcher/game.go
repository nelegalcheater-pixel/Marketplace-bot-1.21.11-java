package main

import (
	"crypto/md5"
	"crypto/sha1"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"os"
	"os/exec"
	"path/filepath"
	"regexp"
	"runtime"
	"strconv"
	"strings"
	"sync"
	"time"
)

type artifact struct {
	Id   string `json:"id"`
	Path string `json:"path"`
	URL  string `json:"url"`
	SHA1 string `json:"sha1"`
}
type rule struct {
	Action   string                      `json:"action"`
	OS       struct{ Name, Arch string } `json:"os"`
	Features map[string]bool             `json:"features"`
}
type library struct {
	Name, URL, SHA1 string
	Downloads       struct {
		Artifact artifact `json:"artifact"`
	} `json:"downloads"`
	Rules []rule `json:"rules"`
}
type version struct {
	ID, MainClass string
	Libraries     []library
	Downloads     map[string]artifact
	AssetIndex    artifact `json:"assetIndex"`
	Arguments     struct{ Game, JVM []json.RawMessage }
}
type index struct {
	Objects map[string]struct {
		Hash string `json:"hash"`
	} `json:"objects"`
}

var httpClient = &http.Client{Timeout: 90 * time.Second}

func getJSON(url string, target any) error {
	r, e := httpClient.Get(url)
	if e != nil {
		return e
	}
	defer r.Body.Close()
	if r.StatusCode != 200 {
		return fmt.Errorf("ошибка загрузки: %s", r.Status)
	}
	return json.NewDecoder(io.LimitReader(r.Body, 10<<20)).Decode(target)
}
func fetch(a artifact, path string) error {
	if a.SHA1 == "" {
		if !strings.HasPrefix(a.URL, "https://maven.fabricmc.net/") {
			return fmt.Errorf("отсутствует контрольная сумма: %s", a.URL)
		}
		resp, err := httpClient.Get(a.URL + ".sha1")
		if err != nil {
			return err
		}
		data, err := io.ReadAll(io.LimitReader(resp.Body, 128))
		resp.Body.Close()
		if err != nil || resp.StatusCode != 200 {
			return fmt.Errorf("не удалось получить контрольную сумму: %s", a.URL)
		}
		a.SHA1 = strings.TrimSpace(string(data))
		if !regexp.MustCompile(`^[0-9a-f]{40}$`).MatchString(a.SHA1) {
			return fmt.Errorf("неверная контрольная сумма: %s", a.URL)
		}
	}
	if f, e := os.Open(path); e == nil {
		h := sha1.New()
		_, e = io.Copy(h, f)
		f.Close()
		if e == nil && hex.EncodeToString(h.Sum(nil)) == a.SHA1 {
			return nil
		}
	}
	if err := os.MkdirAll(filepath.Dir(path), 0755); err != nil {
		return err
	}
	r, e := httpClient.Get(a.URL)
	if e != nil {
		return e
	}
	defer r.Body.Close()
	if r.StatusCode != 200 {
		return fmt.Errorf("%s: %s", a.URL, r.Status)
	}
	f, e := os.CreateTemp(filepath.Dir(path), ".download-*")
	if e != nil {
		return e
	}
	tmp := f.Name()
	defer os.Remove(tmp)
	h := sha1.New()
	_, e = io.Copy(io.MultiWriter(f, h), io.LimitReader(r.Body, 1<<30))
	closeErr := f.Close()
	if e != nil {
		return e
	}
	if closeErr != nil {
		return closeErr
	}
	if hex.EncodeToString(h.Sum(nil)) != a.SHA1 {
		return fmt.Errorf("контрольная сумма не совпадает: %s", a.URL)
	}
	return os.Rename(tmp, path)
}
func allowed(rules []rule) bool {
	if len(rules) == 0 {
		return true
	}
	ok := false
	osName := runtime.GOOS
	if osName == "darwin" {
		osName = "osx"
	}
	for _, r := range rules {
		if len(r.Features) > 0 || (r.OS.Name != "" && r.OS.Name != osName) || (r.OS.Arch != "" && r.OS.Arch != "x86_64" && r.OS.Arch != "amd64") {
			continue
		}
		ok = r.Action == "allow"
	}
	return ok
}
func arguments(raw []json.RawMessage) []string {
	var out []string
	for _, b := range raw {
		var s string
		if json.Unmarshal(b, &s) == nil {
			out = append(out, s)
			continue
		}
		var v struct {
			Rules []rule
			Value json.RawMessage
		}
		if json.Unmarshal(b, &v) != nil || !allowed(v.Rules) {
			continue
		}
		if json.Unmarshal(v.Value, &s) == nil {
			out = append(out, s)
		} else {
			var arr []string
			if json.Unmarshal(v.Value, &arr) == nil {
				out = append(out, arr...)
			}
		}
	}
	return out
}
func mavenPath(name string) (string, error) {
	p := strings.Split(name, ":")
	if len(p) < 3 || len(p) > 4 {
		return "", fmt.Errorf("неверная библиотека %s", name)
	}
	file := p[1] + "-" + p[2]
	if len(p) == 4 {
		file += "-" + p[3]
	}
	return strings.ReplaceAll(p[0], ".", "/") + "/" + p[1] + "/" + p[2] + "/" + file + ".jar", nil
}
func offlineUUID(name string) string {
	b := md5.Sum([]byte("OfflinePlayer:" + name))
	b[6] = (b[6] & 15) | 0x30
	b[8] = (b[8] & 63) | 0x80
	return hex.EncodeToString(b[:])
}
func prepare(root, nickname, java string) (*exec.Cmd, error) {
	if !regexp.MustCompile(`^[A-Za-z0-9_]{3,16}$`).MatchString(nickname) {
		return nil, fmt.Errorf("ник: 3–16 латинских букв, цифр или _")
	}
	if java == "" {
		java = "java"
	}
	if _, e := exec.LookPath(java); e != nil {
		return nil, fmt.Errorf("Java 21 не найдена: укажите путь к java.exe")
	}

	versionOutput, versionErr := exec.Command(java, "-version").CombinedOutput()
	match := regexp.MustCompile(`version "([0-9]+)`).FindSubmatch(versionOutput)
	if versionErr != nil || len(match) < 2 {
		return nil, fmt.Errorf("не удалось определить версию Java")
	}
	major, err := strconv.Atoi(string(match[1]))
	if err != nil || major < 21 {
		return nil, fmt.Errorf("для Minecraft 1.21.11 нужна Java 21 или новее")
	}
	if err := install(root); err != nil {
		return nil, err
	}
	var manifest struct {
		Versions []struct{ ID, URL, SHA1 string }
	}
	if err := getJSON("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json", &manifest); err != nil {
		return nil, err
	}
	var entry artifact
	for _, v := range manifest.Versions {
		if v.ID == "1.21.11" {
			entry = artifact{URL: v.URL, SHA1: v.SHA1}
			break
		}
	}
	if entry.URL == "" {
		return nil, fmt.Errorf("версия 1.21.11 не найдена")
	}
	versionPath := filepath.Join(root, "versions", "1.21.11", "1.21.11.json")
	if err := fetch(entry, versionPath); err != nil {
		return nil, err
	}
	f, e := os.Open(versionPath)
	if e != nil {
		return nil, e
	}
	var vanilla version
	e = json.NewDecoder(f).Decode(&vanilla)
	f.Close()
	if e != nil {
		return nil, e
	}
	var fabric version
	if e = getJSON("https://meta.fabricmc.net/v2/versions/loader/1.21.11/0.18.1/profile/json", &fabric); e != nil {
		return nil, e
	}
	type job struct {
		a    artifact
		path string
	}
	var jobs []job
	var cp []string
	for _, l := range append(fabric.Libraries, vanilla.Libraries...) {
		if !allowed(l.Rules) {
			continue
		}
		a := l.Downloads.Artifact
		if a.URL == "" {
			p, err := mavenPath(l.Name)
			if err != nil {
				return nil, err
			}
			a = artifact{Path: p, URL: l.URL + p, SHA1: l.SHA1}
		}
		if a.Path == "" {
			return nil, fmt.Errorf("нет пути библиотеки %s", l.Name)
		}
		path := filepath.Join(root, "libraries", filepath.FromSlash(a.Path))
		cp = append(cp, path)
		jobs = append(jobs, job{a, path})
	}
	client := vanilla.Downloads["client"]
	clientPath := filepath.Join(root, "versions", "1.21.11", "1.21.11.jar")
	cp = append(cp, clientPath)
	jobs = append(jobs, job{client, clientPath})
	indexPath := filepath.Join(root, "assets", "indexes", vanilla.AssetIndex.Id+".json")
	if e = fetch(vanilla.AssetIndex, indexPath); e != nil {
		return nil, e
	}
	f, e = os.Open(indexPath)
	if e != nil {
		return nil, e
	}
	var assets index
	e = json.NewDecoder(f).Decode(&assets)
	f.Close()
	if e != nil {
		return nil, e
	}
	seen := map[string]bool{}
	for _, o := range assets.Objects {
		h := o.Hash
		if len(h) != 40 || seen[h] {
			continue
		}
		seen[h] = true
		jobs = append(jobs, job{artifact{URL: "https://resources.download.minecraft.net/" + h[:2] + "/" + h, SHA1: h}, filepath.Join(root, "assets", "objects", h[:2], h)})
	}
	queue := make(chan job)
	errs := make(chan error, 1)
	var wg sync.WaitGroup
	for i := 0; i < 16; i++ {
		wg.Add(1)
		go func() {
			defer wg.Done()
			for j := range queue {
				if err := fetch(j.a, j.path); err != nil {
					select {
					case errs <- err:
					default:
					}
				}
			}
		}()
	}
	for _, j := range jobs {
		queue <- j
	}
	close(queue)
	wg.Wait()
	select {
	case err := <-errs:
		return nil, err
	default:
	}
	native := filepath.Join(root, "natives")
	os.MkdirAll(native, 0755)
	repl := strings.NewReplacer("${auth_player_name}", nickname, "${version_name}", "1.21.11-fabric", "${game_directory}", root, "${assets_root}", filepath.Join(root, "assets"), "${assets_index_name}", vanilla.AssetIndex.Id, "${auth_uuid}", offlineUUID(nickname), "${auth_access_token}", "0", "${clientid}", "0", "${auth_xuid}", "0", "${version_type}", "release", "${natives_directory}", native, "${launcher_name}", "KillerKC2", "${launcher_version}", "0.8", "${classpath}", strings.Join(cp, string(os.PathListSeparator)))
	args := append([]string{"-Xmx2G"}, arguments(vanilla.Arguments.JVM)...)
	args = append(args, arguments(fabric.Arguments.JVM)...)
	args = append(args, fabric.MainClass)
	args = append(args, arguments(vanilla.Arguments.Game)...)
	for i := range args {
		args[i] = repl.Replace(args[i])
	}
	cmd := exec.Command(java, args...)
	cmd.Dir = root
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	return cmd, nil
}
