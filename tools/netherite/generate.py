"""Stream N assets: ancient debris, netherite block, scrap, ingot, tools, armor; names, sounds, recipes, advancements.

    python tools/netherite/generate.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402
import building  # noqa: E402

M = assets.MODID
BLOCKS = ["ancient_debris", "netherite_block"]
GEAR = ["netherite_sword", "netherite_shovel", "netherite_pickaxe", "netherite_axe", "netherite_hoe",
        "netherite_helmet", "netherite_chestplate", "netherite_leggings", "netherite_boots"]
ITEMS = ["netherite_scrap", "netherite_ingot"] + GEAR
OURS = set(BLOCKS) | set(ITEMS)

building.port_blockstate("ancient_debris")
building.simple_blockstate("netherite_block")
for name in BLOCKS + ITEMS:
    building.item_model(name)
for layer in (1, 2):
    assets.texture("models/armor/netherite_layer_%d" % layer)

events = {}
for group in ("ancient_debris", "netherite_block"):
    for kind in ("break", "step", "place", "hit", "fall"):
        e = "block.%s.%s" % (group, kind)
        events[e] = assets.copy_sound(e)
events["item.armor.equip_netherite"] = assets.copy_sound("item.armor.equip_netherite")
assets.merge_sounds("netherite", events)

smelting = []
for recipe, kind in building.recipes_making(OURS):
    if kind == "minecraft:smelting":
        smelting.append(building.smelting_from(recipe, OURS))
    elif kind.startswith("minecraft:crafting"):
        building.port_recipe(recipe, OURS)
building.merge_smelting("netherite", smelting)

lines = []
lines += building.port_advancement("nether/obtain_ancient_debris", OURS, {"ancient_debris": building.obtain("ancient_debris")})
lines += building.port_advancement("nether/netherite_armor", OURS, {"netherite_armor": {
    "trigger": "minecraft:inventory_changed", "conditions": {"items": [
        {"item": "%s:%s" % (M, p)} for p in ("netherite_helmet", "netherite_chestplate", "netherite_leggings", "netherite_boots")]}}})
lines += building.port_advancement("husbandry/obtain_netherite_hoe", OURS, {"netherite_hoe": building.obtain("netherite_hoe")})
lines += building.name_lines(BLOCKS)
lines += building.name_lines(ITEMS, kind="item")
lines += assets.subtitle_lines(events)
assets.merge_lang("netherite", sorted(set(lines)))
print("netherite: %d blocks, %d items, %d smelting, %d sound events" % (len(BLOCKS), len(ITEMS), len(smelting), len(events)))
