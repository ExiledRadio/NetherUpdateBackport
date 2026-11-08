# Requests for Backport Core

Things this mod needs from Backport Core (`E:\MincraftModding\Workspaces\BackportCore`). This repo never edits
Core; each request says the API wanted and what this mod does until it exists.

## 1. Add this repo to `dependents` (small)

`build.gradle`: `def dependents = [..., 'NetherUpdateBackport']`, so `gradlew build copyToDependents` refreshes
`NetherUpdateBackport/deps/`. **Until then:** the dev jar is copied by hand from `BackportCore/build/libs`.

## 2. 1.16's vanilla retextures for the `modern` pack (small)

A diff of the 1.15.2 and 1.16.5 client jars, for textures 1.12.2 also has, finds 1.16 changed:
`block/nether_wart_block`, `block/purpur_pillar_top`, `block/observer_back_on`, the diamond tools and armor
(`item/diamond_{sword,pickaxe,axe,shovel,hoe,helmet,chestplate,leggings,boots}`), `item/golden_pickaxe`,
`item/golden_hoe`, `item/wooden_hoe`, `item/wooden_pickaxe`, `item/stone_hoe`, and `gui/container/beacon`
(the netherite ingot slot). Wanted: these in `manual_map.py` under their 1.12.2 names, so they apply whenever
`textures.modernTextures` is on. **Until then:** 1.12.2's look for those; nothing here depends on it.

## 3. Modern structure templates, jigsaw, and a nether terrain adapter (large; for stream X)

Shared with Update Aquatic's stream S (the template loader itself), plus two things only nether structures need:

```java
// 1.16+ structure NBT (palette, blocks, entities) to 1.12.2 states, with block-name remaps a mod supplies.
ModernTemplate t = ModernTemplates.load(new ResourceLocation("netherupdatebackport", "bastion/treasure/bases/lava_basin"));
t.place(world, origin, rotation, mirror, processors, random);

// Jigsaw assembly: start pool, max depth (1.16 bastions: 6), bounding-box test, jigsaw blocks read from the
// templates (name, target, pool, joint, final_state), pool JSONs from assets/<mod>/worldgen/template_pool/.
List<PlacedPiece> pieces = Jigsaw.assemble(world, startPool, depth, origin, random);

// Nether columns have several floors: the lowest air-over-solid y in [minY, maxY] at x,z, predicted
// before generation where the generator allows it, else read from generated chunks.
NetherHeights heights = NetherHeights.forWorld(world);
int floor = heights.floor(x, z, 32, 120);
```

Ruined portals also need `TerrainHeights` in the overworld (already in Core) and an "ocean floor" variant
(`heights.floor(x, z)` ignoring water). **Until then:** stream X is not started.

## 4. Waterlogging adapter (later pass)

Blocks of this mod that 1.16 makes waterloggable are listed in `PLAN.md` (*Waterlogging*). They implement
Core's `Waterloggable` once it exists. **Until then:** not waterloggable.

## 5. Found by stream X against vanilla 1.16.5 (Core 1.2.0)

**Done** on Core's `nether-fixes` branch (version still 1.2.0; not yet merged into Core `main`): the first four
items below. This mod's workarounds were removed when stream X merged; it relies on Core for them now.

