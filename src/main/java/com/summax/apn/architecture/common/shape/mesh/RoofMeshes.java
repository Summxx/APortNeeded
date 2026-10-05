package com.summax.apn.architecture.common.shape.mesh;

import com.summax.apn.architecture.core.model.mesh.IMesh;
import com.summax.apn.architecture.core.model.mesh.PolygonData;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates the roof meshes, including the geometry joining neighbouring ridges and valleys.
 */
public final class RoofMeshes {

    private static final int OUTER = 1;
    private static final int INNER = 2;

    private static final double[] UNIT_Y = {0, 1, 0}, UNIT_NY = {0, -1, 0};
    private static final double[] UNIT_X = {1, 0, 0}, UNIT_NX = {-1, 0, 0};
    private static final double[] UNIT_Z = {0, 0, 1}, UNIT_NZ = {0, 0, -1};
    private static final double[] UNIT_PYNZ = {0, 0.707, -0.707}, UNIT_PYPZ = {0, 0.707, 0.707};
    private static final double[] UNIT_PXPY = {0.707, 0.707, 0}, UNIT_NXPY = {-0.707, 0.707, 0};

    /**
     * Local sides of a roof as mask bits: left (+X), right (-X), front (-Z), back (+Z).
     */
    public static final int LEFT = 1, RIGHT = 2, FRONT = 4, BACK = 8;

    public enum Join {
        RIDGE("roof_ridge", "roof_smart_ridge"),
        RIDGE_OR_SLOPE("roof_ridge", "roof_smart_ridge", "roof_tile", "roof_outer_corner", "roof_inner_corner"),
        VALLEY("roof_valley", "roof_smart_valley"),
        VALLEY_OR_SLOPE("roof_valley", "roof_smart_valley", "roof_tile", "roof_inner_corner");

        private final java.util.Set<String> shapes;

        Join(String... shapes) {
            this.shapes = java.util.Set.of(shapes);
        }

        public boolean accepts(String shape) {
            return this.shapes.contains(shape);
        }
    }

    /**
     * @return what the roof joins on each local side, or null if the shape never connects.
     */
    public static Join[] connectionSides(String name) {
        return switch (name) {
            case "roof_tile" -> new Join[]{null, null, Join.RIDGE, Join.VALLEY};
            case "roof_outer_corner" -> new Join[]{Join.RIDGE, null, Join.RIDGE, null};
            case "roof_inner_corner" -> new Join[]{null, Join.VALLEY, null, Join.VALLEY};
            case "roof_ridge" -> new Join[]{null, null, Join.RIDGE_OR_SLOPE, Join.RIDGE_OR_SLOPE};
            case "roof_smart_ridge" -> new Join[]{Join.RIDGE_OR_SLOPE, Join.RIDGE_OR_SLOPE, Join.RIDGE_OR_SLOPE, Join.RIDGE_OR_SLOPE};
            case "roof_valley" -> new Join[]{null, null, Join.VALLEY_OR_SLOPE, Join.VALLEY_OR_SLOPE};
            case "roof_smart_valley" -> new Join[]{Join.VALLEY_OR_SLOPE, Join.VALLEY_OR_SLOPE, Join.VALLEY_OR_SLOPE, Join.VALLEY_OR_SLOPE};
            default -> null;
        };
    }

    private final ShapeMeshBuilder builder;
    private final int connections;
    private int texture;
    private double[] normal;
    private final List<double[]> vertices = new ArrayList<>();

    private RoofMeshes(String name, int connections) {
        this.builder = new ShapeMeshBuilder(name);
        this.connections = connections;
    }

    public static IMesh<String, PolygonData> create(String name) {
        return create(name, 0);
    }

