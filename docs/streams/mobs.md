# Stream Z: zombified piglins and other mob changes

Package `mobs` (client: `mobs/client`). Config `mobs` (`enabled`). No entity ids, no packets. Patch P1
(`asm/PigZombieTransformer`, see `docs/PATCHES.md`), the first patch of this mod's own coremod.

## Changelog section

*   **Zombie pigmen are now zombified piglins**: 1.16's name, model (the piglin's wide head, snout and tusks, one ear
    torn off, arms raised when hostile), texture and sounds. They are still the same mob, so existing worlds,
    spawners, farms and other mods' settings keep working.
*   **1.16's anger rules**: a zombified piglin is angry only at whoever hit it, never at every player nearby. When
    one is hit, every zombified piglin around (its follow range across, 10 blocks up and down) joins the fight, and
    while one can see its target it raises the alarm again every 4 to 6 seconds. Out of sight, it forgets after 20 to
    39 seconds; it forgives you at once when you die. Hit by another mob, it fights that mob and calms down once the
    mob is dead. Only one angry at you keeps you from sleeping.
*   Zombified piglins avoid walking through lava and never spawn on nether wart blocks.

## How it works

*   Vanilla's `minecraft:zombie_pigman` keeps its id, class (`EntityPigZombie`), loot table (1.16's is the same three
    pools), stats and spawn egg. Its name is 1.12.2's own key `entity.PigZombie.name`, overridden in this mod's
    `en_us.lang` (`tools/lang/vanilla_keys.py` maps it to `entity.minecraft.zombified_piglin`, so the other
    languages get Mojang's name).
*   **Anger** (1.16.5's `ZombifiedPiglinEntity` and `IAngerable`), state in a capability (`mobs/Anger`, saved as
    1.16's `AngerTime` and `AngryAt`), rules in `mobs/ZombifiedPiglins`:
    *   On joining a world, the pigman's two 1.12.2 target tasks (inner classes of `EntityPigZombie`) are removed and
        1.16's added: `HurtByTargetGoal` (fights back at an unseen attacker too, alarms every pigman in follow range
        with no target) at 1, and the nearest player it is angry at (1 in 10, sight needed) at 2. Tasks other mods
        added stay. Path cost of lava 8. This runs only from `EntityJoinWorldEvent`, on the new entity, never from
        a goal.
    *   Each tick (`LivingUpdateEvent`, skipped without AI or while dying): the first angry grunt, 0-1 s after taking
        a target; a new target starts 20-39 s (400-780 ticks) of anger at it; the timer runs only while the target
        is not a player; anger at a mob ends when the target is gone and the mob it was angry at still exists in the
        world (dead or dying); with a target, an alarm every 80-120 ticks if it can see it. While angry, its death
        counts as a player kill (1.16's `lastHurtByPlayerTime = tickCount`). Taking a player as target sets it as
        the attacking player (`setLastHurtByPlayer`).
    *   A player's death (`LivingDeathEvent`, lowest priority) calms every pigman angry at them within 32 across and
        10 up and down (`forgiveDeadPlayers`, on by default in 1.16).
    *   1.16's `universalAnger` and `forgiveDeadPlayers` game rules do not exist in 1.12.2: their defaults (off, on)
        are built in.
    *   A pigman saved angry by 1.12.2 (its `Anger` and `HurtBy` tags) stays angry at the same player for the time it
        had left.
*   **Patch P1** switches 1.12.2's own anger off and lets vanilla's code read the new state: `becomeAngryAt` does
    nothing (it was 20-40 s of anger at every player); `isAngry()` answers from the capability, so vanilla's own
    speed boost (+0.05, adults only) applies; `isPreventingPlayerRest(player)` is 1.16's `isAngryAt(player)`; the
    ambient sound is the angry grunt while angry; hurt and death sounds are the zombified piglin's. Each patched
    method starts with `if (PigmanHooks.active())`, so with `mobs.enabled` off the class behaves as vanilla's.
    Without the patch (another coremod changed those methods) the targeting rules still apply and a warning is
    logged; sounds, speed boost and sleeping stay 1.12.2's.
*   **Spawning**: `LivingSpawnEvent.CheckSpawn` denies a pigman on a nether wart block (1.16's
    `checkZombifiedPiglinSpawnRules`), for natural spawns and spawners alike.
*   **Client**: `RenderZombifiedPiglin` replaces vanilla's renderer for `EntityPigZombie` through Forge's renderer
    registry: 1.16.5's `PiglinModel` as built for the zombified piglin (player model with sleeves and jacket, piglin
    head, snout, tusks, right ear only, ears flapping with movement), the zombie arm pose when aggressive, armor
    that follows that pose (`ModelZombie` 0.5 and 1.02), shadow 0.5. Texture and four sound events from 1.18,
    under this mod's id (`tools/mobs/generate.py`), with 1.16's subtitles.

### Decided from the sources

*   The brief said "the group within 20 blocks that can see". 20 blocks is Bedrock's radius (minecraft.wiki); Java
    1.16.5 alarms within the follow range (35 for zombified piglins, a little more with the spawn bonus), 10 up and
    down, with no sight needed for the first alarm and sight needed for the repeated one. The Java numbers are used.
*   minecraft.wiki says the Java anger lasts 20-55 s; 1.16.5's code is `rangeOfSeconds(20, 39)`. The code is used.

## Tested headlessly

`-Pselftest=mobs`: **pass=54 fail=0**; `-Pselftest=all` (every stream): **pass=7197 fail=0**; `check_assets.py`: 0 errors,
647 sound files. (One earlier `all` run on another fresh seed failed 2 checks in the fungus test: a tree from chunk
population grew into its cleared area where the crimson door goes. Seed-dependent and not this stream's.)

*   P1 is applied and active; the name line is in the lang file.
*   A spawned pigman has 1.16's two target goals and none of 1.12.2's; lava path cost 8; vanilla's counter unused.
*   A player's hit: no 1.12.2 anger; it targets that player, angry at them for 400-780 ticks; `isAngry()` true;
    angry at the hitter and not another player; stops only that player from sleeping; a pigman 4 blocks off joins,
    one beyond follow range and one 12 blocks above do not; the adult speed boost is on; angry, hurt, death and calm
    ambient sounds are the new events.
*   The timer holds while it targets the player; with the player beyond follow range the target is dropped, and it
    calms after exactly its timer, forgets the player, lets them sleep and loses the speed boost.
*   A dead player is forgiven at once.
*   Hit by a skeleton: it fights and is angry at the skeleton, not at the player beside it; the timer runs; the
    skeleton dead, it calms at once.
*   The repeated alarm reaches a pigman that arrived later within 121 ticks; shut in a stone box with no sight of the
    player, it still fights back but raises no alarm.
*   Anger survives a save and load; vanilla's `Anger` tag stays unused; a pigman saved angry by 1.12.2 carries its
    player and time over; an angry baby gets no speed boost.
*   No spawning on a nether wart block; stone is left to vanilla.

## Not tested

*   Anything visual or audible: the model, texture, ear animation, arm pose, armor fit, baby proportions, sounds and
    subtitles in a real client.
*   Live AI over time with real players (chasing across terrain, losing sight in caves, the sleeping message).
*   The patch against other coremods that touch `EntityPigZombie` in a real modpack.
*   The lang override when the stream is switched off (resources cannot be switched off: the name stays "Zombified
    Piglin" while the model goes back to 1.12.2's).

## For the integrator

*   This stream adds the mod's coremod (`asm/NetherUpdateCore`, manifest attributes and `fml.coreMods.load` in
    `build.gradle`, `docs/PATCHES.md`). Another stream adding patches registers its transformer in
    `NetherUpdateCore.getASMTransformerClass`.
*   `tools/lang/vanilla_keys.py` gained one `SPECIAL` line; rerun it (and `build_langs.py`) after the merge.
*   Not done here, as not 1.16's own: 1.16's zombified piglin rides with `getMyRidingOffset` -0.05 as a baby (1.12.2's
    zombie: 0.0), which needs a new method on `EntityPigZombie` (a candidate in `docs/PATCHES.md`).
