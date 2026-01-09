package com.megaman.maverick.game.world.body

import com.mega.game.engine.world.body.IBody
import com.megaman.maverick.game.ConstKeys

enum class BodySense {
    FORCE_APPLIED,
    IN_WATER,
    BODY_TOUCHING_BLOCK,
    FEET_ON_GROUND,
    FEET_ON_ICE,
    FEET_ON_SAND,
    FEET_ON_SNOW,
    HEAD_TOUCHING_BLOCK,
    SIDE_TOUCHING_BLOCK_LEFT,
    SIDE_TOUCHING_BLOCK_RIGHT,
    SIDE_TOUCHING_ICE_LEFT,
    SIDE_TOUCHING_ICE_RIGHT,
    HEAD_TOUCHING_LADDER,
    FEET_TOUCHING_LADDER,
    TOUCHING_CART
}

interface IBodySenseListener {
    fun listenToBodySense(bodySense: BodySense, current: Boolean, old: Boolean?)
}

const val BODY_SENSE_LISTENER_KEY = "${ConstKeys.BODY}_${ConstKeys.SENSE}_${ConstKeys.LISTENER}"

fun IBody.isSensing(bodySense: BodySense) = getProperty(bodySense) == true

fun IBody.isSensingAny(bodySenses: Iterable<BodySense>) = bodySenses.any { isSensing(it) }

fun IBody.isSensingAny(vararg bodySenses: BodySense) = isSensingAny(bodySenses.asIterable())

fun IBody.isSensingAll(vararg bodySenses: BodySense) = isSensingAll(bodySenses.asIterable())

fun IBody.isSensingAll(bodySenses: Iterable<BodySense>) = bodySenses.all { isSensing(it) }

fun IBody.setBodySense(bodySense: BodySense, value: Boolean) {
    val old = putProperty(bodySense, value) as Boolean?
    val listener = getBodySenseListener()
    listener?.listenToBodySense(bodySense, value, old)
}

fun IBody.setBodySenseListener(listener: IBodySenseListener) = putProperty(BODY_SENSE_LISTENER_KEY, listener)

fun IBody.getBodySenseListener(): IBodySenseListener? = getProperty(BODY_SENSE_LISTENER_KEY, IBodySenseListener::class)

fun IBody.resetBodySenses() = BodySense.entries.forEach { putProperty(it, false) }
