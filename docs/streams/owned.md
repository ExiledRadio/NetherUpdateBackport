# Stream Ow: owner copies

Package `owned`. No config: the copies are registered exactly when their owner, Village & Pillage
(`villagerbackport`), is absent (Rule 1, and the family's "every mod works on its own"). Not yet: the crossbow copy
(with stream P).

## Changelog section

*   Without Village & Pillage, this mod adds the **smithing table** (2 iron ingots and 4 planks) with 1.16's
    upgrade screen: a diamond tool or armor piece and a netherite ingot make the netherite piece, keeping its
    enchantments, name and damage.

## How it works

*   An identical copy of Village & Pillage's table: `netherupdatebackport:smithing_table`, one state (metadata 0),
    wood, hardness 2.5, Village & Pillage's resistance; its container and screen copied from Village & Pillage's
    (`ContainerSmithingTable`, `GuiSmithingTable`, 1.16's layout and texture); its block model, textures, screen
    texture, sound and recipe copied from Village & Pillage's resources by `tools/owned/copy_from_owner.py`. The
    title is vanilla's `container.upgrade`.
*   Upgrades come from Backport Core's `SmithingUpgrades` (stream N adds netherite's nine). Without Village & Pillage
    this mod is the table's owner and calls `SmithingUpgrades.configure` with Village & Pillage's defaults.
*   Replaces stream N's crafting-grid fallback (removed). The hoglins' nether-wide spawn switch is gone too:
    they spawn where vanilla puts them (stream V).
*   **For Village & Pillage's maintainer** (not done here): when Village & Pillage is added to a world that had this
    copy, rename `netherupdatebackport:smithing_table` to its own (same state, same metadata).

## Tested headlessly

`-Pselftest=all`: **pass=7106 fail=0**; `check_assets.py` 0 errors.

*   Registered with its item in a game without Village & Pillage; one state, metadata 0, hardness 2.5.
*   The recipe (2 iron, 4 planks of any wood).
*   With netherite present the table is active and its screen is the upgrade container; a damaged, named,
    Sharpness V diamond sword plus netherite ingots shows a netherite sword keeping all three; taking it uses the
    sword and one ingot; an iron sword upgrades to nothing.

## Not tested

*   The screen drawn, and opening it by a real right-click (the test's player has no network connection).
*   A game with Village & Pillage, where the copy must not register (the check is `Loader.isModLoaded`).
