package ru.rvsteal;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public final class Hitboxes {
    private Hitboxes() {}
    public static void pick(float delta) {
        Minecraft c = Minecraft.getInstance();
        if (!RvSteal.active() || !ModuleSettings.hitboxes || c.player == null || c.level == null
                || c.getCameraEntity() != c.player) return;
        var eye = c.player.getEyePosition(delta);
        double range = c.player.entityInteractionRange();
        var end = eye.add(c.player.getViewVector(delta).scale(range));
        double nearest = range * range;
        // Preserve vanilla occlusion and existing nearer hits.
        if (c.hitResult != null && c.hitResult.getType() != HitResult.Type.MISS)
            nearest = Math.min(nearest, eye.distanceToSqr(c.hitResult.getLocation()));
        EntityHitResult best = null;
        for (Player other : c.level.players()) {
            if (other == c.player || other.isSpectator() || !other.isAlive() || other.isRemoved()) continue;
            var box = other.getBoundingBox().move(other.getPosition(delta).subtract(other.position())).inflate(ModuleSettings.hitboxSize / 100.0);
            var point = box.clip(eye, end);
            if (point.isPresent() && eye.distanceToSqr(point.get()) < nearest) {
                nearest = eye.distanceToSqr(point.get()); best = new EntityHitResult(other, point.get());
            }
        }
        if (best != null) { c.hitResult = best; c.crosshairPickEntity = best.getEntity(); }
    }
}
