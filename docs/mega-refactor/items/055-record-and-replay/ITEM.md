# 055: Record and replay for contract checks

- Status: planned
- Type: feature
- Depends on: `010`, `011`, `006`, `031`
- Related: `008`, `009`
- Branches: none yet

## Goal

Check the gameplay contract with less manual work: record everything relevant in a frame, and replay or compare it frame by frame.

## Owner's input

From the record-and-replay discussion (summary): inputs alone are not enough; the recorder must capture any relevant data (events, actions, changes, drawings, movements) for a frame-by-frame replay, and recording must not lower the FPS. Manual testing is fine at the start.

## Open questions

- Must replays reproduce real player sessions, or only scripted inputs? (Also asked in `010`.)

## Sources

- `NOTES.md` in this folder.
- `items/010-fixed-tick/NOTES.md`: what the recorder must record about timing.
- `items/006-headless-boot-run/NOTES.md`: the headless runner (stage A).
