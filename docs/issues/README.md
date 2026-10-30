# Issues

Bugs and behavior questions, one Markdown file per issue. Refactor ideas do not go here: they
are items in `docs/mega-refactor/ITEMS.md`.

## Rules

- File name: `YYYY-MM-DD-<slug>.md`, with the date the issue was found. Dates and slugs do not
  collide between parallel branches, so no numbers are needed.
- Create, change and close issue files only on `master`. A branch that finds a bug writes it in
  its `docs/TODO.md` under "Bug candidates". The issue is filed on `master` from there.
- A fix goes in its own branch `fix/<slug>` off `master`, with a short contract: what changes and
  what must not change.
- A refactor branch that must change buggy behavior names the issue file in its `CONTRACT.md`
  under "Intended changes".
- To close an issue, set `status: closed` and add a `## Resolution` section with the commit or
  branch. Do not delete the file.
- Keep the index below up to date.

## Front matter

```yaml
status: open | closed
verified: yes | no | question
area: engine | core | content | build
gameplay-change: yes | no | unknown
found-by: <branch or session> (<date>)
related-items: [<mega-refactor item IDs>]
```

- `verified: yes`: confirmed by reading the code or by a test.
- `verified: no`: found by reading; needs a check in the game.
- `verified: question`: maybe intended. The owner decides.
- `gameplay-change`: whether the fix changes how the game plays.

## Sections

`Where`, `What happens`, `What should happen`, `How to reproduce`, and optional `Notes` and
`Resolution`.

## Index

