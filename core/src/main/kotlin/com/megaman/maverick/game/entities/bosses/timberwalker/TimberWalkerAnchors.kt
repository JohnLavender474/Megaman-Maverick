package com.megaman.maverick.game.entities.bosses.timberwalker

import com.badlogic.gdx.utils.Array

class PxBox(val x: Int, val y: Int, val w: Int, val h: Int)

class PxPoint(val x: Int, val y: Int)

class TimberWalkerAnchors(
    val hull: PxBox,
    val cab: PxBox?,
    val saw: PxBox?,
    val claw: PxBox?,
    val grip: PxPoint?,
    val footY: Int,
    val rightX: Int
)

class TimberWalkerAnimDef(
    val frames: Array<String>,
    val durations: Array<Float>,
    val mirrored: BooleanArray,
    val loop: Boolean
)
