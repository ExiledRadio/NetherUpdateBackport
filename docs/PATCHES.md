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

### P3. Distortion Effects: the portal and nausea wobble

*   **Class**: `net.minecraft.client.renderer.EntityRenderer`, method `setupCameraTransform(FI)V` (`func_78479_a`).
    **Transformer**: `asm/LookTransformer`. **Hook**: `lookfeel/LookHooks.portalWobble`. **Stream**: Lf.
*   **Vanilla 1.16.2+**: `GameRenderer.renderLevel` multiplies the wobble strength
    (`lerp(partialTicks, oPortalTime, portalTime)`) by `screenEffectScale` twice before it rotates and stretches
    the view.
*   **Why a patch**: the strength is a local of `setupCameraTransform`, used right after it is computed; Forge's
    `CameraSetup` event fires later (in `orientCamera`), after the wobble's rotations are on the matrix stack.
*   **What changes**: the value of `prevTimeInPortal + (timeInPortal - prevTimeInPortal) * partialTicks` goes through
    `LookHooks.portalWobble(f)` before it is stored. No jump, no new local. At 100% (the default) the hook returns
    the value unchanged. The green nausea overlay 1.16.2 shows instead of the lost wobble is drawn on
    `RenderGameOverlayEvent.Pre(ALL)`, without a patch.
*   **Fails safe**: if the method is missing, `prevTimeInPortal` is not read exactly twice, or no `FSTORE` ends the
    expression before another call or label, nothing is patched and `P3 ... patch skipped` is logged; the wobble
    stays 1.12.2's at any setting.
