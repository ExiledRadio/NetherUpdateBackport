"""Blockstates, recipes and smelting ported from modern files, for every stream's generator.

Lives beside assets.py (which it builds on): blockstates from 1.18 when their properties match 1.12.2's,
or from 1.12.2's own vanilla blockstate with the model names swapped; 1.18's crafting recipes rewritten
in 1.12.2's JSON format with a condition naming the items they need; 1.18's smelting recipes as entries
for common/Smelting.
"""
import json
import os

import assets

MODID = assets.MODID


def our_model(value):
    """'minecraft:block/x' (a 1.18 blockstate model name) -> ported, as our 1.12 blockstate name 'mod:x'."""
    ns, _, path = value.rpartition(":")
    if (ns or "minecraft") != "minecraft":
        return value
    if not path.startswith("block/"):
        path = "block/" + path
    assets.port_model(path)
    return "%s:%s" % (MODID, path[len("block/"):])


def _rename_models(node, fn):
    if isinstance(node, dict):
        return {k: (fn(v) if k == "model" and isinstance(v, str) else _rename_models(v, fn)) for k, v in node.items()}
    if isinstance(node, list):
        return [_rename_models(v, fn) for v in node]
    return node


def port_blockstate(name, ours=None, drop=()):
    """Ports blockstates/<name>.json from the 1.18.2 jar when its property names and values are 1.12.2's
    too (axis; facing, half and shape of stairs; ...). Every model it names is ported. Variant keys that
    mention a property in `drop` (say 'waterlogged') are reduced to their other properties.
    """
    data = assets.jar_json("assets/minecraft/blockstates/%s.json" % name)
    data = _rename_models(data, our_model)
    if "variants" in data:
        variants = {}
        for key, value in data["variants"].items():
            parts = [p for p in key.split(",") if p and p.split("=")[0] not in drop]
            variants.setdefault(",".join(parts) or "normal", value)
        data["variants"] = variants
    assets.write_json("blockstates/%s.json" % (ours or name), data)


def old_blockstate(old, ours, models):
    """Writes our blockstate from 1.12.2's vanilla blockstates/<old>.json, whose properties are what our
    block has, with each 1.12.2 model name replaced by a 1.18 model (ported):
    models = {"stone_button": "polished_blackstone_button", ...}.
    """
    data = json.loads(assets.OLD_JAR.read("assets/minecraft/blockstates/%s.json" % old).decode("utf-8"))

    def fn(value):
        if value not in models:
            raise KeyError("no model given for 1.12.2's %r in %s" % (value, old))
        return our_model("minecraft:block/" + models[value])
    assets.write_json("blockstates/%s.json" % ours, _rename_models(data, fn))


def simple_blockstate(name, model=None):
    """A one-state block drawn with 1.18's block/<model or name>."""
    assets.write_json("blockstates/%s.json" % name,
                      {"variants": {"normal": {"model": our_model("minecraft:block/" + (model or name))}}})


def slab_blockstates(slab, full_model):
    """A two-block slab (see BlockNetherSlab): half=bottom|top for the slab, one state for its double."""
    assets.write_json("blockstates/%s.json" % slab, {"variants": {
        "half=bottom": {"model": our_model("minecraft:block/" + slab)},
        "half=top": {"model": our_model("minecraft:block/" + slab + "_top")}}})
    double = slab[:-len("_slab")] + "_double_slab"
    assets.write_json("blockstates/%s.json" % double,
                      {"variants": {"normal": {"model": our_model("minecraft:block/" + full_model)}}})


def wall_blockstate(wall):
    """1.12.2's wall multipart (boolean sides), drawn with 1.18's post and low side models."""
    post = our_model("minecraft:block/%s_post" % wall)
    side = our_model("minecraft:block/%s_side" % wall)
    parts = [{"when": {"up": "true"}, "apply": {"model": post}}]
    for face, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        apply = {"model": side, "uvlock": True}
        if y:
            apply["y"] = y
        parts.append({"when": {face: "true"}, "apply": apply})
    # A wall never computes "no post, no sides" (vanilla's has no part for it either), but the state
    # exists; give it the post so every state draws something.
    parts.append({"when": {"up": "false", "north": "false", "east": "false", "south": "false", "west": "false"},
                  "apply": {"model": post}})
    assets.write_json("blockstates/%s.json" % wall, {"multipart": parts})


def item_model(name):
    """Ports models/item/<name>.json (and the block model it shows) from 1.18."""
    return assets.port_model("item/" + name)


