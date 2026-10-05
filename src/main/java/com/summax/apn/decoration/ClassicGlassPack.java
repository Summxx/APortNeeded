package com.summax.apn.decoration;

import com.summax.apn.APortNeeded;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.fml.ModList;

/**
 * Registers the built-in resource pack with the old tinted glass specular maps.
 */
public final class ClassicGlassPack {

    private static final String PATH = "resourcepacks/classic_glass";

    private ClassicGlassPack() {
    }

    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES)
            return;
        var root = ModList.get().getModFileById(APortNeeded.MOD_ID).getFile().findResource(PATH);
        var pack = Pack.readMetaAndCreate("builtin/" + APortNeeded.MOD_ID + "_classic_glass",
                Component.translatable("pack.apn.classic_glass"), false,
                id -> new PathPackResources(id, root, true), PackType.CLIENT_RESOURCES, Pack.Position.TOP,
                PackSource.BUILT_IN);
        if (pack != null)
            event.addRepositorySource(consumer -> consumer.accept(pack));
    }
}