    /**
     * @param connections the local sides with a roof to connect to.
     * @return the roof mesh, or null if the shape isn't a roof.
     */
    public static IMesh<String, PolygonData> create(String name, int connections) {
        var r = new RoofMeshes(name, connections);
        switch (name) {
            case "roof_tile", "roof_tile_se" -> r.renderSlope();
            case "slope_tile_a1", "slope_tile_a1_se" -> r.renderSlopeA1();
            case "slope_tile_a2", "slope_tile_a2_se" -> r.renderSlopeA2();
            case "slope_tile_b1", "slope_tile_b1_se" -> r.renderSlopeB1();
            case "slope_tile_b2", "slope_tile_b2_se" -> r.renderSlopeB2();
            case "slope_tile_b3", "slope_tile_b3_se" -> r.renderSlopeB3();
            case "slope_tile_c1", "slope_tile_c1_se" -> r.renderSlopeC1();
            case "slope_tile_c2", "slope_tile_c2_se" -> r.renderSlopeC2();
            case "slope_tile_c3", "slope_tile_c3_se" -> r.renderSlopeC3();
            case "slope_tile_c4", "slope_tile_c4_se" -> r.renderSlopeC4();
            case "roof_outer_corner" -> r.renderOuterCorner();
            case "roof_inner_corner" -> r.renderInnerCorner();
            case "roof_ridge" -> r.renderRidge();
            case "roof_smart_ridge" -> r.renderSmartRidge();
            case "roof_valley" -> r.renderValley();
            case "roof_smart_valley" -> r.renderSmartValley();
            default -> {
                return null;
            }
        }
        return r.builder.build();
    }

    // ------------------------------------------------------------------------------------- Shapes

    private boolean at(int side) {
        return (this.connections & side) != 0;
    }

    private void renderSlope() {
        boolean valley = at(BACK);
        // Sloping face
        beginNegZSlope();
        if (valley) {
            beginTriangle();
            vertex(1, 1, 1, 0, 0);
            vertex(1, 0, 0, 0, 1);
            vertex(0.5, 0.5, 0.5, 0.5, 0.5);
            newTriangle();
            vertex(1, 0, 0, 0, 1);
            vertex(0, 0, 0, 1, 1);
            vertex(0.5, 0.5, 0.5, 0.5, 0.5);
            newTriangle();
            vertex(0, 0, 0, 1, 1);
            vertex(0, 1, 1, 1, 0);
            vertex(0.5, 0.5, 0.5, 0.5, 0.5);
            endFace();
            connectValleyBack();
        } else {
            beginQuad();
            vertex(1, 1, 1, 0, 0);
            vertex(1, 0, 0, 0, 1);
            vertex(0, 0, 0, 1, 1);
            vertex(0, 1, 1, 1, 0);
            endFace();
        }
        // Other faces
        leftTriangle();
        rightTriangle();
        bottomQuad();
        if (!valley)
            backQuad();
        if (at(FRONT))
            connectRidgeFront();
    }

    private void renderSlopeA1() {
        renderVariableSlope(1.0, 0.5);
        renderVariableFaceLeft(0, 0.5);
        renderVariableTriangleLeft(0.5, 0.5);
        renderVariableFaceRight(0, 0.5);
        renderVariableTriangleRight(0.5, 0.5);
        renderVariableFrontFace(0.5);
        bottomQuad();
        backQuad();
    }

    private void renderSlopeA2() {
        renderVariableSlope(0.5, 0);
        renderVariableTriangleLeft(0, 0.5);
        renderVariableTriangleRight(0, 0.5);
        bottomQuad();
        renderVariableBackFace(0.5);
    }

    private void renderSlopeB1() {
        renderVariableSlope(1.0, 0.66666);
        renderVariableFaceLeft(0, 0.66666);
        renderVariableTriangleLeft(0.66666, 0.33333);
        renderVariableFaceRight(0, 0.66666);
        renderVariableTriangleRight(0.66666, 0.33333);
        renderVariableFrontFace(0.66666);
        bottomQuad();
        backQuad();
    }

    private void renderSlopeB2() {
        renderVariableSlope(0.66666, 0.33333);
        renderVariableFaceLeft(0, 0.33333);
        renderVariableTriangleLeft(0.33333, 0.33333);
        renderVariableFaceRight(0, 0.33333);
        renderVariableTriangleRight(0.33333, 0.33333);
        renderVariableFrontFace(0.33333);
        bottomQuad();
        renderVariableBackFace(0.66666);
    }

    private void renderSlopeB3() {
        renderVariableSlope(0.33333, 0);
        renderVariableTriangleLeft(0, 0.33333);
        renderVariableTriangleRight(0, 0.33333);
        bottomQuad();
        renderVariableBackFace(0.33333);
    }

    private void renderSlopeC1() {
        renderVariableSlope(1, 0.75);
        renderVariableFaceLeft(0, 0.75);
        renderVariableTriangleLeft(0.75, 0.25);
        renderVariableFaceRight(0, 0.75);
        renderVariableTriangleRight(0.75, 0.25);
        renderVariableFrontFace(0.75);
        bottomQuad();
        backQuad();
    }

