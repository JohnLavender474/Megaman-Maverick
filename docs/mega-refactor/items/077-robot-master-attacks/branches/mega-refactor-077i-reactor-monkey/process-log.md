# Process log: mega-refactor-077i-reactor-monkey

## Status

- State: active
- Goal: a richer Reactor Monkey mini boss: 2 phases with the Mad Beat, intro, full death, throw deck, Two-Ball
  Juggle, enraged ball and face, Hot Potato
- Brief: in the PR text (`claude-web.md`)
- Done: `PLAN.md` steps 1 (docs) and 2 (atlas `ReactorMonkey`, `TextureAsset.REACTOR_MONKEY`, README credit)
- Next: `PLAN.md` steps 3 to 9 (fight code, one agent), then step 10 (draft PR)
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-26 (branch start, docs and assets)

- Owner order (from the phone): go ahead with the branch and the first-pass implementation, then push a draft PR.
- Branch cut from `mega-refactor-077h-reactor-monkey-art` (stacked; `077h` is not merged). Cloud session: Gradle and
  Maven are blocked by the proxy, so nothing compiles; the CI of the PR is the automated check.
- Docs: `SCOPE.md` (draft, first pass), `PLAN.md`, `NOTES.md`, `TODO.md`, `TEST-DEBT.md`, `ASSETS.md`.
- Assets: new frames folder `assets/sprites/frames/ReactorMonkey/` (22 `ReactorMonkey/*` strips from `077h` round 4,
  `chest_beat*` from round 5; `ReactorMonkeyBall/mad_0..3`). Page `ReactorMonkey.png` 1136 x 2042 and
  `ReactorMonkey.txt` (26 regions) packed by `temp/077i-reactor-monkey/pack.py`; `verify.py`: every region equals its
  frame pixel for pixel, 2 px apart, inside the 2 px border. `ReactorMonkey.tps` made by hand from
  `Backgrounds13.tps` (Basic, no rotation, border and shape padding 2, `ClearTransparentPixels`).
  `TextureAsset.REACTOR_MONKEY` added (all entries load). `assets/assets.txt` is git-ignored and generated, so no edit.
- README `CREDITS`: Reactor Monkey now has an "Original by" Balloon Cart line and an "Updated by Claude Fable 5.2" line.
- Play-check rooms from `ReactorMan_v3.tmx`: `room5` (spawn 199) before `mini_boss_room_1` (via `room6`, no spawn)
  and `alt_path_1_mini_boss_room_1` (via `alt_path_1_room_1`, no spawn); `room12` (spawn 566) before
  `mini_boss_room_2`.
