# Notes: 095 Netplay with lockstep

Research for this item: scans, counts, options, X6 facts (name the file and section) and
reviews. Give the date and the `master` commit that each finding is based on.

## What rollback is (2026-11-01)

Web claims come from page extracts, not a line-by-line read. The example with `Megaman` is an illustration, not a design.

Rollback is a way to run a networked game with no wait for remote input. Each machine guesses the input that has not arrived, and it plays on. If a guess was wrong, the machine goes back to the last tick with correct inputs, and it runs the game forward again with the real input. All of this happens inside one rendered frame. GGPO [pioneered this in peer-to-peer games](https://www.ggpo.net/) in 2009.

### Start from lockstep

In lockstep, each machine runs the same simulation from the same inputs. To run a tick, a machine needs the input of all players for that tick. If an input is late, the machine waits, or it delays all local input so that remote input can arrive in time. Rollback keeps the lockstep base, and it [adds a way to hide this delay](https://www.snapnet.dev/blog/netcode-architectures-part-2-rollback/). Only inputs go over the network in both cases.

### The steps on each tick

1. **Save.** The machine saves the complete game state of the tick. It keeps a short history of these states.
2. **Apply local input.** Local input runs at once, [as if the game were offline](https://words.infil.net/w02-netcode-p4.html).
3. **Predict remote input.** The machine guesses each missing input. Most implementations [carry the last known input forward](https://www.snapnet.dev/blog/netcode-architectures-part-2-rollback/).
4. **Run the tick and render.**
5. **Compare when the real input arrives.** If the guess was correct, the machine only records the input. [No correction occurs, and the player sees no problem](https://words.infil.net/w02-netcode-p4.html).
6. **Correct if the guess was wrong.** The machine [restores the last tick with all inputs known, runs forward with the real input, predicts the later ticks again, and renders the current tick](https://www.snapnet.dev/blog/netcode-architectures-part-2-rollback/).

The guess is often correct because players change their inputs rarely. Infil estimates [about 5 direction changes per second](https://words.infil.net/w02-netcode-p4.html), so about 8% of frames need new remote input.

### Example in this game

Inference. The values come from `MegamanValues.kt`, at 60 ticks per second. Two players run toward each other, with a 3-tick network delay.

- Tick 100: player B presses jump. B's machine jumps at once.
- Ticks 100 to 102 on player A's machine: B's input has not arrived. A's machine repeats B's last input, so B keeps running on A's screen.
- Tick 103 on A's machine: B's real input for tick 100 arrives, and it differs from the guess. A's machine restores tick 99, runs ticks 100 to 103 again with the jump, and draws tick 103.
- Result: A never sees the start of the jump. B pops about 1.2 tiles up on A's screen, because `JUMP_VEL` is 25 tiles/s. A's own character, shots and timers stay correct.

With lockstep, A's own input would have a 3-tick delay, and no pop occurs. Rollback changes a delay on your own actions into a visual correction of the other player's actions.

### What the player sees

- A remote action [appears a few frames into its start](https://words.infil.net/w02-netcode-p4.html), because the correction removes its first frames.
- A wrong movement guess can show as [teleporting or rubber-banding](https://www.snapnet.dev/blog/netcode-architectures-part-2-rollback/).
- Designers can [add startup frames](https://www.snapnet.dev/blog/netcode-architectures-part-2-rollback/) to an action, so the remote player still sees its important frames after a skip.
- Most implementations [also add a small input delay](https://www.snapnet.dev/blog/netcode-architectures-part-2-rollback/) to reduce the prediction. [Killer Instinct and Injustice 2 use a fixed 3 frames](https://words.infil.net/w02-netcode-p4.html). So rollback in shipped games is usually "a small fixed delay, then rollback for later inputs", not zero delay.

### What it needs

- All that lockstep needs: [determinism and a fixed tick rate](https://www.snapnet.dev/blog/netcode-architectures-part-2-rollback/).
- [Save and load of the full game state, and a tick that runs with no render](https://www.ggpo.net/).
- Enough CPU to run many ticks in one frame. [15 ticks in 16.66 ms leaves about 1.1 ms for each tick](https://www.snapnet.dev/blog/netcode-architectures-part-2-rollback/). If the corrections take longer than the frame, each later frame has more to catch up, and the game [can grind to a halt](https://www.snapnet.dev/blog/netcode-architectures-part-2-rollback/).
- Control of side effects. A correction must not play a sound again, so games [often track only whether an effect occurred, and start or cancel it](https://www.snapnet.dev/blog/netcode-architectures-part-2-rollback/).

### Limit

Prediction past about 100 to 150 ms [tends to get unplayable quickly](https://www.snapnet.dev/blog/netcode-architectures-part-2-rollback/), because corrections get too large. Rollback reduces the effect of latency, and it does not remove latency.

## Does PvP need rollback? (2026-11-01, `master` at `dc246a0c2`)

A research agent wrote this. No owner decision follows from it yet. Web claims come from page extracts, not a line-by-line read. Items marked "inference" are the agent's own conclusions.

**Result:** rollback is not strictly necessary. Fixed-delay lockstep fits a 2-player match at moderate ping. Plain peer-to-peer lockstep does not fit 4 or more players with strangers (inference), because of stalls and the slowest peer, not because of input delay. Rollback is one fix, and a sequencing relay is another. `010`, `011` and the determinism rules serve both, so no decision is necessary now.

### Reasons, tested

| # | Reason | Modes | Verdict for this game |
|---|---|---|---|
| 1 | Input delay on every action (90 ms ping is about 3 frames) | Both | Better. 2 to 3 frames can go unnoticed (Infil p3). |
| 2 | One late input stops all peers | Both, worse in PvP | Better, not unique to rollback. Rollback also stalls at its window end. A sequencing server removes the wait. |
| 3 | Jitter sets the buffer to the worst case | Both | Better. A fixed session delay removes the swing, but a spike becomes a stall. |
| 4 | The slowest peer sets the delay. With 10% poor links, a match has one in about 35% of cases at 4 players and 57% at 8 (SnapNet 1). | Both | A change is necessary at 4 or more with strangers (inference), but the change need not be rollback. |
| 5 | A changing delay breaks muscle memory | Both | Not needed if the delay is fixed for the session. |
| 6 | "Delay kills reactive dodges" | PvP | Not needed. Dodge margin is `b - r - d` with lockstep and `b - r - L` with rollback, and `d = L`, so they are equal (inference). |
| 7 | Anticipated actions (a read, a pre-timed jump, aim) | Mainly PvP | Better. This is the real PvP gain (inference from Infil p4). |
| 8 | Long distance: 150 ms is unplayable with delay in a fighting game, but fine with rollback (Infil p6) | Both | Necessary only if matches at about 150 ms ping or more are a target. A play test must confirm this for this game. |
| 9 | Compared with the Call of Duty model, a dodge against an interpolated shot can fail | PvP | Better than that model, because all combat here is projectiles and dodges. |

Not reasons for rollback: determinism, bandwidth (`InputFrame` is 1 `Long`), late join, fairness, packet loss alone.

### Facts from the code

- `Bullet` moves at 10 tiles/s, not 12: `BULLET_VEL = 12` (`MegamanValues.kt:94`), but `Bullet.CLAMP = 10f` (`Bullet.kt:53,208`). It crosses the 16-tile view in 96 ticks at 60 Hz.
- `DAMAGE_RECOVERY_TIME = 1.5` s (90 ticks) and `DAMAGE_DURATION = 0.5` s (`MegamanValues.kt:79-80`). So frame-exact combos do not exist as in fighting games (inference).
- A jump needs `isJustPressed` (`JumpBehavior.kt:49-50`). The agent's grep found no coyote time and no jump buffer.
- Minimum distance for a reactive dodge, with 190 ms reaction: 2.3 tiles at 0 ms latency, 3.3 at 100 ms, 3.8 at 150 ms (inference).
- A wrong prediction of a remote jump pops about 2.5 tiles at 100 ms, more than the body height. Instant moves with no startup make this a real cost (inference).

### Measured CPU

`--diagnostics`, Ryzen 7 PRO 5850U, JDK 25, `--performance medium`, idle player. No shots, one player, GPU time not measured.

| Capture | `screen.render` median / p95 / p99 | `WorldSystem` median |
|---|---|---|
| `DESERT_MAN --room room10` | 5.27 / 7.51 / 9.30 ms | 4.60 ms |
| `REFACTOR_TEST_016` | 0.97 / 1.80 / 3.18 ms | 0.72 ms |

Ticks that fit in 16.67 ms, before any save and restore cost: 3 at the stage median, 2 at p95; 17 at the small-map median, 9 at p95. So rollback is not practical in full stages today. It is plausible in a small arena on this machine, and unproven on weak hardware.

### What rollback needs here

`grep` line counts in `entities/`, approximate: 418 entity class files, 1,359 `var`, 1,121 mutable collection fields, 909 `Timer(` lines in `core`, 120 entity-typed fields, and 285 local timers or collections in `define*` functions that a field copy cannot reach. Pools also need work: a restore must undo a return to the pool (`GameEntityPoolCreator.kt:15`). Audio must not repeat in a resimulation (251 `requestToPlaySound` lines in `core`). A resimulation needs a tick with no render, and `ICameraView.overlaps` and 13 gameplay `isFinished()` reads block this.

A PvP map with no enemies needs about 1 in 25 of the 442 classes (inference), so a class whitelist makes rollback much smaller. Spec Ops has the larger state, so lockstep stays correct there whichever choice PvP makes (inference).

### What must stay true now, for any choice

1. A tick is a pure function of state and all slots' `InputFrame`s, with no wall clock and no render read.
2. The `010` loop allows 0, 1 or many ticks in one frame, and takes input per tick from a source that can be a queue.
3. `010` chooses few whole physics steps per tick, because each step is paid again in a resimulation.
4. Events, spawns and the camera run inside the tick.
5. Audio and effects go out after the tick, and can be dropped or deduplicated.
6. No new per-spawn state in captured locals.
7. No code outside the simulation keeps a reference to a pooled gameplay entity past a tick (`045`).
8. Cosmetic entities move out of the checked state.
9. The session layer sends `InputFrame`s with tick numbers, over unreliable transport with redundant inputs.
10. A sync test and the `055` per-tick state hash exist early.
11. PvP weapon design leaves room for 2 to 3 startup ticks (Infil p6).

### Agent's recommendation (inference)

Keep lockstep as the first netplay path for both modes. If strangers, about 150 ms ping or 4 or more net players are a target, the agent's best fit for PvP is 2 to 3 ticks of fixed delay plus rollback with a window of up to 8, for a class whitelist in a one-room map, with a relay. Measure a real arena with 4 players firing on a named weak machine before the decision.

### Gaps

- The agent could not read the networking of Mega Man X Online: Deathmatch, the closest precedent.
- GDC talks were read only through listings and secondary write-ups. The For Honor sources do not confirm GGPO-style rollback.
- No state size in bytes exists, because no serializer exists. Weak-machine and Wasm costs are not measured.

### Sources

- [Gaffer: Deterministic Lockstep](https://gafferongames.com/post/deterministic_lockstep/)
- [SnapNet: part 1, Lockstep](https://www.snapnet.dev/blog/netcode-architectures-part-1-lockstep/), [part 2, Rollback](https://www.snapnet.dev/blog/netcode-architectures-part-2-rollback/), [part 3, Snapshot Interpolation](https://www.snapnet.dev/blog/netcode-architectures-part-3-snapshot-interpolation/)
- [GGPO](https://www.ggpo.net/), [GGRS sessions](https://docs.rs/crate/ggrs/0.13.0/source/docs/sessions.md)
- [Infil p3](https://words.infil.net/w02-netcode-p3.html), [p4](https://words.infil.net/w02-netcode-p4.html), [p5](https://words.infil.net/w02-netcode-p5.html), [p6](https://words.infil.net/w02-netcode-p6.html)
- [Rollback networking in INVERSUS](https://www.gamedeveloper.com/design/rollback-networking-in-inversus)
- [Edgegap: MKX and Injustice 2](https://edgegap.com/blog/game-backend-deep-dive-mortal-kombat-x-injustice-2-rollback)
- [Claypool: Latency and Player Actions](https://ftp.cs.wpi.edu/pub/techreports/pdf/06-13.pdf)
- [Photon Quantum intro](https://doc.photonengine.com/quantum/current/getting-started/quantum-intro)
- [Gambetta: Lag Compensation](https://www.gabrielgambetta.com/lag-compensation.html)