    private void renderSlopeC2() {
        renderVariableSlope(0.75, 0.50);
        renderVariableFaceLeft(0, 0.50);
        renderVariableTriangleLeft(0.50, 0.25);
        renderVariableFaceRight(0, 0.50);
        renderVariableTriangleRight(0.50, 0.25);
        renderVariableFrontFace(0.50);
        bottomQuad();
        renderVariableBackFace(0.75);
    }

    private void renderSlopeC3() {
        renderVariableSlope(0.50, 0.25);
        renderVariableFaceLeft(0, 0.25);
        renderVariableTriangleLeft(0.25, 0.25);
        renderVariableFaceRight(0, 0.25);
        renderVariableTriangleRight(0.25, 0.25);
        renderVariableFrontFace(0.25);
        bottomQuad();
        renderVariableBackFace(0.50);
    }

    private void renderSlopeC4() {
        renderVariableSlope(0.25, 0);
        renderVariableTriangleLeft(0, 0.25);
        renderVariableTriangleRight(0, 0.25);
        bottomQuad();
        renderVariableBackFace(0.25);
    }

    private void renderVariableSlope(double start, double end) {
        double dy = end - start;
        double invLen = 1 / Math.sqrt(1 + dy * dy);
        beginInnerFaces(new double[]{0, invLen, invLen * dy});
        beginQuad();
        vertex(1, start, 1, 0, 0);
        vertex(1, end, 0, 0, 1);
        vertex(0, end, 0, 1, 1);
        vertex(0, start, 1, 1, 0);
        endFace();
    }

    private void renderVariableTriangleLeft(double offset, double height) {
        beginOuterFaces(UNIT_X);
        beginTriangle();
        vertex(1, offset + height, 1, 0, 0);
        vertex(1, offset, 1, 0, 1 - height);
        vertex(1, offset, 0, 1, 1 - height);
        endFace();
    }

    private void renderVariableTriangleRight(double offset, double height) {
        beginOuterFaces(UNIT_NX);
        beginTriangle();
        vertex(0, offset + height, 1, 1, 0);
        vertex(0, offset, 0, 0, 1 - height);
        vertex(0, offset, 1, 1, 1 - height);
        endFace();
    }

    private void renderVariableFaceLeft(double offset, double height) {
        beginOuterFaces(UNIT_NX);
        beginQuad();
        vertex(0, offset + height, 0, 0, 1 - height);
        vertex(0, offset, 0, 0, 1);
        vertex(0, offset, 1, 1, 1);
        vertex(0, offset + height, 1, 1, 1 - height);
        endFace();
    }

    private void renderVariableFaceRight(double offset, double height) {
        beginOuterFaces(UNIT_X);
        beginQuad();
        vertex(1, offset + height, 1, 0, 1 - height);
        vertex(1, offset, 1, 0, 1);
        vertex(1, offset, 0, 1, 1);
        vertex(1, offset + height, 0, 1, 1 - height);
        endFace();
    }

    private void renderVariableFrontFace(double height) {
        beginOuterFaces(UNIT_NZ);
        beginQuad();
        vertex(1, height, 0, 0, 1 - height);
        vertex(1, 0, 0, 0, 1);
        vertex(0, 0, 0, 1, 1);
        vertex(0, height, 0, 1, 1 - height);
        endFace();
    }

    private void renderVariableBackFace(double height) {
        beginOuterFaces(UNIT_Z);
        beginQuad();
        vertex(0, height, 1, 0, 1 - height);
        vertex(0, 0, 1, 0, 1);
        vertex(1, 0, 1, 1, 1);
        vertex(1, height, 1, 1, 1 - height);
        endFace();
    }

    private void renderOuterCorner() {
        // Front slope
        beginNegZSlope();
        beginTriangle();
        vertex(0, 1, 1, 1, 0);
        vertex(1, 0, 0, 0, 1);
        vertex(0, 0, 0, 1, 1);
        endFace();
        // Left slope
        beginPosXSlope();
        beginTriangle();
        vertex(0, 1, 1, 0, 0);
        vertex(1, 0, 1, 0, 1);
        vertex(1, 0, 0, 1, 1);
        endFace();
        // Back
        beginOuterFaces(UNIT_Z);
        beginTriangle();
        vertex(0, 1, 1, 0, 0);
        vertex(0, 0, 1, 0, 1);
        vertex(1, 0, 1, 1, 1);
        endFace();
        rightTriangle();
        bottomQuad();
        if (at(FRONT))
            connectRidgeFront();
        if (at(LEFT))
            connectRidgeLeft();
    }

