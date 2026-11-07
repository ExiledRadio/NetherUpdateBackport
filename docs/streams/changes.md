# Stream C: smaller changes

Package `changes` (client part in `changes/client`). Config `changes` (`enabled`, `hoes`, `lavaFog`). No blocks,
items, entities or packets.

## Changelog section

*   **Hoes are a real tool**: they break leaves, hay bales, nether and warped wart blocks, sponges, dried kelp
    blocks, targets and shroomlights faster (wood 2x, stone 4x, iron 6x, diamond 8x, gold 12x, netherite faster
    still, Efficiency on top). Like other tools, a hoe now wears 1 durability for each block it breaks that is
    not instant, and 2 for each hit.
*   **Fire Resistance lets you see farther in lava**: 5 blocks instead of 1. Lava's fog is the newer, sharper kind
    for everyone, and spectators see through it.
*   Fixed: blackstone and nether gold ore generation no longer makes the game generate extra chunks next to the
    ones being decorated ("cascading worldgen lag" in the log).

## How it works

*   **Hoes** (`HoeTool`, at init when `changes.hoes` is on, needs a restart). 1.16.5's `HoeItem extends ToolItem`
    with `DIGGABLES`; current Java's `#mineable/hoe` is the same list for the blocks 1.12.2 has.
    *   Harvest tool: `setHarvestLevel("hoe", 0)` on vanilla's leaves (both blocks), hay bale, nether wart block and
        sponge (dry and wet); our target, warped wart block and shroomlight already have it from U and F. Other
        mods' blocks by name, only when they name no tool already: `updateaquaticbackport:dried_kelp_block`
        (Update Aquatic owns it), `futuremc:warped_wart_block`, `futuremc:shroomlight`, and every `BlockLeaves`
        (today's `#leaves` is in `#mineable/hoe`, and mods put their leaves there). None of these needs a tool to
        drop, so drops do not change.
    *   Every `ItemHoe` (vanilla's, our netherite hoe, other mods') gets the `hoe` tool class at its material's
        harvest level, so other mods' right-tool checks see it. The material is read from `ItemHoe.toolMaterial`
        (both names).
    *   Speed: `PlayerEvent.BreakSpeed` multiplies the speed vanilla worked out by the material's efficiency plus
        Efficiency's `level² + 1`. 1.12.2 starts a hoe at 1, and the haste, fatigue, water and air factors are
        multipliers, so this gives exactly 1.16's `getDestroySpeed`. A hoe whose own `getDestroySpeed` is already
        above 1 there is left alone.
    *   Durability, 1.16's `ToolItem.mineBlock` and `hurtEnemy`: `BlockEvent.BreakEvent` (lowest priority, not
        cancelled, server) takes 1 for a block with hardness above 0; `LivingHurtEvent` takes the second point of
        a direct melee hit (vanilla's `ItemHoe.hitEntity` takes the first). A hoe class that declares its own
        `onBlockDestroyed` or `hitEntity` keeps its own rules.
*   **Lava fog** (`changes.client.ChangesClient`, numbers in `LavaVision`). 1.12.2's `EntityRenderer.setupFog` asks
    Forge's `FogDensity` event first; for a camera in lava this sets linear fog and cancels it, so vanilla's
    exponential fog (density 2) is skipped. Numbers from current Java's `LavaFogEnvironment` (26.3; 1.16.5 had 3
    blocks for Fire Resistance, current Java 5): spectator -8 to half the render distance, Fire Resistance 0 to 5,
    anyone else 0.25 to 1. As in 1.16, lava's fog wins over Blindness's. Read every frame, so `lavaFog` needs no
    restart.
*   **Advancements**: none. 1.16's eleven new nether-tab advancements all have a stream (U: crying obsidian,
    lodestone, respawn anchor; N: debris, netherite armor; R: both strider ones; P: *Oh Shiny*, *Hot Tourist
    Destinations*; X: *War Pigs*, *Those Were the Days*). Vanilla's own nether advancements are the same in 1.15.2
    and 1.16.5 (checked against both jars), so there is nothing to change either.
*   **Cascading worldgen** (stone's `BlackstoneGeneration`, blocks' `NetherGoldGeneration`): `WorldGenMinable` adds
    8 to the position itself, as vanilla's nether quartz call relies on, so the extra `+8` there put blobs 16-31
    blocks into the populated area plus their reach (up to about 7 blocks for a 33-block blob), past the 2x2
    chunks population guarantees. Ancient debris places its blocks itself, within 2 of a centre at +8 to +23, and
    was fine.

## Tested headlessly

`-Pselftest=all` on a fresh world: **pass=7227 fail=0** (counts vary a little with generation), **0 cascading
worldgen warnings** (52 on main's code with a fresh world); `-Pselftest=changes`: pass=92 fail=0;
`check_assets.py`: 0 errors.

*   Harvest tool `hoe` on every state of leaves, leaves2, hay, nether wart block and sponge, and on our target,
    warped wart block and shroomlight; stone keeps the pickaxe.
*   Dried kelp block: with Update Aquatic's dev jar (0.1.0) in `eclipse/mods`, found by name and given the hoe
    (log: "5 vanilla and 1 other blocks"); without it, nothing.
*   Every vanilla hoe is a `hoe` tool at levels 0/1/2/3/0, efficiencies 2/4/6/8/12; the netherite hoe too, faster
    than diamond.
*   Dig speed as a multiple of the bare hand: each hoe on hay = its efficiency; Efficiency III iron = 16; wooden on
    oak leaves 2, iron on wet sponge 6, stone on nether wart block 4, diamond on a target 8; a hoe on stone and a
    stick on hay 1. Leaves and hay still harvestable by hand.
*   Durability through the real break path (`tryHarvestBlock`): hay 1, stone 1, a flower 0, creative 0, a stick
    untouched; a real melee hit on a pig 2.
*   Lava fog numbers for Fire Resistance, none and spectator.

## Not tested

*   The fog on screen (client only; needs a game with lava and the effect). Both branches' numbers are tested.
*   Hoe mining speed felt in a client (the same event runs on both sides; the server's value is tested).
*   Other mods' hoes with their own rules, and other mods' leaves.

## Left as is (candidate patch)

1.16 hoes are `DIGGER` items, so they take Efficiency, Fortune and Silk Touch at an enchanting table and an anvil,
with their material's enchantability. 1.12.2's `EnumEnchantmentType.DIGGER` takes only `ItemTool` and `ItemHoe`
has no enchantability, so that needs a patch (`EnumEnchantmentType.DIGGER.canEnchantItem` and
`ItemHoe.getItemEnchantability`). For `docs/PATCHES.md`'s candidate table; not added there by this stream.

## For the integrator

*   `en_us.lang`: the `# --- config ---` block was regenerated with `tools/config_lang.py` (4 new lines for
    `changes.hoes` and `changes.lavaFog`); rerun it after merging other streams' config changes.
*   One line each in `NetherUpdateBackport.init`, `ClientSetup.init` and `SelfTestRunner.TESTS`.
