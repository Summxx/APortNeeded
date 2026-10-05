package com.summax.apn.architecture.client.render.model.baked;

import com.google.common.collect.Maps;
import com.summax.apn.architecture.client.render.model.resolver.IQuadMetadataResolver;
import com.summax.apn.architecture.core.math.ITrans3;
import com.summax.apn.architecture.core.math.ITrans3Immutable;
import com.summax.apn.architecture.core.model.mesh.IMesh;
import com.summax.apn.architecture.core.model.mesh.IPolygon;
import com.summax.apn.architecture.core.model.mesh.IPolygonData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.pipeline.QuadBakingVertexConsumer;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Direction;
import org.joml.Vector3f;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A baked quad container provider that uses a mesh to generate the quads.
 *
 * @param <I> The type of the part id.
 * @param <D> The type of the polygon data.
 */
public class BakedQuadContainerProviderMesh<I, D extends IPolygonData<D>> implements IMultipartBakedQuadContainerProvider<I, D> {

    /**
     * How faces get their texture coordinates.
     */
    public enum TextureMode {
        /**
         * Every face projects the texture from its position, like a vanilla block.
         */
        PROJECTED,
        /**
         * Odd texture indices are projected, the others keep the model's UVs.
         */
        MODEL
    }

    private final IMesh<I, D> mesh;
    private final TextureMode textureMode;
    private final Map<ITrans3Immutable, IMesh<I, D>> cache;

    public BakedQuadContainerProviderMesh(IMesh<I, D> mesh, TextureMode textureMode) {
        this.mesh = mesh;
        this.textureMode = textureMode;
        // Chunk building is multithreaded, so we need a concurrent map here.
        this.cache = Maps.newConcurrentMap();
        this.cache.put(ITrans3Immutable.IDENTITY, mesh);
    }

    private IMesh<I, D> getMesh(ITrans3Immutable transform) {
        // We don't rotate the UVs, models keep their own.
        return this.cache.computeIfAbsent(transform, t -> this.mesh.transform(t, false));
    }

    @Override
    public IBakedQuadContainer getQuads(@Nullable I partId, LevelAccessor level, BlockPos pos, BlockState state, IQuadMetadataResolver<D> metadataResolver, ITrans3 transform, boolean forceRebuild) {
        return this.getQuadsTakesAll(partId, level, pos, state, null, metadataResolver, transform, forceRebuild);
    }

    @Override
    public IBakedQuadContainer getQuads(@Nullable I partId, ItemStack stack, IQuadMetadataResolver<D> metadataResolver, ITrans3 transform, boolean forceRebuild) {
        return this.getQuadsTakesAll(partId, null, null, null, stack, metadataResolver, transform, forceRebuild);
    }

    @Override
    public IBakedQuadContainer getQuads(@Nullable I partId, IQuadMetadataResolver<D> metadataResolver, ITrans3 transform, boolean forceRebuild) {
        return this.getQuadsTakesAll(partId, null, null, null, null, metadataResolver, transform, forceRebuild);
    }

