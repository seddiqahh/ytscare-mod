package com.example.ytscare;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * One entry per monster. To rename a monster, change displayName here AND in lang/en_us.json.
 * To make one bigger/smaller change "scale". Texture = assets/ytscare/textures/entity/<id>.png (64x64 skin).
 */
public enum MonsterType {
    //          id             name                   scale  hp    speed  dmg  behaviour            jumpscare        egg1      egg2      slim arms
    RED_SPIRAL  ("red_spiral",   "Khazneh",       1.30f,  60, 0.27,  6, Behavior.CHASER,     Style.SHAKE,   0x8A0000, 0x2A0000, true),
    SUIT_SHADE  ("suit_shade",   "Ury",    1.55f,  70, 0.25,  8, Behavior.STALKER,    Style.ZOOM,    0x0B0B0B, 0xFFFFFF, false),
    GRIN_THING  ("grin_thing",   "YCN",    0.80f,  30, 0.38,  4, Behavior.CHASER,     Style.GLITCH,  0x2A2A2A, 0xFFFFFF, true),
    RED_HOODIE  ("red_hoodie",   "zSaeedx",          0.95f,  40, 0.30,  5, Behavior.TELEPORTER, Style.FLASH,   0x862727, 0x6A8AA5, true),
    TUXEDO_SHADE("tuxedo_shade", "3zez",        1.25f,  50, 0.26,  7, Behavior.WEEPING,    Style.ZOOM,    0x000000, 0xE0E0E0, false),
    SPLIT_MASK  ("split_mask",   "Lunge",          1.10f,  50, 0.29,  6, Behavior.TELEPORTER, Style.GLITCH,  0x000000, 0xD8160F, false),
    CROWN_PRINCE("crown_prince", "OmarFive",    1.10f,  55, 0.28,  6, Behavior.CHASER,     Style.FLASH,   0xD4900F, 0x5A96C8, false),
    DUCK        ("duck",         "Rest",           0.65f,  25, 0.42,  3, Behavior.CHASER,     Style.SHAKE,   0x1E1E1E, 0xD4D46A, true),
    SMILEY      ("smiley",       "SeddiQ",               1.00f,  45, 0.27,  5, Behavior.WEEPING,    Style.GLITCH,  0xE6C200, 0x3A0F6A, true),
    HOOD_KID    ("hood_kid",     "Gray",       1.00f,  45, 0.30,  6, Behavior.STALKER,    Style.ZOOM,    0x4D5361, 0x1E1E1E, false),
    GALAXY      ("galaxy",       "Fariis",        2.40f, 220, 0.22, 14, Behavior.CHASER,     Style.GLITCH,  0x4010A0, 0x20C0E0, false),
    RED_KING    ("red_king",     "ii3mory",       1.70f, 120, 0.27, 10, Behavior.TELEPORTER, Style.SHAKE,   0xA8202A, 0xD4900F, true),
    BEANIE      ("beanie",       "S3eeDx",   1.00f,  40, 0.28,  5, Behavior.WEEPING,    Style.FLASH,   0xB0A094, 0x504848, true),
    PANDA       ("panda",        "Zetrex",        1.40f,  80, 0.25,  8, Behavior.STALKER,    Style.SHAKE,   0x000000, 0xDDDDDD, true);
    /** CHASER: runs at you. STALKER: stares from afar, charges if you look at it too long / get close.
     *  WEEPING: freezes while you look at it. TELEPORTER: appears behind you. */
    public enum Behavior { CHASER, STALKER, WEEPING, TELEPORTER }

    /** How the fullscreen jumpscare looks. */
    public enum Style { SHAKE, ZOOM, GLITCH, FLASH }

    public final String id;
    public final String displayName;
    public final float scale;
    public final double health;
    public final double speed;
    public final double damage;
    public final Behavior behavior;
    public final Style style;
    public final int eggPrimary;
    public final int eggSecondary;
    /** true = Alex/slim 3px arms, false = Steve/wide 4px arms (must match the skin file!) */
    public final boolean slim;
    public final ResourceLocation texture;
    /** glowing-eyes overlay (only the eye pixels are painted), see textures/entity/eyes/ */
    public final ResourceLocation eyesTexture;

    MonsterType(String id, String displayName, float scale, double health, double speed, double damage,
                Behavior behavior, Style style, int eggPrimary, int eggSecondary, boolean slim) {
        this.id = id;
        this.displayName = displayName;
        this.scale = scale;
        this.health = health;
        this.speed = speed;
        this.damage = damage;
        this.behavior = behavior;
        this.style = style;
        this.eggPrimary = eggPrimary;
        this.eggSecondary = eggSecondary;
        this.slim = slim;
        this.texture = new ResourceLocation(YtScareMod.MODID, "textures/entity/" + id + ".png");
        this.eyesTexture = new ResourceLocation(YtScareMod.MODID, "textures/entity/eyes/" + id + ".png");
    }

    public static MonsterType byId(String id) {
        for (MonsterType t : values()) if (t.id.equals(id)) return t;
        return RED_SPIRAL;
    }

    public static MonsterType byOrdinal(int i) {
        MonsterType[] v = values();
        return v[Math.floorMod(i, v.length)];
    }

    public static List<String> ids() {
        List<String> l = new ArrayList<>();
        for (MonsterType t : values()) l.add(t.id);
        return l;
    }
}
