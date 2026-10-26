# Notes: mega-refactor-031b-engine-input

## Decisions

- 2026-10-26 (owner, in the `031b` assessment): `InputFrame` and `InputSource` are engine mechanism; the game decides
  the buttons, names and mapping. Engine `InputButton { val index: Int }`, implemented by `MegaControllerButton` with
  an explicit `index`. The frame is a value class over a `Long`.
- 2026-10-26 (owner, on the draft scope): the four open questions as proposed (see `docs/SCOPE.md`, "Decisions on the
  open questions"): menus out; actuators stored directly, `putActuator` and `removeActuator` removed; the source on
  `ControllerComponent`; the slot view binds the source.

## Overlap with other branches

None active.
