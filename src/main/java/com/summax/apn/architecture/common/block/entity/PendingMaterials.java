package com.summax.apn.architecture.common.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Materials waiting for a shape to be placed at a position.
 */
public final class PendingMaterials {

    private static final long LIFETIME = 20 * 60 * 30;
    private static final Map<Level, Map<BlockPos, Entry>> PENDING = new WeakHashMap<>();

    private record Entry(CompoundTag tag, long time) {
    }

    private PendingMaterials() {
    }

    public static synchronized void addAll(Level level, Map<BlockPos, CompoundTag> materials) {
        var map = PENDING.computeIfAbsent(level, l -> new HashMap<>());
        long now = level.getGameTime();
        map.values().removeIf(e -> now - e.time() > LIFETIME);
        materials.forEach((pos, tag) -> map.put(pos.immutable(), new Entry(tag, now)));
    }

    @Nullable
    public static synchronized CompoundTag take(Level level, BlockPos pos) {
        var map = PENDING.get(level);
        if (map == null || map.isEmpty())
            return null;
        var entry = map.remove(pos);
        return entry == null || level.getGameTime() - entry.time() > LIFETIME ? null : entry.tag();
    }
}
