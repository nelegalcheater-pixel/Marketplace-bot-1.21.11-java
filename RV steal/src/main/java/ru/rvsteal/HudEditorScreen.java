package ru.rvsteal;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

final class HudEditorScreen extends Screen {
    private final Screen parent;
    private String dragging;
    private int dx, dy;
    HudEditorScreen(Screen parent) { super(Component.literal("Перетащи элементы HUD")); this.parent = parent; }
    static int defaultX(String name, int w) {
        return switch (name) { case "Staffs" -> Math.max(8, w - 188); case "Target" -> w / 2 + 20; case "Armor" -> Math.max(0, w - 90); default -> 8; };
    }
    static int defaultY(String name, int h) {
        return switch (name) { case "Watermark", "Armor" -> 8; case "Coordinates" -> 35; case "Health" -> 55; case "Hotkeys" -> 80; case "Potions" -> 110; case "Better Near" -> 140; case "Target" -> h / 2 + 20; case "Block ESP" -> h - 60; default -> 72; };
    }
    @Override protected void init() {
        addRenderableWidget(Button.builder(Component.literal("Сброс HUD"), b -> HudLayout.reset()).bounds(width / 2 - 105, 4, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Готово"), b -> onClose()).bounds(width / 2 + 5, 4, 100, 20).build());
    }
    @Override public void render(GuiGraphics g, int mx, int my, float delta) {
        g.fill(0, 0, width, height, 0x8011131c);
        for (String name : HudLayout.NAMES) {
            int x = HudLayout.x(name, width, defaultX(name, width)), y = HudLayout.y(name, height, defaultY(name, height));
            Ui.round(g, x, y, 90, 18, 4, 0xd029303b); g.drawString(font, name, x + 4, y + 5, 0xffe2dafa);
        }
        super.render(g, mx, my, delta);
    }
    @Override public boolean mouseClicked(MouseButtonEvent e, boolean dbl) {
        if (super.mouseClicked(e, dbl)) return true;
        if (e.button() != 0) return false;
        for (int i = HudLayout.NAMES.length - 1; i >= 0; i--) {
            String name = HudLayout.NAMES[i];
            int x = HudLayout.x(name, width, defaultX(name, width)), y = HudLayout.y(name, height, defaultY(name, height));
            if (e.x() >= x && e.x() < x + 90 && e.y() >= y && e.y() < y + 18) { dragging = name; dx = (int)e.x() - x; dy = (int)e.y() - y; return true; }
        }
        return false;
    }
    @Override public boolean mouseDragged(MouseButtonEvent e, double x, double y) {
        if (dragging == null) return false;
        HudLayout.move(dragging, Math.clamp((int)e.x() - dx, 0, Math.max(0, width - 90)), Math.clamp((int)e.y() - dy, 0, Math.max(0, height - 18)), width, height);
        return true;
    }
    @Override public boolean mouseReleased(MouseButtonEvent e) { if (dragging != null) { dragging = null; RvSteal.save(); return true; } return super.mouseReleased(e); }
    @Override public void onClose() { RvSteal.save(); minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
