# Plan: mega-refactor-081e-x-sabre

Ordered steps. Each step has its own check. Commit after each step. No game launch.

1. **Test room `sabre`.** A subagent adds the room after `ground_dash` in `RefactorTest081.tmx` with the
   `level-builder` skill: a flat floor, a wall for the wall slide, a ledge, a `SabreBlock`, a `Junkroid`, a
   `HoverGunner`, a `MetaWheelF`, a `MetaDridler`, spawn 6, painted tiles. Enemy properties copied from
   `X6Intro_16x14_v1.tmx`. Check: the level analyzer reads the room; `LaunchOptionsResolver` finds `--room sabre`.
2. **Input and the sabre part.** `MegamanXSabre` (state, one timer, the frame index, the kind) and the `B` actuator
   rule: `UP` held on the `B` press starts the slash and fires no shot; that press does not charge and its release
   fires nothing. Start gates (ground dash, air dash, wall grab, kick lock, damage, one at a time). X6 times in
   `MegamanXValues`. Check: tests for each rule; a mutation check.
3. **Body animations.** `sabre_floor` (art frames 1 to 7), `sabre_air`, `sabre_wall` keys from the sabre part's frame
   index, before `dash` in the key order; the wall strip flip. Check: animation key tests; `core:smokeTest`.
4. **The slash entity.** `MegamanXSabreSlash`: `IOwnable`, an `ABSTRACT` body with one `DAMAGER` fixture, its own
   sprite at `PLAYGROUND, 2`. The box per frame is the alpha bounds of the arc frame (B1), read once from the atlas
   regions; no box on empty frames. Placed by the sabre part each frame from the body sprite position; destroyed on
   every end. Sound `WHIP_V2_SOUND`. Check: tests for the box per frame and facing, the destroy on each end;
   `core:smokeTest`.
5. **Moves and cancels (B3).** Floor: stop, no turn, jump cancel after the cancel time. Air: air control and turn,
   a landing or a wall slide switches the kind (owner, 2026-11-09), no air dash, no jump cut. Wall: hold still,
   jump = wall kick. Damage ends every slash. Check: tests for each rule; Megaman's tests unchanged.
6. **Damage entries.** `X6Damage` takes the sabre values (`NORMAL` 6, `SABRE_ONLY` 6 floor and 9 air or wall), and
   `SABRE_ONLY` gives the buster 0 (B2: `SabreBlock`, `MetaWheelF`). `D1000` 2. `D1000Core` stun. Check: tests per
   target; an unknown damager still does 0.
7. **Checks.** The 4 Gradle tasks; mutation checks; the review before the play test; the play checks and the diff
   review.
