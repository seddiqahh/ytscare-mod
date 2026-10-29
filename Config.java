package com.example.ytscare;

import net.minecraftforge.common.ForgeConfigSpec;

/** Generated at config/ytscare-common.toml */
public class Config {
    private static final ForgeConfigSpec.Builder B = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue AMBIENT = B
            .comment("Random whispers / footsteps / knocks / scary sounds around the player")
            .define("ambientScares", true);

    public static final ForgeConfigSpec.IntValue INTERVAL = B
            .comment("Average number of ticks between ambient scares PER PLAYER (20 ticks = 1 sec). Lower = more scares.")
            .defineInRange("ambientScareIntervalTicks", 900, 20, 1_000_000);

    public static final ForgeConfigSpec.BooleanValue FAKE_JUMPSCARES = B
            .comment("Sometimes show a FAKE fullscreen jumpscare with no monster nearby")
            .define("fakeJumpscares", true);

    public static final ForgeConfigSpec.BooleanValue FAKE_CHAT = B
            .comment("Sometimes show creepy fake chat messages from the monsters")
            .define("fakeChatMessages", true);

    public static final ForgeConfigSpec.BooleanValue GLOWING_EYES = B
            .comment("Monsters get full-bright glowing eyes (strongest in the dark)")
            .define("glowingEyes", true);

    public static final ForgeConfigSpec.BooleanValue HEARTBEAT = B
            .comment("Heartbeat that speeds up and gets louder as a STALKER-type monster gets closer")
            .define("stalkerHeartbeat", true);

    public static final ForgeConfigSpec.IntValue HEARTBEAT_RANGE = B
            .comment("Distance in blocks at which a stalker starts to make your heart beat")
            .defineInRange("heartbeatRange", 40, 8, 128);

    public static final ForgeConfigSpec.BooleanValue LIGHT_DISRUPTION = B
            .comment("Monsters put out torches / campfires / candles, flicker redstone lamps and flicker your screen")
            .define("lightDisruption", true);

    public static final ForgeConfigSpec.IntValue LIGHT_RADIUS = B
            .comment("Blocks around a monster in which lights get disturbed (big monsters get more)")
            .defineInRange("lightDisruptionRadius", 8, 2, 32);

    public static final ForgeConfigSpec.BooleanValue TORCHES_BREAK = B
            .comment("true = torches near monsters burn out (become a stick). false = only flicker effects, nothing is destroyed")
            .define("torchesBurnOut", true);

    public static final ForgeConfigSpec SPEC = B.build();
}
