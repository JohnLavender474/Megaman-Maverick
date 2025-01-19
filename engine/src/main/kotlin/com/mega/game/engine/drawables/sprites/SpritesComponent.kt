package com.mega.game.engine.drawables.sprites

import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.interfaces.UpdateFunction
import com.mega.game.engine.common.objects.GamePair
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.components.IGameComponent

class SpritesComponent(
    val sprites: OrderedMap<Any, GameSprite> = OrderedMap(),
    val updatables: ObjectMap<Any, UpdateFunction<GameSprite>> = ObjectMap()
) :
    IGameComponent, Updatable {

    companion object {
        const val DEFAULT_KEY = "default_key"
    }

    constructor(vararg sprites: GamePair<Any, GameSprite>) : this(OrderedMap<Any, GameSprite>().apply {
        sprites.forEach { put(it.first, it.second) }
    })

    constructor(sprite: GameSprite) : this(DEFAULT_KEY pairTo sprite)

    override fun update(delta: Float) {
        updatables.forEach { e ->
            val name = e.key
            val function = e.value
            sprites[name]?.let { function.update(delta, it) }
        }
    }

    fun putSprite(sprite: GameSprite) = putSprite(DEFAULT_KEY, sprite)

    fun putSprite(key: Any, sprite: GameSprite): GameSprite? = sprites.put(key, sprite)

    fun containsSprite(key: Any) = sprites.containsKey(key)

    fun removeSprite(key: Any): GameSprite? = sprites.remove(key)

    fun putUpdateFunction(function: UpdateFunction<GameSprite>) {
        putUpdateFunction(DEFAULT_KEY, function)
    }

    fun putUpdateFunction(key: Any, function: UpdateFunction<GameSprite>) {
        updatables.put(key, function)
    }

    fun removeUpdateFunction(key: Any) {
        updatables.remove(key)
    }

    fun clearUpdateFunctions() = updatables.clear()
}

class SpritesComponentBuilder {

    private val sprites: OrderedMap<Any, GameSprite> = OrderedMap()
    private val updatables: ObjectMap<Any, UpdateFunction<GameSprite>> = ObjectMap()

    private var currentKey: Any = SpritesComponent.DEFAULT_KEY

    fun sprite(sprite: GameSprite) = sprite(SpritesComponent.DEFAULT_KEY, sprite)

    fun sprite(key: Any, sprite: GameSprite): SpritesComponentBuilder {
        sprites.put(key, sprite)
        currentKey = key
        return this
    }

    fun updatable(updatable: UpdateFunction<GameSprite>): SpritesComponentBuilder {
        updatables.put(currentKey, updatable)
        return this
    }

    fun build() = SpritesComponent(sprites, updatables)
}
