---
status: open
verified: yes
area: core
gameplay-change: no
found-by: item 014 research (2026-10-22)
related-items: [014, 044]
---

# Five contact debug logs name the wrong phase or fixture type

## Where

`MegaContactListener.kt`:

| Line | Branch | Log text |
|------|--------|----------|
| 117 | begin `DEATH` × {`FEET`, `SIDE`, `HEAD`, `BODY`} | `Death-Feet/Side/Head/Block` (should be `Body`) |
| 890 | continue `BLOCK`-`BODY` | `endContact(): Block-Body` |
| 1212 | continue `LASER`-`BLOCK` | `beginContact(): Laser-Block` |
| 1240 | continue `LASER`-`BODY` | `beginContact(): Laser-Body` |
| 1255 | continue `LASER`-`SHIELD` | `beginContact(): Laser-Shield` |

## What happens

With contact debug logs on, these lines report the wrong phase or type, which misleads debugging.

## What should happen

The text names the real phase and types. No gameplay effect.

## How to reproduce

Read the lines above.

## Notes

Item `014` replaces these logs with a dispatcher log that uses rule IDs, which fixes this.
