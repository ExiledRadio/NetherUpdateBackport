"""Owner copies' assets, taken from the owner itself (Village & Pillage) so they look and sound identical.

    python tools/owned/copy_from_owner.py

Reads Village & Pillage's resources (its core-extraction worktree, the Backport Core version); never edits them.
"""
import json
import os
import shutil
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID
OWNER = "E:/MincraftModding/Workspaces/RLCraftVillagerTrading-core-extraction/src/main/resources/assets/villagerbackport"


def copy(rel, dst=None, rename=True):
    src = os.path.join(OWNER, rel)
    out = os.path.join(assets.ASSETS, dst or rel)
    os.makedirs(os.path.dirname(out), exist_ok=True)
    if rename and rel.endswith(".json"):
        text = open(src, encoding="utf-8").read().replace("villagerbackport:", M + ":")
        with open(out, "w", encoding="utf-8", newline="\n") as f:
            f.write(text)
    else:
        shutil.copyfile(src, out)


# The smithing table: blockstate, models, textures, its screen, its sound.
copy("blockstates/smithing_table.json")
copy("models/block/smithing_table.json")
copy("models/item/smithing_table.json")
for side in ("bottom", "front", "side", "top"):
    copy("textures/blocks/smithing_table_%s.png" % side, rename=False)
copy("textures/gui/smithing.png", rename=False)

owner_sounds = json.load(open(os.path.join(OWNER, "sounds.json"), encoding="utf-8"))
event = json.loads(json.dumps(owner_sounds["block.smithing_table.use"]).replace("villagerbackport:", M + ":"))
for s in event["sounds"]:
    name = s["name"] if isinstance(s, dict) else s
    path = name.split(":", 1)[1]
    copy("sounds/%s.ogg" % path, rename=False)
if "subtitle" in event and event["subtitle"].startswith("subtitles.villagerbackport."):
    event["subtitle"] = event["subtitle"].replace("villagerbackport", M)


# The crossbow (with stream P): item models, textures, sounds and recipe, as the owner ships them.
for m in ("crossbow", "crossbow_arrow", "crossbow_base", "crossbow_firework", "crossbow_pulling_0", "crossbow_pulling_1",
          "crossbow_pulling_2"):
    copy("models/item/%s.json" % m)
for t in ("crossbow_arrow", "crossbow_firework", "crossbow_pulling_0", "crossbow_pulling_1", "crossbow_pulling_2",
          "crossbow_standby"):
    copy("textures/items/%s.png" % t, rename=False)
crossbow_events = {}
subtitles = set()
for name in ("item.crossbow.shoot", "item.crossbow.loading_start", "item.crossbow.loading_middle", "item.crossbow.loading_end",
             "item.crossbow.quick_charge_1", "item.crossbow.quick_charge_2", "item.crossbow.quick_charge_3"):
    ev = json.loads(json.dumps(owner_sounds[name]).replace("villagerbackport:", M + ":").replace("subtitles.villagerbackport.",
                                                                                               "subtitles.%s." % M))
    for snd in ev["sounds"]:
        path = (snd["name"] if isinstance(snd, dict) else snd).split(":", 1)[1]
        copy("sounds/%s.ogg" % path, rename=False)
    if "subtitle" in ev:
        subtitles.add(ev["subtitle"])
    crossbow_events[name] = ev
crossbow_recipe = json.load(open(os.path.join(OWNER, "recipes/crossbow.json"), encoding="utf-8"))
crossbow_recipe["result"]["item"] = "%s:crossbow" % M
crossbow_recipe["conditions"] = [{"type": "%s:items" % M, "items": ["%s:crossbow" % M]}]
assets.write_json("recipes/crossbow.json", crossbow_recipe)

recipe = json.load(open(os.path.join(OWNER, "recipes/smithing_table.json"), encoding="utf-8"))
recipe["result"]["item"] = "%s:smithing_table" % M
recipe["conditions"] = [{"type": "%s:items" % M, "items": ["%s:smithing_table" % M]}]
assets.write_json("recipes/smithing_table.json", recipe)

sound_events = {"block.smithing_table.use": event}
sound_events.update(crossbow_events)
assets.merge_sounds("owned", sound_events)

# Stream X: the lantern, smooth quartz slab and stone brick wall 1.16's bastions and ruined portals use.
# And the mossy stone brick stairs, slab and wall 1.16's block_age processor makes of a ruined portal's stone bricks.
for state in ("lantern", "smooth_quartz_slab", "smooth_quartz_double_slab", "stone_brick_wall", "mossy_stone_brick_stairs",
              "mossy_stone_brick_slab", "mossy_stone_brick_double_slab", "mossy_stone_brick_wall"):
    copy("blockstates/%s.json" % state)
for model in ("lantern", "hanging_lantern", "smooth_quartz_slab", "smooth_quartz_slab_top", "smooth_quartz_double_slab",
              "stone_brick_wall_post", "stone_brick_wall_side", "stone_brick_wall_inventory", "mossy_stone_brick_stairs",
              "mossy_stone_brick_stairs_inner", "mossy_stone_brick_stairs_outer", "mossy_stone_brick_slab", "mossy_stone_brick_slab_top",
              "mossy_stone_brick_double_slab", "mossy_stone_brick_wall_post", "mossy_stone_brick_wall_side",
              "mossy_stone_brick_wall_inventory"):
    copy("models/block/%s.json" % model)
for item in ("lantern", "smooth_quartz_slab", "stone_brick_wall", "mossy_stone_brick_stairs", "mossy_stone_brick_slab",
             "mossy_stone_brick_wall"):
    copy("models/item/%s.json" % item)
