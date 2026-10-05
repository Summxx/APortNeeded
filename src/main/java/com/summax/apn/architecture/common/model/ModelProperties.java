package com.summax.apn.architecture.common.model;


import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelProperty;

import java.util.Objects;

public class ModelProperties {
    public static final ModelProperty<Level> LEVEL = new ModelProperty<Level>(Objects::nonNull);
    public static final ModelProperty<BlockPos> POS = new ModelProperty<BlockPos>(Objects::nonNull);
    public static final ModelProperty<BlockEntity> TILE = new ModelProperty<BlockEntity>();
    public static final ModelProperty<BlockState> BASE_MATERIAL = new ModelProperty<BlockState>();
    public static final ModelProperty<BlockState> SECONDARY_MATERIAL = new ModelProperty<BlockState>();
    /**
     * Roofs: local sides joining a neighbouring roof, see RoofConnections.
     */
    public static final ModelProperty<Integer> ROOF_CONNECTIONS = new ModelProperty<Integer>();
}
