package com.example.ytscare;

import com.example.ytscare.network.Net;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(YtScareMod.MODID)
public class YtScareMod {
    public static final String MODID = "ytscare";

    public YtScareMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModEntities.ENTITIES.register(bus);
        ModSounds.SOUNDS.register(bus);
        ModItems.ITEMS.register(bus);
        ModItems.TABS.register(bus);
        Net.init();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
