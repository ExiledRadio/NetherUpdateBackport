# Stream H: hoglins and zoglins

Package `hoglin`. Config `hoglins` (`enabled`, `spawnWithoutBiomes`). Entity ids 10 (hoglin) and 11 (zoglin).

## Changelog section

*   **Hoglins**: big, tusked nether beasts that charge players and toss them into the air. They stay away from
    warped fungi, respawn anchors and nether portals, back off when piglins outnumber them, and the herd joins any
    fight one of them is in. Breed them with crimson fungus; babies follow the adults. Taken out of the nether they
    shake, and after 15 seconds turn into **zoglins**. They drop porkchops (cooked if they die burning) and leather.
*   **Zoglins**: undead hoglins that attack anything nearby but creepers and each other. They drop rotten flesh.
*   Spawn eggs for both. (Hoglins spawn in crimson forests, which come with the nether biomes; until then only from
    eggs and spawners, or anywhere in the nether with `hoglins.spawnWithoutBiomes`.)

## How it works

*   1.16.5's `HoglinEntity`, `HoglinTasks` (its brain), `ZoglinEntity` and `IFlinging`, with the brain's activities
    rebuilt as 1.12.2 AI tasks: swim; walk away from a repellent within 8 across / 4 up-down (and 1 chance in 200
    a tick of dropping a fight near one); flee the avoid target at 1.3x for 5-20 s; melee every 40 ticks; breed;
    babies follow (0.6); wander (0.4); look. Targets the nearest visible survival player in 16 blocks unless
    pacified, breeding or retreating. Hurt, it fights back and every adult hoglin that can see it joins
    (closer target wins); a baby retreats instead; hitting a piglin while adult piglins outnumber the adult
    hoglins makes the herd retreat from it (piglins read by registry name, stream P's).
*   Stats: 40 health, speed 0.3, knockback resistance 0.6, attack 6 (baby 0.5), 5 experience (baby 3), 1 in 5 spawns
    a baby. The fling: half the damage plus a random part (3-8), sideways push (1 - target's knockback resistance)
    x 0.2-0.7 turned by a random -10..10 the game reads as radians, upward up to half of it.
*   Zombification after 300 ticks outside the nether (not when immune or AI-less), keeping age, name and persistence;
    10 s Nausea. 1.12.2 has no conversion event for other mods to cancel.
*   Zoglins: same stats and fling, undead, target anything living but creepers, zoglins and creative/spectator players.
*   Client: 1.16.5's `BoarModel` boxes and animation for both, the baby scaling of 1.16's `AgeableModel`, and a
    converting hoglin's shaking (1.16's `isShaking`). Textures, 14 sound events and both loot tables from 1.18.

## Tested headlessly

`-Pselftest=all` (every stream): **pass=6931 fail=0**; `check_assets.py`: 0 errors, 594 sound files.

*   Both registered with spawn eggs; hoglin health, speed, knockback resistance, attack; crimson fungus (not wheat)
    to breed; a baby bites for 0.5.
*   An adult's hit lands for 3-8 and throws the pig; over 300 hits the damage covers exactly 3-8.
*   In the overworld a hoglin converts: after 15 s (305 updates) it is gone and one zoglin with Nausea stands there;
    an immune one does not convert; in the nether none converts; immunity is saved.
*   Repellents: none, not pacified; a warped fungus 5 blocks off is found and pacifies; a nether portal is found;
    nothing from 25 blocks.
*   Zoglin targets: not creepers or zoglins, yes pigs and hoglins; zoglins are undead with 40 health.
*   Loot: 50 hoglins give 100-200 porkchops; a burning one gives cooked porkchops; a zoglin 1-3 rotten flesh.

## Not tested

*   Anything visual: the model, its animation, baby proportions, shaking; sounds playing.
*   Live AI over time in a real world (pathing away from repellents, herd fights, retreating from piglins, which
    need stream P).
*   Natural spawning (stream V; or the `spawnWithoutBiomes` switch).
