package com.summax.apn.architecture.common.block.entity;

import com.summax.apn.architecture.common.model.ModelProperties;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import com.summax.apn.architecture.common.ArchitectureMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class BlockEntityShape extends BlockEntityArchitecture {

    private BlockState baseMaterialState, secondaryMaterialState;
    /**
     * Sides disconnected with the chisel, as Direction 3D data value bits.
     */
    private int disabledConnections;

    public BlockEntityShape(BlockPos pos, BlockState state) {
        super(ArchitectureMod.CONTENT.blockEntityTypeShape, pos, state);
    }

    /**
     * Gets the BlockEntityShape at the given position, if it exists. Returns an empty optional if it does not.
     *
     * @param world The world to get the BlockEntityShape from.
     * @param pos   The position to get the BlockEntityShape from.
     * @return An optional containing the BlockEntityShape, or an empty optional if it does not exist.
     */
    public static Optional<BlockEntityShape> getAtOptionally(LevelAccessor world, BlockPos pos) {
        return Optional.ofNullable(world.getBlockEntity(pos)).map(te -> te instanceof BlockEntityShape ? (BlockEntityShape) te : null);
    }

    /**
     * Gets the BlockEntityShape at the given position, if it exists. Returns null if it does not.
     *
     * @param world The world to get the BlockEntityShape from.
     * @param pos   The position to get the BlockEntityShape from.
     * @return The BlockEntityShape, or null if it does not exist.
     */
    @Nullable
    public static BlockEntityShape getAt(LevelAccessor world, BlockPos pos) {
        var te = world.getBlockEntity(pos);
        if (te instanceof BlockEntityShape) {
            return (BlockEntityShape) te;
        }
        return null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        this.writeMaterials(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.readMaterials(tag);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.baseMaterialState == null && this.level != null && !this.level.isClientSide()) {
            var tag = PendingMaterials.take(this.level, this.worldPosition);
            if (tag != null) {
                this.readMaterials(tag);
                this.setChanged();
            }
        }
    }

    public CompoundTag getMaterialsTag() {
        var tag = new CompoundTag();
        this.writeMaterials(tag);
        return tag;
    }

    private void writeMaterials(CompoundTag tag) {
        tag.put("BaseMaterial", NbtUtils.writeBlockState(this.getEffectiveBaseMaterialState()));
        this.getSecondaryMaterialState().ifPresent(s -> tag.put("SecondaryMaterial", NbtUtils.writeBlockState(s)));
        if (this.disabledConnections != 0)
            tag.putInt("Disconnected", this.disabledConnections);
    }

    private void readMaterials(CompoundTag tag) {
        var blocks = BuiltInRegistries.BLOCK.asLookup();
        this.baseMaterialState = tag.contains("BaseMaterial") ? NbtUtils.readBlockState(blocks, tag.getCompound("BaseMaterial")) : null;
        this.secondaryMaterialState = tag.contains("SecondaryMaterial") ? NbtUtils.readBlockState(blocks, tag.getCompound("SecondaryMaterial")) : null;
        this.disabledConnections = tag.getInt("Disconnected");
    }

    @Override
    @NotNull
    public CompoundTag getUpdateTag() {
        var tag = super.getUpdateTag();
        this.writeMaterials(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        this.readMaterials(tag);
        this.requestModelDataUpdate();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        var tag = pkt.getTag();
        if (tag != null) {
            this.readMaterials(tag);
            this.onMaterialsSynced();
        }
    }

    private void onMaterialsSynced() {
        this.requestModelDataUpdate();
        if (this.level != null && this.level.isClientSide()) {
            // Rebuild in the background, UPDATE_IMMEDIATE would block the main thread.
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /**
     * Marks the shape as changed and sends the new materials to clients.
     */
    private void onMaterialsChanged() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    @NotNull
    public ModelData getModelData() {
        return ModelData.builder()
                .with(ModelProperties.BASE_MATERIAL, this.getEffectiveBaseMaterialState())
                .with(ModelProperties.SECONDARY_MATERIAL, this.getEffectiveSecondaryMaterialState())
                .build();
    }

    public boolean isConnectionEnabled(Direction side) {
        return (this.disabledConnections & (1 << side.get3DDataValue())) == 0;
    }

    private void setConnectionEnabled(Direction side, boolean enabled) {
        int bit = 1 << side.get3DDataValue();
        this.disabledConnections = enabled ? this.disabledConnections & ~bit : this.disabledConnections | bit;
        this.onMaterialsChanged();
    }

    /**
     * Toggles the connection on the given side, for this shape and the one facing it.
     */
    public void toggleConnection(Direction side) {
        boolean enabled = !this.isConnectionEnabled(side);
        this.setConnectionEnabled(side, enabled);
        if (this.level != null && this.level.getBlockEntity(this.worldPosition.relative(side)) instanceof BlockEntityShape neighbour)
            neighbour.setConnectionEnabled(side.getOpposite(), enabled);
    }


    /**
     * Sets the base material state of this shape.
     *
     * @param baseMaterialState The base material state to set.
     * @return This BlockEntityShape instance.
     */
    public BlockEntityShape setBaseMaterialState(BlockState baseMaterialState) {
        this.baseMaterialState = baseMaterialState;
        this.onMaterialsChanged();
        return this;
    }

    /**
     * Gets the secondary material state of this shape wrapped in an optional for additional safety, if the secondary material state is not set then the base material should be used.
     *
     * @return The secondary material state of this shape.
     */
    public boolean hasBaseMaterial() {
        return this.baseMaterialState != null;
    }

    public Optional<BlockState> getSecondaryMaterialState() {
        return Optional.ofNullable(this.secondaryMaterialState);
    }

    /**
     * Sets the secondary material state of this shape.
     *
     * @param secondaryMaterialState The secondary material state to set.
     * @return This BlockEntityShape instance.
     */
    public BlockEntityShape setSecondaryMaterialState(BlockState secondaryMaterialState) {
        this.secondaryMaterialState = secondaryMaterialState;
        this.onMaterialsChanged();
        return this;
    }

    /**
     * Gets the effective base material state of this shape, if the base material state is not set then oak planks should be used to prevent crashes.
     *
     * @return The effective base material state of this shape.
     */
    public BlockState getEffectiveBaseMaterialState() {
        return isValidMaterial(this.baseMaterialState) ? this.baseMaterialState : Blocks.OAK_PLANKS.defaultBlockState();
    }

    /**
     * Make sure we never use air, shapes or block entity blocks as a material, fall back to oak planks instead.
     */
    public static boolean isValidMaterial(@Nullable BlockState state) {
        return state != null && !state.isAir() && !(state.getBlock() instanceof EntityBlock);
    }

    /**
     * Gets the effective secondary material state of this shape, if the secondary material state is not set then the effective base material state should be used.
     *
     * @return The effective secondary material state of this shape.
     */
    public BlockState getEffectiveSecondaryMaterialState() {
        return isValidMaterial(this.secondaryMaterialState) ? this.secondaryMaterialState : this.getEffectiveBaseMaterialState();
    }

    /**
     * @param i the texture index, 0 and 1 for the base material, 2 and 3 for the secondary one.
     */
    public BlockState getMaterialStateForIndex(int i) {
        return i < 2 ? this.getEffectiveBaseMaterialState() : this.getEffectiveSecondaryMaterialState();
    }
}
