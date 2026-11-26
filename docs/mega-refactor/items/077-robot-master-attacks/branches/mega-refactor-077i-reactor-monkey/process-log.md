# Process log: mega-refactor-077i-reactor-monkey

## Status

- State: active
- Goal: a richer Reactor Monkey mini boss: 2 phases with the Mad Beat, intro, full death, throw deck, Two-Ball
  Juggle, enraged ball and face, Hot Potato
- Brief: in the PR text (`claude-web.md`)
- Done: `PLAN.md` steps 1 (docs), 2 (atlas `ReactorMonkey`, `TextureAsset.REACTOR_MONKEY`, README credit) and 3 to 9
  (phases, damage, tuning rows, intro, throw deck, Mad Beat, enraged face, steam, Two-Ball Juggle, Hot Potato,
  death; not compiled)
- Next: `PLAN.md` step 10 (draft PR; text in `temp/077i-reactor-monkey/pr-body.md`)
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

## 2026-11-26 (steps 3 to 6, fight code first pass)

- Code agent, cloud session: nothing compiled (Gradle blocked); the CI of the PR is the check.
- `ReactorMonkey.kt` rewritten around 7 states (`INTRO_DROP`, `INTRO_LAND`, `INTRO_ROAR`, `STAND`, `HOLD`, `THROW`,
  `MAD_BEAT`); new `bosses/reactormonkey/ReactorMonkeyTuning.kt`; `ReactorMonkeyBall` gets `lob`, `skip`, a per-ball
  bounce limit and the enraged regions (spawn prop `ConstKeys.RED`); `PlayerDamageNegotiator` entries (4 and 3, the
  `master` fallback values). Details and numbers in `PLAN.md` steps 3 to 6.
- Plan change: `CHEST_BEAT_PASSES = 3`, not the art default 4 (main-session order; the owner decides at the play test).
- Choices: the intro drop is a draw offset (the body stays pinned); sounds are existing assets (`QUAKE_SOUND` land,
  `MM2_MECHA_DRAGON_SOUND` roar, `POUND_SOUND` beats; no steam sound); the mad face flash is a red tint blink for
  0.5 s over the strip's own red-eye cells; the throw delay interpolates by the health of the current phase.
- Test resources: `spawn-allocation-baseline.txt` line `bosses/ReactorMonkey.kt 1` removed (`throwDelayTimer` is a
  field now); `entity-tags.txt` unchanged (no new entity class).
- Defeat still uses the `master` explosions and alpha fade (step 9 changes it).

## 2026-11-26 (steps 7 to 9, juggle, Hot Potato, death)

- Code agent, cloud session: nothing compiled (Gradle blocked); the CI of the PR is the check.
- Step 7: the ball reference became owned lists (`incomingBalls`, `heldBall`, `liveBalls`); a phase-2 move choice
  (throw, juggle; never twice in a row); `ReactorMonkeyTuning.juggleBalls` (2 normal, 3 hard, phase 2).
- Step 8: Hot Potato in `ReactorMonkeyBall.hitProjectile` (full `ChargedShot` of a player); the owner flips to the
  shooter, which `AbstractEnemy.canBeDamagedBy` needs for the monkey to take the hit; the returned ball damages only
  its thrower. Monkey: `RETURNED_BALL_DAMAGE = 4`, stun `BALL_HIT` 1.2 s, `STUNNED_DAMAGE_SCALAR = 1.5`.
- Step 9: the `077g` death pattern: `dying` × 4, `collapse`, `dead` 1 s, `defeatDur` 3.0 s; no fade, no blink, no
  explosions; every ball explodes at the defeat.
- Choices: no stun during the Mad Beat (the pose goes on); a stun cancels the juggle and explodes the balls in his
  hands; a missed returned ball turns back into his ball at its first bounce; a caught ball is moved to the catch
  point (a late catch in a juggle no longer leaves it inside his body). Numbers in `PLAN.md` steps 7 to 9.
- Test resources: `entity-tags.txt` and `spawn-allocation-baseline.txt` unchanged (no new class, no constructor in
  `onSpawn`/`onDestroy`).
