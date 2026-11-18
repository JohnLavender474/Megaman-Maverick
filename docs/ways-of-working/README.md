# Ways of working

The owner's preferences for the approach to a task, as opposed to how the code looks. One file per topic. Read the
files that match your task before you start it, and give them to each subagent that does such a task. `git.md` applies
to every task.

| File | Read it before you |
|---|---|
| [`git.md`](git.md) | commit, merge, run Gradle or stop a process (every task) |
| [`reports.md`](reports.md) | report work to the owner (every task) |
| [`art.md`](art.md) | draw, fix, convert or credit art: sprites, tiles, backgrounds |
| [`assets.md`](assets.md) | add or change frames, atlases or other asset files |
| [`temp-files.md`](temp-files.md) | write a helper script, a generator or a draft |
| [`gameplay.md`](gameplay.md) | change gameplay that the owner play-tests in rounds |
| [`levels.md`](levels.md) | draft or edit a level, or an entity that reads places from its room |

Related, in other places:

- How Kotlin code looks and is shaped: `docs/CODE_STYLE.md`.
- The process of refactor branches (planning, briefs, reviews, merges): `docs/mega-refactor/CLAUDE.md`.

## Add a rule

Put a new owner preference in the file of its topic, with the date and the branch or session where it came from.
When no file fits, add a new file and a row in the table above, and in the table in the root `CLAUDE.md`. Each file
starts with one line that says when to read it.
