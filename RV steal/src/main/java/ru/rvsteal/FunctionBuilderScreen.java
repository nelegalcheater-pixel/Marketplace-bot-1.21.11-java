package ru.rvsteal;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class FunctionBuilderScreen extends Screen {
    private final Screen parent;
    private String status = "", name = "", actions = "ночное зрение; координаты";
    FunctionBuilderScreen(Screen parent) { super(Component.literal("Конструктор функций")); this.parent = parent; }
    @Override protected void init() {
        int x = (width - 300) / 2, y = Math.max(4, (height - 200) / 2);
        EditBox n = new EditBox(font, x + 10, y + 32, 280, 20, Component.literal("Название")); n.setMaxLength(32); n.setValue(name); n.setHint(Component.literal("Название функции")); n.setResponder(v -> name = v); addRenderableWidget(n);
        EditBox a = new EditBox(font, x + 10, y + 65, 280, 20, Component.literal("Действия")); a.setMaxLength(1024); a.setValue(actions); a.setResponder(v -> actions = v); addRenderableWidget(a);
        addRenderableWidget(new PanelButton("Сохранить функцию", x + 10, y + 95, 136, b -> {
            try { CustomFunctions.create(name, actions); RvSteal.save(); status = "Сохранено во вкладку Custom"; }
            catch (IllegalArgumentException e) { status = e.getMessage(); }
        }));
        addRenderableWidget(new PanelButton("Удалить по имени", x + 154, y + 95, 136, b -> { CustomFunctions.functions.remove(name.trim()); RvSteal.save(); status = "Удалено"; }));
        addRenderableWidget(new PanelButton("Доступные действия", x + 10, y + 123, 280, b -> minecraft.setScreen(new ActionsScreen(this))));
        addRenderableWidget(new PanelButton("Назад", x + 10, y + 167, 280, b -> onClose()));
    }
    @Override public void render(GuiGraphics g, int mx, int my, float delta) {
        int x = (width - 300) / 2, y = Math.max(4, (height - 200) / 2);
        g.fill(0, 0, width, height, 0xa0090b10); Ui.round(g, x, y, 300, 196, 8, 0xfa191f29);
        g.drawString(font, title, x + 10, y + 12, 0xffd9bdff);
        g.drawString(font, "Действия через ;  (тумблер управляет всеми)", x + 10, y + 54, 0xff969cad);
        g.drawString(font, font.plainSubstrByWidth(status, 280), x + 10, y + 149, 0xffc8b5ed);
        super.render(g, mx, my, delta);
    }
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
