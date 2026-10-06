package com.summax.apn.mixin;

import com.summax.apn.architecture.common.compat.BuildingGadgetsCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.UUID;

@Pseudo
@Mixin(targets = "com.direwolf20.buildinggadgets2.util.BuildingUtils", remap = false)
public abstract class BuildingUtilsMixin {

    @Inject(method = "build", at = @At("HEAD"), require = 0)
    private static void apn$pasteShapeMaterials(Level level, Player player, ArrayList<?> list, BlockPos lookingAt, ItemStack gadget, boolean needItems,
                                                CallbackInfoReturnable<UUID> cir) {
        BuildingGadgetsCompat.onPaste(level, gadget, lookingAt);
    }

    @Inject(method = "exchange", at = @At("HEAD"), require = 0)
    private static void apn$exchangeShapeMaterials(Level level, Player player, ArrayList<?> list, BlockPos lookingAt, ItemStack gadget, boolean needItems,
                                                   boolean returnItems, CallbackInfoReturnable<UUID> cir) {
        BuildingGadgetsCompat.onPaste(level, gadget, lookingAt);
    }
}
