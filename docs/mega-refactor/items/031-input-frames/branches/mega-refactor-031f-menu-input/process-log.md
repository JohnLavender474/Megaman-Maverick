# Process log: mega-refactor-031f-menu-input

## Status

- State: blocked
- Goal: the menus and skip screens read frames, with `Esc` and the back action; the `controllerPoller` facade goes
- Done:
- Next: the scan and a draft `docs/SCOPE.md`, after `031e` merges
- Blocked: by `031d` (devices: the keyboard aliases and `Esc` as a reserved system key) and `031e` (bindings: it
  changes the bindings screen, which is one of the menus)

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-30

- No branch exists yet. The folder was prepared on `master`. This is the "menu-input branch" of the `031` decisions
  of 2026-10-26 (item `ITEM.md`).
- Proposed goal (not a scope; the scan and `docs/SCOPE.md` decide):
  - The menus (`MegaMenuScreen` and its subclasses), the skip screens (`LogoScreen`, `AbstractStoryScreen`,
    `GameOverScreen`, `GetWeaponScreen`, `SimpleInitGameScreen`, `CreditsScreen`) and `ScreenController` read frames,
    not `game.controllerPoller`.
  - `Esc` is back in menus and pause in gameplay (`031` decision of 2026-10-25, with a `TODO` to consider another
    action later). The back action in menus.
  - Removals when no caller is left: `game.controllerPoller`, `FrameControllerPoller` and `FrameControllerPollerTest`,
    the engine `IControllerPoller`, and the diagnostics entry label `"controllerPoller"` in
    `MegamanMaverickGame.render`.
- The scan waits for `031e` to merge, because `031d` and `031e` change the code that this branch reads.
