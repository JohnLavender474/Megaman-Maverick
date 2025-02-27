package com.mega.game.engine.animations

import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.GamePair
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.drawables.sprites.GameSprite

open class Animator(
    val keySupplier: (String?) -> String?,
    val animations: ObjectMap<String, IAnimation>,
    var updateScalar: Float = 1f,
    var onChangeKey: ((String?, String?) -> Unit)? = null,
    var shouldAnimatePredicate: (Float) -> Boolean = { true }
) : IAnimator {

    companion object {
        const val TAG = "Animator"
        const val DEFAULT_KEY = "default_key"
    }

    open val currentAnimation: IAnimation?
        get() = if (currentKey != null) animations[currentKey] else null
    var currentKey: String? = null
        protected set

    constructor(animation: IAnimation) : this({ DEFAULT_KEY }, objectMapOf(DEFAULT_KEY pairTo animation))

    override fun shouldAnimate(delta: Float) = shouldAnimatePredicate.invoke(delta)

    override fun animate(sprite: GameSprite, delta: Float) {
        val nextKey = keySupplier(currentKey)
        if (currentKey != nextKey) {
            onChangeKey?.invoke(currentKey, nextKey)
            currentAnimation?.reset()
        }
        currentKey = nextKey
        currentAnimation?.let {
            it.update(delta * updateScalar)
            it.getCurrentRegion()?.let { region -> sprite.setRegion(region) }
        }
    }

    override fun reset() {
        currentKey = null
        updateScalar = 1f
        animations.values().forEach { it.reset() }
    }
}

class AnimatorBuilder {

    private var keySupplier: ((String?) -> String?) = { Animator.DEFAULT_KEY }
    private val animations: ObjectMap<String, IAnimation> = ObjectMap()
    private var updateScalar: Float = 1f
    private var onChangeKey: ((String?, String?) -> Unit)? = null

    fun setKeySupplier(supplier: (String?) -> String?) = apply {
        this.keySupplier = supplier
    }

    fun applyToAnimations(function: (ObjectMap<String, IAnimation>) -> Unit) = apply {
        function.invoke(animations)
    }

    fun addAnimation(animation: IAnimation) = addAnimation(Animator.DEFAULT_KEY, animation)

    fun addAnimation(key: String, animation: IAnimation) = apply {
        this.animations.put(key, animation)
    }

    fun addAnimations(animationsMap: ObjectMap<String, IAnimation>) = apply {
        animationsMap.forEach { this.animations.put(it.key, it.value) }
    }

    fun addAnimations(vararg animations: GamePair<String, IAnimation>) = apply {
        animations.forEach { this.animations.put(it.first, it.second) }
    }

    fun setUpdateScalar(scalar: Float) = apply {
        this.updateScalar = scalar
    }

    fun setOnChangeKeyListener(listener: (String?, String?) -> Unit) = apply {
        this.onChangeKey = listener
    }

    fun build(): Animator {
        require(animations.size > 0) { "Animator must have at least one animation." }
        return Animator(keySupplier, animations, updateScalar, onChangeKey)
    }
}
