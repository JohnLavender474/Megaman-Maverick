package com.mega.game.engine.drawables.sprites

import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.common.interfaces.UpdateFunction
import com.mega.game.engine.common.objects.GamePair
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.components.IGameComponent

class SpritesComponent(
    var sprites: OrderedMap<Any, GameSprite> = OrderedMap(),
    var preProcessFuncs: ObjectMap<Any, UpdateFunction<GameSprite>> = ObjectMap(),
    var postProcessFuncs: ObjectMap<Any, UpdateFunction<GameSprite>> = ObjectMap()
) : IGameComponent {

    companion object {
        const val DEFAULT_KEY = "default_key"
    }

    constructor(vararg sprites: GamePair<Any, GameSprite>) : this(OrderedMap<Any, GameSprite>().apply {
        sprites.forEach { put(it.first, it.second) }
    })

    constructor(sprite: GameSprite) : this(DEFAULT_KEY pairTo sprite)

    fun preProcess(delta: Float) {
        preProcessFuncs.forEach { e ->
            val name = e.key
            val function = e.value
            sprites[name]?.let { function.update(delta, it) }
        }
    }

    fun postProcess(delta: Float) {
        postProcessFuncs.forEach { e ->
            val name = e.key
            val function = e.value
            sprites[name]?.let { function.update(delta, it) }
        }
    }

    fun putSprite(sprite: GameSprite) = putSprite(DEFAULT_KEY, sprite)

    fun putSprite(key: Any, sprite: GameSprite): GameSprite? = sprites.put(key, sprite)

    fun containsSprite(key: Any) = sprites.containsKey(key)

    fun removeSprite(key: Any): GameSprite? = sprites.remove(key)

    fun putPreProcess(function: UpdateFunction<GameSprite>) {
        putPreProcess(DEFAULT_KEY, function)
    }

    fun putPreProcess(key: Any, function: UpdateFunction<GameSprite>) {
        preProcessFuncs.put(key, function)
    }

    fun removePreProcess(key: Any) {
        preProcessFuncs.remove(key)
    }

    fun clearPreProcess() = preProcessFuncs.clear()

    fun putPostProcess(function: UpdateFunction<GameSprite>) {
        putPostProcess(DEFAULT_KEY, function)
    }

    fun putPostProcess(key: Any, function: UpdateFunction<GameSprite>) {
        postProcessFuncs.put(key, function)
    }

    fun removePostProcess(key: Any) {
        postProcessFuncs.remove(key)
    }

    fun clearPostProcess() = postProcessFuncs.clear()
}

class SpritesComponentBuilder {

    private val sprites: OrderedMap<Any, GameSprite> = OrderedMap()
    private val preProcess: ObjectMap<Any, UpdateFunction<GameSprite>> = ObjectMap()
    private val postProcess: ObjectMap<Any, UpdateFunction<GameSprite>> = ObjectMap()

    private var currentKey: Any = SpritesComponent.DEFAULT_KEY

    fun sprite(sprite: GameSprite) = sprite(SpritesComponent.DEFAULT_KEY, sprite)

    fun sprite(key: Any, sprite: GameSprite): SpritesComponentBuilder {
        sprites.put(key, sprite)
        currentKey = key
        return this
    }

    fun preProcess(updatable: UpdateFunction<GameSprite>): SpritesComponentBuilder {
        preProcess.put(currentKey, updatable)
        return this
    }

    fun postProcess(updatable: UpdateFunction<GameSprite>): SpritesComponentBuilder {
        postProcess.put(currentKey, updatable)
        return this
    }

    fun build() = SpritesComponent(sprites, preProcess, postProcess)
}
