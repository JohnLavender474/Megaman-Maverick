# 032: Platform-neutral `engine` module

- Status: planned
- Type: refactor
- Depends on: none
- Related: `001`, `002`, `009`, `011`, `021`, `033`, `035`, `044`, `054`
- Branches: none yet

## Goal

The `engine` works on desktop, Android and web.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- Remove `gdx-tools` and `gdx-controllers-desktop` from `engine`. Align `gdx-freetype` with `$gdxVersion`. Move `kotlin-reflect` out (after `021`).
- A `PlatformServices` interface (clock, task runner, log sink, capability flags). `lwjgl3` supplies the JVM implementation.
- Route `AsyncPathfindingSystem`, `RuntimeDiagnostics`, `GameLogger` time and `Random` seeding through it.
- Delete or fix `Float.round` (it breaks in comma-decimal locales).
- A CI guard: `engine` has no desktop or JVM-only dependencies (optional TeaVM compile check).

## Owner's input

Answer to review item 31 (2026-10-22, verbatim):

> yes, the `engine` should work on desktop, mobile, android, etc.

## Open questions

- Is web a real target or a nice-to-have? It decides whether a TeaVM compile check goes in CI now.
- May desktop-only dev tools (diagnostics file output, async pathfinding) be absent on web and Android?

## Sources

- `NOTES.md` in this folder.
