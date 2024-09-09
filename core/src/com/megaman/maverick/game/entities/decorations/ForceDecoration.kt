package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic

class ForceDecoration(game: MegamanMaverickGame) : MegaGameEntity(game), ISpritesEntity, IAnimatedEntity,
    ICullableEntity {

    companion object {
        const val TAG = "ForceDecoration"
        private var region: TextureRegion? = null
    }

    private lateinit var bounds: GameRectangle
    private var rotation = 0f

    override fun getEntityType() = EntityType.DECORATION

    override fun init() {
        if (region == null)
            region = game.assMan.getTextureRegion(TextureAsset.SPECIALS_1.source, "Force")
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
        addComponent(defineCullablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        rotation = spawnProps.getOrDefault(ConstKeys.ROTATION, 0f, Float::class)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.5f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setCenter(bounds.getCenter())
            _sprite.setOriginCenter()
            _sprite.rotation = rotation
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 1, 4, 0.1f, true)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }

    private fun defineCullablesComponent(): CullablesComponent {
        val cullOutOfBounds = getGameCameraCullingLogic(game.getGameCamera(), { bounds }, 0f)
        return CullablesComponent(objectMapOf(ConstKeys.CULL_OUT_OF_BOUNDS to cullOutOfBounds))
    }
}