    private void renderInnerCorner() {
        // Left slope
        beginPosXSlope();
        beginTriangle();
        vertex(0, 1, 0, 1, 0);
        vertex(0.5, 0.5, 0.5, 0.5, 0.5);
        vertex(1, 0, 0, 1, 1);
        endFace();
        // Front slope
        beginNegZSlope();
        beginTriangle();
        vertex(1, 1, 1, 0, 0);
        vertex(1, 0, 0, 0, 1);
        vertex(0.5, 0.5, 0.5, 0.5, 0.5);
        endFace();
        // Front triangle
        beginOuterFaces(UNIT_NZ);
        beginTriangle();
        vertex(0, 1, 0, 1, 0);
        vertex(1, 0, 0, 0, 1);
        vertex(0, 0, 0, 1, 1);
        endFace();
        leftTriangle();
        bottomQuad();
        if (at(BACK))
            connectValleyBack();
        else
            terminateValleyBack();
        if (at(RIGHT))
            connectValleyRight();
        else
            terminateValleyRight();
    }

    private void renderRidge() {
        // Front slope
        beginNegZSlope();
        beginQuad();
        vertex(1, 0.5, 0.5, 0, 0.5);
        vertex(1, 0, 0, 0, 1);
        vertex(0, 0, 0, 1, 1);
        vertex(0, 0.5, 0.5, 1, 0.5);
        endFace();
        // Other slopes
        ridgeBackSlope();
        ridgeFront(false);
        ridgeBack(false);
        ridgeLeftFace();
        ridgeRightFace();
        bottomQuad();
    }

    private void renderSmartRidge() {
        ridgeLeft();
        ridgeRight();
        ridgeBack(true);
        ridgeFront(true);
        bottomQuad();
    }

    private void renderValley() {
        connectValleyLeft();
        connectValleyRight();
        smartValleyFront();
        smartValleyBack();
        bottomQuad();
    }

    private void renderSmartValley() {
        smartValleyLeft();
        smartValleyRight();
        smartValleyFront();
        smartValleyBack();
        bottomQuad();
    }

    // ------------------------------------------------------------------------------------- Valleys

    private void smartValleyLeft() {
        if (at(LEFT))
            connectValleyLeft();
        else
            terminateValleyLeft();
    }

    private void terminateValleyLeft() {
        beginNegXSlope();
        beginTriangle();
        vertex(1, 1, 0, 0, 0);
        vertex(0.5, 0.5, 0.5, 0.5, 0.5);
        vertex(1, 1, 1, 1, 0);
        endFace();
        leftQuad();
    }

    private void smartValleyRight() {
        if (at(RIGHT))
            connectValleyRight();
        else
            terminateValleyRight();
    }

    private void smartValleyFront() {
        if (at(FRONT))
            connectValleyFront();
        else
            terminateValleyFront();
    }

    private void smartValleyBack() {
        if (at(BACK))
            connectValleyBack();
        else
            terminateValleyBack();
    }

    private void connectValleyFront() {
        beginPosXSlope();
        beginTriangle();
        vertex(0, 1, 0, 1, 0);
        vertex(0.5, 0.5, 0.5, 0.5, 0.5);
        vertex(0.5, 0.5, 0, 1, 0.5);
        endFace();
        beginNegXSlope();
        beginTriangle();
        vertex(1, 1, 0, 0, 0);
        vertex(0.5, 0.5, 0, 0, 0.5);
        vertex(0.5, 0.5, 0.5, 0.5, 0.5);
        endFace();
        valleyEndFront();
    }

    private void connectValleyBack() {
        beginPosXSlope();
        beginTriangle();
        vertex(0, 1, 1, 0, 0);
        vertex(0.5, 0.5, 1, 0, 0.5);
        vertex(0.5, 0.5, 0.5, 0.5, 0.5);
        endFace();
        beginNegXSlope();
        beginTriangle();
        vertex(1, 1, 1, 1, 0);
        vertex(0.5, 0.5, 0.5, 0.5, 0.5);
        vertex(0.5, 0.5, 1, 1, 0.5);
        endFace();
        valleyEndBack();
    }

