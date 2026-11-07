# Stream R: striders

Package `strider`. Config `striders` (`enabled`, `spawn`). Entity id 20.

## Changelog section

*   **Striders** walk on lava across the nether. Away from lava they turn purple, shiver and slow down; water and rain
    hurt them. Feed them warped fungus to breed them; they follow warped fungus and warped fungus on a stick.
*   Saddle one and ride it holding a **warped fungus on a stick** (a fishing rod and a warped fungus) to steer; use the
    stick to boost it (one of 100 uses). Some spawn ridden by a zombified piglin, some carrying a baby.
*   Striders drop string (and their saddle).
*   Advancements *This Boat Has Legs* and *Feels Like Home* (50 blocks on overworld lava).

## How it works

*   1.16.5's `StriderEntity`: 20 health, speed 0.175, fire-immune, water-sensitive; cold away from lava (speed x0.66,
    ridden at 0.23 of its speed instead of 0.55); panic 1.65, tempt 1.4, walk to lava within 8 blocks 1.5, breed,
    follow parent 1.1, wander, look. Jockeys: 1 in 30 saddled with a zombified piglin holding the stick (vanilla's
    `minecraft:zombie_pigman`), 1 in 10 carrying a baby.
*   **Standing on lava**, which 1.12.2 has no way to express: after each move, a strider whose feet are in a lava
    block with no lava above is held on the half-block surface (1.16's stable lava shape) and counts as on the ground;
    it is not "in lava" there, so it walks at land speed. Under lava it rises (1.16's `floatStrider`). Lava costs
    nothing on paths and is its favorite place.
*   **Riding**: vanilla's pig riding (`EntityPig.travel`) with the strider's speeds; the boost is the carrot on a
    stick's (140-980 ticks, up to +115%), synced to the client through the entity's data.
*   **Spawning** on lava in the nether: 1.16's weight 60 in groups of 1-2 as creatures, through a Forge spawn
    placement type "in lava" (1.12.2 has only ground, water and air) and 1.16's spawn rule (lava up to air). The
    strider's own collision check ignores the lava it spawns in.
*   Client: 1.16.5's `StriderModel` (body, legs, six bristles and their animation), warm and cold textures, the saddle
    as a second model, shivering while cold, half size for a baby.

## Tested headlessly

`-Pselftest=striders`: **pass=130 fail=0**; all streams: **pass=7097 fail=0**; `check_assets.py` 0 errors.

*   Registered with a spawn egg; the stick lasts 100 uses; 20 health, speed 0.175, fire-immune.
*   Dropped onto a 4-deep lava pool, after 3 seconds it is unharmed, stands exactly on the surface, on the ground,
    warm and not swimming; one placed at the bottom floats up more than 2 blocks.
*   On stone it is cold and at 0.66 speed; standing in water hurts it.
*   A saddle goes on (used up, saved in NBT); an empty hand mounts it; it steers only with the stick; a boost costs
    one use and a second waits; warped fungus breeds it.
*   The spawn placement accepts lava and refuses air; lava with air above is a spawn spot, under stone not; in 300
    spawns, a few zombified piglin jockeys (holding the stick, saddled) and about one in ten carrying a baby.
*   2-5 string; the stick's recipe; both advancements load.

## Not tested

*   Anything visual: the model, animation, textures, shivering; sounds playing.
*   A player actually riding and steering over a lava lake (needs a client), and natural spawning over time.
