# Notes: mega-refactor-077h-reactor-monkey-art

Per-strip timings, ear points and frame tables: `drafts/enraged/notes.md` and `drafts/intro-death/notes.md`.
Fight ideas: `IDEAS.md`. Art by Claude Fable 5.2 (an AI model by Anthropic), directed by the developer. The original
Reactor Monkey artist is unknown (ask the owner before any credit line).

## Decisions

- Drafts live here (not in `temp/`, which is never committed) so the owner can open them from the pull request.
- Art agents: the whole head is red in the mad face; `ball_mad` is a red orb; no reactor on the body, so the intro flash is visor-only.
- 2026-11-26 (owner): death plays the full sequence (`dying`, `collapse`, `dead`), no alpha fade.
- 2026-11-26 (owner): Hot Potato is in: a charged shot can knock his ball back; if it hits him he reacts (new `ball_hit` and `mad_ball_hit` strips). Design details in `IDEAS.md` move 6.
- 2026-11-26 (owner): round-1 art has oddities: details of the head, torso and waist of the original are missing, and the shoulders are misplaced in some frames. Round 2 rebuilds the strips from the original parts (fresh Fable agent).
- Known: the strict 3 colors per 8x8 rule fails in about a third of areas, as in today's frames (about 31 to 41 percent).
- 2026-11-26 (owner): round 3 (arm fixes, extra frames) and round 4 (a "cool green mohawk"). Current drafts: `drafts/round-4/` (everything for the fight, incl. `stand`/`throw` with the mohawk). Older rounds stay for comparison.
- 2026-11-26 (owner): the body must heave during the chest beat. Current drafts: `drafts/round-5/` (chest_beat with heave; everything else as round 4).
