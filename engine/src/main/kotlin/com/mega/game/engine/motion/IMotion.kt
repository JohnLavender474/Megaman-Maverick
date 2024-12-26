package com.mega.game.engine.motion

import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.interfaces.Resettable
import com.mega.game.engine.common.interfaces.Updatable

interface IMotion : Updatable, Resettable {

    fun getMotionValue(out: Vector2): Vector2?
}
