# Requests for Backport Core

Things this mod needs from Backport Core (`E:\MincraftModding\Workspaces\BackportCore`). This repo never edits
Core; each request says the API wanted and what this mod does until it exists.

## 1. Add this repo to `dependents` (small)

`build.gradle`: `def dependents = [..., 'NetherUpdateBackport']`, so `gradlew build copyToDependents` refreshes
`NetherUpdateBackport/deps/`. **Until then:** the dev jar is copied by hand from `BackportCore/build/libs`.

## 2. 1.16's vanilla retextures for the `modern` pack (small)

A diff of the 1.15.2 and 1.16.5 client jars, for textures 1.12.2 also has, finds 1.16 changed:
`block/nether_wart_block`, `block/purpur_pillar_top`, `block/observer_back_on`, the diamond tools and armor
(`item/diamond_{sword,pickaxe,axe,shovel,hoe,helmet,chestplate,leggings,boots}`), `item/golden_pickaxe`,
`item/golden_hoe`, `item/wooden_hoe`, `item/wooden_pickaxe`, `item/stone_hoe`, and `gui/container/beacon`
(the netherite ingot slot). Wanted: these in `manual_map.py` under their 1.12.2 names, so they apply whenever
`textures.modernTextures` is on. **Until then:** 1.12.2's look for those; nothing here depends on it.

## 3. Modern structure templates, jigsaw, and a nether terrain adapter (large; for stream X)

Shared with Update Aquatic's stream S (the template loader itself), plus two things only nether structures need:

```java
// 1.16+ structure NBT (palette, blocks, entities) to 1.12.2 states, with block-name remaps a mod supplies.
ModernTemplate t = ModernTemplates.load(new ResourceLocation("netherupdatebackport", "bastion/treasure/bases/lava_basin"));
t.place(world, origin, rotation, mirror, processors, random);

// Jigsaw assembly: start pool, max depth (1.16 bastions: 6), bounding-box test, jigsaw blocks read from the
// templates (name, target, pool, joint, final_state), pool JSONs from assets/<mod>/worldgen/template_pool/.
List<PlacedPiece> pieces = Jigsaw.assemble(world, startPool, depth, origin, random);

// Nether columns have several floors: the lowest air-over-solid y in [minY, maxY] at x,z, predicted
// before generation where the generator allows it, else read from generated chunks.
NetherHeights heights = NetherHeights.forWorld(world);
int floor = heights.floor(x, z, 32, 120);
```

Ruined portals also need `TerrainHeights` in the overworld (already in Core) and an "ocean floor" variant
(`heights.floor(x, z)` ignoring water). **Until then:** stream X is not started.

## 4. Waterlogging adapter (later pass)

Blocks of this mod that 1.16 makes waterloggable are listed in `PLAN.md` (*Waterlogging*). They implement
Core's `Waterloggable` once it exists. **Until then:** not waterloggable.
