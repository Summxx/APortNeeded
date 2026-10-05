package com.summax.apn.architecture.common.shape.mesh;

import com.summax.apn.architecture.core.math.ITrans3;
import com.summax.apn.architecture.core.model.mesh.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds a mesh from polygons centred on the block (-0.5 to 0.5).
 * <p>
 * Texture indices: 0 base material, 1 projected base, 2 secondary material, 3 projected secondary.
 */
public final class ShapeMeshBuilder {

    private static final double EPSILON = 1e-4;

    private final String name;
    private final Map<String, Face.Builder<PolygonData>> faces = new LinkedHashMap<>();

    public ShapeMeshBuilder(String name) {
        this.name = name;
    }

    /**
     * Adds a triangle or a quad, flipped if it doesn't face the outward direction.
     */
    public ShapeMeshBuilder add(int texture, double[] outward, double[][] pos, double[][] uv) {
        int n = pos.length;
        var normal = newellNormal(pos);
        if (normal == null)
            return this;
        if (dot(normal, outward) < 0) {
            pos = reversed(pos);
            uv = reversed(uv);
            normal = new double[]{-normal[0], -normal[1], -normal[2]};
        }
        var direction = nearestDirection(normal);
        var data = new PolygonData(cullFace(pos, normal), direction, texture, -1);
        var key = texture + ":" + Math.round(normal[0] * 1000) + "," + Math.round(normal[1] * 1000) + "," + Math.round(normal[2] * 1000);
        var face = this.faces.computeIfAbsent(key, k -> new Face.Builder<>());
        if (n == 3) {
            var tri = new Tri.Builder<PolygonData>();
            for (int i = 0; i < 3; i++)
                tri.addVertex(vertex(pos[i], normal, uv[i]));
            face.addPolygon(tri.setData(data).build());
        } else {
            var quad = new Quad.Builder<PolygonData>();
            for (int i = 0; i < 4; i++)
                quad.addVertex(vertex(pos[i], normal, uv[i]));
            face.addPolygon(quad.setData(data).build());
        }
        return this;
    }

    public IMesh<String, PolygonData> build() {
        var part = new Part.Builder<String, PolygonData>().setId("root");
        for (var face : this.faces.values())
            part.addFace(face.build());
        return new Mesh.Builder<String, PolygonData>(this.name).addPart(part.build()).build()
                .transform(ITrans3.BLOCK_CENTER, false);
    }

    private static IVertex vertex(double[] p, double[] normal, double[] uv) {
        return new Vertex(p[0], p[1], p[2], normal[0], normal[1], normal[2], uv[0], uv[1]);
    }

    private static double[][] reversed(double[][] a) {
        var r = new double[a.length][];
        for (int i = 0; i < a.length; i++)
            r[i] = a[a.length - 1 - i];
        return r;
    }

    private static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    /**
     * Computes the normal with Newell's method.
     *
     * @return the unit normal, or null for degenerate polygons.
     */
    static double[] newellNormal(double[][] pos) {
        double x = 0, y = 0, z = 0;
        for (int i = 0; i < pos.length; i++) {
            var a = pos[i];
            var b = pos[(i + 1) % pos.length];
            x += (a[1] - b[1]) * (a[2] + b[2]);
            y += (a[2] - b[2]) * (a[0] + b[0]);
            z += (a[0] - b[0]) * (a[1] + b[1]);
        }
        double length = Math.sqrt(x * x + y * y + z * z);
        return length > 1e-9 ? new double[]{x / length, y / length, z / length} : null;
    }

    private static FaceDirection nearestDirection(double[] n) {
        double ax = Math.abs(n[0]), ay = Math.abs(n[1]), az = Math.abs(n[2]);
        if (ay >= ax && ay >= az)
            return n[1] > 0 ? FaceDirection.UP : FaceDirection.DOWN;
        if (ax >= az)
            return n[0] > 0 ? FaceDirection.EAST : FaceDirection.WEST;
        return n[2] > 0 ? FaceDirection.SOUTH : FaceDirection.NORTH;
    }

    /**
     * Gets the cull face of a polygon lying on a side of the block.
     */
    private static CullFace cullFace(double[][] pos, double[] n) {
        for (int axis = 0; axis < 3; axis++) {
            for (int sign = -1; sign <= 1; sign += 2) {
                boolean onPlane = true;
                for (var p : pos)
                    onPlane &= Math.abs(p[axis] - 0.5 * sign) < EPSILON;
                if (onPlane && n[axis] * sign > 0.99)
                    return switch (axis) {
                        case 0 -> sign > 0 ? CullFace.EAST : CullFace.WEST;
                        case 1 -> sign > 0 ? CullFace.UP : CullFace.DOWN;
                        default -> sign > 0 ? CullFace.SOUTH : CullFace.NORTH;
                    };
            }
        }
        return CullFace.NONE;
    }
}
