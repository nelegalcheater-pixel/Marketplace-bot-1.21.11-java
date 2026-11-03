package ru.rvsteal.mixin;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.rvsteal.Hitboxes;
@Mixin(GameRenderer.class)
public class HitboxPickMixin {
    @Inject(method = "pick", at = @At("TAIL"))
    private void killerkc2$pick(float delta, CallbackInfo ci) { Hitboxes.pick(delta); }
}
