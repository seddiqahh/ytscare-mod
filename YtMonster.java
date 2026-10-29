package com.example.ytscare.entity;

import com.example.ytscare.AmbientScares;
import com.example.ytscare.LightDisruption;
import com.example.ytscare.ModSounds;
import com.example.ytscare.MonsterType;
import com.example.ytscare.network.JumpscarePacket;
import com.example.ytscare.network.Net;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.EnumSet;

public class YtMonster extends Monster {
    private MonsterType cached; // NOTE: no initializer on purpose (registerGoals runs inside super())

    boolean stalking = true;
    int watchTicks = 0;
    private int scareCooldown = 0;
    private int teleportCooldown = 200;
    /** boss bar for the big monsters (>= 100 HP) */
    private final ServerBossEvent bossBar = new ServerBossEvent(Component.literal("?"), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);

    public YtMonster(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 20;
        this.setMaxUpStep(Math.max(0.6F, mt().scale * 0.6F)); // big monsters can walk up blocks
        this.bossBar.setName(Component.literal(mt().displayName));
    }

    public MonsterType mt() {
        if (cached == null) {
            cached = MonsterType.byId(ForgeRegistries.ENTITY_TYPES.getKey(getType()).getPath());
        }
        return cached;
    }

    public static AttributeSupplier.Builder attributes(MonsterType t) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, t.health)
                .add(Attributes.MOVEMENT_SPEED, t.speed)
                .add(Attributes.ATTACK_DAMAGE, t.damage)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, Math.min(0.9, 0.2 * t.scale));
    }

    // ------------------------------------------------------------------ AI

    @Override
    protected void registerGoals() {
        MonsterType.Behavior b = mt().behavior;
        this.goalSelector.addGoal(0, new FloatGoal(this));
        if (b == MonsterType.Behavior.WEEPING) this.goalSelector.addGoal(0, new FreezeWhenWatchedGoal(this));
        if (b == MonsterType.Behavior.STALKER) this.goalSelector.addGoal(0, new StalkGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 24.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) return;

        LightDisruption.tick(this); // torches burn out, lamps flicker near the monster
        if (scareCooldown > 0) scareCooldown--;
        LivingEntity target = getTarget();
        if (target == null) {
            stalking = true;
            watchTicks = 0;
            return;
        }

        // proximity jumpscare (before it even hits you)
        if (target instanceof ServerPlayer sp && !isStillStalking() && scareCooldown <= 0
                && distanceToSqr(sp) < 12.25 && hasLineOfSight(sp)) {
            scare(sp);
        }

        // teleporter: pop up right behind the player
        if (mt().behavior == MonsterType.Behavior.TELEPORTER && --teleportCooldown <= 0) {
            teleportCooldown = 160 + random.nextInt(200);
            if (distanceTo(target) > 8) {
                Vec3 back = target.getLookAngle().multiply(1, 0, 1).normalize().scale(-2.5);
                if (randomTeleport(target.getX() + back.x, target.getY(), target.getZ() + back.z, true)
                        && target instanceof ServerPlayer sp) {
                    AmbientScares.playTo(sp, ModSounds.WHISPER.get(), sp.getX(), sp.getY(), sp.getZ(), 1.5F, 0.8F);
                    AmbientScares.playTo(sp, ModSounds.HEARTBEAT.get(), sp.getX(), sp.getY(), sp.getZ(), 1.0F, 1.0F);
                }
            }
        }
    }

    private boolean isStillStalking() {
        return mt().behavior == MonsterType.Behavior.STALKER && stalking;
    }

    private boolean isBoss() {
        return mt().health >= 100;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (isBoss()) bossBar.setProgress(getHealth() / getMaxHealth());
    }

    @Override
    public void startSeenByPlayer(ServerPlayer p) {
        super.startSeenByPlayer(p);
        if (isBoss()) bossBar.addPlayer(p);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer p) {
        super.stopSeenByPlayer(p);
        bossBar.removePlayer(p);
    }

    /** Fullscreen jumpscare + scream + short darkness. */
    public void scare(ServerPlayer sp) {
        scareCooldown = 240;
        Net.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new JumpscarePacket(mt().ordinal(), 40));
        sp.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof ServerPlayer sp && scareCooldown <= 0) scare(sp);
        return hit;
    }

    // ------------------------------------------------------------------ sounds

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.AMBIENT.get(mt()).get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource src) {
        return ModSounds.HURT.get(mt()).get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.DEATH.get(mt()).get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 120;
    }

    // ------------------------------------------------------------------ helpers

    /** Is this player's crosshair on the entity (same math as Enderman)? */
    public static boolean isLookingAt(Player p, LivingEntity e) {
        Vec3 view = p.getViewVector(1.0F).normalize();
        Vec3 to = new Vec3(e.getX() - p.getX(), e.getEyeY() - p.getEyeY(), e.getZ() - p.getZ());
        double dist = to.length();
        if (dist < 0.001) return true;
        to = to.normalize();
        return view.dot(to) > 1.0 - 0.05 / dist && p.hasLineOfSight(e);
    }

    boolean watchedByAnyPlayer() {
        for (Player p : level().players()) {
            if (!p.isSpectator() && distanceToSqr(p) < 48 * 48 && isLookingAt(p, this)) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ goals

    /** WEEPING: cannot move or attack while any player is looking at it. */
    static class FreezeWhenWatchedGoal extends Goal {
        private final YtMonster m;

        FreezeWhenWatchedGoal(YtMonster m) {
            this.m = m;
            setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
        }

        @Override public boolean canUse() { return m.getTarget() != null && m.watchedByAnyPlayer(); }
        @Override public boolean canContinueToUse() { return canUse(); }
        @Override public void start() { m.getNavigation().stop(); }
        @Override public void tick() {
            m.getNavigation().stop();
            m.setDeltaMovement(0, Math.min(0, m.getDeltaMovement().y), 0);
        }
    }

    /** STALKER: creeps closer only when unseen. Stare at it too long (or get close) and it charges. */
    static class StalkGoal extends Goal {
        private final YtMonster m;

        StalkGoal(YtMonster m) {
            this.m = m;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() { return m.stalking && m.getTarget() != null; }
        @Override public boolean canContinueToUse() { return canUse(); }

        @Override
        public void tick() {
            LivingEntity t = m.getTarget();
            if (t == null) return;
            m.getLookControl().setLookAt(t, 30, 30);
            double d = m.distanceTo(t);
            boolean watched = t instanceof Player p && isLookingAt(p, m);
            if (watched) {
                m.watchTicks++;
                m.getNavigation().stop();
            } else {
                m.watchTicks = Math.max(0, m.watchTicks - 1);
                if (d > 10) m.getNavigation().moveTo(t, 0.6D); else m.getNavigation().stop();
            }
            if (m.watchTicks > 50 || d < 6 || m.hurtTime > 0) {
                m.stalking = false;
                m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, 1, false, false));
                if (t instanceof ServerPlayer sp) m.scare(sp);
            }
        }
    }
}
