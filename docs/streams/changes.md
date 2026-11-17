# Stream C: smaller changes

Package `changes` (client part in `changes/client`). Config `changes` (`enabled`, `hoes`, `lavaFog`,
`knockbackResistance`, `mending`, `portalSearch`, `lavaPushes`, `redstoneDot`, `fishing`, `totemFireResistance`,
`blockSounds`, and the audit's low-impact set: `witherSoulSoil`, `magmaPathing`, `dispensers`, `endermanBlocks`,
`hoeEnchanting`, `compassVanishing`, `spawnCosts`, `caveMood`, `parrotImitation`, `snowGolemWater`, `undeadHorseLeash`,
`pistonHardness`, `wallsPanes`, `furnaceXp`, `vineClimbing`, `burningProjectilesTnt`, `armorStandPlacement`,
`endPlatform`, `suffocation`, `ghastSoundRange`, `campfireDropsFood`, `witherRoseSoulSoil`). One block
(`redstone_dot`, no item); no entities or packets. Patches P5 to P25 (`docs/PATCHES.md`).

## Changelog section

*   **Hoes are a real tool**: they break leaves, hay bales, nether and warped wart blocks, sponges, dried kelp
    blocks, targets and shroomlights faster (wood 2x, stone 4x, iron 6x, diamond 8x, gold 12x, netherite faster
    still, Efficiency on top). Like other tools, a hoe now wears 1 durability for each block it breaks that is
    not instant, and 2 for each hit.
*   **Fire Resistance lets you see farther in lava**: 5 blocks instead of 1. Lava's fog is the newer, sharper kind
    for everyone, and spectators see through it.
*   **Knockback resistance** takes its share off every knockback (netherite armor 10% a piece, hoglins 60%),
    instead of being a chance to take none.
*   **Mending** repairs only damaged items that have it; XP no longer goes to an undamaged one.
*   **Nether portals** link to a portal within 16 blocks on the nether side (the overworld side still looks 128
    blocks out), so portals built close together in the overworld no longer share one in the nether.
*   **Flowing lava pushes** mobs, players and items, faster in the nether, as flowing water does.
*   **Redstone dust**: lone dust is a cross that powers all four sides; use it to make a dot that powers only the
    block below, and again for a cross. Dust powers every side it points into, corners and junctions too.
*   **Fishing**: treasure only in open water (5x4x5 of water or air around the bobber); lily pads are junk now.
*   **The totem of undying** also gives Fire Resistance for 40 seconds.
*   **Sounds**: netherrack, nether bricks, quartz ore, bone blocks, soul sand, nether wart and nether wart blocks
    sound as in 1.16.
*   **Withers** can be built on soul soil (or soul soil and soul sand mixed); **wither roses** can be planted on
    soul soil (with Village & Pillage Backport).
*   **Mobs avoid magma blocks** when finding their way, as they avoid fire.
*   **Dispensers** put saddles on pigs, striders and tamed horses, donkeys and mules, horse armor on tamed horses,
    carpets on tamed llamas and chests on tamed llamas, donkeys and mules, and shear mooshrooms and snow golems.
*   **Endermen** pick up crimson and warped nylium, fungi and roots, and no longer netherrack.
*   **Hoes** can be enchanted with Efficiency, Fortune and Silk Touch, at the enchanting table and the anvil.
*   **Compasses** and lodestone compasses take Curse of Vanishing.
*   **Spawn costs**: soul sand valleys and warped forests keep their mobs from crowding together.
*   **The cave sound** plays after you have spent long enough in the dark, not at random.
*   **Parrots** imitate nearby mobs far less often, on their own and on your shoulder.
*   **Snow golems** are hurt by splash and lingering water bottles.
*   **Skeleton and zombie horses** can be put on a lead.
*   **Pistons** are harder (1.5) and a pickaxe breaks them faster.
*   **Walls** connect to glass panes and iron bars, and they to walls.
*   **Breaking a furnace** gives the experience for what is in its output.
*   **Climbing** vines, weeping and twisting vines and scaffolding makes step sounds, and vines have their 1.16
    climbing sound.
*   **Any burning projectile** lights TNT it hits.
*   **Armor stands** placed in tall grass or snow leave it where it is.
*   **The End's obsidian platform** is rebuilt when any entity comes through a portal; players arrive in its
    middle.
*   **Farmland and grass paths** suffocate.
*   **Ghasts** are heard from 80 blocks, not 160.
*   **Soul campfires** throw off their food when put out (not with Caves & Cliffs Backport, as from 1.17).
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

*   **Behavior changes** (patches P5 to P11, hooks `ChangeHooks`): knockback, Mending, portal search, lava
    pushing, the totem and redstone are described in `docs/PATCHES.md`. Each setting is read on every call.
