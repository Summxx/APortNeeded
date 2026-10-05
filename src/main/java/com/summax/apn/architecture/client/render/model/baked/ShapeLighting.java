package com.summax.apn.architecture.client.render.model.baked;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraftforge.client.model.data.ModelProperty;

/**
 * Light and occlusion around a shape, captured when its chunk is built and used to light sloped faces per vertex.
 */
public final class ShapeLighting {

    public static final ModelProperty<ShapeLighting> PROPERTY = new ModelProperty<>();

    private static final int SIZE = 3;
    private static final double OFFSET = 0.5;
    private final byte[] block = new byte[SIZE * SIZE * SIZE];
    private final byte[] sky = new byte[SIZE * SIZE * SIZE];
    private final float[] shade = new float[SIZE * SIZE * SIZE];
    private final boolean[] opaque = new boolean[SIZE * SIZE * SIZE];

    private ShapeLighting() {
    }

    public static ShapeLighting capture(BlockAndTintGetter level, BlockPos pos) {
        var lighting = new ShapeLighting();
        var cell = new BlockPos.MutableBlockPos();
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                for (int z = 0; z < SIZE; z++) {
                    cell.set(pos.getX() + x - 1, pos.getY() + y - 1, pos.getZ() + z - 1);
                    int i = index(x, y, z);
                    var state = level.getBlockState(cell);
                    lighting.shade[i] = state.getShadeBrightness(level, cell);
                    lighting.opaque[i] = state.getLightBlock(level, cell) >= level.getMaxLightLevel();
                    lighting.block[i] = (byte) level.getBrightness(LightLayer.BLOCK, cell);
                    lighting.sky[i] = (byte) level.getBrightness(LightLayer.SKY, cell);
                }
            }
        }
        return lighting;
    }

    private static int index(int x, int y, int z) {
        return (x * SIZE + y) * SIZE + z;
    }

    /**
     * Samples the light in front of a vertex, writes block light, sky light and occlusion into out.
     */
    public void sample(double px, double py, double pz, float nx, float ny, float nz, float[] out) {
        double tx = clamp(px + nx * OFFSET) - 0.5, ty = clamp(py + ny * OFFSET) - 0.5, tz = clamp(pz + nz * OFFSET) - 0.5;
        int ix = (int) Math.floor(tx), iy = (int) Math.floor(ty), iz = (int) Math.floor(tz);
        double fx = tx - ix, fy = ty - iy, fz = tz - iz;
        double shadeSum = 0;
        int blockMax = -1, skyMax = -1;
        for (int dx = 0; dx < 2; dx++) {
            for (int dy = 0; dy < 2; dy++) {
                for (int dz = 0; dz < 2; dz++) {
                    double w = (dx == 0 ? 1 - fx : fx) * (dy == 0 ? 1 - fy : fy) * (dz == 0 ? 1 - fz : fz);
                    int i = index(ix + dx + 1, iy + dy + 1, iz + dz + 1);
                    shadeSum += w * this.shade[i];
                    // We use the brightest open cell, the renderer never goes below the block's own light anyway.
                    if (!this.opaque[i]) {
                        blockMax = Math.max(blockMax, this.block[i]);
                        skyMax = Math.max(skyMax, this.sky[i]);
                    }
                }
            }
        }
        int own = index(1, 1, 1);
        float blockLight = blockMax >= 0 ? blockMax : this.block[own];
        float skyLight = skyMax >= 0 ? skyMax : this.sky[own];
        out[0] = blockLight;
        out[1] = skyLight;
        out[2] = (float) shadeSum;
    }

    private static double clamp(double v) {
        return Math.max(-0.49, Math.min(1.49, v));
    }
}
