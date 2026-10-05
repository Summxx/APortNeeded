package com.summax.apn.decoration;

import com.summax.apn.APortNeeded;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Decoration module: tinted glass and chroma lamps.
 */
public final class DecorationModule {

    public static final int TINTED_GLASS_VARIANTS = 16;

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, APortNeeded.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, APortNeeded.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, APortNeeded.MOD_ID);

    public static final List<RegistryObject<Block>> TINTED_GLASS = new ArrayList<>();
    public static final List<RegistryObject<Block>> CHROMA_LAMPS = new ArrayList<>();

    static {
        for (int i = 0; i < TINTED_GLASS_VARIANTS; i++) {
            TINTED_GLASS.add(registerBlock("tinted_glass_" + i,
                    () -> new TintedGlassBlock(BlockBehaviour.Properties.copy(Blocks.GLASS))));
        }
        for (var colour : DyeColor.values()) {
            CHROMA_LAMPS.add(registerBlock("chroma_lamp_" + colour.getName(),
                    () -> new ChromaLampBlock(colour, BlockBehaviour.Properties.of()
                            .mapColor(colour)
                            .strength(1.0F, 10.0F)
                            .sound(SoundType.STONE)
                            .lightLevel(state -> ChromaLampBlock.LIGHT_LEVEL)
                            .emissiveRendering((state, level, pos) -> true))));
        }
        TABS.register("decoration", () -> CreativeModeTab.builder()
                .title(Component.translatable("item_group.apn.decoration"))
                .icon(() -> CHROMA_LAMPS.get(DyeColor.LIGHT_BLUE.getId()).get().asItem().getDefaultInstance())
                .displayItems((params, output) -> {
                    TINTED_GLASS.forEach(b -> output.accept(b.get()));
                    CHROMA_LAMPS.forEach(b -> output.accept(b.get()));
                })
                .build());
    }

    private DecorationModule() {
    }

    private static RegistryObject<Block> registerBlock(String name, java.util.function.Supplier<Block> block) {
        var registered = BLOCKS.register(name, block);
        ITEMS.register(name, () -> new BlockItem(registered.get(), new Item.Properties()));
        return registered;
    }

    public static void init(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        TABS.register(modEventBus);
        modEventBus.addListener(MicroblockCompat::onRegister);
        modEventBus.addListener(ClassicGlassPack::onAddPackFinders);
    }
}
