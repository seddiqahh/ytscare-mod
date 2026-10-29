package com.example.ytscare.network;

import com.example.ytscare.YtScareMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class Net {
    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(YtScareMod.MODID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);

    public static void init() {
        CHANNEL.registerMessage(0, JumpscarePacket.class, JumpscarePacket::encode, JumpscarePacket::decode, JumpscarePacket::handle);
    }
}
