# Nether Update Backport: plan

Everything Java Edition 1.16 (the Nether Update, with 1.16.1 to 1.16.5) added or changed, grouped into
streams, each in its own package, config category and worktree. Working display name "Nether Update
Backport"; the name is not final (it is taken on CurseForge), so it lives only in `gradle.properties`
(`mod_name`, substituted into the mod class and `mcmod.info`) and in `en_us.lang` (`itemGroup`). The mod id
`netherupdatebackport` is permanent.

Sources: minecraft.wiki's *Java Edition 1.16* to *1.16.5* pages and the pages they link. Where current Java
behaves differently from 1.16, current Java wins (marked **now:**), as the family's villager-parity rule
says. Numbers the wiki leaves out come from 1.16.5's decompiled code (`Workspaces/MC1165Reference`, Forge
36.2.39 with Mojang's names, made for this mod). Assets come from the 1.18.2 client jar and the 1.18 asset
index (`tools/assets.py`); none of 1.16's nether textures changed after 1.16.

## Scope at a glance

| In | Out (and why) |
| --- | --- |
| Every new block, item, mob, enchantment, music disc, particle and advancement; 1.16's changes to zombie pigmen, fire, the nether's music and sounds, and smaller block rules | Data packs, custom dimensions and biomes from JSON, the jigsaw block, the attribute rename, new gamerules and commands: technical, 1.12.2 cannot take them and players never see them |
| Netherite upgrades on Village & Pillage's smithing table (Rule 2) | The smithing table itself (Village & Pillage owns it; see N) |
| The four nether biomes and the structures (see *Decided*) | Social-interactions screen, chat-delay and other options-menu changes |

## Content owned by another mod of the family

Nothing 1.16 added belongs to another mod: this mod declares every block it registers with
`BlockOwnership.declare("netherupdatebackport", ...)`, so later mods (Trails & Tales' crimson and warped
hanging signs, say) find ours. This mod *uses* these older things and never registers copies of them:

| Owner | Thing | How we use it |
| --- | --- | --- |
| Village & Pillage (`villagerbackport`) | Smithing table | Netherite upgrades registered through Backport Core's `SmithingUpgrades.add`, which the table reads. Without Village & Pillage: an identical copy of its table (`owned`, Rule 1), with 1.16's upgrade screen. |
| Village & Pillage | Campfire | The soul campfire is our own block, written after the campfire's code pattern (same tile layout, cooking, signal smoke over hay, dousing). When the campfire's owner is installed, its campfire recipes are read so both cook the same things. |
| Village & Pillage | Lantern, chain-less | Soul lantern is ours; it hangs from our chain. |
| Village & Pillage | Crossbow | Piglins carry the owner's crossbow when installed; without it, an identical copy of the owner's crossbow (`owned`, Rule 1), so half of them carry crossbows as in vanilla. |
| Village & Pillage | Composter | 1.16's compostables (fungi, roots, sprouts, vines, wart blocks, shroomlight) through `common/Compat.compost`. |
| Village & Pillage | Loom, banner patterns | The Snout (piglin) banner pattern from bastion chests, registered with the owner's pattern list when installed; otherwise the item exists but has no loom. |
| Village & Pillage | Blue ice | The basalt generator (lava beside blue ice over soul soil) uses the owner's blue ice, or Update Aquatic's copy, looked up through `BlockOwnership.resolve`. |
| Update Aquatic (`updateaquaticbackport`) | Bubble columns | Soul sand already makes them there; soul soil does not (as 1.16). Nothing to do. |

## Streams

Each stream owns one package under `com.exiledradio.netherupdatebackport`, one config category with an
`enabled` switch (already present in `ModConfig`), one block of packet ids and one block of entity ids.
No stream calls another's classes; when one needs another's item it looks it up with
`common/Owners.ours(name)`, which returns null when that stream is switched off.

