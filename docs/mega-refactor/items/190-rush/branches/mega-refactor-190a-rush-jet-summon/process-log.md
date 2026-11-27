# Process log: mega-refactor-190a-rush-jet-summon

## Status

- State: active
- Goal: Rush Jet goes through Rush: `SHOOT` beams Rush down, he sits, a join pause on contact, beam out on unequip
- Brief: in chat (Claude on the web; the owner gave the flow and asked for a first pass with a draft PR)
- Done: first pass implemented, reviewed and fixed; draft PR open
- Next: the owner's answers to D1 to D12 in the PR, the play test, then `190b` (the owner's sprites)
- Blocked:

## Entries

## 2026-11-27

- Item `190` and this branch folder created on `master` (cloud session from the phone). Scope drafted from the
  owner's five-step flow; decisions D1 to D11 in `docs/NOTES.md` made without the owner, to be asked in the PR.
- Implementation (plan steps 1 to 5) in one commit. Gradle cannot run in the cloud: `services.gradle.org`,
  `repo.maven.apache.org` and `plugins.gradle.org` are blocked (the Gradle zip itself came from GitHub, but no
  dependency resolves). The CI of the pushed branch is the automated check: `gradle` (engine and core tests, smoke
  test, both jars, detekt) passed on `3dc3e2d`.
- Mutation check (one CI run on a temporary branch `temp-190a-mutations`; the cloud proxy refuses the delete of the remote branch, so the owner deletes it): summon ignores the phase,
  `onRushGone` ignores the identity, `weaponMovesReady` always `true`. Each one failed its tests (5 cases in `MegamanRushHandlerTest`,
  `MegamanWeaponRulesTest`, `MegamanAirDashTest`); the code is restored.
- Review before the play test: one Sonnet agent. No compile error. Findings and fixes:
  1. Check 5 could not show what it says (every room is one view wide): new room `wide`, two views wide; check 5 and
     check 6 rewritten.
  2. Choosing Rush Jet again in the pause menu sent Rush away (`onDeselect` runs on a re-select): the dismissal moved
     from `RUSH_JET.onDeselect` to `MegamanWeaponsHandler.onChangeWeapon`, only for a real change; test
     `MegamanWeaponChangeRushTest`.
  3. A `leave()` before the queued spawn ran was lost, and a failed spawn left the handler stuck: `leaveOnSpawn` in
     `Rush`, and `arrive`/`leaveFromBack` return the spawn result; the handler goes back to `ABSENT` on a failure.
  4. Atlas regions had no `Rush/` prefix: regions renamed to `Rush/<name>`.
  5. and 6. Named `loop =` arguments; LibGDX `Array` in the test.
  7. Level-end beam look: bug candidate in `docs/TODO.md`.
  8. Stale `FEET_ON_GROUND` on a pooled Rush: cleared in `onSpawn`.
