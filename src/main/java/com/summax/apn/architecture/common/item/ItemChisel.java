/*
 * MIT License
 *
 * Copyright (c) 2017 Benjamin K
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.summax.apn.architecture.common.item;

import com.summax.apn.architecture.common.block.entity.BlockEntityShape;
import com.summax.apn.architecture.common.ArchitectureMod;
import com.summax.apn.architecture.common.block.BlockShape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class ItemChisel extends Item {

    public ItemChisel(ResourceLocation id) {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var world = context.getLevel();
        var pos = context.getClickedPos();
        var player = context.getPlayer();
        var face = context.getClickedFace();
        if (world.getBlockEntity(pos) instanceof BlockEntityShape shape) {
            if (!world.isClientSide())
                onChiselUse(shape, player, face, context.getClickLocation().subtract(Vec3.atCenterOf(pos)));
            return InteractionResult.sidedSuccess(world.isClientSide());
        }
        var state = world.getBlockState(pos);
        var block = state.getBlock();
        if ((block == Blocks.GLASS) || (block == Blocks.GLASS_PANE)
                || (block == Blocks.GLOWSTONE) || (block == Blocks.ICE)) {
            world.setBlock(pos, Blocks.AIR.defaultBlockState(), 0x3);
            if (!world.isClientSide()) {
                this.dropBlockAsItem(world, pos, state);
                world.levelEvent(2001, pos, Block.getId(Blocks.STONE.defaultBlockState())); // block breaking sound and particles
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }

    /**
     * Toggles the connection on the clicked edge, or removes the cladding when the centre is clicked.
     */
    private static void onChiselUse(BlockEntityShape shape, @Nullable Player player, Direction face, Vec3 hit) {
        var side = zoneHit(face, hit);
        if (side != null) {
            shape.toggleConnection(side);
            return;
        }
        var cladding = shape.getSecondaryMaterialState();
        if (cladding.isEmpty() || !(shape.getBlockState().getBlock() instanceof BlockShape<?> block)
                || !block.getShape().acceptsCladding())
            return;
        if (player == null || !player.isCreative())
            Block.popResource(shape.getLevel(), shape.getBlockPos(),
                    ArchitectureMod.CONTENT.itemCladding.newStack(cladding.get(), 1));
        shape.setSecondaryMaterialState(null);
    }

    /**
     * Gets the side whose edge zone contains the hit, null for the centre.
     */
    @Nullable
    private static Direction zoneHit(Direction face, Vec3 hit) {
        double r = 0.5 - 1 / 4d;
        if (hit.x <= -r && face != Direction.WEST) return Direction.WEST;
        if (hit.x >= r && face != Direction.EAST) return Direction.EAST;
        if (hit.y <= -r && face != Direction.DOWN) return Direction.DOWN;
        if (hit.y >= r && face != Direction.UP) return Direction.UP;
        if (hit.z <= -r && face != Direction.NORTH) return Direction.NORTH;
        if (hit.z >= r && face != Direction.SOUTH) return Direction.SOUTH;
        return null;
    }

    private void dropBlockAsItem(Level world, BlockPos pos, BlockState state) {
        ItemStack stack = new ItemStack(state.getBlock());
        Block.popResource(world, pos, stack);
    }

}
