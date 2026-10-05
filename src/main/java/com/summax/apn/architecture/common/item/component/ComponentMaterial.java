package com.summax.apn.architecture.common.item.component;

import com.summax.apn.architecture.common.block.entity.BlockEntityShape;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a set of materials for an item, consisting of a base and secondary block state.
 *
 * @param base      The base block state.
 * @param secondary The secondary block state.
 */
public record ComponentMaterial(@NotNull BlockState base, @NotNull BlockState secondary) {
    public static final Codec<ComponentMaterial> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
            BlockState.CODEC.fieldOf("base").forGetter(ComponentMaterial::base),
            BlockState.CODEC.fieldOf("secondary").forGetter(ComponentMaterial::secondary)
    ).apply(instance, ComponentMaterial::new));

    public static final ComponentMaterial DEFAULT = new ComponentMaterial(Blocks.OAK_PLANKS.defaultBlockState(), Blocks.AIR.defaultBlockState());

    private static final String TAG_KEY = "material";

    /**
     * Reads the material stored on the given stack, or {@link #DEFAULT} if none is present.
     */
    public static ComponentMaterial get(ItemStack stack) {
        var tag = stack.getTag();
        if (tag == null) {
            return DEFAULT;
        }
        var materialTag = tag.get(TAG_KEY);
        if (materialTag == null) {
            return DEFAULT;
        }
        // Cache by tag identity, the tag gets replaced rather than mutated when the material changes.
        var cached = CACHE.getIfPresent(materialTag);
        if (cached == null) {
            cached = CODEC.parse(NbtOps.INSTANCE, materialTag).result().orElse(DEFAULT);
            CACHE.put(materialTag, cached);
        }
        return cached;
    }

    /**
     * Decoded materials by tag instance, weak keys so dropped items don't stay in memory.
     */
    private static final Cache<Tag, ComponentMaterial> CACHE = CacheBuilder.newBuilder().weakKeys().maximumSize(4096).build();

    /**
     * Stores the given material on the stack.
     */
    public static void set(ItemStack stack, ComponentMaterial material) {
        CODEC.encodeStart(NbtOps.INSTANCE, material).result().ifPresent(t -> stack.getOrCreateTag().put(TAG_KEY, t));
    }

    public ComponentMaterial(BlockState base) {
        this(base, Blocks.AIR.defaultBlockState());
    }

    public boolean hasBase() {
        return !this.base().isAir();
    }

    public boolean hasSecondary() {
        return !this.secondary().isAir();
    }

    public BlockState safeBase() {
        return this.hasBase() && BlockEntityShape.isValidMaterial(this.base()) ? this.base() : Blocks.OAK_PLANKS.defaultBlockState();
    }

    public BlockState safeSecondary() {
        return this.hasSecondary() && BlockEntityShape.isValidMaterial(this.secondary()) ? this.secondary() : this.safeBase();
    }
}
