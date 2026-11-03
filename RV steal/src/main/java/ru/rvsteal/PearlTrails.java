package ru.rvsteal;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import ru.rvsteal.mixin.GameRendererAccess;
import java.util.*;

final class PearlTrails {
    private static class Trail { final List<Vec3> points = new ArrayList<>(); int lastTick; }
    private static final Map<UUID, Trail> trails = new HashMap<>();
    private static int tick;
    private static Object world;
    static void tick(Minecraft c) {
        if (world != c.level || !ModuleSettings.pearlTrails || c.level == null) { world = c.level; trails.clear(); }
        if (!ModuleSettings.pearlTrails || c.level == null) return;
        tick++;
        for (var entity : c.level.entitiesForRendering()) if (entity instanceof ThrowableItemProjectile pearl) {
            if (!trails.containsKey(pearl.getUUID()) && trails.size() >= 64) continue;
            var trail = trails.computeIfAbsent(pearl.getUUID(), k -> new Trail());
            if (trail.points.isEmpty() || trail.points.getLast().distanceToSqr(pearl.position()) > .01) trail.points.add(pearl.position());
            if (trail.points.size() > 80) trail.points.removeFirst();
            trail.lastTick = tick;
        }
        trails.values().removeIf(t -> tick - t.lastTick > 40);
    }
    static void render(GuiGraphics g, float delta) {
        var c = Minecraft.getInstance();
        if (!ModuleSettings.pearlTrails || c.player == null || c.level == null || c.options.hideGui) return;
        var camera = c.gameRenderer.getMainCamera();
        var matrix = c.gameRenderer.getProjectionMatrix(((GameRendererAccess)c.gameRenderer).killerkc2$getFov(camera, delta, true))
                .rotate(new Quaternionf(camera.rotation()).conjugate());
        var cp = camera.position();
        for (var trail : trails.values()) {
            for (Vec3 point : trail.points) {
                var b = EspProjection.bounds(AABB.ofSize(point, .1, .1, .1).move(-cp.x, -cp.y, -cp.z), matrix, g.guiWidth(), g.guiHeight());
                if (b != null) {
                    int x = (b[0] + b[2]) / 2, y = (b[1] + b[3]) / 2;
                    g.fill(x - 1, y - 1, x + 2, y + 2, 0xffbc9aff);
                }
            }
        }
    }
}
