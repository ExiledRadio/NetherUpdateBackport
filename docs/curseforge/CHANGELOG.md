# 1.1.0

Behavior changes from 1.16 to things 1.12.2 already has, and fixes. Each change can be turned off in the Smaller Changes and Mob Changes settings.

*   **Knockback resistance takes its share off every knockback** (netherite armor 10% a piece, hoglins 60%) instead of being a chance to take none.
*   **Mending repairs only damaged items**: XP no longer goes to an undamaged item.
*   **Nether portals link to a portal within 16 blocks on the Nether side** (the overworld side still looks 128 blocks out), so portals built close together in the overworld no longer share one in the Nether.
*   **Flowing lava pushes** mobs, players and items, as flowing water does, faster in the Nether.
*   **Redstone dust**: lone dust is a cross that powers all four sides; use it to make a dot that powers only the block below, and again for a cross. Dust powers every side it points into, corners and junctions too.
*   **Fishing**: treasure only in open water (5x4x5 of water or air around the bobber); lily pads are junk now.
*   **The totem of undying** also gives Fire Resistance for 40 seconds.
*   **Neutral mobs hold a grudge, and forgive**: wolves, endermen, polar bears and iron golems stay angry at whoever hurt them for 20 to 39 seconds after losing them, and the anger is saved with them. A player who dies is forgiven by every one of them nearby, Buzzy Bees Backport's bees included.
*   **Game rules `forgiveDeadPlayers` and `universalAnger`**, on and off by default: with `forgiveDeadPlayers` off, mobs stay angry at a player who died; with `universalAnger` on, a neutral mob, zombified piglin or piglin a player hits is angry at every player nearby.
*   **Mobs riding** a boat, a minecart or another mob never despawn.
*   **Wither skeletons attack piglins** and piglin brutes.
*   **Block sounds**: netherrack, nether bricks, quartz ore, bone blocks, soul sand, nether wart and nether wart blocks sound as in 1.16.
*   **Smaller 1.16 changes**:
    *   Withers can be built on soul soil; wither roses can be planted on it (with Village & Pillage Backport).
    *   Mobs avoid magma blocks when finding their way.
    *   Dispensers put saddles, horse armor, carpets and chests on pigs, striders and tamed horses, donkeys, mules and llamas, and shear mooshrooms and snow golems.
    *   Endermen pick up crimson and warped nylium, fungi and roots, and no longer netherrack.
    *   Hoes take Efficiency, Fortune and Silk Touch at the enchanting table and anvil; compasses take Curse of Vanishing.
    *   Soul sand valleys and warped forests keep their mobs from crowding together.
    *   The cave sound plays after you have spent long enough in the dark, not at random.
    *   Parrots imitate nearby mobs far less often.
    *   Splash and lingering water bottles hurt snow golems.
    *   Skeleton and zombie horses can be put on a lead.
    *   Pistons are harder and a pickaxe breaks them faster.
    *   Walls connect to glass panes and iron bars.
    *   Breaking a furnace gives the experience for what is in its output.
    *   Climbing vines, weeping and twisting vines and scaffolding makes step sounds.
    *   Any burning projectile lights TNT.
    *   Armor stands placed in tall grass or snow leave it where it is.
    *   The End's obsidian platform is rebuilt when any entity comes through, and players arrive in its middle.
    *   Farmland and grass paths suffocate.
    *   Ghasts are heard from 80 blocks, not 160.
    *   Soul campfires throw off their food when put out (not with Caves & Cliffs Backport, as from 1.17).
    *   The strider's saddle sound is quieter, as in current Java.
*   **Superflat worlds get no ruined portals**, with or without decoration, as no superflat preset lists them. The Nether keeps them.
*   **Ruined portals in Caves & Cliffs Backport's 1.18 worlds** start where 1.18 puts them, and underground ones go down to y -49.
*   **Backport Core's Look & Feel switch** turned off gives 1.12.2's look: Singleplayer no longer opens Create New World when there are no worlds, and Entity Distance, Distortion Effects and FOV Effects do nothing while hidden.
*   **Fixed crimson and warped wood recipes taking over other mods' recipes**: grids of only vanilla planks made the vanilla bookshelf or chest instead of another mod's per-wood one. They now need crimson or warped wood in the grid.
*   **Fixed two chains with Trails & Tales Backport** when it loaded first: two chain items and two recipes.

Needs Backport Core 1.3.0 or newer.

# 1.0.0

First release: all of 1.16 Nether Update on 1.12.2. Needs Backport Core 1.1.0 or newer. Fluidlogged API comes through Backport Core and only adds waterlogging.

*   **Four new nether biomes**: crimson forest, warped forest, soul sand valley and basalt deltas; the old nether becomes the nether wastes. Laid out as 1.16 lays them out for the seed. Only newly explored nether chunks get them.
*   **Piglins** with bartering, gold anger, hunting and soul-fire fear; **piglin brutes** in bastions; **hoglins** and **zoglins**; **striders** you can ride with a warped fungus on a stick.
*   **Zombified piglins** replace zombie pigmen, with 1.16's model, sounds and anger rules. Existing worlds and farms keep working.
*   **Bastion remnants** (all four kinds), **ruined portals** in every variant, **nether fossils**. Bastions and fortresses share spots as in 1.16. All found by `/locate structure`.
*   **Netherite**: ancient debris, scrap, ingots, the block, and tools and armor upgraded at the smithing table. Netherite drops never burn and float up through lava. Without Village & Pillage, this mod brings its own smithing table and crossbow.
*   **Blackstone** and its polished, brick, cracked, chiseled and gilded forms; **basalt** and polished basalt, with the basalt generator.
*   **Crimson and warped** nylium, fungi (bone meal grows huge fungi), roots, sprouts, weeping and twisting vines, wart block, shroomlight, and every wood block.
*   **Soul fire**, soul soil, soul torch, soul lantern, soul campfire, and **Soul Speed**.
*   **Chain**, **crying obsidian**, **respawn anchor**, **lodestone** and the lodestone compass, **target**, **nether gold ore**, quartz bricks, cracked and chiseled nether bricks.
*   **Pigstep** in bastion chests; 1.16's nether music and per-biome ambience; music finishes instead of cutting off.
*   Hoes mine leaves, hay and wart blocks faster. Fire Resistance lets you see 5 blocks into lava.
*   The Snout banner pattern, every 1.16 advancement for the content above, and waterlogging where 1.16 has it.
*   1.16's look and feel through Backport Core: the nether panorama, splashes, logo, Mojang Studios loading screen, and the new options.
*   Seed maps set to 1.16 show the nether's biomes and structures where they really are.
*   Worlds that used another mod's chains, soul torches, soul lanterns, soul soil, ancient debris, netherite and Pigstep keep them, converted to this mod's.
*   Every setting is on Backport Core's settings screen, under Mod Options.
*   Eighteen languages.
