package com.megaman.game.events;

import com.badlogic.gdx.utils.ObjectMap;

public enum EventType {

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
    BOSS_SPAWN,
    BOSS_DEAD,

    REQ_SHAKE_CAM;

    private static final ObjectMap<String, EventType> strToEvents = new ObjectMap<>() {{
        put("PlayerReady", PLAYER_READY);
    }};

    public static EventType getEventType(String s) {
        return strToEvents.get(s);
    }

}
