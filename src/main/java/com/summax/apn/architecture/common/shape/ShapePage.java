package com.summax.apn.architecture.common.shape;

import com.google.common.collect.ImmutableList;
import com.summax.apn.architecture.common.ArchitectureMod;

import java.util.Arrays;
import java.util.List;

import static com.summax.apn.architecture.common.shape.EnumShape.*;

/**
 * A page of shapes displayed in the sawbench, shapes without placement logic are left out until they are implemented.
 */
public record ShapePage(String translationKey, List<EnumShape> shapes) {

    public static final List<ShapePage> PAGES = createPages(
            new ShapePage("roofing",
                    ROOF_TILE, ROOF_OUTER_CORNER, ROOF_INNER_CORNER, ROOF_RIDGE, ROOF_SMART_RIDGE, ROOF_VALLEY,
                    ROOF_SMART_VALLEY, ROOF_OVERHANG, ROOF_OVERHANG_OUTER_CORNER, ROOF_OVERHANG_INNER_CORNER,
                    ROOF_OVERHANG_GABLE_LH, ROOF_OVERHANG_GABLE_RH, ROOF_OVERHANG_GABLE_END_LH, ROOF_OVERHANG_GABLE_END_RH,
                    ROOF_OVERHANG_RIDGE, ROOF_OVERHANG_VALLEY, BEVELLED_OUTER_CORNER, BEVELLED_INNER_CORNER),
            new ShapePage("rounded",
                    CYLINDER, CYLINDER_HALF, CYLINDER_QUARTER, CYLINDER_LARGE_QUARTER, ANTICYLINDER_LARGE_QUARTER,
                    PILLAR, POST, POLE, SPHERE_FULL, SPHERE_HALF, SPHERE_QUARTER, SPHERE_EIGHTH, SPHERE_EIGHTH_LARGE,
                    SPHERE_EIGHTH_LARGE_REV),
            new ShapePage("classical",
                    PILLAR_BASE, PILLAR, DORIC_CAPITAL, DORIC_TRIGLYPH, DORIC_TRIGLYPH_CORNER, DORIC_METOPE,
                    IONIC_CAPITAL, CORINTHIAN_CAPITAL, ARCHITRAVE, ARCHITRAVE_CORNER, CORNICE_LH, CORNICE_RH,
                    CORNICE_END_LH, CORNICE_END_RH, CORNICE_RIDGE, CORNICE_VALLEY, CORNICE_BOTTOM),
            new ShapePage("arches",
                    ARCH_D1, ARCH_D2, ARCH_D3A, ARCH_D3B, ARCH_D3C, ARCH_D4A, ARCH_D4B, ARCH_D4C),
            new ShapePage("railings",
                    BALUSTRADE_PLAIN, BALUSTRADE_PLAIN_OUTER_CORNER, BALUSTRADE_PLAIN_INNER_CORNER,
                    BALUSTRADE_PLAIN_WITH_NEWEL, BALUSTRADE_PLAIN_END, BANISTER_PLAIN_TOP, BANISTER_PLAIN,
                    BANISTER_PLAIN_BOTTOM, BANISTER_PLAIN_END, BANISTER_PLAIN_INNER_CORNER, BALUSTRADE_FANCY,
                    BALUSTRADE_FANCY_CORNER, BALUSTRADE_FANCY_WITH_NEWEL, BALUSTRADE_FANCY_NEWEL, BANISTER_FANCY_TOP,
                    BANISTER_FANCY, BANISTER_FANCY_BOTTOM, BANISTER_FANCY_END, BANISTER_FANCY_NEWEL_TALL),
            new ShapePage("other",
                    CLADDING_SHEET, SLAB, STAIRS, STAIRS_OUTER_CORNER, STAIRS_INNER_CORNER, SLOPE_TILE_A1, SLOPE_TILE_A2,
                    SLOPE_TILE_B1, SLOPE_TILE_B2, SLOPE_TILE_B3, SLOPE_TILE_C1, SLOPE_TILE_C2, SLOPE_TILE_C3,
                    SLOPE_TILE_C4, ANGLED_ROOF_RIDGE, DOUBLE_ROOF_TILE),
            new ShapePage("glow",
                    SQUARE_SE, SLAB_SE, ROOF_TILE_SE, SLOPE_TILE_A1_SE, SLOPE_TILE_A2_SE, SLOPE_TILE_B1_SE,
                    SLOPE_TILE_B2_SE, SLOPE_TILE_B3_SE, SLOPE_TILE_C1_SE, SLOPE_TILE_C2_SE, SLOPE_TILE_C3_SE,
                    SLOPE_TILE_C4_SE)
    );

    public ShapePage(String name, EnumShape... shapes) {
        this(String.format("%s.shapepage.%s", ArchitectureMod.MOD_ID, name), List.of(shapes));
    }

    private static List<ShapePage> createPages(ShapePage... pages) {
        return Arrays.stream(pages)
                .map(p -> new ShapePage(p.translationKey(), p.shapes().stream()
                        // Cladding can't be placed, the sawbench makes cladding items out of it.
                        .filter(s -> s.getPlacementLogic() != null || s == CLADDING_SHEET)
                        .collect(ImmutableList.toImmutableList())))
                .filter(p -> !p.shapes().isEmpty())
                .collect(ImmutableList.toImmutableList());
    }

    public int size() {
        return this.shapes.size();
    }

    public EnumShape get(int index) {
        return this.shapes.get(index);
    }

    /**
     * @return the index of the shape's icon in the sawbench icon atlas.
     */
    public static int getIconIndex(EnumShape shape) {
        return shape.getDefinition().id();
    }

    /**
     * @return the number of material blocks consumed to craft the shape.
     */
    public static int getMaterialCost(EnumShape shape) {
        return shape.getDefinition().materialUsed();
    }

    /**
     * @return the number of shape items produced per craft.
     */
    public static int getItemsProduced(EnumShape shape) {
        return shape.getDefinition().itemsProduced();
    }
}
