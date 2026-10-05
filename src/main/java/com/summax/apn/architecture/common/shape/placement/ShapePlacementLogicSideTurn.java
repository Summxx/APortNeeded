package com.summax.apn.architecture.common.shape.placement;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.summax.apn.architecture.common.block.BlockArchitecture;
import com.summax.apn.architecture.common.block.BlockShape;
import com.summax.apn.architecture.common.block.state.BlockStateShape;
import com.summax.apn.architecture.common.shape.ShapeTable;
import com.summax.apn.architecture.common.shape.orientation.*;
import com.summax.apn.architecture.common.shape.transformation.SideTurn;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * Banisters line up with the stairs below them, shapes placed against another shape match its profile,
 * otherwise the orientation comes from the clicked face and position.
 */
public class ShapePlacementLogicSideTurn implements IShapePlacementLogic<BlockArchitecture> {

    /**
     * Offset of railings from the centre of the block, puts the rail against the edge.
     */
    public static final double PLACEMENT_OFFSET = 7D / 16D;

    public static double placementOffset(String shape) {
        return "banister_fancy_end".equals(shape) ? 6D / 16D : PLACEMENT_OFFSET;
    }

    private final ShapeTable.Data data;
    private final ImmutableCollection<ShapeOrientationProperty<?>> properties;

    public ShapePlacementLogicSideTurn(ShapeTable.Data data) {
        this.data = data;
        this.properties = data.hasFlag(ShapeTable.PLACE_OFFSET)
                ? ImmutableList.of(ShapeOrientationPropertyFacing.INSTANCE, ShapeOrientationPropertySpin.INSTANCE,
                ShapeOrientationPropertyLateralOffset.INSTANCE)
                : ImmutableList.of(ShapeOrientationPropertyFacing.INSTANCE, ShapeOrientationPropertySpin.INSTANCE);
    }

    @Override
    public @NotNull ShapeOrientation getShapeOrientationForPlacement(@NotNull BlockArchitecture beingPlaced,
                                                                     @NotNull Level level,
                                                                     @NotNull BlockPos placementPos,
                                                                     @NotNull Player placer,
                                                                     @NotNull BlockHitResult hitResult) {
        // The face of the block clicked, pointing towards the shape being placed.
        var face = hitResult.getDirection();
        // Click position relative to the centre of the block being placed.
        var hit = hitResult.getLocation().subtract(Vec3.atCenterOf(placementPos));
        var neighbour = level.getBlockState(placementPos.relative(face.getOpposite()));

        if (!placer.isShiftKeyDown()) {
            if (this.data.banister()) {
                var onStairs = this.orientOnStairs(neighbour, face, hit);
                if (onStairs != null)
                    return onStairs;
            }
            var matched = this.orientFromProfiles(neighbour, face);
            if (matched != null)
                return matched;
        }
        return this.orientFromHitPosition(placer, face, hit);
    }

    /**
     * Lines a banister up with the stairs it was placed on.
     */
    private ShapeOrientation orientOnStairs(BlockState neighbour, Direction face, Vec3 hit) {
        int nside, nturn;
        if (neighbour.getBlock() instanceof StairBlock && face.getAxis() == Direction.Axis.Y) {
            nside = neighbour.getValue(StairBlock.HALF) == Half.TOP ? 1 : 0;
            // Turns are counted down the stairs, the opposite of the stairs facing.
            var stairsFacing = neighbour.getValue(StairBlock.FACING).getOpposite();
            nturn = (turnToFaceEast(Direction.SOUTH) - turnToFaceEast(stairsFacing)) & 3;
            if (nside == 1 && (nturn & 1) == 0)
                nturn ^= 2;
        } else {
            var side = sideTurn(neighbour);
            if (side == null)
                return null;
            nside = side[0];
            nturn = side[1];
        }
        int side = face.getOpposite().get3DDataValue();
        if (side != nside)
            return null;
        return this.orientation(side, nturn & 3, this.offsetForHit(side, nturn, hit));
    }

    /**
     * Matches the profile of the neighbouring shape.
     */
    private ShapeOrientation orientFromProfiles(BlockState neighbour, Direction face) {
        var nsideTurn = sideTurn(neighbour);
        if (nsideTurn == null || !(neighbour.getBlock() instanceof BlockShape<?> nblock))
            return null;
        int nside = nsideTurn[0], nturn = nsideTurn[1];
        var otherProfile = profileGlobal(nblock.getShape().getDefinition(), nside, nturn, face);
        if (otherProfile == null)
            return null;
        var thisFace = face.getOpposite();
        for (int i = 0; i < 4; i++) {
            int turn = (nturn + i) & 3;
            var thisProfile = profileGlobal(this.data, nside, turn, thisFace);
            if (thisProfile != null && thisProfile.matches(otherProfile))
                return this.orientation(nside, turn, lateralOffset(neighbour));
        }
        return null;
    }

