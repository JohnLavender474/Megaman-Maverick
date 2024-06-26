package com.megaman.maverick.game.events

enum class EventType {
    GAME_PAUSE,
    GAME_RESUME,
    GAME_OVER,
    PLAYER_SPAWN,
    PLAYER_JUST_DIED,
    PLAYER_DONE_DYIN,
    PLAYER_READY,
    ADD_PLAYER_HEALTH,
    ADD_HEART_TANK,
    BEGIN_ROOM_TRANS,
    CONTINUE_ROOM_TRANS,
    END_ROOM_TRANS,
    NEXT_ROOM_REQ,
    GATE_INIT_OPENING,
    GATE_FINISH_OPENING,
    GATE_INIT_CLOSING,
    GATE_FINISH_CLOSING,
    ENTER_BOSS_ROOM,
    BEGIN_BOSS_SPAWN,
    END_BOSS_SPAWN,
    BOSS_DEFEATED,
    BOSS_DEAD,
    MINI_BOSS_DEAD,
    VICTORY_EVENT,
    END_LEVEL,
    TELEPORT,
    REQ_SHAKE_CAM,
    REQ_BLACK_BACKGROUND,
    STUN_PLAYER,
    TURN_CONTROLLER_ON,
    TURN_CONTROLLER_OFF
}
