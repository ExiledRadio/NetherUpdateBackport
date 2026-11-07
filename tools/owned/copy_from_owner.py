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
assets.merge_sounds("owned", {"block.smithing_table.use": event})

recipe = json.load(open(os.path.join(OWNER, "recipes/smithing_table.json"), encoding="utf-8"))
recipe["result"]["item"] = "%s:smithing_table" % M
recipe["conditions"] = [{"type": "%s:items" % M, "items": ["%s:smithing_table" % M]}]
assets.write_json("recipes/smithing_table.json", recipe)

lines = ["tile.%s.smithing_table.name=%s" % (M, assets.name("block.minecraft.smithing_table")),
         "container.upgrade=%s" % assets.name("container.upgrade")]
sub = event.get("subtitle")
if sub and sub.startswith("subtitles.%s." % M):
    lines.append("%s=%s" % (sub, assets.VANILLA_LANG.get(sub.replace(M + ".", "", 1), "Smithing Table used")))
assets.merge_lang("owned", sorted(lines))
print("owned: smithing table copied from Village & Pillage")