    private static ShapeTable.Profile profileGlobal(ShapeTable.Data shape, int side, int turn, Direction globalFace) {
        if (shape == null)
            return null;
        var normal = globalFace.getNormal();
        var local = SideTurn.toLocal(side, turn, new Vec3(normal.getX(), normal.getY(), normal.getZ()));
        var localFace = Direction.getNearest(local.x, local.y, local.z);
        return shape.profileForLocalFace(localFace.get3DDataValue());
    }

    private static int[] sideTurn(BlockState state) {
        if (!(state instanceof BlockStateShape shapeState)
                || !(state.getBlock() instanceof BlockShape<?> shape)
                || !(shape.getPlacementLogic() instanceof ShapePlacementLogicSideTurn))
            return null;
        var orientation = shapeState.getOrientation();
        var facing = orientation.getValue(ShapeOrientationPropertyFacing.INSTANCE);
        var spin = orientation.getValue(ShapeOrientationPropertySpin.INSTANCE);
        if (facing == null || spin == null)
            return null;
        return new int[]{facing.value().get3DDataValue(), spin.value().getQuarterTurns()};
    }

    private static double lateralOffset(BlockState state) {
        if (!(state instanceof BlockStateShape shapeState))
            return 0;
        var offset = shapeState.getOrientation().getValue(ShapeOrientationPropertyLateralOffset.INSTANCE);
        return offset != null ? offset.value().getSign() * PLACEMENT_OFFSET : 0;
    }

    private ShapeOrientation orientFromHitPosition(Player placer, Direction face, Vec3 hit) {
        boolean underneath = this.data.hasFlag(ShapeTable.PLACE_UNDERNEATH);
        int rightSideUp = underneath ? 1 : 0;
        int upsideDown = underneath ? 0 : 1;
        int side = switch (face) {
            case UP -> rightSideUp;
            case DOWN -> upsideDown;
            default -> {
                if (placer.isShiftKeyDown())
                    yield face.getOpposite().get3DDataValue();
                yield hit.y > 0.0 ? upsideDown : rightSideUp;
            }
        };
        int turn = this.turnForHit(side, hit);
        double offset = this.data.hasFlag(ShapeTable.PLACE_OFFSET) ? this.offsetForHit(side, turn, hit) : 0;
        return this.orientation(side, turn, offset);
    }

    private int turnForHit(int side, Vec3 hit) {
        var h = SideTurn.toLocal(side, 0, hit);
        double x = h.x, z = h.z;
        return switch (this.data.symmetry()) {
            case NONE, QUADRILATERAL -> 0;
            // Nearest edge.
            case BILATERAL -> Math.abs(z) > Math.abs(x) ? (z < 0 ? 2 : 0) : (x > 0 ? 1 : 3);
            // Nearest corner.
            case UNILATERAL -> z > 0 ? (x < 0 ? 0 : 1) : (x > 0 ? 2 : 3);
        };
    }

    private double offsetForHit(int side, int turn, Vec3 hit) {
        var h = SideTurn.toLocal(side, turn, hit);
        return h.x < 0 ? -PLACEMENT_OFFSET : PLACEMENT_OFFSET;
    }

    /**
     * Sneaking moves the shape to the next side, otherwise railings switch edges before the shape turns.
     */
    public ShapeOrientation hammered(ShapeOrientation current, boolean sneaking) {
        var facing = current.getValue(ShapeOrientationPropertyFacing.INSTANCE);
        var spin = current.getValue(ShapeOrientationPropertySpin.INSTANCE);
        var lateral = current.getValue(ShapeOrientationPropertyLateralOffset.INSTANCE);
        int side = facing != null ? facing.value().get3DDataValue() : 0;
        int turn = spin != null ? spin.value().getQuarterTurns() : 0;
        double dx = lateral != null ? lateral.value().getSign() * PLACEMENT_OFFSET : 0;
        if (sneaking) {
            side = (side + 1) % 6;
        } else {
            if (dx != 0)
                dx = -dx;
            if (dx >= 0)
                turn = (turn + 1) % 4;
        }
        return this.orientation(side, turn, dx);
    }

    private ShapeOrientation orientation(int side, int turn, double offset) {
        if (this.data.hasFlag(ShapeTable.PLACE_OFFSET)) {
            return ShapeOrientation.forProperties(
                    ShapeOrientationPropertyFacing.of(SideTurn.sideDirection(side)),
                    ShapeOrientationPropertySpin.of(EnumSpin.byIndex(turn)),
                    ShapeOrientationPropertyLateralOffset.of(EnumLateralOffset.forSign(offset))
            );
        }
        return ShapeOrientation.forProperties(
                ShapeOrientationPropertyFacing.of(SideTurn.sideDirection(side)),
                ShapeOrientationPropertySpin.of(EnumSpin.byIndex(turn))
        );
    }

    private static int turnToFaceEast(Direction direction) {
        return switch (direction) {
            case SOUTH -> 1;
            case WEST -> 2;
            case NORTH -> 3;
            default -> 0;
        };
    }

    @Override
    public @NotNull ImmutableCollection<ShapeOrientationProperty<?>> getProperties() {
        return this.properties;
    }
}
