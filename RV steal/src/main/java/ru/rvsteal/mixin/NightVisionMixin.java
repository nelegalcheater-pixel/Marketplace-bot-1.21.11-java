package ru.rvsteal.mixin;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import ru.rvsteal.RvSteal;
@Mixin(LightTexture.class)
public class NightVisionMixin {
    // 1.21.11 uses AmbientLightFactor as the first lightmap uniform. Full ambient
    // lighting also avoids darkness attenuation, without installing fake effects.
    @ModifyArg(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putFloat(F)Lcom/mojang/blaze3d/buffers/Std140Builder;", ordinal = 0), index = 0)
    private float killerkc2$ambient(float original) {
        return RvSteal.nightVisionEnabled() ? 1f : original;
    }
    @Redirect(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;hasEffect(Lnet/minecraft/core/Holder;)Z"))
    private boolean killerkc2$vision(LocalPlayer player, Holder<MobEffect> effect) {
        return (RvSteal.nightVisionEnabled() && effect == MobEffects.NIGHT_VISION) || player.hasEffect(effect);
    }
}
