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

open class WindyGrass(game: MegamanMaverickGame) : MegaGameEntity(game), ISpritesEntity, IAnimatedEntity,
    ICullableEntity {

    companion object {
        const val TAG = "WindyGrass"
        private var leftRegion: TextureRegion? = null
        private var rightRegion: TextureRegion? = null
        private var middleRegion: TextureRegion? = null
    }

    private lateinit var bounds: GameRectangle
    private val animationsToRemove = Array<String>()

    override fun getType() = EntityType.DECORATION

    override fun init() {
        if (leftRegion == null || rightRegion == null || middleRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENVIRONS_1.source)
            leftRegion = atlas.findRegion("$TAG/Left")
            rightRegion = atlas.findRegion("$TAG/Right")
            middleRegion = atlas.findRegion("$TAG/Middle")
        }
        addComponent(SpritesComponent())
        addComponent(AnimationsComponent())
        addComponent(defineCullablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "spawn(): spawnProps = $spawnProps")
        super.onSpawn(spawnProps)

        bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!

        val tiles = bounds.getWidth().toInt() / ConstVals.PPM
        for (i in 0 until tiles) {
            val key = "grass_$i"

            val positionX = bounds.getX() + (i * ConstVals.PPM)
            val positionY = bounds.getY()

            val grassSprite = GameSprite(DrawingPriority(DrawingSection.BACKGROUND, 1))
            grassSprite.setSize(ConstVals.PPM.toFloat())
            grassSprite.setPosition(positionX, positionY)
            sprites.put(key, grassSprite)

            val region = if (i == 0) leftRegion else if (i == tiles - 1) rightRegion else middleRegion
            val animation = Animation(region!!, 1, 2, 0.2f, true)
            val animator = Animator(animation)

            putAnimator(key, grassSprite, animator)

            animationsToRemove.add(key)
        }
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        sprites.clear()
        animationsToRemove.forEach { animationsComponent.removeAnimator(it) }
    }

    private fun defineCullablesComponent(): CullablesComponent {
        val cullOutOfBounds = getGameCameraCullingLogic(game.getGameCamera(), { bounds }, 0f)
        return CullablesComponent(objectMapOf(ConstKeys.CULL_OUT_OF_BOUNDS pairTo cullOutOfBounds))
    }
}
