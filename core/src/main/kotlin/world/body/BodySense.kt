package com.megaman.maverick.game.world.body

import com.mega.game.engine.world.body.Body
import com.megaman.maverick.game.ConstKeys

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

interface IBodySenseListener {
    fun listenToBodySense(bodySense: BodySense, current: Boolean, old: Boolean?)
}

const val BODY_SENSE_LISTENER_KEY = "${ConstKeys.BODY}_${ConstKeys.SENSE}_${ConstKeys.LISTENER}"

fun Body.isSensing(bodySense: BodySense) = getProperty(bodySense) == true

fun Body.isSensingAny(bodySenses: Iterable<BodySense>) = bodySenses.any { isSensing(it) }

fun Body.isSensingAny(vararg bodySenses: BodySense) = isSensingAny(bodySenses.asIterable())

fun Body.setBodySense(bodySense: BodySense, value: Boolean) {
    val old = putProperty(bodySense, value) as Boolean?
    val listener = getBodySenseListener()
    listener?.listenToBodySense(bodySense, value, old)
}

fun Body.setBodySenseListener(listener: IBodySenseListener) = putProperty(BODY_SENSE_LISTENER_KEY, listener)

fun Body.getBodySenseListener(): IBodySenseListener? = getProperty(BODY_SENSE_LISTENER_KEY, IBodySenseListener::class)

fun Body.resetBodySenses() = BodySense.entries.forEach { putProperty(it, false) }
