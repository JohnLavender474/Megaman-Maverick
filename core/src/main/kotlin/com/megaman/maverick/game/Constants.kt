package com.megaman.maverick.game

import com.badlogic.gdx.math.Vector3

object PreferenceFiles {
    const val MEGAMAN_MAVERICK_SAVE_FILE = "Megaman Maverick - Save File"
    const val MEGAMAN_MAVERICK_CONTROLLER_PREFERENCES = "Megaman Maverick - Controller Preferences"
    const val MEGAMAN_MAVERICK_KEYBOARD_PREFERENCES = "Megaman Maverick - Keyboard Preferences"
}

object ConstVals {
    const val ONE = 1f
    const val VIEW_WIDTH = 16f
    const val VIEW_HEIGHT = 14f
    const val PPM = 32
    const val FPS = 60
    const val FIXED_TIME_STEP = 1 / 150f
    const val STANDARD_RESISTANCE_X = 6f
    const val STANDARD_RESISTANCE_Y = 4f
    const val MAX_HEALTH = 30
    const val MIN_HEALTH = 0
    const val BOSS_DROP_DOWN_DURATION = 0.25f
    const val MEGAMAN_MAVERICK_FONT = "Megaman10Font.ttf"
    const val UI_ARROW_BLINK_DUR = 0.3f
    const val ROOM_TRANS_DELAY_DURATION = 0.35f
    const val ROOM_TRANS_DURATION = 1f
    const val HEALTH_BAR_X = 0.5f
    const val WEAPON_BAR_X = 1f
    const val STATS_BAR_Y = 9f
    const val STANDARD_MAX_STAT_BITS = 30
    const val STAT_BIT_WIDTH = 0.5f
    const val STAT_BIT_HEIGHT = 0.125f
    const val DEFAULT_PATHFINDING_MAX_ITERATIONS = 100
    const val DEFAULT_PATHFINDING_MAX_DISTANCE = 100
    const val DEFAULT_RETURN_BEST_PATH = true
    const val GAME_CAM_ROTATE_TIME = 0.75f
    const val DEFAULT_PARALLAX_X = 0.25f
    const val DEFAULT_PARALLAX_Y = 0f
    const val DUR_PER_BIT = 0.075f
    const val EMPTY_STRING = ""
    const val TEXT_ROW_DECREMENT = 0.025f
    const val ARROW_CENTER_ROW_DECREMENT = 0.25f
    const val MIN_LIVES = 0
    const val MAX_LIVES = 9
    const val START_LIVES = 3
    const val MIN_CURRENCY = 0
    const val MAX_CURRENCY = 999
}

