package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
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
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.DecorationsFactory
import kotlin.math.ceil

class Splash(game: MegamanMaverickGame) : MegaGameEntity(game), ISpritesEntity {

    companion object {
        const val TAG = "Splash"
        private const val SPLASH_REGION_KEY = "Water/Splash"
        private const val ALPHA = 0.5f
        private var splashRegion: TextureRegion? = null

        fun generate(splasher: GameRectangle, water: GameRectangle) {
            GameLogger.debug(TAG, "Generating splash for splasher [$splasher] and water [$water]")
            val numSplashes = ceil(splasher.width / ConstVals.PPM).toInt()
            for (i in 0 until numSplashes) {
                val splash = EntityFactories.fetch(EntityType.DECORATION, DecorationsFactory.SPLASH)!!
                val spawn = Vector2(splasher.x + ConstVals.PPM / 2f + i * ConstVals.PPM, water.y + water.height)
                splash.spawn(props(ConstKeys.POSITION pairTo spawn))
            }
        }
    }

    private lateinit var animation: IAnimation

    override fun getEntityType() = EntityType.DECORATION

    override fun init() {
        if (splashRegion == null) splashRegion =
            game.assMan.getTextureRegion(TextureAsset.ENVIRONS_1.source, SPLASH_REGION_KEY)
        addComponent(defineSpritesCompoent())
        addComponent(defineAnimationsComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.POSITION) as Vector2
        firstSprite!!.setPosition(spawn, Position.BOTTOM_CENTER)
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({
        if (animation.isFinished()) destroy()
    })

    private fun defineAnimationsComponent(): AnimationsComponent {
        animation = Animation(splashRegion!!, 1, 5, 0.075f, false)
        return AnimationsComponent(this, Animator(animation))
    }

    private fun defineSpritesCompoent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, -1))
        sprite.setSize(ConstVals.PPM.toFloat())
        sprite.setAlpha(ALPHA)
        return SpritesComponent(sprite)
    }
}
