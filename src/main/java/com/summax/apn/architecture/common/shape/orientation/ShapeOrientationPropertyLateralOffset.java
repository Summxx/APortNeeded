package com.summax.apn.architecture.common.shape.orientation;

public class ShapeOrientationPropertyLateralOffset extends ShapeOrientationProperty<EnumLateralOffset> {

    public final static ShapeOrientationPropertyLateralOffset INSTANCE = new ShapeOrientationPropertyLateralOffset();

    protected ShapeOrientationPropertyLateralOffset() {
        super("lateral_offset", EnumLateralOffset.class, EnumLateralOffset.getValues());
    }

    public static Value<EnumLateralOffset> of(EnumLateralOffset value) {
        return INSTANCE.findValue(value);
    }
}
