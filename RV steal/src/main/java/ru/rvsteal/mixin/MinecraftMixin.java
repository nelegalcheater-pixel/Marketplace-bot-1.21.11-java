package ru.rvsteal.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.rvsteal.RvSteal;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void killerkc2$esp(Entity entity, CallbackInfoReturnable<Boolean> callback) {
        Minecraft client = (Minecraft) (Object) this;
        if (entity != client.player && entity.isAlive()
                && ((RvSteal.playerEsp && entity instanceof Player)
                    || (RvSteal.mobEsp && entity instanceof Enemy))) callback.setReturnValue(true);
    }
}
