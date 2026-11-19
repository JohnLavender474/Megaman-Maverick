# Notes: mega-refactor-077c-elec-devil

## Decisions

- 2026-11-18 (owner): signature transformation: **Spark Serpent**. The body cells join into a chain of cubes, linked
  by arcs, that moves through the room. The eye cell is the head and the weak point. Gaps in the chain let the player
  pass.
- 2026-11-18 (owner): the serpent path comes from TMX objects, not from the room perimeter at runtime, so that the
  level design controls the flow.
- 2026-11-18 (owner): smaller attacks: **Live Wire** (an arc links consecutive flying pieces), **Eye Spread / Storm
  Rain** (mixed aims: lead, current, near random, wide random), **Circuit Floor** (pieces embed in the floor and a
  current runs along them).
- 2026-11-18 (owner): no Blackout attack. The fight room is already dark, the boss cells and flying pieces are the
  light sources, and Megaman is always lit.
- 2026-11-18 (owner): art: same design, polish. Horn prongs with an arc, a larger eye with a colour telegraph, bolder
  circuit traces with a current pulse, a claw hand, a cell-by-cell assemble and split, a death of several frames.
- 2026-11-18 (owner, deferred): serpent cubes and player shots. Bullets and buster shots are destroyed by a cube.
  `MoonScythe` and `Axe` cut through the cubes and become "electrified". This needs new state and animations in the
  projectile classes, so it is not in this branch yet (see `TODO.md`).
- 2026-11-18 (owner, art drafts in `temp/077c-elec-devil/art/` of the main checkout): horn variant A (curved, tips
  up); the body draft as is (cyan traces with a pulse, no X band, round eye with a slit pupil and a lid); the Spark
  Serpent body is orbs (as the flying cells); `appear` has 8 frames (0.06 s each). The other open choices of the 3
  `notes.md` files take the agents' defaults until a play test shows otherwise.

- 2026-11-18 (owner, from the `078` Wily Stage 1 art work): the boss's 2nd phase needs complete darkness. This
  replaces "the fight room is already dark" for phase 2 only. The boss-room generator becomes an entity that `078`
  adds: its core light and its coil-tip sparks send light events, and it has an on/off state ("no lights, no
  sparks"). `077c` turns the generator off when phase 2 starts. Whichever branch merges second wires the call (owner
  accepted the recommendation). Art draft: `temp/078-wily1-art/5-cave/generator/` in the main checkout.
- 2026-11-18 (owner): "complete darkness" in phase 2 means only the generator turns off. Megaman and the boss's
  parts stay light sources in both phases.
- 2026-11-18 (owner, play round 1): each Live Wire pair must be dodgeable by a crouch or a jump. Both pieces of a
  link stay in one band, rows 0 to 1 (jump over) or rows 2 to 5 (crouch under); a jump-over pair passes a player in
  at most 0.35 s (a full jump clears it for 0.51 s). Numbers: `temp/077c-live-wire-dodge/jump_sim.py`.
- 2026-11-18 (owner accepted the recommendation): the speed and launch ramp is one ramp per phase (phase 1 ramps as
  on `master`), not one ramp over the 2 bars.
- 2026-11-18 (owner accepted the recommendation): the Live Wire arc is blue/white in the 3 colors of the flying
  piece (`#0080FF`, `#80FFFF`, `#F0F0F0`), drawn as the draft segment `live_wire_arc.png` repeated along the line
  (last tile clipped, a frame offset per tile) with a `live_wire_cap.png` spark at each end, 1:1 like the piece. Not
  a stretched sprite. Review: `temp/077c-arc-art-review/` in the main checkout. Sources (for the step 7 credits):
  both strips are new pixels by Claude Fable 5.2, directed by the developer, inspired by the crackles in
  `ElecDevilBodyPiece.png` (author not recorded in the README; ask the owner).
- 2026-11-18 (owner): Eye Spread fires its 3 balls one after another, 0.1 s apart (`EYE_SPREAD_SHOT_GAP`), at the
  3 targets taken at the end of `CHARGE`, one `VOLT` sound per ball.
