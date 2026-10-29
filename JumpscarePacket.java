package com.example.ytscare.network;

import com.example.ytscare.client.JumpscareClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> client: "show monster #type fullscreen for N ticks". */
public class JumpscarePacket {
    public final int type;
    public final int duration;

    public JumpscarePacket(int type, int duration) {
        this.type = type;
        this.duration = duration;
    }

    public static void encode(JumpscarePacket p, FriendlyByteBuf b) {
        b.writeVarInt(p.type);
        b.writeVarInt(p.duration);
    }

    public static JumpscarePacket decode(FriendlyByteBuf b) {
        return new JumpscarePacket(b.readVarInt(), b.readVarInt());
    }

    public static void handle(JumpscarePacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> JumpscareClient.start(p.type, p.duration)));
        ctx.get().setPacketHandled(true);
    }
}
