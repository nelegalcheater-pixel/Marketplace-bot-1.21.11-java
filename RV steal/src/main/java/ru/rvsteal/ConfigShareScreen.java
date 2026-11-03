package ru.rvsteal;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

final class ConfigShareScreen extends Screen {
    private final Screen parent;
    private String status = "", name = "Мой конфиг";
    private final Path directory = FabricLoader.getInstance().getConfigDir().resolve("killerkc2-profiles");
    private List<String> names = List.of();
    ConfigShareScreen(Screen parent) { super(Component.literal("Конфигурации")); this.parent = parent; }
    private Path profile() {
        if (!name.matches("[\\p{L}\\p{N} _-]{1,32}")) throw new IllegalArgumentException("Имя: буквы, цифры, пробел, _ и -");
        return directory.resolve(name + ".kc2");
    }
    private void action(Runnable work) { try { work.run(); } catch (Exception e) { status = "Ошибка: " + Objects.toString(e.getMessage(), "операция не выполнена"); } }
    private void refresh() {
        try {
            Files.createDirectories(directory);
            try (var paths = Files.list(directory)) { names = paths.filter(p -> p.toString().endsWith(".kc2")).map(p -> p.getFileName().toString().replaceFirst("\\.kc2$", "")).sorted().toList(); }
        } catch (Exception e) { status = "Не удалось прочитать профили"; }
    }
    @Override protected void init() {
        refresh(); int x = (width - 300) / 2, y = Math.max(2, (height - 218) / 2);
        EditBox input = new EditBox(font, x + 12, y + 37, 276, 20, Component.literal("Имя профиля")); input.setMaxLength(32); input.setValue(name); input.setResponder(v -> name = v); addRenderableWidget(input);
        addRenderableWidget(new PanelButton("Сохранить", x + 12, y + 64, 134, b -> action(() -> {
            try { Files.writeString(profile(), ConfigCodec.encode(RvSteal.snapshot()), StandardCharsets.UTF_8); refresh(); status = "Профиль сохранён"; } catch (java.io.IOException e) { throw new IllegalArgumentException("Ошибка записи"); }
        })));
        addRenderableWidget(new PanelButton("Загрузить", x + 154, y + 64, 134, b -> action(() -> {
            try { Path p = profile(); if (Files.size(p) > 65536) throw new IllegalArgumentException("Слишком большой файл"); RvSteal.apply(ConfigCodec.decode(Files.readString(p))); RvSteal.save(); status = "Загружено: " + name; } catch (java.io.IOException e) { throw new IllegalArgumentException("Профиль не найден"); }
        })));
        addRenderableWidget(new PanelButton("Выбрать следующий", x + 12, y + 90, 134, b -> { if (!names.isEmpty()) { name = names.get((names.indexOf(name) + 1) % names.size()); input.setValue(name); } }));
        addRenderableWidget(new PanelButton("Удалить", x + 154, y + 90, 134, b -> action(() -> {
            try { Files.deleteIfExists(profile()); refresh(); status = "Удалено"; } catch (java.io.IOException e) { throw new IllegalArgumentException("Ошибка удаления"); }
        })));
        addRenderableWidget(new PanelButton("Поделиться кодом", x + 12, y + 123, 134, b -> action(() -> {
            try { minecraft.keyboardHandler.setClipboard(ConfigCodec.encode(RvSteal.snapshot())); status = "Код скопирован"; } catch (java.io.IOException e) { throw new IllegalArgumentException("Ошибка копирования"); }
        })));
        addRenderableWidget(new PanelButton("Импорт из буфера", x + 154, y + 123, 134, b -> action(() -> {
            try { RvSteal.apply(ConfigCodec.decode(minecraft.keyboardHandler.getClipboard())); RvSteal.save(); status = "Импортировано"; } catch (Exception e) { throw new IllegalArgumentException("Неверный код"); }
        })));
        addRenderableWidget(new PanelButton("Назад", x + 12, y + 183, 276, b -> onClose()));
    }
    @Override public void render(GuiGraphics g, int mx, int my, float delta) {
        int x = (width - 300) / 2, y = Math.max(2, (height - 218) / 2);
        g.fill(0, 0, width, height, 0xa0090b10); Ui.round(g, x, y, 300, 214, 9, 0xfa191f29);
        g.drawString(font, title, x + 12, y + 12, 0xffdfc7ff);
        g.drawString(font, "Имя профиля   •   сохранено: " + names.size(), x + 12, y + 26, 0xff969cad);
        g.drawString(font, font.plainSubstrByWidth(status, 276), x + 12, y + 158, 0xffcdb9ef);
        super.render(g, mx, my, delta);
    }
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
