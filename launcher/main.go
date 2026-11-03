package main

import (
	"archive/zip"
	"crypto/rand"
	"embed"
	"encoding/hex"
	"encoding/json"
	"flag"
	"fmt"
	"log"
	"net"
	"net/http"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"strings"
	"time"
)

//go:embed assets/*
var assets embed.FS

type request struct {
	Launcher string `json:"launcher"`
}

func install(root string) error {
	dir := filepath.Join(root, "mods")
	if err := os.MkdirAll(dir, 0755); err != nil {
		return err
	}
	// Detect existing copies by mod ID rather than overwriting unrelated files.
	skipAPI := false
	apiCount := 0
	oldClient := []string{}
	entries, err := os.ReadDir(dir)
	if err != nil {
		return err
	}
	for _, entry := range entries {
		if entry.IsDir() || !strings.HasSuffix(strings.ToLower(entry.Name()), ".jar") {
			continue
		}
		path := filepath.Join(dir, entry.Name())
		ownName := strings.EqualFold(entry.Name(), "killerkc2-client.jar") || strings.EqualFold(entry.Name(), "fabric-api-killerkc2.jar")
		recognized := false
		z, err := zip.OpenReader(path)
		if err != nil {
			if ownName {
				return fmt.Errorf("файл %s уже существует и не является нашим модом", entry.Name())
			}
			continue
		}
		for _, file := range z.File {
			if file.Name != "fabric.mod.json" || file.UncompressedSize64 > 65536 {
				continue
			}
			reader, err := file.Open()
			if err != nil {
				continue
			}
			var meta struct {
				ID      string `json:"id"`
				Version string `json:"version"`
			}
			err = json.NewDecoder(reader).Decode(&meta)
			reader.Close()
			if err != nil {
				continue
			}
			if meta.ID == "fabric-api" {
				apiCount++
				if apiCount > 1 {
					z.Close()
					return fmt.Errorf("найдено несколько Fabric API: уберите дубликаты перед установкой")
				}
				if strings.EqualFold(entry.Name(), "fabric-api-killerkc2.jar") {
					recognized = true
				}
				if meta.Version != "0.140.2+1.21.11" {
					z.Close()
					return fmt.Errorf("найдена другая версия Fabric API (%s). Уберите её перед установкой", meta.Version)
				}
				skipAPI = true
			}
			if meta.ID == "rvsteal" {
				if strings.EqualFold(entry.Name(), "killerkc2-client.jar") {
					recognized = true
				} else {
					oldClient = append(oldClient, path)
				}
			}
		}
		z.Close()
		if ownName && !recognized {
			return fmt.Errorf("имя %s занято другим модом", entry.Name())
		}
	}
	// Only replace this launcher's own files; preserve the user's other mods.
	for _, name := range []string{"killerkc2-client.jar", "fabric-api-killerkc2.jar"} {
		if name == "fabric-api-killerkc2.jar" && skipAPI {
			continue
		}
		data, err := assets.ReadFile("assets/" + name)
		if err != nil {
			return err
		}
		temp := filepath.Join(dir, name+".tmp")
		if err = os.WriteFile(temp, data, 0644); err != nil {
			return err
		}
		if err = os.Rename(temp, filepath.Join(dir, name)); err != nil {
			return err
		}
	}
	for _, path := range oldClient {
		if err := os.Remove(path); err != nil {
			return fmt.Errorf("уберите старый KillerKC2: %s", path)
		}
	}
	return nil
}
func handler(token, root string, preview bool) http.Handler {
	mux := http.NewServeMux()
	mux.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path != "/" {
			http.NotFound(w, r)
			return
		}
		w.Header().Set("Content-Type", "text/html; charset=utf-8")
		w.Header().Set("Cache-Control", "no-store")
		w.Header().Set("X-Frame-Options", "DENY")
		page, _ := assets.ReadFile("assets/index.html")
		fmt.Fprint(w, strings.ReplaceAll(string(page), "__TOKEN__", token))
	})
	mux.HandleFunc("/api/install", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		if r.Method != "POST" || r.Header.Get("X-KillerKC2-Token") != token {
			http.Error(w, "Forbidden", 403)
			return
		}
		if preview {
			http.Error(w, "Предпросмотр: установка отключена", 409)
			return
		}
		if err := install(root); err != nil {
			http.Error(w, "Ошибка установки: "+err.Error(), 500)
			return
		}
		json.NewEncoder(w).Encode(map[string]string{"message": "Мод установлен в " + filepath.Join(root, "mods") + ". Выберите Fabric 1.21.11 в Minecraft Launcher. Совместимый Fabric API сохранён, старые копии KillerKC2 заменены."})
	})
	mux.HandleFunc("/api/launch", func(w http.ResponseWriter, r *http.Request) {
		if r.Method != "POST" || r.Header.Get("X-KillerKC2-Token") != token {
			http.Error(w, "Forbidden", 403)
			return
		}
		if preview || runtime.GOOS != "windows" {
			http.Error(w, "Запуск доступен в Windows-версии", 409)
			return
		}
		var input request
		if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 4096)).Decode(&input); err != nil {
			http.Error(w, "Неверный запрос", 400)
			return
		}
		path := strings.TrimSpace(input.Launcher)
		if path == "" {
			path = filepath.Join(os.Getenv("ProgramFiles(x86)"), "Minecraft Launcher", "MinecraftLauncher.exe")
		}
		if !filepath.IsAbs(path) || !strings.EqualFold(filepath.Ext(path), ".exe") {
			http.Error(w, "Укажите полный путь к MinecraftLauncher.exe", 400)
			return
		}
		if _, err := os.Stat(path); err != nil {
			http.Error(w, "Лаунчер не найден. Укажите путь к установленному MinecraftLauncher.exe.", 400)
			return
		}
		if err := exec.Command(path).Start(); err != nil {
			http.Error(w, "Не удалось запустить Minecraft Launcher", 500)
			return
		}
		fmt.Fprint(w, "Minecraft Launcher запущен. Выберите профиль Fabric 1.21.11.")
	})
	return mux
}
func main() {
	preview := flag.Bool("preview", false, "Read-only preview")
	port := flag.Int("port", 0, "HTTP port (0 chooses a free port)")
	flag.Parse()
	key := make([]byte, 32)
	if _, err := rand.Read(key); err != nil {
		log.Fatal(err)
	}
	root := filepath.Join(os.Getenv("APPDATA"), ".minecraft")
	listener, err := net.Listen("tcp", fmt.Sprintf("127.0.0.1:%d", *port))
	if err != nil {
		log.Fatal(err)
	}
	url := "http://" + listener.Addr().String()
	fmt.Println("KillerKC2 Launcher: " + url)
	if runtime.GOOS == "windows" {
		exec.Command("rundll32", "url.dll,FileProtocolHandler", url).Start()
	}
	server := &http.Server{Handler: handler(hex.EncodeToString(key), root, *preview), ReadHeaderTimeout: 5 * time.Second, IdleTimeout: 30 * time.Second}
	log.Fatal(server.Serve(listener))
}
