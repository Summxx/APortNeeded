package com.summax.apn.architecture.common.shape.mesh;

import com.google.gson.Gson;
import com.summax.apn.architecture.core.model.mesh.IMesh;
import com.summax.apn.architecture.core.model.mesh.PolygonData;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Loads the OBJSON shape models from data/apn/objson/shape.
 */
public final class ShapeModels {

    private static final Gson GSON = new Gson();

    private static final class Model {
        double[] bounds;
        Face[] faces;
    }

    private static final class Face {
        int texture;
        double[][] vertices;
        int[][] triangles;
    }

    private ShapeModels() {
    }

    public static IMesh<String, PolygonData> load(String name) {
        // Read from the jar, the dedicated server needs these as well.
        var path = "/data/apn/objson/shape/" + name + ".objson";
        var in = Objects.requireNonNull(ShapeModels.class.getResourceAsStream(path), "Missing shape model " + path);
        Model model;
        try (var reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            model = GSON.fromJson(reader, Model.class);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to read shape model " + path, e);
        }

        var shift = recentring(name, model);
        var builder = new ShapeMeshBuilder(name);
        for (var face : model.faces) {
            for (var tri : face.triangles) {
                var pos = new double[3][];
                var uv = new double[3][];
                var outward = new double[3];
                for (int i = 0; i < 3; i++) {
                    var v = face.vertices[tri[i]];
                    pos[i] = adjust(name, new double[]{v[0] + shift[0], v[1] + shift[1], v[2] + shift[2]});
                    uv[i] = new double[]{v[6], v[7]};
                    // We use the vertex normals to know which side is outside.
                    outward[0] += v[3];
                    outward[1] += v[4];
                    outward[2] += v[5];
                }
                builder.add(face.texture, outward, pos, uv);
            }
        }
        return builder.build();
    }

    /**
     * Anything overhanging more than this is part of the design (roof overhangs, cornices).
     */
    private static final double MAX_OVERHANG = 0.025;
    private static final double EPSILON = 1e-3;
    /**
     * Balustrade rails are 2/16 thick, between 5/16 and 7/16 from the centre.
     */
    private static final double RAIL_MIN = 5D / 16D, RAIL_MAX = 7D / 16D, RAIL_SHIFT = 1D / 16D;

    private static double[] recentring(String name, Model model) {
        var shift = new double[3];
        if (keepsOverhang(name))
            return shift;
        for (int axis = 0; axis < 3; axis++) {
            double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
            for (var face : model.faces)
                for (var v : face.vertices) {
                    min = Math.min(min, v[axis]);
                    max = Math.max(max, v[axis]);
                }
            boolean nearFull = Math.abs(min + 0.5) < MAX_OVERHANG && Math.abs(max - 0.5) < MAX_OVERHANG;
            boolean offEdges = Math.abs(min + 0.5) > 1e-6 && Math.abs(max - 0.5) > 1e-6;
            if (nearFull && offEdges)
                shift[axis] = -(min + max) / 2;
        }
        return shift;
    }

    /**
     * Snaps shapes to the block bounds and moves the balustrade rails against the edge.
     */
    private static double[] adjust(String name, double[] p) {
        if (name.startsWith("balustrade_") && !name.startsWith("balustrade_stair_")) {
            // Keep the rail slightly behind the newel to avoid z-fighting.
            if (p[2] > RAIL_MIN - EPSILON && p[2] < RAIL_MAX + EPSILON)
                p[2] += name.equals("balustrade_fancy_with_newel") ? RAIL_SHIFT - 1D / 256D : RAIL_SHIFT;
        }
        if (name.equals("balustrade_plain_inner_corner") || name.equals("balustrade_plain_outer_corner")
                || name.equals("balustrade_fancy_corner") || name.equals("balustrade_stair_plain_inner_corner")) {
            // Corners have a second rail along Z.
            if (p[0] < -RAIL_MIN + EPSILON && p[0] > -RAIL_MAX - EPSILON)
                p[0] -= RAIL_SHIFT;
        }
        if (name.equals("balustrade_stair_plain_inner_corner") && p[2] > RAIL_MIN - EPSILON && p[2] < RAIL_MAX + EPSILON)
            p[2] += RAIL_SHIFT;
        if (!keepsOverhang(name)) {
            for (int axis = 0; axis < 3; axis++) {
                double excess = Math.abs(p[axis]) - 0.5;
                if (excess > 0 && excess < MAX_OVERHANG)
                    p[axis] = Math.copySign(0.5, p[axis]);
            }
        }
        return p;
    }

    private static boolean keepsOverhang(String name) {
        return name.startsWith("roof_overhang") || name.startsWith("cornice") || name.startsWith("balustrade_stair_")
                || name.equals("balustrade_fancy_newel_tall");
    }
}
