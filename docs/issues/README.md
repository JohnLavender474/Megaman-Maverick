# Issues

Bugs and behavior questions, one Markdown file per issue. Refactor ideas do not go here: they
are items in `docs/mega-refactor/ITEMS.md`.

## Rules

- File name: `YYYY-MM-DD-<slug>.md`, with the date the issue was found. Dates and slugs do not
  collide between parallel branches, so no numbers are needed.
- Create, change and close issue files only on `master`. A branch that finds a bug writes it in
  its `docs/TODO.md` under "Bug candidates". The issue is filed on `master` from there.
- A fix goes in its own branch `fix/<slug>` off `master`, with a short contract: what changes and
  what must not change, and the named checks for both. A fix reads the always-on rules in
  `docs/mega-refactor/objectives/README.md`, and a conflict goes in the short contract.
- A fix that changes gameplay lists its play checks in the short contract, with each full command
  and what to look at. Use the format in "Test maps" in `docs/mega-refactor/CLAUDE.md`: a list, the
  worktree path first, experimental checks before control checks, and a `--level` and `--room` on
  each check. List a control check for each live level that the change can affect. When the scope
  of a fix grows, update the short contract and its checks in the same session.
- Each time a commit on `master` changes the issue file or the short contract of an open fix branch,
  merge `master` into that branch at once (`git merge master` in its worktree), so that the copy in
  the branch stays current. A docs-only merge needs no test run.
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
| [The room-music block on respawn reads the room before Mega moves, and can throw](2026-10-22-respawn-stale-room-music.md) | yes | core | closed |
| [A pause during a gate opening turns physics and behaviors on again when the game resumes](2026-10-22-pause-during-gate-reenables-physics.md) | yes | core | open |
| [When a health fill ends during a pause, all systems turn on while the game is still paused](2026-10-22-health-fill-end-unpauses-systems.md) | no | core | open |
| [Saved keyboard bindings are not applied at boot](2026-10-22-saved-key-bindings-not-applied.md) | yes | core | closed |
| [`selectButtonAction` resets at every boot although it is saved](2026-10-22-select-button-action-resets.md) | yes | core | closed |
| [Volume changes in game are not saved](2026-10-22-volume-not-saved.md) | yes | core | open |
| [The save keeps which health tanks the player has, not how full they are](2026-10-22-health-tank-fill-not-saved.md) | yes | core | open |
| [`runQueue` runs the last added runnable first](2026-10-22-run-queue-lifo.md) | yes | core | open |
| [`OrderedSet.random()` changes the order of the set](2026-10-22-ordered-set-random-reorders.md) | yes | engine | open |
| [`superRandom()` makes a new clock-seeded `Random` on each call](2026-10-22-super-random-reseeds.md) | yes | engine | open |
| [The frame delta is not clamped](2026-10-22-frame-delta-not-clamped.md) | yes | core | open |
| [`Float.round` throws in comma-decimal locales](2026-10-22-float-round-locale.md) | yes | engine | open |
| [`GapingFish` crashes on spawn: the atlas has no `GapingFish/frozen` region](2026-10-23-gaping-fish-missing-frozen-region.md) | yes | content | closed |
| [`engine` pins `gdx-freetype:1.11.0` against `gdxVersion` 1.14.2 and pulls the unused `gdx-tools:1.6.3`](2026-10-22-engine-gradle-dependencies.md) | yes | build | open |
| [12 TexturePacker projects point to missing source folders, and 3 atlases have no project](2026-10-22-stale-tps-files.md) | yes | content | open |
| [`WILY_CASTLE_SCREEN` is in `ScreenEnum` but not registered](2026-10-22-wily-castle-screen-not-registered.md) | yes | core | open |
| [The TMX background `section` property has no effect](2026-10-22-background-section-ignored.md) | yes | core | open |
| [An event spawner may drop `BEGIN_ROOM_TRANS` for enemies that the room transition destroyed](2026-10-22-spawner-drops-room-transition-event.md) | yes | core | closed |
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
| [A gamepad model with no saved bindings gets code `0` on all 8 buttons after "LOAD SAVED SETTINGS"](2026-10-25-preferred-controller-code-zero.md) | yes | core | closed |
| [Leaving the keyboard or gamepad settings screen during a level restarts the level](2026-10-25-bindings-screen-restarts-level.md) | yes | core | closed |
| [Players cannot bind a button to the `P` key without causing screenshots](2026-10-25-p-key-binding-takes-screenshots.md) | yes | core | open |
| [After gamepad 0 is unplugged while another gamepad stays, the game keeps the old gamepad's codes](2026-10-25-gamepad-index-shift-keeps-old-codes.md) | yes | core | closed |
| ["RESET TO DEFAULTS" in the key and gamepad settings is not saved](2026-10-25-reset-to-defaults-not-saved.md) | yes | core | closed |
| [A gamepad disconnect during a button capture leaves the capture state and the listener behind](2026-10-25-capture-state-left-after-gamepad-disconnect.md) | yes | core | closed |
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
| [`RobotMasterIntroScreen.dispose` logs an error at shutdown after an uncaught exception](2026-10-30-robot-master-intro-dispose-after-audio.md) | yes | core | open |
| [The `SPAWN` damage lock appears to have no effect](2026-10-30-spawn-damage-lock-no-effect.md) | no | core | open |
| [`Saw` type `t` passes a velocity to `body.setCenter`](2026-10-30-saw-trajectory-velocity-as-position.md) | no | core | open |
| [Make the `BigAssMaverickRobot` hand motion less uniform](2026-10-30-big-ass-maverick-robot-hand-motion.md) | question | core | open |
| [`CameraRoomTransition.cancel()` does not reset its timers](2026-10-30-camera-room-transition-cancel-timers.md) | no | core | open |
| [A player spawn in the map's bottom-left area replaces the start spawn](2026-10-30-spawn-in-start-area-replaces-start-spawn.md) | yes | core | open |
| [Question: `CameraRoomTransition` uses the raw viewport size on a turned camera](2026-11-01-camera-room-transition-raw-viewport-size.md) | question | engine | open |
| [A `START` release at the start of a lock pauses the game, and the pause cannot be left](2026-11-01-start-release-at-lock-start-pauses.md) | no | core | closed |
| [A `HeartTank` that falls on quicksand does not sink](2026-11-01-heart-tank-does-not-sink-in-sand.md) | yes | core | closed |
| [With left and right both held, Mega runs in place and can face a wall during a wall slide](2026-11-01-left-right-both-held.md) | yes | core | closed |
| [With left and right both held during a `RODENT_CLAWS` shot, the facing goes right and never left](2026-11-01-rodent-claws-run-facing.md) | yes | core | closed |
| [A `START` release at the start of a lock from a listener can still pause, with a dead menu](2026-11-01-listener-lock-start-release-pauses.md) | no | core | open |
| [A focus-loss or minimize pause under a lock gives a pause menu that a pad cannot use](2026-11-01-focus-loss-pause-under-lock.md) | no | core | open |
| [A press read carries a held button to the next screen](2026-11-01-press-read-carries-held-button.md) | no | core | open |
| [A rebind of `START` or `A` with the key held past `0.25` s confirms the row again](2026-11-01-rebind-confirm-held-key.md) | no | core | open |
| [`Esc` on the level select screen after an extra level opens the extras rows](2026-11-01-level-select-back-after-extra-level.md) | no | core | open |
| [The stun bounce assumes that Mega's body direction is `UP`](2026-10-30-stun-bounce-assumes-up.md) | yes | core | open |
| [Entities keep a pooled scratch object in a field, so `CeilingCrusher` teleports and kills at once](2026-11-01-stored-scratch-vector-in-entity-fields.md) | yes | core | open |
| [`JetMet` `init()` throws: the atlas has no frozen region, and a live map uses it](2026-11-01-jet-met-no-frozen-region.md) | yes | content | closed |
| [`ColtonJoe` `init()` throws: the atlas has no `ColtonJoe/frozen` region](2026-11-01-colton-joe-no-frozen-region.md) | yes | content | closed |
| [`TreeStump` `init()` throws: it passes the enum name as the atlas path](2026-11-01-tree-stump-wrong-atlas-key.md) | yes | core | closed |
| [10 entity classes that no live map uses fail `init()`](2026-11-01-unused-entities-fail-init.md) | yes | content | open |
| [4 entities in live maps have a missing region, and `init()` does not throw](2026-11-01-silent-missing-regions.md) | no | content | open |
| [With `--players 2`, two players on one quicksand area cannot jump out](2026-11-01-two-players-stuck-in-quicksand.md) | yes | core | closed |
| [A Koopa shell takes its roll direction from the target, not from the player who kicked it](2026-11-02-koopa-shell-rolls-toward-target.md) | no | core | open |
| [Question: enemies copy their direction from the target player](2026-11-02-enemy-direction-from-target.md) | question | core | open |
| [With `--players 2`, a dead slot 1 still counts at the `players.any` sites](2026-11-02-players-any-counts-dead-slot.md) | yes | core | closed |
| [`DarknessV4` loops while its iterator has no next entry](2026-11-02-darkness-v4-inverted-iterator-loop.md) | no | core | open |
| [Retire Gauntlet 2 and 4 entities that only it or test maps use](2026-11-02-retire-gauntlet-2.md) | yes | core | closed |
| [The `BODY_TOUCHING_BLOCK` check on an enemy's item drop is never true](2026-11-02-enemy-drop-block-check-never-true.md) | yes | core | open |
| [The y velocity of a `PushableBlock` grows with no limit while it rests](2026-11-02-pushable-block-velocity-builds-at-rest.md) | yes | core | closed |
| [A `PushableBlock` stays over a hole on a sliver of the ledge](2026-11-02-pushable-block-ledge-sliver.md) | yes | core | closed |
| [On the last 1.6 px of a ledge, Mega Man stays up in the jump pose](2026-11-02-ledge-sliver-jump-pose.md) | no | core | open |
| [With `--players 2`, the death of slot 1 destroys P1's gems and resets a gauntlet](2026-11-02-slot-1-death-acts-on-p1-objects.md) | yes | core | open |
| [With `--players 2`, only slot 0 dies past a camera edge](2026-11-02-slot-1-no-camera-edge-death.md) | yes | core | open |
| [A dead player's weapon `spawned` sets are not cleared](2026-11-02-weapon-spawned-sets-survive-death.md) | no | core | closed |
| [`throwAxe` runs after a `0.1` s delay with no live check](2026-11-02-throw-axe-no-live-check.md) | no | core | closed |
| [The factory preload runs `init()` twice for 25 `Bullet` and 25 `Block`](2026-11-02-entity-factory-preload-runs-init-twice.md) | yes | core | open |
| [`LocksComponent.reset()` does not call its change hook](2026-11-02-locks-reset-skips-change-hook.md) | no | engine | open |
| [Question: the screen and the boss read `mini` and `end` from different sources](2026-11-02-boss-props-have-two-sources.md) | question | core | open |
| [The check that stops an item drop on a cart kill is always true](2026-11-02-cart-kill-drop-check-always-true.md) | yes | core | open |
| [Question: a moving `CartV2` kills a boss in one hit](2026-11-02-cart-kills-boss-in-one-hit.md) | question | core | open |
| [Question: a player shot in flight can hit a boss during its intro](2026-11-02-boss-takes-hits-during-intro.md) | question | core | open |
| [On a second `BigAssMaverickRobot` fight, a hand floats in midair while the boss falls](2026-11-02-big-ass-robot-hand-floats-on-second-fight.md) | yes | core | closed |
| [A camera-bounds spawner respawns its enemy after a step of 1 px out of the view and back](2026-11-05-bounds-spawner-respawns-greedily.md) | yes | core | closed |
| [`X6_INTRO` `room2`: the floor block is 1 tile below the painted floor, and the boss gate is above standing height](2026-11-06-x6-intro-room2-floor-below-art.md) | yes | content | open |
| [Code casts `IAnimator` to `Animator` to read animator state, and `Animator` mixes several concerns](2026-11-06-animator-casts-and-composition.md) | yes | engine, core | open |
| [A camera shake that runs at level exit goes on in the next level](2026-11-06-camera-shake-survives-level-exit.md) | no | core | open |
| [`Background.update` builds a debug string each frame, also when its tag is filtered out](2026-11-09-background-debug-string-each-frame.md) | yes | core | open |
| [The camera "rounding" in `MegaLevelScreen.render` does nothing](2026-11-09-camera-rounding-does-nothing.md) | yes | core | open |
| [Hot paths allocate per frame or per step where no item covers it](2026-11-09-per-frame-allocations-hot-paths.md) | yes | engine, core | open |
