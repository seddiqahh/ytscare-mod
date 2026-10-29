package com.example.ytscare;

import com.example.ytscare.network.JumpscarePacket;
import com.example.ytscare.network.Net;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/** Random whispers, footsteps, knocks, fake chat and fake jumpscares. Only the targeted player hears them. */
@Mod.EventBusSubscriber(modid = YtScareMod.MODID)
public class AmbientScares {
    private static final String[] LINES = {
            "I see you.", "Behind you.", "Don't turn around.", "Why did you come here?",
            "Look up.", "You can't leave.", "It's already inside.", "Turn off the lights.",
            "I'm right here.", "Don't look at me.", "Run.", "Found you."
    };

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)) return;
        if (!Config.AMBIENT.get() || p.isSpectator()) return;
        RandomSource r = p.getRandom();
        if (r.nextInt(Config.INTERVAL.get()) != 0) return;
        trigger(p, r.nextInt(8));
    }

    public static void trigger(ServerPlayer p, int kind) {
        RandomSource r = p.getRandom();
        Vec3 behind = p.getLookAngle().multiply(1, 0, 1).normalize().scale(-1);
        switch (kind) {
            case 0, 1 -> { // whisper very close, from a random side
                double a = r.nextDouble() * Math.PI * 2, d = 1.5 + r.nextDouble() * 3;
                playTo(p, ModSounds.WHISPER.get(), p.getX() + Math.cos(a) * d, p.getY(), p.getZ() + Math.sin(a) * d, 1.6F, 0.9F + r.nextFloat() * 0.3F);
            }
            case 2 -> // distant scream/growl
                playTo(p, ModSounds.SCARE.get(), p.getX() + behind.x * 25, p.getY(), p.getZ() + behind.z * 25, 2.0F, 0.8F + r.nextFloat() * 0.4F);
            case 3 -> // footsteps right behind you
                playTo(p, ModSounds.STEPS.get(), p.getX() + behind.x * 3, p.getY(), p.getZ() + behind.z * 3, 1.5F, 1.0F);
            case 4 -> // knocking
                playTo(p, ModSounds.KNOCK.get(), p.getX() + behind.x * 2, p.getY() + 1, p.getZ() + behind.z * 2, 2.0F, 0.9F + r.nextFloat() * 0.2F);
            case 5 -> { // fake chat message from a monster
                if (Config.FAKE_CHAT.get()) {
                    MonsterType t = MonsterType.byOrdinal(r.nextInt(MonsterType.values().length));
                    p.sendSystemMessage(Component.literal("<" + t.displayName + "> " + LINES[r.nextInt(LINES.length)])
                            .withStyle(ChatFormatting.DARK_RED));
                    playTo(p, ModSounds.WHISPER_VOICE.get(), p.getX(), p.getY(), p.getZ(), 1.0F, 1.0F);
                }
            }
            case 6 -> // breathing on your neck + heartbeat
                playTo(p, ModSounds.BREATH.get(), p.getX() + behind.x, p.getY() + 1.5, p.getZ() + behind.z, 1.8F, 1.0F);
            case 7 -> { // rare fake jumpscare
                if (Config.FAKE_JUMPSCARES.get() && r.nextInt(3) == 0) {
                    Net.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p),
                            new JumpscarePacket(r.nextInt(MonsterType.values().length), 30));
                } else {
                    playTo(p, ModSounds.HEARTBEAT.get(), p.getX(), p.getY(), p.getZ(), 2.0F, 1.0F);
                }
            }
        }
    }

    /** Play a sound at a position, but only for this one player. */
    public static void playTo(ServerPlayer p, SoundEvent ev, double x, double y, double z, float vol, float pitch) {
        p.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(ev),
                SoundSource.AMBIENT, x, y, z, vol, pitch, p.getRandom().nextLong()));
    }
}
