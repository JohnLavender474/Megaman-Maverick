# Death (sensor)

- **Layer:** `sensors` (entity type `SENSOR`; class
  `com.megaman.maverick.game.entities.sensors.Death`)
- **NOT the `hazards` layer.** Instant-death pits in real levels are built with `sensors:Death`,
  not with a `hazards` object. Many shipped levels have **no `hazards` layer at all** and rely on
  `Death` sensors for all environmental killing.
- **Known properties:**
  - `instant` — `bool`, default `false`. Marks the `DEATH` fixture as instant. Pit/void kill
    planes set `instant=true`.
- **Behavior:** spawns an abstract body whose single `DEATH` fixture exactly covers the object's
  bounds and kills anything overlapping it. No sprite — it is invisible in game and in the
  `tmx-visualizer` output is only seen where it overlaps a block (the `N-D/X` combo). The body
  always updates.

## Bottomless-pit pattern (the main use)

A bottomless pit is just a **gap in the floor blocks** with a wide `Death` kill-plane sitting
in the void **below the room rectangle**:

1. Leave a hole in the bottom block row of the room (omit the floor `Block`s across the pit
   columns). In the visualizer this reads as missing `X` cells in the bottom row.
2. Place one `Death` object spanning the pit's width, positioned **just below the room's bottom
   edge** (a few dozen px under the floor), tall enough to catch a falling player.
   - Because it sits below the room rect, it does **not** render in that room's per-room grid —
     a bottom-row gap with nothing else is the pit; the kill-plane is off-grid beneath it.
3. Set `instant=true` so the fall is an immediate death rather than chip damage.

### Canonical example — `assets/tiled_maps/tmx/IntroStage_v3.tmx`

`room1` is at `y=2592`, height `448` → floor bottom edge at `y=3040`. Its pit `Death` sensors:

| id | x | y | w × h | instant |
|----|------|------|----------|---------|
| 20 | 864  | 3088 | 96 × 112 | true |
| 28 | 2176 | 3088 | 96 × 112 | true |
| 48 | 3424 | 3088 | 160 × 112 | true |

So each kill-plane starts `~48px` below the floor (3088 vs 3040), is `112px` tall (reaches well
into the void), and its width (`64`–`160px`, i.e. 2–5 tiles) matches the floor gap above it.

### In-room death strips (secondary use)

The same class also makes thin (`32px`-tall) death strips that lie **on a ledge inside** a room
(e.g. IntroStage ids 410/412/415/416, which leave `instant` at its default). These render as the
`N-D/X` combo where the strip overlaps the surface blocks (see `room3`/`room6` in the IntroStage
visualization).

## Related

- Visible instant-kill obstacles use `hazards:Spike` instead — see
  `levels/hazards/spike/Spike.md`. Use `Spike` when the player should *see* the threat; use a
  `Death` sensor for invisible void/pit planes.
