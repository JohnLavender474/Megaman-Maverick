package com.megaman.maverick.game.world

import com.engine.world.Body

enum class BodySense {
  IN_WATER,
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

fun Body.isSensing(bodySense: BodySense) = getProperty(bodySense.toString()) == true

fun Body.isSensingAny(bodySenses: Iterable<BodySense>) = bodySenses.any { isSensing(it) }

fun Body.isSensingAny(vararg bodySenses: BodySense) = isSensingAny(bodySenses.asIterable())

fun Body.isSensingAll(bodySenses: Iterable<BodySense>) = bodySenses.all { isSensing(it) }

fun Body.isSensingAll(vararg bodySenses: BodySense) = isSensingAll(bodySenses.asIterable())

fun Body.set(bodySense: BodySense, value: Boolean) = putProperty(bodySense.toString(), value)

fun Body.set(vararg bodySenses: BodySense, value: Boolean) = bodySenses.forEach { set(it, value) }

fun Body.set(vararg bodySenses: Pair<BodySense, Boolean>) =
    bodySenses.forEach { set(it.first, it.second) }
