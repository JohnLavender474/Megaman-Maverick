# Plan: mega-refactor-047k-gate-gather

New API and its first production caller:

| API | First caller |
|-----|--------------|
| `Gate.markTouched(player)` | `GateRule` (`BEGIN` and `CONTINUE`) |
| `AXIS_ONLY` on `NEXT_ROOM_REQ` | written by `Gate`, read by `MegaLevelScreen` |

1. **Test map.** A subagent adds `coop_gate` and `coop_gate_next` to `RefactorTest047.tmx` (copy target:
   `RefactorTest051.tmx` gate `212`, block `211`). Check: level-analyzer readout.
2. **Contact rule.** `GateRule` marks the touching player; `MegaContactRules` adds `CONTINUE`;
   `contact-rule-selection.txt`. Check: `GateRuleTest`, `ContactRuleSelectionTest`.
3. **Gate rule.** Touched set, the open check before the `OPENING` step, `trigger(axisOnly)`. Check: new `Gate` tests.
4. **Room request.** `NEXT_ROOM_REQ` carries `AXIS_ONLY`; `MegaLevelScreen` passes it. Check: a test of the event.
5. **Mutation checks.** One batch. Check: each fails its named case.
6. **Automated checks** of `CONTRACT.md`.
7. **Review before the play test.** Then the play checks and Diffity.
