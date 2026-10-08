package com.summax.apn.architecture.common.compat;

import com.summax.apn.architecture.common.block.BlockShape;
import com.summax.apn.architecture.common.block.entity.BlockEntityShape;
import com.summax.apn.architecture.common.item.ItemShape;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.event.level.BlockEvent;

/**
 * Takes the material from the player's stack when Effortless Building places a shape without the item's tag.
 */
public final class HeldShapeMaterial {

    private HeldShapeMaterial() {
    }

    public static void onEntityPlace(BlockEvent.EntityPlaceEvent event) {
        if (!EffortlessCompat.isPlacing() || !(event.getEntity() instanceof Player player) || event.getLevel().isClientSide())
            return;
        if (event instanceof BlockEvent.EntityMultiPlaceEvent multi) {
            for (var snapshot : multi.getReplacedBlockSnapshots())
                apply(player, event.getLevel(), snapshot.getPos());
        } else {
            apply(player, event.getLevel(), event.getPos());
        }
    }

    private static void apply(Player player, LevelAccessor level, BlockPos pos) {
        // Shapes placed from their item already have a material.
        if (!(level.getBlockEntity(pos) instanceof BlockEntityShape shape) || shape.hasBaseMaterial()
                || !(shape.getBlockState().getBlock() instanceof BlockShape<?> block))
            return;
        var stack = findStack(player, block.asItem());
        if (!stack.isEmpty())
            shape.setBaseMaterialState(ItemShape.getStateFromStack(stack));
    }

    /**
     * Held stacks first, then the rest of the inventory, like the tools that take items from it.
     */
    private static ItemStack findStack(Player player, Item item) {
        for (var hand : InteractionHand.values()) {
            var stack = player.getItemInHand(hand);
            if (stack.getItem() == item)
                return stack;
        }
        for (var stack : player.getInventory().items) {
            if (stack.getItem() == item)
                return stack;
        }
        return ItemStack.EMPTY;
    }
}
