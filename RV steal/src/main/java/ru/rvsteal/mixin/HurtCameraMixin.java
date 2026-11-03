package ru.rvsteal.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.rvsteal.RvSteal;

@Mixin(GameRenderer.class)
public class HurtCameraMixin {
    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void killerkc2$noHurt(PoseStack pose, float delta, CallbackInfo callback) {
        if (RvSteal.noHurtCamEnabled()) callback.cancel();
    }
}
