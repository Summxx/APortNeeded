package com.summax.apn.architecture.client.render.model.impl;

import net.minecraft.client.renderer.RenderType;
import com.summax.apn.architecture.common.block.state.BlockStateArchitecture;
import com.summax.apn.architecture.common.model.ModelProperties;
import com.summax.apn.architecture.common.shape.mesh.RoofConnections;
import com.summax.apn.architecture.common.shape.mesh.RoofMeshes;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import net.minecraft.world.level.block.state.BlockState;
import com.summax.apn.architecture.client.render.model.baked.IModelResolverBaked;
import com.summax.apn.architecture.client.render.model.resolver.IModelResolver;
import com.summax.apn.architecture.common.shape.EnumShape;
import com.summax.apn.architecture.core.model.mesh.PolygonData;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.jetbrains.annotations.NotNull;

public class BakedModelShapeGeneric implements IModelResolverBaked<PolygonData> {

    private final ModelResolverShapeGeneric resolver;
    private final ItemTransforms transforms;

    private final String shapeName;

    public BakedModelShapeGeneric(EnumShape shape, ItemTransforms transforms) {
        this.shapeName = shape.getName();
        this.resolver = new ModelResolverShapeGeneric(shape);
        this.transforms = transforms;
    }

    @Override
    @NotNull
    public ItemTransforms getTransforms() {
        return this.transforms;
    }

    @Override
    public IModelResolver<PolygonData> getModelResolver() {
        return this.resolver;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean useAmbientOcclusion(BlockState state) {
        return this.useAmbientOcclusion();
    }

    @Override
    public boolean useAmbientOcclusion(BlockState state, RenderType renderType) {
        return this.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean usesBlockLight() {
        return true;
    }

    @Override
    public boolean isCustomRenderer() {
        return true;
    }

    @Override
    @NotNull
    public TextureAtlasSprite getParticleIcon() {
        return this.resolver.getDefaultSprite();
    }

    @Override
    @NotNull
    public ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }

    /**
     * Roofs check their neighbours when the chunk is built so they can join ridges and valleys.
     */
    @Override
    @NotNull
    public ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData) {
        if (RoofMeshes.connectionSides(this.shapeName) == null)
            return modelData;
        int connections = RoofConnections.at(level, pos, state);
        return connections == 0 ? modelData : modelData.derive().with(ModelProperties.ROOF_CONNECTIONS, connections).build();
    }

    @Override
    @NotNull
    public List<BakedQuad> getQuads(@Nullable BlockStateArchitecture state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData extraData, @Nullable RenderType renderType) {
        var connections = extraData.get(ModelProperties.ROOF_CONNECTIONS);
        var base = extraData.get(ModelProperties.BASE_MATERIAL);
        if (connections == null || connections == 0 || state == null || base == null)
            return IModelResolverBaked.super.getQuads(state, side, rand, extraData, renderType);
        var secondary = extraData.get(ModelProperties.SECONDARY_MATERIAL);
        return this.resolver.getQuads(base, secondary != null ? secondary : base, state.getTransform(), connections).quadsFor(side);
    }
}
