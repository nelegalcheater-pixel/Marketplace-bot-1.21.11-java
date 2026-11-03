package ru.rvsteal;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.io.*;
import java.util.Properties;

final class ConfigShareScreen extends Screen {
    private final Screen parent;
    private String status = "";
    ConfigShareScreen(Screen parent) { super(Component.literal("Обмен конфигом")); this.parent = parent; }
    @Override protected void init() {
        int x = (width - 260) / 2, y = (height - 130) / 2;
        addRenderableWidget(Button.builder(Component.literal("Скопировать конфиг"), b -> {
            try {
                minecraft.keyboardHandler.setClipboard(ConfigCodec.encode(RvSteal.snapshot()));
                status = "Скопировано в буфер обмена";
            } catch (IOException e) { status = "Ошибка копирования"; }
        }).bounds(x, y + 24, 260, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Загрузить из буфера обмена"), b -> {
            try {
                Properties p = ConfigCodec.decode(minecraft.keyboardHandler.getClipboard());
                RvSteal.apply(p); RvSteal.save(); status = "Конфиг загружен";
            } catch (Exception e) { status = "Неверный код конфига"; }
        }).bounds(x, y + 50, 260, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Назад"), b -> onClose()).bounds(x, y + 76, 260, 20).build());
    }
    @Override public void render(GuiGraphics g, int mx, int my, float delta) {
        g.fill(0, 0, width, height, 0xe811131c);
        g.drawString(font, title, (width - 260) / 2, (height - 130) / 2 + 7, 0xffd9bdff);
        super.render(g, mx, my, delta);
        g.drawString(font, status, (width - 260) / 2, (height - 130) / 2 + 105, 0xffd9bdff);
    }
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
