package ru.rvsteal.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.world.entity.item.ItemEntity;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.rvsteal.RvSteal;
import java.util.WeakHashMap;

@Mixin(ItemEntityRenderer.class)
public class ItemPhysicsMixin {
    @Unique private static final WeakHashMap<ItemEntityRenderState, Boolean> killerkc2$grounded = new WeakHashMap<>();
    @Unique private boolean killerkc2$lying;
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;F)V", at = @At("TAIL"))
    private void killerkc2$state(ItemEntity entity, ItemEntityRenderState state, float delta, CallbackInfo ci) {
        killerkc2$grounded.put(state, entity.onGround());
    }
    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V", at = @At("HEAD"))
    private void killerkc2$submit(ItemEntityRenderState state, PoseStack pose, net.minecraft.client.renderer.SubmitNodeCollector nodes, net.minecraft.client.renderer.state.CameraRenderState camera, CallbackInfo ci) {
        killerkc2$lying = RvSteal.itemPhysicsEnabled() && killerkc2$grounded.getOrDefault(state, false);
    }
    @Redirect(method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V", ordinal = 0))
    private void killerkc2$translate(PoseStack pose, float x, float y, float z) { pose.translate(x, killerkc2$lying ? 0.13f : y, z); }
    @Redirect(method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionfc;)V", ordinal = 0))
    private void killerkc2$rotate(PoseStack pose, Quaternionfc rotation) { pose.mulPose(killerkc2$lying ? new Quaternionf().rotateX((float)Math.PI / 2) : rotation); }
}
