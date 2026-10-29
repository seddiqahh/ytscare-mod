package com.example.ytscare;

import com.example.ytscare.entity.YtMonster;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.Map;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, YtScareMod.MODID);
    public static final Map<MonsterType, RegistryObject<EntityType<YtMonster>>> TYPES = new EnumMap<>(MonsterType.class);

    static {
        for (MonsterType t : MonsterType.values()) {
            TYPES.put(t, ENTITIES.register(t.id, () ->
                    EntityType.Builder.<YtMonster>of(YtMonster::new, MobCategory.MONSTER)
                            .sized(0.6F * t.scale, 1.95F * t.scale)   // <- hitbox grows with monster size
                            .clientTrackingRange(10)
                            .build(t.id)));
        }
    }
}
