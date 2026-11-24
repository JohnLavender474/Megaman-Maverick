# Process log: mega-refactor-184a-sub-frame-taps

## Status

- State: dropped (2026-11-23)
- Goal: keep sub-frame taps and short releases, keyboard and gamepad
- Done: planning; scope approved 2026-11-23 (keyboard only)
- Brief: https://claude.ai/artifact/KsqcNgA8N8xyULvNek8EdA
- Next: none
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-23

- Branch start on `master`. Merge target: directly into `master`. Worktree `.worktrees/mega-refactor-184a-sub-frame-taps/`.

- Scan. D1 check by a research subagent: SDL2 2.0.20 and Jamepad are not safe from a second thread (use-after-free at hot-plug). By D1, all Jamepad calls move to one sampler thread. Findings in `docs/NOTES.md`.
- Draft `SCOPE.md`, brief published, decisions D4 to D6 asked.
- Owner asked for research of other LibGDX/Java pad libraries (2 read-only subagents: input4j, sdl2gdx). None keeps sub-frame pad taps. Owner chose keyboard only; the branch is now small (3 production files): `PLAN.md` removed, steps in `SCOPE.md`. Brief updated with the scope change.

## 2026-11-23 (drop)

- The owner abandoned item `184`. No code was written. The branch docs were copied to `master`; the branch keeps its 2 planning commits.
