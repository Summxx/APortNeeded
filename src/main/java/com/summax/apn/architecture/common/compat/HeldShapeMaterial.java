package com.summax.apn.architecture.common.compat;

import com.summax.apn.architecture.common.block.BlockShape;
import com.summax.apn.architecture.common.block.entity.BlockEntityShape;
import com.summax.apn.architecture.common.item.ItemShape;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.level.BlockEvent;

/**
 * Takes the material from the held stack for tools that place shapes without the item's tag.
 */
public final class HeldShapeMaterial {

    private HeldShapeMaterial() {
    }

    public static void onEntityPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof Player player) || !(event.getPlacedBlock().getBlock() instanceof BlockShape<?> block)
                || !(event.getLevel().getBlockEntity(event.getPos()) instanceof BlockEntityShape shape))
            return;
        for (var hand : InteractionHand.values()) {
            var stack = player.getItemInHand(hand);
            if (stack.getItem() == block.asItem()) {
                var material = ItemShape.getStateFromStack(stack);
                if (material != shape.getEffectiveBaseMaterialState())
                    shape.setBaseMaterialState(material);
                return;
            }
        }
    }
}
