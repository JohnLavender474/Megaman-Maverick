# Contract: mega-refactor-077b-desert-man

- Status: approved
- Approved: 2026-11-15 (owner)

The fight is tuned in fast play-test rounds (`docs/WAYS_OF_WORKING.md` "Gameplay iteration"): the tests are written
from `docs/TEST-DEBT.md` when the owner says the fight is final.

## Automated checks

- [x] `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` pass, with the tests of `docs/TEST-DEBT.md`.
- [x] The review before the merge finds that every manual check runs as written. No game launch.

## Manual playthrough

`cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-077b-desert-man`

Experimental checks:

- `./run-game-alpha.sh --level DESERT_MAN --room room16 --heartTanks all --healthTanks all`: 2 phases (the bar rolls
  over); a pellet takes 2 bits (2 phases, same hit count as `master`); the punch has a white-fist wind-up and the long-punch arm has no gap; the tornado
  spins up and does not chase you; in phase 2 he burrows, the trail runs to you and splashes before he erupts, and the
  erupt jump aims at you. The new attacks: the long punch (rebuilt arm, fist and speed lines), the fist throw with a
  needle fan, and the needle spin (crouch with crossed arms, tucked rise, needle bursts at the top). The intro is a
  one-shot pose, not the dance wiggle.
- `./run-game-alpha.sh --level DESERT_MAN --room room16 --difficulty hard --heartTanks all --healthTanks all`: the hard
  rows.
- `./run-game-alpha.sh --level WILY_STAGE_1 --room room9 --heartTanks all --healthTanks all`: the rematch has the same
  fight, and the burrow trail reads in the dark room.

## Must not change

- Every other boss. The damage of each Desert Man damager to the player (3).
- The fall onto the sand at the start of the fight, the quicksand, the stage layout.

## Intended changes

| Change | Before | After |
|---|---|---|
| Health | 1 bar of 30 | 2 phases |
| Pellet / half / full | 1 / 1 / 2 | 2 / 2 / 4 (2 × `master` on 2 phases: same hits to kill); Moon Scythe 8; Reactor Shot 6; other specials and explosions 2 |
| Punch | no tell | white-fist wind-up |
| Tornado | 0.2 s tell, homes | 0.5 s spin-up, locked direction; the short-punch damager is on for the whole moving tornado |
| Phase 2 | none | burrow and erupt (new `burrow` / `erupt` art); the erupt jump aims at the target |
| Long punch | stretched `arm_extension` pieces | rebuilt: wind-up, eased extend and retract, arm, fist and speed-line sprites, fixtures updated each tick |
| New attacks | none | fist throw (`throw_windup`, `throw`, `throw_burst`, aimed needle fan) and needle spin (`needle_spin_tell`, `needle_spin_tuck`, `needle_spin`) |
| Intro | the dance wiggle while the bar fills | the one-shot `intro` pose |
| `STAND` choice | random, the same attack can repeat | each attack has a roll from the tuning row, no attack twice in a row, `JUMP` as the fallback |
| Limb colors | black | black with a 1 px grey outline (reads on the black sunset background) |
| Long-punch arm | a 22 px gap on each side | no gap |
| Damage to the player | 3 by the size rule | 3 in the damage table, with a `TODO` |
