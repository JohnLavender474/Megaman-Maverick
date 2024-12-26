package com.mega.game.engine.entities.contracts

import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.IAnimator
import com.mega.game.engine.common.objects.GamePair
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.entities.IGameEntity

interface IAnimatedEntity : IGameEntity {

    val animationsComponent: AnimationsComponent
        get() {
            val key = AnimationsComponent::class
            return getComponent(key)!!
        }
    val animators: OrderedMap<Any, IAnimator>
        get() = animationsComponent.animators

    fun putAnimator(sprite: GameSprite, animator: IAnimator) =
        animationsComponent.putAnimator(sprite, animator)

    fun putAnimator(key: Any, sprite: GameSprite, animator: IAnimator) =
        animationsComponent.putAnimator(key, sprite, animator)

    fun containsAnimator(key: Any) = animationsComponent.containsAnimator(key)

    fun removeAnimator(key: Any, out: GamePair<GameSprite, IAnimator>? = null): GamePair<GameSprite, IAnimator>? =
        animationsComponent.removeAnimator(key, out)

    fun getAnimator(key: Any) = animationsComponent.getAnimator(key)

    fun getAnimatedSprite(key: Any) = animationsComponent.getAnimatedSprite(key)

    fun forEachAnimator(action: (Any, GameSprite, IAnimator) -> Unit) = animationsComponent.forEachAnimator(action)
}
