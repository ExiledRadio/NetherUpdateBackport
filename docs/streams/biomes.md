# Stream V: nether biomes

Package `biome` (client side in `biome/client`). Config `biomes` (`enabled`, `weights`, `overrideOtherMods`). No
entity or packet ids. Proposal A of *Decided* item 1.

## Changelog section

*   **Four new nether biomes**: the **crimson forest** (crimson nylium, huge crimson fungi, weeping vines, hoglins
    and piglins), the **warped forest** (warped nylium, huge warped fungi, nether sprouts, twisting vines, endermen),
    the **soul sand valley** (soul sand and soul soil, basalt pillars, skeletons and ghasts, blue fog) and the
    **basalt deltas** (basalt and blackstone, lava pools rimmed with magma, basalt columns, magma cubes, white ash).
    The old nether becomes the **nether wastes**.
*   The nether's biomes are laid out exactly as in 1.16 for the world's seed, so seed maps for 1.16 show them right.
*   Each biome has its own fog color and ambient particles (ash, white ash, crimson and warped spores).
*   Gravel generates in veins in every nether biome but the basalt deltas; mushrooms can grow anywhere in the nether
    wastes and the forests, the nether roof included.
*   Only nether chunks generated from now on get the new biomes; chunks you have explored stay nether wastes.
*   When another mod already changes the nether's biomes, these step aside (a config option makes them win).

## How it works

*   **Biomes**: `netherupdatebackport:crimson_forest`, `warped_forest`, `soul_sand_valley`, `basalt_deltas`, each a
    `BiomeHell` subclass tagged NETHER, HOT, DRY. Nether wastes is vanilla's `minecraft:hell`; 1.12.2 biome names are
    plain text, so all five names are set from `biome.netherupdatebackport.<id>` (server: at post-init; client: on
    every resource reload, so in the player's language).
*   **Provider**: at post-init dimension -1 is re-registered with a `DimensionType` whose provider is
    `WorldProviderNether`, a `WorldProviderHell` that changes only the biome source (terrain stays
    `ChunkGeneratorHell`; `getDimensionType()` is still `NETHER`). If dimension -1's provider is anything but vanilla's
    `WorldProviderHell` at that point, another mod has replaced the nether and ours steps aside, unless
    `biomes.overrideOtherMods`. A mod that replaces it later simply wins. Everything below checks that the world's
    biome source is ours (`NetherBiomes.active`).
*   **Biome source** (`NetherBiomeSource`), bit-exact 1.16.5 for seed parity: `NetherBiomeProvider` with the
    `NETHER` preset's five points (nether wastes 0/0/0/0/0, soul sand valley humidity -0.5, crimson forest
    temperature 0.4, warped forest humidity 0.5 offset 0.375, basalt deltas temperature -0.5 offset 0.175), float
    fitness, first point wins ties; four `MaxMinNoiseMixer`s (first octave -7, amplitudes 1, 1) on seeds world seed
    +0..+3; one biome per 4x4x4 cell, y ignored (`useY` false). `Noise` ports `ImprovedNoiseGenerator`,
    `OctavesNoiseGenerator` (including the skipped 262 random steps and the second random for positive octaves) and
    `MaxMinNoiseMixer` line for line; `java.util.Random` stands in for `SharedSeedRandom`, which is the same
    generator. Block biomes go through 1.16's `FuzzedBiomeMagnifier` with the SHA-256 world-seed hash; 1.12.2 stores
    one biome per column, so a column takes the fuzzed biome at y 64 (`COLUMN_Y`).
    `NetherBiomeSource.dump(seed, qx0, qz0, n, out)` writes the 4-block grid as 1.16 numeric ids (8, 170-173) for
    the seed check.
*   `biomes.weights`: a weight divides a point's fitness (bigger grows the biome), 0 removes it; all 1 (the default)
    is exactly vanilla.
*   **Surfaces** (`NetherSurfaces`, on `ChunkGeneratorEvent.ReplaceBiomeBlocks`, answered DENY when a chunk has any
    new biome): 1.12.2's own `buildSurfaces` algorithm, run per column with the generator's own noise and random
    (read by reflection), so nether wastes columns and all bedrock are vanilla's; the other columns get 1.16.5's
    `NetherForestsSurfaceBuilder` (nylium, netherrack, wart blocks), `SoulSandValleySurfaceBuilder` and
    `BasaltDeltasSurfaceBuilder` (both `ValleySurfaceBuilder`: floor and ceiling picked by noise, gravel patches near
    y 29-34) on the raw terrain, with 1.16's surface depth noise (`NoiseChunkGenerator`'s, after its three terrain
    noises), their own noises on 1.16's seeds and 1.16's per-chunk random, then vanilla's bedrock over them. Sea
    level is 1.16's 32 (1.12.2's lava sea is at the same height).
