package ru.rvsteal;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import java.util.*;

final class ScreenHud {
    private static List<String> hotkeys = List.of(), potions = List.of(), staffs = List.of();
    private static String watermark = "";
    private static int ticks;
    private static int leftBottom = 72;
    static int leftBottom() { return leftBottom; }
    static void tick(Minecraft c) {
        if (c.player == null || c.level == null) { ticks = 0; hotkeys = potions = staffs = List.of(); return; }
        if (ticks++ % 10 != 0) return;
        var connection = c.getConnection();
        var self = connection == null ? null : connection.getPlayerInfo(c.player.getUUID());
        watermark = "KillerKC2  |  " + c.player.getName().getString() + "  |  " + c.getFps() + " fps  |  " + (self == null ? "--" : self.getLatency()) + " ms";
        var keys = new ArrayList<String>();
        for (var m : RvScreen.allModules()) {
            Integer key = ModuleSettings.binds.get(m.name());
            if (key == null || !m.get().getAsBoolean()) continue;
            String name = GLFW.glfwGetKeyName(key, 0);
            keys.add(m.name() + " [" + (name == null ? "KEY " + key : name.toUpperCase(Locale.ROOT)) + "]");
        }
        hotkeys = keys;
        var effects = new ArrayList<String>();
        for (var e : c.player.getActiveEffects()) {
            int seconds = Math.max(0, e.getDuration() / 20);
            effects.add(Component.translatable(e.getDescriptionId()).getString() + " " + (e.getAmplifier() + 1) + "  " + (e.isInfiniteDuration() ? "∞" : String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60)));
        }
        if (ModuleSettings.nightVision) effects.add("Night Vision [client]");
        potions = effects;
        var online = new ArrayList<String>();
        if (connection != null) for (var info : connection.getOnlinePlayers()) {
            var team = info.getTeam();
            String rank = team == null ? "" : team.getName() + " " + team.getPlayerPrefix().getString();
            if (info.getTabListDisplayName() != null) rank += " " + info.getTabListDisplayName().getString();
            String value = rank.toLowerCase(Locale.ROOT);
            if (value.matches(".*(?:admin|moder|staff|helper|админ|модер|хелпер).*")) online.add(info.getProfile().name());
        }
        staffs = online;
    }
    static void render(GuiGraphics g) {
        var c = Minecraft.getInstance();
        if (c.player == null || c.level == null || c.options.hideGui) return;
        if (ModuleSettings.watermark) {
            int w = Math.min(g.guiWidth() - 16, c.font.width(watermark) + 16);
            Ui.round(g, 8, 8, w, 20, 6, 0xc0192029);
            g.drawString(c.font, c.font.plainSubstrByWidth(watermark, w - 12), 14, 14, 0xffd8d2f4);
        }
        leftBottom = 72;
        if (ModuleSettings.hotkeysHud) { panel(g, "Hotkeys", hotkeys, 8, leftBottom); leftBottom += 30 + Math.min(5, hotkeys.size()) * 12; }
        if (ModuleSettings.potionsHud) { panel(g, "Potions", potions, 8, leftBottom); leftBottom += 30 + Math.min(5, potions.size()) * 12; }
        if (ModuleSettings.staffHud) panel(g, "Staffs [TAB rank]", staffs, Math.max(8, g.guiWidth() - 188), 72);
    }
    private static void panel(GuiGraphics g, String title, List<String> lines, int x, int y) {
        var font = Minecraft.getInstance().font;
        if (y > g.guiHeight() - 22) return;
        int w = font.width(title) + 16;
        for (String line : lines) w = Math.max(w, font.width(line) + 16);
        w = Math.min(w, Math.min(180, g.guiWidth() - x - 4));
        int rows = Math.min(Math.min(5, lines.size()), Math.max(0, (g.guiHeight() - y - 26) / 12));
        Ui.round(g, x, y, w, 22 + rows * 12, 5, 0xc0192029);
        g.drawString(font, title, x + 7, y + 7, 0xffc2b6f5);
        for (int i = 0; i < rows; i++) g.drawString(font, font.plainSubstrByWidth(lines.get(i), w - 14), x + 7, y + 23 + i * 12, 0xffd2d5e0);
    }
}
