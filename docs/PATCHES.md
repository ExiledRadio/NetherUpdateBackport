# Patches

The family's standing rule (2026-11-07): closest to vanilla, always; bytecode patching is allowed where vanilla
parity needs it, through this mod's own small ASM coremod (package `asm`, one transformer per patch, each listed
here), no MixinBooter dependency, compatible with Fluidlogged API's transformers.

The coremod is `asm/NetherUpdateCore`, named in the jar's manifest (`FMLCorePlugin`, `FMLCorePluginContainsFMLMod:
true`) and, for `runServer`/`runClient`, by `-Dfml.coreMods.load` (`build.gradle`). Sorting index 1001, so classes
arrive with SRG member names in a real instance and MCP names in dev; transformers match both. A transformer that
does not find the exact shape it expects changes nothing and logs a warning; it never half-patches a class. Patches
only add a guard at a method's head, so they never touch the instructions another transformer (Fluidlogged API's)
looks for.

## Applied

### P1. Zombified piglin anger and sounds

*   **Class**: `net.minecraft.entity.monster.EntityPigZombie`. **Transformer**: `asm/PigZombieTransformer`.
    **Hooks**: `mobs/PigmanHooks`. **Switch**: `mobs.enabled`. **Stream**: Z.
*   **Vanilla 1.16.5**: `ZombifiedPiglinEntity` is angry only at the entity it targets (`IAngerable`), for 20-39 s
    once out of sight; only that player cannot sleep near it; its ambient sound is the angry one while angry.
*   **Why a patch**: 1.12.2's anger lives in the pigman's own private `becomeAngryAt`, called from
    `attackEntityFrom` before any Forge event fires, and it angers the pigman at every player. Replacing the entity
    would break every world, spawner and mod that names `minecraft:zombie_pigman`; no event answers `isAngry`,
    the sleep check or the sound getters, and `PlaySoundAtEntityEvent` does not say which entity plays a sound.
*   **What changes**: six methods get `if (PigmanHooks.active()) { ... }` at their head, the rest untouched:
    `becomeAngryAt(Entity)` returns; `isAngry()` returns the stream's anger state (so vanilla's `updateAITasks`
    gives and takes its speed boost by it); `isPreventingPlayerRest(EntityPlayer)` returns "angry at this
    player"; `getAmbientSound()` the angry or ambient zombified piglin event; `getHurtSound` and `getDeathSound`
    the zombified piglin's. The targeting itself is not patched: it is 1.16's goals, swapped in at
    `EntityJoinWorldEvent` (`mobs/ZombifiedPiglins`).
*   **Fails safe**: if any of the six methods is missing or already starts on a stack-map frame, nothing is
    patched and `P1 ... patch skipped` is logged; the stream then logs that sounds, the speed boost and the sleep
    check stay 1.12.2's, while 1.16's targeting still applies. With `mobs.enabled` off, `active()` is false and
    every method runs as vanilla's.
*   **Other mods**: a mod that also changes these six methods keeps working as long as it does not need their
    first instruction. Not yet checked against other coremods in a real instance.
*   **Tested** (`-Pselftest=mobs`): applied and active; a hit starts no 1.12.2 anger; `isAngry()` follows the new
    state; only the hitter is kept from sleeping; the speed boost comes and goes with the anger (adults only);
    angry, ambient, hurt and death sounds are the new events.

### P2. Jukebox comparator signal for 1.16's discs

*   **Class**: `net.minecraft.block.BlockJukebox`. **Transformer**: `asm/JukeboxTransformer`. **Hooks**:
    `music/MusicHooks`. **Switch**: `music.enabled`. **Stream**: M.
*   **Vanilla 1.16.5**: each `MusicDiscItem` carries its own comparator signal (`analogOutput`); Pigstep's is 13.
*   **Why a patch**: 1.12.2's `getComparatorInputOverride` works the signal out from the item id
    (`id + 1 - id(record_13)`), which is right for vanilla's twelve discs only: a modded disc's id gives a number in
    the thousands, so a comparator reads 15. No Forge hook or event covers a vanilla block's comparator output.
*   **What changes**: one method gets, at its head, `if (MusicHooks.active()) { int s = MusicHooks.jukeboxSignal(world,
    pos); if (s >= 0) return s; }`. The hook answers only for this mod's discs (`ItemMusicDisc.comparatorSignal`) and
    gives -1 for every other disc and an empty jukebox, so vanilla's own code still answers those.
*   **Fails safe**: if the method is missing or starts on a stack-map frame, nothing is patched and `P2 ... patch
    skipped` is logged; Pigstep then gives what 1.12.2's formula gives (a comparator reads 15). With `music.enabled`
    off, `active()` is false.
*   **Other mods**: a mod that also changes this method keeps working as long as it does not need its first
    instruction. Not yet checked against other coremods in a real instance.
*   **Tested** (`-Pselftest=music`): applied; a jukebox with Pigstep gives 13 and a comparator beside it outputs 13;
    13, cat and wait still give 1, 2 and 12; an empty jukebox 0.

## Candidates: approximations made without patches that a patch would make vanilla

| Where | What vanilla 1.16 does | What this mod does now | Patch |
| --- | --- | --- | --- |
| L, Soul Speed | Not in random enchanted books or librarian trades (`isDiscoverable` false) | Can appear in 1.12.2's random enchanted loot and the vanilla librarian | `EnchantRandomly` and `ListEnchantedBookForEmeralds`: skip `soul_speed` |
| N, fireproof items | Silent in lava | 1.12.2 plays its fizz when the item enters a lava block | `EntityItem.onUpdate`: no burn sound for fire-immune items |
| R, striders | Stand on lava (`canStandOnFluid`) | Held on the surface after each move, so they can jitter at block edges and path through lava nodes | `Entity.move` collision: lava's top half solid for striders |
| Z, zombified piglins | A baby rides 0.05 lower (`getMyRidingOffset` -0.05; adults -0.45 as a zombie's) | 1.12.2's zombie offset (baby 0.0) | Add `getYOffset` to `EntityPigZombie` (a new method, so not a head guard) |
| L, soul sand | Soul Speed sets soul sand's speed factor to 1 | Divides soul sand's 0.4 back out the next tick | `BlockSoulSand.onEntityCollision`: skip for Soul Speed wearers |
| V, spawning | Soul sand valleys and warped forests limit their mobs with spawn costs (`MobSpawnInfo.addMobCharge`: energy budget 0.15 / 0.12, charge 0.7 / 1.0) | Plain spawn lists: those biomes can hold denser groups than 1.16's | `WorldEntitySpawner.findChunksForSpawning`: 1.16's `PotentialCalculator` per spawn attempt |

Each is worth doing when its stream is next touched; none blocks a stream.