*   **Features** (`NetherDecorator`, from each biome's `decorate`, and for nether wastes from
    `DecorateBiomeEvent.Post`), from 1.16.5's `BiomeMaker` and `Features`:
    *   crimson forest: weeping vines (ceiling wart clumps with vines, 10), huge crimson fungi (count-multilayer 8),
        crimson forest vegetation (6);
    *   warped forest: huge warped fungi (8), warped forest vegetation (5), nether sprouts (4), twisting vines (10);
    *   soul sand valley: basalt pillars (10), a crimson roots patch, soul fire patches on soul soil, soul sand veins
        (12 of 12, y 0-31);
    *   basalt deltas: deltas (count-multilayer 40), small (4) and large (2) basalt columns, basalt (75) and blackstone
        (25) blobs, delta springs (16) instead of open springs, and the doubled closed springs, quartz and gold;
    *   all but deltas: gravel veins (2 of 33, y 5-41); wastes and forests: brown (1 in 4) and red (1 in 8) mushroom
        patches anywhere up to twice the column height.
    *   1.12.2's own nether decoration (fire, glowstone, nether mushrooms, quartz, magma, springs) already matches
        1.16's and keeps running; Forge's terrain events turn off the nether mushrooms in the forests and soul sand
        valleys and the open springs in the deltas, as 1.16 leaves them out there.
    *   Huge fungi, forest vegetation and twisting vines are stream F's own feature code (`HugeFungus`,
        `NetherVegetation`), called with "not planted", as PLAN.md's F section intends.
    *   **No cascading**: every feature starts at chunk origin + 8 + 0..15 and reads and writes through `Region`, the
        four chunks a population may touch: outside reads as bedrock (which every feature leaves alone), outside
        writes are dropped, and placement uses flag 2. 1.12.2 ores (`WorldGenMinable`, which adds 8 itself) take
        origin + 0..15 as vanilla's do. The wider 1.16 features (large basalt columns, wide deltas) are clipped at
        that edge, which can leave a straight edge on a rare column cluster.
*   **Spawns** (1.16.5's lists): crimson forest zombified piglin 1 (2-4), hoglin 9 (3-4), piglin 5 (3-4); warped
    forest enderman 1 (4-4); soul sand valley skeleton 20 (5-5), ghast 50 (4-4), enderman 1 (4-4); basalt deltas ghast
    40 (1-1), magma cube 100 (2-5); strider 60 (1-2, creature) in all four; piglin 15 (4-4) added to nether wastes.
    This mod's mobs are looked up by registry name at post-init, so a stream switched off (or piglins, not merged
    yet) just adds nothing. Striders keep stream R's in-lava placement and `striders.spawn`.
*   **Client** (`BiomeClient`): the fog's base color is 1.16's `CubicSampler` Gaussian blend of the fog colors of
    the 6x6 biome cells around the camera (through `WorldProviderNether.getFogColor`, so 1.12.2's later fog steps,
    lava fog included, still apply); ambient particles as 1.16's `ClientWorld.animateTick` (667 x 2 random positions
    within 16 and 32 blocks a tick, each spawning the biome's particle with its probability): ash and white ash as
    1.16's `RisingParticle`, spores as `UnderwaterParticle`, all on the generic_0 sprite (1.12.2's particles.png
    index 0), so no textures.
*   **Lookup for other streams**: `biome.NetherBiomes.at(world, pos)` returns a `Kind` (`NETHER_WASTES`,
    `CRIMSON_FOREST`, `WARPED_FOREST`, `SOUL_SAND_VALLEY`, `BASALT_DELTAS`) or null outside these five; `of(biome)`
    for a biome; `Kind.id`, `vanillaId`, `fogColor`, `particle`. Works on both sides and with V switched off
    (nether wastes only).

## For the integrator

*   *Hot Tourist Destinations* (P) needs no code: 1.12.2's `minecraft:location` trigger takes a `biome`, so five
    criteria on `minecraft:hell`, `netherupdatebackport:crimson_forest`, `warped_forest`, `soul_sand_valley` and
    `basalt_deltas` do it. Stream M can call `NetherBiomes.at` for music and ambience.
