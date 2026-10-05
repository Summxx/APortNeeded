package com.summax.apn.architecture.common.shape.transformation;

import com.summax.apn.architecture.common.shape.orientation.*;
import com.summax.apn.architecture.core.math.ITrans3;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.NotNull;

public class ShapeTransformationResolverSideTurn implements IShapeTransformationResolver {

    private final double offsetDistance;

    /**
     * @param offsetDistance distance of the lateral offset in blocks.
     */
    public ShapeTransformationResolverSideTurn(double offsetDistance) {
        this.offsetDistance = offsetDistance;
    }

    @Override
    public @NotNull ITrans3 resolve(@NotNull ShapeOrientation orientation) {
        var facing = orientation.getValue(ShapeOrientationPropertyFacing.INSTANCE);
        var spin = orientation.getValue(ShapeOrientationPropertySpin.INSTANCE);
        var offset = orientation.getValue(ShapeOrientationPropertyLateralOffset.INSTANCE);
        int side = (facing != null ? facing.value() : Direction.DOWN).get3DDataValue();
        int turn = spin != null ? spin.value().getQuarterTurns() : 0;
        double offsetX = offset != null ? offset.value().getSign() * this.offsetDistance : 0;
        return SideTurn.transform(side, turn, offsetX);
    }
}
