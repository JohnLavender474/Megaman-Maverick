package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.animations.AnimationsComponentBuilder
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.animations.IAnimation
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.extensions.objectMapOf
import com.maverick.game.engine.common.extensions.objectSetOf
import com.maverick.game.engine.common.extensions.swapped
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.cullables.CullablesComponent
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponentBuilder
import com.maverick.game.engine.drawables.sprites.setCenter
import com.maverick.game.engine.drawables.sprites.setSize
import com.maverick.game.engine.entities.contracts.IAnimatedEntity
import com.maverick.game.engine.entities.contracts.ICullableEntity
import com.maverick.game.engine.entities.contracts.ISpritesEntity
import com.maverick.game.engine.motion.SineWave
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.utils.SpriteSkin
import com.megaman.maverick.game.entities.utils.getStandardEventCullingLogic
import com.megaman.maverick.game.entities.utils.onMaxSpawnedByTag
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getRotatedBounds
import kotlin.math.max

class FloatingEmber(services: MegaServices) : MegaGameEntity(services), ISpritesEntity, IAnimatedEntity,
    ICullableEntity {

    companion object {
        const val TAG = "FloatingEmber"

        private const val DEFAULT_CULL_TIME = 1f

        private const val MAX_SPAWNED_ALLOWED = 8

        private const val FADE_DUR = 0.25f

        private const val MIN_FREQUENCY = 0.05f
        private const val MAX_FREQUENCY = 0.15f

        private const val MIN_AMPLITUDE = 0.25f
        private const val MAX_AMPLITUDE = 0.5f

        private const val MIN_RISE_SPEED = 4f
        private const val MAX_RISE_SPEED = 6f

        private val SKINS = objectSetOf(SpriteSkin.DEFAULT, SpriteSkin.LAB)
        private val regions = ObjectMap<SpriteSkin, TextureRegion>()

        private fun regionName(skin: SpriteSkin) = if (skin == SpriteSkin.DEFAULT) TAG else skin.regionName(TAG, TAG)
    }

    private val sinePosition = Vector2()
    private val sine = SineWave(sinePosition, 0f, 0f, 0f)

    private val fadeTimer = Timer(FADE_DUR)
    private var fading = false

    private val outOfBoundsTimer = Timer(DEFAULT_CULL_TIME)

    private val position = Vector2()
    private var maxY = 0f
    private val out = Vector2()
    private var skin = SpriteSkin.DEFAULT

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = services.assets.getTextureAtlas(TextureAsset.DECORATIONS_1.source)
            SKINS.forEach { entry -> regions.put(entry, atlas.findRegion(regionName(entry))) }
        }
        super.init()
        addComponent(defineSpritesComponent())
        addComponent(defineCullablesComponent())
        addComponent(defineAnimationsComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        skin = SpriteSkin.of(TAG, spawnProps, SKINS)

        val center = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        position.set(center)

        val maxY = spawnProps.get(ConstKeys.MAX_Y)
        this.maxY = when (maxY) {
            is Float -> maxY
            is RectangleMapObject -> maxY.rectangle.getCenter().y
            else -> services.gameCamera.getRotatedBounds().getMaxY() + 6f * ConstVals.PPM
        }

        fading = false
        fadeTimer.reset()

        outOfBoundsTimer.reset()

        val speed = services.random.nextFloat(MIN_RISE_SPEED, MAX_RISE_SPEED) * ConstVals.PPM
        val flipX = services.random.nextBoolean()
        val amplitude = services.random.nextFloat(MIN_AMPLITUDE, MAX_AMPLITUDE) * ConstVals.PPM * if (flipX) -1 else 1
        val frequency = services.random.nextFloat(MIN_FREQUENCY, MAX_FREQUENCY) * ConstVals.PPM
        sine.set(GameObjectPools.fetch(Vector2::class).set(center).swapped(), speed, amplitude, frequency)

        onMaxSpawnedByTag(TAG, MAX_SPAWNED_ALLOWED) { set ->
            val iter = set.iterator()
            while (iter.hasNext) {
                val ember = iter.next()
                if (ember is FloatingEmber && !ember.fading) {
                    GameLogger.debug(TAG, "onMaxSpawnedByTag(): set to fading: $ember")
                    ember.fading = true
                    break
                }
            }
        }
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    private fun defineCullablesComponent() = CullablesComponent(
        objectMapOf(
            ConstKeys.CULL_EVENTS pairTo getStandardEventCullingLogic(
                this,
                objectSetOf(EventType.LEVEL_RESET)
            )
        )
    )

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        sine.update(delta)
        sine.getMotionValue(out).swapped().let { position.set(it) }

        if (!services.gameCamera.getRotatedBounds().contains(position)) {
            outOfBoundsTimer.update(delta)
            if (outOfBoundsTimer.isFinished()) fading = true
        } else if (!fading) outOfBoundsTimer.reset()

        if (!fading && position.y > maxY) fading = true

        if (fading) {
            fadeTimer.update(delta)
            if (fadeTimer.isFinished()) destroy()
        }
    })

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(
            TAG, GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 5))
                .also { sprite -> sprite.setSize(0.25f * ConstVals.PPM) }
        )
        .preProcess { _, sprite ->
            sprite.setCenter(position)
            sprite.setAlpha(if (fading) max(0f, 1f - fadeTimer.getRatio()) else 1f)
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG).animator(
            Animator(
                keySupplier = { skin.name },
                animations = ObjectMap<String, IAnimation>().also { animations ->
                    SKINS.forEach { entry -> animations.put(entry.name, Animation(regions[entry], 2, 1, 0.1f, true)) }
                }
            )
        )
        .build()

    override fun getType() = EntityType.DECORATION
}
