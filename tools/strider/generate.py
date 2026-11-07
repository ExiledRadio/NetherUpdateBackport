"""Stream R assets: strider textures, sounds, loot table, the warped fungus on a stick, names, advancements.

    python tools/strider/generate.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402
import building  # noqa: E402

M = assets.MODID
OURS = {"warped_fungus_on_a_stick", "warped_fungus", "strider"}
for t in ("strider", "strider_cold", "strider_saddle"):
    assets.texture("entity/strider/" + t)
building.item_model("warped_fungus_on_a_stick")

events = {}
for e in ("ambient", "death", "eat", "happy", "hurt", "retreat", "saddle", "step", "step_lava"):
    events["entity.strider." + e] = assets.copy_sound("entity.strider." + e)
assets.merge_sounds("strider", events)

assets.write_json("loot_tables/entities/strider.json", {"pools": [
    {"name": "string", "rolls": 1, "entries": [{"type": "item", "name": "minecraft:string", "weight": 1, "functions": [
        {"function": "set_count", "count": {"min": 2, "max": 5}},
        {"function": "looting_enchant", "count": {"min": 0, "max": 1}}]}]}]})

for recipe, kind in building.recipes_making({"warped_fungus_on_a_stick"}):
    if kind.startswith("minecraft:crafting"):
        building.port_recipe(recipe, OURS)

lines = []
lines += building.port_advancement("nether/ride_strider", OURS, {"used_warped_fungus_on_a_stick": building.event("ride_strider")})
lines += building.port_advancement("nether/ride_strider_in_overworld_lava", OURS,
                                   {"ride_entity_in_lava": building.event("ride_strider_in_overworld_lava")})
lines += building.name_lines(["warped_fungus_on_a_stick"], kind="item")
lines.append("entity.%s.strider.name=%s" % (M, assets.name("entity.minecraft.strider")))
lines += assets.subtitle_lines(events)
assets.merge_lang("strider", sorted(set(lines)))
print("strider: %d sound events" % len(events))
