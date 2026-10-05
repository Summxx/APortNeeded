package com.summax.apn.decoration;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;

/**
 * A full bright block of solid colour, glowing at light level 15.
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
