"""Stream F assets: crimson and warped plants, vines, wart block, shroomlight and both wood sets.

    python tools/fungus/generate.py
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402
import building  # noqa: E402

M = assets.MODID
WOODS = ["crimson", "warped"]

ITEM_BLOCKS = []      # blocks with an ItemBlock (models/item from 1.18)
for w in WOODS:
    ITEM_BLOCKS += ["%s_nylium" % w, "%s_fungus" % w, "%s_roots" % w, "%s_stem" % w, "stripped_%s_stem" % w,
                    "%s_hyphae" % w, "stripped_%s_hyphae" % w, "%s_planks" % w, "%s_slab" % w, "%s_stairs" % w,
                    "%s_fence" % w, "%s_fence_gate" % w, "%s_trapdoor" % w, "%s_button" % w, "%s_pressure_plate" % w]
ITEM_BLOCKS += ["nether_sprouts", "weeping_vines", "twisting_vines", "warped_wart_block", "shroomlight"]
ITEMS = ["%s_door" % w for w in WOODS] + ["%s_sign" % w for w in WOODS]
OURS = set(ITEM_BLOCKS) | set(ITEMS)

# Blockstates ---------------------------------------------------------------------------------------
for w in WOODS:
    for name in ("%s_nylium", "%s_fungus", "%s_roots", "%s_planks", "%s_sign", "%s_wall_sign"):
        building.port_blockstate(name % w)
    for name in ("%s_stem", "stripped_%s_stem", "%s_hyphae", "stripped_%s_hyphae"):
        building.port_blockstate(name % w)
    building.slab_blockstates("%s_slab" % w, "%s_planks" % w)
    building.port_blockstate("%s_stairs" % w, drop=("waterlogged",))
    building.port_blockstate("%s_fence" % w, drop=("waterlogged",))
    building.port_blockstate("%s_fence_gate" % w, drop=("powered",))
    building.port_blockstate("%s_door" % w, drop=("powered",))
    building.port_blockstate("%s_trapdoor" % w, drop=("powered", "waterlogged"))
    building.old_blockstate("wooden_button", "%s_button" % w,
                            {"wooden_button": "%s_button" % w, "wooden_button_pressed": "%s_button_pressed" % w})
    building.old_blockstate("wooden_pressure_plate", "%s_pressure_plate" % w,
                            {"wooden_pressure_plate_up": "%s_pressure_plate" % w,
                             "wooden_pressure_plate_down": "%s_pressure_plate_down" % w})
    assets.texture("entity/signs/%s" % w)
for name in ("nether_sprouts", "weeping_vines", "weeping_vines_plant", "twisting_vines", "twisting_vines_plant",
             "warped_wart_block", "shroomlight"):
    building.port_blockstate(name)
for name in ITEM_BLOCKS + ITEMS:
    building.item_model(name)

# Sounds ------------------------------------------------------------------------------------------
events = {}
for group in ("nylium", "fungus", "roots", "nether_sprouts", "weeping_vines", "stem", "wart_block", "shroomlight"):
    for kind in ("break", "step", "place", "hit", "fall"):
        e = "block.%s.%s" % (group, kind)
        events[e] = assets.copy_sound(e)
for e in ("item.axe.strip", "block.growing_plant.crop"):
    events[e] = assets.copy_sound(e)
assets.merge_sounds("fungus", events)

# Recipes -----------------------------------------------------------------------------------------
tags = {}
for w in WOODS:
    tags["minecraft:%s_stems" % w] = [{"item": "%s:%s" % (M, n)} for n in
                                       ("%s_stem" % w, "stripped_%s_stem" % w, "%s_hyphae" % w, "stripped_%s_hyphae" % w)]
for recipe, kind in building.recipes_making(OURS):
    if kind.startswith("minecraft:crafting"):
        building.port_recipe(recipe, OURS, tags=tags)

# Vanilla's wooden things from any planks (1.16's #planks and #wooden_slabs take nether wood): our planks and
# slabs beside 1.12.2's own, in one ingredient so they mix.
any_planks = [{"item": "minecraft:planks", "data": 32767}] + [{"item": "%s:%s_planks" % (M, w)} for w in WOODS]
any_slabs = [{"item": "minecraft:wooden_slab", "data": 32767}] + [{"item": "%s:%s_slab" % (M, w), "data": 0} for w in WOODS]
wood_tags = {"minecraft:planks": any_planks, "minecraft:wooden_slabs": any_slabs}
made = []
for n in assets.JAR.namelist():
    if not (n.startswith("data/minecraft/recipes/") and n.endswith(".json")):
        continue
    data = json.loads(assets.JAR.read(n).decode("utf-8"))
    text = json.dumps(data)
    if not data["type"].startswith("minecraft:crafting_") or not ('"minecraft:planks"' in text or '"minecraft:wooden_slabs"' in text):
        continue
    name = n[len("data/minecraft/recipes/"):-len(".json")]
    try:
        # Every 1.12.2 wooden thing; anything 1.12.2 lacks (barrels, looms, beds by color) raises and is skipped.
        made.append(building.port_recipe(name, OURS, our_name="nether_wood_" + name, tags=wood_tags))
    except KeyError:
        pass

# Names -------------------------------------------------------------------------------------------
lines = building.name_lines(ITEM_BLOCKS)
for w in WOODS:
    lines += building.name_lines(["%s_door" % w, "%s_sign" % w, "%s_wall_sign" % w])
    lines.append("item.%s.%s_door.name=%s" % (M, w, assets.name("block.minecraft.%s_door" % w)))
    lines.append("item.%s.%s_sign.name=%s" % (M, w, assets.name("block.minecraft.%s_sign" % w)))
    lines.append("tile.%s.%s_double_slab.name=%s" % (M, w, assets.name("block.minecraft.%s_slab" % w)))
lines += building.name_lines(["weeping_vines_plant", "twisting_vines_plant"])
lines += assets.subtitle_lines(events)
assets.merge_lang("fungus", sorted(set(lines)))
print("fungus: %d block items, %d items, %d sound events, %d wooden recipes: %s"
      % (len(ITEM_BLOCKS), len(ITEMS), len(events), len(made), ", ".join(sorted(made))))
