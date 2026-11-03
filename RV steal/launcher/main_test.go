package main

import (
	"archive/zip"
	"bytes"
	"fmt"
	"mime/multipart"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"testing"
)

func TestInstallAndProtection(t *testing.T) {
	root := t.TempDir()
	if err := install(root); err != nil {
		t.Fatal(err)
	}
	for _, n := range []string{"killerkc2-client.jar", "fabric-api-killerkc2.jar"} {
		d, e := os.ReadFile(filepath.Join(root, "mods", n))
		if e != nil || len(d) == 0 {
			t.Fatal(n, e)
		}
	}
	h := handler("test", root, false)
	req := httptest.NewRequest("POST", "/api/install", nil)
	w := httptest.NewRecorder()
	h.ServeHTTP(w, req)
	if w.Code != http.StatusForbidden {
		t.Fatal(w.Code)
	}
	req = httptest.NewRequest("POST", "/api/install", nil)
	req.Header.Set("X-KillerKC2-Token", "test")
	w = httptest.NewRecorder()
	h.ServeHTTP(w, req)
	if w.Code != 200 {
		t.Fatal(w.Code)
	}
	h = handler("test", root, true)
	w = httptest.NewRecorder()
	h.ServeHTTP(w, req)
	if w.Code != 409 {
		t.Fatal(w.Code)
	}
}

func fixtureMod(t *testing.T, dir, name, id, version string) {
	t.Helper()
	file, err := os.Create(filepath.Join(dir, name))
	if err != nil {
		t.Fatal(err)
	}
	z := zip.NewWriter(file)
	w, err := z.Create("fabric.mod.json")
	if err != nil {
		t.Fatal(err)
	}
	fmt.Fprintf(w, `{"id":%q,"version":%q}`, id, version)
	if err := z.Close(); err != nil {
		t.Fatal(err)
	}
	if err := file.Close(); err != nil {
		t.Fatal(err)
	}
}
func TestUpgradePreservesOtherMods(t *testing.T) {
	root := t.TempDir()
	dir := filepath.Join(root, "mods")
	os.MkdirAll(dir, 0755)
	fixtureMod(t, dir, "old-client.jar", "rvsteal", "0.6.0")
	fixtureMod(t, dir, "api.jar", "fabric-api", "0.140.2+1.21.11")
	fixtureMod(t, dir, "other.jar", "other-mod", "1.0")
	api, _ := os.ReadFile(filepath.Join(dir, "api.jar"))
	other, _ := os.ReadFile(filepath.Join(dir, "other.jar"))
	if err := install(root); err != nil {
		t.Fatal(err)
	}
	if _, err := os.Stat(filepath.Join(dir, "old-client.jar")); !os.IsNotExist(err) {
		t.Fatal("old client remains")
	}
	if _, err := os.Stat(filepath.Join(dir, "fabric-api-killerkc2.jar")); !os.IsNotExist(err) {
		t.Fatal("duplicate API installed")
	}
	for name, want := range map[string][]byte{"api.jar": api, "other.jar": other} {
		got, _ := os.ReadFile(filepath.Join(dir, name))
		if string(got) != string(want) {
			t.Fatal("modified", name)
		}
	}
}
func TestRejectsIncompatibleAPIAndNameCollision(t *testing.T) {
	for _, test := range []struct{ name, id, version string }{{"api.jar", "fabric-api", "wrong"}, {"killerkc2-client.jar", "unrelated", "1.0"}} {
		root := t.TempDir()
		dir := filepath.Join(root, "mods")
		os.MkdirAll(dir, 0755)
		fixtureMod(t, dir, test.name, test.id, test.version)
		before, _ := os.ReadFile(filepath.Join(dir, test.name))
		if err := install(root); err == nil {
			t.Fatal("expected conflict")
		}
		after, _ := os.ReadFile(filepath.Join(dir, test.name))
		if string(before) != string(after) {
			t.Fatal("conflict was overwritten")
		}
	}
}

func TestUploadMod(t *testing.T) {
	root := t.TempDir()
	fixtures := t.TempDir()
	fixtureMod(t, fixtures, "sample.jar", "sample", "1")
	data, err := os.ReadFile(filepath.Join(fixtures, "sample.jar"))
	if err != nil {
		t.Fatal(err)
	}
	request := func(name, token string, data []byte) *httptest.ResponseRecorder {
		var body bytes.Buffer
		m := multipart.NewWriter(&body)
		f, err := m.CreateFormFile("mod", name)
		if err != nil {
			t.Fatal(err)
		}
		f.Write(data)
		m.Close()
		req := httptest.NewRequest("POST", "/api/upload", &body)
		req.Header.Set("Content-Type", m.FormDataContentType())
		req.Header.Set("X-KillerKC2-Token", token)
		w := httptest.NewRecorder()
		handler("token", root, false).ServeHTTP(w, req)
		return w
	}
	if w := request("sample.jar", "bad", data); w.Code != 403 {
		t.Fatal(w.Code)
	}
	if w := request("sample.jar", "token", data); w.Code != 200 {
		t.Fatal(w.Code, w.Body.String())
	}
	if w := request("sample.jar", "token", data); w.Code != 409 {
		t.Fatal(w.Code)
	}
	if w := request("broken.jar", "token", []byte("bad")); w.Code != 400 {
		t.Fatal(w.Code)
	}
	got, err := os.ReadFile(filepath.Join(root, "mods", "sample.jar"))
	if err != nil || !bytes.Equal(got, data) {
		t.Fatal(err)
	}
}
