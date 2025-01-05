package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
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
import kotlin.math.ceil

class Smoke(game: MegamanMaverickGame) : MegaGameEntity(game), ISpritesEntity, IAnimatedEntity, ICullableEntity {

    companion object {
        const val TAG = "Smoke"
        private const val SPRITE_WIDTH = 3f
        private const val SPRITE_HEIGHT = 7f
        private const val REGION_COUNT = 4
        private val regions = Array<TextureRegion>()
    }

    private val bounds = GameRectangle()

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.DECORATIONS_1.source)
            for (i in 0 until REGION_COUNT) {
                val key = "$TAG/$i"
                val region = atlas.findRegion(key)
                regions.add(region)
            }

        }
        super.init()
        addComponent(SpritesComponent())
        addComponent(AnimationsComponent())
        addComponent(defineCullablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
        bounds.set(spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!)
        defineDrawables(bounds)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        sprites.clear()
        animators.clear()
    }

    private fun defineDrawables(bounds: GameRectangle) {
        val count = ceil(bounds.getHeight() / (SPRITE_HEIGHT * ConstVals.PPM)).toInt()
        for (i in 0 until count) {
            val key = "$i"

            val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 0))
            sprite.setBounds(
                bounds.getX(),
                bounds.getY() + i * SPRITE_HEIGHT * ConstVals.PPM,
                SPRITE_WIDTH * ConstVals.PPM,
                SPRITE_HEIGHT * ConstVals.PPM
            )
            sprites.put(key, sprite)

            val animation = Animation(regions, 0.1f, true)
            val animator = Animator(animation)
            putAnimator(key, sprite, animator)
        }
    }

    private fun defineCullablesComponent() = CullablesComponent(
        objectMapOf(
            ConstKeys.CULL_OUT_OF_BOUNDS pairTo getGameCameraCullingLogic(game.getGameCamera(), { bounds })
        )
    )

    override fun getTag() = TAG

    override fun getEntityType() = EntityType.DECORATION
}