*   **Tested** (`-Pselftest=lookfeel`): the patch put through the dev jar's MCP-named class and Forge's SRG-named
    `forge-1.12.2-14.23.5.2847-srgBin.jar` adds exactly one hook call, inside `setupCameraTransform`, and every
    method passes ASM's `BasicVerifier`; the hook scales by the square (50% gives a quarter). `runClient
    -Pselftest=client`: applied when the client loads the class.

### P4. Entity Distance

*   **Class**: `net.minecraft.client.renderer.RenderGlobal`, method `setupTerrain` (`func_174970_a`). **Transformer**:
    `asm/LookTransformer`. **Hook**: `lookfeel/LookHooks.entityDistance`. **Stream**: Lf.
*   **Vanilla 1.16**: `LevelRenderer.renderLevel` sets `Entity.setViewScale(clamp(renderDistance / 8, 1, 2.5) *
    entityDistanceScaling)`.
*   **Why a patch**: 1.12.2 sets the same weight (`Entity.setRenderDistanceWeight`, a private static) in
    `setupTerrain` every frame and reads it while culling entities in the same frame; no event runs in between.
*   **What changes**: the argument of the method's one `Entity.setRenderDistanceWeight` call goes through
    `LookHooks.entityDistance(w)`. At 100% (the default) it is unchanged.
*   **Fails safe**: method missing, or not exactly one such call: skipped (`P4 ... patch skipped`); entities are
    drawn at 1.12.2's distance.
*   **Tested**: as P3 (dev and SRG classes, one hook call in `setupTerrain`, `BasicVerifier`; 200% doubles the
    weight; applied in the dev client).

### P5 to P11. Behavior changes to existing things (the 2026-11-16 audit)

One transformer, `asm/MechanicsTransformer`; every hook in `changes/ChangeHooks`, each reading its own setting on
every call (`changes.*`, `mobs.ridersStay`), so a change switched off in the settings screen runs vanilla's code at
once. Each patch fails safe on its own: a method missing or of another shape is left alone and `P<n> ... patch
skipped` is logged. **Tested** (`-Pselftest=mechanics`): each applied, and the behavior below, through vanilla's own
entry points (`knockBack`, `onCollideWithPlayer`, `placeInExistingPortal`, `onEntityUpdate`, `attackEntityFrom`,
`despawnEntity`, `getWeakPower`/`getActualState`). Not yet checked against other coremods in a real instance.

*   **P5. Knockback resistance scales knockback.** `EntityLivingBase.knockBack(Entity, float, double, double)`
    (`func_70653_a`): head guard `if (ChangeHooks.knockbackActive()) { ChangeHooks.knockBack(...); return; }`.
    *Vanilla 1.16.5* `LivingEntity.knockback`: strength times `1 - knockback resistance`, then the same push.
    *Why a patch*: 1.12.2 rolls the resistance as a chance (`rand.nextDouble() >= resistance`) after Forge's
    `LivingKnockBackEvent`, inside the method; no event comes after the roll. The hook fires Forge's event first,
    as vanilla does, so other mods' changes to the strength still apply. Netherite's 0.1 per piece and hoglins'
    0.6 now take that share off every hit.
*   **P6. Mending only takes damaged items.** `EntityXPOrb.onCollideWithPlayer` (`func_70100_b_`): its one
    `EnchantmentHelper.getEnchantedItem(Enchantment, EntityLivingBase)` call is renamed to
    `ChangeHooks.mendingItem`, same descriptor. *Vanilla 1.16.5*: `getRandomItemWith(MENDING, player,
    ItemStack::isDamaged)`. *1.12.2* picks any Mending item; an undamaged pick sends the whole orb to the XP bar
    while a damaged Mending item stays damaged. The repair amounts stay Forge 1.12.2's (`getXpRepairRatio`).
*   **P7. The nether side searches 16 blocks for a portal.** `Teleporter.placeInExistingPortal` (`func_180620_b`):
    its three `sipush 128` and two `bipush -128` (the search square) become `ChangeHooks.portalRadius(this.world)`
    (negated for the -128s). *Vanilla 1.16.5* `findPortalAround`: 16 when the destination is the nether, else 128.
    *Why*: the radius is a constant in the loop bounds.
*   **P8. Flowing lava pushes.** `Entity.onEntityUpdate` (`func_70030_z`): right after its `handleWaterMovement()`
    (and the `pop` of its result), `ChangeHooks.lavaPush(this)`. *Vanilla 1.16.5*
    `updateFluidHeightAndDoFluidPushing(LAVA, 0.007 in an ultrawarm dimension, else 0.0023)`: the flow of each lava
    block the box is in (scaled by depth under 0.4), averaged, normalized except for players, at least 0.0045 for a
    still entity. Entities `isPushedByWater` says no to (squid, fish, flying players) are not pushed, as 1.16's
    `isPushedByFluid`. Runs on both sides, as vanilla's, so a player's own movement sees it.
*   **P9. The totem gives Fire Resistance.** `EntityLivingBase.checkTotemDeathProtection` (`func_190628_d`): after
    its one `World.setEntityState` (the totem has worked), `ChangeHooks.totemUsed(this)`: Fire Resistance 800 ticks
    (1.16.2, 20w28a).
*   **P10. Riders do not despawn.** `EntityLiving.despawnEntity` (`func_70623_bb`): head
    `if (ChangeHooks.keepRider(this)) return;`. *Vanilla 1.16.5* `MobEntity.requiresCustomPersistence` is
    `isPassenger()`, which skips `checkDespawn`. *Why a patch*: Forge's `AllowDespawn` runs only every 32nd tick in
    1.12.2; the instant despawn beyond 128 blocks runs in between. Any vehicle counts, as in 1.16's code (boats,
    minecarts, another mob).
*   **P11. Redstone dust's dot and cross.** `BlockRedstoneWire.getWeakPower` (`func_180656_a`): head guard returning
    `ChangeHooks.weakPower(this, state, world, pos, side)`; `getActualState` (`func_176221_a`): its one result goes
    through `ChangeHooks.actualState`. *Vanilla 1.16.5* `getSignal`: up and below as before, a side only when the
    wire's connection state (a lone connection drawn as a line, none as a cross) points into it; so corners and
    T-junctions now power what they point into, as 1.16 does. An unconnected wire is drawn (and boxed) as a cross.
    The dot itself is a block, `netherupdatebackport:redstone_dot` (1.12.2's wire has no metadata to spare), which
    exists only while unconnected (`changes/RedstoneDot`). While any wire works out its own power, no wire of either
    block gives power, as vanilla's single instance does. Lines, line ends, lone dust and lamps behave as in 1.12.2.

### P12 to P25. The audit's low-impact behavior changes

A second transformer, `asm/SmallChangesTransformer`; every hook in `changes/SmallHooks`, each reading its own
`changes.*` setting (P24: the `universalAnger` game rule) on every call. Each patch is skipped with a warning when
its method is not of the expected shape; `-Pselftest=smallchanges` checks each is applied.

*   **P12. Withers on soul soil.** `BlockSkull.getWitherBasePattern`/`getWitherPattern` (`func_176414_j`,
    `func_176416_l`): the `BlockStateMatcher.forBlock(Blocks.SOUL_SAND)` call right after `getstatic SOUL_SAND`
    becomes `SmallHooks.witherBase(Block)`, a predicate for soul sand or soul soil. *Vanilla 1.16.5*:
    `BlockTags.WITHER_SUMMON_BASE_BLOCKS`. *Why*: the patterns are built once into private fields.
*   **P13. Mobs avoid magma.** `WalkNodeProcessor.checkNeighborBlocks` (`func_193578_a`): its result passes through
    `SmallHooks.neighbors`: a walkable node with a magma block anywhere in the 3x3x3 around it (not its own column)
    is `DANGER_FIRE`. *Vanilla 1.16.5* `checkNeighbourBlocks` with `isBurningBlock`. Lava there is 1.15's (Buzzy
    Bees Backport's audit item) and left alone.
*   **P14. Hoe enchantments.** The `EnumEnchantmentType$N` whose `canEnchantItem` tests `instanceof ItemTool`
    (DIGGER): head `if (SmallHooks.hoeDigger(item)) return true;`; and `ItemHoe` gets a `getItemEnchantability`
    (`func_77619_b`, named as its `onItemUse` shows the environment) returning its material's. *Vanilla 1.16.5*:
    `HoeItem extends ToolItem`, a digger with its tier's enchantability. Hoes take Efficiency, Fortune and Silk
    Touch at the table and the anvil (Unbreaking and Mending they took at the anvil already).
*   **P15. Compasses take Curse of Vanishing.** `Enchantment.canApply` (`func_92089_a`): head
    `if (SmallHooks.vanishingCompass(this, stack)) return true;` for the compass and the lodestone compass.
    *Vanilla 1.16.5*: `CompassItem implements IVanishable`.
*   **P16. Parrot imitation rate.** `EntityParrot.onLivingUpdate`: its `playMimicSound` call becomes
    `SmallHooks.parrotTick` (1 tick in 400, then 1.16's `imitateNearbyMobs`, itself 1 in 2); `playAmbientSound`
    (`func_192005_a`, a shoulder parrot): head guard running 1.16's `playShoulderEntityAmbientSound` (1 tick in 200,
    an imitation or its own call, no imitation on Peaceful). 1.12.2 imitates 1 tick in 50.
*   **P17. Water bottles hurt snow golems.** `EntityPotion.isWaterSensitiveEntity` (`func_190544_c`): head
    `if (SmallHooks.waterSensitive(entity)) return true;`. *Vanilla 1.16.5*: `SnowGolemEntity.isSensitiveToWater`.
*   **P18. Undead horses on a lead.** `AbstractHorse.canBeLeashedTo` (`func_184652_a`): head guard returning
    `!getLeashed()`. *Vanilla 1.16.5* has no undead exception (`MobEntity.canBeLeashed`).
*   **P19. Walls and panes.** `BlockWall.canConnectTo` (`func_176253_e`): head
    `if (SmallHooks.wallToPane(world, pos)) return true;` for any `BlockPane` (glass panes, iron bars). Panes ask the
    wall's `canBeConnectedTo`, which calls the same method, so they connect back. Backport Core's `ModernWall`
    already does both.
*   **P20. Furnace XP when broken.** `BlockFurnace.breakBlock`: before its `InventoryHelper.dropInventoryItems`
    (inside vanilla's `!keepInventory` branch), `SmallHooks.furnaceBroken(world, pos)`: the output slot's XP, by
    1.16's `createExperience`. 1.12.2 owes XP only for the output still in the slot (it pays when one is taken),
    which is what 1.16's `RecipesUsed` holds there.
*   **P21. Suffocation in farmland and grass paths.** `BlockFarmland`, `BlockGrassPath`: a new
    `causesSuffocation(IBlockState)` (`func_176214_u`, named as the class's `isFullCube` shows) returning
    `SmallHooks.suffocates()`. *Vanilla 1.16.5*: `isSuffocating(Blocks::always)` for the two and soul sand; 1.12.2's
    soul sand already suffocates (a full cube).
*   **P22. Ghast sound range.** `EntityGhast.getSoundVolume` (`func_70599_aP`): head guard returning 5 (80 blocks)
    instead of 10.
*   **P23. Armor stands in grass and snow.** `ItemArmorStand.onItemUse`: its two `World.setBlockToAir` calls become
    `SmallHooks.armorStandClear`, which leaves the block (20w07a: "placing an armor stand inside a replaceable block
    no longer removes it").
*   **P24. Universal anger: the hurt-by goal.** `EntityAIHurtByTarget.shouldExecute`: head
    `if (SmallHooks.ignoreHurtByPlayer(this.taskOwner)) return false;` while `universalAnger` is on and the attacker
    is a player. *Vanilla 1.16.5* `HurtByTargetGoal.canUse` (all mobs); `mobs.ResetAngerGoal` then takes the hit.
*   **P25. Climbing steps.** `Entity.move` (`func_70091_d`): `block != Blocks.LADDER` (`if_acmpeq`) becomes
    `SmallHooks.climbable(block, LADDER)` (`ifne`): vines, weeping and twisting vines and Village & Pillage's
    scaffolding count their vertical movement as steps, so climbing them plays their step sound. *Vanilla 1.16.5*:
    `BlockTags.CLIMBABLE`. Vines get 1.16's `SoundType.VINE` (their own climbing step) in `changes.SmallChanges`.

## Candidates: approximations made without patches that a patch would make vanilla

| Where | What vanilla 1.16 does | What this mod does now | Patch |
| --- | --- | --- | --- |
| L, Soul Speed | Not in random enchanted books or librarian trades (`isDiscoverable` false) | Can appear in 1.12.2's random enchanted loot and the vanilla librarian | `EnchantRandomly` and `ListEnchantedBookForEmeralds`: skip `soul_speed` |
| N, fireproof items | Silent in lava | 1.12.2 plays its fizz when the item enters a lava block | `EntityItem.onUpdate`: no burn sound for fire-immune items |
| R, striders | Stand on lava (`canStandOnFluid`) | Held on the surface after each move, so they can jitter at block edges and path through lava nodes | `Entity.move` collision: lava's top half solid for striders |
| Z, zombified piglins | A baby rides 0.05 lower (`getMyRidingOffset` -0.05; adults -0.45 as a zombie's) | 1.12.2's zombie offset (baby 0.0) | Add `getYOffset` to `EntityPigZombie` (a new method, so not a head guard) |
| L, soul sand | Soul Speed sets soul sand's speed factor to 1 | Divides soul sand's 0.4 back out the next tick | `BlockSoulSand.onEntityCollision`: skip for Soul Speed wearers |

Each is worth doing when its stream is next touched; none blocks a stream.
