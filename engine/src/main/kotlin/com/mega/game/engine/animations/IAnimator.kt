package com.mega.game.engine.animations

import com.mega.game.engine.common.interfaces.Resettable
import com.mega.game.engine.drawables.sprites.GameSprite

interface IAnimator : Resettable {

    fun shouldAnimate(delta: Float): Boolean

    fun animate(sprite: GameSprite, delta: Float)
}
