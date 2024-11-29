package com.megaman.maverick.game.entities.megaman.sprites

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.megaman.components.*
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper

class MegamanTrailSprite(game: MegamanMaverickGame) : MegaGameEntity(game), ISpritesEntity {

    companion object {
        const val TAG = "MegamanTrailingSprite"
        const val AIR_DASH = "air_dash"
        const val GROUND_SLIDE = "ground_slide"
        const val GROUND_SLIDE_SHOOT = "ground_slide_shoot"
        private const val FADE_DUR = 0.2f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private val fadeTimer = Timer(FADE_DUR)

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.MEGAMAN_TRAIL_SPRITE.source)
            regions.put(AIR_DASH, atlas.findRegion(AIR_DASH))
            regions.put(GROUND_SLIDE, atlas.findRegion(GROUND_SLIDE))
            regions.put(GROUND_SLIDE_SHOOT, atlas.findRegion(GROUND_SLIDE_SHOOT))
        }
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineSpritesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        fadeTimer.reset()

        val type = spawnProps.get(ConstKeys.TYPE, String::class)!!
        defaultSprite.setRegion(regions[type])

        defaultSprite.setFlip(megaman().shouldFlipSpriteX(), megaman().shouldFlipSpriteY())

        defaultSprite.setOriginCenter()
        defaultSprite.rotation = megaman().getSpriteRotation()

        val position = DirectionPositionMapper.getInvertedPosition(megaman().getSpriteDirection())
        defaultSprite.setPosition(megaman().body.getPositionPoint(position), position)

        defaultSprite.translateX(megaman().getSpriteXTranslation() * ConstVals.PPM)
        defaultSprite.translateY(megaman().getSpriteYTranslation() * ConstVals.PPM)
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        fadeTimer.update(delta)
        if (fadeTimer.isFinished()) destroy()
    })

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 0))
        sprite.setSize(2.5f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            val alpha = 1f - fadeTimer.getRatio()
            sprite.setAlpha(alpha)
        }
        return spritesComponent
    }

    override fun getEntityType() = EntityType.DECORATION
}
