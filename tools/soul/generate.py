"""Stream L assets: soul soil, soul fire, soul torch, soul lantern, soul campfire, Soul Speed, particles.

    python tools/soul/generate.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402
import building  # noqa: E402

M = assets.MODID
ITEMS = ["soul_soil", "soul_torch", "soul_lantern", "soul_campfire"]
OURS = set(ITEMS)

building.simple_blockstate("soul_soil")
building.port_blockstate("soul_fire")
building.old_blockstate("torch", "soul_torch", {"normal_torch": "soul_torch", "normal_torch_wall": "soul_wall_torch"})
building.port_blockstate("soul_lantern", drop=("waterlogged",))
building.port_blockstate("soul_campfire", drop=("signal_fire", "waterlogged"))
for name in ITEMS:
    building.item_model(name)

# Particles drawn from the block atlas, and the campfire's smoke.
assets.texture("particle/soul_fire_flame")
for i in range(11):
    assets.texture("particle/soul_%d" % i)
for i in range(12):
    assets.texture("particle/big_smoke_%d" % i)

events = {}
for group in ("soul_soil", "lantern"):
    for kind in ("break", "step", "place", "hit", "fall"):
        e = "block.%s.%s" % (group, kind)
        events[e] = assets.copy_sound(e)
for e in ("block.campfire.crackle", "particle.soul_escape"):
    events[e] = assets.copy_sound(e)
assets.merge_sounds("soul", events)

tags = {"minecraft:soul_fire_base_blocks": [{"item": "minecraft:soul_sand"}, {"item": "%s:soul_soil" % M}],
        "minecraft:logs": {"type": "forge:ore_dict", "ore": "logWood"}}
for recipe, kind in building.recipes_making(OURS):
    if kind.startswith("minecraft:crafting"):
        building.port_recipe(recipe, OURS, tags=tags)

lines = building.name_lines(ITEMS + ["soul_fire"])
lines.append("enchantment.%s.soul_speed=%s" % (M, assets.name("enchantment.minecraft.soul_speed")))
lines += assets.subtitle_lines(events)
assets.merge_lang("soul", sorted(set(lines)))
print("soul: %d items, %d sound events" % (len(ITEMS), len(events)))
