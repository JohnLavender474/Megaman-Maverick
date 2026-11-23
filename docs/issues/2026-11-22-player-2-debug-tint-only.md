---
status: open
verified: question
area: core
gameplay-change: yes
found-by: co-op status research (2026-11-22)
related-items: [047, 060, 081]
---

# Player 2 has only a debug tint, and no plan exists for a real look

## Where

- `core/src/main/kotlin/com/megaman/maverick/game/entities/megaman/components/SpritesComponent.kt:46` (`SECOND_PLAYER_TINT = Color.ORANGE`) and `:115` (applied when the slot index is above 0, with a `TODO` that names an open choice in `047`).
- `docs/mega-refactor/items/047-co-op/ITEM.md:42`: "for now, player 2 has a debug tint as a marker."

## What happens

- Slot 1 draws the main `Megaman` sprite with a flat `setColor` tint. No shader or palette swap is used.
- The jetpack flame sprite (`defineJetpackFlameSprite`) is not tinted.
- Shots, charge effects, weapon sprites, beam-in and death effects look the same for both players.
- `047` has no open question for the real marker, although the code `TODO` points to one.

## What should happen

The owner decides how player 2 looks, for example a palette swap of the NES colors, separate sprites, or a different character per slot (`060i`, `081`). The choice is then an open question or a decision in `047`.

## How to reproduce

`./run-game-test-coop.sh`: slot 1 is orange; its jetpack flame and shots have the normal colors.

## Notes

- `047` `NOTES.md` ("Tint"): the tint is never cleared, which matters only if an object moves from slot 1 to slot 0.
- Palette and color-count rules for art are in `docs/ways-of-working/art.md`.
