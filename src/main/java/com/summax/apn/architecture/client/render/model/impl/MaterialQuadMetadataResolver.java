package com.summax.apn.architecture.client.render.model.impl;

import com.google.common.collect.Maps;
import com.summax.apn.architecture.client.render.model.resolver.IQuadMetadataResolver;
import com.summax.apn.architecture.core.model.mesh.PolygonData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Resolves face textures from the base (indices 0 and 1) and secondary (2 and 3) materials.
 *
 * @param base      the base material.
 * @param secondary the secondary material.
 */
public record MaterialQuadMetadataResolver(@NotNull BlockState base,
                                           @NotNull BlockState secondary) implements IQuadMetadataResolver<PolygonData> {

    /**
     * Added to the material's tint index on faces of the secondary material, so the block colour handler knows which
     * material to colour them like.
     */
    public static final int SECONDARY_TINT_OFFSET = 100;

    private record FaceTexture(TextureAtlasSprite sprite, int tintIndex) {
    }

    private static final Map<BlockState, TextureAtlasSprite> TEXTURE_CACHE = Maps.newConcurrentMap();
    /**
     * Per material, the texture of each side indexed by Direction ordinal.
     */
    private static final Map<BlockState, FaceTexture[]> FACE_CACHE = Maps.newConcurrentMap();

    /**
     * Clears the cached material textures, must be called when the texture atlas is reloaded.
     */
    public static void clearCache() {
        TEXTURE_CACHE.clear();
        FACE_CACHE.clear();
    }

    public static TextureAtlasSprite getTextureForState(BlockState state) {
        return TEXTURE_CACHE.computeIfAbsent(state, s -> Minecraft.getInstance().getBlockRenderer()
                .getBlockModelShaper().getBlockModel(s).getParticleIcon());
    }

    private static FaceTexture faceTexture(BlockState state, Direction side) {
        return FACE_CACHE.computeIfAbsent(state, MaterialQuadMetadataResolver::readFaceTextures)[side.ordinal()];
    }

    /**
     * Takes the first quad the material's model draws on each side: its base layer, before any overlay.
     */
    private static FaceTexture[] readFaceTextures(BlockState state) {
        var model = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getBlockModel(state);
        var random = RandomSource.create(42L);
        var general = model.getQuads(state, null, random, ModelData.EMPTY, null);
        var faces = new FaceTexture[Direction.values().length];
        for (var side : Direction.values()) {
            random.setSeed(42L);
            BakedQuad quad = model.getQuads(state, side, random, ModelData.EMPTY, null).stream().findFirst()
                    .orElseGet(() -> general.stream().filter(q -> q.getDirection() == side).findFirst().orElse(null));
            faces[side.ordinal()] = quad != null
                    ? new FaceTexture(quad.getSprite(), quad.getTintIndex())
                    : new FaceTexture(getTextureForState(state), -1);
        }
        return faces;
    }

    private static boolean isSecondary(PolygonData metadata) {
        return metadata.textureIndex() >= 2;
    }

    @Override
    public TextureAtlasSprite getTexture(PolygonData metadata) {
        var material = isSecondary(metadata) ? this.secondary : this.base;
        return faceTexture(material, metadata.face().toDirection()).sprite();
    }

    @Override
    public int getTintIndex(PolygonData metadata) {
        var secondary = isSecondary(metadata);
        var tint = faceTexture(secondary ? this.secondary : this.base, metadata.face().toDirection()).tintIndex();
        if (tint < 0)
            return -1;
        return secondary ? tint + SECONDARY_TINT_OFFSET : tint;
    }
}
