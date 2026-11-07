"""Stream V assets: the nether biomes' names.

    python tools/biome/generate.py

The particles use vanilla's generic_0 sprite (1.12.2's particles.png index 0), so no textures are copied.
Nether wastes is vanilla's minecraft:hell; its name comes in under our key too, as 1.12.2 biome names are
plain text that this mod sets from the language file.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID
BIOMES = ["nether_wastes", "crimson_forest", "warped_forest", "soul_sand_valley", "basalt_deltas"]

lines = ["biome.%s.%s=%s" % (M, b, assets.name("biome.minecraft.%s" % b)) for b in BIOMES]
assets.merge_lang("biome", sorted(lines))
print("biome: %d names" % len(lines))
