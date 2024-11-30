package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureAtlas
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

    enum class SplashType(
        val defaultAlpha: Float,
        val defaultSection: DrawingSection,
        val defaultPriority: Int,
        val defaultSize: Float,
        val regionkey: String
    ) {
        BLUE(0.5f, DrawingSection.PLAYGROUND, -1, ConstVals.PPM.toFloat(), "Water/Splash"),
        WHITE(0.5f, DrawingSection.PLAYGROUND, -1, ConstVals.PPM.toFloat(), "Water/WhiteSplash"),
        TOXIC(0.5f, DrawingSection.PLAYGROUND, -1, ConstVals.PPM.toFloat(), "Water/ToxicSplash"),
        SAND(1f, DrawingSection.FOREGROUND, 15, 1.5f * ConstVals.PPM, "SandSplash")
    }

    companion object {
        const val TAG = "Splash"
        private const val CULL_TIME = 0.375f
        private val regions = ObjectMap<String, TextureRegion>()

        fun splashOnWaterSurface(splasher: GameRectangle, water: GameRectangle) {
            GameLogger.debug(TAG, "Generating splash for splasher [$splasher] and water [$water]")
            val numSplashes = ceil(splasher.getWidth() / ConstVals.PPM).toInt()
            for (i in 0 until numSplashes) {
                val splash = EntityFactories.fetch(EntityType.DECORATION, DecorationsFactory.SPLASH)!!
                val spawn =
                    Vector2(splasher.getX() + ConstVals.PPM / 2f + i * ConstVals.PPM, water.getY() + water.getHeight())
                splash.spawn(props(ConstKeys.POSITION pairTo spawn))
            }
        }
    }

    private val cullTimer = Timer(CULL_TIME)
    private lateinit var type: SplashType

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENVIRONS_1.source)
            SplashType.entries.forEach { t ->
                val region = atlas.findRegion(t.regionkey)
                regions.put(t.name, region)
            }
        }
        addComponent(SpritesComponent(GameSprite()))
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
        val size = spawnProps.getOrDefault(ConstKeys.SIZE, type.defaultSize, Float::class)
        val rotation = spawnProps.getOrDefault(ConstKeys.ROTATION, 0f, Float::class)
        val alpha = spawnProps.getOrDefault(ConstKeys.ALPHA, type.defaultAlpha, Float::class)
        val priority = spawnProps.getOrDefault(
            ConstKeys.PRIORITY,
            DrawingPriority(type.defaultSection, type.defaultPriority),
            DrawingPriority::class
        )

        defaultSprite.let { sprite ->
            sprite.setSize(size)
            sprite.setPosition(spawn, Position.BOTTOM_CENTER)

            sprite.setOriginCenter()
            sprite.rotation = rotation

            sprite.setAlpha(alpha)

            sprite.priority.section = priority.section
            sprite.priority.value = priority.value
        }

        cullTimer.reset()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        cullTimer.update(delta)
        if (cullTimer.isFinished()) destroy()
    })

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = { type.name }
        val animations = ObjectMap<String, IAnimation>()
        SplashType.entries.forEach { t -> animations.put(t.name, Animation(regions[t.name], 1, 5, 0.075f, false)) }
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    override fun getEntityType() = EntityType.DECORATION
}
