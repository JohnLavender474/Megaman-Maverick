package com.test.game.world

import com.engine.world.Body

enum class BodySense {
  HIT_BY_HEAD,
  FEET_ON_GROUND,
  FEET_ON_ICE,
  HEAD_TOUCHING_BLOCK,
  SIDE_TOUCHING_BLOCK_LEFT,
  SIDE_TOUCHING_BLOCK_RIGHT,
  SIDE_TOUCHING_ICE_LEFT,
  SIDE_TOUCHING_ICE_RIGHT,
  HEAD_TOUCHING_LADDER,
  FEET_TOUCHING_LADDER
}

fun Body.bodyIsSensing(bodySense: BodySense) = getProperty(bodySense.toString()) == true

fun Body.setBodySense(bodySense: BodySense, value: Boolean) = putProperty(bodySense.toString(), value)