    private final IBakedQuadContainer getQuadsTakesAll(@Nullable I partId, @Nullable LevelAccessor level, @Nullable BlockPos pos, @Nullable BlockState state, @Nullable ItemStack stack, IQuadMetadataResolver<D> metadataResolver, ITrans3 transform, boolean forceRebuild) {
        // We ignore forceRebuild here, as we're not actually caching the resulting containers themselves.
        var containerBuilder = new BakedQuadContainer.Builder();
        // Cull by the mesh's cull face, the quad direction is only used for lighting.
        final var cullDirection = new AtomicReference<Direction>();
        var quadBaker = new QuadBakingVertexConsumer(bakedQuad -> containerBuilder.addForDirection(bakedQuad, cullDirection.get()));
        var m = this.getMesh(transform.asImmutable());
        var faces = partId == null ? m.getFaces() : Objects.requireNonNull(m.getPart(partId), "Unable to find part with id: " + partId + " on mesh: " + this.mesh.getName()).getFaces();
        for (var face : faces) {
            for (var polygon : face.getPolygons()) {
                var polygonData = polygon.getPolygonData();
                cullDirection.set(polygonData.cullFace().toDirection());
                var normal = computeNormal(polygon);
                var direction = this.textureMode == TextureMode.MODEL && polygonData.face() != null
                        ? polygonData.face().toDirection()
                        : Direction.getNearest(normal.x(), normal.y(), normal.z());
                var texture = metadataResolver.getTexture(level, pos, state, stack, polygonData);
                var tintIndex = metadataResolver.getTintIndex(level, pos, state, stack, polygonData);
                var vertexCount = polygon.getVertexCount();
                quadBaker.setSprite(texture);
                quadBaker.setTintIndex(tintIndex);
                quadBaker.setDirection(direction);
                quadBaker.setShade(true);
                quadBaker.setHasAmbientOcclusion(true);
                boolean projected = this.textureMode == TextureMode.PROJECTED || (polygonData.textureIndex() & 1) != 0;
                var uvs = projected ? projectedUVs(polygon, direction) : null;
                var startIndex = 0;
                if (polygon.getVertexCount() == 3) {
                    startIndex = -1;
                }
                for (int i = startIndex; i < vertexCount; i++) {
                    var vertexIndex = Math.max(0, i);
                    var v = polygon.getVertex(vertexIndex);
                    double u = uvs != null ? uvs[vertexIndex * 2] : v.getU();
                    double uvV = uvs != null ? uvs[vertexIndex * 2 + 1] : v.getV();
                    quadBaker.vertex(v.getX(), v.getY(), v.getZ())
                            .color(-1)
                            .normal(normal.x(), normal.y(), normal.z())
                            .uv(texture.getU(u * 16F), texture.getV(uvV * 16F))
                            .uv2(1, 0)
                            .overlayCoords(1, 0)
                            .endVertex();
                }
            }
        }
        return containerBuilder.build();
    }

    /**
     * Computes the normal of a planar polygon from its vertex positions with Newell's method.
     */
    private static Vector3f computeNormal(IPolygon<?> polygon) {
        float nx = 0, ny = 0, nz = 0;
        int count = polygon.getVertexCount();
        for (int i = 0; i < count; i++) {
            var a = polygon.getVertex(i);
            var b = polygon.getVertex((i + 1) % count);
            nx += (float) ((a.getY() - b.getY()) * (a.getZ() + b.getZ()));
            ny += (float) ((a.getZ() - b.getZ()) * (a.getX() + b.getX()));
            nz += (float) ((a.getX() - b.getX()) * (a.getY() + b.getY()));
        }
        var normal = new Vector3f(nx, ny, nz);
        return normal.lengthSquared() > 0 ? normal.normalize() : new Vector3f(0, 1, 0);
    }

    /**
     * Projects the texture onto the polygon like vanilla blocks do.
     *
     * @return u, v pairs in [0, 1] per vertex, or null to keep the mesh UVs when the polygon spans more than a block.
     */
    private static double[] projectedUVs(IPolygon<?> polygon, Direction direction) {
        int count = polygon.getVertexCount();
        var uvs = new double[count * 2];
        double minU = Double.MAX_VALUE, minV = Double.MAX_VALUE, maxU = -Double.MAX_VALUE, maxV = -Double.MAX_VALUE;
        for (int i = 0; i < count; i++) {
            var v = polygon.getVertex(i);
            double x = v.getX(), y = v.getY(), z = v.getZ();
            double u, w;
            switch (direction) {
                case UP -> { u = x; w = z; }
                case DOWN -> { u = x; w = 1 - z; }
                case NORTH -> { u = 1 - x; w = 1 - y; }
                case SOUTH -> { u = x; w = 1 - y; }
                case WEST -> { u = z; w = 1 - y; }
                default -> { u = 1 - z; w = 1 - y; }
            }
            uvs[i * 2] = u;
            uvs[i * 2 + 1] = w;
            minU = Math.min(minU, u);
            minV = Math.min(minV, w);
            maxU = Math.max(maxU, u);
            maxV = Math.max(maxV, w);
        }
        // Shift parts sticking out of the block back into the texture so we never sample outside the sprite.
        double shiftU = minU < -1e-4 || maxU > 1 + 1e-4 ? -Math.floor(minU + 1e-4) : 0;
        double shiftV = minV < -1e-4 || maxV > 1 + 1e-4 ? -Math.floor(minV + 1e-4) : 0;
        if (maxU + shiftU > 1 + 1e-4 || maxV + shiftV > 1 + 1e-4)
            return null;
        for (int i = 0; i < count; i++) {
            uvs[i * 2] += shiftU;
            uvs[i * 2 + 1] += shiftV;
        }
        return uvs;
    }

}
