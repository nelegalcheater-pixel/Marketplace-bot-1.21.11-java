package ru.rvsteal;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class ScriptEditorScreen extends Screen {
    private final Screen parent;
    private String name = "", code = "-- Выполняется каждый тик\nif health() < 10 then\n  module(\"Night Vision\", true)\nend", status = "";
    ScriptEditorScreen(Screen parent) { super(Component.literal("Редактор Lua")); this.parent = parent; }
    ScriptEditorScreen(Screen parent, String name) { this(parent); var s = UserScripts.scripts.get(name); if (s != null) { this.name = name; code = s.code(); } }
    @Override protected void init() {
        int w = Math.min(340, width - 16), x = (width - w) / 2, y = 6;
        EditBox n = new EditBox(font, x, y + 18, w, 20, Component.literal("Название")); n.setMaxLength(32); n.setValue(name); n.setHint(Component.literal("Название функции")); n.setResponder(v -> name = v); addRenderableWidget(n);
        int editorHeight = Math.max(24, height - 131);
        MultiLineEditBox editor = MultiLineEditBox.builder().setX(x).setY(y + 52).setShowBackground(true).setShowDecorations(true).setPlaceholder(Component.literal("Код Lua"))
                .build(font, w, editorHeight, Component.literal("Код Lua"));
        editor.setCharacterLimit(UserScripts.MAX_CODE); editor.setValue(code); editor.setValueListener(v -> code = v); addRenderableWidget(editor);
        int row = y + 56 + editorHeight, half = (w - 4) / 2;
        addRenderableWidget(new PanelButton("Сохранить", x, row, half, b -> { try { UserScripts.create(name, code); RvSteal.save(); status = "Сохранено в Custom (выключено)"; } catch (RuntimeException e) { status = e.getMessage(); } }));
        addRenderableWidget(new PanelButton("Удалить", x + half + 4, row, half, b -> { UserScripts.delete(name.trim()); RvSteal.save(); status = "Удалено"; }));
        addRenderableWidget(new PanelButton("Назад", x, row + 24, w, b -> onClose()));
    }
    @Override public void render(GuiGraphics g, int mx, int my, float delta) {
        int w = Math.min(340, width - 16), x = (width - w) / 2;
        g.fill(0, 0, width, height, 0xf0191f29); g.drawString(font, title, x, 8, 0xffdfc7ff);
        g.drawString(font, "Lua: health(), hunger(), module(name, bool), message(text)", x, 44, 0xffa99dbd);
        g.drawString(font, font.plainSubstrByWidth(status, w), x, height - 16, 0xffdfc7ff);
        super.render(g, mx, my, delta);
    }
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
