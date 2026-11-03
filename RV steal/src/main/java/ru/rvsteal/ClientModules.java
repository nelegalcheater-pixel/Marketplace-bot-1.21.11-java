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
    static LivingEntity target;
    private static int deathTicks, totemCooldown;
    private static Double previousGamma;

    static void tick(Minecraft client) {
        target = null;
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
        if (totemCooldown > 0) totemCooldown--;
        if (ModuleSettings.autoTotem && totemCooldown == 0 && player.containerMenu == player.inventoryMenu
                && player.inventoryMenu.getCarried().isEmpty() && !player.isUsingItem()
                && !player.getOffhandItem().is(net.minecraft.world.item.Items.TOTEM_OF_UNDYING)) {
            for (int i = 0; i < 36; i++) if (player.getInventory().getItem(i).is(net.minecraft.world.item.Items.TOTEM_OF_UNDYING)) {
                client.gameMode.handleInventoryMouseClick(player.inventoryMenu.containerId, i < 9 ? i + 36 : i,
                        40, net.minecraft.world.inventory.ClickType.SWAP, player);
                totemCooldown = 10; break;
            }
        }
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
        if (player.isUsingItem() || client.gameMode.isDestroying()) return;

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
            boolean aura = target instanceof Player ? RvSteal.playerAura : RvSteal.mobAura;
            boolean legit = target instanceof Player ? ModuleSettings.playerLegit : ModuleSettings.mobLegit;
            if (aura && legit) {
                var offset = target.getEyePosition().subtract(player.getEyePosition());
                float yaw = (float) Math.toDegrees(Math.atan2(offset.z, offset.x)) - 90;
                float pitch = (float) -Math.toDegrees(Math.atan2(offset.y, Math.sqrt(offset.x * offset.x + offset.z * offset.z)));
                float yawStep = net.minecraft.util.Mth.wrapDegrees(yaw - player.getYRot());
                float pitchStep = pitch - player.getXRot();
                player.setYRot(player.getYRot() + Math.clamp(yawStep, -10f, 10f));
                player.setXRot(Math.clamp(player.getXRot() + Math.clamp(pitchStep, -8f, 8f), -90f, 90f));
                if (Math.abs(yawStep) > 12 || Math.abs(pitchStep) > 10) return;
            }
            if (player.getAttackStrengthScale(0) < 1) return;
            client.gameMode.attack(player, target);
            player.swing(InteractionHand.MAIN_HAND);
        }
    }
}
