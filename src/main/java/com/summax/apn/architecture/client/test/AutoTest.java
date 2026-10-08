package com.summax.apn.architecture.client.test;

import com.summax.apn.architecture.common.ArchitectureMod;
import com.summax.apn.architecture.common.block.BlockShape;
import com.summax.apn.architecture.common.block.entity.BlockEntityShape;
import com.summax.apn.architecture.common.shape.EnumShape;
import com.summax.apn.architecture.common.shape.orientation.*;
import com.summax.apn.architecture.core.ArchitectureLog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import com.summax.apn.decoration.DecorationModule;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Development only rendering test, enabled with -Dapn.autotest=true.
 */
public final class AutoTest {

    private static final BlockPos ORIGIN = new BlockPos(0, 150, 0);
    private static final int SETTLE_TICKS = 700;

    /**
     * Viewpoints: name, time of day, camera position and rotation.
     */
    private record View(String name, long time, double x, double y, double z, float yaw, float pitch) {
    }

    private static final View[] VIEWS = {
            new View("day-front", 6000, 0.5, 152.5, -7.5, 0, 15),
            new View("night-lamp", 18000, -0.5, 152.0, -7.0, 0, 12),
            new View("slopes-day", 6000, 20.0, 153.5, -4.0, 50, 28),
            new View("slopes-night", 18000, 20.0, 153.5, -4.0, 50, 28)
    };

    private static int tick = -1;
    private static int view;
    private static boolean built;

    private AutoTest() {
    }

    public static boolean isEnabled() {
        return Boolean.getBoolean("apn.autotest");
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;
        var mc = Minecraft.getInstance();
        var server = mc.getSingleplayerServer();
        if (mc.player == null || mc.level == null || server == null)
            return;

        if (!built) {
            built = true;
            mc.options.hideGui = true;
            server.execute(() -> buildScene(server.overworld()));
            moveTo(mc, VIEWS[0]);
            tick = 0;
            return;
        }
        if (tick < 0)
            return;
        tick++;
        if (tick == SETTLE_TICKS) {
            var name = "apn-test-" + VIEWS[view].name() + ".png";
            Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), message -> ArchitectureLog.info("AutoTest: " + message.getString()));
            view++;
            if (view >= VIEWS.length) {
                tick = -1;
                ArchitectureLog.info("AutoTest: done, stopping.");
                mc.stop();
                return;
            }
            moveTo(mc, VIEWS[view]);
            tick = SETTLE_TICKS - 400;
        }
    }

    private static void moveTo(Minecraft mc, View view) {
        var server = mc.getSingleplayerServer();
        var uuid = mc.player.getUUID();
        server.execute(() -> {
            var level = server.overworld();
            level.setDayTime(view.time());
            level.setWeatherParameters(6000, 0, false, false);
            var player = server.getPlayerList().getPlayer(uuid);
            if (player != null) {
                player.getAbilities().mayfly = true;
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
                player.teleportTo(level, view.x(), view.y(), view.z(), view.yaw(), view.pitch());
            }
        });
    }

    private static void buildScene(ServerLevel level) {
        // Stone, compared to vanilla stone stairs (use white concrete to measure lighting without texture noise).
        var quartz = Blocks.STONE.defaultBlockState();
        // Platform and clear air above it.
        for (int x = -8; x <= 24; x++) {
            for (int z = -8; z <= 8; z++) {
                level.setBlockAndUpdate(ORIGIN.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState());
                for (int y = 0; y < 6; y++)
                    level.setBlockAndUpdate(ORIGIN.offset(x, y, z), Blocks.AIR.defaultBlockState());
            }
        }
        // Vanilla block and shapes in a row, lit by a lamp in front.
        level.setBlockAndUpdate(ORIGIN.offset(-3, 0, 0), quartz);
        level.setBlockAndUpdate(ORIGIN.offset(-5, 0, 0), Blocks.STONE_STAIRS.defaultBlockState()
                .setValue(net.minecraft.world.level.block.StairBlock.FACING, Direction.WEST));
        place(level, ORIGIN.offset(-1, 0, 0), EnumShape.ROOF_TILE, Direction.DOWN, 1, quartz);
        place(level, ORIGIN.offset(1, 0, 0), EnumShape.SLOPE_TILE_B1, Direction.DOWN, 1, quartz);
        place(level, ORIGIN.offset(3, 0, 0), EnumShape.STAIRS, Direction.DOWN, 1, quartz);
        // Lit by our own purple chroma lamps.
        var lamp = DecorationModule.CHROMA_LAMPS.get(DyeColor.PURPLE.getId()).get().defaultBlockState();
        level.setBlockAndUpdate(ORIGIN.offset(0, 0, -2), lamp);
        for (int y = 0; y < 4; y++)
            level.setBlockAndUpdate(ORIGIN.offset(0, y, 3), lamp);
        // A column of slopes against a wall, like a pillar.
        for (int y = 0; y < 4; y++) {
            place(level, ORIGIN.offset(-1, y, 4), EnumShape.ROOF_TILE, Direction.EAST, 0, quartz);
            place(level, ORIGIN.offset(1, y, 4), EnumShape.ROOF_TILE, Direction.WEST, 0, quartz);
            level.setBlockAndUpdate(ORIGIN.offset(0, y, 5), quartz);
        }
        buildSlopeComparison(level, quartz);
    }

    private static void buildSlopeComparison(ServerLevel level, BlockState material) {
        material = Blocks.WHITE_CONCRETE.defaultBlockState();
        var lamp = DecorationModule.CHROMA_LAMPS.get(DyeColor.WHITE.getId()).get().defaultBlockState();
        level.setBlockAndUpdate(ORIGIN.offset(16, 0, -2), lamp);
        for (int x = 0; x < 3; x++)
            place(level, ORIGIN.offset(12 + x, 0, -4), EnumShape.ROOF_TILE, Direction.DOWN, 2, material);
        for (int x = 0; x < 3; x++) {
            place(level, ORIGIN.offset(12 + x, 0, 0), EnumShape.ROOF_TILE, Direction.DOWN, 0, material);
            level.setBlockAndUpdate(ORIGIN.offset(12 + x, 0, 1), material);
        }
    }

    private static void place(ServerLevel level, BlockPos pos, EnumShape shape, Direction side, int turn, BlockState material) {
        BlockShape<?> block = ArchitectureMod.CONTENT.blockShapes.get(shape);
        var orientation = ShapeOrientation.forProperties(
                ShapeOrientationPropertyFacing.of(side),
                ShapeOrientationPropertySpin.of(EnumSpin.byIndex(turn)));
        var state = orientation.applyToState(block.defaultBlockState());
        level.setBlockAndUpdate(pos, state);
        if (level.getBlockEntity(pos) instanceof BlockEntityShape entity)
            entity.setBaseMaterialState(material);
    }
}
