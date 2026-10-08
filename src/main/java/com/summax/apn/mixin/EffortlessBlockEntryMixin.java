package com.summax.apn.mixin;

import com.summax.apn.architecture.common.compat.EffortlessCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "nl.requios.effortlessbuilding.utilities.BlockEntry", remap = false)
public abstract class EffortlessBlockEntryMixin {

    @Inject(method = "setItemAndFindNewBlockState", at = @At("HEAD"), require = 0)
    private void apn$beginFindState(CallbackInfo ci) {
        EffortlessCompat.beginFindState();
    }

    @Inject(method = "setItemAndFindNewBlockState", at = @At("RETURN"), require = 0)
    private void apn$endFindState(CallbackInfo ci) {
        EffortlessCompat.endFindState();
    }
}