object ConstKeys {
    const val CUSTOM = "custom"
    const val ROCK = "rock"
    const val EXCEPTION = "exception"
    const val FOCUS = "focus"
    const val CAN = "can"
    const val DEACTIVATED = "deactivated"
    const val WHOLE = "whole"
    const val AFTER = "after"
    const val NEST = "nest"
    const val FLAG = "flag"
    const val HARD = "hard"
    const val OWNED = "owned"
    const val GROW = "grow"
    const val TRANS = "trans"
    const val TANK = "tank"
    const val HEART = "heart"
    const val IS = "is"
    const val DUPLICATE = "duplicate"
    const val FACES = "faces"
    const val RED = "red"
    const val STAND_BY = "stand_by"
    const val GRID = "grid"
    const val HAND = "hand"
    const val RESIDUAL = "residual"
    const val CHECKPOINT = "checkpoint"
    const val PINK = "pink"
    const val PIECE = "piece"
    const val PIECES = "pieces"
    const val IGNORE = "ignore"
    const val DARKNESS = "darkness"
    const val FROZEN = "frozen"
    const val MOON = "moon"
    const val FIRE = "fire"
    const val OPTIONS = "options"
    const val EXIT = "exit"
    const val LEVEL = "level"
    const val DEF = "def"
    const val SELECTOR = "selector"
    const val STATIC = "static"
    const val ORIGIN = "origin"
    const val CHARGE = "charge"
    const val HOVER = "hover"
    const val MAIN = "main"
    const val ARM = "arm"
    const val BOUNCE = "bounce"
    const val ATTACHED = "attached"
    const val BLANK = "blank"
    const val BLAST = "blast"
    const val STANDARD = "standard"
    const val SINE = "sine"
    const val HIT_BY_DAMAGEABLE = "hit_by_damageable"
    const val HIT_BY_LASER = "hit_by_laser"
    const val RISEN = "risen"
    const val BURST = "burst"
    const val OWN = "own"
    const val FULL = "full"
    const val HALF = "half"
    const val SET = "set"
    const val COUNT = "count"
    const val FUNCTION = "function"
    const val PLATFORM = "platform"
    const val RING = "ring"
    const val AURA = "aura"
    const val ARRAY = "array"
    const val STATE = "state"
    const val OTHER = "other"
    const val NO = "no"
    const val ID = "id"
    const val WALL = "wall"
    const val A = "a"
    const val B = "b"
    const val SURFACE = "surface"
    const val INIT = "init"
    const val DEFEATED = "defeated"
    const val STAND = "stand"
    const val FADE = "fade"
    const val FADE_OUT_MUSIC = "fade_out_music"
    const val AMPLITUDE = "amplitude"
    const val DRIFT = "drift"
    const val SELECT = "select"
    const val COLLIDE = "collide"
    const val DESTROY = "destroy"
    const val CHILDREN = "children"
    const val SCANNER = "scanner"
    const val THUMP = "thump"
    const val TRIGGERABLE = "triggerable"
    const val BUTTON = "button"
    const val COMMAND = "command"
    const val RUN = "run"
    const val LISTENER = "listener"
    const val BEAM = "beam"
    const val BEAMER = "beamer"
    const val SCREEN = "screen"
    const val SPRITE = "sprite"
    const val SUPPLIER = "supplier"
    const val DAMAGER = "damager"
    const val SHIELD = "shield"
    const val ARROW = "arrow"
    const val PRESSED = "pressed"
    const val DIVISOR = "divisor"
    const val TAKE_FRICTION = "take_friction"
    const val SAND = "sand"
    const val ICE = "ice"
    const val SNOW = "snow"
    const val ORB = "orb"
    const val BULLET = "bullet"
    const val ALPHA = "alpha"
    const val FREQUENCY = "frequency"
    const val BLACK = "black"
    const val CAM = "cam"
    const val SENSE = "sense"
    const val NOT = "not"
    const val TAG = "tag"
    const val FORCE = "force"
    const val RECEIVE = "receive"
    const val CLAMP = "clamp"
    const val WATER = "water"
    const val HIT = "hit"
    const val HIT_WATER = "${HIT}_${WATER}"
    const val POOL = "pool"
    const val BALL = "ball"
    const val WAIT = "wait"
    const val S = "s"
    const val SELECTED = "selected"
    const val NONE = "none"
    const val DISTANCE = "distance"
    const val ITERATIONS = "iterations"
    const val HEURISTIC = "heuristic"
    const val DRAW = "draw"
    const val OUTLINE = "outline"
    const val ALLOW_OUT_OF_BOUNDS = "allow_out_of_bounds"
    const val TILED_MAP_LOAD_RESULT = "tiled_map_load_result"
    const val HIT_BY_BODY = "hit_by_body"
    const val HIT_BY_SIDE = "hit_by_side"
    const val HIT_BY_EXPLOSION = "hit_by_explosion"
    const val FOOT = "foot"
    const val CAN_BE_HIT = "can_be_hit"
    const val HIT_BY_BLOCK = "hit_by_block"
    const val DAMAGEABLE = "damageable"
    const val SPIN = "spin"
    const val STICK_TO_BLOCK = "stick_to_block"
    const val FEET = "feet"
    const val FEET_ON_GROUND = "feet_on_ground"
    const val DEBUG = "debug"
    const val EXPLOSION = "explosion"
    const val FACE = "face"
    const val CONTROLLER = "controller"
    const val SYSTEM = "system"
    const val VELOCITY = "velocity"
    const val ROOM_TRANSITION = "room_transition"
    const val ROOM = "room"
    const val ROOMS = "${ROOM}${S}"
    const val BOSS = "boss"
    const val RETURN = "return"
    const val SPOT = "spot"
    const val FRAME = "frame"
    const val ROLL = "roll"
    const val SHOOT = "shoot"
    const val RISE = "rise"
    const val EDIT = "edit"
    const val FIRST = "first"
    const val AREA = "area"
    const val PRE_PROCESS = "pre_process"
    const val CUSTOM_PRE_PROCESS = "custom_pre_process"
    const val CUSTOM_POST_PROCESS = "custom_post_process"
    const val PROJECTILES = "projectiles"
    const val APPLY_SCALAR_TO_CHILDREN = "apply_scalar_to_children"
    const val BLOCK = "block"
    const val MOVEMENT = "movement"
    const val SCALAR = "scalar"
    const val RETREAT = "retreat"
    const val ATTACK = "attack"
    const val MOVE = "move"
    const val START = "start"
    const val HIT_BY_PROJECTILE = "hit_by_projectile"
    const val HIT_BY_PLAYER = "hit_by_player"
    const val HIT_BY_FEET = "hit_by_feet"
    const val ACTIVE = "active"
    const val BIG = "big"
    const val DELAY = "delay"
    const val FALL = "fall"
    const val ELAPSE = "elapse"
    const val PRIORITY = "priority"
    const val SECTION = "section"
    const val PASSWORD = "password"
    const val LINES = "lines"
    const val CIRCLE = "circle"
    const val POLYGON = "polygon"
    const val MINI = "mini"
    const val INDEX = "index"
    const val JUMP = "jump"
    const val DECORATIONS = "decorations"
    const val FILTER = "filter"
    const val ENTITY_KILLED_BY_DEATH_FIXTURE = "entity_killed_by_death_fixture"
    const val DEATH_LISTENER = "death_listener"
    const val GREEN = "green"
    const val GRAVITY_CHANGEABLE = "gravity_changeable"
    const val INTERVAL = "interval"
    const val MIDDLE = "middle"
    const val SCALE = "scale"
    const val ANGLE = "angle"
    const val HEAD = "head"
    const val ON_DAMAGE_INFLICTED_TO = "on_damage_inflicted_to"
    const val DROP_ITEM_ON_DEATH = "drop_item_on_death"
    const val BLOCK_FILTERS = "block_filters"
    const val CLOSE = "close"
    const val FIXTURE_LABELS = "fixture_labels"
    const val FRONT = "front"
    const val BACK = "back"
    const val FIXTURE = "fixture"
    const val FIXTURES = "fixtures"
    const val BODY = "body"
    const val BODIES = "bodies"
    const val SLOW = "slow"
    const val OBJECT = "object"
    const val MUSIC = "music"
    const val INSTANT = "instant"
    const val POSITION_SUPPLIER = "position_supplier"
    const val CULL_TIME = "cull_time"
    const val ENEMY_SPAWN = "enemy_spawn"
    const val CULL_EVENTS = "cull_events"
    const val CULL_OUT_OF_BOUNDS = "cull_out_of_bounds"
    const val TARGET = "target"
    const val RADIANCE = "radiance"
    const val CENTER = "center"
    const val RADIUS = "radius"
    const val LIGHT = "light"
    const val SOURCE = "source"
    const val KEYS = "keys"
    const val HIDDEN = "hidden"
    const val SOUND = "sound"
    const val TRIGGER = "trigger"
    const val FLIP = "flip"
    const val NEXT = "next"
    const val ON_TELEPORT_START = "on_teleport_start"
    const val ON_TELEPORT_CONTINUE = "on_teleport_continue"
    const val ON_TELEPORT_END = "on_teleport_end"
    const val COLOR = "color"
    const val DRAW_LINE = "draw_line"
    const val CHILD_KEY = "child_key"
    const val SPEED = "speed"
    const val PARALLAX = "parallax"
    const val FOREGROUND = "foreground"
    const val BACKGROUND = "background"
    const val ENTITY_TYPE = "entity_type"
    const val CULL = "cull"
    const val CUSTOM_CULL = "custom_cull"
    const val CULL_TYPE = "cull_type"
    const val CULL_ROOM = "cull_room"
    const val ON = "on"
    const val OFF = "off"
    const val TEXT = "text"
    const val MIN = "min"
    const val MAX = "max"
    const val MAX_Y = "max_y"
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
    const val ANIMATION_KEY = "${ANIMATION}_${KEY}"
    const val FACING = "facing"
    const val ROW = "row"
    const val ROWS = "rows"
    const val COLUMN = "column"
    const val COLUMNS = "columns"
    const val RESET = "reset"
    const val SUCCESS = "success"
    const val END = "end"
    const val SPLASH = "splash"
    const val OFFSET = "offset"
    const val OFFSET_X = "offset_x"
    const val OFFSET_Y = "offset_y"
    const val WIDTH = "width"
    const val HEIGHT = "height"
    const val CHILD = "child"
    const val PARENT = "parent"
    const val GRAVITY = "gravity"
    const val ROTATABLE = "rotatable"
    const val ROTATED = "rotated"
    const val GRAVITY_ROTATABLE = "${GRAVITY}_${ROTATABLE}"
    const val HAZARDS = "hazards"
    const val PPM = "ppm"
    const val SENSORS = "sensors"
    const val SENSOR = "sensor"
    const val RESPAWNABLE = "respawnable"
    const val X = "x"
    const val Y = "y"
    const val LARGE = "large"
    const val TIMED = "timed"
    const val PENDULUM = "pendulum"
    const val ROTATION = "rotation"
    const val LAUNCH = "launch"
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
    const val COLLECTION = "collection"
    const val WHITE = "white"
    const val BLUE = "blue"
    const val RUNNABLE = "runnable"
    const val ENTITY = "entity"
    const val CONSUMER = "consumer"
    const val OWNER = "owner"
    const val TRAJECTORY = "trajectory"
    const val BOOLEAN = "boolean"
    const val BODY_LABELS = "body_labels"
    const val BODY_SENSES = "body_senses"
    const val SPAWNS = "spawns"
    const val SPAWN = "spawn"
    const val SPAWNER = "spawner"
    const val ATLAS = "atlas"
    const val REGION = "region"
    const val GAME = "game"
    const val UI = "ui"
    const val SYSTEMS = "systems"
    const val ONLY = "only"
    const val UP = "up"
    const val DOWN = "down"
    const val LEFT = "left"
    const val RIGHT = "right"
    const val SPECIALS = "specials"
    const val FROM = "from"
    const val BACKGROUNDS = "backgrounds"
    const val POSITION = "position"
    const val PRIOR = "prior"
    const val WORLD_CONTAINER = "world_graph_map"
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
    const val FRICTION = "friction"
    const val FRICTION_X = "${FRICTION}_${X}"
    const val FRICTION_Y = "${FRICTION}_${Y}"
    const val SIDE = "side"
    const val RUNNING = "running"
    const val VELOCITY_ALTERATION = "velocity_alteration"
    const val LADDER = "ladder"
    const val HEALTH = "health"
    const val FILL = "fill"
    const val HEALTH_FILL_TYPE = "${HEALTH}_${FILL}_${TYPE}"
    const val CLEAR_FEET_BLOCKS = "clear_feet_blocks"
    const val FEET_BLOCKS = "feet_blocks"
    const val CONTACT_WATER = "contact_water"
    const val SPRITE_WIDTH = "sprite_width"
    const val SPRITE_HEIGHT = "sprite_height"
}

object ConstFuncs {

    fun getGameCamInitPos(out: Vector3 = Vector3()): Vector3 {
        out.x = ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f
        out.y = ConstVals.VIEW_HEIGHT * ConstVals.PPM / 2f
        return out
    }

    fun getUiCamInitPos(out: Vector3 = Vector3()): Vector3 {
        out.x = ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f
        out.y = ConstVals.VIEW_HEIGHT * ConstVals.PPM / 2f
        return out
    }
}
