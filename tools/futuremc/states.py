"""Writes assets/netherupdatebackport/migration/futuremc_states.json: what each metadata value of the other
mod's 1.16 blocks (and of the blocks this mod copies from Village & Pillage when that mod is absent) meant, in the
format of Backport Core's recorded state tables (`/futuremc dump`).

Read from that mod's 0.2.21 jar (javap and Vineflower, G:/MCDownloads/futuremc-0.2.21), and checked against a
`/futuremc dump` on a real server with it installed:
  * `soul_fire_lantern` is its LanternBlock: `hanging` is meta 1, standing meta 0.
  * `soul_fire_torch` extends 1.12.2's BlockTorch: facing east 1, west 2, south 3, north 4, up 5.
  * `chain`, `soul_soil`, `netherite_block`, `ancient_debris` have no properties (its chain is vertical only).
  * `lantern`, `smithing_table` (facing in 2-5, horizontal only), `stone_brick_wall` and `mossy_stone_brick_wall`
    as Village & Pillage recorded them (its migration/futuremc_states.json).
Its blackstone set and nether gold ore register only in a deobfuscated (development) game, so no world has them.

    python -I tools/futuremc/states.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

WALL = {"east": "false", "north": "false", "south": "false", "up": "false", "west": "false"}
states = {
    "ancient_debris": {"0": {}},
    "chain": {"0": {}},
    "netherite_block": {"0": {}},
    "soul_soil": {"0": {}},
    "soul_fire_lantern": {"0": {"hanging": "false"}, "1": {"hanging": "true"}},
    "soul_fire_torch": {str(meta): {"facing": facing}
                        for meta, facing in ((1, "east"), (2, "west"), (3, "south"), (4, "north"), (5, "up"))},
    "lantern": {"0": {"hanging": "false"}, "1": {"hanging": "true"}},
    "smithing_table": {str(meta): {"facing": facing}
                       for meta, facing in ((2, "north"), (3, "south"), (4, "west"), (5, "east"))},
    "stone_brick_wall": {"0": dict(WALL)},
    "mossy_stone_brick_wall": {"0": dict(WALL)},
}
assets.write_json("migration/futuremc_states.json", states)
print("futuremc_states.json: %d blocks" % len(states))
