package com.mega.game.engine.entities.contracts

import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.common.interfaces.UpdateFunction
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.entities.IGameEntity

interface ISpritesEntity : IGameEntity {

    val spritesComponent: SpritesComponent
        get() {
            val key = SpritesComponent::class
            return getComponent(key)!!
        }
    var sprites: OrderedMap<Any, GameSprite>
        get() = this.spritesComponent.sprites
        set(value) {
            this.spritesComponent.sprites = value
        }
    val defaultSprite: GameSprite
        get() = this.sprites[SpritesComponent.DEFAULT_KEY]

    fun putSprite(sprite: GameSprite) = spritesComponent.putSprite(sprite)

    fun putSprite(key: Any, sprite: GameSprite): GameSprite? = spritesComponent.putSprite(key, sprite)

    fun containsSprite(key: Any) = spritesComponent.containsSprite(key)

    fun removeSprite(key: Any): GameSprite? = spritesComponent.removeSprite(key)

    fun putSpritePreProcess(function: UpdateFunction<GameSprite>) = spritesComponent.putPreProcess(function)

    fun putSpritePreProcess(key: Any, updateFunction: UpdateFunction<GameSprite>) =
        spritesComponent.putPreProcess(key, updateFunction)

    fun clearSpritePreProcess() = spritesComponent.clearPreProcess()

    fun removeSpritePreProcess(key: Any) = spritesComponent.removePreProcess(key)

    fun putSpritePostProcess(function: UpdateFunction<GameSprite>) = spritesComponent.putPostProcess(function)

    fun putSpritePostProcess(key: String, updateFunction: UpdateFunction<GameSprite>) =
        spritesComponent.putPostProcess(key, updateFunction)

    fun clearSpritePostProcess() = spritesComponent.clearPostProcess()

    fun removeSpritePostProcess(key: Any) = spritesComponent.removePostProcess(key)
}
