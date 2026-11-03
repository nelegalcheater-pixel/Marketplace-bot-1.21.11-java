package ru.rvsteal;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
final class ActionsScreen extends Screen {
    private final Screen parent;
    ActionsScreen(Screen parent) { super(Component.literal("Доступные действия")); this.parent = parent; }
    @Override protected void init() { addRenderableWidget(new PanelButton("Назад", (width - 260) / 2, height - 28, 260, b -> onClose())); }
    @Override public void render(GuiGraphics g, int mx, int my, float delta) {
        g.fill(0, 0, width, height, 0xf0191f29);
        int x = (width - 300) / 2, y = 12;
        g.drawString(font, title, x, y, 0xffdfc7ff);
        String[] names = CustomFunctions.supported().split("; ");
        for (int i = 0; i < names.length; i++) g.drawString(font, names[i], x + (i % 2) * 150, y + 28 + (i / 2) * 19, 0xffc5bdd8);
        g.drawString(font, "Например: включить ночное зрение; координаты", x, height - 47, 0xffa499bd);
        super.render(g, mx, my, delta);
    }
    @Override public void onClose() { minecraft.setScreen(parent); }
}
