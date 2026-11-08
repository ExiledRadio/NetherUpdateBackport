# In-game checklist

Things only a person in a real game can confirm. Each line says what to look at and what vanilla does.

## Lf. 1.16's look and feel (with Backport Core 1.1.0)

Self-tests: `gradlew runServer -Pselftest=lookfeel` (registration, assets, splash pool, options, hooks, patches
P3/P4 on MCP and SRG classes, lang) and `gradlew runClient -Pselftest=client` (title screen, log only).

- [ ] **Title screen**: the nether panorama (crimson forest and basalt, from 1.16.5) turns slowly, sharp, with no
      darkening gradient over it (1.16's overlay is clear). With Update Aquatic, Village & Pillage or Buzzy Bees
      also installed, it is still the nether one (Core's `presentation.panorama = LATEST`).
- [ ] **Splashes**: over several launches, 1.16 ones show ("Moderately attractive!", "Music by Lena Raine!",
      "Zoglin!?"), never "Sexy!" or "Getting ready to show!".
- [ ] **Singleplayer** with an empty `saves` folder opens Create New World; Cancel goes back to the title screen.
- [ ] **World loading**: "Reading world data..." first, then the chunk-status grid with a percentage (Core's 1.16
      revision).
- [ ] **Video Settings**: after Entity Shadows come Distortion Effects, Entity Distance (50% to 500%, steps of 25%)
      and FOV Effects. Entity Distance at 50% makes mobs vanish at half the usual distance; 500% shows them far out.
- [ ] **Accessibility Settings** (from Options): ends with Distortion Effects, then FOV Effects; moving either one
      moves the same slider on Video Settings.
- [ ] **Distortion Effects**: stand in a nether portal at 100% (full wobble), 50% (a quarter), 0% (none). With
      Nausea (a pufferfish) below 100%, a green swirl overlay appears instead of the lost wobble.
- [ ] **FOV Effects**: sprint at 100% (vanilla zoom), 25% (half the change), 0% (no change); also drawing a bow.
- [ ] **F3+F4** (cheats on): the game mode switcher opens at the screen's center with four slots; F4 moves the
      selection; letting go of F3 switches mode. Without cheats: "[Debug]: Unable to open game mode switcher; no
      permission".
- [ ] **F3+N**: survival to spectator, then back to survival (the previous mode, once a change was seen this
      session). F3+Q lists "F3 + F4 = Open game mode switcher" and the reworded F3+N line.
- [ ] **Reworded vanilla text**: death screen "You Died!" / "Title Screen", creative tab "Saved Hotbars",
      Controls "Swap Item With Offhand", server list "Direct Connection" and "Incompatible version!", the mount
      message "Press Shift to Dismount", and in another language (e.g. Español) the same from Mojang's files.
- [ ] Menu music is unchanged (1.16 kept menu1 to menu4).

## Lf2. 1.16's pieces in Backport Core's finished presentation module (Core 1.1.0)

Self-tests: `runServer -Pselftest=lookfeel` (assets, warning keys, vanilla text in 18 languages) and
`runClient -Pselftest=client` (Core's pack serves the logo, 1.16 text through Core, music and F3 steps, the guide).

- [ ] **Startup**: as loading ends, the red (0xEF323D) Mojang Studios screen with the white logo and a full white
      bar shows for a second, then fades over the title screen. 1.16.5's logo halves line up.
- [ ] **Title logo**: 1.16.5's redrawn "Minecraft" logo; the title screen fades in (logo, splash, buttons after the
      panorama) on the first start. The Accessibility icon sits right of Options/Quit and opens Accessibility Settings.
- [ ] **Buttons**: white labels, white outline when hovered (1.15's widgets, kept in 1.16).
- [ ] **Multiplayer** from the title screen: "Caution: Third-Party Online Play" first, Proceed / Back and the
      check box; ticked, it never shows again.
- [ ] **Accessibility Settings**: ends with Distortion Effects and FOV Effects (also on Video Settings, the same
      value on both), "OFF" at zero, a tooltip on hover; Accessibility Guide bottom left beside Done (1.16.4).
      Line Spacing and Chat Delay change the chat (Core's); "[+N pending lines]" while held back.
- [ ] **F3**: no "Looking at" line; "Targeted Block: x, y, z" heading on the right; in singleplayer
      "SC: n, M: a, C: b, A: c, W: d" under Local Difficulty.
- [ ] **Music**: the nether biomes' tracks; switching game mode does not cut the music; a new dimension stops it.
      With Update Aquatic too: under water in an ocean the underwater tracks.
- [ ] **Text**: "You Died!", "Saved Hotbars", "Swap Item With Offhand" (1.16's wording through Core, whatever else
      is installed).
