# In-game checklist

Things only a person in a real game can confirm. Each line says what to look at and what vanilla does.

## Lf. 1.16's look and feel (with Backport Core 1.2.0)

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
