# spawn_type Property Values

Canonical lowercase values:

| Value         | Trigger         | Required companion property          |
|---------------|-----------------|--------------------------------------|
| *(absent)*    | In-camera       | None                                 |
| `spawn_now`   | Level start     | None                                 |
| `spawn_room`  | Room entry      | `spawn_room` = target room name      |
| `spawn_event` | Game event fire | `events` = comma-separated EventType |

`respawnable` (bool, default `true`) controls whether the entity re-spawns after destruction.

NOTE: Some TMX files show the spawn type values in camel-case instead of snake-case — that is a legacy typo
