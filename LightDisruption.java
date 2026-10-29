package com.example.ytscare;

import com.example.ytscare.entity.YtMonster;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Server side of "monsters mess with the light":
 *  - torches (all 4 kinds) burn out and turn into a stick
 *  - campfires, soul campfires, candles and candle cakes get snuffed
 *  - powered redstone lamps flicker off for a few ticks and come back
 * The client side (screen flicker) is in client/FlickerClient.java.
 */
@Mod.EventBusSubscriber(modid = YtScareMod.MODID)
public class LightDisruption {
    /** lamps that are currently switched off by us: dimension -> (pos -> game time when they come back) */
    private static final Map<ResourceKey<Level>, Map<BlockPos, Long>> PENDING = new HashMap<>();

    /** called from YtMonster.aiStep (server only) */
    public static void tick(YtMonster m) {
        if (!Config.LIGHT_DISRUPTION.get() || m.tickCount % 10 != 0) return;
        if (!(m.level() instanceof ServerLevel level)) return;

        MonsterType t = m.mt();
        int r = (int) Math.ceil(Config.LIGHT_RADIUS.get() * Math.max(1.0F, t.scale * 0.75F));
        RandomSource rnd = m.getRandom();
        BlockPos center = m.blockPosition();

        for (int i = 0; i < 20; i++) { // 20 random samples per pass -> lights go out one by one, not all at once
            int dx = rnd.nextInt(2 * r + 1) - r, dy = rnd.nextInt(2 * r + 1) - r, dz = rnd.nextInt(2 * r + 1) - r;
            if (dx * dx + dy * dy + dz * dz > r * r) continue;
            BlockPos pos = center.offset(dx, dy, dz);
            if (!level.hasChunkAt(pos)) continue;
            affect(level, pos, level.getBlockState(pos), rnd);
        }
    }

    private static void affect(ServerLevel level, BlockPos pos, BlockState s, RandomSource rnd) {
        if (s.is(Blocks.TORCH) || s.is(Blocks.WALL_TORCH) || s.is(Blocks.SOUL_TORCH) || s.is(Blocks.SOUL_WALL_TORCH)) {
            if (!Config.TORCHES_BREAK.get()) return;
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            Block.popResource(level, pos, new ItemStack(Items.STICK)); // burnt out
            fizz(level, pos);
        } else if (s.getBlock() instanceof CampfireBlock && s.getValue(CampfireBlock.LIT)) {
            level.setBlock(pos, s.setValue(CampfireBlock.LIT, false), 3);
            fizz(level, pos);
        } else if (s.getBlock() instanceof AbstractCandleBlock && s.getValue(AbstractCandleBlock.LIT)) {
            level.setBlock(pos, s.setValue(AbstractCandleBlock.LIT, false), 3);
            fizz(level, pos);
        } else if (s.is(Blocks.REDSTONE_LAMP) && s.getValue(RedstoneLampBlock.LIT)) {
            Map<BlockPos, Long> map = PENDING.computeIfAbsent(level.dimension(), k -> new HashMap<>());
            BlockPos key = pos.immutable();
            if (map.containsKey(key)) return;
            level.setBlock(pos, s.setValue(RedstoneLampBlock.LIT, false), 2);
            map.put(key, level.getGameTime() + 4 + rnd.nextInt(14)); // dark for 0.2 - 0.9 s
        }
    }

    private static void fizz(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.7F, 0.9F + level.random.nextFloat() * 0.3F);
        level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 6, 0.1, 0.15, 0.1, 0.01);
    }

    /** switch flickered lamps back on */
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.side.isClient() || !(e.level instanceof ServerLevel level)) return;
        Map<BlockPos, Long> map = PENDING.get(level.dimension());
        if (map == null || map.isEmpty()) return;
        long now = level.getGameTime();
        Iterator<Map.Entry<BlockPos, Long>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Long> en = it.next();
            if (en.getValue() > now) continue;
            it.remove();
            restore(level, en.getKey());
        }
    }

    /** never leave a lamp dark when the server stops in the middle of a flicker */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent e) {
        MinecraftServer server = e.getServer();
        for (Map.Entry<ResourceKey<Level>, Map<BlockPos, Long>> en : PENDING.entrySet()) {
            ServerLevel level = server.getLevel(en.getKey());
            if (level == null) continue;
            for (BlockPos pos : en.getValue().keySet()) restore(level, pos);
        }
        PENDING.clear();
    }

    private static void restore(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return;
        BlockState s = level.getBlockState(pos);
        if (s.is(Blocks.REDSTONE_LAMP) && !s.getValue(RedstoneLampBlock.LIT) && level.hasNeighborSignal(pos)) {
            level.setBlock(pos, s.setValue(RedstoneLampBlock.LIT, true), 2);
        }
    }
}
