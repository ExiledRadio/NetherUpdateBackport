"""Stream C assets: 1.16's sounds for nether blocks 1.12.2 already has (changes.BlockSounds).

Nether bricks' and wart blocks' events are stream U's and stream F's already; this adds the rest.

    python tools/changes/generate.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

events = {}
for group in ("netherrack", "nether_ore", "bone_block", "soul_sand"):
    for kind in ("break", "step", "place", "hit", "fall"):
        e = "block.%s.%s" % (group, kind)
        events[e] = assets.copy_sound(e)
for e in ("block.nether_wart.break", "item.nether_wart.plant"):
    events[e] = assets.copy_sound(e)
assets.merge_sounds("changes", events)

lines = assets.subtitle_lines(events)
if lines:
    assets.merge_lang("changes", lines)
print("changes: %d sound events, %d subtitle lines" % (len(events), len(lines)))
