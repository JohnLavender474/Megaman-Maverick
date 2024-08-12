package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.common.GameLogger
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setSize
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.entities.contracts.ISpritesEntity
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaGameEntity

open class WindyGrass(game: MegamanMaverickGame) : MegaGameEntity(game), ISpritesEntity, IAnimatedEntity {

    companion object {
        const val TAG = "AnimatedTile"
        private var leftRegion: TextureRegion? = null
        private var rightRegion: TextureRegion? = null
        private var middleRegion: TextureRegion? = null
    }

    private lateinit var bounds: GameRectangle

    override fun init() {
        if (leftRegion == null || rightRegion == null || middleRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENVIRONS_1.source)
            leftRegion = atlas.findRegion("WindyGrass/Left")
            rightRegion = atlas.findRegion("WindyGrass/Right")
            middleRegion = atlas.findRegion("WindyGrass/Middle")
        }
        addComponent(SpritesComponent())
        addComponent(AnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "spawn(): spawnProps = $spawnProps")
        super.spawn(spawnProps)
        bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        val tiles = bounds.width.toInt() / ConstVals.PPM
        for (i in 0 until tiles) {
            val positionX = bounds.x + (i * ConstVals.PPM)
            val positionY = bounds.y
            val grassSprite = GameSprite(DrawingPriority(DrawingSection.BACKGROUND, 1))
            grassSprite.setSize(ConstVals.PPM.toFloat())
            grassSprite.setPosition(positionX, positionY)
            sprites.put("grass_$i", grassSprite)
            val region = if (i == 0) leftRegion else if (i == tiles - 1) rightRegion else middleRegion
            val animation = Animation(region!!, 1, 2, 0.2f, true)
            val animator = Animator(animation)
            animators.add({ grassSprite } to animator)
        }
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super<MegaGameEntity>.onDestroy()
        sprites.clear()
        animators.clear()
    }
}