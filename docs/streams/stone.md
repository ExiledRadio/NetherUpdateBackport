# Stream S: blackstone and basalt

Package `stone`. Config `stone` (`enabled`, `generate`, `basaltGenerator`). Shared shapes added to `common/shape`
(slabs, stairs, walls, buttons, pressure plates, pillars) for stream F to reuse; `common/Sounds`,
`common/Smelting`, `common/StreamCondition` and `tools/building.py` added for every stream.

## Changelog section

*   **Blackstone**, with blackstone stairs, slabs and walls. Works like cobblestone for stone tools, furnaces and
    brewing stands, mixed with cobblestone or on its own.
*   **Polished blackstone** (4 blackstone), with stairs, slabs, walls, a pressure plate (players and mobs) and a
    button. **Polished blackstone bricks** (4 polished blackstone) with stairs, slabs and walls; smelt them into
    **cracked polished blackstone bricks**. **Chiseled polished blackstone** from two polished blackstone slabs.
*   **Gilded blackstone**: drops 2-5 gold nuggets one time in ten (more often with Fortune, always at Fortune III),
    otherwise itself; Silk Touch keeps it.
*   **Basalt** and **polished basalt** (4 basalt), placed along an axis like logs, with their own 1.16 sounds.
*   **Basalt generator**: lava over soul soil, touching blue ice, turns into basalt.
*   Blackstone generates in blobs deep in the nether, in newly explored chunks.

## How it works

*   **Numbers** from 1.16.5's `Blocks`: hardness and blast resistance blackstone 1.5/6, polished blackstone 2/6,
    bricks, cracked and chiseled 1.5/6, blackstone and brick slabs 2/6, basalt 1.25/4.2, plate and button 0.5.
    `Shapes.strength` converts 1.16's blast resistance to 1.12.2's storage (x5/3) so explosions read the same.
*   **Slabs** are the family's two-block form (Village & Pillage's): `<x>_slab` and `<x>_double_slab` (no item).
    Walls extend vanilla's wall so they join cobblestone walls and every wall built the same way.
*   **Assets** are ported by `tools/stone/generate.py`: 1.18's blockstates where 1.12.2 has the same properties
    (stairs, pillars), 1.12.2's own button and plate blockstates with 1.18's models, 1.12.2's wall multipart with
    1.18's post and side models, 1.18's crafting recipes rewritten for 1.12.2 with a condition naming the items
    they need (`netherupdatebackport:items`), 1.18's smelting recipe into `smelting.json`.
*   **Stone tools from blackstone** are added recipes (`blackstone_stone_pickaxe` and so on) whose ingredient is
    "cobblestone or blackstone"; vanilla's recipes are untouched, so JEI shows both. Blackstone is not added to
    the `cobblestone` ore name, since that would make it work in every mod's cobblestone recipe, which 1.16 does not.
*   **Basalt generator**: 1.16's rule (`FlowingFluidBlock.shouldSpreadLiquid`): lava, source or flowing, over soul
    soil with blue ice above it or on any side. Checked at Forge's neighbor-notify event for the changed block and
    its six neighbors, converted at the end of the world tick. Soul soil is stream L's (by name); blue ice is
    Village & Pillage's, Update Aquatic's copy, or another mod's, in that order. Without both it does nothing.
*   **Generation**: 1.16's `ore_blackstone` (2 blobs of 33 per chunk, y 5-31, replacing netherrack) through an
    `IWorldGenerator` in the nether only. Decoration: the terrain itself does not change.
*   Every block is declared to Backport Core's ownership table as this mod's (Rule 1).

## Tested headlessly

`./gradlew runServer -Pselftest=stone`, then `python tools/check_assets.py`: **`SELFTEST DONE pass=5421 fail=0`**,
**22 blocks, 347 states, 19 item models, 51 sound files, 0 errors**.

*   All 22 blocks registered; 19 items, no item for the three double slabs; every state's metadata round-trips.
*   Hardness and blast resistance of ten blocks against 1.16.5; pickaxe as the tool; basalt's own sound type with
    a registered event.
*   A real player's right-click: basalt on a block's east face lies along X; a slab goes on the bottom half and a
    second makes the double slab, which drops two slabs.
*   A blackstone wall and a cobblestone wall join each other.
*   Gilded blackstone in 4000 drops: nuggets 8-12% of the time, always 2-5; Fortune III always nuggets; Silk
    Touch allowed.
*   All 22 recipe files registered; furnace from 8 blackstone; stone pickaxe from mixed blackstone and
    cobblestone; polished blackstone, polished basalt, stairs (4), slabs (6) and button; bricks smelt to cracked.
*   Basalt generator with soul sand and packed ice standing in for soul soil and blue ice (neither is in this
    game): lava beside the ice turns to basalt, ice above counts, lava without ice or not over the soil stays lava.
*   Freshly generated nether chunks (4 populated chunks around chunk 1500,1500) contain blackstone, all within
    the expected heights.

## Not tested

*   Anything visual (no client): models, textures, item icons, slab and wall shapes as drawn.
*   Sounds playing.
*   The basalt generator with the real soul soil and blue ice (needs stream L and Village & Pillage or Update
    Aquatic in the same game).
*   Village & Pillage's stonecutter showing the blackstone recipes (needs that mod).
