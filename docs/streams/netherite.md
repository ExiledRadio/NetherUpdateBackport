# Stream N: netherite

Package `netherite`. Config `netherite` (`enabled`, `generate`, `craftingFallback`). Entity id 1 (fireproof item).

## Changelog section

*   **Ancient debris** deep in the nether, never touching air. Mine it with a diamond pickaxe; explosions can't break it.
    Smelt it into **netherite scrap**; 4 scrap and 4 gold ingots make a **netherite ingot**; 9 ingots a **netherite
    block** (a beacon base; the ingot pays a beacon).
*   **Netherite tools and armor**: upgrade diamond gear with a netherite ingot at the smithing table, keeping its
    enchantments and name. With no smithing table installed, the same upgrade works in the crafting grid.
*   Netherite items never burn and float up through lava when dropped.
*   Advancements *Hidden in the Depths*, *Cover Me in Debris*, *Serious Dedication*; *Country Lode, Take Me Home*
    (stream U's) now appears.

## How it works

*   1.16.5's `ItemTier.NETHERITE` and `ArmorMaterial.NETHERITE`, through 1.12.2's tool classes: sword 8, pickaxe 6,
    axe 10 (speed 1.0), shovel 6.5, hoe 1 (speed 4; 1.12.2's hoe formula would give 5 and is overridden), 2031 uses,
    mining level 4, enchantability 15; armor 3/8/6/3 with durability 407/592/555/481, toughness 3, and 1.16's
    knockback resistance 0.1 per piece added as an attribute (1.12.2's armor material has no field for it).
*   **Smithing (Rule 2)**: the nine upgrades are added to Backport Core's `SmithingUpgrades` at init, which Village
    & Pillage's smithing table reads. Village & Pillage 2.3.0 (before Core) also finds them by its name rules
    (`diamond_x` -> `netherite_x` in a mod with a netherite ingot). **Crafting fallback** (`NetheriteUpgradeRecipe`)
    registered only when Village & Pillage is absent: diamond piece + ingot, its tag (enchantments, name, repair cost)
    and damage carried over.
*   **Fireproof drops**: every netherite item makes an `EntityFireproofItem` through Forge's `Item.createEntity`. It
    ignores all fire damage (1.16's `canBeHurtBy`), and in lava moves as 1.16.5's `ItemEntity`: no gravity, sideways
    motion x0.95, a slight rise; 1.12.2's random bounce of items entering lava is undone. 1.12.2 still plays its
    fizz sound there.
*   **Generation**: 1.16.5's two `NoExposedOreFeature`s (up to 3 at y 8-24, up to 2 at y 8-119, per chunk), replacing
    netherrack, basalt or blackstone, never placed next to air. Decoration only.
*   Ore names `ingotNetherite`, `scrapNetherite`, `blockNetherite`, `oreAncientDebris`.

## Tested headlessly

`-Pselftest=netherite`: **pass=126 fail=0**; all streams: **pass=6991 fail=0**; `check_assets.py`: **83 blocks,
883 states, 85 item models, 529 sound files, 0 errors**.

*   Hardness 30/50, blast resistance 1200, diamond level, beacon base and payment.
*   Attack damage and speed modifiers of sword, pickaxe, axe, shovel, hoe; 2031 uses; mining level 4; each armor
    piece's points and durability; toughness 3; 0.1 knockback resistance; repaired with ingots.
*   A netherite ingot dropped into lava becomes the fireproof item, survives 5 seconds in it, and rises when still
    under the surface; a gold ingot in the same lava burns.
*   Smelting debris (2 experience); ingot and block recipes; the crafting fallback makes a netherite sword keeping
    Sharpness III, a custom name and its damage.
*   Backport Core's smithing registry lists all nine; `find(diamond pickaxe, our ingot)` gives our pickaxe.
*   Advancements loaded (including stream U's *Country Lode*); 36 new nether chunks hold ancient debris, none
    touching air.

## Not tested

*   Visuals: item icons, armor on a player, the fireproof item's rendering (it uses vanilla's item renderer by
    class inheritance).
*   Village & Pillage's smithing table screen doing the upgrade (needs that mod; the registry side is tested).
