package com.megaman.maverick.game.entities.decorations


import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity

class SpaceSatellite(game: MegamanMaverickGame) : MegaGameEntity(game), ISpritesEntity {

    companion object {
        const val TAG = "SpaceSatellite"
        private var region: TextureRegion? = null
    }

    private lateinit var position: Vector2
    private var left = false

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.DECORATIONS_1.source, TAG)
        super.init()
        addComponent(defineSpritesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        position = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
        left = spawnProps.getOrDefault(ConstKeys.LEFT, false, Boolean::class)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(region!!, DrawingPriority(DrawingSection.BACKGROUND, 10))
        sprite.setSize(12.5f * ConstVals.PPM, 6.25f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.setPosition(position, Position.BOTTOM_CENTER)
            sprite.setFlip(left, false)
        }
        return spritesComponent
    }

    override fun getEntityType() = EntityType.DECORATION
}
