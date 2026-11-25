# Claude on the web or mobile

Read this file when the session runs in Claude on the web or the phone (a cloud session that the owner reads in the
chat), not in Claude Code on the owner's laptop. It replaces the steps of the process that need a laptop: the HTML
brief, Diffity, a local play test.

2026-11-25, owner (session of `078h`, from the phone): the brief as an HTML page and the Diffity review work only in
Claude Code on a laptop or PC. On the web and mobile, surface simple docs or artifacts here in the chat, and do the
review on GitHub in a pull request.

How to tell: the session is a cloud session with `add_repo`, no `diffity` skill and no `localhost` browser, and the
owner reads in the Claude app. When in doubt, ask once.

## Rules

1. **Briefs.** Do not write `BRIEF.src.html` and do not use `visual-explainer`. Put the brief in the reply as short
   text with the same parts as in `docs/mega-refactor/CLAUDE.md` ("Brief": goal, before and after, code sketch,
   decisions, risks, play checks). Use a simple Docs artifact only when the brief is too long for the chat or the
   owner should keep or share it; give its link. `SCOPE.md` is committed as always. Write the brief form in the
   status block of the branch `process-log.md` ("brief: in chat" or the link).
2. **Questions.** Ask in the chat with the options and a recommendation (`AskUserQuestion` when it exists, else plain
   text), as in `reports.md`.
3. **Where to look.** A pull request link, an Artifact or Docs link, or an image sent to the chat. Never a
   `localhost` URL.
4. **Code review is a GitHub pull request**, in place of Diffity:
   - Commit all branch work, push the branch (`git -c push.negotiate=true push -u origin <branch>`) and open a pull
     request to the merge target (REST API: `gh api repos/<owner>/<repo>/pulls -f title=... -f head=<branch> -f base=<target> -F body=@file`;
     `gh pr create` fails because GitHub GraphQL is blocked in these sessions; add `-F draft=true` for a draft). The body holds the goal, the contract checks and their result,
     the decisions made without the owner, and a link to `SCOPE.md`.
   - Give the PR link with the list of play checks.
   - The owner comments on the PR. Read the comments with the REST API (`gh api repos/<owner>/<repo>/pulls/<n>/comments`,
     `.../issues/<n>/comments`, `.../pulls/<n>/reviews`; review threads: `GET .../pulls/<n>/ccr/review_threads`), fix them in new commits on the branch (not uncommitted: the PR keeps the history), push, and reply to
     each thread with what was done. Do not resolve a thread that the owner has not answered; the owner resolves
     it.
   - Write each round in the branch process log as for Diffity: the number of threads and what was done.
   - Do not merge the PR unless the owner says so or approves it. The merge rules of `docs/mega-refactor/CLAUDE.md`
     ("Branch merge") still apply, with the PR in place of the Diffity threads: no open review threads, and
     `master` compared before the squash.
5. **No play test in the cloud.** The session has no window. Give the play checks as text. The PR stays open until
   the owner reports that the play test passed or says otherwise.
6. **Automated checks.** The Gradle checks may not run in the cloud: on 2026-11-25 the proxy blocked
   `services.gradle.org`, so the wrapper could not download Gradle. Say so in the PR and in the process log, and
   treat the CI run of the PR (`.github/workflows/ci.yml`, on a push to any branch) as the automated check. Read the
   result with `gh api repos/<owner>/<repo>/commits/<sha>/check-runs`; do not write a polling loop (`git.md`). A check that did not run is listed as not
   run, never as passed.
7. **Repo setup.** `add_repo` with `access: "push"`, then a shallow clone (`git clone --depth 1`) into
   `/home/claude/megaman-maverick`, then `register_repo_root`. The rules of the root `CLAUDE.md` hold: the main
   checkout stays on `master` and each branch gets a worktree in `.worktrees/`. A shallow clone needs a bounded fetch
   (`git fetch --depth=50 origin master`) before a push to `master`, and a rebase of local commits onto it when
   `master` moved. Process docs on `master` are committed and pushed after the owner asked for the change.
8. **Tools that are missing in the cloud.** TexturePacker is not installed, so an atlas is made by a script in
   `temp/` that follows the libGDX atlas format (same region names, `Nearest` filter, 2 px between regions) and the
   `.tps` project is added by hand. Say it in the PR, so that the owner can repack on a laptop with `Basic`. Replacing
   a region with a same-size image in place needs no repack. Do not run art tools that need a display.
9. **Everything else is the same**: `art.md` (art goes to an art agent; the Fable confirmation), `git.md`, `reports.md`
   (3 parts only), `assets.md`, `temp-files.md`.

## TODO

- Claude Code on the laptop may replace Diffity with GitHub pull requests too (owner, 2026-11-25). When the owner
  decides, fold the rules of 4 above into `docs/mega-refactor/CLAUDE.md` ("Diff review" and "Branch merge"), drop the
  Diffity steps and this file's split between the two setups.
