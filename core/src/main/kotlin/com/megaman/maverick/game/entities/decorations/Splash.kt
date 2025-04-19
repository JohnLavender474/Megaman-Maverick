package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.audio.AudioComponent
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
import com.mega.game.engine.entities.contracts.IAudioEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.utils.GameObjectPools
import kotlin.math.ceil

class Splash(game: MegamanMaverickGame) : MegaGameEntity(game), ISpritesEntity, IAudioEntity {

    enum class SplashType(
        val defaultAlpha: Float,
        val defaultSection: DrawingSection,
        val defaultPriority: Int,
        val defaultSize: Float,
        val regionkey: String
    ) {
        BLUE(0.5f, DrawingSection.PLAYGROUND, -1, 2f * ConstVals.PPM, "Water/Splash_v2"),
        WHITE(0.5f, DrawingSection.PLAYGROUND, -1, 2f * ConstVals.PPM, "Water/WhiteSplash"),
        TOXIC(0.5f, DrawingSection.PLAYGROUND, -1, 2f * ConstVals.PPM, "Water/ToxicSplash"),
        BLUE_RAIN(1f, DrawingSection.PLAYGROUND, -1, ConstVals.PPM.toFloat(), "Water/BlueRainSplash"),
        PURPLE_RAIN(1f, DrawingSection.PLAYGROUND, -1, ConstVals.PPM.toFloat(), "Water/PurpleRainSplash"),
        SAND(1f, DrawingSection.PLAYGROUND, 10, 2f * ConstVals.PPM, "SandSplash")
    }

    companion object {
        const val TAG = "Splash"
        private const val CULL_TIME = 0.375f
        private val regions = ObjectMap<String, TextureRegion>()

        fun splashOnWaterSurface(
            splasher: GameRectangle,
            water: GameRectangle,
            type: SplashType,
            makeSound: Boolean = true
        ) {
            GameLogger.debug(TAG, "splashOnWaterSurface(): splasher=$splasher, water=$water, makeSound=$makeSound")

            val numSplashes = ceil(splasher.getWidth() / ConstVals.PPM).toInt()

            for (i in 0 until numSplashes) {
                val spawn = GameObjectPools.fetch(Vector2::class).set(
                    splasher.getX() + ConstVals.PPM / 2f + i * ConstVals.PPM,
                    water.getY() + water.getHeight()
                )

                val splash = MegaEntityFactory.fetch(Splash::class)!!
                splash.spawn(
                    props(
                        ConstKeys.TYPE pairTo type,
                        ConstKeys.POSITION pairTo spawn,
                        ConstKeys.SOUND pairTo makeSound,
                    )
                )
            }
        }
    }

    private val cullTimer = Timer(CULL_TIME)
    private lateinit var type: SplashType

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENVIRONS_1.source)
            SplashType.entries.forEach { t -> regions.put(t.name, atlas.findRegion(t.regionkey)) }
        }
        addComponent(SpritesComponent(GameSprite()))
        addComponent(defineAnimationsComponent())
        addComponent(defineUpdatablesComponent())
        addComponent(AudioComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        type = when {
            spawnProps.containsKey(ConstKeys.TYPE) -> {
                val rawType = spawnProps.get(ConstKeys.TYPE)
                rawType as? SplashType ?: if (rawType is String) SplashType.valueOf(rawType.uppercase())
                else throw IllegalArgumentException("Type value must be a string or SplashType: $rawType")
            }

            else -> SplashType.BLUE
        }

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        val size = spawnProps.getOrDefault(ConstKeys.SIZE, type.defaultSize, Float::class)
        val rotation = spawnProps.getOrDefault(ConstKeys.ROTATION, 0f, Float::class)
        val alpha = spawnProps.getOrDefault(ConstKeys.ALPHA, type.defaultAlpha, Float::class)
        val priority = spawnProps.getOrDefault(
            ConstKeys.PRIORITY, DrawingPriority(type.defaultSection, type.defaultPriority), DrawingPriority::class
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

        val makeSound = spawnProps.getOrDefault(ConstKeys.SOUND, true, Boolean::class)
        if (makeSound) when (type) {
            SplashType.BLUE, SplashType.WHITE, SplashType.TOXIC ->
                requestToPlaySound(SoundAsset.SPLASH_SOUND, false)

            SplashType.SAND -> requestToPlaySound(SoundAsset.BRUSH_SOUND, false)
            else -> {}
        }
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        cullTimer.update(delta)
        if (cullTimer.isFinished()) destroy()
    })

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = { type.name }
        val animations = ObjectMap<String, IAnimation>()
        SplashType.entries.forEach { t -> animations.put(t.name, Animation(regions[t.name], 1, 5, 0.075f, false)) }
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    override fun getType() = EntityType.DECORATION

    override fun getTag() = TAG
}
