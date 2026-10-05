package com.summax.apn.architecture.common.shape.transformation;

import com.summax.apn.architecture.core.math.IMatrix4Immutable;
import com.summax.apn.architecture.core.math.ITrans3;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * Shapes rest on a side (0 to 5, Direction's index) and turn in quarter turns around it.
 */
public final class SideTurn {

    private static final double[][][][] SIDE_TURN = new double[6][4][][];

    static {
        double[][][] sides = {
                identity(),                                // 0, DOWN
                rot(180, 1, 2),                            // 1, UP: rotX(180)
                rot(90, 1, 2),                             // 2, NORTH: rotX(90)
                mul(rot(-90, 1, 2), rot(180, 2, 0)),       // 3, SOUTH: rotX(-90) * rotY(180)
                mul(rot(-90, 0, 1), rot(90, 2, 0)),        // 4, WEST: rotZ(-90) * rotY(90)
                mul(rot(90, 0, 1), rot(-90, 2, 0))         // 5, EAST: rotZ(90) * rotY(-90)
        };
        for (int side = 0; side < 6; side++) {
            for (int turn = 0; turn < 4; turn++) {
                SIDE_TURN[side][turn] = mul(sides[side], rot(turn * 90, 2, 0));
            }
        }
    }

    private SideTurn() {
    }

    private static double[][] identity() {
        return new double[][]{{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};
    }

    private static double[][] rot(double deg, int i, int j) {
        var a = Math.toRadians(deg);
        var s = Math.round(Math.sin(a));
        var c = Math.round(Math.cos(a));
        var r = identity();
        r[i][i] = c;
        r[i][j] = -s;
        r[j][i] = s;
        r[j][j] = c;
        return r;
    }

    private static double[][] mul(double[][] a, double[][] b) {
        var r = new double[3][3];
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                r[i][j] = a[i][0] * b[0][j] + a[i][1] * b[1][j] + a[i][2] * b[2][j];
        return r;
    }

    public static double[][] rotation(int side, int turn) {
        return SIDE_TURN[side][turn & 3];
    }

    /**
     * Converts a world vector to the shape's local space.
     */
    public static Vec3 toLocal(int side, int turn, Vec3 v) {
        var m = rotation(side, turn);
        return new Vec3(
                v.x * m[0][0] + v.y * m[1][0] + v.z * m[2][0],
                v.x * m[0][1] + v.y * m[1][1] + v.z * m[2][1],
                v.x * m[0][2] + v.y * m[1][2] + v.z * m[2][2]);
    }

    /**
     * Rotates around the block centre after offsetting along the local X axis.
     */
    public static ITrans3 transform(int side, int turn, double offsetX) {
        var r = rotation(side, turn);
        // world = c + r * (local - c) + r * (offsetX, 0, 0), c being the block centre.
        double tx = 0.5 - 0.5 * (r[0][0] + r[0][1] + r[0][2]) + r[0][0] * offsetX;
        double ty = 0.5 - 0.5 * (r[1][0] + r[1][1] + r[1][2]) + r[1][0] * offsetX;
        double tz = 0.5 - 0.5 * (r[2][0] + r[2][1] + r[2][2]) + r[2][0] * offsetX;
        return ITrans3.ofImmutable(IMatrix4Immutable.of(
                r[0][0], r[0][1], r[0][2], tx,
                r[1][0], r[1][1], r[1][2], ty,
                r[2][0], r[2][1], r[2][2], tz,
                0, 0, 0, 1));
    }

    public static Direction sideDirection(int side) {
        return Direction.from3DDataValue(side);
    }
}
