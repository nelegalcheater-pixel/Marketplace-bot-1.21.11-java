<!-- coderabbit-ai-generated:v1 -->

<!-- coderabbit-ai-generated:v1 -->

<!-- coderabbit-ai-generated-section:v1:4354ca225a98449b5badd88683a6fe77ed78970e05b3e5d4f586caf46df358fc -->

# KillerKC2 client

Клиентский мод для Minecraft Java 1.21.11 и Fabric. Проект находится в разработке; это ещё не полная копия ReallyVisuals.

## Установка

Установите Fabric Loader и Fabric API для Minecraft 1.21.11. Скачайте артефакт `killerkc2-client` из успешного запуска GitHub Actions и положите JAR в папку `mods`.

<!-- coderabbit-ai-generated:v1 -->

<!-- coderabbit-ai-generated-section:v1:2b9f676a9dc2a21bf7dc15f1808c60ba67f473411b01ce98974dca8f10c18240 -->

Нажмите Right Shift в игре, чтобы открыть меню. Better Near показывает загруженных игроков в пределах 128 блоков; Armor HUD показывает надетую броню и её оставшуюся прочность; XYZ / FPS показывает координаты и FPS. Настройки сохраняются в `config/rvsteal.properties`.

<!-- coderabbit-ai-generated:v1 -->

<!-- coderabbit-ai-generated-section:v1:a09f44908598a6fcfa1178034159f296643bb4cbdd74c0e7d9bbdb4d2f6dcd81 -->

Меню открывается правым Shift и закрывается Escape. Вкладки Combat, PvE, Visual и Utility включают Player Aura и Mob Aura, атаку под прицелом, Auto Tool, автовозрождение, подсветку игроков и монстров, максимальную яркость, HUD, автоспринт и Bunny Hop. Настройки по умолчанию выключены. Аура атакует ближайшую доступную цель после восстановления удара, в пределах обычной дистанции взаимодействия и при прямой видимости; подсветка работает только для загруженных сущностей. Auto Tool переключает инструмент в хотбаре и оставляет его выбранным.

<!-- /coderabbit-ai-generated:v1 -->

Текст HUD, броня и список игроков обновляются раз в 5 тиков, чтобы не форматировать текст и не обходить игроков на каждом кадре. Это оптимизация нагрузки самого мода; замеры FPS и сравнение с другими модами оптимизации ещё не выполнены. Замена Sodium и других оптимизаторов не реализована.

<!-- /coderabbit-ai-generated:v1 -->

## Сборка

Нужна Java 21. В этой папке выполните `./gradlew build`. Готовый JAR находится в `build/libs/`. Для сборки через GitHub откройте Actions → Build KillerKC2 client → Run workflow.


<!-- /coderabbit-ai-generated:v1 -->