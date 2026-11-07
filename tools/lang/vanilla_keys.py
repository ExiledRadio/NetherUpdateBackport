"""Maps each en_us.lang key of this mod to the modern Minecraft key it takes its name from, if any.

Rule 4 of the family plan: vanilla names come from Mojang's language files and are never translated by us.
This writes tools/lang/vanilla_keys.json:

    {"vanilla": {"tile.netherupdatebackport.kelp.name": "block.minecraft.kelp", ...},
     "ours":    ["itemGroup.netherupdatebackport", "config.netherupdatebackport.kelp", ...]}

"vanilla" keys get their text in every language from the language files of the Minecraft version that
has that key; "ours" are translated by us. A key is "vanilla" only when its English equals the modern
English exactly, so a name we changed is never silently replaced. Rerun after any generator:

    python tools/lang/vanilla_keys.py
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID
SPECIAL = {
    # 1.13 renamed vanilla's own wooden trapdoor and pressure plate once the other woods had theirs.
    "tile.trapdoor.name": "block.minecraft.oak_trapdoor",
    "tile.pressurePlateWood.name": "block.minecraft.oak_pressure_plate",
}


def candidates(key):
    if key in SPECIAL:
        return [SPECIAL[key]]
    parts = key.split(".")
    if key.startswith("tile.%s." % M) and key.endswith(".name"):
        name = ".".join(parts[2:-1])
        return ["block.minecraft." + name, "item.minecraft." + name]
    if key.startswith("item.%s." % M) and key.endswith(".name"):
        name = ".".join(parts[2:-1])
        return ["item.minecraft." + name, "block.minecraft." + name]
    if key.startswith("subtitles.%s." % M):
        return ["subtitles." + key[len("subtitles.%s." % M):]]
    return []


def main():
    lang = os.path.join(assets.ASSETS, "lang", "en_us.lang")
    vanilla, ours, mismatched = {}, [], []
    for line in open(lang, encoding="utf-8").read().splitlines():
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, text = line.split("=", 1)
        found = None
        for cand in candidates(key):
            if cand in assets.VANILLA_LANG:
                found = cand
                break
        if found and assets.VANILLA_LANG[found] == text:
            vanilla[key] = found
        else:
            if found:
                mismatched.append("%s: ours %r, vanilla %s %r" % (key, text, found, assets.VANILLA_LANG[found]))
            ours.append(key)
    out = os.path.join(os.path.dirname(os.path.abspath(__file__)), "vanilla_keys.json")
    with open(out, "w", encoding="utf-8", newline="\n") as f:
        f.write(json.dumps({"vanilla": vanilla, "ours": ours}, indent=2, sort_keys=True) + "\n")
    for m in mismatched:
        print("DIFFERS", m)
    print("%d vanilla names, %d of our own" % (len(vanilla), len(ours)))


if __name__ == "__main__":
    main()
