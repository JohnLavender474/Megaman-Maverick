package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setBounds
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.utils.extensions.getPosition

class CrystalConveyorBKG(game: MegamanMaverickGame) : MegaGameEntity(game), ISpritesEntity, IAnimatedEntity {

    companion object {
        const val TAG = "CrystalConveyorBKG"
        private const val WIDTH = 2f
        private const val HEIGHT = 4f
        private var region: TextureRegion? = null
    }

    private val bounds = GameRectangle().setSize(WIDTH * ConstVals.PPM, HEIGHT * ConstVals.PPM)

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.DECORATIONS_1.source, TAG)
        super.init()
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        val position = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPosition()
        bounds.setPosition(position)
    }

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite(DrawingPriority(DrawingSection.BACKGROUND)))
        .updatable { _, sprite -> sprite.setBounds(bounds) }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(AnimatorBuilder().addAnimation(Animation(region!!, 4, 2, 0.1f, true)).build())
        .build()

    override fun getEntityType() = EntityType.DECORATION

    override fun getTag() = TAG
}
