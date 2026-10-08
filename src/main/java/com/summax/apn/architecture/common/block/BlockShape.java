package com.summax.apn.architecture.common.block;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import com.summax.apn.architecture.common.item.ItemCladding;
import com.summax.apn.architecture.common.item.ItemShape;
import java.util.List;
import com.google.common.collect.ImmutableList;
import com.summax.apn.architecture.common.block.entity.BlockEntityShape;
import com.summax.apn.architecture.common.block.state.BlockStateArchitecture;
import com.summax.apn.architecture.common.block.state.BlockStateShape;
import com.summax.apn.architecture.common.shape.EnumShape;
import com.summax.apn.architecture.common.shape.orientation.ShapeOrientation;
import com.summax.apn.architecture.common.shape.placement.IShapePlacementLogic;
import com.summax.apn.architecture.core.ArchitectureLog;
import com.summax.apn.architecture.core.math.ITrans3;
import com.summax.apn.architecture.core.math.ITrans3Immutable;
import com.summax.apn.architecture.core.model.voxelize.IVoxelizer;
import com.summax.apn.architecture.core.physics.AABB;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import com.summax.apn.architecture.common.ArchitectureMod;
import com.summax.apn.architecture.common.compat.EffortlessCompat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Base class for all shape blocks, handles placement logic and state definition based on the shape.
 *
 * @param <T> a self-referential generic type.
 */
public class BlockShape<T extends BlockShape<T>> extends BlockArchitecture implements EntityBlock {

    private final EnumShape shape;

    public BlockShape(EnumShape shape) {
        this(shape, BlockBehaviour.Properties.of().lightLevel(state -> shape.isGlowing() ? 15 : 0));
    }

    public BlockShape(EnumShape shape, Properties properties) {
        this(shape, properties, ShapeOrientation::forState);
    }

    public BlockShape(EnumShape shape, Properties properties, ShapeOrientation orientation) {
        this(shape, properties, state -> orientation);
    }

    public BlockShape(EnumShape shape, Properties properties, Function<BlockStateShape, ShapeOrientation> orientationFunc) {
        super(properties);
        this.shape = shape;
        // We use a custom BlockState implementation, so we have to use reflection to force the state definition to use it.
        var builder = new StateDefinition.Builder<Block, BlockState>(this);
        var placementLogic = this.shape.getPlacementLogic();
        // TODO: These shouldn't be null in the future, we just want to compile for now.
        if (placementLogic != null)
            placementLogic.getProperties().forEach(builder::add);
        this.createBlockStateDefinition(builder);
        setStateDefinition(this,
                builder.create(Block::defaultBlockState,
                        (block, propertyValues, codec) ->
                                new BlockStateShape((BlockShape<?>) block, orientationFunc, propertyValues, codec)
                )
        );
        this.registerDefaultState(this.getStateDefinition().any());
    }

    public EnumShape getShape() {
        return this.shape;
    }

    public IShapePlacementLogic<T> getPlacementLogic() {
        return this.shape.getPlacementLogic();
    }

    /**
     * Gets the block as a generic type, used when we need to return the block as a generic type.
     *
     * @return this cast to the generic type.
     */
    @SuppressWarnings("unchecked")
    private T self() {
        return (T) this;
    }

    /**
     * Gets the block state as a shape state, used when we need to reference our custom state implementation.
     *
     * @param state the state to cast.
     * @return the state cast to our custom state implementation, or null if it is not.
     */
    @Nullable
    private BlockStateShape asShapeState(BlockState state) {
        if (state instanceof BlockStateShape) {
            return (BlockStateShape) state;
        } else {
            return null;
        }
    }


    @Override
    public ITrans3Immutable getTransformForState(BlockStateArchitecture state) {
        var shapeState = this.asShapeState(state);
        if (shapeState == null) {
            // Expected while Block's constructor builds its temporary state definition, before ours replaces it.
            ArchitectureLog.debug("BlockShape#getTransformForState called with a non-shape state.");
            return ITrans3.ofIdentity();
        }
        var transformationResolver = Optional.ofNullable(this.getShape().getTransformationResolver()).orElse(s -> ITrans3.ofIdentity());
        return transformationResolver.resolve(shapeState).asImmutable();
    }

