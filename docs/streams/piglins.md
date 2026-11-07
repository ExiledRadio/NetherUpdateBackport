# Stream P: piglins and piglin brutes

Package `piglin`. Config `piglins` (`enabled`, `spawn`). Entity ids 15 (piglin) and 16 (piglin brute). No packets.
Also finishes stream Ow's crossbow copy (package `owned`, entity ids 40-42; see `owned.md`).

## Changelog section

*   **Piglins**: gold-loving nether pig-folk with golden swords or crossbows, sometimes gold armor. They pick up and
    admire gold for six seconds; a gold ingot (thrown or handed to one) is **bartered** for something from current
    Java's table: Soul Speed books and boots, Fire Resistance potions, ender pearls, iron nuggets, string, quartz,
    obsidian, crying obsidian, fire charges, leather, soul sand, nether bricks, spectral arrows, gravel or blackstone.
    They keep other things in an inventory, wear better gear, and eat porkchops.
*   They attack players who wear no gold armor, and anyone who opens a chest (or barrel, shulker box, ender chest,
    container minecart) in front of them or breaks gold blocks, gold ores or gilded blackstone nearby. Hit one and
    its friends join in. They hunt hoglins (and back off when outnumbered), sometimes dance after a kill, fight
    wither skeletons, and keep away from soul fire, soul torches, soul lanterns, soul campfires, zombified piglins
    and zoglins. Babies run from danger and ride baby hoglins.
*   Taken out of the nether, they shake and after 15 seconds become **zombified piglins**.
*   **Piglin brutes**: 50 health, golden axes, hostile to every player whatever they wear, guarding the spot where
    they spawned. They only spawn in bastion remnants.
*   **Snout banner pattern** item.
*   Without Village & Pillage, this mod now also has its **crossbow** (with Quick Charge, Multishot and Piercing), so
    piglins can carry one; with Village & Pillage, they carry its crossbow.
*   Advancements: *Oh Shiny*, *Those Were the Days*, *War Pigs*, *Hot Tourist Destinations*.

## How it works

*   **Brain.** 1.16.5's `PiglinTasks` / `PiglinBruteBrain`, behavior for behavior, run from the entity's server tick
    (`AbstractPiglin.updateAITasks`), not from AI goals: `Memories` (1.16's memories with expiry; anger, admiring
    disabled, hunted recently and home are saved), the sensors (living entities and players within 16 blocks, the
    piglin's own sensor with nemesis, hoglins, zombified, players and gold, wanted items within 8x4, repellents
    8 across / 4 up-down, every 20 ticks), and 1.16's walk-to-target and look tasks that carry out what the behaviors
    choose. Activities in 1.16's order (admire, fight, avoid, celebrate, ride, idle); leaving one forgets its memory.
    Idle wandering and looking are 1.16's weighted shuffled groups (`Shuffled`). The 1.12 goal list only floats and
    opens doors.
*   **Piglin** (`EntityPiglin`): 16 health, speed 0.35 (+20% baby), attack 5, 5 experience, 0.6 x 1.95 (half for a baby),
    eyes 1.74 / 0.93. Spawns: 20% babies; adults a crossbow or golden sword 50/50; each gold armor piece 10%;
    difficulty enchantments; first hunt after 30-120 s. Inventory of 8, dropped on death and conversion.
*   **Gold**: loved items are current Java's `piglin_loved` (gold ores incl. ore name `oreGold` and raw gold from any
    mod, Village & Pillage's bell, gilded blackstone, nether gold ore...); currency is the gold ingot; admire 120 ticks;
    hit by a player = no admiring for 400 ticks; hit while admiring a gold ingot eats it (as vanilla). Guarded
    blocks are `guarded_by_piglins`; chests are caught at `PlayerContainerEvent.Open` (with line of sight), blocks at
    `BlockEvent.BreakEvent` (any idle piglin within 16). Creative and spectator players and peaceful are never targets.
