"""Stream M assets: the Pigstep disc, 1.16's nether music and the nether biomes' ambience.

    python tools/music/generate.py

Sounds come from the 1.18 asset index (tools/assets.py). A file 1.12.2 already ships, byte for byte (the four old
nether tracks, music/game/nether/nether1-4), is not copied: our sounds.json names it as minecraft:..., which every
1.12.2 game has in its own asset index.
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

INDEX_112 = json.load(open(assets.INSTALL + "/assets/indexes/1.12.json", encoding="utf-8"))["objects"]

BIOMES = ("nether_wastes", "crimson_forest", "warped_forest", "soul_sand_valley", "basalt_deltas")


def copy_sound(event):
    """assets.copy_sound, except that files identical to 1.12.2's stay vanilla's."""
    spec = json.loads(json.dumps(assets.vanilla_sound(event)))
    reused = []
    for sound in spec["sounds"]:
        entry = sound if isinstance(sound, dict) else {"name": sound}
        if entry.get("type") == "event":
            continue
        path = entry["name"].rpartition(":")[2]
        key = "minecraft/sounds/%s.ogg" % path
        if key in INDEX_112 and INDEX_112[key]["hash"] == assets.INDEX[key]["hash"]:
            reused.append(path)
    ours = assets.copy_sound(event)
    for i, sound in enumerate(ours["sounds"]):
        entry = sound if isinstance(sound, dict) else {"name": sound}
        path = entry["name"].rpartition(":")[2]
        if path in reused:
            os.remove(os.path.join(assets.ASSETS, "sounds", *(path + ".ogg").split("/")))
            entry["name"] = "minecraft:" + path
            ours["sounds"][i] = entry if len(entry) > 1 else entry["name"]
    return ours


events = {}
for biome in BIOMES:
    events["music.nether." + biome] = copy_sound("music.nether." + biome)
    for part in ("loop", "mood", "additions"):
        events["ambient.%s.%s" % (biome, part)] = copy_sound("ambient.%s.%s" % (biome, part))
events["music_disc.pigstep"] = copy_sound("music_disc.pigstep")
assets.merge_sounds("music", events)

# Removing a reused file can leave an empty folder behind.
for root, dirs, files in os.walk(os.path.join(assets.ASSETS, "sounds", "music"), topdown=False):
    if not os.listdir(root):
        os.rmdir(root)

assets.item_generated("music_disc_pigstep", assets.texture("item/music_disc_pigstep"))

lines = [
    "item.%s.music_disc_pigstep.name=%s" % (assets.MODID, assets.name("item.minecraft.music_disc_pigstep")),
    # ItemRecord's own description key: item.record.<name>.desc
    "item.record.%s.pigstep.desc=%s" % (assets.MODID, assets.name("item.minecraft.music_disc_pigstep.desc")),
]
lines += assets.subtitle_lines(events)
assets.merge_lang("music", sorted(set(lines)))
print("music: %d sound events, %d files" % (len(events), sum(
    1 for spec in events.values() for s in spec["sounds"]
    if not (s if isinstance(s, str) else s["name"]).startswith("minecraft:"))))
