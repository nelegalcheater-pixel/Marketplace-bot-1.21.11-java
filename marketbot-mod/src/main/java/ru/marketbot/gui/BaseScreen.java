package ru.marketbot.gui;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Общая база экранов. Намеренно использует только готовые виджеты Minecraft
 * (кнопки, поля, надписи) и не переопределяет рендер/ввод — так меньше шансов
 * сломаться при смене версии.
 */
public abstract class BaseScreen extends Screen {
    protected final Screen parent;
    /** Строка статуса внизу экрана. */
    protected static String status = "";

    protected BaseScreen(String title, Screen parent) {
        super(Component.literal(title));
        this.parent = parent;
    }

    protected StringWidget label(int x, int y, int w, String text) {
        // Ширина = ширине текста, чтобы надпись стояла у левого края без alignLeft().
        int tw = Math.max(1, Math.min(w, this.font.width(text)));
        return addRenderableWidget(new StringWidget(x, y, tw, 12, Component.literal(text), this.font));
    }

    protected Button btn(int x, int y, int w, String text, Runnable action) {
        return addRenderableWidget(
                Button.builder(Component.literal(text), b -> action.run()).bounds(x, y, w, 20).build());
    }

    protected EditBox field(int x, int y, int w, String value, int max) {
        EditBox e = new EditBox(this.font, x, y, w, 20, Component.empty());
        e.setMaxLength(max);
        e.setValue(value == null ? "" : value);
        return addRenderableWidget(e);
    }

    protected static String cut(String s, int n) {
        return s.length() <= n ? s : s.substring(0, Math.max(1, n - 2)) + "..";
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
