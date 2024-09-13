package com.megaman.maverick.game.world.body

import com.mega.game.engine.world.body.Body

enum class BodySense {
    FORCE_APPLIED,
    IN_WATER,
    BODY_TOUCHING_BLOCK,
    FEET_ON_GROUND,
    FEET_ON_ICE,
    FEET_ON_SAND,
    HEAD_TOUCHING_BLOCK,
    SIDE_TOUCHING_BLOCK_LEFT,
    SIDE_TOUCHING_BLOCK_RIGHT,
    SIDE_TOUCHING_ICE_LEFT,
    SIDE_TOUCHING_ICE_RIGHT,
    HEAD_TOUCHING_LADDER,
    FEET_TOUCHING_LADDER,
    TOUCHING_CART,
    TELEPORTING
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

fun Body.resetBodySenses() = BodySense.values().forEach { putProperty(it.toString(), false) }
