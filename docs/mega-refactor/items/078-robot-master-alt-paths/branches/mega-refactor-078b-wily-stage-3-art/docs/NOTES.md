# Notes: mega-refactor-078b-wily-stage-3-art

## Decisions

- 2026-11-19 (owner, round-1 review https://claude.ai/artifact/XcDwaErABYx2Q47KjzrToB):
  - S1: thick walls are teal all the way in (no dark deep panel).
  - F3: in the tall furnace rooms (`room3`, `room5`) the furnace background is one copy at the room bottom, with
    the plain lab wall above (2 background objects).
  - R1: the emblem plate of each refight boss goes in the room before the refight room, as a sign of the next boss.
  - Owner accepted the recommendations: S2 the trace is in a new `decor` tile layer after `tiles1`; S3 the trace
    runs under every floor top; F2 the ember rim on all bottom edges of the furnace area; R2 the emblem colors are
    sampled from each boss sprite; C2 the `wily_room` blocks are pale steel with a violet core.
  - The 10 smaller round-1 choices (E1 to E3, F1, F4, R3, C1, C3, B1, B2) stay as drawn.
- 2026-11-19 (owner): the conveyor belts of the stage get their own art. Owner accepted the recommendation: a new
  `ConveyorBelt` `type` value (lab style) with new frames in `Platforms1`, set on the 15 belts of
  `WilyStage3_v4.tmx`. Today they use `type=green`, the Glacier Man art; Glacier Man keeps `green`.

- 2026-11-19 (owner accepted the recommendations, round-2 review version 2):
  - Q1: `room7` holds both plates in fight order, gem (Precious Woman) left and leaf (Timber Woman) right;
    `precious_woman_room` has no plate (`temp/078-wily3-art/3-refight/round2/option_both_plates_in_room7.png`).
  - Q2: the 2 backgrounds of `room3` and `room5` use `parallax_y` 1, so the furnace copy stays at the room bottom.
  - Q3: the lab belt keeps the moving cyan chevrons.
  - Q4: the lab belt keeps the round ends.

## Overlap with other branches
