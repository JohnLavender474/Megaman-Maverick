# Gameplay iteration

Read this file before you change gameplay that the owner play-tests in rounds (a moveset, a boss, timings, damage).

- **Iterate fast; test when the design is final.** While the owner play-tests and changes a moveset or a feel (a
  boss's attacks, timings, motion), each round is: change the production code and hand it back for
  play. Do not run Gradle (not even `lwjgl3:jar`): the run scripts (`./gradlew lwjgl3:run`) compile the change when
  the owner launches, so a build of your own only adds a wait. Do not write new unit tests, run mutation checks,
  update broken tests or dispatch review agents in these rounds. Small changes are made directly, not through an agent.
  When the owner says the moveset is final, do one pass: tests for the final design, mutation checks, the full
  automated checks and the review before the play test. Meanwhile, keep the spec in plain words: one line per rule of
  behavior that must stay true (and one line per existing test that a round broke), in the branch
  `docs/TEST-DEBT.md`. The final pass turns each line into a test. The merge rules
  do not change: a branch still merges only
  with every check of its contract passed. (Owner, 2026-11-14, branch `077a`: waiting for test rewrites at each
  boss change was too slow.)

- **No JAR build during a play test round.** After the play checks are handed over, a fix runs only the unit tests
  (`engine:test`, `core:test`), and only when the owner says no game runs from the worktree. Do not run `lwjgl3:jar`:
  the owner's next launch compiles the change. The full automated checks run once, when the round closes. (Owner,
  2026-11-14, branch `024a`: the play test already covers the JAR build.)

- **Damage to the player per fight goes in the player damage table.** Set what a fight's damagers do to the player in
  `PlayerDamageNegotiator` (`players/`), one entry per damager tag, not in a value on the damager class. When a damager
  class is shared by several owners (for example `BigAssMaverickRobotOrb`, used by 3 bosses), the entry chooses by its
  owner: `dmgNeg { if ((it as X).owner is SomeBoss) 1 else 3 }`, and the `else` keeps the damage of the other owners.
  Then one place holds the whole damage of a fight. (Owner, 2026-11-14, branch `077a`: every damager of the intro
  boss does 1 bit.)
