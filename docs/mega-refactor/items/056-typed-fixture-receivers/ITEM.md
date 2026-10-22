# 056: Typed fixture receivers

- Status: planned
- Type: refactor
- Depends on: none
- Related: `014`, `029`
- Branches: none yet

## Goal

Replace the hand-written receiver extension trios in `world/body/IFixtureExtensions.kt` (`setX`, `hasX`, `getX` on untyped string keys) with one generic, typed receiver registration. Then a new receiver kind is a key declaration, not three more functions, and a wrong lambda type fails at compile time.

## Proposed scope

From the research in `NOTES.md`. Not approved. The branch `SCOPE.md` decides the final scope.

- Add `ReceiverKey<R>`, `PhasedReceiverKey<R>` (one key per `ProcessState`) and generic `setReceiver`/`receiver` accessors on `IFixture`, stored in the existing fixture `Properties` with object keys. In `core` first (`world/body/FixtureReceivers.kt`). A genre-neutral `TypedKey<T>` can move to the engine later.
- Add a `Receivers` object with one `fun interface` and one key for each existing kind (about 17). Keep each kind's current shape: phased stays phased, stateless stays stateless.
- Turn each old trio into a one-line adapter, so that about 176 of the 183 registration sites compile unchanged. Edit the 7 sites that pass a function-typed `val`.
- Move `setExceptionForNoProjectileCollision` (`FixtureLabel.kt:44-53`) to the same API.
- Delete what nothing uses: `hasConsumer`, and the dead `hitByLaser` kind (see `docs/issues/2026-10-22-unused-contact-hooks.md`).
- Later, optional: inline the adapters at the registration sites, then delete them.
- Out of scope:
  - giving every kind the same phase shape;
  - `hitByPlayer(Megaman)`, which `029` changes;
  - typed keys for body properties and `spawnProps`.

## Order

The research recommends doing this before the `014` rule files, or as the first branch of `014`. Then the new rules call the typed API at once, and about 70 rule call sites are not written twice.

## Owner's input

2026-10-22, while reviewing the `014` sample code (verbatim):

> I've actually been a bit hesitant to want to keep extending what I believe is a code smell: using extensions when instead a more generic receiver registration setup would be better. We can mark that as a TODO and proceed; it's just worth calling out, I think.

## Open questions

- Its own item before `014`, or the first branch of `014` (`014a`)?
- `core` first, or a `TypedKey<T>` in the engine at once (see the same question for the `014` table)?

## Sources

- `NOTES.md` in this folder.
