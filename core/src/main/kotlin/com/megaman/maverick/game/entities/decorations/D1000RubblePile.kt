package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponentBuilder
import com.maverick.game.engine.entities.contracts.ISpritesEntity
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.utils.TickFrames

class D1000RubblePile(services: MegaServices) : MegaGameEntity(services), ISpritesEntity {

    companion object {
        const val TAG = "D1000RubblePile"
        private const val REGION = "D1000Rubble/pile"
        private var region: TextureRegion? = null
    }

    val bottomCenter = Vector2()
    private var facingRight = false

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = services.assets.getTextureAtlas(TextureAsset.BOSSES_3.source).findRegion(REGION)
            ?: throw IllegalStateException("Missing region $REGION")
        super.init()
        addComponent(defineSpritesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
        bottomCenter.set(spawnProps.get(ConstKeys.POSITION, Vector2::class)!!)
        facingRight = spawnProps.get(ConstKeys.FACING, Facing::class)!! == Facing.RIGHT
    }

    fun width() = region!!.regionWidth * TickFrames.SCALE

    fun height() = region!!.regionHeight * TickFrames.SCALE

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 2)))
        .preProcess { _, sprite ->
            sprite.setRegion(region!!)
            sprite.setSize(width(), height())
            sprite.setFlip(facingRight, false)
            sprite.setPosition(bottomCenter.x - width() / 2f, bottomCenter.y)
        }
        .build()

    override fun getType() = EntityType.DECORATION
}
