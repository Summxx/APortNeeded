package com.summax.apn.architecture.common.block;

import com.summax.apn.architecture.core.model.mesh.IMesh;
import com.summax.apn.architecture.core.model.mesh.IPolygon;
import net.minecraft.core.Direction;

/**
 * The part of one side of the block a shape covers with faces lying on that side, sampled on a grid. Two shapes
 * touching on a side can hide each other's faces there when one footprint covers the other, like vanilla does for full
 * block faces.
 */
public final class FaceFootprint {

    /**
     * Samples per block edge: a quarter of a texture pixel.
     */
    private static final int RESOLUTION = 64;
    private static final double EPSILON = 1e-6;

    public static final FaceFootprint EMPTY = new FaceFootprint(new long[RESOLUTION * RESOLUTION / 64], 0);

    private final long[] samples;
    private final double area;

    private FaceFootprint(long[] samples, double area) {
        this.samples = samples;
        this.area = area;
    }

    public boolean isEmpty() {
        return this.area <= EPSILON;
    }

    public boolean isFull() {
        return this.area >= 1 - EPSILON;
    }

    /**
     * @return true if this footprint covers every part of the other one.
     */
    public boolean covers(FaceFootprint other) {
        if (other.isEmpty())
            return false;
        if (this.isFull())
            return true;
        if (other.area > this.area + EPSILON)
            return false;
        for (int i = 0; i < this.samples.length; i++) {
            if ((other.samples[i] & ~this.samples[i]) != 0)
                return false;
        }
        return true;
    }

    /**
     * @param mesh the shape's mesh in block space (0 to 1), already oriented.
     * @param side the side of the block.
     */
    public static FaceFootprint of(IMesh<String, ?> mesh, Direction side) {
        var samples = new long[RESOLUTION * RESOLUTION / 64];
        double area = 0;
        for (var face : mesh.getFaces()) {
            for (var polygon : face.getPolygons()) {
                if (polygon.getPolygonData().cullFace().toDirection() != side)
                    continue;
                var points = project(polygon, side.getAxis());
                area += Math.abs(signedArea(points));
                rasterize(points, samples);
            }
        }
        return area <= EPSILON ? EMPTY : new FaceFootprint(samples, area);
    }

    /**
     * Coordinates on the side's plane, the same for both blocks sharing it.
     */
    private static double[][] project(IPolygon<?> polygon, Direction.Axis axis) {
        int n = polygon.getVertexCount();
        var points = new double[n][];
        for (int i = 0; i < n; i++) {
            var v = polygon.getVertex(i);
            points[i] = switch (axis) {
                case X -> new double[]{v.getY(), v.getZ()};
                case Y -> new double[]{v.getX(), v.getZ()};
                case Z -> new double[]{v.getX(), v.getY()};
            };
        }
        return points;
    }

    private static double signedArea(double[][] p) {
        double a = 0;
        for (int i = 0; i < p.length; i++) {
            var q = p[(i + 1) % p.length];
            a += p[i][0] * q[1] - q[0] * p[i][1];
        }
        return a / 2;
    }

    /**
     * Marks the grid cells whose centre lies inside the convex polygon.
     */
    private static void rasterize(double[][] p, long[] samples) {
        double sign = Math.signum(signedArea(p));
        if (sign == 0)
            return;
        double minU = 1, minV = 1, maxU = 0, maxV = 0;
        for (var q : p) {
            minU = Math.min(minU, q[0]);
            minV = Math.min(minV, q[1]);
            maxU = Math.max(maxU, q[0]);
            maxV = Math.max(maxV, q[1]);
        }
        int u0 = Math.max(0, (int) Math.floor(minU * RESOLUTION)), u1 = Math.min(RESOLUTION - 1, (int) Math.ceil(maxU * RESOLUTION));
        int v0 = Math.max(0, (int) Math.floor(minV * RESOLUTION)), v1 = Math.min(RESOLUTION - 1, (int) Math.ceil(maxV * RESOLUTION));
        for (int iu = u0; iu <= u1; iu++) {
            double u = (iu + 0.5) / RESOLUTION;
            for (int iv = v0; iv <= v1; iv++) {
                double v = (iv + 0.5) / RESOLUTION;
                if (inside(p, u, v, sign)) {
                    int bit = iu * RESOLUTION + iv;
                    samples[bit >>> 6] |= 1L << (bit & 63);
                }
            }
        }
    }

    private static boolean inside(double[][] p, double u, double v, double sign) {
        for (int i = 0; i < p.length; i++) {
            var a = p[i];
            var b = p[(i + 1) % p.length];
            double cross = (b[0] - a[0]) * (v - a[1]) - (b[1] - a[1]) * (u - a[0]);
            if (cross * sign < -EPSILON)
                return false;
        }
        return true;
    }
}