*   **Bartering** (`Bartering`): loot table `netherupdatebackport:gameplay/piglin_bartering`, written by the generator
    from the 1.18.2 jar's table (identical to current Java's: 18 entries, total weight 459). Blackstone, crying
    obsidian and Soul Speed are other streams': those entries name a placeholder and carry this mod's loot condition
    `netherupdatebackport:registered` and functions `set_item` / `enchant_randomly`, so a switched-off stream drops
    its entry from the draw instead of failing the table.
*   **Crossbow**: the piglin times its own load (25 ticks, less with the copy's Quick Charge) and 20-39 tick hold,
    shoots its own arrow at 1.6 with 14 - 4 x difficulty spread (the copy's arrow, so Piercing works; Multishot's side
    bolts), and marks the held crossbow `Charged` so it shows loaded. It never runs another mod's item code
    (`Crossbows`: owner's, then the copy, then Future MC's or Crossbows Backport's, by name). Backs away from targets
    within 5.
*   **Hoglins**: hunting needs `EntityHoglin.canBeHunted()` (the one call into stream H's class; everything else is by
    registry name); outnumbered piglins retreat for 5-20 s; 10% dance after a hoglin kill (1.16's game-time seed).
*   **Zombification**: outside the nether, 300 ticks, then a plain vanilla `EntityPigZombie` with the equipment, age,
    name, persistence and 10 s of Nausea; stream Z's code gives it 1.16 behavior when it joins the world.
*   **Brute** (`EntityPiglinBrute`): 50 health, attack 7, 20 experience, golden axe; targets anger, then any player
    within 12, then wither skeletons; returns home from up to 100 blocks, potters within 5; angry snorts 1.25% a tick
    in a fight; picks up only golden axes; despawns in peaceful.
*   **Spawning**: piglins in `Biomes.HELL` (the nether wastes), weight 15, groups of 4 (`piglins.spawn`). Brutes: none
    naturally.
*   **For stream X**: `BastionMobs.spawn(world, pos, yaw, template)` with 1.16's bastion mob templates
    (`crossbow_piglin`, `sword_piglin`, `melee_piglin`, `melee_piglin_always`; the last two are brutes, as 1.16.2
    made them): persistent, `CannotHunt`, structure-spawn rules (no baby or weapon roll), brute home where placed.
    Stable hoglins: `EntityHoglin.setCannotBeHunted(true)`.
*   **Client**: 1.16's `PiglinModel` (player model, wide head, snout, flapping ears; dancing, admiring, raised and
    swung weapon, crossbow hold and charge poses from `ModelHelper`), armor layer at 0.5 / 1.02, conversion shaking.
    16 sound events and both textures from 1.18.
*   **Snout pattern**: item only (one to a stack, uncommon, "Banner Pattern" / "Snout"); its banner and shield
    textures are shipped under our id for later. No `BannerPattern` constant is added: Village & Pillage's loom only
    takes its own pattern class and would list a new constant as a free pattern (see *Integrator*).

## Advancements

| Advancement | Criteria | Fires now? |
| --- | --- | --- |
| *Oh Shiny* (`nether/distract_piglin`) | events `distract_piglin` (thrown loved item picked up by an adult) or `distract_piglin_directly` (gold handed over), player wearing gold | yes, this stream |
| *Those Were the Days* (`nether/find_bastion`) | event `enter_bastion` | **needs X** |
| *War Pigs* (`nether/loot_bastion`) | any of events `loot_bastion_other`, `loot_bastion_treasure`, `loot_bastion_hoglin_stable`, `loot_bastion_bridge` (1.16's four chest tables) | **needs X** |
| *Hot Tourist Destinations* (`nether/explore_nether`) | nether wastes: vanilla `minecraft:location` in `minecraft:hell` (works now); events `visit_soul_sand_valley`, `visit_warped_forest`, `visit_crimson_forest`, `visit_basalt_deltas` | wastes only; **the other four need V** |

X and V fire them with `common/Triggers.fire(player, "<event>")`. V may instead switch its four criteria to
`minecraft:location` with its biome ids once they are registered (a location criterion naming an unregistered biome
fails the whole advancement, which is why they are events now).

## Tested headlessly

`-Pselftest=piglins`: **pass=179 fail=0** (seven runs in a row); `-Pselftest=all`: **pass=7454 fail=0**;
`check_assets.py`: 0 errors (746 sound files).

*   Registration, spawn eggs, the Snout item, the crossbow copy with its 3 enchantments, 3 projectiles and recipe;
    piglins on Hell's spawn list (15, 4-4).
*   Loved items, currency, repellent items, guarded blocks; the crossbow's loaded tag.
*   400 spawns: babies about 1 in 5 and unarmed; adults all crossbow or sword, about half each; helmets about 1 in 10;
    stats; baby speed and size; no spawning on nether wart blocks. Bastion templates: right mob and weapon,
    persistent, CannotHunt, never a baby, brute home.
*   4590 barters: one stack each; counts in range; Soul Speed I-III on books and boots; potion kinds; every one of
    the 18 outcomes within 4.5 sigma of its weight.
*   Picking up thrown gold into the off hand, admiring 120 ticks, then a barter thrown; gold handed over by a player;
    babies refuse; nuggets to the inventory; a porkchop eaten and no more for a while; walking to a loved item it sees.
*   Anger: breaking stone (no), a gold block (yes, 600 ticks, not from 27 blocks); opening a chest in sight; hurt:
    anger, spread to friends, a baby flees, a player's hit disables admiring; gold armor; target order (nemesis before
    a player, nothing near a zombified piglin).
*   Live AI: a wither skeleton in sight is fought; a zombified piglin close by is avoided (walking away); a soul
    lantern is found as repellent; a sword piglin hits a pig it is angry at; a crossbow piglin charges, loads, shoots,
    unloads.
*   Hoglins: hunting starts (anger, hunted-recently), fight, celebration after the kill; a stable hoglin is never
    hunted; a baby hoglin is noticed; a second baby piglin climbs on the first; three hoglins make a hurt piglin
    retreat.
*   Brutes: stats; attack a player in gold armor within 12 only; hit a pig; ignore gold.
*   Zombification after 300 ticks (keeping the sword, Nausea), brutes too; immune and nether piglins do not.
*   Saving: baby, immunity, CannotHunt, inventory, anger with its time left, hunted recently.
*   Advancements load; *Oh Shiny* is granted by its event; a biome event counts for *Hot Tourist Destinations*.

## Not tested

*   Anything visual: the model, poses, ears, held crossbow, armor fit, shaking; sounds playing; the crossbow copy's
    first-person animation.
*   Long-running AI in a real world: pathing around, repellent avoidance over time, celebrations and dancing, babies
    riding hoglins on their own, brutes walking home, door opening.
*   Natural spawning in the nether (only the spawn list entry is checked).
*   A game with Village & Pillage (the owner's crossbow by name; the copy not registered), or with Future MC.
*   *Oh Shiny* from a real thrown item (the thrower lookup by name), and from a real player's right-click.
*   The release jar in a real instance.

## Integrator

*   One shared-file line each: `NetherUpdateBackport.init` (`PiglinContent.init()`), `ClientSetup.init` (crossbow copy
    client and `PiglinClient`), `SelfTestRunner.TESTS`. `ModConfig.Piglins.spawn` added. `tools/owned/copy_from_owner.py`
    now copies the crossbow too; it and `tools/piglin/generate.py` write separate lang and sound blocks.
*   Stream Z merged a coremod after this branch was cut; this stream adds no patch.
*   `docs/PLAN.md`: mark P done; Ow's crossbow is done.
*   **Village & Pillage's maintainer**: for the Snout pattern to work at its loom, V&P needs a way to accept another
    mod's pattern items and keep their patterns out of the free list (for example a Backport Core pattern registry).
    Then this stream adds the `PIGLIN` constant (hash `pig`, 1.16's) and registers the item. Also, when V&P is added
    to a world that had this mod's crossbow copy, rename `netherupdatebackport:crossbow` and its three enchantments
    and projectiles to its own.
*   Stream V: register piglins in the crimson forest (weight 5, groups 3-4) and fire the four `visit_*` events (or
    switch them to location criteria). Stream X: `BastionMobs.spawn`, `setCannotBeHunted` for stable hoglins, and fire
    `enter_bastion` and the `loot_bastion_*` events.
