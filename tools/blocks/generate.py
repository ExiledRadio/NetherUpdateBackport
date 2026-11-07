"""Stream U assets: chain, crying obsidian, target, respawn anchor, lodestone, lodestone compass, nether gold
ore, quartz bricks, cracked and chiseled nether bricks.

    python tools/blocks/generate.py
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402
import building  # noqa: E402

M = assets.MODID

FULL = ["crying_obsidian", "lodestone", "nether_gold_ore", "quartz_bricks", "cracked_nether_bricks", "chiseled_nether_bricks"]
BLOCKS = FULL + ["chain", "target", "respawn_anchor"]
# netherite_ingot is stream N's: the lodestone recipe needs it and is skipped without it.
OURS = set(BLOCKS) | {"lodestone_compass", "netherite_ingot"}

for name in FULL:
    building.simple_blockstate(name)
building.port_blockstate("chain", drop=("waterlogged",))
building.port_blockstate("respawn_anchor")
# 1.18's target has one model; every power level looks the same.
assets.write_json("blockstates/target.json", {"variants": {
    "power=%d" % p: {"model": building.our_model("minecraft:block/target")} for p in range(16)}})
for name in BLOCKS:
    building.item_model(name)

# The lodestone compass: 1.18's needle frames, picked by the "angle" property exactly as 1.12.2's own
# compass.json picks them.
compass = json.loads(assets.OLD_JAR.read("assets/minecraft/models/item/compass.json").decode("utf-8"))
for i in range(32):
    tex = assets.texture("item/compass_%02d" % i)
    assets.write_json("models/item/lodestone_compass_%02d.json" % i, {"parent": "item/generated", "textures": {"layer0": tex}})
for o in compass["overrides"]:
    o["model"] = "%s:item/lodestone_%s" % (M, o["model"].split("/")[-1])
compass["textures"]["layer0"] = "%s:item/compass_16" % M
assets.write_json("models/item/lodestone_compass.json", compass)

# Sounds
events = {}
for group in ("chain", "nether_bricks", "nether_gold_ore", "lodestone"):
    for kind in ("break", "step", "place", "hit", "fall"):
        e = "block.%s.%s" % (group, kind)
        events[e] = assets.copy_sound(e)
for e in ("block.respawn_anchor.charge", "block.respawn_anchor.set_spawn", "block.respawn_anchor.deplete",
          "block.respawn_anchor.ambient", "item.lodestone_compass.lock"):
    events[e] = assets.copy_sound(e)
assets.merge_sounds("blocks", events)

# Recipes and smelting
smelting = []
for recipe, kind in building.recipes_making(OURS - {"netherite_ingot"}):
    if kind == "minecraft:smelting":
        smelting.append(building.smelting_from(recipe, OURS))
    elif kind.startswith("minecraft:crafting"):
        building.port_recipe(recipe, OURS)
# 1.16 smelts nether gold ore into a gold ingot (the recipe is named after the ingot).
smelting.append(building.smelting_from("gold_ingot_from_smelting_nether_gold_ore", OURS))
building.merge_smelting("blocks", smelting)

# Advancements
lines = []
lines += building.port_advancement("nether/obtain_crying_obsidian", OURS, {"crying_obsidian": building.obtain("crying_obsidian")})
lines += building.port_advancement("nether/charge_respawn_anchor", OURS, {"charge_respawn_anchor": building.event("charge_respawn_anchor_max")})
lines += building.port_advancement("nether/use_lodestone", OURS, {"use_lodestone": building.event("use_lodestone")})
lines += building.port_advancement("adventure/bullseye", OURS, {"bullseye": building.event("target_bullseye")})

# Names and messages (1.16's own keys, which 1.12.2 lacks)
lines += building.name_lines(BLOCKS)
lines += building.name_lines(["lodestone_compass"], kind="item")
for key in ("block.minecraft.set_spawn", "block.minecraft.spawn.not_valid"):
    lines.append("%s=%s" % (key, assets.VANILLA_LANG[key]))
lines += assets.subtitle_lines(events)
assets.merge_lang("blocks", sorted(set(lines)))
print("blocks: %d blocks, %d sound events, %d smelting" % (len(BLOCKS), len(events), len(smelting)))
