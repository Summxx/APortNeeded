package com.summax.apn.architecture.common.shape;

import com.summax.apn.architecture.common.ArchitectureMod;
import com.summax.apn.architecture.common.block.BlockArchitecture;
import com.summax.apn.architecture.common.shape.placement.IShapePlacementLogic;
import com.summax.apn.architecture.common.shape.placement.ShapePlacementLogicSideTurn;
import com.summax.apn.architecture.common.shape.transformation.IShapeTransformationResolver;
import com.summax.apn.architecture.common.shape.transformation.ShapeTransformationResolverSideTurn;
import com.summax.apn.architecture.core.model.mesh.IMesh;
import com.summax.apn.architecture.core.model.mesh.PolygonData;
import com.summax.apn.architecture.core.model.voxelize.IVoxelizer;
import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum EnumShape {

    ROOF_TILE("roof_tile"),
    ROOF_OUTER_CORNER("roof_outer_corner"),
    ROOF_INNER_CORNER("roof_inner_corner"),
    ROOF_RIDGE("roof_ridge"),
    ROOF_SMART_RIDGE("roof_smart_ridge"),
    ROOF_VALLEY("roof_valley"),
    ROOF_SMART_VALLEY("roof_smart_valley"),

    ROOF_OVERHANG("roof_overhang"),
    ROOF_OVERHANG_OUTER_CORNER("roof_overhang_outer_corner"),
    ROOF_OVERHANG_INNER_CORNER("roof_overhang_inner_corner"),

    CYLINDER("cylinder"),
    CYLINDER_HALF("cylinder_half"),
    CYLINDER_QUARTER("cylinder_quarter"),
    CYLINDER_LARGE_QUARTER("cylinder_large_quarter"),
    ANTICYLINDER_LARGE_QUARTER("anticylinder_large_quarter"),
    PILLAR("pillar"),
    POST("post"),
    POLE("pole"),

    BEVELLED_OUTER_CORNER("bevelled_outer_corner"),
    BEVELLED_INNER_CORNER("bevelled_inner_corner"),

    PILLAR_BASE("pillar_base"),
    DORIC_CAPITAL("doric_capital"),
    IONIC_CAPITAL("ionic_capital"),
    CORINTHIAN_CAPITAL("corinthian_capital"),
    DORIC_TRIGLYPH("doric_triglyph"),
    DORIC_TRIGLYPH_CORNER("doric_triglyph_corner"),
    DORIC_METOPE("doric_metope"),
    ARCHITRAVE("architrave"),
    ARCHITRAVE_CORNER("architrave_corner"),


    SPHERE_FULL("sphere_full"),
    SPHERE_HALF("sphere_half"),
    SPHERE_QUARTER("sphere_quarter"),
    SPHERE_EIGHTH("sphere_eighth"),
    SPHERE_EIGHTH_LARGE("sphere_eighth_large"),
    SPHERE_EIGHTH_LARGE_REV("sphere_eighth_large_rev"),

    ROOF_OVERHANG_GABLE_LH("roof_overhang_gable_lh"),
    ROOF_OVERHANG_GABLE_RH("roof_overhang_gable_rh"),
    ROOF_OVERHANG_GABLE_END_LH("roof_overhang_gable_end_lh"),
    ROOF_OVERHANG_GABLE_END_RH("roof_overhang_gable_end_rh"),
    ROOF_OVERHANG_RIDGE("roof_overhang_ridge"),
    ROOF_OVERHANG_VALLEY("roof_overhang_valley"),

    CORNICE_LH("cornice_lh"),
    CORNICE_RH("cornice_rh"),
    CORNICE_END_LH("cornice_end_lh"),
    CORNICE_END_RH("cornice_end_rh"),
    CORNICE_RIDGE("cornice_ridge"),
    CORNICE_VALLEY("cornice_valley"),
    CORNICE_BOTTOM("cornice_bottom"),

    CLADDING_SHEET("cladding_sheet", false),

    ARCH_D1("arch_d1"),
    ARCH_D2("arch_d2"),
    ARCH_D3A("arch_d3a"),
    ARCH_D3B("arch_d3b"),
    ARCH_D3C("arch_d3c"),
    ARCH_D4A("arch_d4a"),
    ARCH_D4B("arch_d4b"),
    ARCH_D4C("arch_d4c"),

    BANISTER_PLAIN_BOTTOM("banister_plain_bottom"),
    BANISTER_PLAIN("banister_plain"),
    BANISTER_PLAIN_TOP("banister_plain_top"),

    BALUSTRADE_FANCY("balustrade_fancy"),
    BALUSTRADE_FANCY_CORNER("balustrade_fancy_corner"),
    BALUSTRADE_FANCY_WITH_NEWEL("balustrade_fancy_with_newel"),
    BALUSTRADE_FANCY_NEWEL("balustrade_fancy_newel"),

    BALUSTRADE_PLAIN("balustrade_plain"),
    BALUSTRADE_PLAIN_OUTER_CORNER("balustrade_plain_outer_corner"),
    BALUSTRADE_PLAIN_WITH_NEWEL("balustrade_plain_with_newel"),

    BANISTER_PLAIN_END("banister_plain_end"),

    BANISTER_FANCY_NEWEL_TALL("banister_fancy_newel_tall"),

    BALUSTRADE_PLAIN_INNER_CORNER("balustrade_plain_inner_corner"),
    BALUSTRADE_PLAIN_END("balustrade_plain_end"),

    BANISTER_FANCY_BOTTOM("banister_fancy_bottom"),
    BANISTER_FANCY("banister_fancy"),
    BANISTER_FANCY_TOP("banister_fancy_top"),
    BANISTER_FANCY_END("banister_fancy_end"),

    BANISTER_PLAIN_INNER_CORNER("banister_plain_inner_corner"),

    SLAB("slab"),
    STAIRS("stairs"),
    STAIRS_OUTER_CORNER("stairs_outer_corner"),
    STAIRS_INNER_CORNER("stairs_inner_corner"),
    SLOPE_TILE_A1("slope_tile_a1"),
    SLOPE_TILE_A2("slope_tile_a2"),
    SLOPE_TILE_B1("slope_tile_b1"),
    SLOPE_TILE_B2("slope_tile_b2"),
    SLOPE_TILE_B3("slope_tile_b3"),
    SLOPE_TILE_C1("slope_tile_c1"),
    SLOPE_TILE_C2("slope_tile_c2"),
    SLOPE_TILE_C3("slope_tile_c3"),
    SLOPE_TILE_C4("slope_tile_c4"),
    ANGLED_ROOF_RIDGE("angled_roof_ridge"),
    DOUBLE_ROOF_TILE("double_roof_tile"),
    SQUARE_SE("square_se"),
    SLAB_SE("slab_se"),
    ROOF_TILE_SE("roof_tile_se"),
    SLOPE_TILE_A1_SE("slope_tile_a1_se"),
    SLOPE_TILE_A2_SE("slope_tile_a2_se"),
    SLOPE_TILE_B1_SE("slope_tile_b1_se"),
    SLOPE_TILE_B2_SE("slope_tile_b2_se"),
    SLOPE_TILE_B3_SE("slope_tile_b3_se"),
    SLOPE_TILE_C1_SE("slope_tile_c1_se"),
    SLOPE_TILE_C2_SE("slope_tile_c2_se"),
    SLOPE_TILE_C3_SE("slope_tile_c3_se"),
    SLOPE_TILE_C4_SE("slope_tile_c4_se");


    private static final Map<String, EnumShape> NAME_LOOKUP = Arrays.stream(values())
            .collect(Collectors.toMap(EnumShape::getName, Function.identity()));
    private static final Map<ResourceLocation, EnumShape> ID_LOOKUP = Arrays.stream(values())
            .collect(Collectors.toMap(EnumShape::getId, Function.identity()));
    private final String name;
    private final String localizationKey;
    private final ResourceLocation id;
    private final IShapePlacementLogic<?> placementLogic;
    private final IShapeTransformationResolver transformationResolver;
    private final ShapeTable.Data definition;

    EnumShape(String name) {
        this(name, true);
    }

    /**
     * @param implemented false for shapes that can't be placed (cladding), they stay hidden.
     */
    EnumShape(String name, boolean implemented) {
        this.name = name;
        this.localizationKey = String.format("shape.%s.%s", ArchitectureMod.MOD_ID, name);
        this.id = new ResourceLocation(ArchitectureMod.MOD_ID, String.format("shape/%s", name));
        this.definition = ShapeTable.get(name);
        this.placementLogic = implemented ? new ShapePlacementLogicSideTurn(this.definition) : null;
        this.transformationResolver = implemented
                ? new ShapeTransformationResolverSideTurn(ShapePlacementLogicSideTurn.placementOffset(name))
                : null;
    }

    /**
     * Gets the definition of this shape from the shape table.
     */
    public ShapeTable.Data getDefinition() {
        return this.definition;
    }

    /**
     * Gets the EnumShape with the given name.
     *
     * @param name The name of the EnumShape.
     * @return The EnumShape with the given name.
     * @throws NullPointerException if there is no EnumShape with the given name.
     */
    public static EnumShape byName(String name) {
        var shape = NAME_LOOKUP.get(name);
        return Objects.requireNonNull(shape, "No shape with name " + name);
    }

    /**
     * Gets the EnumShape with the given id.
     *
     * @param id The id of the EnumShape.
     * @return The EnumShape with the given id.
     * @throws NullPointerException if there is no EnumShape with the given id.
     */
    public static EnumShape byId(ResourceLocation id) {
        var shape = ID_LOOKUP.get(id);
        return Objects.requireNonNull(shape, "No shape with id " + id);
    }

    public String getName() {
        return this.name;
    }

    public ResourceLocation getId() {
        return this.id;
    }

    /**
     * @return true for the glowing variants.
     */
    public boolean isGlowing() {
        return this.name.endsWith("_se");
    }

    public String getLocalizationKey() {
        return this.localizationKey;
    }

    public <T extends BlockArchitecture> IShapePlacementLogic<T> getPlacementLogic() {
        // We're just using the minimum bounds of the interface so this is perfectly safe
        // noinspection unchecked
        return (IShapePlacementLogic<T>) this.placementLogic;
    }

    public IShapeTransformationResolver getTransformationResolver() {
        return this.transformationResolver;
    }

    private volatile Boolean acceptsCladding;

    public boolean acceptsCladding() {
        var accepts = this.acceptsCladding;
        if (accepts == null) {
            var mesh = this.getMesh();
            accepts = this.definition != null && switch (this.definition.kind()) {
                case ROOF -> true;
                case MODEL, BANISTER -> mesh != null && mesh.getFaces().stream()
                        .flatMap(f -> f.getPolygons().stream())
                        .anyMatch(p -> p.getPolygonData().textureIndex() >= 2);
            };
            this.acceptsCladding = accepts;
        }
        return accepts;
    }

    public IMesh<String, PolygonData> getMesh() {
        return ShapeMeshes.getMesh(this);
    }

    public IVoxelizer getVoxelizer() {
        return ShapeMeshes.getVoxelizer(this);
    }
}
