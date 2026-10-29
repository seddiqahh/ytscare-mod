package com.example.ytscare.client;

import com.example.ytscare.Config;
import com.example.ytscare.YtScareMod;
import com.example.ytscare.entity.YtMonster;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Light flicker you can SEE: when a monster is close, the world randomly dips into darkness for a few ticks
 * (this affects every light source, also glowstone / lanterns / sea lanterns, which blocks cannot do).
 * Drawn below the hotbar, so the HUD never flickers.
 */
@Mod.EventBusSubscriber(modid = YtScareMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class FlickerClient {
    private static final double RANGE = 16.0;
    private static final RandomSource RND = RandomSource.create();
    private static int flickerTicks = 0;
    private static float flickerAlpha = 0.0F;

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.isPaused() || !Config.LIGHT_DISRUPTION.get()) {
            flickerTicks = 0;
            return;
        }
        if (flickerTicks > 0) {
            flickerTicks--;
            return;
        }

        double best = Double.MAX_VALUE;
        for (YtMonster m : mc.level.getEntitiesOfClass(YtMonster.class, mc.player.getBoundingBox().inflate(RANGE))) {
            if (m.isAlive()) best = Math.min(best, m.distanceTo(mc.player));
        }
        if (best > RANGE) return;

        float closeness = (float) Mth.clamp(1.0 - best / RANGE, 0.0, 1.0);
        if (RND.nextFloat() < 0.03F + 0.22F * closeness) { // start a flicker
            flickerTicks = 1 + RND.nextInt(3 + (int) (3 * closeness));
            flickerAlpha = 0.35F + 0.55F * closeness * RND.nextFloat() + 0.1F * RND.nextFloat();
        }
    }

    public static void render(ForgeGui gui, GuiGraphics g, float partial, int w, int h) {
        if (flickerTicks <= 0) return;
        int a = (int) (Mth.clamp(flickerAlpha, 0.0F, 0.95F) * 255);
        g.fill(0, 0, w, h, a << 24);
    }
}
