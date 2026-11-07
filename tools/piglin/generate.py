"""Stream P assets: piglin and brute textures, sounds, the bartering loot table, the Snout banner pattern,
advancements and names.

    python tools/piglin/generate.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402
import building  # noqa: E402

M = assets.MODID
OURS = {"piglin_banner_pattern", "polished_blackstone_bricks", "netherite_boots"}

assets.texture("entity/piglin/piglin")
assets.texture("entity/piglin/piglin_brute")
building.item_model("piglin_banner_pattern")
# The Snout's banner and shield textures, ready for when a loom can apply it (Village & Pillage's, see docs).
assets.texture("entity/banner/piglin")
assets.texture("entity/shield/piglin")

events = {}
for e in ("admiring_item", "ambient", "angry", "celebrate", "converted_to_zombified", "death", "hurt", "jealous", "retreat",
          "step"):
    events["entity.piglin." + e] = assets.copy_sound("entity.piglin." + e)
for e in ("ambient", "angry", "converted_to_zombified", "death", "hurt", "step"):
    events["entity.piglin_brute." + e] = assets.copy_sound("entity.piglin_brute." + e)
assets.merge_sounds("piglin", events)

# ------------------------------------------------------------------------------------------------
# Bartering: current Java's table (the 1.18 jar's, unchanged since 1.16.2), in 1.12.2's format.
# Other streams' things go through our condition and functions so a switched-off stream skips its entry.
# ------------------------------------------------------------------------------------------------
OTHER_STREAMS = {"minecraft:crying_obsidian": M + ":crying_obsidian", "minecraft:blackstone": M + ":blackstone"}
POTIONS = {"minecraft:fire_resistance": "minecraft:fire_resistance", "minecraft:water": "minecraft:water"}
ENCHANTMENTS = {"minecraft:soul_speed": M + ":soul_speed"}

table = assets.jar_json("data/minecraft/loot_tables/gameplay/piglin_bartering.json")
entries = []
for i, e in enumerate(table["pools"][0]["entries"]):
    name = e["name"]
    out = {"type": "item", "weight": e["weight"], "entryName": "%s_%d" % (name.split(":")[1], i)}
    functions = []
    conditions = []
    if name in OTHER_STREAMS:
        out["name"] = "minecraft:stone"
        conditions.append({"condition": M + ":registered", "items": [OTHER_STREAMS[name]]})
        functions.append({"function": M + ":set_item", "item": OTHER_STREAMS[name]})
    elif name in ("minecraft:potion", "minecraft:splash_potion"):
        out["name"] = name
    else:
        item = building.item(name, set(), set())
        out["name"] = item["item"]
        if "data" in item and item["data"]:
            functions.append({"function": "set_data", "data": item["data"]})
    for f in e.get("functions", []):
        kind = f["function"].split(":")[1]
        if kind == "set_count":
            functions.append({"function": "set_count", "count": {"min": int(f["count"]["min"]), "max": int(f["count"]["max"])}})
        elif kind == "set_potion":
            functions.append({"function": "set_nbt", "tag": "{Potion:\"%s\"}" % POTIONS[f["id"]]})
        elif kind == "enchant_randomly":
            ours = [ENCHANTMENTS[x] for x in f["enchantments"]]
            conditions.append({"condition": M + ":registered", "enchantments": ours})
            functions.append({"function": M + ":enchant_randomly", "enchantments": ours})
        else:
            raise ValueError("unhandled barter function " + kind)
    if functions:
        out["functions"] = functions
    if conditions:
        out["conditions"] = conditions
    entries.append(out)
assets.write_json("loot_tables/gameplay/piglin_bartering.json", {"pools": [{"name": "barter", "rolls": 1, "entries": entries}]})

# ------------------------------------------------------------------------------------------------
# Advancements. Oh Shiny fires from this stream; the others wait on streams V and X (see docs/streams/piglins.md).
# ------------------------------------------------------------------------------------------------
lines = []
lines += building.port_advancement("nether/distract_piglin", OURS, {
    "distract_piglin": building.event("distract_piglin"),
    "distract_piglin_directly": building.event("distract_piglin_directly")},
    [["distract_piglin", "distract_piglin_directly"]])
lines += building.port_advancement("nether/find_bastion", OURS, {"bastion": building.event("enter_bastion")})
LOOT = ["loot_bastion_other", "loot_bastion_treasure", "loot_bastion_hoglin_stable", "loot_bastion_bridge"]
lines += building.port_advancement("nether/loot_bastion", OURS, {c: building.event(c) for c in LOOT}, [LOOT])
# Hot Tourist Destinations: the nether wastes is 1.12.2's Hell biome, so vanilla's location criterion works now; the
# four new biomes are events stream V fires (a location criterion naming a biome that is not registered would fail
# the whole advancement).
BIOMES = ["minecraft:nether_wastes", "minecraft:soul_sand_valley", "minecraft:warped_forest", "minecraft:crimson_forest",
          "minecraft:basalt_deltas"]
criteria = {"minecraft:nether_wastes": {"trigger": "minecraft:location", "conditions": {"biome": "minecraft:hell"}}}
for b in BIOMES[1:]:
    criteria[b] = building.event("visit_" + b.split(":")[1])
lines += building.port_advancement("nether/explore_nether", OURS, criteria, [[b] for b in BIOMES])

lines += ["entity.%s.piglin.name=%s" % (M, assets.name("entity.minecraft.piglin")),
          "entity.%s.piglin_brute.name=%s" % (M, assets.name("entity.minecraft.piglin_brute")),
          "item.%s.piglin_banner_pattern.name=%s" % (M, assets.name("item.minecraft.piglin_banner_pattern")),
          "item.%s.piglin_banner_pattern.desc=%s" % (M, assets.name("item.minecraft.piglin_banner_pattern.desc"))]
lines += assets.subtitle_lines(events)
assets.merge_lang("piglin", sorted(set(lines)))
print("piglin: %d sound events, %d barter entries" % (len(events), len(entries)))
