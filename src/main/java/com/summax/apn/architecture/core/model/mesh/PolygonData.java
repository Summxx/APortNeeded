package com.summax.apn.architecture.core.model.mesh;

import com.summax.apn.architecture.core.math.ITrans3;
import org.jetbrains.annotations.NotNull;

/**
 * Default implementation of {@link IPolygonData}.
 *
 * @param cullFace     The cull face of this polygon.
 * @param textureIndex The texture index of the polygon.
 * @param tintIndex    The tint index of the polygon.
 */
public record PolygonData(@NotNull CullFace cullFace, @NotNull FaceDirection face, int textureIndex,
                          int tintIndex) implements IPolygonData<PolygonData> {

    @Override
    public PolygonData transform(@NotNull ITrans3 trans) {
        // The face direction must follow the transform even for faces that can't be culled, such as slopes.
        return new PolygonData(this.cullFace() == CullFace.NONE ? CullFace.NONE : trans.transformCullFace(this.cullFace()),
                trans.transformFaceDirection(this.face()),
                this.textureIndex(),
                this.tintIndex());
    }

}
