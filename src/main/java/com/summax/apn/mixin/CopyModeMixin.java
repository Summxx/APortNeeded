package com.summax.apn.mixin;

import com.summax.apn.architecture.common.compat.BuildingGadgetsCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;

@Pseudo
@Mixin(targets = "com.direwolf20.buildinggadgets2.util.modes.Copy", remap = false)
public abstract class CopyModeMixin {

    @Inject(method = "collectWorld", at = @At("RETURN"), require = 0)
    private void apn$copyShapeMaterials(Direction hitSide, Player player, BlockPos start, BlockState state, CallbackInfoReturnable<ArrayList<?>> cir) {
        BuildingGadgetsCompat.onCopy(player, cir.getReturnValue().isEmpty());
    }
}
