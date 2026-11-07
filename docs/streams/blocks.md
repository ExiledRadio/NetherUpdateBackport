# Stream U: smaller blocks

Package `blocks`. Config `blocks` (`enabled`, `generateGold`).

## Changelog section

*   **Chain** (2 iron nuggets + 1 ingot): thin, placed along an axis; lanterns hang from it.
*   **Crying obsidian**: as strong as obsidian, glows (light 10) and drips purple tears.
*   **Target**: projectiles that hit it give a redstone signal, strongest at the centre (arrows 1 second, others 0.4 s).
    *Bullseye* advancement.
*   **Respawn anchor** (6 crying obsidian + 3 glowstone): charge with glowstone (up to 4, also from a dispenser);
    in the nether it sets your respawn point and each respawn uses a charge; anywhere else it explodes.
    *Not Quite "Nine" Lives*.
*   **Lodestone** (8 chiseled stone bricks + a netherite ingot) and the **lodestone compass**: use a compass on a
    lodestone and it points there from anywhere in that dimension. *Country Lode, Take Me Home*.
*   **Nether gold ore**: 2-6 gold nuggets (more with Fortune), smelts to a gold ingot; generates in the nether.
*   **Quartz bricks**, **cracked nether bricks** (smelt nether bricks), **chiseled nether bricks**.
*   *Who is Cutting Onions?* for getting crying obsidian.

## How it works

*   Numbers from 1.16.5 (`Blocks`, `TargetBlock`, `RespawnAnchorBlock`, `CompassItem`, `Features`).
*   Target hits come from Forge's `ProjectileImpactEvent`; power is 1.16's formula, ignored while counting down.
*   Respawn anchor: the anchor is stored in the player's persisted NBT beside vanilla's bed spawn (1.16 has one
    respawn point: a later bed or `/spawnpoint` forgets it; vanilla's own setSpawnPoint during a respawn does
    not). After a death the player respawns where vanilla says, then is sent to a free spot beside the anchor
    (1.16's offsets), using a charge; a gone, empty or walled-in anchor is forgotten with 1.16's message.
    The explosion is power 5 with fire, and breaks no blocks with water beside or above it (what 1.16's
    water-resistance trick works out to).
*   Lodestone compass is its own item (1.12.2's compass cannot rename itself or move its needle by NBT) with
    1.16's NBT layout; the needle is a client property getter; frames from vanilla's `compass.json`.
*   Generation: 1.16's `ore_gold_nether` (10 veins of 10, y 10-117) as an `IWorldGenerator`, nether only.
*   Advancements in vanilla's Nether and Adventure tabs, using `common/Triggers` events.

## Tested headlessly

`-Pselftest=blocks`: **pass=545 fail=0**; with stream S (`-Pselftest=all`): **pass=5744 fail=0**;
`check_assets.py`: **31 blocks, 377 states, 29 item models, 171 sound files, 0 errors**.

*   Registration, items, metadata round trips, hardness, crying obsidian's tool level and light.
*   Chain placed on a side runs along that axis, 1.16's box, small-centre ends.
*   Target: formula values (15 centre, 1 edge, 8 half way); a real arrow powers it (>=12) and its neighbor;
    reset scheduled; a second hit ignored; reset turns it off.
*   Respawn anchor: 4 glowstone charge it (one used each); light and comparator 0/3/7/11/15; explodes in the
    overworld; a glowstone dispenser charges it; in the nether it does not explode and records the spawn;
    using it stands the player beside it and spends a charge; walled in, it refuses and keeps its charge;
    setting a bed spawn forgets it.
*   Compass on a lodestone: in place for one, split from a stack; forgets a removed lodestone.
*   Nether gold ore drops 2-6, more with Fortune III; smelting; nether bricks to cracked.
*   Recipes all registered (lodestone's skipped: no netherite yet); chain, quartz bricks (4), chiseled nether
    bricks, respawn anchor, target.
*   Advancements loaded; nether gold ore in freshly generated nether chunks.

## Not tested

*   Visuals: models, tears, compass needle, anchor particles; sounds playing.
*   A real death and respawn through vanilla's `PlayerList` (the pieces are tested; the full flow needs a client).
*   *Country Lode, Take Me Home* loads only once stream N adds *Hidden in the Depths* (its parent): until then it
    logs "Couldn't load advancement".
