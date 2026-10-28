---
status: closed
verified: yes
area: core
gameplay-change: yes
found-by: owner (2026-10-28)
related-items: []
---

# A moving `CartV2` pushes dropped items that lie on the ground in front of it

## Where

- `core/src/main/kotlin/com/megaman/maverick/game/entities/special/CartV2.kt`: `CartV2` extends `Block`, so its body
  has a `BLOCK` fixture that moves.
- `core/src/main/kotlin/com/megaman/maverick/game/entities/items/`: the dropped items (`HealthBulb`,
  `WeaponEnergyBulb`, `Life`; base classes `AbstractItem`, `AbstractEnergyItem`).
- Live content: `DESERT_MAN`, `CartV2` objects in `room9` (1) and `room10` (2).

## What happens

Reported by the owner from play. A dropped item (health, weapon energy, a life) lies on the ground. A `CartV2` moves
toward it. When the cart reaches the item, the cart pushes the item along in front of it.

An item that lies on top of a moving cart moves with the cart. That part is correct.

## What should happen

- An item on the ground in front of a moving cart does not move at all. The cart moves over (through) the item, and the
  item stays where it is.
- An item that rests on top of a moving cart still moves with the cart, as it does today.
- Nothing else changes: Megaman, enemies, projectiles and other bodies act with carts as they do today.

## How to reproduce

`./run-game-alpha.sh --level DESERT_MAN --room room9`: get an enemy near the cart track to drop an item on the ground
(or wait for a drop), then let a `CartV2` move toward it. Watch whether the cart pushes the item. Repeat in `room10`.
Also put an item on top of a cart (an enemy dies above it) and check that the item rides with the cart.

## Notes

- Cause not checked yet. The likely cause: the item's body collides with the cart's moving `BLOCK` fixture, and the
  collision handler (`world/collisions/MegaCollisionHandler.kt`) moves the item out of the overlap to the side. The
  fix branch must find the real cause first (read the collision path and prove it with a test).
- A fix must keep the carry behavior, so it cannot simply turn off all collisions between items and carts. A likely
  shape: an item does not resolve side collisions with a moving cart's block fixture, but it still lands on and rides
  the cart from above. The fix branch decides this in its short contract.
- The fix goes in its own branch `fix/<slug>` (see the rules above), with a test for both cases (an item in front of
  the cart stays still; an item on the cart rides with it) and the manual check above.

## Resolution

Fixed by `fix/cart-pushes-dropped-items` (squash `b55f4ee23`). Cause (proved by `CartV2ItemTest`): two paths moved the item.
The collision handler pushed it out to the side while the cart's front edge overlapped it, and once the cart was over
it, `FeetBlockRule` carried it by the cart's position change (`STICK_TO_BLOCK`). Each path alone moved the item. Fix
in `AbstractItem`: when the block is a `CartV2` and the item's bottom is below the cart's vertical center, a block
filter skips the collision and the feet do not stick. An item on top of the cart still rides it. It applies to every
item (also `Coin`, `Screw`, `HealthTank`, `HeartTank`). The owner's play test in `DESERT_MAN` `room9` and `room10`
passes (2026-10-28).