*   P must **not** register its own biome spawns: V adds piglins to the crimson forest and nether wastes by name once
    `netherupdatebackport:piglin` exists. With V switched off nothing adds piglins to the nether wastes; P may want to
    add that one spawn itself when `biomes.enabled` is false.
*   S's `BlackstoneGeneration` and U's `NetherGoldGeneration` add 8 to their positions and use `WorldGenMinable`,
    which adds 8 again, so their veins reach up to about 7 blocks past the populated area (a cascading-worldgen risk;
    see the test notes). Vanilla's own ores pass origin + 0..15. In basalt deltas 1.16 has no `ore_blackstone`
    (blackstone blobs instead); S still places its two blobs there.
*   N's ancient debris generator: unchanged (every 1.16 nether biome has debris).

## Tested headlessly

`-Pselftest=biome`: **pass=139 fail=0** (three runs, three random world seeds); all streams (`-Pselftest=all`):
**pass=6514 fail=0**; `check_assets.py`: **84 blocks, 884 states, 87 item models, 636 sound files, 0 errors**.

*   Registration: four biomes, their names (and hell's "Nether Wastes"), NETHER/HOT/DRY, no rain, the lookup.
*   Spawn lists against 1.16.5's numbers, hoglins and striders included; piglins only when that entity exists.
*   Provider: dimension -1 is `WorldProviderNether` with `NetherBiomeSource` seeded from the world, still
    `DimensionType.NETHER`, water vaporizes; the step-aside test says vanilla's type is vanilla and other types are not.
*   Noise: 1.16's improved noise equals 1.12.2's own `NoiseGeneratorImproved` (same random draws and gradients) to
    1e-9 over 200 random seeds and points; the source is deterministic per seed, differs between seeds, ignores y,
    has all five biomes over 2% each of a 3200-block square; fuzzed block biomes mostly agree with their cell and a
    chunk's columns are the fuzzed biomes at y 64; weight 0 removes crimson forests, weight 3 grows warped ones.
*   Seed-check grids written for six seeds (the world's, 0, 1, 12345 and two large ones).
*   Features on a hand-built stage: the region reads outside as bedrock and drops writes there; a basalt pillar hangs
    from a ceiling to the floor (and needs a ceiling); multilayer placement finds both floors; deltas pour lava and
    magma, never outside the region; large basalt columns stand on a floor; a blob is a Manhattan diamond of radius
    at most 7 from the top netherrack; `withinManhattan` covers its box, nearest first.
*   Generation: for each of the five biomes, a 140-block square of it is found in the seed, 8 x 8 chunks generated and
    the inner 6 x 6 inspected: populated, every column stores the biome, bedrock floor and roof in every column, and
    per biome nylium floors, huge fungi, roots, vines and wart blocks (forests), soul sand and soul soil and no
    mushrooms (valley), basalt, blackstone and magma (deltas), and nothing new in the wastes.
*   **Cascading worldgen: 0** chunk loads during population while the five biomes generate (a chunk-load watch with
    stack traces, plus Forge's own warning). That watch also found that **streams S and U** cascade: their
    `BlackstoneGeneration` and `NetherGoldGeneration` add 8 and `WorldGenMinable` adds 8 again. They are switched off
    for this stream's generation test; with them on, Forge logs 70 warnings in `-Pselftest=all` (all from those two;
    17 in `-Pselftest=stone,blocks` with this stream off too, so they predate it), and with them off, 0 in
    `-Pselftest=stone,blocks,netherite`.
*   `StoneSelfTest`'s "blackstone only near its blob range" check now looks at nether wastes columns only: basalt
    deltas have blackstone floors.

## Not tested

*   Bit-for-bit agreement with vanilla 1.16.5 or cubiomes: the self-test writes `eclipse/selftest-nether-biomes-<seed>.txt`
    (256 x 256 cells at 4-block resolution, quart -128..127, 1.16 numeric ids) for six seeds, ready for
    `tools/seedcheck`, but no oracle was run here.
*   Anything visual: fog colors, particles, surfaces as seen in a client; music and ambience (stream M).
*   Natural mob spawning over time in the new biomes.
*   Living alongside another mod's nether (the step-aside decision is tested on vanilla's and the overworld's types).
