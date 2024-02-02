package com.megaman.maverick.game.world

import com.engine.world.Body

enum class BodySense {
    IN_WATER,
    FEET_ON_GROUND,
    FEET_ON_ICE,
    HEAD_TOUCHING_BLOCK,
    SIDE_TOUCHING_BLOCK_LEFT,
    SIDE_TOUCHING_BLOCK_RIGHT,
    SIDE_TOUCHING_ICE_LEFT,
    SIDE_TOUCHING_ICE_RIGHT,
    HEAD_TOUCHING_LADDER,
    FEET_TOUCHING_LADDER,
    BODY_TOUCHING_CART
}

fun Body.isSensing(bodySense: BodySense) = getProperty(bodySense.toString()) == true

fun Body.isSensingAny(bodySenses: Iterable<BodySense>) = bodySenses.any { isSensing(it) }

fun Body.isSensingAny(vararg bodySenses: BodySense) = isSensingAny(bodySenses.asIterable())

fun Body.isSensingAll(bodySenses: Iterable<BodySense>) = bodySenses.all { isSensing(it) }

fun Body.isSensingAll(vararg bodySenses: BodySense) = isSensingAll(bodySenses.asIterable())

fun Body.setBodySense(bodySense: BodySense, value: Boolean) =
    putProperty(bodySense.toString(), value)

fun Body.setBodySense(vararg bodySenses: BodySense, value: Boolean) =
    bodySenses.forEach { setBodySense(it, value) }

fun Body.setBodySense(vararg bodySenses: Pair<BodySense, Boolean>) =
    bodySenses.forEach { setBodySense(it.first, it.second) }
