package ru.rvsteal.mixin;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.rvsteal.RvSteal;
@Mixin(LightTexture.class)
public class NightVisionMixin {
    @Redirect(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;hasEffect(Lnet/minecraft/core/Holder;)Z"))
    private boolean killerkc2$vision(LocalPlayer player, Holder<MobEffect> effect) {
        return (RvSteal.nightVisionEnabled() && effect == MobEffects.NIGHT_VISION) || player.hasEffect(effect);
    }
}
