package ru.rvsteal.mixin;

import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeProbe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.rvsteal.WorldVisuals;

@Mixin(EnvironmentAttributeProbe.class)
public class WorldColorMixin {
    @Shadow Level level;
    @Inject(method = "getValue", at = @At("HEAD"), cancellable = true)
    private <T> void killerkc2$color(EnvironmentAttribute<T> attribute, float delta, CallbackInfoReturnable<T> callback) {
        if (level == null || !level.isClientSide()) return;
        Integer color = WorldVisuals.color(attribute);
        // The three attributes handled above all have Integer values.
        if (color != null) {
            @SuppressWarnings("unchecked") T value = (T) color;
            callback.setReturnValue(value);
        }
    }
}