    private void valleyEndFront() {
        beginOuterFaces(UNIT_NZ);
        beginTriangle();
        vertex(1, 1, 0, 0, 0);
        vertex(1, 0, 0, 0, 1);
        vertex(0.5, 0.5, 0, 0.5, 0.5);
        newTriangle();
        vertex(1, 0, 0, 0, 1);
        vertex(1, 0, 0, 1, 1);
        vertex(0.5, 0.5, 0, 0.5, 0.5);
        newTriangle();
        vertex(0, 0, 0, 1, 1);
        vertex(0, 1, 0, 1, 0);
        vertex(0.5, 0.5, 0, 0.5, 0.5);
        endFace();
    }

    private void valleyEndBack() {
        beginOuterFaces(UNIT_Z);
        beginTriangle();
        vertex(0, 1, 1, 0, 0);
        vertex(0, 0, 1, 0, 1);
        vertex(0.5, 0.5, 1, 0.5, 0.5);
        newTriangle();
        vertex(0, 0, 1, 0, 1);
        vertex(1, 0, 1, 1, 1);
        vertex(0.5, 0.5, 1, 0.5, 0.5);
        newTriangle();
        vertex(1, 0, 1, 1, 1);
        vertex(1, 1, 1, 1, 0);
        vertex(0.5, 0.5, 1, 0.5, 0.5);
        endFace();
    }

    private void terminateValleyRight() {
        beginPosXSlope();
        beginTriangle();
        vertex(0, 1, 1, 0, 0);
        vertex(0.5, 0.5, 0.5, 0.5, 0.5);
        vertex(0, 1, 0, 1, 0);
        endFace();
        rightQuad();
    }

    private void terminateValleyFront() {
        beginPosZSlope();
        beginTriangle();
        vertex(0, 1, 0, 0, 0);
        vertex(0.5, 0.5, 0.5, 0.5, 0.5);
        vertex(1, 1, 0, 1, 0);
        endFace();
        frontQuad();
    }

    private void terminateValleyBack() {
        beginNegZSlope();
        beginTriangle();
        vertex(1, 1, 1, 0, 0);
        vertex(0.5, 0.5, 0.5, 0.5, 0.5);
        vertex(0, 1, 1, 1, 0);
        endFace();
        backQuad();
    }

    private void connectValleyLeft() {
        beginPosZSlope();
        beginTriangle();
        vertex(0.5, 0.5, 0.5, 0.5, 0.5);
        vertex(1, 0.5, 0.5, 1, 0.5);
        vertex(1, 1, 0, 1, 0);
        endFace();
        beginNegZSlope();
        beginTriangle();
        vertex(0.5, 0.5, 0.5, 0.5, 0.5);
        vertex(1, 1, 1, 0, 0);
        vertex(1, 0.5, 0.5, 0, 0.5);
        endFace();
        valleyEndLeft();
    }

    private void connectValleyRight() {
        beginPosZSlope();
        beginTriangle();
        vertex(0.5, 0.5, 0.5, 0.5, 0.5);
        vertex(0, 1, 0, 0, 0);
        vertex(0, 0.5, 0.5, 0, 0.5);
        endFace();
        beginNegZSlope();
        beginTriangle();
        vertex(0.5, 0.5, 0.5, 0.5, 0.5);
        vertex(0, 0.5, 0.5, 1, 0.5);
        vertex(0, 1, 1, 1, 0);
        endFace();
        valleyEndRight();
    }

    private void valleyEndLeft() {
        beginOuterFaces(UNIT_X);
        beginTriangle();
        vertex(1, 1, 1, 0, 0);
        vertex(1, 0, 1, 0, 1);
        vertex(1, 0.5, 0.5, 0.5, 0.5);
        newTriangle();
        vertex(1, 0, 1, 0, 1);
        vertex(1, 0, 0, 1, 1);
        vertex(1, 0.5, 0.5, 0.5, 0.5);
        newTriangle();
        vertex(1, 0, 0, 1, 1);
        vertex(1, 1, 0, 1, 0);
        vertex(1, 0.5, 0.5, 0.5, 0.5);
        endFace();
    }

