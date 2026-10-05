package com.summax.apn.architecture.common.block.state;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.MapCodec;
import com.summax.apn.architecture.common.block.BlockArchitecture;
import com.summax.apn.architecture.core.ArchitectureLog;
import com.summax.apn.architecture.core.math.ITrans3;
import com.summax.apn.architecture.core.math.ITrans3Immutable;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class BlockStateArchitecture extends BlockState {

    private CachedProperties cachedProperties;

    private record CachedProperties(
            ITrans3Immutable cachedTransform,
            CompletableFuture<VoxelShape> cachedShape
    ) {
    }

    protected BlockStateArchitecture(BlockArchitecture block,
                                   ImmutableMap<Property<?>, Comparable<?>> properties,
                                   MapCodec<BlockState> codec) {
        super(block, properties, codec);
    }

    public static BlockStateArchitecture create(BlockArchitecture block,
                                                ImmutableMap<Property<?>, Comparable<?>> properties,
                                                MapCodec<BlockState> codec) {
        var state = new BlockStateArchitecture(block, properties, codec);
        state.postConstruct();
        return state;
    }

    protected void postConstruct() {
        this.cachedProperties = new CachedProperties(
                this.self().getTransformForState(this),
                this.self().getBoxesForState(this).thenApply(boxes -> {
                    // Join without optimizing at each step, optimizing once at the end is much cheaper.
                    var shape = Shapes.empty();
                    for (var box : boxes) {
                        shape = Shapes.joinUnoptimized(shape, Shapes.create(box.toMC()), BooleanOp.OR);
                    }
                    ArchitectureLog.debug("Finished creating shape for state: {}", this);
                    return shape.optimize();
                })
        );
    }

    private BlockArchitecture self() {
        return (BlockArchitecture) this.getBlock();
    }

    @NotNull
    public ITrans3 getTransform() {
        return this.cachedProperties.cachedTransform;
    }

    @NotNull
    public VoxelShape getShape() {
        return this.cachedProperties.cachedShape.join();
    }

    private volatile Boolean fullBlock;

    /**
     * @return true if the shape fills the whole block, cached since the light engine asks it constantly.
     */
    public boolean isFullBlockShape() {
        var full = this.fullBlock;
        if (full == null) {
            full = Block.isShapeFullBlock(this.getShape());
            this.fullBlock = full;
        }
        return full;
    }


}
