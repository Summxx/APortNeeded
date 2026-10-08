package com.summax.apn.architecture.common.compat;

import com.summax.apn.architecture.common.block.entity.BlockEntityShape;
import com.summax.apn.architecture.common.block.entity.PendingMaterials;
import com.summax.apn.architecture.core.ArchitectureLog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Keeps shape materials through Building Gadgets 2 copy and paste.
 */
public final class BuildingGadgetsCompat {

    private static final ResourceLocation COPY_PASTE = new ResourceLocation("buildinggadgets2", "gadget_copy_paste");

    private static Method getUuid, getData, addTags, peekTags, removeTags;
    private static Constructor<?> newTagPos;
    private static Field tagField, posField;
    private static boolean ready, failed;

    private BuildingGadgetsCompat() {
    }

    public static void onCopy(Player player, boolean empty) {
        if (!(player.level() instanceof ServerLevel level) || !init())
            return;
        var gadget = heldCopyGadget(player);
        var tag = gadget.getTag();
        if (tag == null)
            return;
        try {
            var tags = new ArrayList<Object>();
            if (!empty && tag.contains("copystart") && tag.contains("copyend")) {
                var start = NbtUtils.readBlockPos(tag.getCompound("copystart"));
                var box = BoundingBox.fromCorners(start, NbtUtils.readBlockPos(tag.getCompound("copyend")));
                for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
                    for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
                        var chunk = level.getChunkSource().getChunkNow(cx, cz);
                        if (chunk == null)
                            continue;
                        for (var be : chunk.getBlockEntities().values()) {
                            if (be instanceof BlockEntityShape shape && box.isInside(be.getBlockPos()))
                                tags.add(newTagPos.newInstance(shape.getMaterialsTag(), be.getBlockPos().subtract(start)));
                        }
                    }
                }
            }
            var uuid = getUuid.invoke(null, gadget);
            var data = getData.invoke(null, level.getServer().overworld());
            if (tags.isEmpty())
                removeTags.invoke(data, uuid);
            else
                addTags.invoke(data, uuid, tags);
        } catch (ReflectiveOperationException | RuntimeException e) {
            disable(e);
        }
    }

    public static void onPaste(Level level, ItemStack gadget, BlockPos origin) {
        if (!(level instanceof ServerLevel serverLevel) || !isCopyGadget(gadget) || !init())
            return;
        try {
            var data = getData.invoke(null, serverLevel.getServer().overworld());
            var tags = (List<?>) peekTags.invoke(data, getUuid.invoke(null, gadget));
            if (tags == null || tags.isEmpty())
                return;
            var materials = new HashMap<BlockPos, CompoundTag>();
            for (var entry : tags)
                materials.put(origin.offset((BlockPos) posField.get(entry)), (CompoundTag) tagField.get(entry));
            PendingMaterials.addAll(level, materials);
        } catch (ReflectiveOperationException | RuntimeException e) {
            disable(e);
        }
    }

    private static boolean isCopyGadget(ItemStack stack) {
        return !stack.isEmpty() && COPY_PASTE.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    private static ItemStack heldCopyGadget(Player player) {
        for (var hand : InteractionHand.values()) {
            var stack = player.getItemInHand(hand);
            if (isCopyGadget(stack))
                return stack;
        }
        return ItemStack.EMPTY;
    }

    private static synchronized boolean init() {
        if (ready || failed)
            return ready;
        try {
            var nbt = Class.forName("com.direwolf20.buildinggadgets2.util.GadgetNBT");
            var data = Class.forName("com.direwolf20.buildinggadgets2.common.worlddata.BG2Data");
            var tagPos = Class.forName("com.direwolf20.buildinggadgets2.util.datatypes.TagPos");
            getUuid = nbt.getMethod("getUUID", ItemStack.class);
            getData = data.getMethod("get", ServerLevel.class);
            addTags = data.getMethod("addToTEMap", java.util.UUID.class, ArrayList.class);
            peekTags = data.getMethod("peekTEMap", java.util.UUID.class);
            removeTags = data.getMethod("getTEMap", java.util.UUID.class);
            newTagPos = tagPos.getConstructor(CompoundTag.class, BlockPos.class);
            tagField = tagPos.getField("tag");
            posField = tagPos.getField("pos");
            ready = true;
        } catch (ReflectiveOperationException | LinkageError e) {
            disable(e);
        }
        return ready;
    }

    private static void disable(Throwable e) {
        ready = false;
        failed = true;
        ArchitectureLog.error("Building Gadgets 2 support disabled, its API may have changed.", e);
    }
}
