# Contract: mega-refactor-084d-damage-reaction

The gameplay that this branch must not change, and how to check it. Write this file during
planning, before you change code. Do the checks before the branch merges, and write the result
in the branch process log.

- Status: approved
- Approved: 2026-10-30

The branch moves stun, knockback, the recovery flash and freeze into components. Mega must react to damage in the same
frames, with the same velocities, flashes and sounds. `M/` = `core/src/main/kotlin/com/megaman/maverick/game/entities/megaman/`.

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes, with these tests:
  - One test class for each stateful component: a new instance equals the state after `reset()`, and the hooks stay.
  - Stun: counts down only on the ground; a second stun is ignored; the 3 event checks.
  - Knockback: the velocity for each of the 4 directions, on each side of the damager; no knockback for a class in
    `exempt` or for a damager that is not an `IBodyEntity`.
  - Recovery: starts on the frame that damage ends; `flash` toggles at the interval; `flash` is `false` at the end.
  - Freeze: `mashed` is not read while not frozen or on cooldown; the thaw comes in the frame of the last mash; `onMash`
    runs before `onThaw`.
  - `invincible` with `testServices()`: true on each update from the hit to the end of recovery, with no false frame
    between `damaged` and recovery. True with a `DAMAGE` lock, before play, after play and after `destroy()`. False while
    stunned, and false while frozen.
  - The temporary suite `Temp084dDamageReactionEquivalenceTest`, with the frozen copy `Temp084dFrozenDamageReaction`: the old
    and the new code run on the same seeded frames (delta, feet, hit, damage end, freezer hit, stun type, mash), and each
    frame has the same `stunned`, recovery, flash, `frozen`, mash count, velocity and ordered list of hook calls. Both files
    are deleted in the last commit before the merge.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] No old state is left: `grep -nE 'stunTimer|damageRecoveryTimer|damageFlashTimer|hitsToUnfreeze|frozenPushTimer|noDmgBounce|IFreezableEntity' core/src/main/kotlin/com/megaman/maverick/game/entities/megaman/Megaman.kt` finds nothing.
- [ ] The parts do not depend on `Megaman`: `grep -lE 'Megaman\b|MegamanValues|MegamanWeapon|SoundAsset' <the 9 new files in entities/state/>` finds nothing.
- [ ] A short launch (about 15-20 s) of `REFACTOR_TEST_084` starts with no exception, on the baseline for the new map and at
  the end of the branch.

## Frame order

Each old line runs at this position in the lambda or the function. Review the final diff against this table.

| Old line in `M/Megaman.kt` | New position |
|---|---|
| `:744` stun update | `StunComponent.update`, same position |
| `:745` start of recovery | First line of `DamageRecoveryComponent.update`, after `:746` |
| `:746` charge reset | Stays in the lambda, right after the stun update |
| `:748-756` recovery and flash | `DamageRecoveryComponent.update`; `:756` stays outside the guard |
| `:807-827` mash and thaw | `FreezeComponent.update`, same position |
| `:600-634` stun event | `onStunPlayerEvent`, in the `STUN_PLAYER` arm of `onEvent` |
| `:664-689` knockback | `knockBackFrom`, same position in `takeDamageFrom` |
| `:691-695` freeze | `freezeFrom`, same position, before the sounds |

## Manual playthrough

Run the commands from the worktree:

`cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-084d-damage-reaction`

The plan adds the test map `RefactorTest084.tmx` (`REFACTOR_TEST_084`) with the rooms below.

Stun:

- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room stun`: the pulser stuns Mega on the ground and during a wall slide.
  Mega bounces back, shows the stun animation, cannot move or shoot, and a charge stops. The stun ends only after the feet
  are back on the ground. A pulse does nothing while Mega is in the air with no wall slide.

Knockback and flash:

- `./run-game-alpha.sh --level REFACTOR_TEST_084 --room knockback`: get hit from the left and from the right, with gravity
  up, down, left and right. Mega moves away from the damager with the same speed as before.
- `./run-game-alpha.sh --level REFACTOR_TEST_084 --room spring_head`: touch a `SpringHead`. Mega takes the hit with no
  knockback.
- `./run-game-alpha.sh --level REFACTOR_TEST_084 --room knockback`: after a hit, Mega flashes, is not hit again during the
  flash, and shows fully at the end. A hit during a charge stops the charge and its sound.

Freeze:

- `./run-game-alpha.sh --level GLACIER_MAN --room room6`: get hit by an ice shot from `Coldier` or a bomb from `IceFox`. Mega
  freezes and cannot move or shoot. Each `B` release plays the ice sound, a fast mash counts only after the cooldown, and
  the 3rd mash thaws Mega with 5 shards. Mash during a damage flash too.
- `./run-game-alpha.sh --level GLACIER_MAN --room room6`: while frozen, take a hit from another enemy. Frozen Mega **can** be
  hit. This is the current behavior, and it must stay: frozen enemies cannot be hit, but frozen Mega can.

One real stage:

- `./run-game-alpha.sh --level GLACIER_MAN --room room6`: play on to the next rooms with hits, freezes and a death.

## Behaviors to check

- Death while frozen or flashing: `./run-game-alpha.sh --level GLACIER_MAN --room room6`: die while frozen, and die while
  flashing. After the respawn, Mega is not frozen, shows fully and moves at once. State now clears at destroy, not at the
  next spawn.
- Death and respawn: `./run-game-alpha.sh --level REFACTOR_TEST_029 --room player_death`: the respawn is normal, and Mega
  has no damage reaction left.
- Beam-in: `./run-game-alpha.sh --level GLACIER_MAN --room room6`: Mega cannot be hit during the beam-in.
- Performance: one `--diagnostics` capture before and after, in the same room, to show that the frame time does not get
  worse. Use `./run-game-debug.sh --level GLACIER_MAN --room room6`.

## Intended changes

None. The debug text at the old `:606-611` loses `getRatio()`; this is a log line, not gameplay.
