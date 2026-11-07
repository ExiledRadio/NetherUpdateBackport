"""Stream X assets: Mojang's 1.16.5 structure files, the bastion pools and processor lists, the chests' loot tables.

    python tools/structure/generate.py

* structures/bastion/**, structures/ruined_portal/**, structures/nether_fossils/**: the 1.16.5 client jar's templates,
  unchanged (Backport Core reads them as they are).
* worldgen/template_pool/bastion/** and worldgen/processor_list/*.json: 1.16.5's built-in pools and lists as data-pack
  JSON, dumped with Mojang's own codecs by Backport Core's tools/vanilla-oracle (DumpPools + pack_pools.py); copied from
  Core's self-test assets, where that tool put them.
* loot_tables/chests/*.json: 1.16.5's five tables in 1.12.2's format. Current Java changed no weight; it added smithing
  templates and spears (not 1.16 content) and, to ruined portals, a pool of lodestones, which is kept. Items another
  stream or mod adds are named through this mod's item_exists / set_item extensions (see StructureLoot.java).
* The oracle's answers (tools/structure/oracle) for the self-test, when the oracle has been run.
"""
import gzip
import json
import os
import shutil
import sys
import zipfile

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID
JAR = zipfile.ZipFile(assets.INSTALL + "/versions/1.16.5/1.16.5.jar")
CORE_WORLDGEN = "E:/MincraftModding/Workspaces/BackportCore-main/src/selftest/resources/assets/backportcore_selftest/worldgen"
ORACLE_OUT = "G:/MCDownloads/minecraft-1.16.5-gen/nub-structure/out"
SELFTEST = os.path.join(assets.ROOT, "src", "selftest", "resources", "assets", M, "oracle")

# ---- templates
templates = 0
for name in JAR.namelist():
    for folder in ("bastion/", "ruined_portal/", "nether_fossils/"):
        prefix = "data/minecraft/structures/" + folder
        if name.startswith(prefix) and name.endswith(".nbt"):
            assets.write("structures/" + name[len("data/minecraft/structures/"):], JAR.read(name))
            templates += 1

# ---- pools and processor lists
pools = 0
for root, _, files in os.walk(CORE_WORLDGEN):
    for f in files:
        src = os.path.join(root, f)
        rel = os.path.relpath(src, CORE_WORLDGEN).replace("\\", "/")
        if rel.startswith("template_pool/bastion/") or rel.startswith("processor_list/"):
            with open(src, "rb") as fh:
                assets.write("worldgen/" + rel, fh.read())
            pools += 1

# ---- loot tables
# 1.16 names 1.12.2 has under another name.
RENAMED = {"minecraft:glistering_melon_slice": ("minecraft:speckled_melon", 0),
           "minecraft:enchanted_golden_apple": ("minecraft:golden_apple", 1)}
# 1.16 items this mod's streams add, under the same path.
OURS = {"netherite_scrap", "netherite_ingot", "ancient_debris", "lodestone", "gilded_blackstone", "crying_obsidian",
        "chain", "crimson_nylium", "crimson_fungus", "crimson_roots", "piglin_banner_pattern", "music_disc_pigstep"}
# Items owned elsewhere: the owner's first, then this mod's copy, then the mods the owner steps aside for.
OWNED = {"crossbow": ["villagerbackport:crossbow", M + ":crossbow", "futuremc:crossbow", "crossbows:crossbow"],
         "bell": ["villagerbackport:bell", "futuremc:bell"]}
ENCHANTMENTS = {"minecraft:soul_speed": M + ":soul_speed"}


def by_name(items):
    return [{"condition": M + ":item_exists", "items": items}], [{"function": M + ":set_item", "items": items}]


def functions(fs):
    out = []
    for f in fs:
        f = dict(f)
        name = f["function"].replace("minecraft:", "")
        if name == "set_count" and f["count"] == 1:
            continue
        if name == "enchant_randomly" and f.get("enchantments"):
            out.append({"function": M + ":enchant_randomly", "enchantments": [ENCHANTMENTS.get(e, e) for e in f["enchantments"]]})
            continue
        for key in ("count", "damage"):
            if isinstance(f.get(key), dict):
                f[key] = {"min": f[key]["min"], "max": f[key]["max"]}
        f["function"] = name
        out.append(f)
    return out


def entry(e, used):
    kind = e.get("type", "minecraft:item").replace("minecraft:", "")
    weight = e.get("weight", 1)
    if kind == "empty":
        base = "empty"
        name = base
        n = 2
        while name in used:
            name = "%s_%d" % (base, n)
            n += 1
        used.add(name)
        return {"type": "empty", "weight": weight, "entryName": name}
    item = e["name"]
    path = item.split(":", 1)[1]
    fs = functions(e.get("functions", []))
    conditions = []
    if item in RENAMED:
        to, data = RENAMED[item]
        out = {"type": "item", "name": to, "weight": weight}
        if data:
            fs = [{"function": "set_data", "data": data}] + fs
    elif path in OURS or path in OWNED:
        conditions, first = by_name(OWNED.get(path, [M + ":" + path]))
        out = {"type": "item", "name": "minecraft:stick", "weight": weight}
        fs = first + fs
    else:
        out = {"type": "item", "name": item, "weight": weight}
    base = path
    name = base
    n = 2
    while name in used:
        name = "%s_%d" % (base, n)
        n += 1
    used.add(name)
    out["entryName"] = name
    if conditions:
        out["conditions"] = conditions
    if fs:
        out["functions"] = fs
    return out


def rolls(r):
    return {"min": r["min"], "max": r["max"]} if isinstance(r, dict) else r


def table(name, extra_pools=(), event=None):
    data = json.loads(JAR.read("data/minecraft/loot_tables/chests/%s.json" % name).decode("utf-8"))
    out = []
    for i, p in enumerate(list(data["pools"]) + list(extra_pools)):
        used = set()
        pool = {"name": "pool%d" % i, "rolls": rolls(p["rolls"]), "entries": [entry(e, used) for e in p["entries"]]}
        out.append(pool)
    if event:
        out.append({"name": "advancement", "rolls": 1, "entries": [{"type": "empty", "weight": 1, "entryName": "none"}],
                    "conditions": [{"condition": M + ":advancement_event", "event": event}]})
    assets.write_json("loot_tables/chests/%s.json" % name, {"pools": out})


for t in ("bastion_bridge", "bastion_hoglin_stable", "bastion_other", "bastion_treasure"):
    table(t, event="loot_" + t)
# Today's ruined portals also hold lodestones (1 in 3 chests, one or two).
table("ruined_portal", extra_pools=[{"rolls": 1, "entries": [
    {"type": "minecraft:empty", "weight": 1},
    {"type": "minecraft:item", "name": "minecraft:lodestone", "weight": 2,
     "functions": [{"function": "minecraft:set_count", "count": {"min": 1, "max": 2}}]}]}])

# ---- the oracle's answers, for the self-test
oracle = 0
if os.path.isdir(ORACLE_OUT):
    os.makedirs(SELFTEST, exist_ok=True)
    for f in os.listdir(SELFTEST):
        os.remove(os.path.join(SELFTEST, f))
    for f in os.listdir(ORACLE_OUT):
        src = os.path.join(ORACLE_OUT, f)
        if f.endswith(".gz"):
            shutil.copyfile(src, os.path.join(SELFTEST, f))
        else:
            with open(src, "rb") as i, gzip.open(os.path.join(SELFTEST, f + ".gz"), "wb") as o:
                o.write(i.read())
        oracle += 1

print("structure: %d templates, %d pool and processor files, 5 loot tables, %d oracle files" % (templates, pools, oracle))
