"""Game Drops Backport's request 1: 26.1's baby textures from the 26.3 client jar, for babies/client/NetherBabies.

Run: python -I tools/babies/models.py   (idempotent; copies to textures/entity/... in our namespace)
"""
import os
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "netherupdatebackport", "textures", "entity")
JAR = "G:/MCDownloads/minecraft-26.3/client.jar"

TEXTURES = [
    "hoglin/hoglin_baby",
    "hoglin/zoglin_baby",
    "piglin/piglin_baby",
    "piglin/zombified_piglin_baby",
    "strider/strider_baby",
    "strider/strider_cold_baby",
] + ["equipment/humanoid_baby/%s" % m for m in ("leather", "leather_overlay", "chainmail", "iron", "gold", "diamond",
                                               "netherite")]


def main():
    with zipfile.ZipFile(JAR) as jar:
        for t in TEXTURES:
            data = jar.read("assets/minecraft/textures/entity/%s.png" % t)
            path = os.path.join(OUT, *(t + ".png").split("/"))
            os.makedirs(os.path.dirname(path), exist_ok=True)
            with open(path, "wb") as f:
                f.write(data)
            print(t)


if __name__ == "__main__":
    main()