# A wall standing alone in no state the world makes (every side and the post off) still gets a post, as stream S's
# walls have it, so every state draws something.
for wall in ("stone_brick_wall", "mossy_stone_brick_wall"):
    path = os.path.join(assets.ASSETS, "blockstates", wall + ".json")
    data = json.load(open(path, encoding="utf-8"))
    data["multipart"].append({"when": {"up": "false", "north": "false", "east": "false", "south": "false", "west": "false"},
                              "apply": {"model": "%s:%s_post" % (M, wall)}})
    assets.write_json("blockstates/%s.json" % wall, data)
copy("textures/blocks/lantern.png", rename=False)
copy("textures/blocks/lantern.png.mcmeta", rename=False)
copy("textures/items/lantern.png", rename=False)
recipe = json.load(open(os.path.join(OWNER, "recipes/lantern.json"), encoding="utf-8"))
recipe["result"]["item"] = "%s:lantern" % M
recipe["conditions"] = [{"type": "%s:items" % M, "items": ["%s:lantern" % M]}]
assets.write_json("recipes/lantern.json", recipe)
# 1.14's stone brick wall recipes (Village & Pillage writes its building recipes in code).
assets.write_json("recipes/mossy_stone_brick_wall.json", {
    "type": "minecraft:crafting_shaped", "pattern": ["###", "###"],
    "key": {"#": {"item": "minecraft:stonebrick", "data": 1}},
    "result": {"item": "%s:mossy_stone_brick_wall" % M, "count": 6},
    "conditions": [{"type": "%s:items" % M, "items": ["%s:mossy_stone_brick_wall" % M]}]})
assets.write_json("recipes/mossy_stone_brick_stairs.json", {
    "type": "minecraft:crafting_shaped", "pattern": ["#  ", "## ", "###"],
    "key": {"#": {"item": "minecraft:stonebrick", "data": 1}},
    "result": {"item": "%s:mossy_stone_brick_stairs" % M, "count": 4},
    "conditions": [{"type": "%s:items" % M, "items": ["%s:mossy_stone_brick_stairs" % M]}]})
assets.write_json("recipes/mossy_stone_brick_slab.json", {
    "type": "minecraft:crafting_shaped", "pattern": ["###"],
    "key": {"#": {"item": "minecraft:stonebrick", "data": 1}},
    "result": {"item": "%s:mossy_stone_brick_slab" % M, "data": 0, "count": 6},
    "conditions": [{"type": "%s:items" % M, "items": ["%s:mossy_stone_brick_slab" % M]}]})
assets.write_json("recipes/stone_brick_wall.json", {
    "type": "minecraft:crafting_shaped", "pattern": ["###", "###"],
    "key": {"#": {"item": "minecraft:stonebrick", "data": 0}},
    "result": {"item": "%s:stone_brick_wall" % M, "count": 6},
    "conditions": [{"type": "%s:items" % M, "items": ["%s:stone_brick_wall" % M]}]})

lines = ["tile.%s.smithing_table.name=%s" % (M, assets.name("block.minecraft.smithing_table")),
         "item.%s.crossbow.name=%s" % (M, assets.name("item.minecraft.crossbow")),
         "item.%s.crossbow.projectile=%s %%s" % (M, assets.name("item.minecraft.crossbow.projectile")),
         "entity.%s.crossbow_arrow.name=%s" % (M, assets.name("entity.minecraft.arrow")),
         "entity.%s.crossbow_spectral_arrow.name=%s" % (M, assets.name("entity.minecraft.spectral_arrow")),
         "entity.%s.crossbow_firework.name=%s" % (M, assets.name("entity.minecraft.firework_rocket")),
         "tile.%s.lantern.name=%s" % (M, assets.name("block.minecraft.lantern")),
         "tile.%s.smooth_quartz_slab.name=%s" % (M, assets.name("block.minecraft.smooth_quartz_slab")),
         "tile.%s.smooth_quartz_double_slab.name=%s" % (M, assets.name("block.minecraft.smooth_quartz_slab")),
         "tile.%s.stone_brick_wall.name=%s" % (M, assets.name("block.minecraft.stone_brick_wall")),
         "tile.%s.mossy_stone_brick_stairs.name=%s" % (M, assets.name("block.minecraft.mossy_stone_brick_stairs")),
         "tile.%s.mossy_stone_brick_slab.name=%s" % (M, assets.name("block.minecraft.mossy_stone_brick_slab")),
         "tile.%s.mossy_stone_brick_double_slab.name=%s" % (M, assets.name("block.minecraft.mossy_stone_brick_slab")),
         "tile.%s.mossy_stone_brick_wall.name=%s" % (M, assets.name("block.minecraft.mossy_stone_brick_wall")),
         "container.upgrade=%s" % assets.name("container.upgrade")]
sub = event.get("subtitle")
if sub and sub.startswith("subtitles.%s." % M):
    lines.append("%s=%s" % (sub, assets.VANILLA_LANG.get(sub.replace(M + ".", "", 1), "Smithing Table used")))
for e in ("quick_charge", "multishot", "piercing"):
    lines.append("enchantment.%s.%s=%s" % (M, e, assets.name("enchantment.minecraft." + e)))
for sub in subtitles:
    lines.append("%s=%s" % (sub, assets.VANILLA_LANG[sub.replace(M + ".", "", 1)]))
assets.merge_lang("owned", sorted(set(lines)))
print("owned: smithing table, crossbow, lantern, smooth quartz slab, stone brick wall and mossy stone brick stairs, slab and wall copied from Village & Pillage")
