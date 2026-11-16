# Notes: mega-refactor-024b-character-loadouts

## Decisions

Owner answers to the brief, 2026-11-15:

- D1: a `CharacterLoadout` per character in `players/`: the weapon list in select order and the reward per level.
- D2: owned weapons are derived from the shared completed levels and the character's rewards, plus a per-character
  set for `--weapons` and overrides. The save format does not change.
- D3: the hooks and the look fields change now (not the recommendation, which was `060h`). Follow-up answer: the
  owner type is a type parameter on the def, `WeaponDef<in O : MegaPlayer>`. Megaman's defs take `Megaman`; a def
  that takes `MegaPlayer` is shared and fits any loadout. No cast in a hook.
- D4: fix the `EntityQueries.shooting` gap in this branch (not the recommendation, which was an issue): enemies that
  react to shots also see X's and Proto Man's shots.

Earlier owner decisions for item `024` (in `ITEM.md`): the loadout is per character; bosses beaten are shared; a
shared def is pure; the look can differ per character.

## Overlap with other branches

- `060l` (active, uncommitted on 2026-11-15): deletes `ProtomanWeapon.kt`, changes `Protoman.kt` and
  `PlayerStatsHandler.kt`. This branch adds `character` and `shooting` to `Protoman.kt` and changes the weapon bar in
  `PlayerStatsHandler.kt`. The branch that merges second resolves it.
- `011a` (active): changes 2 functions in `entities/megaman/weapons/` when it takes `master`. This branch changes the
  types of those files (the owner type), not those 2 functions. Text conflicts in the signatures are possible.
- `077b` (active): art only. No overlap.
