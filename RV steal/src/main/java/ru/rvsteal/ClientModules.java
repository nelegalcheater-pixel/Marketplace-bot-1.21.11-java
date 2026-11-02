package ru.rvsteal;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

final class ClientModules {
    private static int deathTicks;
    private static Double previousGamma;

    static void tick(Minecraft client) {
        if (RvSteal.bright && previousGamma == null) {
            previousGamma = client.options.gamma().get();
            client.options.gamma().set(1.0);
        } else if (!RvSteal.bright && previousGamma != null) {
            if (client.options.gamma().get().equals(1.0)) client.options.gamma().set(previousGamma);
            previousGamma = null;
        }
        var player = client.player;
        if (player == null || client.level == null) { deathTicks = 0; return; }
        if (RvSteal.autoRespawn && client.screen instanceof DeathScreen) {
            if (++deathTicks == 20) player.respawn();
        } else deathTicks = 0;
        if (client.screen != null || !player.isAlive() || player.isSpectator()) return;
        if (RvSteal.autoSprint && player.input.keyPresses.forward() && !player.input.keyPresses.backward()
                && !player.isShiftKeyDown() && !player.isUsingItem() && !player.horizontalCollision
                && !player.hasEffect(MobEffects.BLINDNESS)
                && (player.getFoodData().getFoodLevel() > 6 || player.getAbilities().mayfly)) player.setSprinting(true);
        if (RvSteal.bunnyHop && player.onGround() && player.input.keyPresses.forward()
                && !player.isShiftKeyDown() && !player.isPassenger()) player.jumpFromGround();
        if (client.gameMode == null) return;
        if (RvSteal.autoTool && client.options.keyAttack.isDown() && client.hitResult instanceof BlockHitResult hit
                && !player.isUsingItem()) {
            var state = client.level.getBlockState(hit.getBlockPos());
            var inventory = player.getInventory();
            int best = inventory.getSelectedSlot();
            float speed = inventory.getItem(best).getDestroySpeed(state);
            for (int i = 0; i < 9; i++) {
                float candidate = inventory.getItem(i).getDestroySpeed(state);
                if (candidate > speed) { speed = candidate; best = i; }
            }
            inventory.setSelectedSlot(best);
        }
        if (player.isUsingItem() || client.gameMode.isDestroying() || player.getAttackStrengthScale(0) < 1) return;
        LivingEntity target = null;
        if (client.crosshairPickEntity instanceof LivingEntity aimed
                && ((RvSteal.attackPlayers && aimed instanceof Player) || (RvSteal.attackMobs && aimed instanceof Enemy))) target = aimed;
        if (target == null && (RvSteal.playerAura || RvSteal.mobAura)) {
            double closest = player.entityInteractionRange() * player.entityInteractionRange();
            for (var entity : client.level.entitiesForRendering()) {
                if (!(entity instanceof LivingEntity candidate) || candidate == player
                        || !candidate.isAlive() || !player.canAttack(candidate) || !player.hasLineOfSight(candidate)
                        || !((RvSteal.playerAura && candidate instanceof Player) || (RvSteal.mobAura && candidate instanceof Enemy))) continue;
                double distance = player.distanceToSqr(candidate);
                if (distance <= closest) { closest = distance; target = candidate; }
            }
        }
        if (target != null && target != player && target.isAlive() && player.canAttack(target) && player.hasLineOfSight(target)
                && player.distanceToSqr(target) <= player.entityInteractionRange() * player.entityInteractionRange()) {
            client.gameMode.attack(player, target);
            player.swing(InteractionHand.MAIN_HAND);
        }
    }
}
