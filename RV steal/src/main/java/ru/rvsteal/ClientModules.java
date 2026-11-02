package ru.rvsteal;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

final class ClientModules {
    private static int deathTicks;

    static void tick(Minecraft client) {
        var player = client.player;
        if (player == null || client.level == null) { deathTicks = 0; return; }
        if (RvSteal.autoRespawn && client.screen instanceof DeathScreen) {
            if (++deathTicks == 20) player.respawn();
        } else deathTicks = 0;
        if (client.screen != null || !player.isAlive() || player.isSpectator()) return;
        if (RvSteal.autoSprint && player.input.keyPresses.forward() && !player.input.keyPresses.backward()
                && !player.isShiftKeyDown() && !player.isUsingItem() && !player.horizontalCollision
                && !player.hasEffect(MobEffects.BLINDNESS)
                && (player.getFoodData().getFoodLevel() > 6 || player.getAbilities().mayfly)) {
            player.setSprinting(true);
        }
        if (client.gameMode == null || player.isUsingItem() || client.gameMode.isDestroying()) return;
        if (!(client.crosshairPickEntity instanceof LivingEntity target) || !target.isAlive()) return;
        boolean selected = (RvSteal.attackPlayers && target instanceof Player)
                || (RvSteal.attackMobs && target instanceof Enemy);
        if (selected && player.canAttack(target) && player.hasLineOfSight(target)
                && player.distanceToSqr(target) <= player.entityInteractionRange() * player.entityInteractionRange()
                && player.getAttackStrengthScale(0) >= 1) {
            client.gameMode.attack(player, target);
            player.swing(InteractionHand.MAIN_HAND);
        }
    }
}
