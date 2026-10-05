package com.summax.apn.architecture.client.render.model.impl;

import com.summax.apn.architecture.client.render.model.baked.BakedQuadContainerProviderMesh;
import com.summax.apn.architecture.client.render.model.baked.BakedQuadContainerProviderMeshCached;
import com.summax.apn.architecture.client.render.model.baked.IBakedQuadContainer;
import com.summax.apn.architecture.client.render.model.resolver.IModelResolver;
import com.summax.apn.architecture.client.render.model.resolver.IQuadMetadataResolver;
import com.summax.apn.architecture.client.render.model.resolver.functional.FunctionalQuadMetadataResolver;
import com.summax.apn.architecture.common.block.entity.BlockEntityShape;
import com.summax.apn.architecture.common.item.ItemShape;
import com.summax.apn.architecture.common.item.component.ComponentMaterial;
import org.jetbrains.annotations.Nullable;
import com.summax.apn.architecture.common.shape.EnumShape;
import com.summax.apn.architecture.common.shape.ShapeMeshes;
import com.summax.apn.architecture.core.math.ITrans3;
import com.summax.apn.architecture.core.model.mesh.PolygonData;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.concurrent.atomic.AtomicReferenceArray;
import com.summax.apn.architecture.common.shape.mesh.RoofMeshes;
import java.util.Objects;

public class ModelResolverShapeGeneric implements IModelResolver<PolygonData> {

    private static final IQuadMetadataResolver<PolygonData> METADATA_RESOLVER;

    static {
        var builder = FunctionalQuadMetadataResolver.<PolygonData>builder();
        METADATA_RESOLVER = builder.textureResolver(
                d -> getTextureForState(Blocks.OAK_PLANKS.defaultBlockState())
        ).blockTextureResolver(
                (level, pos, state, metadata) -> {
                    // Get the tile entity so we can pull in the material states.
                    var shapeBe = BlockEntityShape.getAt(level, pos);
                    Objects.requireNonNull(shapeBe, "Shape tile entity was null when resolving block texture.");
                    return getTextureForState(shapeBe.getMaterialStateForIndex(metadata.textureIndex()));
                }
        ).itemTextureResolver(
                (stack, metadata) -> {
                    // Pull the shape and material states from the item stack, no secondary states are on the items. They get applied with cladding instead.
                    var shape = ItemShape.getShapeFromStack(stack);
                    var material = ItemShape.getStateFromStack(stack);
                    Objects.requireNonNull(shape, "Shape was null when resolving item texture.");
                    return getTextureForState(material);
                }
        ).tintIndexResolver(
                d -> -1
        ).build();
    }

    private final BakedQuadContainerProviderMesh<String, PolygonData> mesh;
    private final EnumShape shape;
    /**
     * Roofs joining their neighbours: one mesh per connections mask, built on first use.
     */
    private final AtomicReferenceArray<BakedQuadContainerProviderMesh<String, PolygonData>> roofVariants =
            new AtomicReferenceArray<>(16);

    public ModelResolverShapeGeneric(EnumShape shape) {
        this.shape = shape;
        this.mesh = new BakedQuadContainerProviderMeshCached<>(ShapeMeshes.getMesh(shape), BakedQuadContainerProviderMesh.TextureMode.MODEL);
    }

    /**
     * @return the quads of a roof joining the neighbours given by the connections mask (RoofConnections).
     */
    public IBakedQuadContainer getQuads(BlockState base, BlockState secondary, ITrans3 transform, int connections) {
        if (connections == 0)
            return this.mesh.getQuads(null, new MaterialQuadMetadataResolver(base, secondary), transform);
        var variant = this.roofVariants.get(connections);
        if (variant == null) {
            var roof = RoofMeshes.create(this.shape.getName(), connections);
            variant = new BakedQuadContainerProviderMeshCached<>(roof != null ? roof : ShapeMeshes.getMesh(this.shape),
                    BakedQuadContainerProviderMesh.TextureMode.MODEL);
            if (!this.roofVariants.compareAndSet(connections, null, variant))
                variant = this.roofVariants.get(connections);
        }
        return variant.getQuads(null, new MaterialQuadMetadataResolver(base, secondary), transform);
    }

    private static TextureAtlasSprite getTextureForState(BlockState state) {
        return MaterialQuadMetadataResolver.getTextureForState(state);
    }

    @Override
    public IQuadMetadataResolver<PolygonData> getMetadataResolver() {
        return METADATA_RESOLVER;
    }

    @Override
    public IBakedQuadContainer getQuads(LevelAccessor level, BlockPos pos, BlockState state,
                                        IQuadMetadataResolver<PolygonData> resolver, ITrans3 transform) {
        return mesh.getQuads(null, resolver, transform);
    }

    @Override
    public IBakedQuadContainer getQuads(LevelAccessor level, BlockPos pos, BlockState state,
                                        @Nullable BlockState base, @Nullable BlockState secondary, ITrans3 transform) {
        if (base == null)
            return this.getQuads(level, pos, state, transform);
        return this.mesh.getQuads(null, new MaterialQuadMetadataResolver(base, secondary == null ? base : secondary), transform);
    }

    @Override
    public IBakedQuadContainer getQuads(ItemStack stack, IQuadMetadataResolver<PolygonData> resolver, ITrans3 transform) {
        var material = ComponentMaterial.get(stack);
        return this.mesh.getQuads(null, new MaterialQuadMetadataResolver(material.safeBase(), material.safeSecondary()), transform);
    }

    @Override
    public TextureAtlasSprite getDefaultSprite() {
        return ModelResolverShapeGeneric.getTextureForState(Blocks.OAK_PLANKS.defaultBlockState());
    }
}
