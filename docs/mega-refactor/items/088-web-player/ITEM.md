# 088: Web player (TeaVM Wasm)

- Status: idea
- Type: feature
- Depends on: `021`, `032`
- Related: `010`, `011`, `033`, `034`, `039`, `054`, `055`
- Branches: none yet

## Goal

Make Megaman Maverick playable in a desktop web browser, from a `teavm` module beside `lwjgl3`. The desktop game stays
the main target and must play the same as before.

This item takes the web half of `054`. A probe on 2026-10-31 (see `NOTES.md`) showed that the code blockers are few:
a patched copy of `master` compiled to Wasm and ran `DESERT_MAN` in headless Chromium. The large open problems are
the size of the assets, and the proof that the web build plays the same as the desktop.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

Backend: `gdx-teavm` with the Wasm-GC target, the same as `mmx-x6`. GWT cannot compile Kotlin (see `NOTES.md`, "Backend
options").

Proposed branches, in order:

- `088a`: the `teavm` module, `WebLauncher`, the page and a CI compile check. It starts after `021` and the `032` work
  that removes the JVM-only code from the path that the web build reaches. The check is the gate: `app.wasm` loads in
  Chromium and Firefox, and a level starts with no error.
- `088b`: a probe and a trace compare for Maverick, as in X6 (`make web-probe`, `make web-trace`). Run scripted input on
  the desktop and on the web, and compare the player and camera values for each update. This needs `011` and probably
  `055`, and the `Math.round` and `Random` differences of TeaVM must be handled.
- `088c`: the web asset set: a tool that writes only the live assets, a loading plan that does not read all the audio
  at startup, and the three live tilesets over 4096 px. It uses `033` if that item is done first.
- `088d`: the web platform parts: keyboard, gamepad, saves in browser storage, the frame rate and the page.

Not in scope: Android (stays in `054`), touch input, public hosting (see "Open questions").

## Owner's input

2026-10-31: "See that `~/IdeaProjects/mmx-x6/` recently gained limited support for gameplay in the web; see
`make web-run` in that repo. I'd like for Megaman Maverick to also support web gameplay at some point. Research what was
done to make web gameplay viable and working in the X6 repo, research what obstacles there are for the Maverick project,
and then let's draft a new `mega-refactor` item."

## Decisions

- 2026-10-31: local first. The first target is a build that runs on localhost. Public hosting is not in scope for now.

## Open questions

Each has a recommendation. None of them is decided.

1. **Public hosting later?** The first target is local (see "Decisions"). Public hosting of ripped Capcom music and
   sprites is more visible than a download, and it would set a download limit for `088c`. Recommendation: decide this
   before `088c` starts.
2. **The full game, or a slice?** X6 shipped the intro only. Maverick has 23 live maps and its startup loads all assets.
   Recommendation: the full game as the target, but one playable level as the `088a` gate.
3. **How exact is "plays the same"?** The X6 trace compare is equal to the bit for each update. Maverick has an unseeded
   random source and a variable frame delta. Recommendation: use the trace compare after `011`, and accept a manual play
   test for what the trace cannot reach.
4. **Which browsers?** X6 blocks Safari and all iOS and iPadOS browsers. Recommendation: the same block at first.
5. **`054`:** split it, so that `054` holds only Android? Recommendation: yes. `054` is unchanged until the owner decides,
   apart from a pointer to this item.

## Sources

- `NOTES.md` in this folder.
- `054` and `032` (`NOTES.md` of `032` has the first list of blockers).
