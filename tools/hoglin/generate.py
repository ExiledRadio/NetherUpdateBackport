"""Stream H assets: hoglin and zoglin textures, sounds, loot tables, names.

    python tools/hoglin/generate.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID
assets.texture("entity/hoglin/hoglin")
assets.texture("entity/hoglin/zoglin")

events = {}
for e in ("ambient", "angry", "attack", "converted_to_zombified", "death", "hurt", "retreat", "step"):
    events["entity.hoglin." + e] = assets.copy_sound("entity.hoglin." + e)
for e in ("ambient", "angry", "attack", "death", "hurt", "step"):
    events["entity.zoglin." + e] = assets.copy_sound("entity.zoglin." + e)
assets.merge_sounds("hoglin", events)


def count(lo, hi):
    return {"function": "set_count", "count": {"min": lo, "max": hi}}


def looting(lo, hi):
    return {"function": "looting_enchant", "count": {"min": lo, "max": hi}}


# Current Java's loot tables in 1.12.2's format.
assets.write_json("loot_tables/entities/hoglin.json", {"pools": [
    {"name": "porkchop", "rolls": 1, "entries": [{"type": "item", "name": "minecraft:porkchop", "weight": 1, "functions": [
        count(2, 4),
        {"function": "furnace_smelt", "conditions": [{"condition": "entity_properties", "entity": "this", "properties": {"on_fire": True}}]},
        looting(0, 1)]}]},
    {"name": "leather", "rolls": 1, "entries": [{"type": "item", "name": "minecraft:leather", "weight": 1, "functions": [
        count(0, 1), looting(0, 1)]}]}]})
assets.write_json("loot_tables/entities/zoglin.json", {"pools": [
    {"name": "rotten_flesh", "rolls": 1, "entries": [{"type": "item", "name": "minecraft:rotten_flesh", "weight": 1, "functions": [
        count(1, 3), looting(0, 1)]}]}]})

lines = ["entity.%s.hoglin.name=%s" % (M, assets.name("entity.minecraft.hoglin")),
         "entity.%s.zoglin.name=%s" % (M, assets.name("entity.minecraft.zoglin"))]
lines += assets.subtitle_lines(events)
assets.merge_lang("hoglin", sorted(set(lines)))
print("hoglins: %d sound events" % len(events))
