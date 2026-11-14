# 077: Better boss fights for every boss: new and panic attacks, aiming, death animations

- Status: active
- Type: feature
- Objectives: `O4`
- Depends on: none
- Related: `015`, `023`, `118`
- Branches: `mega-refactor-077a-intro-boss` (active, 2026-11-13)

## Goal

Make every boss fight richer: the 8 robot masters, the Wily bosses (for example `GutsTank`), the mini bosses and
the intro bosses (for example `BigAssMaverickRobot` in `INTRO_STAGE`, and `D1000` in `X6_INTRO`). Each boss gets one or two new attacks, at least one of
them a "panic" attack that it uses only when its health is less than 25%; smarter aiming with some randomness; and a
richer death animation.

Widened on 2026-11-10 (owner) from "New attacks and a panic attack for each robot master".

This item changes gameplay on purpose. It is not a refactor.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- For each boss: design the new attacks, the panic attack, and how the boss chooses them.
- Aiming: in an attack that fires several shots, not every shot aims at the player's current position. Mix the aims:
  one shot leads the player (where the player will be in about 1-2 seconds, from the current position and
  velocity), one aims at the current position, one lands at a random spot near the player, and one lands at a wider
  random spot to distract. The Inferno Man meteors and the Reactor Man lasers already work in this spirit.
- Death: replace the simple cartoon death (for example the X eyes of `GutsTank` and of `BigAssMaverickRobot`, the `INTRO_STAGE` boss) with a richer
  dying animation for each boss.
- A common way to start the panic phase (the health check at 25%), if two or more bosses need the same code.
- One branch per boss or per small group of bosses, because each boss is its own class. A shared piece (the panic
  phase start, a lead-aim helper, a death-sequence step in the `118` fight routines) goes in the first branch that
  needs it.

## Owner's input

2026-10-29 (verbatim):

> Each and every single robot master in the game needs one or two more attacks, especially at least one "panic"
> attack (a new attack that the robot master uses only when their health is < 25%)

2026-11-10, at the `146a` play test, about `GutsTank` (verbatim):

> The Guts Tank boss lumps energy orbs from his mouth into the air, and they're meant to fall and hit Mega Man.
> However, the calculation for the lumped energy orb is such that it targets Mega Man's position at that exact frame.
> If Mega Man is moving (either running or standing on the tank's platform as it moves left and right), then the orbs
> become easy to dodge. It would be good if at least one of the orbs were lumped in order to target where Mega Man WILL
> BE given the current trajectory, position, etc. Sort of like in the Inferno Man and Reactor Man fights (meteors and
> lasers), not every one should target Mega Man. It's more fun when only one or a few try to hit him, and the others
> have a certain element of randomness to them. For example, if Guts Tank shoots 4 orbs, then one can be lumped to
> where Mega Man MIGHT BE in 1-2 seconds, one lumped at his position in the current frame, one lumped at a random spot
> in the vicinity, and another lumped at a wilder random position in order to distract Mega Man.

> When Guts Tank dies, his eyes shows X's similar to cartoon characters when they die. The boss in the Maverick intro
> stage does the same. This is "cute", but it's overly simplistic right now. These two bosses and other bosses need
> more intricate death/dying animations.

## Decisions

- 2026-11-13 (owner): merge target: each branch squash-merges directly into `master`.
- 2026-11-13 (owner): a boss with health phases gets its harder attacks in its last phase in place of the panic
  attack under 25%. A boss with 1 phase keeps the panic attack.

- 2026-11-10: the item covers every boss (robot masters, Wily bosses, mini bosses, intro bosses), and it also covers
  aiming and death animations. This answers the open question about the mini bosses and the Wily bosses.
- 2026-11-13 (owner): one branch per boss. The owner picks each boss as the work goes on. The first boss is the
  `INTRO_STAGE` boss, `BigAssMaverickRobot`. Research: `temp/077-intro-boss/` (`boss-review.md`,
  `overdrive-health.md`).
- 2026-11-13 (owner): a shared "overdrive" mechanism for every fight: the boss health bar has more than one phase.
  A boss with 2 phases has 2 × `MAX_HEALTH` bits. A lost bit takes the color of the next phase in place of going
  empty, and at 0 the bar rolls over to the next phase at once, with no rest time (not as in the Wily final boss, which
  has a scripted rest between phases). In the next phase the boss is harder: new attacks, quicker attacks.
- 2026-11-13 (owner, `BigAssMaverickRobot`):
  - Mega Buster damage goes up: a pellet 1 → 2, a half charge 2 → 3, a full charge 3 → 4.
  - The body that stays on screen after the head dies is on purpose. It is not a bug.
  - The hand motion: keep the circular orbit, because it refers to the final fight of Mega Man X1, but the orbit is
    no longer the only motion. Add the new motions from the research (hover with drift, a punch with wind-up and
    recoil, and others) and describe them and their integration with a proper state machine.

## Open questions

- Is 25% measured on the boss's maximum health, and does the panic attack also apply in hard mode only, normal mode
  only, or both?
- Does the boss enter the panic phase once (with a signal, for example a flash or a sound), or does it check the
  health before each attack?
- With 2 players, which player does a lead-aimed shot target (see `112`, targeting per entity)?
- Random aims must use the seeded random source (`O2-R2`, after `011`).
- Does this wait for `015` (entity state machine), so that the new attacks use the new state model?

## Sources

- `NOTES.md` in this folder.
