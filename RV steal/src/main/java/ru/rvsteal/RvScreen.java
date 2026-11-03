package ru.rvsteal;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

final class RvScreen extends Screen {
    static record Module(String name, String detail, BooleanSupplier get, Consumer<Boolean> set) {}
    private static final String[] CATEGORIES = {"COMBAT", "PVE", "VISUAL", "UTILITY"};
    private int category;
    private int scroll;

    RvScreen() { super(Component.literal("KillerKC2 client")); }

    private List<Module> modules() { return categoryModules(category); }
    static List<Module> allModules() {
        var list = new java.util.ArrayList<Module>();
        for (int i = 0; i < 4; i++) list.addAll(categoryModules(i));
        return list;
    }
    private static List<Module> categoryModules(int category) {
        return switch (category) {
            case 0 -> List.of(
                new Module("Player Aura", "Атака ближайшего игрока", () -> RvSteal.playerAura, v -> RvSteal.playerAura = v),
                new Module("Trigger PvP", "Атака игрока под прицелом", () -> RvSteal.attackPlayers, v -> RvSteal.attackPlayers = v));
            case 1 -> List.of(
                new Module("Mob Aura", "Атака ближайшего монстра", () -> RvSteal.mobAura, v -> RvSteal.mobAura = v),
                new Module("Trigger PvE", "Атака монстра под прицелом", () -> RvSteal.attackMobs, v -> RvSteal.attackMobs = v),
                new Module("Auto Tool", "Выбор инструмента из хотбара", () -> RvSteal.autoTool, v -> RvSteal.autoTool = v),
                new Module("Auto Respawn", "Возрождение после смерти", () -> RvSteal.autoRespawn, v -> RvSteal.autoRespawn = v));
            case 2 -> List.of(
                new Module("Block ESP", "Выбранные блоки сквозь стены", () -> ModuleSettings.blockEsp, v -> ModuleSettings.blockEsp = v),
                new Module("Night Vision", "Клиентское ночное зрение", () -> ModuleSettings.nightVision, v -> ModuleSettings.nightVision = v),
                new Module("Player ESP", "Подсветка загруженных игроков", () -> RvSteal.playerEsp, v -> RvSteal.playerEsp = v),
                new Module("Mob ESP", "Подсветка враждебных мобов", () -> RvSteal.mobEsp, v -> RvSteal.mobEsp = v),
                new Module("Brightness", "Яркость игры на максимум", () -> RvSteal.bright, v -> RvSteal.bright = v),
                new Module("Better Near", "Список игроков рядом", () -> RvSteal.betterNear, v -> RvSteal.betterNear = v),
                new Module("Armor HUD", "Броня и прочность", () -> RvSteal.armorHud, v -> RvSteal.armorHud = v),
                new Module("XYZ / FPS", "Координаты и частота кадров", () -> RvSteal.coordinatesHud, v -> RvSteal.coordinatesHud = v),
                new Module("Health HUD", "Здоровье и голод", () -> RvSteal.healthHud, v -> RvSteal.healthHud = v));
            default -> List.of(
                new Module("Auto Sprint", "Спринт при движении вперёд", () -> RvSteal.autoSprint, v -> RvSteal.autoSprint = v),
                new Module("Bunny Hop", "Прыжки при движении вперёд", () -> RvSteal.bunnyHop, v -> RvSteal.bunnyHop = v));
        };
    }
    private int left() { return Math.max(4, (width - Math.min(440, width - 8)) / 2); }
    private int top() { return Math.max(4, (height - Math.min(300, height - 8)) / 2); }
    private int panelWidth() { return Math.min(440, width - 8); }
    private int panelHeight() { return Math.min(300, height - 8); }
    private int visibleRows() { return Math.max(1, (panelHeight() - 66) / 36); }
    private int maxScroll() { return Math.max(0, modules().size() - visibleRows()); }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        int x = left(), y = top(), w = panelWidth(), h = panelHeight();
        g.fill(0, 0, width, height, 0xb0000000);
        g.fill(x, y, x + w, y + h, 0xf2181a26);
        g.fill(x, y, x + w, y + 2, 0xffa765fa);
        g.fill(x, y + 36, x + 104, y + h, 0xff11131c);
        g.drawString(font, "KILLERKC2", x + 14, y + 12, 0xffe4dbff);
        g.drawString(font, "CLIENT", x + 92, y + 12, 0xffa765fa);
        g.drawString(font, "ПКМ: настройки", x + w - 105, y + 12, 0xff777b91);
        for (int i = 0; i < CATEGORIES.length; i++) {
            int ty = y + 46 + i * 27;
            if (i == category) { g.fill(x + 5, ty - 4, x + 99, ty + 17, 0xff29243c); g.fill(x + 5, ty - 4, x + 7, ty + 17, 0xffab6cff); }
            g.drawString(font, CATEGORIES[i], x + 14, ty + 2, i == category ? 0xffd9bdff : 0xff8d91a7);
        }
        g.drawString(font, CATEGORIES[category], x + 117, y + 44, 0xffd9bdff);
        int available = visibleRows();
        List<Module> list = modules();
        for (int i = 0; i < available && i + scroll < list.size(); i++) {
            Module module = list.get(i + scroll);
            int ry = y + 61 + i * 36;
            int rx = x + 113, right = x + w - 11;
            boolean hover = mouseX >= rx && mouseX < right && mouseY >= ry && mouseY < ry + 31;
            g.fill(rx, ry, right, ry + 31, hover ? 0xff303344 : 0xff232632);
            g.drawString(font, module.name(), rx + 8, ry + 4, 0xfff1f1f8);
            if (w >= 360) g.drawString(font, module.detail(), rx + 8, ry + 17, 0xff9297a9);
            g.fill(right - 30, ry + 8, right - 6, ry + 22, module.get().getAsBoolean() ? 0xff9d65ed : 0xff494d5e);
            int knob = module.get().getAsBoolean() ? right - 15 : right - 28;
            g.fill(knob, ry + 10, knob + 7, ry + 20, 0xfff6f3ff);
        }
        if (maxScroll() > 0) g.drawString(font, "Колесо мыши  " + (scroll + 1) + "/" + (maxScroll() + 1), x + 117, y + h - 12, 0xff777b91);
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0 && event.button() != 1) return super.mouseClicked(event, doubleClick);
        double mx = event.x(), my = event.y();
        int x = left(), y = top();
        if (event.button() == 0 && mx >= x + 5 && mx < x + 99) {
            for (int i = 0; i < CATEGORIES.length; i++) {
                int ty = y + 42 + i * 27;
                if (my >= ty && my < ty + 21) { category = i; scroll = 0; return true; }
            }
        }
        if (mx >= x + 113 && mx < x + panelWidth() - 11) {
            for (int i = 0; i < visibleRows() && i + scroll < modules().size(); i++) {
                int ry = y + 61 + i * 36;
                if (my >= ry && my < ry + 31) {
                    Module module = modules().get(i + scroll);
                    if (event.button() == 1) { minecraft.setScreen(new SettingsScreen(this, module)); return true; }
                    module.set().accept(!module.get().getAsBoolean());
                    RvSteal.save();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        scroll = Math.clamp(scroll - (int) Math.signum(vertical), 0, maxScroll());
        return true;
    }
    @Override public boolean isPauseScreen() { return false; }
}
