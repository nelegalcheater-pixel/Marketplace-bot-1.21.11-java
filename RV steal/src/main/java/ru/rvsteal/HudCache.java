package ru.rvsteal;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Refresh text at 4 Hz rather than allocating formatted strings on every frame. */
final class HudCache {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private record Armor(ItemStack stack, String durability) {}
    private static String coordinates = "";
    private static String health = "";
    private static List<String> nearby = List.of();
    private static List<Armor> armor = List.of();
    private static Object level;
    private static int ticks;

    static void tick(Minecraft client) {
        if (client.player == null || client.level == null) {
            coordinates = ""; nearby = List.of(); armor = List.of(); level = null; ticks = 0;
            return;
        }
        if (level != client.level) { level = client.level; ticks = 0; }
        if (ticks++ % 5 != 0) return;
        var player = client.player;
        coordinates = RvSteal.coordinatesHud ? String.format(Locale.ROOT, "XYZ: %.1f / %.1f / %.1f | FPS: %d",
                player.getX(), player.getY(), player.getZ(), client.getFps()) : "";
        health = RvSteal.healthHud ? String.format(Locale.ROOT, "HP: %.0f / %.0f | Food: %d", player.getHealth(), player.getMaxHealth(), player.getFoodData().getFoodLevel()) : "";
        var lines = new ArrayList<String>();
        if (RvSteal.betterNear) {
            int maxRows = Math.max(0, (client.getWindow().getGuiScaledHeight() - 84) / 10);
            for (var other : client.level.players()) {
                if (other == player || other.isRemoved() || player.distanceToSqr(other) > 128 * 128) continue;
                if (lines.size() >= maxRows) break;
                lines.add(other.getName().getString() + "  " + Math.round(player.distanceTo(other)) + "m");
            }
        }
        nearby = List.copyOf(lines);
        var items = new ArrayList<Armor>();
        if (RvSteal.armorHud) for (var slot : SLOTS) {
            var stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) continue;
            String durability = "";
            if (stack.isDamageableItem() && stack.getMaxDamage() > 0) {
                int percent = Math.clamp(Math.round(100f * (stack.getMaxDamage() - stack.getDamageValue()) / stack.getMaxDamage()), 0, 100);
                durability = percent + "%";
            }
            items.add(new Armor(stack.copy(), durability));
        }
        armor = List.copyOf(items);
    }

    static void render(GuiGraphics graphics) {
        var client = Minecraft.getInstance();
        if (client.player == null || client.level == null || client.level != level) return;
        if (RvSteal.coordinatesHud) graphics.drawString(client.font, coordinates, 8, 35, 0xffffffff);
        if (RvSteal.healthHud) graphics.drawString(client.font, health, 8, RvSteal.coordinatesHud ? 46 : 35, 0xffff8c9f);
        if (RvSteal.armorHud) {
            int x = Math.max(0, graphics.guiWidth() - 80), y = 8;
            for (var item : armor) {
                graphics.renderItem(item.stack(), x, y);
                graphics.drawString(client.font, item.durability(), x + 19, y + 4, 0xffffffff);
                y += 20;
            }
        }
        if (RvSteal.betterNear) {
            int y = ScreenHud.leftBottom();
            if (y > graphics.guiHeight() - 24) return;
            graphics.drawString(client.font, "Better Near", 8, y, 0xffff6c24);
            y += 12;
            for (String line : nearby) {
                if (y > graphics.guiHeight() - 40) break;
                graphics.drawString(client.font, line, 8, y, 0xffffffff);
                y += 10;
            }
        }
    }
}