    private void valleyEndRight() {
        beginOuterFaces(UNIT_NX);
        beginTriangle();
        vertex(0, 0, 1, 1, 1);
        vertex(0, 1, 1, 1, 0);
        vertex(0, 0.5, 0.5, 0.5, 0.5);
        newTriangle();
        vertex(0, 0, 0, 0, 1);
        vertex(0, 0, 1, 1, 1);
        vertex(0, 0.5, 0.5, 0.5, 0.5);
        newTriangle();
        vertex(0, 1, 0, 0, 0);
        vertex(0, 0, 0, 0, 1);
        vertex(0, 0.5, 0.5, 0.5, 0.5);
        endFace();
    }

    // ------------------------------------------------------------------------------------- Ridges

    private void ridgeLeftFace() {
        beginOuterFaces(UNIT_X);
        beginTriangle();
        vertex(1, 0.5, 0.5, 0.5, 0.5);
        vertex(1, 0, 1, 0, 1);
        vertex(1, 0, 0, 1, 1);
        endFace();
    }

    private void ridgeRightFace() {
        beginOuterFaces(UNIT_NX);
        beginTriangle();
        vertex(0, 0.5, 0.5, 0.5, 0.5);
        vertex(0, 0, 0, 0, 1);
        vertex(0, 0, 1, 1, 1);
        endFace();
    }

    private void ridgeLeft() {
        if (at(LEFT))
            connectRidgeLeft();
        else {
            beginPosXSlope();
            beginTriangle();
            vertex(0.5, 0.5, 0.5, 0.5, 0.5);
            vertex(1, 0, 1, 0, 1);
            vertex(1, 0, 0, 1, 1);
            endFace();
        }
    }

    private void connectRidgeLeft() {
        beginNegZSlope();
        beginTriangle();
        vertex(0.5, 0.5, 0.5, 0.5, 0.5);
        vertex(1, 0.5, 0.5, 0, 0.5);
        vertex(1, 0, 0, 0, 1);
        endFace();
        beginPosZSlope();
        beginTriangle();
        vertex(0.5, 0.5, 0.5, 0.5, 0.5);
        vertex(1, 0, 1, 1, 1);
        vertex(1, 0.5, 0.5, 1, 0.5);
        endFace();
    }

    private void ridgeRight() {
        if (at(RIGHT))
            connectRidgeRight();
        else {
            beginNegXSlope();
            beginTriangle();
            vertex(0.5, 0.5, 0.5, 0.5, 0.5);
            vertex(0, 0, 0, 0, 1);
            vertex(0, 0, 1, 1, 1);
            endFace();
        }
    }

    private void connectRidgeRight() {
        beginNegZSlope();
        beginTriangle();
        vertex(0.5, 0.5, 0.5, 0.5, 0.5);
        vertex(0, 0, 0, 1, 1);
        vertex(0, 0.5, 0.5, 1, 0.5);
        endFace();
        beginPosZSlope();
        beginTriangle();
        vertex(0.5, 0.5, 0.5, 0.5, 0.5);
        vertex(0, 0.5, 0.5, 0, 0.5);
        vertex(0, 0, 1, 0, 1);
        endFace();
    }

    private void ridgeFront(boolean fill) {
        if (at(FRONT))
            connectRidgeFront();
        else if (fill) {
            beginNegZSlope();
            beginTriangle();
            vertex(0.5, 0.5, 0.5, 0.5, 0.5);
            vertex(1, 0, 0, 0, 1);
            vertex(0, 0, 0, 1, 1);
            endFace();
        }
    }

    private void connectRidgeFront() {
        beginPosXSlope();
        beginTriangle();
        vertex(0.5, 0.5, 0.5, 0.5, 0.5);
        vertex(1, 0, 0, 1, 1);
        vertex(0.5, 0.5, 0, 1, 0.5);
        endFace();
        beginNegXSlope();
        beginTriangle();
        vertex(0.5, 0.5, 0.5, 0.5, 0.5);
        vertex(0.5, 0.5, 0, 0, 0.5);
        vertex(0, 0, 0, 0, 1);
        endFace();
    }

    private void ridgeBack(boolean fill) {
        if (at(BACK))
            connectRidgeBack();
        else if (fill) {
            beginPosZSlope();
            beginTriangle();
            vertex(0.5, 0.5, 0.5, 0.5, 0.5);
            vertex(0, 0, 1, 0, 1);
            vertex(1, 0, 1, 1, 1);
            endFace();
        }
    }

