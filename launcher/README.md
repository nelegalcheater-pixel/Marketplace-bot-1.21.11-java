<!-- coderabbit-ai-generated:v1 -->

# KillerKC2 Launcher для Windows

EXE запускает локальную страницу, устанавливает вложенные KillerKC2 0.7.0 и Fabric API 0.140.2+1.21.11 в `%APPDATA%\.minecraft\mods` и открывает установленный Minecraft Launcher.

## Использование

1. Установите Minecraft Launcher, Java 21 и профиль Fabric Loader для Minecraft 1.21.11.
2. Запустите EXE, оставьте консоль открытой и нажмите «Установить мод» на открывшейся странице.
3. Откройте Minecraft Launcher кнопкой, войдите в Microsoft-аккаунт там и выберите Fabric 1.21.11. Если лаунчер не найден, укажите полный путь к его EXE.

Игра должна использовать стандартную папку `%APPDATA%\.minecraft`. Для другой игровой директории установите JAR вручную. Старые копии KillerKC2 заменяются по ID мода. Совместимый Fabric API и другие моды сохраняются; другая версия API или дубликаты требуют ручного устранения конфликта.

EXE не содержит Minecraft, не устанавливает Fabric Loader или Java и не реализует самостоятельный вход Microsoft. Microsoft Store может потребовать указать доступный путь к исполняемому файлу. Это установщик, использующий Minecraft Launcher. Собран для Windows x64; запуск на Windows в этой Linux-среде не проверен.

## Сборка

Нужны Java 21, Go 1.25.3 и доступ к Maven/Fabric репозиториям. Из корня репозитория:

```sh
bash launcher/build.sh
```

Скрипт собирает мод и тесты, копирует JAR и Fabric API в assets, запускает Go-тесты и компилирует Windows EXE. GitHub Actions `Build KillerKC2 client` публикует `killerkc2-client` и `killerkc2-windows-launcher`.

После подготовки assets для просмотра интерфейса выполните из `launcher`: `go run . --preview --port 8088`. Установка и запуск в этом режиме отключены. Страница слушает loopback; изменяющие запросы требуют случайный токен процесса. Рабочий установщик не следует публиковать как сетевой сервис.
