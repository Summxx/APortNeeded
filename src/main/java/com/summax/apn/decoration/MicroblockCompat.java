package com.summax.apn.decoration;

import com.summax.apn.architecture.core.ArchitectureLog;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

/**
 * Registers the decoration blocks as CB Microblocks materials through reflection, if CB Multipart is installed.
 * Don't also list them in custom-micromaterials.cfg, registering a material twice fails.
 */
public final class MicroblockCompat {

    private static final String MOD_ID = "cb_microblock";
    private static final ResourceLocation REGISTRY_NAME = new ResourceLocation(MOD_ID, "micro_material");
    private static final String MATERIAL_CLASS = "codechicken.microblock.api.BlockMicroMaterial";

    private MicroblockCompat() {
    }

    public static void onRegister(RegisterEvent event) {
        if (!event.getRegistryKey().location().equals(REGISTRY_NAME) || !ModList.get().isLoaded(MOD_ID))
            return;
        try {
            var materialClass = Class.forName(MATERIAL_CLASS);
            var constructor = materialClass.getConstructor(BlockState.class);
            var makeKey = materialClass.getMethod("makeMaterialKey", BlockState.class);
            int count = 0;
            for (var blocks : java.util.List.of(DecorationModule.TINTED_GLASS, DecorationModule.CHROMA_LAMPS)) {
                for (RegistryObject<Block> block : blocks) {
                    var state = block.get().defaultBlockState();
                    var material = constructor.newInstance(state);
                    var key = (ResourceLocation) makeKey.invoke(null, state);
                    register(event, key, () -> material);
                    count++;
                }
            }
            ArchitectureLog.info("Registered " + count + " CB Microblocks materials.");
        } catch (ReflectiveOperationException | LinkageError e) {
            ArchitectureLog.error("Failed to register CB Microblocks materials, the CB Multipart API may have changed.", e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void register(RegisterEvent event, ResourceLocation key, Supplier<Object> material) {
        event.register((ResourceKey) event.getRegistryKey(), key, (Supplier) material);
    }
}
