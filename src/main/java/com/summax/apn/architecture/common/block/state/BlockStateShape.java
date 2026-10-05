package com.summax.apn.architecture.common.block.state;

import com.summax.apn.architecture.common.block.FaceFootprint;
import com.summax.apn.architecture.common.shape.mesh.RoofConnections;
import com.summax.apn.architecture.common.shape.orientation.ShapeOrientationPropertyFacing;
import com.summax.apn.architecture.common.shape.orientation.ShapeOrientationPropertySpin;
import net.minecraft.core.Direction;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.MapCodec;
import com.summax.apn.architecture.common.block.BlockShape;
import com.summax.apn.architecture.common.shape.orientation.ShapeOrientation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * An extension of {@link BlockStateArchitecture} that provides a cached ShapeOrientation to avoid any lookups.
 */
public class BlockStateShape extends BlockStateArchitecture {

    private final ShapeOrientation cachedOrientation;

    public BlockStateShape(BlockShape<?> block, ImmutableMap<Property<?>, Comparable<?>> properties,
                           MapCodec<BlockState> codec) {
        this(block, ShapeOrientation::forState, properties, codec);
    }

    public BlockStateShape(BlockShape<?> block, ShapeOrientation orientation,
                           ImmutableMap<Property<?>, Comparable<?>> properties, MapCodec<BlockState> codec) {
        this(block, s -> orientation, properties, codec);
    }

    public BlockStateShape(BlockShape<?> block, Function<BlockStateShape, ShapeOrientation> orientationFunc,
                           ImmutableMap<Property<?>, Comparable<?>> properties, MapCodec<BlockState> codec) {
        super(block, properties, codec);
        this.cachedOrientation = orientationFunc.apply(this);
        this.postConstruct();
    }

    @NotNull
    public ShapeOrientation getOrientation() {
        return this.cachedOrientation;
    }

    /**
     * Per side (Direction ordinal), the part of it the oriented shape covers, computed on first use.
     */
    private volatile FaceFootprint[] footprints;

    @NotNull
    public FaceFootprint getFootprint(Direction side) {
        var cached = this.footprints;
        if (cached == null) {
            // Concurrent callers compute the same arrays, so a race here is fine.
            cached = new FaceFootprint[Direction.values().length];
            var mesh = ((BlockShape<?>) this.getBlock()).getShape().getMesh();
            var oriented = mesh != null ? mesh.transform(this.getTransform(), false) : null;
            var facing = this.cachedOrientation.getValue(ShapeOrientationPropertyFacing.INSTANCE);
            var spin = this.cachedOrientation.getValue(ShapeOrientationPropertySpin.INSTANCE);
            var name = ((BlockShape<?>) this.getBlock()).getShape().getName();
            for (var direction : Direction.values()) {
                // Roof sides joining neighbours change shape, don't hide anything there.
                boolean variable = facing != null && spin != null && RoofConnections.dependsOnNeighbour(name,
                        facing.value().get3DDataValue(), spin.value().getQuarterTurns(), direction);
                cached[direction.ordinal()] = oriented != null && !variable
                        ? FaceFootprint.of(oriented, direction) : FaceFootprint.EMPTY;
            }
            this.footprints = cached;
        }
        return cached[side.ordinal()];
    }

}
