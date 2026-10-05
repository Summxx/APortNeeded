package com.summax.apn.architecture.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.summax.apn.architecture.client.render.model.baked.IArchitectureBakedModel;
import com.summax.apn.architecture.common.block.BlockShape;
import com.summax.apn.architecture.common.item.ItemShape;
import com.summax.apn.architecture.common.item.component.ComponentMaterial;
import com.summax.apn.architecture.common.model.ModelProperties;
import net.minecraft.client.Minecraft;
import java.util.List;
import java.util.ArrayList;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Renders a translucent preview of the shape the player is about to place.
 */
public class ShapePlacementPreview {

    private static final float ALPHA = 0.5F;
    private static final RandomSource RANDOM = RandomSource.create();

    private static BlockState cachedState;
    private static ComponentMaterial cachedMaterial;
    private static List<BakedQuad> cachedQuads;

    /**
     * Drops the cached preview, its quads hold sprites that become invalid when resources reload.
     */
    public static void clearCache() {
        cachedState = null;
        cachedMaterial = null;
        cachedQuads = null;
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)
            return;
        var mc = Minecraft.getInstance();
        var player = mc.player;
        var level = mc.level;
        if (player == null || level == null || !(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK)
            return;

        var hand = player.getMainHandItem().getItem() instanceof ItemShape ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof ItemShape item) || !(item.getBlock() instanceof BlockShape<?> block))
            return;

        var context = new BlockPlaceContext(player, hand, stack, hit);
        if (!context.canPlace())
            return;
        var pos = context.getClickedPos();
        var state = block.getStateForPlacement(context);
        if (state == null || !state.canSurvive(level, pos))
            return;
        if (!(mc.getBlockRenderer().getBlockModel(state) instanceof IArchitectureBakedModel model))
            return;

        var material = ComponentMaterial.get(stack);
        // The same preview is drawn every frame while the player doesn't move: reuse its quads.
        if (state != cachedState || !material.equals(cachedMaterial) || cachedQuads == null) {
            var modelData = ModelData.builder()
                    .with(ModelProperties.BASE_MATERIAL, material.safeBase())
                    .with(ModelProperties.SECONDARY_MATERIAL, material.safeSecondary())
                    .build();
            var quads = new ArrayList<BakedQuad>();
            for (var side : Direction.values())
                quads.addAll(model.getQuads(state, side, RANDOM, modelData, null));
            quads.addAll(model.getQuads(state, null, RANDOM, modelData, null));
            cachedState = state;
            cachedMaterial = material;
            cachedQuads = quads;
        }

        var camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);

        var renderType = Sheets.translucentCullBlockSheet();
        var buffers = mc.renderBuffers().bufferSource();
        var consumer = buffers.getBuffer(renderType);
        var light = LevelRenderer.getLightColor(level, pos);
        var pose = poseStack.last();
        for (var quad : cachedQuads) {
            consumer.putBulkData(pose, quad, 1F, 1F, 1F, ALPHA, light, OverlayTexture.NO_OVERLAY, false);
        }
        poseStack.popPose();
        buffers.endBatch(renderType);
    }
}