| Issue | Verified | Area | Status |
|-------|----------|------|--------|
| [`CullablesSystem` skips culling for the remaining entities after one entity without a cullable](2026-10-22-cullables-system-early-return.md) | yes | engine | open |
| [`InsertionOrderPriorityQueue.size` is always 0](2026-10-22-insertion-order-priority-queue-size.md) | yes | engine | open |
| [`MutableOrderedSet.retainAll` removes nothing and reports the wrong result](2026-10-22-mutable-ordered-set-retain-all.md) | yes | engine | open |
| [`replace("\\s+", "")` treats the pattern as literal text](2026-10-22-literal-whitespace-regex.md) | yes | core | open |
| [The room-music block on respawn reads the room before Mega moves, and can throw](2026-10-22-respawn-stale-room-music.md) | yes | core | open |
| [A pause during a gate opening turns physics and behaviors on again when the game resumes](2026-10-22-pause-during-gate-reenables-physics.md) | yes | core | open |
| [When a health fill ends during a pause, all systems turn on while the game is still paused](2026-10-22-health-fill-end-unpauses-systems.md) | no | core | open |
| [Saved keyboard bindings are not applied at boot](2026-10-22-saved-key-bindings-not-applied.md) | yes | core | open |
| [`selectButtonAction` resets at every boot although it is saved](2026-10-22-select-button-action-resets.md) | yes | core | open |
| [Volume changes in game are not saved](2026-10-22-volume-not-saved.md) | yes | core | open |
| [The save keeps which health tanks the player has, not how full they are](2026-10-22-health-tank-fill-not-saved.md) | yes | core | open |
| [`runQueue` runs the last added runnable first](2026-10-22-run-queue-lifo.md) | yes | core | open |
| [`OrderedSet.random()` changes the order of the set](2026-10-22-ordered-set-random-reorders.md) | yes | engine | open |
| [`superRandom()` makes a new clock-seeded `Random` on each call](2026-10-22-super-random-reseeds.md) | yes | engine | open |
| [The frame delta is not clamped](2026-10-22-frame-delta-not-clamped.md) | yes | core | open |
| [`Float.round` throws in comma-decimal locales](2026-10-22-float-round-locale.md) | yes | engine | open |
| [`GapingFish` crashes on spawn: the atlas has no `GapingFish/frozen` region](2026-10-23-gaping-fish-missing-frozen-region.md) | yes | content | open |
| [`engine` pins `gdx-freetype:1.11.0` against `gdxVersion` 1.14.2 and pulls the unused `gdx-tools:1.6.3`](2026-10-22-engine-gradle-dependencies.md) | yes | build | open |
| [12 TexturePacker projects point to missing source folders, and 3 atlases have no project](2026-10-22-stale-tps-files.md) | yes | content | open |
| [`WILY_CASTLE_SCREEN` is in `ScreenEnum` but not registered](2026-10-22-wily-castle-screen-not-registered.md) | yes | core | open |
| [The TMX background `section` property has no effect](2026-10-22-background-section-ignored.md) | yes | core | open |
| [An event spawner may drop `BEGIN_ROOM_TRANS` for enemies that the room transition destroyed](2026-10-22-spawner-drops-room-transition-event.md) | no | core | open |
| [The same-name "4" spawns in ReactorMan and DesertMan likely activate the far-side spawn](2026-10-22-duplicate-checkpoint-4.md) | no | content | open |
| [Question: explosions from Mega's own shots do not hurt `AbstractEnemy`](2026-10-22-mega-explosions-do-not-hurt-enemies.md) | question | core | open |
| [Question: a checkpoint can change silently while Mega is not ready](2026-10-22-checkpoint-changes-while-not-ready.md) | question | core | open |
| [Question: coins and level points reset only at level start, not on respawn](2026-10-22-coins-not-reset-on-respawn.md) | question | core | open |
| [The build fails when Gradle runs on JDK 25](2026-10-22-jdk25-test-jvm-target.md) | yes | build | open |
| [14 TMX objects in live maps never spawn: wrong layer or unknown class](2026-10-22-misplaced-tmx-objects.md) | yes | content | open |
| [`MagmaWave`'s shield receiver never fires: the filter never makes `SHIELD`-`SIDE` contacts](2026-10-22-shield-side-contact-never-made.md) | yes | core | open |
| [The `BLOCK`-`GRAVITY_CHANGE` contact rule never runs](2026-10-22-block-gravity-change-rule-never-runs.md) | yes | core | open |
| [The filter makes `PLAYER`-`DEATH` contacts that no rule handles](2026-10-22-player-death-contact-unhandled.md) | yes | core | open |
| [Contact state and hooks that nothing uses: `TOUCHING_CART`, `ConstKeys.CART`, `setHitByLaserReceiver`](2026-10-22-unused-contact-hooks.md) | yes | core | open |
| [Five contact debug logs name the wrong phase or fixture type](2026-10-22-wrong-contact-log-texts.md) | yes | core | closed |
| [`hitExplosion` gets its two shapes in the reverse order](2026-10-22-projectile-explosion-shape-order.md) | yes | core | open |
| [`BouncingPebble` bounces to the right when its right side hits a block](2026-10-22-bouncing-pebble-right-side-bounce.md) | no | core | open |
| [`GameLine.rotate` sets `originY` to the `originX` value](2026-10-25-game-line-rotate-origin-y.md) | yes | engine | open |
| [`isPerformanceSubpar()` is true for the high tiers and has no caller](2026-10-25-is-performance-subpar-inverted.md) | yes | core | open |
| [A gamepad model with no saved bindings gets code `0` on all 8 buttons after "LOAD SAVED SETTINGS"](2026-10-25-preferred-controller-code-zero.md) | yes | core | open |
| [Leaving the keyboard or gamepad settings screen during a level restarts the level](2026-10-25-bindings-screen-restarts-level.md) | yes | core | open |
| [Players cannot bind a button to the `P` key without causing screenshots](2026-10-25-p-key-binding-takes-screenshots.md) | yes | core | open |
| [After gamepad 0 is unplugged while another gamepad stays, the game keeps the old gamepad's codes](2026-10-25-gamepad-index-shift-keeps-old-codes.md) | no | core | open |
| ["RESET TO DEFAULTS" in the key and gamepad settings is not saved](2026-10-25-reset-to-defaults-not-saved.md) | yes | core | open |
| [A gamepad disconnect during a button capture leaves the capture state and the listener behind](2026-10-25-capture-state-left-after-gamepad-disconnect.md) | yes | core | open |
| [`jamepadVersion` in `gradle.properties` does not match the Jamepad version that the build uses](2026-10-25-jamepad-version-mismatch.md) | yes | build | open |
| [Five enemies in `DESERT_MAN` are on layers where they cannot spawn](2026-10-27-desert-man-enemies-on-wrong-layers.md) | question | content | open |
| [A `DuoBallCanon` in `WILY_STAGE_1` is on the `blocks` layer and never spawns](2026-10-27-wily-stage-1-duoballcanon-on-blocks-layer.md) | question | content | open |
| [`Starkner` clears its `ALIVE` entry only in its `destroy()` override](2026-10-27-starkner-alive-cleanup-only-in-destroy.md) | yes | core | closed |
| [`SpiderWeb.onDestroy` does not call `super.onDestroy()`](2026-10-27-spider-web-skips-super-on-destroy.md) | yes | core | closed |
| [A moving `CartV2` pushes dropped items that lie on the ground in front of it](2026-10-28-cart-pushes-dropped-items.md) | yes | core | closed |
| [The end-level beam sets the Megaman beam flag to `true` when the beam ends](2026-10-28-end-level-beam-flag-left-true.md) | question | core | open |
| [`PathfindingComponent` keeps its old path through a destroy and a new spawn](2026-10-28-pathfinding-component-keeps-old-path.md) | no | engine | open |
| [Refine the final Dr. Wily boss fight](2026-10-29-refine-wily-final-boss-fight.md) | question | core, content | open |
| [Question: a `down` gravity area turns the camera upside down, so the level shows upside down](2026-10-29-gravity-change-turns-camera-upside-down.md) | question | core | open |
| [Needle Spin does not bounce Megaman up while he rises from a jump and JUMP is held](2026-10-29-needle-spin-no-bounce-while-jump-held.md) | yes | core | open |
