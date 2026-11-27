# 190: Rush in the game: Rush abilities call Rush down to join Mega Man

- Status: active
- Type: feature
- Objectives: `O4`, `O9`
- Depends on: none
- Related: `024` (weapon defs), `012` (named freezes), `178` (NPC allies)
- Branches: `mega-refactor-190a-rush-jet-summon` (active)

## Goal

Rush, Mega Man's dog, is not in the game at all today. Mega Man has one Rush ability, Rush Jet (a reward of the Moon
Man stage), but when he equips it the jetpack is simply on his back: Rush never beams down. In the official Mega Man
games Rush beams down next to Mega Man, waits, and Mega Man "equips" him on contact. This item adds Rush as an entity
and makes each Rush ability go through him, starting with Rush Jet, then Rush Spring and other abilities that the
owner decides later.

## Proposed scope

Not approved. Each branch `SCOPE.md` decides its final scope.

| Branch | Goal | Status |
|---|---|---|
| `mega-refactor-190a-rush-jet-summon` | The wiring of the Rush flow for Rush Jet, with temporary "dog" sprites | active |
| `190b` (not prepared) | Rush's sprites from the owner's first-draft sheet: frames, atlas, credit | idea |
| `190c` (not prepared) | Rush Spring, a second Rush ability | idea |

### The Rush flow (from the owner, 2026-11-27)

1. Mega Man equips a Rush ability (first: Rush Jet).
2. Mega Man presses `SHOOT`: Rush beams down beside Mega Man, on the side he faces.
3. If Rush lands on a platform, he sits and waits for contact with Mega Man.
4. On contact, an animation plays in which the two join into one (for Rush Jet: Rush forms into the jet that sits on
   Mega Man's back). The game pauses while the animation plays.
5. Mega Man can use the ability as long as it stays equipped. When Mega Man unequips it, Rush beams out, and no Rush
   ability can beam Rush down again until his beam up ends.

### Later abilities

- Rush Spring and other abilities are decided later. A Rush ability does not always join Mega Man: Rush Spring (Rush
  Coil in the official games) stays on the ground as a spring. The flow of `190a` is written so that a later ability
  can choose its own "after the landing" part; `190a` builds only the Rush Jet part (no API without a caller).

## Owner's input

- 2026-11-27: "Let's create a new mega-refactor item in Mega Man Maverick for having Rush (Mega's side kick) be
  present. Right now, he's not present at all in the game. Mega Man has one special Rush ability earned after beating
  Moon Man: Rush Jet. However, when Mega Man equips it, Rush doesn't beam down and appear so that Mega can "equip" him
  like he does in other official Mega Man titles. There should be multiple Rush abilities including the Jet, a Rush
  Spring, and other abilities we'll come up with later."
- 2026-11-27: the five steps of "The Rush flow" above, for an A branch that sets up the wiring.
- 2026-11-27: "A first-draft sheet of sprites will be provided soon." Until then, temporary "dog" sprites.
- 2026-11-27: push the item docs to `master`; open a draft PR when the feature branch reaches the end of its first
  pass.

## Decisions

- 2026-11-27 (Claude, the owner was not asked; confirm in the `190a` PR): merge target **directly into `master`**.
  Each branch is a playable step. `190a` does not merge while it holds the temporary sprites, unless the owner
  accepts them for a time.

## Open questions

- Does Rush leave on his own after a while if Mega Man never touches him? (`190a`: no, he waits until he goes off
  the view, Mega Man unequips, or Mega Man dies.)
- Calling Rush costs no energy in `190a`, but an empty Rush Jet cannot call him. Should a call cost energy?
- Gravity turned (Mega Man not upright): `190a` calls Rush only when Mega Man stands upright. What should happen in
  a turned room?
- Co-op: each Megaman has his own Rush. Can player 2 join player 1's Rush? (`190a`: no.)
- Rush Spring: does Rush stay a spring until Mega Man unequips, or does he leave after one bounce or a time?

## Sources

- `NOTES.md` in this folder.
