package ru.rvsteal;
import net.minecraft.client.gui.GuiGraphics;
final class Ui {
    static void round(GuiGraphics g, int x, int y, int w, int h, int radius, int color) {
        int r = Math.min(radius, Math.min(w, h) / 2);
        g.fill(x, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            int inset = r - (int)Math.sqrt(r * r - (r - i - 1) * (r - i - 1));
            g.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            g.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, color);
        }
    }
}
