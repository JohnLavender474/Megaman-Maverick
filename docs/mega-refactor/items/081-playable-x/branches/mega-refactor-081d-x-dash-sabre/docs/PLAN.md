# Plan: mega-refactor-081d-x-dash-sabre

Each step has its own check. All checks use Gradle; no game launch.

1. **Test room `ground_dash`.** A subagent adds it to `RefactorTest081.tmx`: 16+ tiles of flat floor, a wall to dash
   into, a 6-tile pit, a ledge to dash off; a spawn; painted tiles. Check: the XML parses; the level analyzer.
   - Done: yes
2. **Knockback.** `MegamanXValues.KNOCKBACK_X`, `KNOCKBACK_Y`, `KNOCKBACK_DRAG`; X's own `KnockbackComponent`; the body
   applies the side drag only while damaged. Check: tests (speed after a hit, decay, no drag after the damage).
   - Done: yes
3. **Ground dash.** `BehaviorType.GROUND_DASHING`; X's ground dash behavior (start on `R` or `DOWN` + `A`, start delay,
   decel, length, end rules, end slide, off a ledge); the body keeps the side speed in a dash; the `dash` animation
   key. Check: tests.
   - Done: yes
4. **Dash jump and dash wall kick.** X's dash air speed state (set by a dash jump or a kick with `R`, cleared on
   landing and on a wall grab), the run reads it, no air dash after it. Check: tests.
   - Done: yes
5. **Faster `R` air dash.** Check: tests; Megaman's air dash tests unchanged.
   - Done: yes
6. **Docs.** Branch notes; prepare the `081e` branch folder note for the merge.
   - Done: yes
7. **Checks.** The automated checks, mutation checks, the review before the play test. Then the owner's play test.
   - Done: yes
