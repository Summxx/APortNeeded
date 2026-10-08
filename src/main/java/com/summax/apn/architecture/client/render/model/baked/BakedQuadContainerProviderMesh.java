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
    /**
     * Shade sloped faces from their normal instead of their side, used for curved models.
     */
    private final boolean normalShading;
    private final boolean hasSlopedFaces;

    /**
     * @return true if the mesh has faces off the block axes, only those need a ShapeLighting.
     */
    public boolean hasSlopedFaces() {
        return this.hasSlopedFaces;
    }
    private final Map<ITrans3Immutable, IMesh<I, D>> cache;

    public BakedQuadContainerProviderMesh(IMesh<I, D> mesh, TextureMode textureMode) {
        this(mesh, textureMode, false);
    }

    public BakedQuadContainerProviderMesh(IMesh<I, D> mesh, TextureMode textureMode, boolean normalShading) {
        this.mesh = mesh;
        this.textureMode = textureMode;
        this.normalShading = normalShading;
        this.hasSlopedFaces = mesh.getFaces().stream().flatMap(f -> f.getPolygons().stream())
                .anyMatch(polygon -> !isAligned(computeNormal(polygon)));
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
        return this.getQuadsTakesAll(partId, level, pos, state, null, metadataResolver, transform, ShaderPacks.inUse(), null);
    }

    @Override
    public IBakedQuadContainer getQuads(@Nullable I partId, ItemStack stack, IQuadMetadataResolver<D> metadataResolver, ITrans3 transform, boolean forceRebuild) {
        return this.getQuadsTakesAll(partId, null, null, null, stack, metadataResolver, transform, ShaderPacks.inUse(), null);
    }

    @Override
    public IBakedQuadContainer getQuads(@Nullable I partId, IQuadMetadataResolver<D> metadataResolver, ITrans3 transform, boolean forceRebuild) {
        return this.bakeQuads(partId, metadataResolver, transform, ShaderPacks.inUse());
    }

    /**
     * Gets the quads for a placed block, sloped faces get lit from the given lighting.
     */
    public IBakedQuadContainer getQuads(@Nullable I partId, IQuadMetadataResolver<D> metadataResolver, ITrans3 transform, @Nullable ShapeLighting lighting) {
        if (lighting == null || !this.hasSlopedFaces)
            return this.getQuads(partId, metadataResolver, transform, false);
        return this.getQuadsTakesAll(partId, null, null, null, null, metadataResolver, transform, ShaderPacks.inUse(), lighting);
    }

    protected IBakedQuadContainer bakeQuads(@Nullable I partId, IQuadMetadataResolver<D> metadataResolver, ITrans3 transform, boolean shaders) {
        return this.getQuadsTakesAll(partId, null, null, null, null, metadataResolver, transform, shaders, null);
    }

    private final IBakedQuadContainer getQuadsTakesAll(@Nullable I partId, @Nullable LevelAccessor level, @Nullable BlockPos pos, @Nullable BlockState state, @Nullable ItemStack stack, IQuadMetadataResolver<D> metadataResolver, ITrans3 transform, boolean shaders, @Nullable ShapeLighting lighting) {
        var containerBuilder = new BakedQuadContainer.Builder();
        var sample = new float[3];
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
                quadBaker.setSprite(texture);
                quadBaker.setTintIndex(tintIndex);
                quadBaker.setDirection(direction);
                boolean aligned = isAligned(normal);
                boolean lit = lighting != null && !aligned;
                boolean baked = !aligned && !shaders && (lit || this.normalShading);
                float diffuse = baked ? diffuseShade(normal) : 1;
                quadBaker.setShade(!baked && !lit);
                quadBaker.setHasAmbientOcclusion(!baked && !lit);
                boolean projected = this.textureMode == TextureMode.PROJECTED || (polygonData.textureIndex() & 1) != 0;
                var uvs = projected ? projectedUVs(polygon, direction) : null;
                var order = vertexOrder(polygon, aligned ? direction : null);
                for (int i = 0; i < 4; i++) {
                    var vertexIndex = order[i];
                    var v = polygon.getVertex(vertexIndex);
                    double u = uvs != null ? uvs[vertexIndex * 2] : v.getU();
                    double uvV = uvs != null ? uvs[vertexIndex * 2 + 1] : v.getV();
                    int blockLight = 0, skyLight = 0;
                    float brightness = diffuse;
                    if (lit) {
                        lighting.sample(v.getX(), v.getY(), v.getZ(), normal.x(), normal.y(), normal.z(), sample);
                        blockLight = Math.min(240, Math.round(sample[0] * 16));
                        skyLight = Math.min(240, Math.round(sample[1] * 16));
                        if (!shaders)
                            brightness *= sample[2];
                    }
                    int shade = Math.round(255 * brightness);
                    quadBaker.vertex(v.getX(), v.getY(), v.getZ())
                            .color(shade, shade, shade, 255)
                            .normal(normal.x(), normal.y(), normal.z())
                            .uv(texture.getU(u * 16F), texture.getV(uvV * 16F))
                            .uv2(blockLight, skyLight)
                            .overlayCoords(1, 0)
                            .endVertex();
                }
            }
        }
        return containerBuilder.build();
    }

    // Vertices 0-2 and 1-3 must both form a triangle, direction is null for sloped faces.
    private static int[] vertexOrder(IPolygon<?> polygon, @Nullable Direction direction) {
        if (polygon.getVertexCount() == 3)
            return new int[]{0, 1, 2, 0};
        int start = direction != null ? vanillaStart(polygon, direction) : 0;
        var order = new int[]{start, (start + 1) % 4, (start + 2) % 4, (start + 3) % 4};
        if (!flat(polygon, order[0], order[1], order[2]) && !flat(polygon, order[1], order[2], order[3]))
            return order;
        // A quad with a corner on an edge is a triangle, drop that corner.
        for (int i = 0; i < 4; i++) {
            if (flat(polygon, (i + 3) % 4, i, (i + 1) % 4)) {
                int a = (i + 1) % 4, b = (i + 2) % 4, c = (i + 3) % 4;
                return new int[]{a, b, c, a};
            }
        }
        return order;
    }

    private static boolean flat(IPolygon<?> polygon, int i, int j, int k) {
        var a = polygon.getVertex(i);
        var b = polygon.getVertex(j);
        var c = polygon.getVertex(k);
        double ux = b.getX() - a.getX(), uy = b.getY() - a.getY(), uz = b.getZ() - a.getZ();
        double vx = c.getX() - a.getX(), vy = c.getY() - a.getY(), vz = c.getZ() - a.getZ();
        double cx = uy * vz - uz * vy, cy = uz * vx - ux * vz, cz = ux * vy - uy * vx;
        return cx * cx + cy * cy + cz * cz < 1e-12;
    }

    /**
     * Full faces get their AO corners in vanilla's vertex order, so we start on the same corner as vanilla does.
     */
    private static int vanillaStart(IPolygon<?> polygon, Direction direction) {
        double minX = 1, minY = 1, minZ = 1, maxX = 0, maxY = 0, maxZ = 0;
        for (int i = 0; i < 4; i++) {
            var v = polygon.getVertex(i);
            minX = Math.min(minX, v.getX());
            minY = Math.min(minY, v.getY());
            minZ = Math.min(minZ, v.getZ());
            maxX = Math.max(maxX, v.getX());
            maxY = Math.max(maxY, v.getY());
            maxZ = Math.max(maxZ, v.getZ());
        }
        double nan = Double.NaN;
        double[] corner = switch (direction) {
            case DOWN -> new double[]{minX, nan, maxZ};
            case UP -> new double[]{minX, nan, minZ};
            case NORTH -> new double[]{maxX, maxY, nan};
            case SOUTH -> new double[]{minX, maxY, nan};
            case WEST -> new double[]{nan, maxY, minZ};
            case EAST -> new double[]{nan, maxY, maxZ};
        };
        int best = 0;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            var v = polygon.getVertex(i);
            double[] p = {v.getX(), v.getY(), v.getZ()};
            double distance = 0;
            for (int axis = 0; axis < 3; axis++) {
                if (!Double.isNaN(corner[axis]))
                    distance += Math.abs(p[axis] - corner[axis]);
            }
            if (distance < bestDistance) {
                best = i;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static boolean isAligned(Vector3f n) {
        return Math.max(Math.abs(n.x()), Math.max(Math.abs(n.y()), Math.abs(n.z()))) > 0.999F;
    }

    /**
     * Same as the game's directional shade, but for any normal.
     */
    private static float diffuseShade(Vector3f n) {
        return Math.min(n.x() * n.x() * 0.6F + n.y() * n.y() * ((3.0F + n.y()) / 4.0F) + n.z() * n.z() * 0.8F, 1.0F);
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