# 1.18 item ids -> 1.12.2 (item, data) for vanilla things the flattening renamed. Extend as needed.
FLAT = {
    "minecraft:nether_bricks": ("minecraft:nether_brick", 0),
    "minecraft:nether_brick": ("minecraft:netherbrick", 0),
    "minecraft:chiseled_quartz_block": ("minecraft:quartz_block", 1),
    "minecraft:quartz_pillar": ("minecraft:quartz_block", 2),
    "minecraft:charcoal": ("minecraft:coal", 1),
    "minecraft:coal": ("minecraft:coal", 0),
    "minecraft:stone_bricks": ("minecraft:stonebrick", 0),
    "minecraft:chiseled_stone_bricks": ("minecraft:stonebrick", 3),
    "minecraft:red_nether_bricks": ("minecraft:red_nether_brick", 0),
    "minecraft:nether_brick_slab": ("minecraft:stone_slab", 6),
    "minecraft:quartz_slab": ("minecraft:stone_slab", 7),
}
# 1.12.2 items with subtypes: a recipe must give their data value.
SUBTYPED = {"minecraft:" + n for n in (
    "quartz_block", "stone", "planks", "log", "log2", "wool", "coal", "dye", "stonebrick", "sandstone",
    "red_sandstone", "stone_slab", "stone_slab2", "wooden_slab", "prismarine", "sapling", "leaves", "leaves2",
    "red_flower", "double_plant", "sponge", "dirt", "sand", "cobblestone_wall", "golden_apple", "fish",
    "cooked_fish", "skull", "banner", "concrete", "stained_glass", "stained_glass_pane", "stained_hardened_clay",
    "carpet", "anvil", "tallgrass", "purpur_slab", "quartz")} - {"minecraft:quartz"}


def _subtyped(item_id):
    # Our slabs use ItemSlab, which has subtypes.
    return item_id in SUBTYPED or (item_id.startswith(MODID + ":") and item_id.endswith("_slab"))


# 1.18 tags -> a 1.12.2 ingredient
TAGS = {
    "minecraft:stone_tool_materials": [{"item": "minecraft:cobblestone"}],
    "minecraft:stone_crafting_materials": [{"item": "minecraft:cobblestone"}],
    "minecraft:planks": {"type": "forge:ore_dict", "ore": "plankWood"},
    "minecraft:coals": [{"item": "minecraft:coal", "data": 0}, {"item": "minecraft:coal", "data": 1}],
}


def _ingredient(ing, ours, used, tags):
    if isinstance(ing, list):
        out = []
        for i in ing:
            r = _ingredient(i, ours, used, tags)
            out.extend(r if isinstance(r, list) else [r])
        return out
    if "tag" in ing:
        if ing["tag"] not in tags:
            raise KeyError("no 1.12.2 ingredient for tag " + ing["tag"])
        value = json.loads(json.dumps(tags[ing["tag"]]))
        for v in (value if isinstance(value, list) else [value]):
            if v.get("item", "").startswith(MODID + ":"):
                used.add(v["item"])
        return value
    return item(ing["item"], ours, used)


def item(name, ours, used, count=None):
    """@return the 1.12.2 JSON for a 1.18 item id: ours when `ours` has its path, else vanilla's."""
    path = name.split(":", 1)[1]
    if path in ours:
        result = {"item": "%s:%s" % (MODID, path)}
        used.add(result["item"])
    elif name in FLAT:
        result = {"item": FLAT[name][0]}
        if FLAT[name][1]:
            result["data"] = FLAT[name][1]
    elif ("assets/minecraft/models/item/%s.json" % path) in assets.OLD_NAMES:
        result = {"item": name}
    else:
        raise KeyError("unknown 1.12.2 item for " + name)
    if "data" not in result and _subtyped(result["item"]):
        result["data"] = 0
    if count and count != 1:
        result["count"] = count
    return result


