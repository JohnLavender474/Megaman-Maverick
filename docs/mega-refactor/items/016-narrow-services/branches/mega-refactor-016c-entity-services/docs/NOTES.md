# Notes: mega-refactor-016c-entity-services

## Decisions

- 2026-10-29 (owner, gate 1, "Approved"): `SCOPE.md` approved. Questions 3 to 10 and 12 accepted as recommended.
  Changed by the owner (recorded on `master` as `016` decisions 13-15):
  - Question 1 (verbatim): "I agree with your assertion. I think it's worth keeping in mind though that it might be best
    for a \"camera service/component/helper/manager\" to handle things like these fields instead of directly tying
    them to the camera itself." The camera flags go into a new `GameCameraState` next to the camera, reached through
    the holder field `camera`.
  - Question 2 ("Agreed"): the performance value goes into a small settings object (`GameSettings`), the holder field
    `settings`; not `GameClock`.
  - Question 11 (verbatim): "I agree with you, remove them." `getGameCamera`, `playSoundNow` and `stopSoundNow` are
    removed from `IMegaGameEntity` and not moved; callers read `game.audioMan` and `game.getGameCamera()`.

## Overlap with other branches

- 2026-10-29: no other code branch is active (`git worktree list`: the main checkout, the parent
  `mega-refactor-016-master`, and this branch; `fix/pool-identity-freeset` is merged into `master`). The merge target
  is the parent; the branch start `9ba76c833` is its tip.
