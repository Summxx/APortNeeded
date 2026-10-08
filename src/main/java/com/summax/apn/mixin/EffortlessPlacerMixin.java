package com.summax.apn.mixin;

import com.summax.apn.architecture.common.compat.EffortlessCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "nl.requios.effortlessbuilding.utilities.BlockPlacerHelper", remap = false)
public abstract class EffortlessPlacerMixin {

    @Inject(method = "placeBlock", at = @At("HEAD"), require = 0)
    private static void apn$beginPlace(CallbackInfoReturnable<Boolean> cir) {
        EffortlessCompat.beginPlace();
    }

    @Inject(method = "placeBlock", at = @At("RETURN"), require = 0)
    private static void apn$endPlace(CallbackInfoReturnable<Boolean> cir) {
        EffortlessCompat.endPlace();
    }
}
