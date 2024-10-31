package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
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
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.DecorationsFactory
import kotlin.math.ceil

class Splash(game: MegamanMaverickGame) : MegaGameEntity(game), ISpritesEntity {

    enum class SplashType { BLUE, WHITE, TOXIC, SAND }

    companion object {
        const val TAG = "Splash"
        private const val BLUE_SPLASH_REGION_KEY = "Water/Splash"
        private const val WHITE_SPLASH_REGION_KEY = "Water/WhiteSplash"
        private const val TOXIC_SPLASH_REGION_KEY = "Water/ToxicSplash"
        private const val SAND_SPLASH_REGION_KEY = "SandSplash"
        private const val DEFAULT_ALPHA = 0.5f
        private const val CULL_TIME = 0.375f
        private var blueSplashRegion: TextureRegion? = null
        private var whiteSplashRegion: TextureRegion? = null
        private var toxicSplashRegion: TextureRegion? = null
        private var sandSplashRegion: TextureRegion? = null

        fun splashOnWaterSurface(splasher: GameRectangle, water: GameRectangle) {
            GameLogger.debug(TAG, "Generating splash for splasher [$splasher] and water [$water]")
            val numSplashes = ceil(splasher.width / ConstVals.PPM).toInt()
            for (i in 0 until numSplashes) {
                val splash = EntityFactories.fetch(EntityType.DECORATION, DecorationsFactory.SPLASH)!!
                val spawn = Vector2(splasher.x + ConstVals.PPM / 2f + i * ConstVals.PPM, water.y + water.height)
                splash.spawn(props(ConstKeys.POSITION pairTo spawn))
            }
        }
    }

    private val cullTimer = Timer(CULL_TIME)
    private lateinit var type: SplashType

    override fun init() {
        if (blueSplashRegion == null ||
            whiteSplashRegion == null ||
            toxicSplashRegion == null ||
            sandSplashRegion == null
        ) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENVIRONS_1.source)
            blueSplashRegion = atlas.findRegion(BLUE_SPLASH_REGION_KEY)
            whiteSplashRegion = atlas.findRegion(WHITE_SPLASH_REGION_KEY)
            toxicSplashRegion = atlas.findRegion(TOXIC_SPLASH_REGION_KEY)
            sandSplashRegion = atlas.findRegion(SAND_SPLASH_REGION_KEY)
        }
        addComponent(defineSpritesCompoent())
        addComponent(defineAnimationsComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        type = if (spawnProps.containsKey(ConstKeys.TYPE)) {
            val rawType = spawnProps.get(ConstKeys.TYPE)
            rawType as? SplashType ?: if (rawType is String) SplashType.valueOf(rawType.uppercase())
            else throw IllegalArgumentException("Type value must be a string or SplashType: $rawType")
        } else SplashType.BLUE

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        firstSprite!!.setPosition(spawn, Position.BOTTOM_CENTER)
        firstSprite!!.setOriginCenter()
        firstSprite!!.rotation = spawnProps.getOrDefault(ConstKeys.ROTATION, 0f, Float::class)
        val priority = spawnProps.getOrDefault(
            ConstKeys.PRIORITY,
            DrawingPriority(DrawingSection.PLAYGROUND, -1),
            DrawingPriority::class
        )
        firstSprite!!.priority.section = priority.section
        firstSprite!!.priority.value = priority.value
        val alpha = spawnProps.getOrDefault(ConstKeys.ALPHA, DEFAULT_ALPHA, Float::class)
        firstSprite!!.setAlpha(alpha)

        cullTimer.reset()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        cullTimer.update(delta)
        if (cullTimer.isFinished()) destroy()
    })

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = { type.name }
        val animations = objectMapOf<String, IAnimation>(
            SplashType.BLUE.name pairTo Animation(blueSplashRegion!!, 1, 5, 0.075f, false),
            SplashType.WHITE.name pairTo Animation(whiteSplashRegion!!, 1, 5, 0.075f, false),
            SplashType.TOXIC.name pairTo Animation(toxicSplashRegion!!, 1, 5, 0.075f, false)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    private fun defineSpritesCompoent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(ConstVals.PPM.toFloat())
        return SpritesComponent(sprite)
    }

    override fun getEntityType() = EntityType.DECORATION
}
