package ru.rvsteal.mixin;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(GameRenderer.class)
public interface GameRendererAccess {
    @Invoker("getFov") float killerkc2$getFov(Camera camera, float delta, boolean useSetting);
}