def port_recipe(name, ours, our_name=None, tags=None, result=None):
    """Ports data/minecraft/recipes/<name>.json (crafting only) from 1.18 into ours, with a condition
    listing every item of ours it uses or makes. `ours` is the set of registry paths this mod has; `tags`
    adds to or overrides TAGS for this recipe; `result` replaces the result item id.
    @return our recipe name, or None if it is not a crafting recipe.
    """
    data = assets.jar_json("data/minecraft/recipes/%s.json" % name)
    kind = data["type"]
    if kind not in ("minecraft:crafting_shaped", "minecraft:crafting_shapeless"):
        return None
    all_tags = dict(TAGS)
    all_tags.update(tags or {})
    used = set()
    out = {"type": kind}
    if "group" in data:
        out["group"] = "%s:%s" % (MODID, data["group"])
    if kind.endswith("shaped"):
        out["pattern"] = data["pattern"]
        out["key"] = {k: _ingredient(v, ours, used, all_tags) for k, v in data["key"].items()}
    else:
        out["ingredients"] = [_ingredient(i, ours, used, all_tags) for i in data["ingredients"]]
    out["result"] = item(result or data["result"]["item"], ours, used, data["result"].get("count"))
    out["conditions"] = [{"type": "%s:items" % MODID, "items": sorted(used)}]
    our_name = our_name or name
    assets.write_json("recipes/%s.json" % our_name, out)
    return our_name


def recipes_making(paths):
    """@return the names of 1.18's crafting and smelting recipes whose result is one of these paths."""
    found = []
    for n in assets.JAR.namelist():
        if not (n.startswith("data/minecraft/recipes/") and n.endswith(".json")):
            continue
        data = json.loads(assets.JAR.read(n).decode("utf-8"))
        res = data.get("result")
        res = res.get("item") if isinstance(res, dict) else res
        if isinstance(res, str) and res.split(":", 1)[1] in paths:
            found.append((n[len("data/minecraft/recipes/"):-len(".json")], data["type"]))
    return sorted(found)


def smelting_from(name, ours):
    """@return a smelting.json entry for 1.18's data/minecraft/recipes/<name>.json (smelting type)."""
    data = assets.jar_json("data/minecraft/recipes/%s.json" % name)
    used = set()
    ing = item(data["ingredient"]["item"], ours, used)
    res = item(data["result"], ours, used)
    entry = {"input": ing["item"], "output": res["item"], "xp": data.get("experience", 0)}
    if ing.get("data"):
        entry["input_meta"] = ing["data"]
    if res.get("data"):
        entry["output_meta"] = res["data"]
    return entry


def merge_smelting(section, entries):
    """Replaces this stream's section of smelting.json (read by common/Smelting)."""
    path = os.path.join(assets.ASSETS, "smelting.json")
    data = json.load(open(path, encoding="utf-8")) if os.path.exists(path) else {}
    data[section] = entries
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(json.dumps(data, indent=2, sort_keys=True) + "\n")


def name_lines(names, kind="tile"):
    """@return en_us.lang lines naming our blocks (tile) or items with 1.18's English names."""
    lines = []
    for n in names:
        key = ("block.minecraft." if kind == "tile" else "item.minecraft.") + n
        lines.append("%s.%s.%s.name=%s" % (kind, MODID, n, assets.VANILLA_LANG[key]))
    return lines


def event(name):
    """@return a criterion for common/Triggers' event, fired by our code."""
    return {"trigger": "%s:event" % MODID, "conditions": {"event": name}}


def obtain(*paths):
    """@return a 1.12.2 inventory_changed criterion for any of our items."""
    return {"trigger": "minecraft:inventory_changed",
            "conditions": {"items": [{"item": "%s:%s" % (MODID, p)} for p in paths]}}


def port_advancement(path, ours, criteria, requirements=None):
    """Ports data/minecraft/advancements/<path>.json from 1.18 into ours with 1.12.2 criteria.

    The icon becomes ours or 1.12.2's; title and description keep 1.18's translation keys (their English
    is returned as lang lines, so every language comes from Mojang's files); the parent stays vanilla's
    when 1.12.2 has it, else becomes ours under the same path. 1.16's rewards (recipe unlocks) are dropped.
    @return the en_us.lang lines for its title and description
    """
    data = assets.jar_json("data/minecraft/advancements/%s.json" % path)
    display = dict(data["display"])
    icon = display["icon"]
    display["icon"] = item(icon["item"], ours, set())
    out = {"display": display, "criteria": criteria}
    if requirements:
        out["requirements"] = requirements
    parent = data.get("parent")
    if parent:
        ppath = parent.split(":", 1)[1]
        if ("assets/minecraft/advancements/%s.json" % ppath) in assets.OLD_NAMES:
            out["parent"] = parent
        else:
            out["parent"] = "%s:%s" % (MODID, ppath)
    assets.write_json("advancements/%s.json" % path, out)
    lines = []
    for part in ("title", "description"):
        key = display[part]["translate"]
        lines.append("%s=%s" % (key, assets.VANILLA_LANG[key]))
    return lines
