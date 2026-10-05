package com.summax.apn.architecture.common.shape.orientation;

import com.google.common.collect.ImmutableList;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Lateral offset of a shape along its local X axis.
 */
public enum EnumLateralOffset implements StringRepresentable {
    CENTER("center", 0),
    NEGATIVE("negative", -1),
    POSITIVE("positive", 1);

    private static final Collection<EnumLateralOffset> VALUES = ImmutableList.copyOf(values());

    private final String name;
    private final int sign;

    EnumLateralOffset(String name, int sign) {
        this.name = name;
        this.sign = sign;
    }

    public static Collection<EnumLateralOffset> getValues() {
        return VALUES;
    }

    /**
     * @return the offset for the given signed distance, CENTER for zero.
     */
    public static EnumLateralOffset forSign(double value) {
        return value == 0 ? CENTER : value < 0 ? NEGATIVE : POSITIVE;
    }

    public int getSign() {
        return this.sign;
    }

    @Override
    @NotNull
    public String getSerializedName() {
        return this.name;
    }
}
