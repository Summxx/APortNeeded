package com.summax.apn;

import com.summax.apn.architecture.common.ArchitectureMod;
import com.summax.apn.decoration.DecorationModule;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * A Port Needed: ports of the building blocks we miss from older versions, each one living in its own module.
 */
@Mod(APortNeeded.MOD_ID)
public class APortNeeded {

    public static final String MOD_ID = "apn";

    public APortNeeded() {
        var modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        new ArchitectureMod(modEventBus);
        DecorationModule.init(modEventBus);
    }
}
