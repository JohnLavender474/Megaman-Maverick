# Plan: mega-refactor-047i-transition-path

Revised 2026-11-17 for D3 (see `SCOPE.md`). Steps 1 to 7 of the first plan are done (process log); this plan
replaces the `LevelState` field.

New API and its first production caller:

| API | First caller |
|-----|--------------|
| `CameraRoomTransition.axisOnly` and `start(..., axisOnly)` | `CameraRoomBounds.transitionToRoom` |
| `CameraRoomBounds.transitionToRoom(..., axisOnly)` | `CameraRoomExits.update` |
| `RoomTransitionEventKeys.AXIS_ONLY` | written by `LevelRoomHandler`, read by `MegaPlayer` |

1. **Remove the field.** `LevelState.gatheredExitSide`, its 3 clears, the `LevelState` argument of `CameraRoomExits`.
2. **Engine.** `CameraRoomTransition.axisOnly`; `CameraRoomBounds.transitionToRoom(..., axisOnly)`. Check: new
   `CameraRoomTransitionTest`.
3. **Writer.** `CameraRoomExits` passes `axisOnly = true`. Check: `CameraRoomExitsTest`.
4. **Events.** `RoomTransitionEventKeys.AXIS_ONLY` on `BEGIN` and `CONTINUE`. Check: `LevelRoomHandlerTest`.
5. **Reader.** `MegaPlayer` reads the key. Check: `PlayerLifecycleTest`.
6. **Mutation checks.** One batch (engine clear, writer, event, reader). Check: each fails its named case.
7. **Automated checks** of `CONTRACT.md`.
8. **Review before the play test** (diff only; the map and the manual checks are unchanged). Then the play checks
   and Diffity.
