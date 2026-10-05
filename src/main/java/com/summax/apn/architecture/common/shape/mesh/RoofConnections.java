package com.summax.apn.architecture.common.shape.mesh;

import com.summax.apn.architecture.common.block.BlockShape;
import com.summax.apn.architecture.common.block.entity.BlockEntityShape;
import com.summax.apn.architecture.common.block.state.BlockStateShape;
import com.summax.apn.architecture.common.shape.orientation.ShapeOrientationPropertyFacing;
import com.summax.apn.architecture.common.shape.orientation.ShapeOrientationPropertySpin;
import com.summax.apn.architecture.common.shape.transformation.SideTurn;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Finds the neighbouring roofs a roof should connect to.
 */
public final class RoofConnections {

    private static final int[][] LOCAL_SIDES = {{1, 0, 0}, {-1, 0, 0}, {0, 0, -1}, {0, 0, 1}};

    private RoofConnections() {
    }

    /**
     * @return the connection mask of the roof at the given position, 0 for anything else.
     */
    public static int at(BlockGetter level, BlockPos pos, BlockState state) {
        if (!(state instanceof BlockStateShape shapeState) || !(state.getBlock() instanceof BlockShape<?> block))
            return 0;
        var joins = RoofMeshes.connectionSides(block.getShape().getName());
        if (joins == null)
            return 0;
        var orientation = shapeState.getOrientation();
        var facing = orientation.getValue(ShapeOrientationPropertyFacing.INSTANCE);
        var spin = orientation.getValue(ShapeOrientationPropertySpin.INSTANCE);
        if (facing == null || spin == null)
            return 0;
        var rotation = SideTurn.rotation(facing.value().get3DDataValue(), spin.value().getQuarterTurns());
        // Never create a block entity here, this gets called from the chunk building threads.
        var self = level.getExistingBlockEntity(pos) instanceof BlockEntityShape shape ? shape : null;

        int mask = 0;
        for (int i = 0; i < joins.length; i++) {
            if (joins[i] == null)
                continue;
            var side = globalSide(rotation, LOCAL_SIDES[i]);
            if (self != null && !self.isConnectionEnabled(side))
                continue;
            var neighbourPos = pos.relative(side);
            if (!(level.getBlockState(neighbourPos).getBlock() instanceof BlockShape<?> neighbour)
                    || !joins[i].accepts(neighbour.getShape().getName()))
                continue;
            if (level.getExistingBlockEntity(neighbourPos) instanceof BlockEntityShape other
                    && !other.isConnectionEnabled(side.getOpposite()))
                continue;
            mask |= 1 << i;
        }
        return mask;
    }

    private static Direction globalSide(double[][] r, int[] v) {
        double x = r[0][0] * v[0] + r[0][1] * v[1] + r[0][2] * v[2];
        double y = r[1][0] * v[0] + r[1][1] * v[1] + r[1][2] * v[2];
        double z = r[2][0] * v[0] + r[2][1] * v[1] + r[2][2] * v[2];
        return Direction.getNearest(x, y, z);
    }

    /**
     * @return true if the faces on this side change with the neighbours, they should never cull or be culled.
     */
    public static boolean dependsOnNeighbour(String shape, int side, int turn, Direction global) {
        var joins = RoofMeshes.connectionSides(shape);
        if (joins == null)
            return false;
        var normal = global.getNormal();
        var local = SideTurn.toLocal(side, turn, new net.minecraft.world.phys.Vec3(normal.getX(), normal.getY(), normal.getZ()));
        for (int i = 0; i < joins.length; i++) {
            if (joins[i] != null && Math.round(local.x) == LOCAL_SIDES[i][0] && Math.round(local.y) == LOCAL_SIDES[i][1]
                    && Math.round(local.z) == LOCAL_SIDES[i][2])
                return true;
        }
        return false;
    }
}
