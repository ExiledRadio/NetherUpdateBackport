"""Writes the en_us.lang lines for every @Config field and category from ModConfig.java.

Rule 4: every config name and comment is a lang key. Each field's @Config.LangKey gets a name line (the
field name, split into words, or NAMES below) and a .tooltip line (its @Config.Comment, joined). Category
names come from CATEGORIES. The lines go in the '# --- config ---' block of en_us.lang.

    python tools/config_lang.py
"""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import assets  # noqa: E402

SRC = os.path.join(assets.ROOT, "src", "main", "java", "com", "exiledradio", assets.MODID, "ModConfig.java")

CATEGORIES = {
    "stone": "Blackstone and Basalt",
    "blocks": "Smaller Blocks",
    "fungus": "Crimson and Warped",
    "soul": "Soul Fire",
    "netherite": "Netherite",
    "hoglins": "Hoglins and Zoglins",
    "piglins": "Piglins",
    "striders": "Striders",
    "mobs": "Mob Changes",
    "music": "Music and Sounds",
    "changes": "Smaller Changes",
    "biomes": "Nether Biomes",
    "structures": "Nether Structures",
    "compat": "Other Mods",
}

# Field names that read badly split into words.
NAMES = {
    "futureMcDuplicates": "Duplicate Blocks",
}


def words(field):
    if field in NAMES:
        return NAMES[field]
    parts = re.sub(r"([A-Z])", r" \1", field).split()
    return " ".join([parts[0].capitalize()] + [p.lower() for p in parts[1:]])


def strings(arg):
    """@return the joined text of a Java string literal or {"a", "b"} array of them."""
    return " ".join(m.encode("utf-8").decode("unicode_escape") for m in re.findall(r'"((?:[^"\\]|\\.)*)"', arg))


def main():
    text = open(SRC, encoding="utf-8").read()
    lines = []
    for key, name in CATEGORIES.items():
        lines.append("config.%s.%s=%s" % (assets.MODID, key, name))
    # Each annotated field: a run of annotations, then 'public <type> <name> ='.
    for block in re.finditer(r"((?:\s*@Config\.\w+(?:\((?:[^()]|\([^()]*\))*\))?)+)\s*public\s+(?!static)[\w<>\[\]]+\s+(\w+)\s*=",
                             text):
        annotations, field = block.group(1), block.group(2)
        key = re.search(r'@Config\.LangKey\("([^"]+)"\)', annotations)
        comment = re.search(r"@Config\.Comment\(((?:[^()]|\([^()]*\))*)\)", annotations, re.S)
        if not key:
            sys.exit("ModConfig.%s has no @Config.LangKey" % field)
        lines.append("%s=%s" % (key.group(1), words(field)))
        if comment:
            lines.append("%s.tooltip=%s" % (key.group(1), strings(comment.group(1))))
    assets.merge_lang("config", lines)
    print("config: %d lines" % len(lines))


main()
