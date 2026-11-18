# Scope: mega-refactor-077d-reactor-man

What this branch changes and why. Write this file after the scan, with `CONTRACT.md` and before
`BRIEF.src.html`. This is Claude's record. The owner approves the brief, not this file. If the scope
changes later, update this file and the brief, and ask the owner again.

- Status: draft
- Approved: <date, or "no">

## Goal

Draft (2026-11-18, owner request in the `078a` play test). Not approved: the brief decides.

A richer Reactor Man fight (`ReactorMan`, `TubeBeamerV2`), in the spirit of `077a` to `077c`. Part of it: redraw the
ceiling lasers (the `TubeBeamerV2` beam and its flash colors) and the ceiling emitters, so that they match each
other.

## Item

The item (`ITEMS.md` ID) and the parts of its `ITEM.md` that this branch takes, copied here in full.

## Current state

What the scan found: the classes, call sites, events and data involved, how they connect, and
their problems. Use `file_path:line` references.

## In scope

- The fight upgrade (attacks, phases; to be scanned).
- New laser and emitter art. The emitter today is tile art: `Reactor_Man_3` tiles 210 and 211 (rotated), with
  see-through ports where `TubeBeamerV2` draws its black, pink and white flash square behind the tiles.
- Both places that host the fight: the Reactor Man stage, and the refight in Wily Stage 1 (`reactor_man_room` of
  `WilyStage1_v2.tmx`). After the new art is done, update the refight room: replace the 28 old `Reactor_Man_3`
  cells (rows 45 and 46, columns 304 to 317) with the new emitter, drawn in the Wily Stage 1 palette, and remove
  the `Reactor_Man_3` tileset from that map if no cell uses it any more (see `078a`).

## Out of scope

## Affected areas and risks

The files and systems this branch touches, and what could break.

## Overlap with other branches

## Objective check

The always-on rules in `objectives/README.md` and the rules of the linked objectives that this branch
can affect. Effect: `helps`, `no effect`, `waiting` or `conflict`. A conflict goes to the owner before
approval.

| Rule | Effect | Note |
|------|--------|------|

## Open questions
