# Stream L: soul fire

Package `soul` (and `soul/campfire`). Config `soul` (`enabled`, `soulFire`, `campfireEnabled`, `campfireCookTime`,
`campfireExtraFoods`, `campfireExcludedFoods`). Packet id 20 (soul campfire dousing).

## Changelog section

*   **Soul soil**: a soul block that does not slow you down. Dig it with a shovel.
*   **Soul fire**: fire lit on soul sand or soul soil burns blue, gives light 10, hurts more (2) and never spreads.
*   **Soul torch** (coal or charcoal, a stick and soul sand or soul soil), **soul lantern** (8 iron nuggets around a
    soul torch; stands on a block or hangs under one or from a chain) and **soul campfire** (logs, sticks and soul
    sand or soul soil): all give light 10.
*   The **soul campfire** cooks like a campfire, hurts for 2, and drops soul soil.
*   **Soul Speed** I-III (boots, treasure only): faster on soul sand and soul soil, and soul sand no longer slows you.

## How it works

*   **Soul campfire**: Village & Pillage's campfire classes, copied into `soul/campfire` as the family's pattern
    (same tile, four slots, cooking, signal smoke over hay, dousing, burning-arrow lighting), with 1.16's soul
    differences: light 10, 2 damage, one soul soil as its drop, no lava sparks. It cooks what a furnace turns from
    food into food plus the config lists; **with Village & Pillage installed it asks that mod's campfire recipes**
    instead, so both campfires cook the same things.
*   **Soul fire** is its own block (not a `BlockFire`, so it cannot spread): it needs a soul block under it, 2 damage
    and 8 seconds alight on contact. Vanilla fire that appears over soul sand or soul soil (flint and steel, fire
    charges, lava, spreading fire) is found through the neighbor-notify event and becomes soul fire at the end of the
    tick. Punching it puts it out (vanilla's `extinguishFire` only knows its own fire).
*   **Soul torch** is one block with 1.12.2's five torch facings, like 1.12.2's torch (1.16 splits it into a floor and
    a wall block).
*   **Soul Speed**: 1.16.5's `tryAddSoulSpeed`: on changing block while on a soul block, speed +0.03 x (1 + 0.35 x
    level) (transient), 4% chance of a durability point; souls and `particle.soul_escape` every 5 ticks while moving.
    Soul sand's 0.4 slowdown (applied by 1.12.2 inside its collision code) is divided back out at the start of the
    next tick, on both sides. Treasure only, so the enchanting table never offers it.
*   Particles: soul fire flame and soul, drawn from the block atlas with 1.18's textures (client classes).
*   `common/shape/Shapes.Pillar` now defaults to the Y axis; found here: 1.12.2's `BlockRotatedPillar` defaults to X,
    which laid chains and huge fungus stems on their side.

## Tested headlessly

`-Pselftest=all` (every stream): **pass=6779 fail=0** (soul alone: pass=146); `check_assets.py`: **81 blocks,
881 states, 72 item models, 459 sound files, 0 errors**.

*   Registration, items (none for soul fire), metadata, soul soil 0.5 with a shovel, light 10 for fire, torch and lantern.
*   Fire set on soul sand becomes soul fire; flint and steel on soul soil lights soul fire; fire on netherrack stays.
*   Soul fire hurts a pig for exactly 2 and sets it alight; punching it puts it out; it goes when its soul block does.
*   Soul lantern: hangs under a block, stands on the floor, falls with its ceiling, hangs from a chain.
*   Soul torch on a wall. Soul campfire: light 10, hurts for 2, drops one soul soil, cooks raw beef into one steak.
*   Soul Speed: treasure, max III, boots only; a zombie with III boots on soul sand gets exactly +0.0615 speed, its
    motion slowed by soul sand is restored, and the boost goes off soul blocks.
*   Recipes all registered; soul torches (4), soul lantern, soul campfire from birch logs and soul soil.

## Not tested

*   Visuals: blue fire, torch and lantern models, particles, campfire rendering and smoke.
*   Sounds playing. A real player's movement on soul sand (the client side of the slowdown fix).
*   The soul campfire reading Village & Pillage's recipes (needs that mod in the dev game).
*   Soul Speed can still come from 1.12.2's random enchanted books (vanilla's loot function and the vanilla
    librarian trade pick from every enchantment; 1.16 leaves out undiscoverable ones). Not patched.

## Waterlogging (later pass)

The soul campfire is waterloggable since the waterlogging stream (`docs/streams/waterlog.md`): placed in water it is
unlit, water puts a lit one out, a waterlogged one cannot be lit, and the water bucket now waterlogs it instead of
only putting it out. The soul lantern holds water too.
