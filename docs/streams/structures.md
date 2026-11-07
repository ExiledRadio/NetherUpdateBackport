# Stream X: structures

Package `structure`; owner copies in `owned`. Config `structures` (`enabled`, `bastions`, `ruinedPortals`,
`netherFossils`). No entity or packet ids used. Backport Core **1.2.0**. *Decided* item 2: bastions take nether
fortress slots.

## Changelog section

*   **Bastion remnants** in the nether: all four kinds (housing units, bridge, hoglin stables, treasure room), built
    from Mojang's own 1.16 files, with their chests (netherite, ancient debris, Soul Speed books, Pigstep, the Snout
    banner pattern, gold), magma cube spawners, hoglins in the stables, and piglins and piglin brutes on guard.
*   Bastions and **nether fortresses share their spots**, as in 1.16: each spot of the grid holds one or the other, so
    fortresses now generate where 1.16 puts them. Fortresses you have already found stay.
*   **Ruined portals** in the overworld and the nether, in every 1.16 variant (buried in deserts, overgrown in jungles,
    sunk in swamps and oceans, in and on mountains, blackstone in the nether), with crying obsidian, netherrack and
    magma spread around them and a chest (today's loot, lodestones included).
*   **Nether fossils** in soul sand valleys.
*   For a given seed, bastions, fortresses, ruined portals and fossils start in exactly the chunks vanilla 1.16 puts
    them, so seed maps for 1.16 show them right.
*   *Those Were the Days* and *War Pigs* can be earned.
*   Without Village & Pillage, this mod adds the lantern, smooth quartz slab, stone brick wall and mossy stone brick
    stairs, slab and wall that bastions and ruined portals are built with.

## How it works

*   **Bastions** (`BastionStructure`, a Core `ScatteredStructure`): `VanillaPlacements.V1_16_5.BASTION_REMNANT`, a
    candidate starts one when `NetherStructureSlots.startAt` says `BASTION`. The start is Core's
    `Jigsaw.of(minecraft:bastion/starts).maxDepth(6)` at `(cx*16, 33, cz*16)` with `StructureRandoms.start`, its pieces
    `BastionPiece`s (saved as `NUB_BastionPiece`, starts as `NUB_Bastion`).
*   **Shared slots**: pre-init `NetherStructureSlots.install(StructureContent::allows)`. The rule reads stream V's
    `NetherBiomeSource.noiseKind` at the chunk's middle quart `((x << 2) + 2, 0, (z << 2) + 2)`: 1.16.5's
    `createStructures` reads the noise biome, while the column biomes 1.12.2 stores are the fuzzed ones, so Core's
    default rule (`getBiome` at the block) would disagree near biome edges. Bastions everywhere but the basalt deltas;
    other nethers fall back to Core's rule. `structures.bastions` off: no install, 1.12.2's fortresses stay.
*   **Files** (`tools/structure/generate.py`): the 1.16.5 client jar's `bastion/`, `ruined_portal/` and
    `nether_fossils/` templates (194) under `assets/netherupdatebackport/structures/`; the 60 bastion pools and the 12
    processor lists (dumped with Mojang's codecs by Core's `tools/vanilla-oracle`, copied from Core's self-test assets)
    under `worldgen/`; `JigsawPools.source` and `ModernPalette.source` name this mod.
*   **Blocks** (`StructurePalette`): this mod's blocks are named as 1.16 names them, so the source serves the blackstone
    and basalt families (S) and crying obsidian (U). Chain is mapped as `minecraft:iron_chain` (U's). Slabs need a
    mapping (`type` picks the half or the double block): blackstone, polished blackstone, polished blackstone brick
    (S), smooth quartz and mossy stone brick (Village & Pillage's, else the copies), and 1.12.2's stone brick and stone
    slabs, defaulting to the bottom half (Core resolves a property-less `minecraft:stone_slab`, which `block_age`
    writes, as the double slab). Lanterns, stone brick walls and the mossy stone brick stairs and wall come from
    Village & Pillage (a Core source) or, without it, from `owned`.
*   **Owner copies** (`owned`, Rule 1, registered only without Village & Pillage): `lantern` (its class copied),
    `smooth_quartz_slab`/`smooth_quartz_double_slab`, `stone_brick_wall`, `mossy_stone_brick_stairs`,
    `mossy_stone_brick_slab`/`_double_slab`, `mossy_stone_brick_wall`: Village & Pillage's paths, states, metadata,
    strength and assets (`tools/owned/copy_from_owner.py`; the walls' blockstates gain the all-off post stream S's
    walls have). Recipes: lantern (the owner's), the walls, stairs and slab of 1.14 from stone bricks. The smooth quartz
    slab has none (1.12.2 has no smooth quartz item).
*   **Mobs** (`BastionPiece`): the four piglin mob pieces place their blocks without the template's entity and call
    stream P's `piglin.BastionMobs.spawn(world, pos, yaw, template)` by name (reflection on this mod's own class, so X
    never links against P; null-safe without it). That is 1.16's `SpawnReason.STRUCTURE`: no baby or random weapon,
    the template's crossbow (1.12.2 cannot read `minecraft:crossbow` from the file) or golden sword, brutes for both
    `melee_piglin` templates, armor and enchantment rolls kept. Hoglins go through Core: `ModernPalette.entity`
    (registered at init only for registered entities: hoglin, piglin, piglin brute), the template's `CannotBeHunted` and
    `PersistenceRequired` read, 1.16's one-in-five baby.
*   **Jigsaw-only pieces** (`BastionPiece.placeLoneJigsaws`): `bastion/blocks/gold`, `blocks/air` and
    `treasure/extensions/empty` are a lone jigsaw whose final state is the block. Core 1.2.0's `ModernTemplate.place`
    returns before placing jigsaws when a template has no other block, so these are placed here (see *Core*).
*   **Ruined portals** (`RuinedPortals`, `RuinedPortalPiece`, `PortalExtras`): one kind, a per-world instance.
    Overworld `VanillaPlacements.V1_16_5.RUINED_PORTAL` (40, 15, 34222645); **nether (25, 10, 34222645)**, as 1.16.5's
    `DimensionSettings.nether` overrides it. Type by the noise biome at the middle quart (`getBiomesForGeneration`, 1.12.2's
    1:4 layer, which is 1.16's noise biome) through 1.16.5's table by numeric id, Biome Dictionary types for other mods'
    biomes; every nether biome is `nether`. The start is `RuinedPortalStructure.Start.generatePieces` line for line:
    placement and settings per type, giant 5%, template, rotation, mirror, the base height at the box's middle
    (`NetherHeights.baseForWorld`: `height` for `WORLD_SURFACE_WG`, `floor` for `OCEAN_FLOOR_WG`), `findSuitableY` with
    its 3-of-4-corners test on the base columns, `cold` from the biome's temperature there. The template places with
    1.16's processors in 1.16's order (rules built as 1.16 JSON and read by Core, `blockAge`, `lavaSubmerged`,
    `blackstoneReplace`). Then `PortalExtras` runs 1.16's `spreadNetherrack`, `addNetherrackDripColumnsBelowPortal`,
    vines and leaves with 1.16's random (the start chunk's `setDecorationSeed`, feature index 5 of `SURFACE_STRUCTURES`,
    after the chest's `LootTableSeed` draw, which the chest then gets) against the terrain as it will generate
    (`NetherHeights.forWorld`) under the portal's own processed blocks; each chunk writes its share. The piece's box is
    widened to where the extras reach (14 out, 12 down), since 1.12.2 asks a piece only for chunks its box covers.
*   **Nether fossils** (`NetherFossils`): `VanillaPlacements.V1_16_5.NETHER_FOSSIL`, soul sand valleys by the noise
    biome; 1.16.5's start: a random column, a random height from 32 to 125, down to the first air over solid of the
    base column, then rotation and one of the 14 templates; placed about its corner, air left out.
*   **Loot** (`StructureLoot`): 1.16.5's `bastion_bridge`, `bastion_hoglin_stable`, `bastion_other`, `bastion_treasure`
    and `ruined_portal` in 1.12.2's format under `loot_tables/chests/`, mapped with `ModernPalette.lootTable`. Current
    Java changed no 1.16 weight; it added smithing templates and spears (not this mod's) and a lodestone pool to ruined
    portals (kept: weight 2 for 1-2 lodestones against 1 empty). Items of other streams and mods (netherite, debris,
    crying obsidian, gilded blackstone, chains, crimson items, lodestones, the Snout pattern, Pigstep, the crossbow:
    Village & Pillage's, this mod's copy, then the mods it steps aside for) are named by condition `item_exists` and
    function `set_item`, so a missing one drops out of its pool; Soul Speed through `enchant_randomly` by name.
*   **Advancements** (`StructureEvents`, P's JSON): `enter_bastion` once a second for a player inside a bastion piece
    (1.16's location test asks the pieces, not the outer box); `loot_bastion_<table>` from a pool condition
    (`advancement_event`, always false, no random drawn) when a player opens a bastion chest, as
    `player_generates_container_loot`.
*   Everything is driven from Core's populate-step driver: no chunk generator is touched; chunks generated before keep
    what they had.

## Seed parity: checked against vanilla 1.16.5 itself

`tools/structure/oracle/StructureOracle.java` runs Mojang's 1.16.5 server classes (the remapped server jar of Core's
`tools/vanilla-oracle`) for seeds 0, 12345, -7346534567 and 8675309: the nether's multi-noise biome source and noise
generator, the overworld's layered biome source and generator, `createStructures`' biome check,
`ConfiguredStructureFeature.generate`, and the pieces' own template placement into an empty world. Its answers ship
with the self-test (`src/selftest/resources/.../oracle/`, compiled into test builds only).

*   Bastion / fortress / empty (basalt deltas) slots: **6400 regions, 0 differences** (3299 bastions, 2485 fortresses,
    616 empty); the noise biome at every slot is 1.16.5's; in a seed-12345 world Core's `startAt` with this stream's rule
    agrees in all 1600.
*   Ruined portals: every candidate chunk (5696 regions, both dimensions); **5696 starts, 0 differences** in type,
    placement, template, rotation, mirror, air pocket, mossiness, overgrowth, vines, blackstone, column and box; the
    height before the corner test is the same in all 4096 nether starts; the variant table for all 61 vanilla 1.12.2
    biomes.
*   Nether fossils: **10668 soul sand valley candidates, 0 differences**; column, template and rotation of all 5465
    fossils.
*   **Block for block**: 4 whole bastions (one of each kind, 57289-121395 blocks each) and 18 ruined portals (9 overworld,
    9 nether, giant included) built chunk by chunk at their own coordinates over a marker: every block 1.16.5 places is
    placed, the same block and metadata, and nothing where 1.16.5 places nothing. The one exception is the jigsaw block
    1.16.5 leaves in some portal floors: 1.12.2 has none, so Core places its final state (netherrack).

What cannot match: heights that follow terrain. Bastions sit at 33 and every start chunk is exact, but a ruined
portal's height and a fossil's come from each game's own terrain (1.12.2's nether noise, which stream V keeps, is not
1.16's). In this world (seed 12345) 7 of 9 nether portals and 8 of 9 overworld portals came out at 1.16.5's height, 0
of 6 fossils. The extras around a portal follow the terrain too.

## Tested headlessly

`-Pselftest=structure`: **pass=243 fail=0** (seed 12345; also pass=241 fail=0 with a random seed, which skips this
world's slot comparison); `-Pselftest=all`: **pass=6907 fail=0**;
`check_assets.py`: **92 blocks, 1060 states, 93 item models, 647 sound files, 0 errors**.

*   Registration: the slot rule installed; bastions and fossils in the nether only, portals in both, Core's locators;
    nether portal spacing 25/10, overworld 40/15; the owner copies registered, the lantern at light 15.
*   Palette: all 60 bastion pools load; every template they name, the 13 portals and the 14 fossils load and every block resolves; every block the
    processors make resolves; slab types, the chain's axis, a hanging lantern, the hoglin and loot table mappings.
*   Loot: the five tables load; 400 rolls each give no empty stack and no stand-in; netherite, debris, enchanted golden
    apples (treasure), Soul Speed books, scrap, chains (other), crimson items and saddles (stables), lodestones,
    obsidian and glistering melon (portals); *War Pigs*' four events fire for the player, none for portals.
*   The oracle comparisons above.
*   Generation in a seed-12345 world: a bastion laid out from the populate step with the planned pieces and built
    (28973 polished blackstone bricks, 9 chests with this mod's tables), stable hoglins persistent and never hunted, no
    item dropped, no fortress in its slot, saved and loaded the same, *Those Were the Days* inside a piece and not
    outside; 9 nether and 9 overworld ruined portals laid out and built, every planned netherrack, magma, vine and leaf
    placed (those decoration later covered counted apart), one of each saved and loaded the same; 6 fossils built and
    one saved and loaded; a fortress in its 1.16.5 slot.
*   **Cascading worldgen: 0** chunk loads during population across all of it (both dimensions, with stack traces), and
    no Forge cascade warning in the log.

## Not tested

*   Piglins and brutes in bastions: stream P is not on this branch, so the mob pieces stay empty here. After P merges,
    `-Pselftest=all` checks that they spawn armed as their templates.
*   Anything visual (in a client), and a player opening a chest by hand.
*   With Village & Pillage installed: its lantern, slabs and walls should be used and the copies not registered.
*   Other nethers (Biomes O' Plenty's): the rule falls back to Core's block-biome reading, untested.

## For the integrator

*   **Backport Core 1.2.0** is required (`core_version=1.2.0`, `required-after:backportcore@[1.2.0,)`).
*   `build.gradle`: a test build adds `src/selftest/resources` (the oracle answers) to the resources; a release jar has
    none of them (checked).
*   Merge notes: `tools/owned/copy_from_owner.py` and `owned/OwnedContent.java` gained the structure blocks (stream P
    also edits both, for the crossbow); the `owned` lang block is regenerated by that script; the config lang block by
    `tools/config_lang.py`.
*   Stream M: `music.pigstepInFortressChests` ("until bastions exist") should default off, or follow
    `structures.bastions`, now that Pigstep is in bastion chests.
*   Stream P: `BastionMobs.spawn` is called with the template names `crossbow_piglin`, `sword_piglin`, `melee_piglin`,
    `melee_piglin_always`.
*   For Backport Core (see `docs/CORE_REQUESTS.md`, section 5).