| # | Stream | Package | Config | Entity ids | Packet ids | Port | Needs |
| --- | --- | --- | --- | --- | --- | --- | --- |
| S | Blackstone and basalt | `stone` | `stone` | none | 5-9 | 25821 | nothing |
| U | Smaller blocks | `blocks` | `blocks` | none | 10-14 | 25822 | nothing |
| F | Crimson and warped | `fungus` | `fungus` | none | 15-19 | 25823 | nothing |
| L | Soul fire | `soul` | `soul` | none | 20-24 | 25824 | nothing |
| N | Netherite | `netherite` | `netherite` | 1-4 | 25-29 | 25825 | nothing |
| H | Hoglins and zoglins | `hoglin` | `hoglins` | 10-14 | 30-34 | 25826 | F (warped fungus by name) |
| P | Piglins and piglin brutes | `piglin` | `piglins` | 15-19 | 35-39 | 25827 | N, F, U by name |
| R | Striders | `strider` | `striders` | 20-24 | 40-44 | 25828 | F by name |
| Z | Zombified piglins and other mob changes | `mobs` | `mobs` | none | 45-49 | 25829 | nothing |
| M | Music and sounds | `music` | `music` | none | 50-54 | 25830 | nothing (biome music needs V) |
| C | Smaller changes | `changes` | `changes` | none | 55-59 | 25831 | nothing |
| V | Nether biomes | `biome` | `biomes` | none | 60-64 | 25832 | **the user's answer** |
| X | Structures | `structure` | `structures` | 30-34 | 65-69 | 25833 | **the user's answer**, Backport Core |
| Fm | Future MC pairs | `common` (integrator) | `compat` | none | none | 25834 | S, U, F, L, N |
| Ow | Owner copies (smithing table, crossbow) | `owned` | none (registered exactly when the owner is absent) | 40-44 | 70-74 | 25835 | nothing |
| Lf | Look and feel (panorama, splashes, options, F3 keys) | `lookfeel` | none (Backport Core's `presentation` settings) | none | none | 25940 | Backport Core 1.2.0 |

Ports: 25820 on `main`, `25820 + row` in a stream's worktree (row = the table order above). Entity ids are
this mod's own `EntityRegistry.registerModEntity` numbers. Enchantments, potions and sounds have no numbers
to collide on.

### S. Blackstone and basalt

- **Basalt** and **polished basalt** (pillar axis, hardness 1.25, blast 4.2, basedrum, 1.16's basalt sounds);
  4 basalt make 4 polished basalt (and the stonecutter recipe appears through the owner's crafting-recipe
  scan). **Smooth basalt** is 1.17's: not here.
- **Basalt generator**: lava flowing next to blue ice over soul soil makes basalt (1.16), from the fluid's
  neighbor-change event; blue ice resolved through ownership.
- **Blackstone** with its stairs, slab, wall; **polished blackstone** with stairs, slab, wall, pressure
  plate (mobs only, like stone) and button; **polished blackstone bricks** with stairs, slab, wall;
  **cracked polished blackstone bricks** (smelted); **chiseled polished blackstone**; **gilded blackstone**
  (drops itself, or 2-5 gold nuggets 10% of the time, **now:** Fortune raises the chance, Silk Touch keeps it).
- Blackstone works as cobblestone for stone tools, furnaces and brewing stands (an ore name,
  `cobblestone` + recipe copies that take blackstone; vanilla's own recipes are not edited).
- Generation (decoration only, no terrain change): blackstone blobs in the nether (1.16's `ore_blackstone`:
  2 per chunk, size 33, y 5-31), under `stone.generate`.

### U. Smaller blocks

- **Chain** (axis, iron sounds, 1.16's hitbox; 2 nuggets + 1 ingot), climbable no (as 1.16).
- **Crying obsidian** (light 10, dripping obsidian tear particles, hardness 50), **cracked** and **chiseled
  nether bricks**, **quartz bricks** (4 quartz blocks), **nether gold ore** (2-6 nuggets, Fortune, piglins
  angry when mined; generation 10 per chunk y 10-117 and **now:** extra in the old Hell biome's place).
- **Target** (power 1-15 by how near the centre an arrow, trident or snowball lands; 8 ticks, 20 for arrows;
  hoe is its tool).
- **Respawn anchor**: 4 charges of glowstone; sets the spawn point in the nether; in any other dimension
  right-clicking a charged anchor explodes it (power 5, fire). Respawning uses a charge. Handled at
  `PlayerSetSpawnEvent`/`PlayerRespawnEvent` time plus our own saved spawn record per player (1.12.2's spawn
  point is per-dimension only for beds; the anchor's spawn is kept in player capability data and applied
  on respawn), with 1.16's "anchor depleted / obstructed" messages.
- **Lodestone** and the **lodestone compass**: using a compass on a lodestone makes a lodestone compass
  (NBT position + dimension, works in any dimension, spins when the lodestone is gone; enchantment glint).
  The needle uses our own `IItemPropertyGetter` on our compass item (vanilla's compass item is not
  changed); a vanilla compass becomes ours when used on a lodestone.

### F. Crimson and warped

- **Crimson and warped nylium** (spread to netherrack beside them with bone meal; netherrack under a block
  becomes plain netherrack, as 1.16), **fungi** (placed on nylium, soul soil, mycelium, dirt, farmland and
  netherrack; bone meal on the matching nylium grows a **huge fungus**, 40%), **roots**, **nether sprouts**,
  **weeping vines** and **twisting vines** (both with plant parts, growing down and up, climbable, age 0-25
  folded into 16 values as Update Aquatic's kelp does, **now:** shears stop growth), **wart blocks**
  (nether and warped; the nether wart block is vanilla's in 1.12.2 already), **shroomlight**.
- Both **wood sets**: stems, hyphae, stripped stems and hyphae (axe stripping), planks, slabs, stairs, fences,
  fence gates, doors, trapdoors, buttons, pressure plates, signs (Village & Pillage's sign tile pattern:
  our own `TileEntitySign` subclass and renderer). Nether woods do not burn and are not fuel.
- **Compost** values and **bone meal** rules; hoes are the tool for wart blocks (with C).
- Huge fungi and vines/roots also make up the crimson and warped forests of stream V; the features are
  written here so V only places them.

### L. Soul fire

- **Soul soil** (no slowdown, unlike soul sand), **soul fire** (on soul sand and soul soil only: fire placed
  there becomes soul fire, 2 damage instead of 1, light 10, no spread), **soul torch** and wall torch
  (coal + stick + soul soil or sand; light 10, soul fire flame particles), **soul lantern** (light 10,
  hangs from chains), **soul campfire** (light 10, 2 damage, our own block after the campfire's pattern).
- **Soul Speed** I-III (boots, treasure only, from bastion chests and piglin bartering): faster on soul sand
  and soul soil, soul particles, durability loss per 1.16; soul sand's slowdown cancelled for the wearer.
- Particles: soul fire flame, soul. Wither skeletons spawn in soul sand valleys belongs to V.

### N. Netherite

- **Ancient debris** (hardness 30, blast 1200, diamond pickaxe, explosion-proof), **netherite scrap** (smelt
  debris, 2 experience; also in the owner's blast furnace through its furnace-recipe scan), **netherite
  ingot** (4 scrap + 4 gold), **netherite block** (9 ingots, beacon base), beacon payment.
- **Netherite tools and armor** (1.16.2 numbers: 2031 durability, mining level 4 = diamond's in 1.12.2 for
  every block's check, damage +1 over diamond, armor 3/6/8/3, toughness 3, knockback resistance 0.1 per
  piece, enchantability 15), repaired with ingots.
- Netherite items and their dropped item entities **float on lava and never burn** (our own `EntityItem`
  subclass through `Item.createEntity`; entity id 1).
- **Smithing** (Rule 2): `SmithingUpgrades.add(diamond_x, ingot, netherite_x, MOD_ID)` for all nine pieces,
  read by Village & Pillage's table or this mod's copy of it (stream Ow).
- Generation (decoration only): ancient debris, 1.16.2's two placements (one 1-3 vein at y 8-24 and one 1-2
  vein at y 8-119 per chunk, hidden: never exposed to air).
- Advancements: *Hidden in the Depths*, *Cover Me in Debris*, *Serious Dedication* (husbandry).

### H. Hoglins and zoglins

- **Hoglin** (40 health, knockback attacks that throw targets up, breeds with crimson fungus, babies,
  drops porkchops and leather, flees from warped fungus, respawn anchors and nether portals; zombifies into
  a zoglin in the overworld after 15 s). **Cooked porkchop** drop when on fire.
- **Zoglin** (attacks everything that is not a creeper or zoglin, undead).
- Spawning in crimson forests, where vanilla puts them, belongs to V.
- Advancement *Make a Wish?* is piglin's; *The Parrots and the Bats* gains hoglins (breeding).

### P. Piglins and piglin brutes

- **Piglin**: adult and baby, golden sword or crossbow (the owner's), wears gold, likes gold (picks up and
  admires gold items for 6 s, then **bartering** with gold ingots from 1.16.2's table, **now:** current Java's
  weights), angers at players without gold armor, at players who open chests or break gold blocks, gold ore
  and nether gold ore near them, and at anyone who hurts a piglin; avoids soul fire, soul torches, soul
  lanterns, soul campfires and zombified piglins/zoglins; hunts baby hoglins; dances after a hoglin kill;
  zombifies into a zombified piglin (vanilla's pigman) in the overworld after 15 s.
- **Piglin brute** (1.16.2): 50 health, golden axe, always hostile, never barters, spawns only in bastions
  (stream X).
- **Snout banner pattern** item (see the Loom row above).
- Advancements *Oh Shiny* (distract with gold), *Hot Tourist Destinations* (needs V), *War Pigs* (needs X),
  *Those Were the Days* (needs X).
- Gold-related anger reads blocks by name (`nether_gold_ore` from U, gilded blackstone from S).

### R. Striders

- **Strider** (20 health, walks on lava, shivers and slows out of lava, cold/warm look, breeds with warped
  fungus, can be saddled; drops string; babies ride adults; sometimes spawns ridden by a zombified piglin or
  carrying a baby). **Warped fungus on a stick** (steers like the carrot on a stick, 100 uses, 1 per boost).
- Spawns on lava oceans across the nether (the one 1.16 mob that never needed a new biome; behind
  `striders.spawn`).
- Advancements *This Boat Has Legs*, *Feels Like Home*.

### Z. Zombified piglins and other mob changes

- **Zombie pigmen become zombified piglins and stay the same entity.** Vanilla's `minecraft:zombie_pigman`
  keeps its id, so every world, spawner, loot table and other mod that names it keeps working. What
  changes: the name (lang override of `entity.PigZombie.name`), the model and texture (our renderer for
  `EntityPigZombie`, client side), the sounds (1.16's zombified piglin sounds), and 1.16's anger rules
  (angry only at whoever hit it and the group within 20 blocks that can see it; forgets when that player dies
  or after 20-39 s out of sight), done through events and AI tasks added at spawn, never by editing a goal
  list from inside a goal.
- Other 1.16 mob changes that need no biome: wither skeletons and blazes unchanged; endermen and ghasts get
  their 1.16 spawn rules in V.

### M. Music and sounds

- **Pigstep** music disc (bastion chests, stream X; without X, a small chance in nether fortress chests behind a
  switch). Its record item and jukebox play through 1.12.2's `ItemRecord`.
- Nether music: 1.16's tracks (*Rubedo*, *Chrysopoeia*, *So Below*, and the old nether tracks) through
  Forge's `PlaySoundEvent`/music hooks; per biome once V exists, the 1.16 set everywhere before that.
- Nether ambience (per-biome loops and additions) once V exists.

### Lf. Look and feel (1.16's non-content changes)

Registered with Backport Core's `presentation` module (Core 1.2.0), which shows the newest installed version's
pieces. Research: `family-notes/RESEARCH_NON_CONTENT_CHANGES.md`, section 1.16. What is in, what is out and why:
`docs/IN_GAME_CHECKLIST.md` (section Lf) and `docs/CORE_REQUESTS.md` (item 6).

- `Presentation.version("1.16")`: the nether panorama (1.16.5's six faces from the asset index), its clear
  `panorama_overlay.png`, the splash changes (1.15.2 to 1.16.5's `splashes.txt`: 41 added, 14 removed), Entity
  Distance (P4), F3+F4's game mode switcher and F3+N's previous-mode toggle. The "Reading world data" loading
  screen comes from Core's table for 1.16.
- `Presentation.version("1.16.2")`: Distortion Effects (P3 plus the nausea overlay) and FOV Effects, on Video and
  Accessibility Settings.
- Singleplayer opens Create New World when there is no world (1.16's `WorldSelectionList`).
- Vanilla text 1.16 reworded on keys 1.12.2 also has (Saved Hotbars, You Died!, Direct Connection ...) is in
  `en_us.lang`, with the 18 languages from Mojang's files (`tools/lang/build_langs.py`).
- Menu music: unchanged in 1.16 (menu1 to menu4 in 1.12.2 and 1.16.5). The nether music is stream M's.

### C. Smaller changes

- Hoes are the tool for leaves, hay bales, wart blocks, sponges, dried kelp blocks and targets (harvest tool on
  vanilla blocks via `setHarvestLevel`, not a replacement).
- Fire Resistance lets you see farther in lava (client fog event).
- Gold ingots and blocks unchanged; lodestone compass in U.
- New nether-tab advancements that belong to no stream (*Into Fire* stays vanilla; *Not Quite "Nine" Lives* is
  U's).

### V. Nether biomes (still open)

Proposal A (see *Decided* below). Package `biome`. Built after the mobs.

### X. Structures (still open)

Bastion remnants (four kinds, their loot), nether fossils (soul sand valleys), ruined portals (overworld,
nether and their variants, crying obsidian and gold blocks, their loot). Bastions take fortress slots (see *Decided*).

### Fm. Future MC pairs

Future MC 0.2.21 (which the user's Dregora packs run) registers most of 1.16's blocks under `futuremc:`
(ancient debris, basalt and blackstone sets, chain, crimson and warped sets, soul fire set, crying obsidian,
gilded blackstone, nether gold ore, nether sprouts, shroomlight, netherite block and more). Following Rule 3,
once S, U, F, L and N exist this mod contributes its pairs to Backport Core's table
(`FutureMcTable.contributor`, `DuplicateBlocks.register`), with states recorded by `/futuremc dump` in a dev
instance that has Future MC, and `/futuremc check` reporting 0 mismatches.

## Decided (formerly *Still open*)

### 1. The four nether biomes: decided 2026-11-07, Proposal A

The user chose **Proposal A**: real biomes behind `biomes.enabled`, vanilla's nether terrain shape kept, and
stepping aside when another mod has already replaced the nether's biome provider. Built as stream V, after the mobs.
The original write-up follows.

Crimson forest, warped forest, soul sand valley and basalt deltas, with nether wastes for the rest. They
replace vanilla's single `Hell` biome in newly generated nether chunks, which matters in big packs: the
user's RLCraft Revamped wants a 1.16 nether without modded biomes, and many 1.12.2 mods (Lycanites,
Biomes O' Plenty's nether, spawn and loot configs) key on the nether's biome.

**Proposal A (recommended): real biomes, vanilla terrain shape, behind a switch.**
- Register the five biomes (`netherupdatebackport:crimson_forest` and so on; nether wastes is vanilla's
  `minecraft:hell` renamed in lang, so old chunks are already "nether wastes"). Tag them all `NETHER`, `HOT`,
  `DRY` in the Biome Dictionary so other mods' nether spawns and configs still find them.
- A `WorldProviderHell` subclass for dimension -1 (re-registered through `DimensionManager`, no patch) whose
  only change is the biome provider: 1.16's multi-noise biome source with its five parameter points, seeded
  from the world. Terrain stays 1.12.2's `ChunkGeneratorHell`: 1.16's nether biomes all share one terrain
  shape (depth 0.1, scale 0.2), so the walls, lava seas and caves are already right, and every mod that hooks
  vanilla's nether generator still works.
- Per biome: surface (nylium, soul sand and soul soil, basalt and blackstone) and features (huge fungi,
  vines, roots, basalt pillars and deltas, ore changes, fossils), spawn lists from 1.16.5, fog colors,
  particles, music and ambience.
- `biomes.enabled`, default **on**, plus `biomes.weights` to rescale or drop a biome. If another mod already
  replaced the nether's provider (Biomes O' Plenty's nether), ours steps aside unless told to win.
- Existing worlds: generated nether chunks keep their biome; new chunks get the new biomes, with a seam at
  the edge, as any 1.12.2 biome mod.

**Proposal B: decoration only, no new biome ids.** Everything stays `minecraft:hell`. Our own noise (the same
multi-noise points) picks a "region" per column; the surface and features are rewritten in the populate
event, and spawns, fog and music read the region from the same noise (the client gets the seed hash from a
login packet). Nothing in other mods' configs changes, but F3, JEI's biome lists, other mods' biome-keyed
spawns and 1.16's own mob spawning by biome all see one biome, and the per-column region lookups cost more.

Either way the 1.16 mobs' biome spawns (hoglins and piglins in crimson forests, endermen in warped forests,
ghasts and skeletons in soul sand valleys, magma cubes in basalt deltas) come with this stream.

### 2. Structures: decided 2026-11-07

Bastions take nether-fortress slots, as in 1.16 (one or the other per region). Nether fossils and ruined portals
where vanilla puts them. Needs Backport Core's template loader, a jigsaw assembler and nether heights
(`docs/CORE_REQUESTS.md`); stream X starts once Core has them.

### 3. Smaller ones: decided 2026-11-07

- The mod works fully on its own: owner copies (Rule 1), never crafting stand-ins. The crafting-grid netherite
  fallback is gone; the smithing table copy replaces it (stream Ow, done). Piglins get a copy of the crossbow.
- Hoglins spawn only where vanilla puts them (crimson forests, stream V); no nether-wide switch.
- **Closest to vanilla, always** (the user's standing rule): our own ASM coremod is allowed where vanilla parity
  needs it, each patch in `docs/PATCHES.md`, no MixinBooter. Approximations made before this rule that a patch
  could now fix are listed there.
- The display name is still the user's to choose; the working name stays.

## Waterlogging (later pass, through Backport Core's adapter)

Blocks 1.16 made waterloggable that this mod adds: chain, soul lantern, soul campfire, every slab, stair and
wall (blackstone, polished blackstone, polished blackstone bricks, crimson, warped), crimson and warped
fences, trapdoors, signs and wall signs. Left non-waterloggable until Core's adapter exists.

## Shared files and rules

As Update Aquatic's plan, unchanged:

- **`ModConfig.java`**: every category already exists. A stream edits only inside its own class. Category
  classes extend `Object`.
- **`NetworkHandler.java`**: register packets only with ids from your block, in id order.
- **`NetherUpdateBackport.java`**: at most one line per lifecycle hook, calling your package.
- **`selftest/SelfTestRunner.java`**: one line in `TESTS`.
- **`en_us.lang`, `sounds.json`**: written only by your generator through `tools/assets.py`
  (`merge_lang` / `merge_sounds`), which replaces your own `# --- <stream> ---` block and nothing else.
- **`common/`**: read freely; changing it is the integrator's job.
- **Registration**: from your own `@Mod.EventBusSubscriber` class, through `common/Content`.
- **Other streams' items**: by name with `Owners.ours("name")`, null-safe. Another mod's with
  `BlockOwnership.resolve` or `Owners.block(owner, name)`.
- **Advancements**: JSON under `assets/netherupdatebackport/advancements/nether/` with parent
  `minecraft:nether/root` (they appear in vanilla's Nether tab), triggered by `common/Triggers.fire(player,
  "<id>")` where 1.12.2 has no matching criterion.
- **Languages (Rule 4)**: every user-facing string is a key in `en_us.lang`, written by the generator; vanilla
  names through `assets.name(...)`; `@Config.LangKey` with a name and a `.tooltip` line; the other 17 languages
  come from the family's language tool after a stream merges.
- **Docs**: `docs/streams/<stream>.md` with a changelog section, how it works, tested, not tested.

## Build and test

- `./gradlew build` never deploys unless `nubDeployDir` is set in `~/.gradle/gradle.properties`.
- Backport Core: `deps/BackportCore-1.0.0-dev.jar`, plain `compile`. Until Core's `dependents` list has this
  repo (CORE_REQUESTS), copy the jar from `BackportCore/build/libs` by hand.
- `./gradlew runServer -Pselftest=<stream>`; then `python tools/check_assets.py` with 0 errors; `./gradlew
  build` last, and check `func_` names in the jar before it leaves the workspace.
- Free RAM checked before every Gradle run (8 GB minimum); `-Xmx2G`; never `cleanRunServer`; kill only our
  own server, by PID.
- No mixins, coremods or ASM. Client-only types only in `client/` classes.

## Order

1. Plan and scaffold: **done** (`aeee11e`).
2. Blocks first: S, U, F, L, N: **done**, each merged into `main` (see `docs/streams/`).
3. Mobs: H and R **done**; then P (piglins and brutes: 1.16.5's `PiglinTasks` brain, bartering, gold anger,
   hunting, celebrating), Z. Then M and C.
4. Fm once the blocks exist (they do; needs a dev game with Future MC for `/futuremc dump`).
5. V (Proposal A, decided) after the mobs; X after the user's answer on bastions and fortresses, and once Core has
   the template loader.

### 4. Music file size: decided 2026-11-07

Vanilla's own music files ship unchanged, at full quality (the user: "Keep full quality"). Rubedo, Chrysopoeia and
So Below are about 16 MB each, so the jar is about 95 MB. Never re-encode them to save space.
