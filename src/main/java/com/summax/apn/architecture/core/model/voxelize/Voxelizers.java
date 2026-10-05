package com.summax.apn.architecture.core.model.voxelize;

import com.summax.apn.architecture.core.model.mesh.IMesh;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Voxelizers {

    private static final Map<String, IVoxelizer> VOXELIZERS = new ConcurrentHashMap<>();
    private static final int DEFAULT_BLOCK_RESOLUTION = 16;

    public static IVoxelizer of(IMesh<?, ?> mesh, int blockResolution) {
        return VOXELIZERS.computeIfAbsent(mesh.getName(), n -> new Voxelizer(mesh, blockResolution));
    }

    public static IVoxelizer of(IMesh<?, ?> mesh) {
        return of(mesh, DEFAULT_BLOCK_RESOLUTION);
    }

}
