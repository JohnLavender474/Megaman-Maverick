package com.megaman.maverick.game

import com.badlogic.gdx.math.Vector3

object ConstVals {
    const val VIEW_WIDTH = 16f
    const val VIEW_HEIGHT = 14f
    const val PPM = 32
    const val FIXED_TIME_STEP = 1 / 150f
    const val STANDARD_RESISTANCE_X = 1.035f
    const val STANDARD_RESISTANCE_Y = 1.025f
    const val MAX_HEALTH = 30
    const val BOSS_DROP_DOWN_DURATION = 0.25f
    const val MEGAMAN_MAVERICK_FONT = "Megaman10Font.ttf"
}

object ConstKeys {
    const val CULL = "cull"
    const val PAIR = "pair"
    const val ON = "on"
    const val OFF = "off"
    const val TEXT = "text"
    const val MIN = "min"
    const val MAX = "max"
    const val TOP = "top"
    const val VERTICAL = "vertical"
    const val LENGTH = "length"
    const val DEFAULT = "default"
    const val CART = "cart"
    const val LINE = "line"
    const val PASS_THROUGH = "pass_through"
    const val DELTA = "delta"
    const val SIZE = "size"
    const val DEATH = "death"
    const val IMPULSE = "impulse"
    const val RUN_ON_SPAWN = "run_on_spawn"
    const val ANIMATION = "animation"
    const val DURATION = "duration"
    const val NAME = "name"
    const val KEY = "key"
    const val FACING = "facing"
    const val FORM = "form"
    const val ROWS = "rows"
    const val COLUMNS = "columns"
    const val RESET = "reset"
    const val SUCCESS = "success"
    const val END = "end"
    const val SPLASH = "splash"
    const val BLOCK_ON = "block_on"
    const val ORIENTATION = "orientation"
    const val OFFSET_X = "offset_x"
    const val OFFSET_Y = "offset_y"
    const val WIDTH = "width"
    const val HEIGHT = "height"
    const val CHILD = "child"
    const val PARENT = "parent"
    const val UPSIDE_DOWN = "upside_down"
    const val GRAVITY_ROTATABLE = "gravity_rotatable"
    const val HAZARDS = "hazards"
    const val PPM = "ppm"
    const val GRAVITY = "gravity"
    const val SENSORS = "sensors"
    const val RESPAWNABLE = "respawnable"
    const val X = "x"
    const val Y = "y"
    const val LARGE = "large"
    const val TIMED = "timed"
    const val PENDULUM = "pendulum"
    const val ROTATION = "rotation"
    const val PERSIST = "persist"
    const val VALUE = "value"
    const val DIRECTION = "direction"
    const val MASK = "mask"
    const val DISPOSABLES = "disposables"
    const val EVENTS = "events"
    const val EVENT = "event"
    const val SPAWN_TYPE = "spawn_type"
    const val SPAWNERS = "spawners"
    const val READY = "ready"
    const val TYPE = "type"
    const val ARRAY = "array"
    const val COLLECTION = "collection"
    const val RUNNABLE = "runnable"
    const val ENTITY = "entity"
    const val CONSUMER = "consumer"
    const val OWNER = "owner"
    const val TRAJECTORY = "trajectory"
    const val BOOLEAN = "boolean"
    const val BODY_LABELS = "body_labels"
    const val SPAWNS = "spawns"
    const val ROOM = "room"
    const val BOSS = "boss"
    const val ATLAS = "atlas"
    const val REGION = "region"
    const val GAME = "game"
    const val UI = "ui"
    const val SYSTEMS = "systems"
    const val UP = "up"
    const val DOWN = "down"
    const val LEFT = "left"
    const val RIGHT = "right"
    const val SPECIALS = "specials"
    const val BACKGROUNDS = "backgrounds"
    const val POSITION = "position"
    const val CURRENT = "current"
    const val PRIOR = "prior"
    const val WORLD_GRAPH_MAP = "world_graph_map"
    const val DRAWABLES = "drawables"
    const val SHAPES = "shapes"
    const val PLAYER = "player"
    const val ENEMIES = "enemies"
    const val ITEMS = "items"
    const val BLOCKS = "blocks"
    const val TRIGGERS = "triggers"
    const val FOREGROUNDS = "foregrounds"
    const val GAME_ROOMS = "game_rooms"
    const val BOUNDS = "bounds"
    const val RESIST_ON = "resist_on"
    const val GRAVITY_ON = "gravity_on"
    const val FRICTION_X = "friction_x"
    const val FRICTION_Y = "friction_y"
    const val BOUNCE = "force"
    const val SIDE = "side"
    const val RUNNING = "running"
    const val VELOCITY_ALTERATION = "velocity_alteration"
    const val LADDER = "ladder"
    const val HEALTH = "health"
}

object ConstFuncs {

    fun getCamInitPos(): Vector3 {
        val v = Vector3()
        v.x = ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f
        v.y = ConstVals.VIEW_HEIGHT * ConstVals.PPM / 2f
        return v
    }
}
