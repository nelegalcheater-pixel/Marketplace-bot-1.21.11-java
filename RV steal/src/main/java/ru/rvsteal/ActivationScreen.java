package ru.rvsteal;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class ActivationScreen extends Screen {
    private EditBox input;
    private String error = "";
    ActivationScreen() { super(Component.literal("KillerKC2 client")); }
    @Override protected void init() {
        int x = (width - 240) / 2, y = height / 2;
        input = new EditBox(font, x, y - 10, 240, 20, Component.literal("Ключ активации"));
        input.setMaxLength(128); input.setHint(Component.literal("Ключ активации"));
        addRenderableWidget(input); setInitialFocus(input);
        addRenderableWidget(Button.builder(Component.literal("Активировать"), button -> {
            if (AccessKey.activate(input.getValue().trim())) {
                RvSteal.save(); minecraft.setScreen(new RvScreen());
            } else error = "Неверный ключ";
        }).bounds(x, y + 18, 240, 20).build());
    }
    @Override public void render(GuiGraphics g, int mx, int my, float delta) {
        g.fill(0, 0, width, height, 0xe5101419);
        int x = (width - 240) / 2, y = height / 2;
        g.drawString(font, "KILLERKC2 CLIENT / ACTIVATION", x, y - 48, 0xffd3c8ff);
        g.drawString(font, "Введите ключ для доступа к модулям", x, y - 30, 0xff999da8);
        super.render(g, mx, my, delta);
        g.drawString(font, error, x, y + 48, 0xffff7a90);
    }
    @Override public boolean isPauseScreen() { return false; }
}
