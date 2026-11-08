# Stream Fm: pairs with the other mod that adds 1.16 blocks

Package `common` (`FutureMcPairs`, the integrator's), generator `tools/futuremc/states.py`, table
`migration/futuremc_states.json`. Config `compat` (`duplicates`, `convertRemoved`). Quiet compatibility (Rule 3): no
user-facing text names the other mod.

## Changelog section

*   Worlds that used another mod's 1.16 blocks and items (chain, soul lantern, soul torch, soul soil, ancient debris,
    netherite block, netherite gear, the Pigstep disc) keep them when that mod is removed: they turn into this mod's.
*   With both installed, the duplicates are hidden (`compat.duplicates`: OURS, the default, hides theirs; THEIRS hides
    ours; BOTH hides nothing), and only the visible ancient debris generates.

## What the other mod registers

Read from its 0.2.21 jar (`G:\MCDownloads\futuremc-0.2.21`, Vineflower + javap) and from the registry of a world it was
installed in (the user's Dregora instance, `Tip Touchers/level.dat`): blocks `ancient_debris`, `chain`,
`netherite_block`, `soul_soil`, `soul_fire_lantern`, `soul_fire_torch`; items the netherite ingot, scrap, five tools
and four armor pieces, and `record_pigstep`. Its blackstone set and nether gold ore register only when the game is
deobfuscated (its `DEBUG` flag), so no player's world has them. It also adds Village & Pillage's `lantern`,
`smithing_table`, `stone_brick_wall` and `mossy_stone_brick_wall`, which this mod copies when Village & Pillage is
absent (`owned`): those copies pair with its blocks too.

## How it works

*   Backport Core's `FutureMcTable` contributor: same-name counterparts, plus `soul_fire_lantern` -> `soul_lantern`
    and `soul_fire_torch` -> `soul_torch` (both are 1.12.2 torches with the same five facings and metadata),
    `record_pigstep` -> `music_disc_pigstep` (an item rule), and its smithing table's `facing` ignored (Village &
    Pillage's, which ours copies, has one state, as Village & Pillage itself declares).
*   Metadata: its lantern keeps `hanging` in meta 1, its torch is 1.12.2's `BlockTorch` (east 1, west 2, south 3,
    north 4, up 5); the owned blocks' entries are Village & Pillage's recorded ones. A `/futuremc dump` on a real
    server with the other mod installed wrote exactly the same table.
*   `DuplicateBlocks`: the 10 block pairs above and the 12 item pairs. Two of its items (`netherite_ingot`,
    `netherite_scrap`, its `ModeledItem` class) refuse `setCreativeTab(null)` with an exception, which would stop the
    game in Backport Core's `hideFromCreative`; those are hidden here instead (tab cleared through the field, their
    recipes and smelting removed after Core's pass and again from post-init). Core request 6.
*   Ancient debris: with OURS, its debris world generator (`AncientDebrisWorldGen`, found by class name) is taken out
    of Forge's list in post-init; with THEIRS, ours stops (`AncientDebrisGeneration` asks `DuplicateBlocks.isHidden`).
    Either way the hidden debris smelts into the visible scrap, so debris already in a world stays useful.

## Tested

*   Headless, `-Pselftest=futuremc` (dev, without the other mod; Fluidlogged API loaded): **127 checks** (249 with the
    scaffold), 0 failed. Every recorded state of all 10 blocks converts with nothing dropped; each block and 13 items
    convert to ours (a worn sword keeps its damage); every pair's half of ours exists; nothing of ours is hidden
    without the other mod; the generator removal on a stand-in generator.
*   Real server (`G:\NetherTest\fmserver`: Forge 2860, Backport Core 1.1.0, Fluidlogged API 3.3.3, the other mod
    0.2.21 with its Kotlin library, this mod's release jar, no Village & Pillage):
    *   boots; Core hid 10 blocks and removed 8 of their recipes; this mod hid the two items above and removed their
        2 recipes and 1 smelting recipe; their debris generator removed;
    *   `/futuremc check`: **10 pairs, 19 states, 0 mismatches**; `/futuremc dump` identical to our table;
    *   10 of their blocks placed (both torch facings, the hanging lantern, the smithing table facing south) and their
        sword (damage 37), disc and torches in a chest; the other mod taken out; on the next start all ten are ours
        with the same states and the chest holds our sword (damage 37), disc and torches. Then a clean reload.

## Not tested

*   A client: the creative tabs and JEI with both mods installed.
*   `compat.duplicates` THEIRS and BOTH on a real server (THEIRS is the same code with the halves swapped).
*   With Village & Pillage installed as well (then its contributor owns the lantern, smithing table and walls).
*   The other mod logs `Unknown recipe 'minecraft:trapdoor'` for a vanilla advancement on its own; not ours.
