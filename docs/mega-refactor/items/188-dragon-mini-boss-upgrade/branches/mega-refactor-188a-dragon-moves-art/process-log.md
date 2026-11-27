# Process log: mega-refactor-188a-dragon-moves-art

## Status

- State: active
- Goal: Opus ideation of new MechaDragon moves, then Fable art: the existing frames redrawn with legs, plus new frames for the best new attacks (design and art only).
- Done: item `188` and this folder created on `master`; Opus ideation; Fable art rounds 1 to 3 (legs on every strip, dangling legs on the airborne strips, new attack strips).
- Next: the owner reviews the art in the draft PR (brief: in chat, `claude-web.md`); then `188b` (code, atlas repack, README credits).
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-26

- Item `188` created from the owner's request. Merge target: `master` directly. Fable approved. Branch started from `master` (`2ebb704`). No code changes yet. 

- Opus agent (ideation): `temp/dragon-moves/IDEAS.md`, summarized in `docs/NOTES.md`. Recommended: sweeping flame breath (standing), stomp landing and takeoff, chin chomp, aimed volley, charge kept (high lane only).
- Fable art agents: round 1 (legs on every cell, new `land`, `stand`, `breath_*`, `chomp`, `flame_puff`), round 2 (owner: legs look odd pointing like the arms; `fly` and `charging` redrawn with dangling legs, cells grown to 128x144), round 3 (the other airborne strips get the same legs at 128x144). Each owner-feedback round used a fresh agent.
- Checks: PNG scripts (size, alpha, NES master palette, silhouette above the legs, edge steps): 0 hard fails. Not run: Gradle, the game, TexturePacker (not installed in the cloud); atlases not repacked.
- Open: the dragon's colors break the 2-palette rule (old art did too); README credit entry; Opus picks not overruled by the owner (see `SCOPE.md`).
