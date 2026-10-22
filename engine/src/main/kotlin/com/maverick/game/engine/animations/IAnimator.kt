package com.maverick.game.engine.animations

import com.maverick.game.engine.common.interfaces.Resettable
import com.maverick.game.engine.drawables.sprites.GameSprite

interface IAnimator : Resettable {

    fun shouldAnimate(delta: Float): Boolean

    fun animate(sprite: GameSprite, delta: Float)
}
