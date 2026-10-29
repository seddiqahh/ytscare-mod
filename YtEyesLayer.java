package com.example.ytscare.client;

import com.example.ytscare.Config;
import com.example.ytscare.MonsterType;
import com.example.ytscare.entity.YtMonster;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

/**
 * Glowing eyes: draws textures/entity/eyes/<id>.png on top of the model at FULL brightness (ignores world light),
 * so the eyes stay visible and bright in total darkness. In bright places they are only faintly visible.
 */
public class YtEyesLayer extends RenderLayer<YtMonster, YtRenderer.YtModel> {
    private final RenderType renderType;

    public YtEyesLayer(RenderLayerParent<YtMonster, YtRenderer.YtModel> parent, MonsterType type) {
        super(parent);
        this.renderType = RenderType.eyes(type.eyesTexture);
    }

    @Override
    public void render(PoseStack ps, MultiBufferSource buffers, int packedLight, YtMonster e, float limbSwing,
                       float limbSwingAmount, float partialTick, float age, float netHeadYaw, float headPitch) {
        if (!Config.GLOWING_EYES.get() || e.isInvisible()) return;

        int light = e.level().getMaxLocalRawBrightness(e.blockPosition()); // 0 (dark) .. 15 (bright)
        float alpha = Mth.clamp(1.15F - light / 11.0F, 0.3F, 1.0F);
        alpha *= 0.9F + 0.1F * Mth.sin(age * 0.2F); // tiny pulse

        VertexConsumer vc = buffers.getBuffer(this.renderType);
        this.getParentModel().renderToBuffer(ps, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, alpha);
    }
}
