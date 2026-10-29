package com.example.ytscare.client;

import com.example.ytscare.Config;
import com.example.ytscare.ModSounds;
import com.example.ytscare.MonsterType;
import com.example.ytscare.YtScareMod;
import com.example.ytscare.entity.YtMonster;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Heartbeat that gets faster and louder the closer the nearest STALKER monster is
 * (far = ~50 bpm, right next to you = ~150 bpm). Client only, nothing is sent over the network.
 */
@Mod.EventBusSubscriber(modid = YtScareMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class StalkerHeartbeat {
    private static int cooldown = 0;

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.isPaused() || !Config.HEARTBEAT.get()) {
            cooldown = 0;
            return;
        }

        double range = Config.HEARTBEAT_RANGE.get();
        double best = Double.MAX_VALUE;
        for (YtMonster m : mc.level.getEntitiesOfClass(YtMonster.class, mc.player.getBoundingBox().inflate(range))) {
            if (m.isAlive() && m.mt().behavior == MonsterType.Behavior.STALKER) {
                best = Math.min(best, m.distanceTo(mc.player));
            }
        }
        if (best > range) {
            cooldown = 0;
            return;
        }

        float closeness = (float) Mth.clamp(1.0 - best / range, 0.0, 1.0); // 0 far .. 1 touching
        if (--cooldown > 0) return;

        cooldown = Math.round(Mth.lerp(closeness * closeness, 24.0F, 8.0F)); // ticks between beats: ~1.2 s -> 0.4 s
        float volume = 0.25F + 0.95F * closeness;
        float pitch = 0.85F + 0.4F * closeness;
        mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.HEARTBEAT_BEAT.get(), pitch, volume));
    }
}
