package com.example.ytscare.client;

import com.example.ytscare.ModSounds;
import com.example.ytscare.MonsterType;
import com.example.ytscare.YtScareMod;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

/** Fullscreen jumpscare: the monster's face (from its skin) slams onto the screen with a scream. */
@Mod.EventBusSubscriber(modid = YtScareMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class JumpscareClient {
    private static MonsterType active;
    private static int ticks;
    private static int total = 1;

    public static void start(int typeOrdinal, int duration) {
        active = MonsterType.byOrdinal(typeOrdinal);
        total = Math.max(1, duration);
        ticks = total;
        // UI sound = full volume, not positional
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(ModSounds.JUMPSCARE.get(active).get(), 1.0F, 1.0F));
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase == TickEvent.Phase.END && ticks > 0) {
            ticks--;
            if (ticks == 0) active = null;
        }
    }

    public static void render(ForgeGui gui, GuiGraphics g, float partial, int w, int h) {
        if (active == null || ticks <= 0) return;

        float t = Mth.clamp(1.0F - (ticks - partial) / total, 0.0F, 1.0F); // 0 -> 1 over the scare
        Random r = new Random((long) (total - ticks) * 7919L + active.ordinal());
        MonsterType.Style style = active.style;
        ResourceLocation tex = active.texture;
        PoseStack ps = g.pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        boolean whiteFlash = style == MonsterType.Style.FLASH && ((ticks / 2) % 2 == 0) && t < 0.6F;
        g.fill(0, 0, w, h, whiteFlash ? 0xFFFFFFFF : 0xFF000000);

        float zoom = switch (style) {
            case ZOOM -> 0.7F + 2.2F * t * t;
            case SHAKE -> 1.7F;
            case GLITCH -> 1.3F + 0.4F * r.nextFloat();
            case FLASH -> 1.3F + t;
        };
        int size = (int) (Math.min(w, h) * zoom);
        float shake = (style == MonsterType.Style.SHAKE ? 44 : 14) * (1.0F - t * 0.6F);
        float ox = (r.nextFloat() - 0.5F) * shake;
        float oy = (r.nextFloat() - 0.5F) * shake;
        float roll = (r.nextFloat() - 0.5F) * (style == MonsterType.Style.SHAKE ? 14 : 4);

        ps.pushPose();
        ps.translate(w / 2.0F + ox, h / 2.0F + oy, 0);
        ps.mulPose(Axis.ZP.rotationDegrees(roll));

        if (style == MonsterType.Style.GLITCH) { // RGB split
            RenderSystem.setShaderColor(1.0F, 0.15F, 0.15F, 0.8F);
            face(g, tex, size, -26, 0);
            RenderSystem.setShaderColor(0.15F, 1.0F, 1.0F, 0.8F);
            face(g, tex, size, 26, 0);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        face(g, tex, size, 0, 0);
        ps.popPose();

        // blood-red tint that fades out
        g.fill(0, 0, w, h, (((int) (70 * (1.0F - t))) << 24) | 0xAA0000);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    /** Head front (8,8) + hat layer (40,8) of the 64x64 skin, stretched over the screen. */
    private static void face(GuiGraphics g, ResourceLocation tex, int size, int dx, int dy) {
        int x = -size / 2 + dx, y = -size / 2 + dy;
        g.blit(tex, x, y, size, size, 8.0F, 8.0F, 8, 8, 64, 64);
        g.blit(tex, x, y, size, size, 40.0F, 8.0F, 8, 8, 64, 64);
    }
}
