package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic

class Tree(game: MegamanMaverickGame) : MegaGameEntity(game), ISpritesEntity, ICullableEntity {

    companion object {
        const val TAG = "Tree1"
        private var region: TextureRegion? = null
    }

    private lateinit var bounds: GameRectangle

    override fun getEntityType() = EntityType.DECORATION

    override fun init() {
        if (region == null)
            region = game.assMan.getTextureRegion(TextureAsset.ENVIRONS_1.source, "Trees/Tree1")
        addComponent(defineSpritesComponent())
        addComponent(defineCullablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "spawn(): spawnProps = $spawnProps")
        super.onSpawn(spawnProps)
        bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.BACKGROUND, 0))
        sprite.setSize(3f * ConstVals.PPM, 6f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setRegion(region!!)
            _sprite.setPosition(bounds.getPosition())
        }
        return spritesComponent
    }

    private fun defineCullablesComponent(): CullablesComponent {
        val cullOutOfBounds = getGameCameraCullingLogic(game.getGameCamera(), { bounds }, 0f)
        return CullablesComponent(objectMapOf(ConstKeys.CULL_OUT_OF_BOUNDS pairTo cullOutOfBounds))
    }
}