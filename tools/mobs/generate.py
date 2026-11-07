"""Stream Z assets: the zombified piglin's texture, sounds and name.

    python tools/mobs/generate.py

Vanilla's zombie pigman stays the same entity (minecraft:zombie_pigman); only its name changes, through
1.12.2's own key entity.PigZombie.name. Its loot table is already 1.16's (the same three pools).
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

assets.texture("entity/piglin/zombified_piglin")

events = {}
for e in ("ambient", "angry", "death", "hurt"):
    events["entity.zombified_piglin." + e] = assets.copy_sound("entity.zombified_piglin." + e)
assets.merge_sounds("mobs", events)

lines = ["entity.PigZombie.name=%s" % assets.name("entity.minecraft.zombified_piglin")]
lines += assets.subtitle_lines(events)
assets.merge_lang("mobs", sorted(set(lines)))
print("mobs: %d sound events" % len(events))
