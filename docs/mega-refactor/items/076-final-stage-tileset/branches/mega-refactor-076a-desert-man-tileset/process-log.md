# Process log: mega-refactor-076a-desert-man-tileset

## Status

- State: merged
- Goal: Desert Man gets the new original tileset of the `temp/076-desert-tiles/` drafts (phases 1 to 5): a new tileset image and `.tsx`, and the map repainted by the remap functions
- Done:
- Brief: https://claude.ai/artifact/3bbaSvdZyJ8oZeLhVTVE7Y
- Next: none (merged)
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-17

- Branch start. Art drafts made before the branch (5 phases, accepted by the owner): `temp/076-desert-tiles/phase1/` to `phase5/` in the main checkout; the newest script is `phase5/make_phase5.py`.
- Owner decisions before the branch: keep the gap-0 vertical rock joins (`merge_vertical=False`); merge target `master`.
- Open from phase 5: the 2 gate-column tiles (gids 11372, 11462; 8 cells), the bloom head that can read as a pickup, the spikes under water.
- Planning: scan, `SCOPE.md`, `CONTRACT.md`, brief. The owner approved all recommendations in advance (D1 to D6), so the brief shows the decisions as chosen. `SCOPE.md` and `CONTRACT.md` approved 2026-11-17. `PLAN.md` written (6 steps).
- Steps 1 to 5 (Fable subagent): script `tools/make_desert_man_tileset.py` (fails on an unmapped gid), new tiles `gate_column` (D1) and a closed `bloom_head` bud (D2), `desert_man_tileset.png` (113 tiles, 16x8 grid) and `.tsx`, TMX repainted. Script checks pass: only the tileset lines and the 5 layer CSVs changed; same painted cells per layer; every old gid mapped (80 layer/gid pairs, 16,763 cells); the render from the new TMX equals the remap render in all 17 rooms. Renders: `temp/mega-refactor-076a-desert-man-tileset/rooms/` (main checkout). README example path moved from `/tmp` to `temp/`.
- Automated checks (Corretto 17): `engine:test` 710, `core:test` 2416, `core:smokeTest` 469, `lwjgl3:jar`, `detektMain`, `core:verifyRandomRule` pass.
- Review before the play test (1 subagent): load in LibGDX, only tile data changed, all 8 manual checks valid, no block/ladder/spike/water art missing: PASS. 1 finding, fixed: `tools/README.md` needed `mkdir -p temp/076-desert-tiles` before `git show ... >`. Contract wording for room15 corrected (the spikes are only in the water).
- Diff review, round 1 (open): the owner: the tool scripts are temporary, so they go in `temp/`. `tools/` moved to `temp/076-desert-tiles/tools/` (main checkout) and removed from the branch; SCOPE D3, PLAN and NOTES updated. Round 1 closed by the owner ("Diff looks good"): 1 remark, fixed (tools moved). The fix changes only branch docs, so no build input changed and the automated checks were not run again (no Gradle during the play test).
- Owner play test: passed ("Level looks good so far", confirmed as a pass on 2026-11-17). Merge approved.
