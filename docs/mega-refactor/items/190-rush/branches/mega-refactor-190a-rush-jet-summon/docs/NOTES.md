# Notes: mega-refactor-190a-rush-jet-summon

## Decisions

Made by Claude on 2026-11-27 without the owner (the owner asked for a first pass and a draft PR). Each one is asked in
the PR; a change goes back into `SCOPE.md`.

- D1: `SHOOT` with Rush out (arriving, sitting, joined) or leaving fires the buster, as in the official games. Only
  `SHOOT` with no Rush calls him, with no shot and no shoot pose.
- D2: Until Rush is joined, Rush Jet gives the buster look and the normal moves: no jet on the back, no jetpack, no
  jet air dash.
- D3: Calling Rush costs no energy, but an empty Rush Jet cannot call him (`SHOOT` then fires the buster).
- D4: Rush lands 1.5 tiles in front of Mega Man. He passes through blocks while his bottom is more than 2 tiles above
  Mega Man's top (a ceiling over Mega Man), then lands on the first surface.
- D5: A Rush who leaves the view (a pit, Mega Man walks off, a room transition) is gone at once, with no beam; Mega
  Man can call him again at once. A Rush who never lands is the same case.
- D6: Rush waits with no time-out.
- D7: The join pause lasts `JOIN_DUR` (0.6 s); Rush moves from his seat onto Mega Man's back while his join animation
  plays. Mega Man's frame stays frozen.
- D8: Rush is called only when Mega Man stands upright (`direction == UP`); in a turned room `SHOOT` fires the buster.
- D9: Each Megaman has his own Rush; Rush joins only his owner (co-op).
- D10: The temporary "dog" sprites are programmer placeholders made by a script, not by a Fable art agent (the art
  rule is for real art; `190b` brings the owner's sheet).
- D11: Merge target directly into `master` (item `ITEM.md`, "Decisions").
- D12: Choosing the same Rush ability again (pause menu) is no change: Rush stays. Only a change to another weapon
  sends him away.

## Overlap with other branches

Read on 2026-11-27 (`ITEMS.md` active branches):

- `185c-games-layout` moves the game code into `games/mega-man-maverick/`. If it merges first, this branch's new
  files and edits in `core/` move with it: a rebase with path changes, no design overlap.
- `178b-ground-pathfinding`: the follow bot copies player 1's input; a bot Megaman can call his own Rush. No code
  overlap.
- `007a-json-save-slots`: weapons are not saved; no overlap.
- The art branches (`078f`, `078h`, `187a`, `188a`, `189a`, `060k`) touch other atlases only.
