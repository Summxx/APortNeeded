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

import com.summax.apn.architecture.common.block.BlockShape;
import com.summax.apn.architecture.common.block.state.BlockStateShape;
import com.summax.apn.architecture.common.shape.placement.ShapePlacementLogicSideTurn;
import net.minecraft.world.level.block.Block;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public class ItemHammer extends Item {

    public ItemHammer(ResourceLocation id) {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var world = context.getLevel();
        var pos = context.getClickedPos();
        var player = context.getPlayer();
        var state = world.getBlockState(pos);
        if (state instanceof BlockStateShape shapeState && state.getBlock() instanceof BlockShape<?> block
                && block.getPlacementLogic() instanceof ShapePlacementLogicSideTurn logic) {
            if (!world.isClientSide()) {
                var orientation = logic.hammered(shapeState.getOrientation(), player != null && player.isShiftKeyDown());
                // Same block: the block entity and its materials stay.
                world.setBlock(pos, orientation.applyToState(state), Block.UPDATE_ALL);
            }
            return InteractionResult.sidedSuccess(world.isClientSide());
        }
        return InteractionResult.FAIL;
    }

}
