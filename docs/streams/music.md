# Stream M: music and sounds

Package `music` (client: `music/client`). Config `music` (`enabled`, `pigstepInFortresses`, `biomeMusic`,
`biomeAmbience`). No entity ids, no packets. Patch P2 (`asm/JukeboxTransformer`, see `docs/PATCHES.md`).

## Changelog section

*   **Pigstep**, Lena Raine's music disc. It plays in a jukebox like any other disc, and a comparator reads 13 from
    it. It is found in bastion chests, as in 1.16; an option (off by default) also puts it in nether fortress chests,
    as often as in a bastion's (5 in 89).
*   **1.16's nether music**: each nether biome has its own tracks. *Rubedo* plays in nether wastes, *Chrysopoeia*
    in crimson forests, and *So Below* in soul sand valleys and basalt deltas, along with the four old nether
    tracks. Warped forests have no music, as in vanilla. The nether's music now comes every 10 to 20 minutes, like
    the overworld's.
*   **Music no longer stops abruptly** when you switch game modes or walk into another biome. The playing track
    finishes first. The menu, the credits and the End still cut it off, and so does changing dimension.
*   **Nether ambience**: each nether biome has its own sound loop that fades in and out as you cross biomes. It
    also has random additions (wind, whispers, debris, creaks) and mood sounds after long darkness. These replace
    the cave sounds in the nether biomes.

## How it works

*   **Pigstep** (`music/ItemMusicDisc`, `MusicContent`): 1.12.2's own `ItemRecord`, so the jukebox, the "Now
    Playing" line, the tooltip, the rare name color and the record stat are vanilla's. Registry name
    `music_disc_pigstep`, sound event `music_disc.pigstep` (streamed), texture from 1.18. 1.16's `analogOutput`
    is on the item. Patch P2 makes the jukebox's comparator override read it, since 1.12.2 works the signal out
    from item ids. Creepers killed by skeletons never drop it, as in 1.16: 1.12.2's creeper only drops vanilla's
    twelve ids, and 1.16's `creeper_drop_music_discs` tag leaves Pigstep out too.