    @Override
    public CompletableFuture<ImmutableList<AABB>> getBoxesForState(BlockStateArchitecture state) {
        var shapeState = this.asShapeState(state);
        if (shapeState == null) {
            // Expected while Block's constructor builds its temporary state definition, before ours replaces it.
            ArchitectureLog.debug("BlockShape#getBoxesForState called with a non-shape state.");
            return CompletableFuture.completedFuture(DEFAULT_BOX);
        }
        var transformationResolver = Optional.ofNullable(this.getShape().getTransformationResolver()).orElse(s -> ITrans3.ofIdentity());
        var voxelizer = shape.getVoxelizer();
        var transform = transformationResolver.resolve(shapeState);
        var voxelsCompletableFuture = Optional.ofNullable(voxelizer).map(IVoxelizer::voxelize).orElse(CompletableFuture.completedFuture(DEFAULT_BOX));
        return voxelsCompletableFuture.thenApplyAsync(aabbs -> aabbs.stream().map(transform::transformAABB).collect(ImmutableList.toImmutableList()));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        var player = context.getPlayer();
        // Effortless Building works out the state without a player, on the client: use the local one.
        if (player == null && EffortlessCompat.isFindingState() && context.getLevel().isClientSide())
            player = ArchitectureMod.PROXY.getClientPlayer();
        if (player == null) {
            ArchitectureLog.debug("BlockShape#getStateForPlacement called without a player, using the default orientation.");
            return this.defaultBlockState();
        }
        ShapeOrientation safeShapeOrientation = null;
        try {
            safeShapeOrientation = this.getPlacementLogic().getShapeOrientationForPlacement(
                    this.self(),
                    context.getLevel(),
                    context.getClickedPos(),
                    player,
                    new BlockHitResult(
                            context.getClickLocation(),
                            context.getClickedFace(),
                            context.getClickedPos(),
                            context.isInside()
                    )
            );
        } catch (Exception e) {
            ArchitectureLog.error("BlockShape#getStateForPlacement threw an exception, this should not happen.", e);
            safeShapeOrientation = ShapeOrientation.forState(asShapeState(this.defaultBlockState()));
        }
        // Defer to the placement logic to get the correct state for the shape.
        return safeShapeOrientation.applyToState(this.defaultBlockState());
    }

    @Override
    public boolean hasDynamicShape() {
        return false;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityShape(pos, state);
    }



    /**
     * Let light into partial shapes like vanilla stairs and slabs, only full shapes stop it.
     */
    private static boolean isFullBlock(BlockState state, BlockGetter level, BlockPos pos) {
        if (state instanceof BlockStateArchitecture architecture)
            return architecture.isFullBlockShape();
        return Block.isShapeFullBlock(state.getShape(level, pos));
    }

    /**
     * Applies cladding as the shape's secondary material.
     */
    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        var stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof ItemCladding cladding) || !this.getShape().acceptsCladding()
                || !(level.getBlockEntity(pos) instanceof BlockEntityShape shape))
            return super.use(state, level, pos, player, hand, hit);
        if (shape.getSecondaryMaterialState().isEmpty()) {
            var material = cladding.blockStateFromStack(stack);
            if (!BlockEntityShape.isValidMaterial(material))
                return InteractionResult.PASS;
            if (!level.isClientSide()) {
                shape.setSecondaryMaterialState(material);
                if (!player.isCreative())
                    stack.shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /**
     * Neighbouring shapes can hide the faces of this one they cover, see {@link #hidesNeighborFace}.
     */
    @Override
    public boolean supportsExternalFaceHiding(BlockState state) {
        return true;
    }

    /**
     * Hides the neighbour's face when we cover it entirely with an opaque material, or with the same material.
     */
    @Override
    public boolean hidesNeighborFace(BlockGetter level, BlockPos pos, BlockState state, BlockState neighborState, Direction dir) {
        if (!(state instanceof BlockStateShape shapeState))
            return false;
        var footprint = shapeState.getFootprint(dir);
        if (footprint.isEmpty())
            return false;
        // Never create block entities here: chunk meshing calls this from worker threads.
        if (!(level.getExistingBlockEntity(pos) instanceof BlockEntityShape shape))
            return false;
        var material = shape.getEffectiveBaseMaterialState();
        if (neighborState instanceof BlockStateShape neighbourShape) {
            if (!footprint.covers(neighbourShape.getFootprint(dir.getOpposite())))
                return false;
            if (material.canOcclude())
                return true;
            return level.getExistingBlockEntity(pos.relative(dir)) instanceof BlockEntityShape neighbour
                    && neighbour.getEffectiveBaseMaterialState() == material;
        }
        // Hide other blocks behind a full side.
        return footprint.isFull() && material.canOcclude();
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return isFullBlock(state, level, pos) ? level.getMaxLightLevel() : 0;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return !isFullBlock(state, level, pos);
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return isFullBlock(state, level, pos) ? 0.2F : 1.0F;
    }

    /**
     * @return the shape item made of this block's material.
     */
    private ItemStack getShapeStack(BlockGetter level, BlockPos pos) {
        var material = level.getBlockEntity(pos) instanceof BlockEntityShape shape
                ? shape.getEffectiveBaseMaterialState()
                : Blocks.OAK_PLANKS.defaultBlockState();
        return ItemShape.createStack(this.getShape(), material);
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return this.getShapeStack(level, pos);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        // Drop the shape with its material.
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof BlockEntityShape shape) {
            return List.of(ItemShape.createStack(this.getShape(), shape.getEffectiveBaseMaterialState()));
        }
        return List.of();
    }

    @Override
    public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        // Shapes resist explosions like the material they are made of.
        if (level.getBlockEntity(pos) instanceof BlockEntityShape shape) {
            return shape.getEffectiveBaseMaterialState().getExplosionResistance(level, pos, explosion);
        }
        return super.getExplosionResistance(state, level, pos, explosion);
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        // Shapes break like the material they are made of.
        if (level.getBlockEntity(pos) instanceof BlockEntityShape shape) {
            return shape.getEffectiveBaseMaterialState().getDestroyProgress(player, level, pos);
        }
        return super.getDestroyProgress(state, player, level, pos);
    }
}
