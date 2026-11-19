# Reports to the owner

Read this before every message that reports work to the owner (every task, every `mega-refactor` step).

2026-11-18, owner (session of `078`, Wily Stage 1 art): the owner skims reports. Long result summaries are noise.

A report holds only these 3 parts, and each part only when it has content:

1. **Questions.** Each one with options and a recommendation (`AskUserQuestion`).
2. **Decisions made without the owner.** One line each: the choice and the reason. A failure, a blocked step or a
   skipped step counts as a decision and stays in the report.
3. **Where to look.** The Diffity link, an Artifact link, or the absolute path of an image.

Leave out:

- summaries of the result, counts of checks that passed, lists of files written
- the next steps of the process (the process docs hold them)
- owner decisions said again, and facts from an earlier message said again

Detail that someone can need later goes in the task's notes or temp folder, not in the message.

## Answers to "why" questions

2026-11-18, owner (branch `079a`): a long answer about why code exists was "word vomit".

When the owner asks why code exists, or whether another mechanism covers it, answer with:

1. one summary sentence;
2. the updated logic, in a few short lines;
3. the code.

This also applies to `/read-only` questions. The full reasoning (history, edge cases, evidence) goes in the branch
process log, not in the message.