- 2026-11-18 (owner): the flying orb, the Live Wire arc and its caps are drawn at 2x (2 game px per art px), like
  Megaman and the boss body: orb 24x24 art, arc tile 16x8, cap 8x8.
- 2026-11-18 (owner): all ElecDevil art uses exact NES palette entries (the FCEUX table of Megaman's frames): blue
  `$11 #0070EC`, white `$30 #FCFCFC`, cyan `$3C #9CFCF0` (nearest to the old cyan; Megaman's `$2C #00E8D8` is the
  other option). This replaces the 3 colors in the arc decision above.
- 2026-11-18 (owner accepted the recommendation): the new body and transition art goes into play round 1, not step 7:
  `stand`, `charge`, `hand`, the 72 pieces, and the 8-frame `appear`, `turn_to_pieces` and `defeated`, snapped to
  NES by an art agent (Claude Fable 5.2; same map as the recolor, plus the eye flash `$38 #FCE4A0`). The ElecDevil
  frames move to a new atlas `Bosses4` (`TextureAsset.BOSSES_4`), because `appear` and `defeated` grow and `Bosses3`
  cannot be packed with `Basic` (it still fails by 1 sprite without ElecDevil). `appear` runs 8 frames of 0.05 s
  (0.4 s, the phase-2 `APPEAR_DUR`); `defeated` runs 8 frames once and holds the last. Not in yet: `hand_windup`,
  `hand_recoil` (need hand states) and the effect drafts of the later attacks.
- 2026-11-18 (owner): the round eye with a slit pupil and a lid was "too cute". New eye (Claude Fable 5.2, inspired by
  the star core of the original frames): a 4-point star core in a dark housing, a pulse in `stand`, white-hot in
  `charge`, and a glowing, pulsing eye in the new `shoot` strip (the 3 Eye Spread balls) and in `hand` (the hand
  attack shoots from the eye).
- 2026-11-19 (owner accepted the recommendations): **Eye patterns**, a pattern deck in the style of a colour-coded
  orb boss. Each `CHARGE` picks one pattern, never the previous one; the eye (`charge`, `shoot`) and the orbs take the
  pattern's colour: blue **Fan** (the 3 aimed balls), red **Ring** (a slow ring with one ball at the player), green
  **Stall** (balls fly out, stop, then dart at the player), yellow **Bounce** (phase 2 only; balls bank off the floor
  at the player). Phase 1 draws from blue, red, green.
- 2026-11-19 (owner accepted the recommendations): **Tether Claw** replaces the eye shots of `HAND`. A fixed windup
  (the claw opens and flashes), then the claw flies low and fast along the floor (jump over it), holds at the far end,
  and snaps back low (a 2nd jump). The tether is drawn with the Live Wire arc and does not hurt. The fixed windup and
  the high speed make the jump timing nearly the same at any distance (owner: a player beside the boss must also be
  able to dodge). Inspired by a claw midboss of an X1 stage.
- 2026-11-19 (owner): the Tether Claw is redesigned, and this replaces the jump dodge, the dive, the hold pocket and
  the harmless tether above. The tether looks like a damager, so it is one. The claw flies in a straight line from
  the hand to the far wall and back; the line and the claw hurt. The only dodge is to stand or crouch under the high
  end of the slope, near the boss's feet (standing safe up to about 4 tiles from the feet, crouching about 6, in the
  16-tile room). The windup shows a flickering guide line on the path and gives the time to run there from the wall:
  1.4 s (normal), 1.3 s (normal phase 2), 1 s (hard, a slide), 0.9 s (hard phase 2). The tether hitbox is 2 lines
  (Live Wire style), not rectangles, so it does not hit below the drawn line. Why: the old dive swept the column
  beside the boss up to 3.8 tiles, above the full-jump apex of 3.4, so a player at the feet could not dodge.
- 2026-11-19 (art agent recommendation, Claude Fable 5.2): the Fan eye is blue (`charge_blue`, `shoot_blue`); the
  orange eye of round 1 is gone. The 3 hand strips keep a bright eye. Yellow orb `$28`/`$38`. New pixels for the step 7
  credits: the colour strips, the claw strips and `claw.png`, by Claude Fable 5.2, directed by the developer.

