package ru.rvsteal;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.Block;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import net.minecraft.world.entity.item.ItemEntity;
import ru.rvsteal.mixin.GameRendererAccess;
import java.util.*;

final class EspOverlay {
    private record ItemInfo(ItemStack stack, List<String> enchants) {}
    private record EntityInfo(LivingEntity entity, String name, String hp, List<ItemInfo> items) {}
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};
    private static final List<EntityInfo> entities = new ArrayList<>();
    private static final Set<BlockPos> found = new HashSet<>();
    private static Set<Block> selected = Set.of();
    private static Object level;
    private static BlockPos origin;
    private static int cursor, ticks, scanRange;
    private static List<BlockPos> scanOffsets = List.of();
    static void resetBlocks() { found.clear(); origin = null; cursor = 0; selected = Set.of(); }

    static void tick(Minecraft c) {
        if (c.level != level) { level = c.level; entities.clear(); resetBlocks(); ticks = 0; }
        if (c.player == null || c.level == null) { entities.clear(); resetBlocks(); return; }
        if (ticks++ % 5 == 0) {
            entities.clear();
            if (RvSteal.playerEsp || RvSteal.mobEsp) for (var raw : c.level.entitiesForRendering()) {
                if (!(raw instanceof LivingEntity e) || e == c.player || !e.isAlive()) continue;
                var settings = e instanceof Player ? ModuleSettings.players : ModuleSettings.mobs;
                if (!((RvSteal.playerEsp && e instanceof Player) || (RvSteal.mobEsp && e instanceof Enemy))
                        || e.distanceToSqr(c.player) > settings.range * settings.range) continue;
                var gear = new ArrayList<ItemInfo>();
                if (settings.gear || settings.enchants) for (var slot : SLOTS) {
                    var stack = e.getItemBySlot(slot); if (stack.isEmpty()) continue;
                    var enchants = new ArrayList<String>();
                    if (settings.enchants) {
                        var component = stack.get(DataComponents.ENCHANTMENTS);
                        if (component != null) for (var entry : component.entrySet()) {
                            enchants.add(Enchantment.getFullname(entry.getKey(), entry.getIntValue()).getString());
                        }
                    }
                    gear.add(new ItemInfo(stack.copy(), List.copyOf(enchants)));
                }
                entities.add(new EntityInfo(e, e.getName().getString(), String.format(Locale.ROOT, "HP %.1f / %.1f", e.getHealth(), e.getMaxHealth()), gear));
            }
        }
        if (!ModuleSettings.blockEsp) { if (origin != null) resetBlocks(); return; }
        if (selected.isEmpty()) {
            var blocks = new HashSet<Block>();
            for (String part : ModuleSettings.blocks.split(",")) {
                var id = Identifier.tryParse(part.trim());
                if (id != null && BuiltInRegistries.BLOCK.containsKey(id)) blocks.add(BuiltInRegistries.BLOCK.getValue(id));
            }
            selected = blocks;
        }
        int r = ModuleSettings.blockRange;
        if (r != scanRange || scanOffsets.isEmpty()) {
            scanRange = r;
            var offsets = new ArrayList<BlockPos>();
            for (int yy = -r; yy <= r; yy++) for (int zz = -r; zz <= r; zz++) for (int xx = -r; xx <= r; xx++)
                if (xx * xx + yy * yy + zz * zz <= r * r) offsets.add(new BlockPos(xx, yy, zz));
            offsets.sort(Comparator.comparingDouble(pos -> pos.distSqr(BlockPos.ZERO)));
            scanOffsets = offsets; origin = null; cursor = 0; found.clear();
        }
        if (origin == null || origin.distSqr(c.player.blockPosition()) > 16) { origin = c.player.blockPosition(); cursor = 0; found.clear(); }
        found.removeIf(pos -> !c.level.hasChunkAt(pos) || pos.distSqr(c.player.blockPosition()) > r * r || !selected.contains(c.level.getBlockState(pos).getBlock()));
        for (int budget = 0; budget < 4096; budget++) {
            if (cursor >= scanOffsets.size()) { cursor = 0; break; }
            BlockPos pos = origin.offset(scanOffsets.get(cursor++));
            if (c.level.hasChunkAt(pos) && selected.contains(c.level.getBlockState(pos).getBlock()) && found.size() < 512) found.add(pos);
        }
    }

    static void render(GuiGraphics g, float delta) {
        var c = Minecraft.getInstance();
        if (c.player == null || c.level == null || c.level != level || c.options.hideGui) return;
        if (!RvSteal.playerEsp && !RvSteal.mobEsp && !ModuleSettings.blockEsp && !ModuleSettings.itemEsp) return;
        var camera = c.gameRenderer.getMainCamera();
        float fov = ((GameRendererAccess)c.gameRenderer).killerkc2$getFov(camera, delta, true);
        Matrix4f projection = c.gameRenderer.getProjectionMatrix(fov)
                .rotate(new Quaternionf(camera.rotation()).conjugate());
        var cameraPos = camera.position();
        int count = 0;
        for (var info : entities) {
            var e = info.entity();
            if (e.isRemoved() || !e.isAlive()) continue;
            boolean player = e instanceof Player;
            if (player ? !RvSteal.playerEsp : !RvSteal.mobEsp) continue;
            var settings = player ? ModuleSettings.players : ModuleSettings.mobs;
            if (e.distanceToSqr(c.player) > settings.range * settings.range) continue;
            var offset = e.getPosition(delta).subtract(e.position());
            int[] b = EspProjection.bounds(e.getBoundingBox().move(offset).move(-cameraPos.x, -cameraPos.y, -cameraPos.z), projection, g.guiWidth(), g.guiHeight());
            if (b == null) continue;
            int color = 0xff000000 | settings.color;
            int pad = settings.padding;
            b[0] = Math.max(0, b[0] - pad); b[1] = Math.max(0, b[1] - pad);
            b[2] = Math.min(g.guiWidth(), b[2] + pad); b[3] = Math.min(g.guiHeight(), b[3] + pad);
            if (settings.fill > 0) g.fill(b[0], b[1], b[2], b[3], (settings.fill << 24) | settings.color);
            if (settings.box) {
                if (settings.shadow) frame(g, b, 0xff000000 | settings.outlineColor, settings.thickness + 2, settings.corners);
                frame(g, b, color, settings.thickness, settings.corners);
            }
            if (settings.healthBar) {
                int barX = Math.max(0, b[0] - 5);
                int height = Math.max(1, b[3] - b[1]);
                int hpHeight = Math.round(height * Math.clamp(e.getHealth() / Math.max(1f, e.getMaxHealth()), 0f, 1f));
                g.fill(barX, b[1], barX + 3, b[3], 0xff15151d);
                g.fill(barX, b[3] - hpHeight, barX + 2, b[3], 0xff7bed9c);
            }
            if (settings.tracers) line(g, g.guiWidth() / 2, g.guiHeight(), (b[0] + b[2]) / 2, b[3], color);
            float scale = settings.scale / 100f;
            g.pose().pushMatrix();
            g.pose().scale(scale, scale);
            int labelWidth = 0;
            if (settings.name) labelWidth = Math.max(labelWidth, c.font.width(info.name()));
            if (settings.hp) labelWidth = Math.max(labelWidth, c.font.width(info.hp()));
            int x = Math.max(1, Math.min(Math.round((b[0] + b[2]) / (2 * scale)) - labelWidth / 2,
                    Math.round(g.guiWidth() / scale) - Math.max(110, labelWidth)));
            int lines = (settings.name ? 1 : 0) + (settings.hp ? 1 : 0) + (settings.distance ? 1 : 0);
            int y = Math.max(1, Math.round(b[1] / scale) - lines * 10 - 2);
            int textColor = 0xff000000 | settings.textColor;
            if (settings.name) { label(g, info.name(), x, y, textColor, settings.background); y += 10; }
            if (settings.hp) { label(g, info.hp(), x, y, textColor, settings.background); y += 10; }
            if (settings.distance) label(g, Math.round(e.distanceTo(c.player)) + " m", x, y, textColor, settings.background);
            y = Math.round(b[3] / scale) + 2;
            if (settings.gear) {
                int ix = x;
                for (var item : info.items()) { g.renderItem(item.stack(), ix, y); ix += 18; }
                if (!info.items().isEmpty()) y += 18;
            }
            if (settings.enchants) for (var item : info.items()) {
                if (item.enchants().isEmpty()) continue;
                label(g, item.stack().getHoverName().getString(), x, y, textColor, settings.background); y += 10;
                for (String enchant : item.enchants()) {
                    if (y > g.guiHeight() / scale - 10) break;
                    label(g, enchant, x, y, textColor, settings.background); y += 10;
                }
            }
            g.pose().popMatrix();
            if (++count >= 64) break;
        }
        if (ModuleSettings.blockEsp) {
            for (BlockPos pos : found) {
                int[] b = EspProjection.bounds(new AABB(pos).move(-cameraPos.x, -cameraPos.y, -cameraPos.z), projection, g.guiWidth(), g.guiHeight());
                if (b != null) frame(g, b, 0xff000000 | ModuleSettings.blockColor, 1, false);
            }
            label(g, "Block ESP: " + found.size() + "/512 | " + ModuleSettings.blockRange + " m | "
                    + (selected.isEmpty() ? "проверь ID блоков" : "поиск " + (scanOffsets.isEmpty() ? 0 : cursor * 100 / scanOffsets.size()) + "%"),
                    6, g.guiHeight() - 60, 0xff000000 | ModuleSettings.blockColor, true);
        }
        if (ModuleSettings.itemEsp) {
            int items = 0;
            for (var raw : c.level.entitiesForRendering()) {
                if (!(raw instanceof ItemEntity item) || item.isRemoved() || item.distanceToSqr(c.player) > 4096) continue;
                var offset = item.getPosition(delta).subtract(item.position());
                int[] b = EspProjection.bounds(item.getBoundingBox().move(offset).move(-cameraPos.x, -cameraPos.y, -cameraPos.z), projection, g.guiWidth(), g.guiHeight());
                if (b == null) continue;
                frame(g, b, 0xfff6d386, 1, true);
                label(g, item.getItem().getHoverName().getString() + " x" + item.getItem().getCount(), b[0], Math.max(1, b[1] - 11), 0xfff6d386, true);
                if (++items >= 128) break;
            }
        }
    }
    private static void label(GuiGraphics g, String text, int x, int y, int color, boolean background) {
        var font = Minecraft.getInstance().font;
        // Pose scale applies to both label and clip width.
        float scale = g.pose().m00();
        String clipped = font.plainSubstrByWidth(text, Math.max(1, Math.round(g.guiWidth() / scale) - x));
        if (background) g.fill(x - 1, y - 1, x + font.width(clipped) + 1, y + 9, 0xa611131c);
        g.drawString(font, clipped, x, y, color);
    }
    private static void frame(GuiGraphics g, int[] b, int color, int thickness, boolean corners) {
        int t = Math.min(thickness, Math.max(1, Math.min(b[2] - b[0] + 1, b[3] - b[1] + 1)));
        int length = corners ? Math.max(t, (b[2] - b[0]) / 4) : (b[2] - b[0] + 1);
        int vertical = corners ? Math.max(t, (b[3] - b[1]) / 4) : (b[3] - b[1] + 1);
        for (int side = 0; side < 2; side++) {
            int yy = side == 0 ? b[1] : b[3] - t + 1;
            g.fill(b[0], yy, b[0] + length, yy + t, color);
            g.fill(b[2] - length + 1, yy, b[2] + 1, yy + t, color);
            int xx = side == 0 ? b[0] : b[2] - t + 1;
            g.fill(xx, b[1], xx + t, b[1] + vertical, color);
            g.fill(xx, b[3] - vertical + 1, xx + t, b[3] + 1, color);
        }
    }
    private static void line(GuiGraphics g, int x, int y, int endX, int endY, int color) {
        int length = (int)Math.ceil(Math.hypot(endX - x, endY - y));
        if (length == 0) return;
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().rotate((float)Math.atan2(endY - y, endX - x));
        g.fill(0, 0, length, 1, color);
        g.pose().popMatrix();
    }
}