*   **Nether ruined portals are 25 apart, not 40.** 1.16.5's `DimensionSettings.nether` puts
    `RUINED_PORTAL -> (25, 10, 34222645)` into the nether's own structure settings. `VanillaPlacements.V1_16_5.RUINED_PORTAL`
    says "every ruined portal, overworld and nether alike" and is (40, 15). Wanted: a `RUINED_PORTAL_NETHER` constant (and
    the oracle comparing the nether's settings). Here: `RuinedPortals.NETHER_PLACEMENT`.
*   **A template of nothing but jigsaws places nothing.** `ModernTemplate.place` returns when `blocks` and `entities` are
    empty, before its jigsaw loop, so `bastion/blocks/gold` (a gold block), `bastion/blocks/air` and
    `bastion/treasure/extensions/empty` (basalt) are missing from every bastion. Here: `BastionPiece.placeLoneJigsaws`.
*   **A flattened block named without properties resolves to its first Flattening entry.** `block_age` writes
    `minecraft:stone_slab` (1.16's default state: a bottom half) and Core resolves it to `double_stone_slab`; any
    `replacedBy` or `final_state` naming a 1.12.2 block bare has the same problem. Wanted: missing properties taken from
    the block's 1.13 default. Here: explicit mappings for `stone_slab`, `stone_brick_slab`, `smooth_stone_slab`.
*   **A source mod's slab ignores `type`.** `ModernPalette`'s source lookup carries properties by name, so 1.12.2-style
    slabs (a half with `half`, a separate `_double_slab`) get bottom halves for `type=top` and `type=double`; that
    includes Village & Pillage's slabs, which Core lists as a source. Wanted: the family's slab convention in Core. Here:
    `StructurePalette.slab` for the slabs these structures use.
*   For information: 1.16.5's ruined portal files keep one jigsaw block in the floor (final state netherrack) and
    vanilla places it as a jigsaw block; Core places the final state, which is all 1.12.2 can do.


## 6. Found by stream Fm (Core 1.2.0)

*   **`DuplicateBlocks.hideFromCreative` stops the game on the other mod's Kotlin items.** Its `ModeledItem` (netherite
    ingot and scrap, and its bamboo, banner pattern, crossbow, fish bucket, scaffolding, seagrass and trident items)
    overrides `setCreativeTab` with a non-null parameter, so `item.setCreativeTab(null)` throws
    `IllegalArgumentException: tab` from Core's init and the server crashes. Wanted: catch per item and clear
    `Item.tabToDisplayOn` (`field_77701_a`) instead. Here: `FutureMcPairs` hides those two items itself and keeps
    them out of what it hands Core. Village & Pillage's scaffolding pair may hit the same thing.
*   For information: hiding a duplicate does not stop its world generation. This mod takes the other mod's ancient
    debris generator out of `GameRegistry` by class name; a `DuplicateBlocks.registerGenerator(theirClassName)` in
    Core would do it for every family mod.

## 7. Found by the waterlogging stream (Core 1.2.0)

*   **Fluidlogged API's whitelist is asked before `Waterloggable`.** Its built-in `whitelist.jsonc` covers every block
    extending a vanilla stair, slab, wall, fence, trapdoor, sign, fence gate, door, button or pressure plate, with any
    fluid and with flowing fluid (`TRUE_FOR_SIDE`), and its `FluidloggableEvent` handler answers ALLOW before the
    block is asked. So a family block of those shapes takes lava and flowing water whatever its `Waterloggable` says
    (Update Aquatic's prismarine stairs too). Wanted: Core's own `FluidloggableEvent` handler at `HIGHEST` for
    `Waterloggable` blocks, refusing non-water always and non-source water unless the block opts in (default: refuse,
    as 1.13+ vanilla). Here: `common/WaterRules`, for this mod's blocks only (it also refuses water for our
    non-`Waterloggable` blocks of those shapes: fence gates, doors, buttons, pressure plates).
*   **Source forming**: 1.13+ forms a source in a waterloggable block between two sources. With
    `nonSourceFluidlogging` on, Fluidlogged API asks with the flowing state first, which the rule above refuses, so
    no source forms. Core could form it itself (an end-of-tick check, as Update Aquatic's `RequiredWater`).

## 8. Look and feel (stream Lf): pieces Core's `presentation` module does not cover yet

Stream Lf registers 1.16 and 1.16.2 with `Presentation.version`. These 1.16 items are single-slot or need a
screen Core owns, so they wait for Core:

*   **Title logo and the startup screen (20w21a).** 1.16 redrew `textures/gui/title/minecraft.png` and replaced the
    red Mojang screen's logo with `mojangstudios.png`. Wanted: `Contribution.titleLogo(rl)` and
    `startupLogo(rl)`, newest wins. This mod would ship both from the 1.16.5 jar. **Until then:** 1.12.2's logos.
*   **Multiplayer warning screen.** 1.15.2 added "Online play is not rated" before the server list; 1.16 reworded it
    to "Caution: Third-Party Online Play". One screen, newest wins, with its "Do not show this screen again" flag
    (`skipMultiplayerWarning`). Wanted: `Contribution.multiplayerWarning(titleKey, contentKey)`. **Until then:** no
    warning screen (as 1.12.2).
*   **Chat: Line Spacing and Chat Delay (1.16, Accessibility and Chat Settings).** Both change how
    `GuiNewChat` lays out and releases lines, which needs one owner of the chat renderer. Wanted: Core owning the
    chat hooks (line height, a delayed queue) with the two options. **Until then:** not shown.
*   **Accessibility Guide button (1.16.4)** at the bottom of Accessibility Settings (opens
    `https://aka.ms/MinecraftJavaAccessibility`). Wanted: an action-button entry on Core's own screens.
*   **Option polish.** Vanilla shows "Distortion Effects: OFF" and "FOV Effects: OFF" at 0 (`options.*.off`) and
    since 1.16-pre1 a tooltip per option (`options.screenEffectScale.tooltip`, `options.fovEffectScale.tooltip`).
    Wanted: `SliderOption.zeroKey(key)` and `PresentationOption.tooltip(key)`. **Until then:** "0%" and no tooltip.
*   **One option on two screens.** Distortion Effects and FOV Effects sit on Video and on Accessibility Settings.
    Wanted: `option.alsoOn(OptionScreen.VIDEO)`. **Until then:** two options per setting
    (`..._video` ids) kept equal by `onChange` (`lookfeel/LookAndFeel.mirror`).
*   **Debug screen text.** 1.16 changed F3's left column (mob spawning line, coordinates on Targeted Block and
    Targeted Fluid, "Looking at" removed). 1.13 and later versions change the same lines, so they need one owner
    applying each version's edits in order. Wanted: a debug-text pipeline like `FogPipeline`. **Until then:**
    1.12.2's lines.
*   **Vanilla text by version.** Each backport rewords 1.12.2's own keys in its lang file (here: "You Died!",
    "Saved Hotbars", "Direct Connection" ...). That works while versions reword different keys; if two ever reword
    the same key, resource-domain order decides, not the version. Wanted later: Core applying vanilla text
    overrides newest-version-wins. Nothing overlaps among 1.13 to 1.16 today.

## Done in Backport Core 1.2.0 (core-finish, 2026-11-08)

Sections 6, 7 and 8 are done in Core, and this mod uses them (branch `nu-lookfeel2`):
*   6: `DuplicateBlocks` hides items that refuse a null tab itself (`hideItem`), and `registerGenerator` takes the
    other mod's debris generator out; `FutureMcPairs` no longer hides or removes anything itself.
*   7: Core's `waterlog/WaterRules` (water sources only for `Waterloggable` blocks, nothing for the family's other
    stairs-like shapes) and `SourceForming`; this mod's `common/WaterRules` is gone.
*   8: title logo, Mojang Studios startup screen, the multiplayer warning, Core's chat options (Line Spacing,
    Chat Delay), the Accessibility Guide (1.16.4 registered for it), OFF at zero and tooltips, one option on two
    screens (the `_video` mirrors are gone), the F3 text pipeline, vanilla text by version (`vanilla_text/`), and
    the in-game music owner (`music/client/Music116Step`; this mod's own ticker is gone).
