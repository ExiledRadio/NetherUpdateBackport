# Stream Wl: waterlogging

The blocks 1.16 made waterloggable hold water through Backport Core's `waterlog/Waterloggable` adapter over
Fluidlogged API 3.3.3, with 1.16.5's rules. No config, no packets, no entity ids, no ASM. Files: `common/WaterRules`
(new), `common/shape/Shapes` (stairs, walls, fences, trapdoors), `common/shape/BlockNetherSlab`, `fungus/NetherSigns`,
`blocks/BlockChain`, `soul/BlockSoulLantern`, `soul/campfire/BlockSoulCampfire`.

## Changelog section

*   **Waterlogging**: chain, soul lantern, soul campfire, every blackstone, polished blackstone, polished blackstone
    brick, crimson and warped slab, stair and wall, and crimson and warped fences, trapdoors and signs hold water as
    in 1.16: placed into a water source, or with a water bucket. An empty bucket takes the water back. A double slab
    holds none.
*   As in 1.16, flowing water never runs into them, and nothing else of this mod's holds water (fence gates, doors,
    buttons, pressure plates, the soul torch, plants).
*   A soul campfire placed in water is unlit; water poured on a lit one puts it out with a hiss; a waterlogged one
    cannot be lit.

## How it works (1.16.5's code, `MC1165Reference`)

*   **Which blocks**: `Waterloggable` on the classes above. The double slab answers `canHoldWater` false
    (`SlabBlock`: `type != DOUBLE`). Every state of the others holds water (1.16.5 has `waterlogged` on every
    state of them).
*   **`common/WaterRules`**: Fluidlogged API's own list (its `whitelist.jsonc`) waterlogs every block that extends a
    vanilla stair, slab, wall, fence, trapdoor, sign, fence gate, door, button or pressure plate, with any fluid,
    flowing fluid too, and that list is asked before a block's own answers. A `FluidloggableEvent` handler at
    `HIGHEST`, for this mod's blocks only, puts 1.16.5's `IWaterLoggable` rule back: only a water source
    (`canPlaceLiquid`: `fluid == Fluids.WATER`, never `FLOWING_WATER`), and only a `Waterloggable` block. So flowing
    water stops at a fence or a slab instead of filling it, a block placed into flowing water replaces it, and our
    fence gates, doors, buttons and pressure plates (not waterloggable in 1.16) refuse water.
*   **Placement and the bucket** are Fluidlogged API's: setting one of these blocks into a water source keeps the
    source; its bucket hooks fill and drain them; a bucket on a block that refuses (a double slab, a waterlogged one)
    pours beside it, as vanilla's `BucketItem` does. A slab placed onto a waterlogged slab becomes a double slab and
    the water is gone (1.16.5's `SlabBlock.getStateForPlacement`: `waterlogged=false`).
*   **Soul campfire** (1.16.5's `CampfireBlock`):
    *   placed into a water source: unlit (`LIT = !waterlogged`);
    *   water arriving (`onWaterChanged`, so the bucket, a command, anything): a lit one plays
        `entity.generic.extinguish_fire` at volume 1, pitch 1, sends the smoke burst and goes out, keeping what is
        cooking (`placeLiquid`, `dowse`);
    *   the water bucket is no longer handled in `onBlockActivated` (it used to put the fire out and keep no water):
        the bucket item waterlogs the campfire;
    *   `canLight`: flint and steel and fire charges do nothing to a waterlogged campfire (then the item acts as on
        any block), and burning arrows and fireballs do not light it (`onProjectileHit`).
*   **Fire**: soul fire is not waterloggable; water flowing into it washes it away, as vanilla fire.

## Tested (headless, Fluidlogged API 3.3.3 in dev)

`-Pselftest=waterlog`: 88 checks with the scaffold, 0 failed:

*   every state of every block of ours (owned copies included) against 1.16.5: which hold a water source; none
    takes flowing water or lava (under Fluidlogged API's own rules vanilla oak stairs take flowing water, logged);
*   ten blocks placed into a pool keep its water; a double slab and a fence gate replace it; four blocks placed
    into flowing water replace it; a soul campfire placed into a source is unlit, on land lit;
*   a spring beside a dry fence, a dry bottom slab and a dry soul lantern: the water spreads, but none of them takes
    any; a waterlogged slab keeps its source and its water flows out beside it;
*   a player's water bucket waterlogs dry stairs (nothing above), an empty bucket takes it back; on a double slab
    the water goes on top;
*   a lit soul campfire with beef: the bucket waterlogs it and puts it out, the beef stays; flint and steel, a fire
    charge and a burning arrow do not light it; drained it stays out, and then flint and steel lights it; an unlit
    one takes water; water set by a command puts a lit one out;
*   a slab placed onto a waterlogged slab: a double slab with no water, none above;
*   water flowing into soul fire puts it out; a lit soul campfire beside water stays lit and dry.

## Not tested

*   Anything drawn: the water inside the blocks, the campfire's smoke burst. No client was started.
*   1.16.5 forms a water source in a waterloggable block between two sources; with Fluidlogged API's
    `nonSourceFluidlogging` on (its default) the flowing state is refused first, so no source forms in our blocks
    there. With it off, Fluidlogged API's own source forming applies.
*   Other mods' `FluidloggableEvent` handlers, and a player's own Fluidlogged API whitelist for our blocks: our
    handler runs first and wins for this mod's blocks.
*   The owned copies of Village & Pillage's lantern and smithing table (registered when that mod is absent) are left
    as their owner has them: not waterloggable. 1.16 made the lantern waterloggable; that change is to the owner's
    block.
