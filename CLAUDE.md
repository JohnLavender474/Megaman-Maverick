# Megaman Maverick — Claude Instructions

## Project Memories

Discovered facts about the codebase live in `.claude/memories/`, organized into subdirectories by topic.

- Check `.claude/memories/` **before** searching the codebase or running discovery scripts when
  looking for known facts.
- **Update or create memory files/paths** whenever you discover something non-obvious that you had
  to look up from source code, scripts, or experimentation. If a memory is stale or wrong, fix it.
- Organize memories into subdirectories by topic. For example:
  - Entity-specific info → `.claude/memories/levels/<entity_type>/<entity_name>/`, e.g.
    `.claude/memories/levels/enemies/sniper_joe/SniperJoe.md`
  - Tiled map structure facts (design conventions and patterns) → `.claude/memories/levels/`, e.g.
    `.claude/memories/levels/tiled_map/RoomTransitions.md` for describing transitions between rooms
    in a given level map.
- There is no index file to maintain — use `grep` or `find` to locate relevant memories:
  ```bash
  grep -r "SniperJoe" .claude/memories/
  find .claude/memories/levels/enemies -name "*.md"
  ```
