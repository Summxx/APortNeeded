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

package com.summax.apn.architecture.client.proxy;

import com.summax.apn.architecture.client.render.ShapePlacementPreview;
import com.summax.apn.architecture.client.test.AutoTest;
import com.summax.apn.architecture.client.render.model.geometry.ArchitectureGeometryLoader;
import com.summax.apn.architecture.client.render.model.geometry.ArchitectureShapeGeometryLoader;
import com.summax.apn.architecture.client.render.model.impl.BakedModelSawbench;
import com.summax.apn.architecture.client.render.model.impl.MaterialQuadMetadataResolver;
import com.summax.apn.architecture.common.proxy.CommonProxy;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import com.summax.apn.architecture.common.ArchitectureMod;
import com.summax.apn.architecture.common.block.entity.BlockEntityShape;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Item;
import com.summax.apn.architecture.common.item.component.ComponentMaterial;
import net.minecraftforge.common.MinecraftForge;

public class ClientProxy extends CommonProxy {

    @Override
    public void setup(FMLCommonSetupEvent e) {
        super.setup(e);
        MinecraftForge.EVENT_BUS.register(ShapePlacementPreview.class);
        if (AutoTest.isEnabled())
            MinecraftForge.EVENT_BUS.register(AutoTest.class);
    }

    @SubscribeEvent
    public void onModelRegistryEvent(ModelEvent.RegisterGeometryLoaders e) {
        e.register("sawbench_loader", new ArchitectureGeometryLoader(
                () -> (context, baker, spriteGetter, modelState, overrides, modelLocation) -> new BakedModelSawbench(context.getTransforms())
        ));
        e.register("shape_loader", new ArchitectureShapeGeometryLoader());
    }

    /**
     * Colours tinted faces like their material (grass, leaves...).
     */
    @SubscribeEvent
    public void onRegisterBlockColors(RegisterColorHandlersEvent.Block e) {
        var blocks = ArchitectureMod.CONTENT.blockShapes.values().toArray(new Block[0]);
        e.register((state, level, pos, tintIndex) -> {
            if (level == null || pos == null || tintIndex < 0
                    || !(level.getBlockEntity(pos) instanceof BlockEntityShape shape))
                return -1;
            boolean secondary = tintIndex >= MaterialQuadMetadataResolver.SECONDARY_TINT_OFFSET;
            var material = secondary ? shape.getEffectiveSecondaryMaterialState() : shape.getEffectiveBaseMaterialState();
            return e.getBlockColors().getColor(material, level, pos,
                    secondary ? tintIndex - MaterialQuadMetadataResolver.SECONDARY_TINT_OFFSET : tintIndex);
        }, blocks);
    }

    /**
     * Shapes in hand take the default colour of their tinted material (grass, leaves), like the material's own item.
     */
    @SubscribeEvent
    public void onRegisterItemColors(RegisterColorHandlersEvent.Item e) {
        var items = ArchitectureMod.CONTENT.itemShapes.values().toArray(new Item[0]);
        e.register((stack, tintIndex) -> {
            if (tintIndex < 0)
                return -1;
            var material = ComponentMaterial.get(stack);
            boolean secondary = tintIndex >= MaterialQuadMetadataResolver.SECONDARY_TINT_OFFSET;
            var state = secondary ? material.safeSecondary() : material.safeBase();
            try {
                return e.getBlockColors().getColor(state, null, null,
                        secondary ? tintIndex - MaterialQuadMetadataResolver.SECONDARY_TINT_OFFSET : tintIndex);
            } catch (RuntimeException ignored) {
                // Some colour handlers need a level, fall back to no tint.
                return -1;
            }
        }, items);
    }

    @SubscribeEvent
    public void onModelBakingCompleted(ModelEvent.BakingCompleted e) {
        // Material sprites are cached per block state, they become stale whenever the atlas is rebuilt.
        MaterialQuadMetadataResolver.clearCache();
        ShapePlacementPreview.clearCache();
    }

    @Override
    public void registerHandlers() {
        super.registerHandlers();
    }

}
