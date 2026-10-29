package com.example.ytscare.client;

import com.example.ytscare.MonsterType;
import com.example.ytscare.entity.YtMonster;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Renders a monster with a normal 64x64 player skin, scaled by MonsterType.scale, plus glowing eyes. */
public class YtRenderer extends MobRenderer<YtMonster, YtRenderer.YtModel> {
    private final MonsterType type;

    public YtRenderer(EntityRendererProvider.Context ctx, MonsterType type) {
        super(ctx, new YtModel(ctx.bakeLayer(type.slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), type.slim, type), 0.5F * type.scale);
        this.type = type;
        this.addLayer(new YtEyesLayer(this, type));
    }

    @Override
    public ResourceLocation getTextureLocation(YtMonster e) {
        return type.texture;
    }

    @Override
    protected void scale(YtMonster e, PoseStack ps, float partialTick) {
        ps.scale(type.scale, type.scale, type.scale);
    }

    /** Player model with zombie-style raised arms when hunting (and a custom walk for the Faceless Suit). */
    public static class YtModel extends PlayerModel<YtMonster> {
        private final MonsterType type;

        public YtModel(ModelPart part, boolean slim, MonsterType type) {
            super(part, slim);
            this.type = type;
        }

        @Override
        public void setupAnim(YtMonster e, float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch) {
            super.setupAnim(e, limbSwing, limbSwingAmount, age, headYaw, headPitch);
            AnimationUtils.animateZombieArms(this.leftArm, this.rightArm, e.isAggressive(), this.attackTime, age);

            if (type == MonsterType.SUIT_SHADE) {
                suitWalk(e, limbSwing, limbSwingAmount, age);
            }

            // keep the second skin layers glued to the (possibly changed) base parts
            this.hat.copyFrom(this.head);
            this.jacket.copyFrom(this.body);
            this.leftSleeve.copyFrom(this.leftArm);
            this.rightSleeve.copyFrom(this.rightArm);
            this.leftPants.copyFrom(this.leftLeg);
            this.rightPants.copyFrom(this.rightLeg);
        }

        /**
         * The Faceless Suit walks like a tall thing that learned to walk from a video:
         *  - slow, very long, stiff strides that "snap" at the end of each step
         *  - arms hang limp and drag behind the legs instead of swinging in sync
         *  - head hangs forward and tilted, with a slow creepy sway
         *  - when it charges (aggressive) the stride gets faster and the arms reach forward
         */
        private void suitWalk(YtMonster e, float limbSwing, float limbSwingAmount, float age) {
            boolean charging = e.isAggressive();
            float amt = Math.min(limbSwingAmount, 1.0F);
            float swing = limbSwing * (charging ? 0.62F : 0.38F);

            // snap the cosine towards its extremes -> leg "pauses" at the end of each long step
            float c = Mth.cos(swing);
            float stride = Math.signum(c) * (float) Math.pow(Math.abs(c), 0.6) * amt;
            float legAmp = charging ? 1.25F : 1.0F;

            this.rightLeg.xRot = stride * legAmp;
            this.leftLeg.xRot = -stride * legAmp;
            this.rightLeg.yRot = 0.0F;
            this.leftLeg.yRot = 0.0F;
            this.rightLeg.zRot = 0.03F * amt;
            this.leftLeg.zRot = -0.03F * amt;

            if (charging) {
                // arms stretched forward, trembling a little
                float tremble = Mth.sin(age * 0.9F) * 0.04F;
                this.rightArm.xRot = -1.25F + tremble - stride * 0.15F;
                this.leftArm.xRot = -1.25F - tremble + stride * 0.15F;
                this.rightArm.zRot = 0.05F;
                this.leftArm.zRot = -0.05F;
                this.rightArm.yRot = -0.08F;
                this.leftArm.yRot = 0.08F;
            } else {
                // limp arms that lag a bit behind the legs
                float lag = Mth.cos(swing - 0.7F) * amt;
                float idleSway = Mth.sin(age * 0.05F) * 0.03F;
                this.rightArm.xRot = -lag * 0.32F + 0.06F + idleSway;
                this.leftArm.xRot = lag * 0.32F + 0.06F - idleSway;
                this.rightArm.zRot = 0.09F + Mth.cos(age * 0.07F) * 0.02F;
                this.leftArm.zRot = -0.09F - Mth.cos(age * 0.07F + 1.0F) * 0.02F;
                this.rightArm.yRot = 0.0F;
                this.leftArm.yRot = 0.0F;
            }

            // head hangs forward and leans to one side, with a slow sway
            this.head.xRot += 0.28F;
            this.head.zRot = 0.2F + Mth.sin(age * 0.045F) * 0.07F;
        }
    }
}
