package com.example.ytscare.client;

import com.example.ytscare.ModEntities;
import com.example.ytscare.MonsterType;
import com.example.ytscare.YtScareMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = YtScareMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        for (MonsterType t : MonsterType.values()) {
            e.registerEntityRenderer(ModEntities.TYPES.get(t).get(), ctx -> new YtRenderer(ctx, t));
        }
    }

    @SubscribeEvent
    public static void overlays(RegisterGuiOverlaysEvent e) {
        // screen flicker sits under the hotbar so the HUD itself never flickers
        e.registerBelow(VanillaGuiOverlay.HOTBAR.id(), "flicker", FlickerClient::render);
        e.registerAboveAll("jumpscare", JumpscareClient::render);
    }
}
