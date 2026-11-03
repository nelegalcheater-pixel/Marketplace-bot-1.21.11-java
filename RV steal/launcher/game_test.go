package main

import (
	"os"
	"strings"
	"testing"
)

func TestOfflineUUIDAndRules(t *testing.T) {
	if got := offlineUUID("Notch"); got != "b50ad385829d3141a2167e7d7539ba7f" {
		t.Fatal(got)
	}
	p, e := mavenPath("org.ow2.asm:asm:9.9")
	if e != nil || p != "org/ow2/asm/asm/9.9/asm-9.9.jar" {
		t.Fatal(p, e)
	}
	if allowed([]rule{{Action: "allow", Features: map[string]bool{"is_demo_user": true}}}) {
		t.Fatal("unexpected demo")
	}
}
func TestLivePrepare(t *testing.T) {
	if os.Getenv("KC_LIVE_TEST") != "1" {
		t.Skip("live download disabled")
	}
	dir := os.Getenv("KC_TEST_DIR")
	cmd, err := prepare(dir, "TestPlayer", os.Getenv("KC_JAVA"))
	if err != nil {
		t.Fatal(err)
	}
	if !strings.Contains(strings.Join(cmd.Args, " "), "net.fabricmc.loader.impl.launch.knot.KnotClient") {
		t.Fatal("missing fabric")
	}
	if err = cmd.Start(); err != nil {
		t.Fatal(err)
	}
	t.Logf("started pid %d", cmd.Process.Pid)
	if err = cmd.Wait(); err != nil {
		t.Fatal(err)
	}
}