    private void connectRidgeBack() {
        beginPosXSlope();
        beginTriangle();
        vertex(0.5, 0.5, 0.5, 0.5, 0.5);
        vertex(0.5, 0.5, 1, 0, 0.5);
        vertex(1, 0, 1, 0, 1);
        endFace();
        beginNegXSlope();
        beginTriangle();
        vertex(0.5, 0.5, 0.5, 0.5, 0.5);
        vertex(0, 0, 1, 1, 1);
        vertex(0.5, 0.5, 1, 1, 0.5);
        endFace();
    }

    private void ridgeBackSlope() {
        beginPosZSlope();
        beginQuad();
        vertex(0, 0.5, 0.5, 0, 0.5);
        vertex(0, 0, 1, 0, 1);
        vertex(1, 0, 1, 1, 1);
        vertex(1, 0.5, 0.5, 1, 0.5);
        endFace();
    }

    // ------------------------------------------------------------------------------------- Block sides

    private void leftQuad() {
        beginOuterFaces(UNIT_X);
        beginQuad();
        vertex(1, 1, 1, 0, 0);
        vertex(1, 0, 1, 0, 1);
        vertex(1, 0, 0, 1, 1);
        vertex(1, 1, 0, 1, 0);
        endFace();
    }

    private void rightQuad() {
        beginOuterFaces(UNIT_NX);
        beginQuad();
        vertex(0, 1, 0, 0, 0);
        vertex(0, 0, 0, 0, 1);
        vertex(0, 0, 1, 1, 1);
        vertex(0, 1, 1, 1, 0);
        endFace();
    }

    private void frontQuad() {
        beginOuterFaces(UNIT_NZ);
        beginQuad();
        vertex(1, 1, 0, 0, 0);
        vertex(1, 0, 0, 0, 1);
        vertex(0, 0, 0, 1, 1);
        vertex(0, 1, 0, 1, 0);
        endFace();
    }

    private void backQuad() {
        beginOuterFaces(UNIT_Z);
        beginQuad();
        vertex(0, 1, 1, 0, 0);
        vertex(0, 0, 1, 0, 1);
        vertex(1, 0, 1, 1, 1);
        vertex(1, 1, 1, 1, 0);
        endFace();
    }

    private void bottomQuad() {
        beginOuterFaces(UNIT_NY);
        beginQuad();
        vertex(0, 0, 1, 0, 0);
        vertex(0, 0, 0, 0, 1);
        vertex(1, 0, 0, 1, 1);
        vertex(1, 0, 1, 1, 0);
        endFace();
    }

    private void leftTriangle() {
        beginOuterFaces(UNIT_X);
        beginTriangle();
        vertex(1, 1, 1, 0, 0);
        vertex(1, 0, 1, 0, 1);
        vertex(1, 0, 0, 1, 1);
        endFace();
    }

    private void rightTriangle() {
        beginOuterFaces(UNIT_NX);
        beginTriangle();
        vertex(0, 1, 1, 1, 0);
        vertex(0, 0, 0, 0, 1);
        vertex(0, 0, 1, 1, 1);
        endFace();
    }

    // ------------------------------------------------------------------------------------- Drawing

    private void beginPosXSlope() {
        beginInnerFaces(UNIT_PXPY);
    }

    private void beginNegXSlope() {
        beginInnerFaces(UNIT_NXPY);
    }

    private void beginPosZSlope() {
        beginInnerFaces(UNIT_PYPZ);
    }

    private void beginNegZSlope() {
        beginInnerFaces(UNIT_PYNZ);
    }

    private void beginInnerFaces(double[] n) {
        this.normal = n;
        this.texture = INNER;
    }

    private void beginOuterFaces(double[] n) {
        this.normal = n;
        this.texture = OUTER;
    }

    private void beginTriangle() {
        this.vertices.clear();
    }

    private void beginQuad() {
        this.vertices.clear();
    }

    private void newTriangle() {
        endFace();
        beginTriangle();
    }

    private void vertex(double x, double y, double z, double u, double v) {
        this.vertices.add(new double[]{x - 0.5, y - 0.5, z - 0.5, u, v});
    }

    private void endFace() {
        int n = this.vertices.size();
        var pos = new double[n][];
        var uv = new double[n][];
        for (int i = 0; i < n; i++) {
            var v = this.vertices.get(i);
            pos[i] = new double[]{v[0], v[1], v[2]};
            uv[i] = new double[]{v[3], v[4]};
        }
        this.builder.add(this.texture, this.normal, pos, uv);
        this.vertices.clear();
    }
}
