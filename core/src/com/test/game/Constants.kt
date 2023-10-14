package com.test.game

import com.badlogic.gdx.math.Vector3

object ConstVals {
  const val VIEW_WIDTH = 16f
  const val VIEW_HEIGHT = 14f
  const val PPM = 32
  const val FIXED_TIME_STEP = 1 / 150f
}

object ConstKeys {
  const val BODY_LABELS = "body_labels"
  const val SPAWNS = "spawns"
  const val ROOM = "room"
  const val BOSS = "boss"
  const val WIDTH = "width"
  const val HEIGHT = "height"
  const val ROWS = "rows"
  const val COLUMNS = "columns"
  const val PRIORITY = "priority"
  const val ATLAS = "atlas"
  const val REGION = "region"
  const val GAME = "game"
  const val UI = "ui"
  const val SYSTEMS = "systems"
  const val UP = "up"
  const val DOWN = "down"
  const val LEFT = "left"
  const val RIGHT = "right"
  const val A = "a"
  const val B = "b"
  const val START = "start"
  const val BACKGROUNDS = "backgrounds"
  const val LEVEL_MUSIC = "level_music"
  const val TMX_SRC = "tmx_src"
  const val POSITION = "position"
  const val CURRENT = "current"
  const val PRIOR = "prior"
  const val WORLD_GRAPH_MAP = "world_graph_map"
  const val MUSIC = "music"
  const val SOUNDS = "sounds"
  const val SPRITES = "sprites"
  const val SHAPES = "shapes"
  const val SPRITE_SHEETS = "sprite_sheets"
  const val PLAYER = "player"
  const val ENEMIES = "enemies"
  const val ITEMS = "items"
  const val BLOCKS = "blocks"
  const val TRIGGERS = "triggers"
  const val FOREGROUNDS = "foregrounds"
  const val BOUNDS = "bounds"
  const val RESIST_ON = "resist_on"
  const val GRAVITY_ON = "gravity_on"
  const val FRICTION_X = "friction_x"
  const val FRICTION_Y = "friction_y"
}

object ConstFuncs {

  fun getCamInitPos(): Vector3 {
    val v = Vector3()
    v.x = ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f
    v.y = ConstVals.VIEW_HEIGHT * ConstVals.PPM / 2f
    return v
  }
}
