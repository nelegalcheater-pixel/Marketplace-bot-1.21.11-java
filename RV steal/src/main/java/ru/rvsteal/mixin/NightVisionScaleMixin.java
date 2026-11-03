package ru.rvsteal.mixin;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.rvsteal.RvSteal;
@Mixin(GameRenderer.class)
public class NightVisionScaleMixin {
    @Inject(method = "getNightVisionScale", at = @At("HEAD"), cancellable = true)
    private static void killerkc2$vision(LivingEntity entity, float delta, CallbackInfoReturnable<Float> callback) {
        if (RvSteal.nightVisionEnabled()) callback.setReturnValue(1f);
    }
}