- 2026-11-19 (owner play test; art agent recommendations, Claude Fable 5.2): the flying claw has the scale of the
  windup claw (`claw.png` 2 frames of 48x48, open talons, tether point (12, 24) on the back plug; hitbox 26x29 art px
  without the thumb spike). The front fist is a closed claw in `stand`, `charge_*`, `shoot_*`, `appear`,
  `turn_to_pieces`, `defeated` and the 2 hand cells of the pieces (column 4, rows 2 and 3). The back fist stays a fist
  (a blue claw on the blue dome reads badly). Drafts and checks: `temp/077c-claw-frames/art/` in the main checkout.
  New pixels by Claude Fable 5.2, directed by the developer, built on the live `ElecDevil` frames (step 7 credits).

- 2026-11-19 (owner, end of play round 2): ready to merge. Steps 5 (Spark Serpent) and 6 (Circuit Floor, Storm
  Rain) move to a new branch of item 077 after this merge (backlog candidate in `TODO.md`); phase 2 keeps `LAUNCH` as
  its only move attack until then (`TODO` at `pickMoveAttack`). Credits: the original ElecDevil art is by boberatu
  (already in the `README.md` boss list); the update line names Claude Fable 5.2, directed by the developer. No
  Diffity review: the merge runs the final test pass, the automated checks and the subagent review only.

## Overlap with other branches

- `078` (stage redesign; Wily Stage 1 art make-over, decision of 2026-11-18): the "power line" concept ends at the
  generator of the `ElecDevil` boss room. Both can touch the boss room of `WilyStage1_v2.tmx`. `078` is art only (the
  block geometry stays); this branch adds the serpent path objects. Agree the boss room art (walls, cables) with the
  `078` work.
- `078a` scan (2026-11-18, `078a` at its play test, 6 commits): (1) `WilyStage1_v2.tmx` object ids collide: `078a`
  uses 499 to 506 (499, 500 are `AnimatedBackground`, 506 is `WilyGenerator`), `077c` uses 499, 500 for
  `SerpentPathA`/`B` and `serpent_paths="499,500"`. A text merge can keep both sets with no conflict marker except
  `nextobjectid`. The branch that merges second renumbers its objects and checks the map for duplicate ids.
  (2) `WilyGenerator` (`entities/decorations/`, object 506, key 1) is at room x 7.5 on the floor, 9x9 tiles, drawn
  behind the bodies; `078a` NOTES: `077c` finds it with `MegaGameEntities.getOfTag` and calls `powerDown()`.
  `RefactorTest077` has no generator, so the phase-2 darkness can be checked only in `WILY_STAGE_1` until the test
  room gets one (needs the `Decorations2` atlas from `078a`). (3) The `078a` stage palette is its own
  (`temp/078-wily1-art/STYLE.md`, not NES); its cable light `#2FD2FF`/`#BFF4FF` is close to the arc and orb cyan.
  (4) No shared atlas or Kotlin file; `README.md` credits will both add lines.
- `078a` merged (2026-11-18); `077c` took `master` the same day (owner chose before the round-1 play test). The
  serpent paths are now ids 508 and 509 in `WilyStage1_v2.tmx` (`nextobjectid` 510); `047k` also adds 499 and 500,
  so it renumbers past 509. `Projectiles1` repacked with `GreenPelletBlast/wily/*`. `ElecDevil` calls
  `WilyGenerator.powerDown()` on each generator (by tag) at the phase-2 start. `RefactorTest077` has a generator in
  `elec_devil_boss` (object 26, layer `decorations`, same room position as in the stage).
- `118d` (boss fight routines) lists `ElecDevil`. A later move of the fight into routines must keep the new attacks.
- Merge check (2026-11-19): `078c` (active) also changes `Projectiles1.png` and `Projectiles1.txt` (3 new frames in
  `frames/Projectiles1/`). Both sides only add frames, and both packed with `Basic`, so `078c` resolves the
  conflict at its next take of `master` by a repack from the frames folder. `compactor-level` also adds a
  `LevelDefinition` entry and a `LevelCompletionScreensTest` row (list additions). No other overlap.
