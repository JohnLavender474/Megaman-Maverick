package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.animations.AnimationsComponentBuilder
import com.maverick.game.engine.animations.AnimatorBuilder
import com.maverick.game.engine.common.extensions.getTextureRegion
import com.maverick.game.engine.common.extensions.objectMapOf
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.cullables.CullablesComponent
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponentBuilder
import com.maverick.game.engine.drawables.sprites.setBounds
import com.maverick.game.engine.entities.contracts.IAnimatedEntity
import com.maverick.game.engine.entities.contracts.ICullableEntity
import com.maverick.game.engine.entities.contracts.ISpritesEntity
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.utils.extensions.getCenter

class Furnace(game: MegaServices) : MegaGameEntity(game), ISpritesEntity, IAnimatedEntity, ICullableEntity {

    companion object {
        const val TAG = "Furnace"
        private const val SIZE = 2f
        private var region: TextureRegion? = null
    }

    private val bounds = GameRectangle().setSize(SIZE * ConstVals.PPM)

    override fun init(vararg params: Any) {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.DECORATIONS_1.source, TAG)
        super.init()
        addComponent(defineCullablesComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        val center = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        bounds.setCenter(center)
    }

    private fun defineCullablesComponent() = CullablesComponent(
        objectMapOf(
            ConstKeys.CULL_OUT_OF_BOUNDS pairTo getGameCameraCullingLogic(game.getGameCamera(), { bounds })
        )
    )

    private fun defineSpritesComponent() =
        SpritesComponentBuilder().sprite(TAG, GameSprite()).preProcess { _, sprite -> sprite.setBounds(bounds) }.build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder().addAnimation(Animation(region!!, 1, 3, 0.1f, true)).build()
        )
        .build()

    override fun getTag() = TAG

    override fun getType() = EntityType.DECORATION
}
