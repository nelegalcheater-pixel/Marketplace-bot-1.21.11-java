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
            int x = HudLayout.x("Watermark", g.guiWidth(), 8), y = HudLayout.y("Watermark", g.guiHeight(), 8);
            Ui.round(g, x, y, w, 20, 6, 0xc0192029);
            g.drawString(c.font, c.font.plainSubstrByWidth(watermark, w - 12), x + 6, y + 6, 0xffd8d2f4);
        }
        leftBottom = 80;
        if (ModuleSettings.hotkeysHud) { panel(g, "Hotkeys", hotkeys, HudLayout.x("Hotkeys", g.guiWidth(), 8), HudLayout.y("Hotkeys", g.guiHeight(), leftBottom)); leftBottom += 30 + Math.min(5, hotkeys.size()) * 12; }
        if (ModuleSettings.potionsHud) { panel(g, "Potions", potions, HudLayout.x("Potions", g.guiWidth(), 8), HudLayout.y("Potions", g.guiHeight(), leftBottom)); leftBottom += 30 + Math.min(5, potions.size()) * 12; }
        if (ModuleSettings.staffHud) panel(g, "Staffs [TAB rank]", staffs, HudLayout.x("Staffs", g.guiWidth(), Math.max(8, g.guiWidth() - 188)), HudLayout.y("Staffs", g.guiHeight(), 72));
        if (ModuleSettings.targetHud) renderTarget(g, c);
    }
    private static void renderTarget(GuiGraphics g, Minecraft c) {
        var target = ClientModules.target;
        if (target == null && c.crosshairPickEntity instanceof net.minecraft.world.entity.LivingEntity entity) target = entity;
        if (target == null || target.isRemoved() || !target.isAlive()) return;
        int x = HudLayout.x("Target", g.guiWidth(), g.guiWidth() / 2 + 20), y = HudLayout.y("Target", g.guiHeight(), g.guiHeight() / 2 + 20);
        int w = Math.min(150, g.guiWidth() - x - 4);
        if (w < 40 || y + 62 > g.guiHeight()) return;
        Ui.round(g, x, y, w, 60, 6, 0xd0192029);
        g.drawString(c.font, c.font.plainSubstrByWidth(target.getName().getString(), w - 12), x + 6, y + 5, 0xffe8dafa);
        float hp = Math.clamp(target.getHealth() / Math.max(1, target.getMaxHealth()), 0, 1);
        g.drawString(c.font, String.format(Locale.ROOT, "HP %.1f / %.1f", target.getHealth(), target.getMaxHealth()), x + 6, y + 16, 0xffd7d7e9);
        g.fill(x + 6, y + 28, x + w - 6, y + 31, 0xff404451);
        g.fill(x + 6, y + 28, x + 6 + Math.round((w - 12) * hp), y + 31, 0xffa89afa);
        int ix = x + 6;
        for (var slot : new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET}) {
            var stack = target.getItemBySlot(slot);
            if (!stack.isEmpty()) { g.renderItem(stack, ix, y + 35); ix += 18; }
        }
        String effects = target.getActiveEffects().stream().map(e -> Component.translatable(e.getDescriptionId()).getString()).limit(2).reduce((a, b) -> a + ", " + b).orElse("");
        if (!effects.isEmpty()) g.drawString(c.font, c.font.plainSubstrByWidth(effects, w - 12), x + 6, y + 51, 0xffc1b5d9);
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
