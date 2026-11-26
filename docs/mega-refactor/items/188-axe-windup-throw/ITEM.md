# 188: Hold-to-aim axe throw on a generic weapon windup

- Status: active
- Type: feature
- Objectives: `O9`, `O1`
- Depends on: none
- Related: `024` (weapon defs), `081` (X), `121` (enemy clones of the player)
- Branches: `mega-refactor-188a-axe-windup-throw`

## Goal

Mega Man's `AXE_SWINGER` throw becomes hold-to-aim. A tap throws a weak axe. Holding SHOOT keeps Mega Man on the
"axe above the head" frame, draws a dotted arc of the throw, and grows the impulse up to a max (y grows less than x,
so a full throw does not leave the screen vertically for long). Release throws.

To carry it, the per-player weapon charge becomes a plain windup: a small `WeaponWindup` interface (hold, clear), with
the buster's half and full levels as one implementation (`BusterCharge`, with hooks) and the axe as another
(`AxeThrowWindup`, continuous progress only). Each player builds its own windup objects.

## Proposed scope

The branch `SCOPE.md` decides the final scope.

## Owner's input

- 2026-11-26 (chat, phone): "I want to change how Mega Man throws his axe weapon. [...] holding SHOOT to 'charge up'
  the weapon changes the impulse. Tapping SHOOT results in a weak impulse. Holding SHOOT makes Mega Man stay in the
  'holding axe above his head' frame, and a dotted arc is drawn showing the approximate trajectory that the axe will
  follow. The impulse increases up to sensible max values for x and y. [...] the y impulse increases less than the x
  impulse."
- 2026-11-26: the charge logic "needs to be 'dumb' enough to support either setup, where charge status is a 'custom'
  setup that the component itself doesn't itself facilitate."
- 2026-11-26: each player instantiates its own windup objects (no factory); X's charge will be retuned later and must
  not be coupled to Mega Man's; the windup houses its own timer; hooks, not a player reference, for sounds; no default
  `status()` on the interface; names `WeaponWindup`, `BusterCharge`, `AxeThrowWindup`; status computed from time and
  thresholds.
- 2026-11-26: "I'm not worried about unused hooks at this point. Go with recommendations, build out the branch with
  tests, then push a draft PR."

## Decisions

- 2026-11-26: merge target is `master` directly (one branch; recommendation accepted with "go with
  recommendations").
- 2026-11-26: the branch decisions are in the branch `docs/SCOPE.md` ("Decisions").

## Open questions

## Sources

- `NOTES.md` in this folder.
- The branch `docs/NOTES.md`.
