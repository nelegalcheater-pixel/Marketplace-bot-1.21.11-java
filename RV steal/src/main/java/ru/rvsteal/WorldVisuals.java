package ru.rvsteal;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributes;

/** Client rendering only: never alters server time, weather or biome data. */
public final class WorldVisuals {
    private WorldVisuals() {}
    public static Integer color(EnvironmentAttribute<?> attribute) {
        if (!AccessKey.unlocked() || !ModuleSettings.worldTheme) return null;
        if (attribute == EnvironmentAttributes.SKY_COLOR) return ModuleSettings.skyColor;
        if (attribute == EnvironmentAttributes.FOG_COLOR) return ModuleSettings.fogColor;
        if (attribute == EnvironmentAttributes.CLOUD_COLOR) return 0xff000000 | ModuleSettings.cloudColor;
        return null;
    }
    static void tick(Minecraft client) {
        if (!ModuleSettings.ambientParticles || client.player == null || client.level == null || client.isPaused()) return;
        var random = client.level.random;
        var player = client.player;
        var particle = switch (ModuleSettings.particleStyle) {
            case 1 -> ParticleTypes.CHERRY_LEAVES;
            case 2 -> ParticleTypes.FIREFLY;
            default -> ParticleTypes.END_ROD;
        };
        for (int i = 0; i < ModuleSettings.particleDensity; i++) {
            client.level.addParticle(particle, player.getX() + (random.nextDouble() - .5) * 18,
                    player.getY() + random.nextDouble() * 6, player.getZ() + (random.nextDouble() - .5) * 18,
                    0, -.005, 0);
        }
    }
}
