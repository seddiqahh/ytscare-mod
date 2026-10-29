# YT Horror – Jumpscare Monsters (Minecraft Forge 1.20.1)

14 monsters, one per skin you sent. Each has its own **size, stats, behaviour, fullscreen jumpscare style,
scream, ambient whispers/growls, hurt and death sounds**. On top of that the mod adds random ambient scares
(whispers, footsteps behind you, knocking, breathing, heartbeat, fake chat messages, fake jumpscares).

## 1. Build it
1. Install **JDK 17**.
2. Download the **Forge 1.20.1 MDK** (47.3.0) from files.minecraftforge.net, unzip it, and either
   - copy this project's `src/`, `build.gradle`, `settings.gradle`, `gradle.properties` over the MDK's, **or**
   - run `gradle build` here if you have Gradle 8.1+ installed (the MDK's `gradlew` wrapper is easiest).
3. Run `./gradlew build` (Windows: `gradlew build`).
4. Your mod: `build/libs/ytscare-1.20.1-1.0.0.jar` -> put in `.minecraft/mods` with Forge 1.20.1 (47.x).
5. To test straight from the IDE: `./gradlew runClient`.

## 2. Skins (already included)
Your 14 real skin files are installed in `src/main/resources/assets/ytscare/textures/entity/`. Mapping:

| # | file | monster | size | behaviour | arms |
|---|---|---|---|---|---|
| 1  | `red_spiral.png`   | Khazneh     | 1.3x | chases | slim |
| 2  | `suit_shade.png`   | Ury  | 1.55x | stalker | wide |
| 3  | `grin_thing.png`   | YCN  | 0.8x, very fast | chases | slim |
| 4  | `red_hoodie.png`   | zSaeedx        | 0.95x | teleports behind you | slim |
| 5  | `tuxedo_shade.png` | 3zez      | 1.25x | freezes when watched | wide |
| 6  | `split_mask.png`   | Lunge        | 1.1x | teleports behind you | wide |
| 7  | `crown_prince.png` | OmarFive  | 1.1x | chases | wide |
| 8  | `duck.png`         | Rest         | 0.65x, tiny + fast | chases | slim |
| 9  | `smiley.png`       | SeddiQ             | 1.0x | freezes when watched | slim |
| 10 | `hood_kid.png`     | Gray     | 1.0x | stalker | wide |
| 11 | `galaxy.png`       | Fariis      | 2.4x BOSS, 220 HP, boss bar | chases | wide |
| 12 | `red_king.png`     | ii3mory     | 1.7x, 120 HP, boss bar | teleports behind you | slim |
| 13 | `beanie.png`       | S3eeDx | 1.0x | freezes when watched | slim |
| 14 | `panda.png`        | Zetrex      | 1.4x | stalker | slim |

"slim" = Alex 3px arms. This is set per monster in `MonsterType.java` (last argument). If you swap a skin for one with
the other arm type, flip that `true/false`, otherwise the arms look glitchy.

Rename monsters (e.g. to the real YouTuber names) in `MonsterType.java` (`displayName`) **and**
`assets/ytscare/lang/en_us.json`. Sizes/health/speed/damage are the numbers in the same enum.
The fullscreen jumpscare uses the **face of the skin** (head + hat layer).

## 3. Behaviours
- **CHASER** – runs straight at you.
- **STALKER** – stands far away staring at you and creeps closer when you look away. Stare at it too long or let it get
  close and it screams and charges with a speed boost.
- **WEEPING** – cannot move while you are looking at it (Weeping Angel style). Look away and it's on you.
- **TELEPORTER** – periodically teleports right behind you with a whisper + heartbeat.

When a monster gets within ~3.5 blocks (or hits you): fullscreen jumpscare (SHAKE / ZOOM / GLITCH / FLASH style),
loud scream, red flash and a short Darkness effect.

## 4. Sounds
`tools/generate_assets.py` synthesises all custom sounds (whispers, creepy voices, screams, knocking, steps, breathing,
heartbeat, one jumpscare screech per monster) and mixes them with vanilla horror sounds (Warden, Enderman, Ghast...).
To use **your own recordings / real voice clips**: drop `.ogg` files (mono, 44.1kHz) in
`assets/ytscare/sounds/...` and point to them in `assets/ytscare/sounds.json`
(e.g. `"name": "ytscare:whisper/1"` -> your file `sounds/whisper/1.ogg`).

## 5. Commands (need OP / cheats)
```
/ytscare spawn <monster>        spawn one 6 blocks in front of you
/ytscare jumpscare <monster>    test the fullscreen jumpscare on yourself
/ytscare ambient [0-7]          test an ambient scare (0-1 whisper, 2 scream, 3 steps, 4 knock, 5 chat, 6 breath, 7 fake jumpscare)
```
Spawn eggs are in the **YT Horror** creative tab. Monsters also spawn naturally in the Overworld at night / in caves
(rarities in `data/ytscare/forge/biome_modifier/`).

## 6. Config  (`config/ytscare-common.toml`)
`ambientScares`, `ambientScareIntervalTicks` (900 = about one event per 45 s per player), `fakeJumpscares`, `fakeChatMessages`,
`glowingEyes`, `stalkerHeartbeat`, `heartbeatRange` (40), `lightDisruption`, `lightDisruptionRadius` (8), `torchesBurnOut`.

## 6b. Atmosphere features
- **Glowing eyes** - `YtEyesLayer` draws `textures/entity/eyes/<id>.png` at full brightness (strongest in the dark, faint in daylight).
  Only the eye pixels are painted in that PNG - edit it (or the `EYES` table in `tools/generate_assets.py`) to move/recolour eyes.
  The Smiley skin has no eyes, so purple ones were added.
- **Custom loot** - 4 new items (Fear Shard, Cursed Tape, Monster Eye, Abyss Heart) + themed vanilla drops per monster.
  Everything is in `data/ytscare/loot_tables/entities/<id>.json` (Looting works). Bosses (Fariis, Blood King) always drop an Abyss Heart.
  Rare drops (tape, eye) need a player kill.
- **Stalker heartbeat** - `StalkerHeartbeat` (client): STALKER monsters (Faceless Suit, Hooded One, Zetrex) within
  `heartbeatRange` make a heartbeat that goes from ~50 to ~150 bpm and gets louder as they approach.
- **Light disruption** - `LightDisruption` (server): torches (all 4 types) burn out into a stick, campfires/candles are snuffed,
  powered redstone lamps flicker. `FlickerClient` (client) flickers the whole screen so glowstone/lanterns flicker too.
  Set `torchesBurnOut=false` if you don't want torches destroyed.
- **Faceless Suit walk** - custom animation in `YtRenderer.YtModel.suitWalk`: slow long stiff strides, limp dragging arms,
  hanging tilted head; arms reach forward and the stride speeds up when it charges.


## Re-generating assets
`python3 tools/generate_assets.py` (needs `numpy`, `pillow`, `ffmpeg`) regenerates sounds.json, lang, spawn rules, eyes, item textures, loot tables and the synthetic sounds.
Add `--no-audio` to keep your own replaced .ogg files. It does NOT touch your skins (placeholder skins only with `--placeholder-skins`).

## Testing tips
Monsters ignore players in Creative/Spectator mode (vanilla targeting rule) - test in Survival/Adventure, or use
`/ytscare jumpscare <monster>` which works in any mode.
