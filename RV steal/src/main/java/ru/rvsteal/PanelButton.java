package ru.rvsteal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
final class PanelButton extends Button {
    PanelButton(String label, int x, int y, int w, OnPress action) { super(x, y, w, 20, Component.literal(label), action, DEFAULT_NARRATION); }
    @Override protected void renderContents(GuiGraphics g, int mx, int my, float delta) {
        Ui.round(g, getX(), getY(), getWidth(), getHeight(), 5, isHoveredOrFocused() ? 0xff645781 : 0xff30313f);
        var font = Minecraft.getInstance().font;
        g.drawCenteredString(font, getMessage(), getX() + getWidth() / 2, getY() + 6, active ? 0xffeee8fa : 0xff74717c);
    }
}
