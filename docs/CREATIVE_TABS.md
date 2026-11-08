# Creative tabs

Nether Update Backport has no creative tab of its own. Each block and item is in the vanilla tab Minecraft 1.19.2 gives it, next to its vanilla neighbor in 1.19.2's order, through Backport Core's creative placement (`creative.CreativePlacement`; the rule and the table are in Backport Core's `docs/CREATIVE_TABS.md`). Items are placed by their vanilla name: `common/Content` places every block with an item and every item, and the main class's pre-init the spawn eggs. The crossbow copy's enchanted books are in Combat.

"After" and "Before" are the stacks either side of it with this mod and Backport Core alone, from a dev client's `-Dbackportcore.creativeDump` (an item is put after its nearest 1.19.2 neighbor before it, or before the one after it, whichever is of its kind). With other backports installed an item moves to stay by its 1.19.2 neighbor: stripped logs follow the warped stem once the Nether Update adds it, and so on.

## Building Blocks

| Item | After | Before |
|---|---|---|
| `crimson_nylium` | `podzol` | `warped_nylium` (this mod) |
| `warped_nylium` | `crimson_nylium` (this mod) | `cobblestone` |
| `crimson_planks` | `dark_oak_planks` | `warped_planks` (this mod) |
| `warped_planks` | `crimson_planks` (this mod) | `bedrock` |
| `netherite_block` | `diamond_block` | `cobblestone_stairs` |
| `soul_soil` | `soul_sand` | `basalt` (this mod) |
| `basalt` | `soul_soil` (this mod) | `polished_basalt` (this mod) |
| `polished_basalt` | `basalt` (this mod) | `glowstone` |
| `cracked_nether_bricks` | `nether_bricks` | `chiseled_nether_bricks` (this mod) |
| `chiseled_nether_bricks` | `cracked_nether_bricks` (this mod) | `nether_brick_stairs` |
| `crimson_slab` | `dark_oak_slab` | `warped_slab` (this mod) |
| `warped_slab` | `crimson_slab` (this mod) | `sandstone_stairs` |
| `nether_gold_ore` | `jungle_stairs` | `nether_quartz_ore` |
| `ancient_debris` | `nether_quartz_ore` | `quartz_block` |
| `quartz_bricks` | `quartz_block` | `chiseled_quartz_block` |
| `crimson_stem` | `dark_oak_log` | `warped_stem` (this mod) |
| `warped_stem` | `crimson_stem` (this mod) | `stripped_crimson_stem` (this mod) |
| `stripped_crimson_stem` | `warped_stem` (this mod) | `stripped_warped_stem` (this mod) |
| `stripped_warped_stem` | `stripped_crimson_stem` (this mod) | `stripped_crimson_hyphae` (this mod) |
| `stripped_crimson_hyphae` | `stripped_warped_stem` (this mod) | `stripped_warped_hyphae` (this mod) |
| `stripped_warped_hyphae` | `stripped_crimson_hyphae` (this mod) | `crimson_hyphae` (this mod) |
| `crimson_hyphae` | `stripped_warped_hyphae` (this mod) | `warped_hyphae` (this mod) |
| `warped_hyphae` | `crimson_hyphae` (this mod) | `acacia_stairs` |
| `crimson_stairs` | `dark_oak_stairs` | `warped_stairs` (this mod) |
| `warped_stairs` | `crimson_stairs` (this mod) | `prismarine` |
| `warped_wart_block` | `nether_wart_block` | `red_nether_bricks` |
| `mossy_stone_brick_stairs` | `black_concrete_powder` | `mossy_stone_brick_slab` (this mod) |
| `mossy_stone_brick_slab` | `mossy_stone_brick_stairs` (this mod) | `smooth_quartz_slab` (this mod) |
| `smooth_quartz_slab` | `mossy_stone_brick_slab` (this mod) | `crying_obsidian` (this mod) |
| `crying_obsidian` | `smooth_quartz_slab` (this mod) | `blackstone` (this mod) |
| `blackstone` | `crying_obsidian` (this mod) | `blackstone_slab` (this mod) |
| `blackstone_slab` | `blackstone` (this mod) | `blackstone_stairs` (this mod) |
| `blackstone_stairs` | `blackstone_slab` (this mod) | `gilded_blackstone` (this mod) |
| `gilded_blackstone` | `blackstone_stairs` (this mod) | `polished_blackstone` (this mod) |
| `polished_blackstone` | `gilded_blackstone` (this mod) | `polished_blackstone_slab` (this mod) |
| `polished_blackstone_slab` | `polished_blackstone` (this mod) | `polished_blackstone_stairs` (this mod) |
| `polished_blackstone_stairs` | `polished_blackstone_slab` (this mod) | `chiseled_polished_blackstone` (this mod) |
| `chiseled_polished_blackstone` | `polished_blackstone_stairs` (this mod) | `polished_blackstone_bricks` (this mod) |
| `polished_blackstone_bricks` | `chiseled_polished_blackstone` (this mod) | `polished_blackstone_brick_slab` (this mod) |
| `polished_blackstone_brick_slab` | `polished_blackstone_bricks` (this mod) | `polished_blackstone_brick_stairs` (this mod) |
| `polished_blackstone_brick_stairs` | `polished_blackstone_brick_slab` (this mod) | `cracked_polished_blackstone_bricks` (this mod) |
| `cracked_polished_blackstone_bricks` | `polished_blackstone_brick_stairs` (this mod) | (end of the tab) |

## Decoration Blocks

| Item | After | Before |
|---|---|---|
| `crimson_fungus` | `red_mushroom` | `warped_fungus` (this mod) |
| `warped_fungus` | `crimson_fungus` (this mod) | `crimson_roots` (this mod) |
| `crimson_roots` | `warped_fungus` (this mod) | `warped_roots` (this mod) |
| `warped_roots` | `crimson_roots` (this mod) | `nether_sprouts` (this mod) |
| `nether_sprouts` | `warped_roots` (this mod) | `weeping_vines` (this mod) |
| `weeping_vines` | `nether_sprouts` (this mod) | `twisting_vines` (this mod) |
| `twisting_vines` | `weeping_vines` (this mod) | `torch` |
| `soul_torch` | `oak_fence` | `infested_stone` |
| `chain` | `iron_bars` | `glass_pane` |
| `mossy_stone_brick_wall` | `mossy_cobblestone_wall` | `stone_brick_wall` (this mod) |
| `stone_brick_wall` | `mossy_stone_brick_wall` (this mod) | `blackstone_wall` (this mod) |
| `blackstone_wall` | `stone_brick_wall` (this mod) | `polished_blackstone_wall` (this mod) |
| `polished_blackstone_wall` | `blackstone_wall` (this mod) | `polished_blackstone_brick_wall` (this mod) |
| `polished_blackstone_brick_wall` | `polished_blackstone_wall` (this mod) | `anvil` |
| `crimson_fence` | `dark_oak_fence` | `warped_fence` (this mod) |
| `warped_fence` | `crimson_fence` (this mod) | `acacia_fence` |
| `crimson_sign` | `oak_sign` | `warped_sign` (this mod) |
| `warped_sign` | `crimson_sign` (this mod) | `white_bed` |
| `smithing_table` | `end_crystal` | `lantern` (this mod) |
| `lantern` | `smithing_table` (this mod) | `soul_lantern` (this mod) |
| `soul_lantern` | `lantern` (this mod) | `soul_campfire` (this mod) |
| `soul_campfire` | `soul_lantern` (this mod) | `shroomlight` (this mod) |
| `shroomlight` | `soul_campfire` (this mod) | `lodestone` (this mod) |
| `lodestone` | `shroomlight` (this mod) | `respawn_anchor` (this mod) |
| `respawn_anchor` | `lodestone` (this mod) | (end of the tab) |

## Redstone

| Item | After | Before |
|---|---|---|
| `target` | `tnt` | `lever` |
| `polished_blackstone_pressure_plate` | `stone_pressure_plate` | `oak_pressure_plate` |
| `crimson_pressure_plate` | `oak_pressure_plate` | `warped_pressure_plate` (this mod) |
| `warped_pressure_plate` | `crimson_pressure_plate` (this mod) | `redstone_torch` |
| `polished_blackstone_button` | `stone_button` | `oak_trapdoor` |
| `crimson_trapdoor` | `oak_trapdoor` | `warped_trapdoor` (this mod) |
| `warped_trapdoor` | `crimson_trapdoor` (this mod) | `oak_fence_gate` |
| `crimson_button` | `oak_button` | `warped_button` (this mod) |
| `warped_button` | `crimson_button` (this mod) | `trapped_chest` |
| `crimson_fence_gate` | `dark_oak_fence_gate` | `warped_fence_gate` (this mod) |
| `warped_fence_gate` | `crimson_fence_gate` (this mod) | `acacia_fence_gate` |
| `crimson_door` | `dark_oak_door` | `warped_door` (this mod) |
| `warped_door` | `crimson_door` (this mod) | (end of the tab) |

## Transportation

| Item | After | Before |
|---|---|---|
| `warped_fungus_on_a_stick` | `carrot_on_a_stick` | `tnt_minecart` |

## Miscellaneous

| Item | After | Before |
|---|---|---|
| `netherite_ingot` | `gold_ingot` | `netherite_scrap` (this mod) |
| `netherite_scrap` | `netherite_ingot` (this mod) | `stick` |
| `hoglin spawn egg` | `guardian spawn egg` | `horse spawn egg` |
| `piglin spawn egg` | `pig spawn egg` | `piglin_brute spawn egg` (this mod) |
| `piglin_brute spawn egg` | `piglin spawn egg` (this mod) | `polar_bear spawn egg` |
| `strider spawn egg` | `stray spawn egg` | `vex spawn egg` |
| `zoglin spawn egg` | `wolf spawn egg` | `zombie spawn egg` |
| `music_disc_pigstep` | `music_disc_wait` | `piglin_banner_pattern` (this mod) |
| `piglin_banner_pattern` | `music_disc_pigstep` (this mod) | (end of the tab) |

## Tools

| Item | After | Before |
|---|---|---|
| `netherite_shovel` | `diamond_hoe` | `netherite_pickaxe` (this mod) |
| `netherite_pickaxe` | `netherite_shovel` (this mod) | `netherite_axe` (this mod) |
| `netherite_axe` | `netherite_pickaxe` (this mod) | `netherite_hoe` (this mod) |
| `netherite_hoe` | `netherite_axe` (this mod) | `golden_hoe` |

## Combat

| Item | After | Before |
|---|---|---|
| `netherite_sword` | `diamond_sword` | `golden_sword` |
| `netherite_helmet` | `golden_boots` | `netherite_chestplate` (this mod) |
| `netherite_chestplate` | `netherite_helmet` (this mod) | `netherite_leggings` (this mod) |
| `netherite_leggings` | `netherite_chestplate` (this mod) | `netherite_boots` (this mod) |
| `netherite_boots` | `netherite_leggings` (this mod) | `enchanted_book` |
| `crossbow` | `totem_of_undying` | (end of the tab) |

## Not in the creative menu

As in vanilla 1.19.2, which keeps them out of every tab (they are still in the game): `lodestone_compass`.