*   **Redstone dot** (`RedstoneDot`): 1.12.2's wire uses all 16 metadata values for power, so the dot is a block of
    its own (`BlockRedstoneWire`, drops and picks as redstone, tinted by power, vanilla's dot model). It exists only
    while unconnected: using it (1.16's `use`: a player who may build, not sneaking with an item), anything
    connecting to it, or the setting being off turns it back into `minecraft:redstone_wire`. Registered whenever the
    stream is on, so a world keeps its dots if only `redstoneDot` is switched off.
*   **Fishing** (`Fishing`): each server tick, after the hooks' own tick, 1.16.5's `openWater` rule for every
    bobbing hook (`ticksCatchable` and `ticksCaughtDelay` are 1.16's `nibble` and `timeUntilHooked`). Loot tables
    at load: `gameplay/fishing/treasure` loses `minecraft:waterlily`, `gameplay/fishing/junk` gets a lily pad at
    weight 17, and `gameplay/fishing`'s treasure entry is rebuilt with the same weight and quality plus the
    `netherupdatebackport:in_open_water` condition (registered, so a world's own loot table can use it too).
*   **Sounds** (`BlockSounds`): the sound types are set on vanilla's blocks at init. 1.12.2's stone slab is one block
    for every kind, so the nether brick slab keeps stone's sounds; 1.12.2's seeds play no planting sound, so nether
    wart's 1.16 planting sound is not heard (its break sound is). Assets: `tools/changes/generate.py`.

## Tested (`-Pselftest=mechanics`)

*   Knockback with resistance 0, 0.5, 0.6 and 1: x motion -0.4, -0.2, -0.16 and 0, every time; the lift scaled too.
*   Mending: 20 orbs with an undamaged and a damaged Mending item worn all repair the damaged one; with none
    damaged the XP goes to the player.
*   Portal search: 16 in the nether, 128 in the overworld; through `placeInExistingPortal`, a nether portal 20
    blocks off is not linked to, one 12 off is.
*   Lava: a still pig in flowing lava is pushed downstream by 0.0045, a moving one by 0.0023; the entity tick does
    it (P8); 0.007 in the nether; a squid is not pushed.
*   Totem: a zombie holding one survives 100 damage with Fire Resistance 800 ticks and Absorption.
*   Riders: a zombie 200 blocks from the nearest player despawns; one in a boat does not, until it gets out.
*   Redstone: lone dust drawn as a cross powers four sides and below, never above; used, a dot (drawn as one)
    powers only below and picks as redstone; used again, a cross; a dot that gets connected is vanilla's wire; a
    connected wire does not toggle; a line end powers what it points into, not its sides; a corner powers both
    blocks it points into; a redstone block lights a lamp through two wires, which go dark without it.
*   Fishing: tables as above; open water with water and a lily pad, not with a stone in the 5x5 or two above; a
    bobber's flag through a bite, staying not open once broken, open again for the next wait; the condition.
*   Sounds: each of the ten blocks' break sound (and netherrack's step).

## Not tested

*   The fog on screen (client only; needs a game with lava and the effect). Both branches' numbers are tested.
*   Hoe mining speed felt in a client (the same event runs on both sides; the server's value is tested).
*   Other mods' hoes with their own rules, and other mods' leaves.
*   In a client: lava pushing a player (the hook runs on both sides; tested on the server), the cross and dot
    models and the dot's tint, the block sounds heard.
*   Other coremods that change the patched methods (P5 to P11), in a real instance.

## The audit's low-impact set (`-Pselftest=smallchanges`)

Patches P12 to P25 (hooks in `SmallHooks`), and through events `SmallChanges` (dispensers, enderman blocks, pistons,
the vine's sound type, burning projectiles and TNT, the End platform), `SpawnCosts` (1.16.5's `MobDensityTracker`
on `LivingSpawnEvent.CheckSpawn` for natural spawns, charges gathered once a tick from the loaded mobs that may
despawn) and `client.CaveMood` (1.16's `LEGACY_CAVE_SETTINGS` mood outside the nether biomes; 1.12.2's random cave
sound is held off through `WorldClient.ambienceTicks`). Other streams' parts: the soul campfire's food
(`soul.campfire.TileEntitySoulCampfire.dowse`), soul soil's wither rose (`soul.SoulSoil`), and stream Z's game rules
and wither skeletons (`docs/streams/mobs.md`).

Tested: every patch applied; withers matched on soul soil, mixed and soul sand, not soul soil with the setting off;
a node by magma underfoot is a fire danger; a dispenser saddling a pig (and dropping the saddle with nothing there),
armoring and saddling a tamed horse (not an untamed one), a chest on a tamed donkey, a carpet on a tamed llama,
shearing a mooshroom into a cow and a snow golem's pumpkin off; the enderman's set; hoes as diggers, 10 and 22
enchantability, the table offering Efficiency, Fortune, Silk Touch and Unbreaking; the curse on compasses only; a
skeleton next to a soul sand valley skeleton refused (0.49 over 0.15), 10 blocks off allowed, a zombified piglin
free; the cave mood arithmetic; soul soil holding a wither rose (by name) and not other flowers; parrot sounds
counted over 80000 ticks (1 in 800) and a shoulder over 40000 (1 in 200); a water bottle taking 1 from a snow golem;
skeleton and zombie horses leashable; piston hardness, blast resistance and tool; a wall joining a pane and bars and
the pane joining back; 7 XP from 10 iron ingots in a broken furnace; vine climbing and its sound; a burning snowball
lighting TNT, an unburning one not; an armor stand leaving tall grass; a pig going to the End rebuilding the
platform; farmland, grass path and soul sand suffocating; a ghast's volume 5. Each setting's off switch where the
change is read live.

Not tested: the cave mood in a client (only its arithmetic), sounds heard, real pathfinding around magma (the node
type is tested), a player's own arrival in the End (the 1.16.2 centering), and the wither rose with Village &
Pillage Backport installed.

## For the integrator

*   `en_us.lang`: the `# --- config ---` block was regenerated with `tools/config_lang.py` (4 new lines for
    `changes.hoes` and `changes.lavaFog`); rerun it after merging other streams' config changes.
*   One line each in `NetherUpdateBackport.init`, `ClientSetup.init` and `SelfTestRunner.TESTS`.
*   The low-impact set adds `asm/SmallChangesTransformer` (in `NetherUpdateCore`), 24 config keys in all 19
    languages (inserted by hand after `changes.blockSounds` and `mobs.ridersStay`), and `block.vine.step` in
    `sounds.json` (`tools/changes/generate.py`).
