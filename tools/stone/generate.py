"""Stream S assets: blackstone and basalt blockstates, models, textures, sounds, names and recipes.

    python tools/stone/generate.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402
import building  # noqa: E402

M = assets.MODID

FULL = ["blackstone", "polished_blackstone", "polished_blackstone_bricks", "cracked_polished_blackstone_bricks",
        "chiseled_polished_blackstone", "gilded_blackstone"]
PILLARS = ["basalt", "polished_basalt"]
SETS = [("blackstone", "blackstone"), ("polished_blackstone", "polished_blackstone"),
        ("polished_blackstone_brick", "polished_blackstone_bricks")]
PLATE = "polished_blackstone_pressure_plate"
BUTTON = "polished_blackstone_button"

ITEMS = PILLARS + FULL + [PLATE, BUTTON]
for prefix, _ in SETS:
    ITEMS += [prefix + "_stairs", prefix + "_slab", prefix + "_wall"]
OURS = set(ITEMS)

# Blockstates and models
for name in FULL:
    building.simple_blockstate(name)
for name in PILLARS:
    building.port_blockstate(name)
for prefix, full in SETS:
    building.port_blockstate(prefix + "_stairs", drop=("waterlogged",))
    building.slab_blockstates(prefix + "_slab", full)
    building.wall_blockstate(prefix + "_wall")
building.old_blockstate("stone_pressure_plate", PLATE,
                        {"stone_pressure_plate_up": PLATE, "stone_pressure_plate_down": PLATE + "_down"})
building.old_blockstate("stone_button", BUTTON,
                        {"stone_button": BUTTON, "stone_button_pressed": BUTTON + "_pressed"})
for name in ITEMS:
    building.item_model(name)

# Sounds: basalt and gilded blackstone have 1.16 sound groups of their own.
events = {}
for group in ("basalt", "gilded_blackstone"):
    for kind in ("break", "step", "place", "hit", "fall"):
        e = "block.%s.%s" % (group, kind)
        events[e] = assets.copy_sound(e)
assets.merge_sounds("stone", events)

# Recipes: every 1.18 crafting recipe that makes one of these, plus stone tools, furnaces and brewing
# stands taking blackstone as cobblestone (1.16's stone_tool_materials / stone_crafting_materials).
names = []
smelting = []
for recipe, kind in building.recipes_making(OURS):
    if kind == "minecraft:smelting":
        smelting.append(building.smelting_from(recipe, OURS))
    elif kind.startswith("minecraft:crafting"):
        names.append(building.port_recipe(recipe, OURS))
either = [{"item": "minecraft:cobblestone"}, {"item": "%s:blackstone" % M}]
for tool in ("stone_pickaxe", "stone_axe", "stone_shovel", "stone_hoe", "stone_sword", "furnace", "brewing_stand"):
    names.append(building.port_recipe(tool, OURS, our_name="blackstone_" + tool,
                                      tags={"minecraft:stone_tool_materials": either,
                                            "minecraft:stone_crafting_materials": either}))
building.merge_smelting("stone", smelting)

# Names
lines = building.name_lines(ITEMS)
for prefix, _ in SETS:
    lines.append("tile.%s.%s_double_slab.name=%s" % (M, prefix, assets.name("block.minecraft.%s_slab" % prefix)))
lines += assets.subtitle_lines(events)
assets.merge_lang("stone", sorted(lines))
print("stone: %d items, %d recipes, %d smelting, %d sound events" % (len(ITEMS), len(names), len(smelting), len(events)))
