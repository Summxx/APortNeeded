package com.summax.apn.architecture.common.shape;

import com.summax.apn.architecture.core.ArchitectureLog;
import com.summax.apn.architecture.core.model.mesh.IMesh;
import com.summax.apn.architecture.core.model.mesh.PolygonData;
import com.summax.apn.architecture.common.shape.mesh.ShapeModels;
import com.summax.apn.architecture.common.shape.mesh.RoofMeshes;
import com.summax.apn.architecture.core.model.voxelize.Voxelizers;
import com.summax.apn.architecture.core.model.voxelize.IVoxelizer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Responsible for getting a mesh for a given shape enum, and a voxelizer for a given shape enum.
 */
public class ShapeMeshes {

    // TODO: There are likely special cases here we need to error for.

    private static final Map<EnumShape, IMesh<String, PolygonData>> MESHES = new HashMap<>();
    private static final Map<EnumShape, IVoxelizer> VOXELIZERS = new HashMap<>();

    static {
        Arrays.stream(EnumShape.values()).forEach(
                enumShape -> {
                    if (enumShape.getPlacementLogic() == null) {
                        return;
                    }
                    try {
                        var mesh = createMesh(enumShape);
                        register(enumShape, mesh, Voxelizers.of(mesh, 16));
                    } catch (Exception e) {
                        ArchitectureLog.error("Failed to load mesh for shape: " + enumShape.getName(), e);
                    }
                }
        );
    }

    /**
     * Gets the mesh of a shape, either its OBJSON model or a generated roof.
     */
    private static IMesh<String, PolygonData> createMesh(EnumShape enumShape) {
        var data = Objects.requireNonNull(ShapeTable.get(enumShape.getName()), "Unknown shape");
        return switch (data.kind()) {
            case MODEL, BANISTER -> ShapeModels.load(data.model());
            case ROOF -> Objects.requireNonNull(RoofMeshes.create(enumShape.getName()), "Roof not drawn yet");
        };
    }

    private static void register(EnumShape enumShape, IMesh<String, PolygonData> mesh, IVoxelizer voxelizer) {
        MESHES.put(enumShape, mesh);
        VOXELIZERS.put(enumShape, voxelizer);
    }

    public static IMesh<String, PolygonData> getMesh(EnumShape enumShape) {
        return MESHES.get(enumShape);
    }

    public static IVoxelizer getVoxelizer(EnumShape enumShape) {
        return VOXELIZERS.get(enumShape);
    }

}
