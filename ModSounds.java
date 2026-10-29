package com.example.ytscare;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.Map;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, YtScareMod.MODID);

    // per monster
    public static final Map<MonsterType, RegistryObject<SoundEvent>> JUMPSCARE = new EnumMap<>(MonsterType.class);
    public static final Map<MonsterType, RegistryObject<SoundEvent>> AMBIENT = new EnumMap<>(MonsterType.class);
    public static final Map<MonsterType, RegistryObject<SoundEvent>> HURT = new EnumMap<>(MonsterType.class);
    public static final Map<MonsterType, RegistryObject<SoundEvent>> DEATH = new EnumMap<>(MonsterType.class);

    // global ambient scares
    public static final RegistryObject<SoundEvent> WHISPER = reg("ambient.whisper");
    public static final RegistryObject<SoundEvent> WHISPER_VOICE = reg("ambient.voice");
    public static final RegistryObject<SoundEvent> SCARE = reg("ambient.scare");
    public static final RegistryObject<SoundEvent> KNOCK = reg("ambient.knock");
    public static final RegistryObject<SoundEvent> STEPS = reg("ambient.steps");
    public static final RegistryObject<SoundEvent> BREATH = reg("ambient.breath");
    public static final RegistryObject<SoundEvent> HEARTBEAT = reg("ambient.heartbeat");
    /** one single lub-dub, replayed by the client at a speed that depends on the distance of a stalker */
    public static final RegistryObject<SoundEvent> HEARTBEAT_BEAT = reg("ambient.heartbeat_beat");

    static {
        for (MonsterType t : MonsterType.values()) {
            JUMPSCARE.put(t, reg(t.id + ".jumpscare"));
            AMBIENT.put(t, reg(t.id + ".ambient"));
            HURT.put(t, reg(t.id + ".hurt"));
            DEATH.put(t, reg(t.id + ".death"));
        }
    }

    private static RegistryObject<SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(YtScareMod.MODID, name)));
    }
}
