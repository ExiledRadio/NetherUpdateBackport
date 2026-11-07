# Patches

The family's standing rule (2026-11-07): closest to vanilla, always; bytecode patching is allowed where vanilla
parity needs it, through this mod's own small ASM coremod (package `asm`, one transformer per patch, each listed
here), no MixinBooter dependency, compatible with Fluidlogged API's transformers.

## Applied

None yet.

## Candidates: approximations made without patches that a patch would make vanilla

| Where | What vanilla 1.16 does | What this mod does now | Patch |
| --- | --- | --- | --- |
| L, Soul Speed | Not in random enchanted books or librarian trades (`isDiscoverable` false) | Can appear in 1.12.2's random enchanted loot and the vanilla librarian | `EnchantRandomly` and `ListEnchantedBookForEmeralds`: skip `soul_speed` |
| N, fireproof items | Silent in lava | 1.12.2 plays its fizz when the item enters a lava block | `EntityItem.onUpdate`: no burn sound for fire-immune items |
| R, striders | Stand on lava (`canStandOnFluid`) | Held on the surface after each move, so they can jitter at block edges and path through lava nodes | `Entity.move` collision: lava's top half solid for striders |
| L, soul sand | Soul Speed sets soul sand's speed factor to 1 | Divides soul sand's 0.4 back out the next tick | `BlockSoulSand.onEntityCollision`: skip for Soul Speed wearers |
| V, spawning | Soul sand valleys and warped forests limit their mobs with spawn costs (`MobSpawnInfo.addMobCharge`: energy budget 0.15 / 0.12, charge 0.7 / 1.0) | Plain spawn lists: those biomes can hold denser groups than 1.16's | `WorldEntitySpawner.findChunksForSpawning`: 1.16's `PotentialCalculator` per spawn attempt |

Each is worth doing when its stream is next touched; none blocks a stream.
