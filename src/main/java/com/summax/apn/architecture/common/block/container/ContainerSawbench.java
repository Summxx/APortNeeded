package com.summax.apn.architecture.common.block.container;

import com.summax.apn.architecture.common.ArchitectureMod;
import com.summax.apn.architecture.common.item.ItemShape;
import com.summax.apn.architecture.common.shape.EnumShape;
import com.summax.apn.architecture.common.shape.ShapePage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Sawbench menu, works like the vanilla stonecutter.
 */
public class ContainerSawbench extends AbstractContainerMenu {

    public static final int MATERIAL_SLOT = 0;
    public static final int RESULT_SLOT = 1;
    private static final int PLAYER_SLOTS_START = 2;
    private static final int PLAYER_SLOTS_END = PLAYER_SLOTS_START + 36;
    /**
     * Button ids are encoded as page * PAGE_STRIDE + slot.
     */
    public static final int PAGE_STRIDE = 64;

    private final ContainerLevelAccess access;
    private final Container materialContainer = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            ContainerSawbench.this.slotsChanged(this);
        }
    };
    private final ResultContainer resultContainer = new ResultContainer();
    private final DataSlot selectedPage = DataSlot.standalone();
    private final DataSlot selectedSlot = DataSlot.standalone();
    private final int[] selectedSlotPerPage = new int[ShapePage.PAGES.size()];

    public ContainerSawbench(Inventory playerInv, int id, @Nullable BlockPos pos) {
        super(ArchitectureMod.CONTENT.universalMenuType, id);
        this.access = pos == null ? ContainerLevelAccess.NULL : ContainerLevelAccess.create(playerInv.player.level(), pos);

        this.addSlot(new Slot(this.materialContainer, 0, 12, 19) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return getMaterialState(stack) != null;
            }
        });
        this.addSlot(new Slot(this.resultContainer, 0, 12, 57) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(@NotNull Player player, @NotNull ItemStack stack) {
                stack.onCraftedBy(player.level(), player, stack.getCount());
                ContainerSawbench.this.consumeMaterial();
                super.onTake(player, stack);
            }
        });

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 143 + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 201));
        }

        this.addDataSlot(this.selectedPage);
        this.addDataSlot(this.selectedSlot);
    }

    /**
     * Gets the block state a stack can be cut from, or null if the stack is not an acceptable material.
     */
    @Nullable
    public static BlockState getMaterialState(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem))
            return null;
        Block block = blockItem.getBlock();
        BlockState state = block.defaultBlockState();
        if (block instanceof SlabBlock || block == Blocks.GLASS || state.is(net.minecraftforge.common.Tags.Blocks.GLASS))
            return state;
        if (block instanceof EntityBlock)
            return null;
        return state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO) ? state : null;
    }

    public int getSelectedPage() {
        return this.selectedPage.get();
    }

    public int getSelectedSlot() {
        return this.selectedSlot.get();
    }

    @Nullable
    public EnumShape getSelectedShape() {
        int page = this.selectedPage.get();
        int slot = this.selectedSlot.get();
        if (page >= 0 && page < ShapePage.PAGES.size()) {
            var shapePage = ShapePage.PAGES.get(page);
            if (slot >= 0 && slot < shapePage.size())
                return shapePage.get(slot);
        }
        return null;
    }

    /**
     * @return the number of material items consumed per craft for the current selection.
     */
    public int getMaterialMultiple() {
        var shape = this.getSelectedShape();
        if (shape == null)
            return 0;
        var factor = this.materialContainer.getItem(0).getItem() instanceof BlockItem bi && bi.getBlock() instanceof SlabBlock ? 2 : 1;
        return factor * ShapePage.getMaterialCost(shape);
    }

    public int getResultMultiple() {
        var shape = this.getSelectedShape();
        return shape == null ? 0 : ShapePage.getItemsProduced(shape);
    }

    /**
     * Selects a page, restoring the last shape selected on it.
     */
    public void selectPage(int page) {
        if (page >= 0 && page < ShapePage.PAGES.size()) {
            this.selectShape(page, this.selectedSlotPerPage[page]);
        }
    }

    public void selectShape(int page, int slot) {
        if (page >= 0 && page < ShapePage.PAGES.size() && slot >= 0 && slot < ShapePage.PAGES.get(page).size()) {
            this.selectedPage.set(page);
            this.selectedSlot.set(slot);
            this.selectedSlotPerPage[page] = slot;
            this.setupResult();
        }
    }

    @Override
    public boolean clickMenuButton(@NotNull Player player, int id) {
        int page = id / PAGE_STRIDE;
        int slot = id % PAGE_STRIDE;
        if (page < 0 || page >= ShapePage.PAGES.size() || slot >= ShapePage.PAGES.get(page).size())
            return false;
        this.selectShape(page, slot);
        return true;
    }

    @Override
    public void slotsChanged(@NotNull Container container) {
        super.slotsChanged(container);
        if (container == this.materialContainer)
            this.setupResult();
    }

    private void setupResult() {
        var shape = this.getSelectedShape();
        var material = this.materialContainer.getItem(0);
        var state = getMaterialState(material);
        if (shape != null && state != null && material.getCount() >= this.getMaterialMultiple()) {
            this.resultContainer.setItem(0, shape == EnumShape.CLADDING_SHEET
                    ? ArchitectureMod.CONTENT.itemCladding.newStack(state, this.getResultMultiple())
                    : ItemShape.createStack(shape, state, this.getResultMultiple()));
        } else {
            this.resultContainer.setItem(0, ItemStack.EMPTY);
        }
        this.broadcastChanges();
    }

    private void consumeMaterial() {
        this.materialContainer.removeItem(0, this.getMaterialMultiple());
        this.setupResult();
    }

    @Override
    @NotNull
    public ItemStack quickMoveStack(@NotNull Player player, int index) {
        var slot = this.slots.get(index);
        if (!slot.hasItem())
            return ItemStack.EMPTY;
        var stack = slot.getItem();
        var original = stack.copy();
        if (index == RESULT_SLOT) {
            if (!this.moveItemStackTo(stack, PLAYER_SLOTS_START, PLAYER_SLOTS_END, true))
                return ItemStack.EMPTY;
            slot.onQuickCraft(stack, original);
        } else if (index == MATERIAL_SLOT) {
            if (!this.moveItemStackTo(stack, PLAYER_SLOTS_START, PLAYER_SLOTS_END, false))
                return ItemStack.EMPTY;
        } else if (getMaterialState(stack) != null) {
            if (!this.moveItemStackTo(stack, MATERIAL_SLOT, MATERIAL_SLOT + 1, false))
                return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty())
            slot.setByPlayer(ItemStack.EMPTY);
        slot.setChanged();
        if (stack.getCount() == original.getCount())
            return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        this.resultContainer.removeItemNoUpdate(0);
        this.access.execute((level, pos) -> this.clearContainer(player, this.materialContainer));
        if (this.access == ContainerLevelAccess.NULL)
            this.clearContainer(player, this.materialContainer);
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(this.access, player, ArchitectureMod.CONTENT.blockSawbench);
    }
}
