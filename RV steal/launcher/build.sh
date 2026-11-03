#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
(cd .. && bash ./gradlew build launcherAssets --no-daemon)
go test ./...
GOOS=windows GOARCH=amd64 CGO_ENABLED=0 go build -trimpath -ldflags='-s -w' -o KillerKC2-Launcher.exe .
