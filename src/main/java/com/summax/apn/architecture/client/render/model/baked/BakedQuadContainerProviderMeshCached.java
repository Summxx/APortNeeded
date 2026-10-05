package com.summax.apn.architecture.client.render.model.baked;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.summax.apn.architecture.client.render.model.resolver.IQuadMetadataResolver;
import com.summax.apn.architecture.core.math.ITrans3;
import com.summax.apn.architecture.core.model.mesh.IMesh;
import com.summax.apn.architecture.core.model.mesh.IPolygonData;
import org.jetbrains.annotations.Nullable;


/**
 * An extension of {@link BakedQuadContainerProviderMesh} that caches the quad containers it generates.
 *
 * @param <I> The type of the part ID.
 * @param <D> The type of the polygon data.
 */
public class BakedQuadContainerProviderMeshCached<I, D extends IPolygonData<D>> extends BakedQuadContainerProviderMesh<I, D> {

    /**
     * Chunk building is multithreaded, so we need a concurrent map here.
     */
    private final Map<Key<I, D>, IBakedQuadContainer> cache = new ConcurrentHashMap<>();

    private record Key<I, D>(@Nullable I partId, IQuadMetadataResolver<D> resolver, ITrans3 transform, boolean shaders) {
    }

    public BakedQuadContainerProviderMeshCached(IMesh<I, D> mesh, TextureMode textureMode) {
        super(mesh, textureMode);
    }

    public BakedQuadContainerProviderMeshCached(IMesh<I, D> mesh, TextureMode textureMode, boolean normalShading) {
        super(mesh, textureMode, normalShading);
    }

    @Override
    public IBakedQuadContainer getQuads(@Nullable I partId, IQuadMetadataResolver<D> metadataResolver, ITrans3 transform, boolean forceRebuild) {
        boolean shaders = ShaderPacks.inUse();
        var key = new Key<>(partId, metadataResolver, transform, shaders);
        if (forceRebuild) {
            var quadContainer = this.bakeQuads(partId, metadataResolver, transform, shaders);
            this.cache.put(key, quadContainer);
            return quadContainer;
        }
        return this.cache.computeIfAbsent(key, k -> this.bakeQuads(partId, metadataResolver, transform, shaders));
    }

}
