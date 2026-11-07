# Stream F: crimson and warped

Package `fungus`. Config `fungus` (`enabled`). Adds door, trapdoor, fence and fence gate to `common/shape`.

## Changelog section

*   **Crimson and warped nylium**: netherrack with a fungal top. Drops netherrack unless mined with Silk Touch;
    turns back to netherrack under a block. Bone meal on it grows roots, fungi and sprouts around it (and
    sometimes twisting vines); bone meal on netherrack next to nylium spreads the nylium.
*   **Crimson and warped fungi**: bone meal grows them into **huge fungi** on their own nylium.
*   **Crimson and warped roots**, **nether sprouts** (shears only), **warped wart block**, **shroomlight** (light 15).
*   **Weeping vines** hang and grow down, **twisting vines** grow up; both are climbable, grow with bone meal, and
    stop growing when you use shears on the tip.
*   **Crimson and warped woods**: stems, hyphae, stripped forms (use an axe), planks, slabs, stairs, fences, fence
    gates, doors, trapdoors, buttons, pressure plates and signs. Nether wood never burns and is not fuel. The
    planks make sticks, crafting tables, chests, wooden tools and everything else made from any planks.
*   With a composter installed, the plants, vines, wart blocks and shroomlight compost.

## How it works

*   Numbers and algorithms from 1.16.5: `Blocks`, `NyliumBlock`, `FungusBlock`, `NetherRootsBlock`,
    `NetherSproutsBlock`, the plant-block classes for vines, `HugeFungusFeature`, `NetherVegetationFeature`,
    `TwistingVineFeature`, `WeepingVineFeature`, `NetherrackBlock`'s bone meal. Vine ages 0-25 fold into 16
    values (a new tip takes the next age 15 times in 25), as Update Aquatic's kelp.
*   The features in `HugeFungus` and `NetherVegetation` take a "planted" flag; stream V's forests will call them
    for generation.
*   Woods use `Material.WOOD`, so axes, wooden fences (they join oak fences) and other wood checks treat them as
    wood; no fire info is registered and `FurnaceFuelBurnTimeEvent` makes them burn 0 ticks.
*   Signs follow Village & Pillage's: vanilla sign blocks and item with our blocks, a `TileEntitySign` subclass
    and a copy of vanilla's renderer that tells standing from wall signs by `instanceof`.
*   Ore names `plankWood`, `slabWood`, `stairWood`, `fenceWood`, `fenceGateWood`. Forge turns vanilla's "any planks"
    recipes into `plankWood` recipes, so crimson planks would make oak signs, trapdoors and buttons;
    `OakRecipes` narrows those ten vanilla recipes in place to leave nether planks out (Village & Pillage does the
    same for the sign).
*   Recipes: 1.18's for every item here, plus 16 "nether_wood_" copies of vanilla wooden recipes (sticks, crafting
    table, chest, tools, bowl, bookshelf, note block, jukebox, piston, shield, tripwire hook, daylight detector)
    that take nether and vanilla planks together. Potted fungi and roots are not done (1.12.2's flower pot
    accepts only vanilla plants).

## Tested headlessly

`-Pselftest=fungus`: **pass=700 fail=0**; all streams (`-Pselftest=all`): **pass=6446 fail=0**;
`check_assets.py`: **76 blocks, 856 states, 68 item models, 383 sound files, 0 errors**.

*   Registration of all 45 blocks; which have items; metadata round trips; hardness; shroomlight light 15.
*   Fungus on nylium and not on stone; nylium under stone becomes netherrack; nylium drops netherrack.
*   Bone meal (through vanilla's `ItemDye.applyBonemeal`): plants grow around crimson nylium; netherrack next to
    warped nylium becomes warped nylium; lone netherrack takes none.
*   A planted huge crimson fungus: stem from the fungus's spot, 4-26 tall, hat of nether wart blocks; refuses the
    wrong nylium; bone meal success near 40% in 1000 tries.
*   Weeping vines: need a ceiling; random ticks grow them down with a tip below body blocks; climbable; the whole
    vine falls without its ceiling. Twisting vines grow with bone meal; shears on the tip set it fully grown and
    it stops.
*   An iron axe strips a warped stem through the real right-click event, keeping the axis and wearing the axe.
*   Planks and slabs are not fuel, oak planks still are; flammability 0; planks are `plankWood`.
*   A crimson fence joins oak and warped fences; a crimson door places both halves and drops itself; a warped
    sign stands on a block (with our tile, a vanilla sign tile) and hangs on a side.
*   Recipes: all registered; planks from stripped hyphae; sticks and a crafting table from mixed planks; doors,
    signs, trapdoors, buttons, plates and slabs give the nether versions; oak planks still make oak signs.

## Not tested

*   Anything visual: models, cutout rendering, sign text drawing, door and trapdoor shapes as drawn.
*   Sounds playing. Composting (needs Village & Pillage).
*   Huge fungi generated in the world (stream V).
