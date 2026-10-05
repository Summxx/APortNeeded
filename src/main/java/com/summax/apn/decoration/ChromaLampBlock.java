package com.summax.apn.decoration;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;

/**
 * Chroma lamp: a flat, unshaded, full bright block of solid colour that glows at full
 * light level. Each colour has its own flat texture so the colour survives in microblocks and copycat blocks.
 */
public class ChromaLampBlock extends Block {

    /**
     * Every colour glows at full light level so they can all be used as light sources.
     */
    public static final int LIGHT_LEVEL = 15;

    private final DyeColor colour;

    public ChromaLampBlock(DyeColor colour, Properties properties) {
        super(properties);
        this.colour = colour;
    }

    public DyeColor getColour() {
        return this.colour;
    }
}
