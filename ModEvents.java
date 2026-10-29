package com.example.ytscare;

import com.example.ytscare.entity.YtMonster;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = YtScareMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEvents {

    @SubscribeEvent
    public static void attributes(EntityAttributeCreationEvent e) {
        for (MonsterType t : MonsterType.values()) {
            e.put(ModEntities.TYPES.get(t).get(), YtMonster.attributes(t).build());
        }
    }

    @SubscribeEvent
    public static void spawnRules(SpawnPlacementRegisterEvent e) {
        for (MonsterType t : MonsterType.values()) {
            e.register(ModEntities.TYPES.get(t).get(), SpawnPlacements.Type.ON_GROUND,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules,
                    SpawnPlacementRegisterEvent.Operation.OR);
        }
    }
}