*   **Fortress chests**: a `LootTableLoadEvent` pool on `chests/nether_bridge` (`netherupdatebackport:pigstep`).
    It rolls once, with Pigstep at weight 5 and nothing at weight 84, which is 1.16's `bastion_other` chance (5 of
    89). Behind `music.pigstepInFortresses` (default off since bastions merged: 1.16 has Pigstep in bastions only; a new key, so 0.1.0's `pigstepInFortressChests = true` does not carry over), read when loot tables load.
*   **Which music** (`music/MusicChoice`): the music-choosing logic, alone in one class with no client types:
    *   `Track` is 1.16's `BackgroundMusicSelector`: an event, min and max delay, and whether it replaces.
    *   `MENU` (20-600, replaces), `CREATIVE` and `GAME` (12000-24000, no), `CREDITS` and `END_BOSS` (0, replace),
        and `END` (6000-24000, replaces) are 1.16.5's `BackgroundMusicTracks` on 1.12.2's own events.
    *   `nether(kind)` gives `music.nether.<biome>` as game music.
    *   `inWorld(...)` is 1.16.5's `getSituationalMusic`. A dimension's own music comes first (Forge's
        `WorldProvider.getMusicType`, replacing, as in 1.12.2). The End gets end or boss music. Outside the nether,
        creative flying gets creative music. Then the biome's music, if it has any, else game music.
    *   The biome is `biome.NetherBiomes.at(world, pos)`, stream V's lookup.
*   **When it plays** (`music/MusicPlayer`): 1.16.5's `MusicTicker`, line for line, over a `Speaker` interface. A
    different track stops the playing one only if it replaces; otherwise the playing track finishes and the next
    waits its delay. `stop()` (changing dimension, as 1.16's `handleRespawn`) adds 100 ticks to the wait. An event
    with no sounds (warped forest's) plays nothing, and the wait starts over.
*   **Client** (`music/client`):
    *   `Music116Ticker` replaces `Minecraft.musicTicker` on the first client tick. It does this only when that
        field still holds vanilla's own `MusicTicker`; a ticker another mod put there is left alone, with a
        warning in the log. The new ticker reads the situation (credits screen, menu, dimension, boss bar,
        creative flying, nether biome) and plays through the sound handler. Requires `music.biomeMusic` (restart).
    *   `BiomeSounds` is 1.16.5's `BiomeSoundHandler`, one per client player, ticked at the end of the player's
        tick. On a biome change every loop fades out over 40 ticks and the new biome's fades in, as 1.16 does. The
        loop follows the listener, since 1.12.2 has no relative sounds, with no attenuation. An addition plays at
        the listener with a 0.0111 chance per tick. For the mood, each tick samples one block within 8 of the eyes.
        Darkness there (no sky light, block light 0) raises the mood by 1/6000 and light lowers it. At 1 the mood
        sound plays 2 blocks past that block, with linear attenuation.
    *   `MusicClient` also drops 1.12.2's `ambient.cave` sounds while a nether biome's ambience plays, because
        1.16's nether biomes have their own mood instead. `music.biomeAmbience` switches all of it live.
*   **Assets** (`tools/music/generate.py`, from the 1.18 asset index). The 21 sound events are five
    `music.nether.*`, fifteen `ambient.<biome>.loop/mood/additions` and `music_disc.pigstep`. The four old nether
    tracks are byte for byte 1.12.2's own files, so `sounds.json` names them `minecraft:music/game/nether/netherN`
    and they are not copied. The 1.18 sound lists are used: they are newer than 1.16.5's in some ambience entries,
    the music weights are the same, and current Java wins. None of these events has a subtitle in vanilla (1.16.5
    or 1.18), so no subtitle lines were added.

### Decided from the sources

*   **Warped forest is silent**: 1.16.5's and 1.18's `music.nether.warped_forest` has no sounds, so nothing plays
    there. The ticker's wait just starts over.
*   **Another mod's nether biome** (the nether with another biome provider): 1.16 would play that biome's own music
    or the overworld's. This mod plays nether wastes' music, the nether's default biome.
*   **Dimension change** waits: while a track plays, the ticker holds its wait at the max delay. 1.16's
    `stopPlaying` then adds 100 ticks, so the new dimension's music comes 10 to 20 minutes later, not at once.
*   **Underwater music** is 1.13's (Update Aquatic's version), not added here.

## Tested headlessly

`-Pselftest=music`: **pass=217 fail=0**. `-Pselftest=all` (every stream, fresh world): **pass=6842 fail=0**.
`check_assets.py`: **84 blocks, 884 states, 88 item models, 809 sound files, 0 errors**. The dedicated server boots
with the stream on.

*   All 21 sound events are registered. Pigstep is a 1.12.2 record: it stacks to 1, is rare, plays
    `music_disc.pigstep`, has signal 13 and has its translation key.
*   P2 is applied. Pigstep goes into a jukebox through its own right click and leaves the hand. The jukebox gives 13,
    and a real comparator beside it outputs 13. 13, cat and wait still give 1, 2 and 12, and an empty jukebox 0.
*   The fortress table has the pool. In 20000 rolls with a fixed seed, the share is inside 0.048-0.064 (expected
    0.0562). Dungeon chests have no pool.
*   Every `MusicChoice` track's event, delays and replace flag. `inWorld` covers overworld survival and creative,
    the End with and without the dragon, each nether biome (creative too), another mod's nether biome, a hell biome
    in another dimension, and a dimension's own music.
*   `MusicPlayer` with a fake speaker:
    *   the first track starts after 101 ticks;
    *   creative does not stop the survival track;
    *   an ended track's successor waits 12000-24000 ticks;
    *   the menu stops the game track and starts within 11 ticks;
    *   walking between nether biomes keeps the track;
    *   `stop()` adds 100 ticks;
    *   warped forest's empty track is let go and re-waits.
*   The ambience numbers for all five biomes (6000, 8, 2.0, 0.0111), the mood arithmetic, and a mood sound after
    6000 dark ticks.
*   `sounds.json` holds every event. Warped forest's music is empty. Each biome track has its weight (6 or 7),
    volume 0.5 and streaming, beside the four old tracks as `minecraft:` files. The lang lines are present.

## Not tested

*   Anything audible, since no client was run: whether the ticker installs in a real client, the tracks heard, the
    fades, panning of the loops and additions, mood sound placement, cave-sound suppression, the "Now Playing"
    line, and the tooltip.
*   Ticker conflicts with other music mods in a real pack (the step-aside path only logs).
*   Live config toggling of `biomeAmbience` in a client.

## For the integrator

*   **Jar size:** the sound files are vanilla's, unchanged. *Rubedo*, *Chrysopoeia* and *So Below* are about 16 MB
    each, Pigstep 4.8 MB and the ambience 34 MB. `sounds/` grows from 8.8 MB to about 92 MB, so the jar goes from
    about 11 MB to about 95 MB. Re-encoding the three long tracks at a lower bitrate would save about 35 MB. That
    is the user's call; it is not done here.
*   This stream adds patch P2 to `NetherUpdateCore.getASMTransformerClass` and `docs/PATCHES.md`.
*   `tools/check_assets.py` now accepts `minecraft:` sound files found in 1.12.2's asset index (they are not in the
    client jar). `tools/lang/vanilla_keys.py` gained one `SPECIAL` line for Pigstep's description key. Rerun it,
    `build_langs.py` and `config_lang.py` after the merge.
*   When stream X adds bastion loot, it owns Pigstep's real source (`bastion_other`, weight 5 of 89). X should
    then retire the fortress pool, by a new default-off key (old defaults do not migrate) or by skipping it
    once bastions generate.
*   Backport Core's presentation module can take `MusicChoice` (the rules), `MusicPlayer` (the ticker) and the
    ticker swap in `music/client/Music116Ticker` as they are. Underwater music (Update Aquatic) and the menu pool
    would then join `MusicChoice` there.
