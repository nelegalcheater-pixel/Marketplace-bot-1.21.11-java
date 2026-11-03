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
import org.joml.Vector4f;
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
    private static int cursor, ticks;
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
        int r = ModuleSettings.blockRange, side = r * 2 + 1, volume = side * side * side;
        if (origin == null || origin.distSqr(c.player.blockPosition()) > 64) { origin = c.player.blockPosition(); cursor = 0; found.clear(); }
        found.removeIf(pos -> !c.level.hasChunkAt(pos) || pos.distSqr(c.player.blockPosition()) > r * r || !selected.contains(c.level.getBlockState(pos).getBlock()));
        for (int budget = 0; budget < 4096; budget++) {
            if (cursor >= volume) { cursor = 0; origin = c.player.blockPosition(); break; }
            int index = cursor++;
            BlockPos pos = origin.offset(index % side - r, index / (side * side) - r, index / side % side - r);
            if (pos.distSqr(c.player.blockPosition()) <= r * r && c.level.hasChunkAt(pos)
                    && selected.contains(c.level.getBlockState(pos).getBlock()) && found.size() < 512) found.add(pos);
        }
    }
    static void render(GuiGraphics g, float delta) {
        var c = Minecraft.getInstance();
        if (c.player == null || c.level == null || c.level != level || c.options.hideGui) return;
        if (!RvSteal.playerEsp && !RvSteal.mobEsp && !ModuleSettings.blockEsp) return;
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
            int[] b = bounds(e.getBoundingBox().move(offset).move(-cameraPos.x, -cameraPos.y, -cameraPos.z), projection, g);
            if (b == null) continue;
            int color = player ? 0xffc58cff : 0xffff9370;
            if (settings.box) outline(g, b, color);
            int x = Math.clamp((b[0] + b[2]) / 2 - 45, 0, Math.max(0, g.guiWidth() - 160));
            int y = Math.max(1, b[1] - 23);
            if (settings.name) { text(g, info.name(), x, y, color); y += 10; }
            if (settings.hp) { text(g, info.hp(), x, y, 0xff8df5ad); y += 10; }
            y = Math.max(y, b[3] + 2);
            if (settings.gear) {
                int ix = x;
                for (var item : info.items()) { g.renderItem(item.stack(), ix, y); ix += 18; }
                if (!info.items().isEmpty()) y += 18;
            }
            if (settings.enchants) for (var item : info.items()) {
                if (item.enchants().isEmpty()) continue;
                text(g, item.stack().getHoverName().getString(), x, y, 0xffccccdf); y += 10;
                for (String enchant : item.enchants()) {
                    if (y > g.guiHeight() - 10) break;
                    text(g, enchant, x, y, 0xffbda3df); y += 10;
                }
            }
            if (++count >= 64) break;
        }
        if (ModuleSettings.blockEsp) for (BlockPos pos : found) {
            int[] b = bounds(new AABB(pos).move(-cameraPos.x, -cameraPos.y, -cameraPos.z), projection, g);
            if (b != null) outline(g, b, 0xff65e5e1);
        }
    }
    private static void text(GuiGraphics g, String text, int x, int y, int color) {
        var font = Minecraft.getInstance().font;
        String clipped = font.plainSubstrByWidth(text, Math.max(1, g.guiWidth() - x));
        g.fill(x - 1, y - 1, x + font.width(clipped) + 1, y + 9, 0xa611131c);
        g.drawString(font, clipped, x, y, color);
    }
    private static int[] bounds(AABB b, Matrix4f matrix, GuiGraphics g) {
        float minX = Float.POSITIVE_INFINITY, minY = minX, maxX = Float.NEGATIVE_INFINITY, maxY = maxX;
        for (int i = 0; i < 8; i++) {
            Vector4f v = matrix.transform(new Vector4f((float)((i & 1) == 0 ? b.minX : b.maxX), (float)((i & 2) == 0 ? b.minY : b.maxY), (float)((i & 4) == 0 ? b.minZ : b.maxZ), 1));
            if (v.w <= 0.05f) return null;
            float x = (v.x / v.w + 1) * g.guiWidth() / 2f, y = (1 - v.y / v.w) * g.guiHeight() / 2f;
            minX = Math.min(minX, x); maxX = Math.max(maxX, x); minY = Math.min(minY, y); maxY = Math.max(maxY, y);
        }
        if (maxX < 0 || maxY < 0 || minX > g.guiWidth() || minY > g.guiHeight()) return null;
        return new int[]{Math.clamp((int)minX, 0, g.guiWidth()), Math.clamp((int)minY, 0, g.guiHeight()), Math.clamp((int)maxX, 0, g.guiWidth()), Math.clamp((int)maxY, 0, g.guiHeight())};
    }
    private static void outline(GuiGraphics g, int[] b, int color) {
        g.fill(b[0], b[1], b[2] + 1, b[1] + 1, color); g.fill(b[0], b[3], b[2] + 1, b[3] + 1, color);
        g.fill(b[0], b[1], b[0] + 1, b[3] + 1, color); g.fill(b[2], b[1], b[2] + 1, b[3] + 1, color);
    }
}
