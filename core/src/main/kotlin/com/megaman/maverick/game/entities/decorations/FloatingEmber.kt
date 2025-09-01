package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.extensions.swapped
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.motion.SineWave
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.utils.DrawableShapesComponentBuilder
import com.megaman.maverick.game.entities.utils.getStandardEventCullingLogic
import com.megaman.maverick.game.entities.utils.onMaxSpawnedByTag
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter
import kotlin.math.max

class FloatingEmber(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity, IAnimatedEntity,
    ICullableEntity {

    companion object {
        const val TAG = "FloatingEmber"

        private const val DEFAULT_CULL_TIME = 1f

        private const val MAX_SPAWNED_ALLOWED = 10

        private const val FADE_DUR = 0.25f

        private const val MIN_FREQUENCY = 0.05f
        private const val MAX_FREQUENCY = 0.15f

        private const val MIN_AMPLITUDE = 0.025f
        private const val MAX_AMPLITUDE = 0.05f

        private const val MIN_RISE_SPEED = 4f
        private const val MAX_RISE_SPEED = 6f

        private var region: TextureRegion? = null
    }

    private lateinit var sine: SineWave

    private val fadeTimer = Timer(FADE_DUR)
    private var fading = false

    private val outOfBoundsTimer = Timer(DEFAULT_CULL_TIME)

    private var maxY = 0f

    private val out = Vector2()

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.DECORATIONS_1.source, TAG)
        super.init()
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineCullablesComponent())
        addComponent(defineAnimationsComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val center = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(center)

        val maxY = spawnProps.get(ConstKeys.MAX_Y)
        this.maxY = when (maxY) {
            is Float -> maxY
            is RectangleMapObject -> maxY.rectangle.getCenter().y
            else -> game.getGameCamera().getRotatedBounds().getMaxY() + 6f * ConstVals.PPM
        }

        fading = false
        fadeTimer.reset()

        outOfBoundsTimer.reset()

        val speed = UtilMethods.getRandom(MIN_RISE_SPEED, MAX_RISE_SPEED) * ConstVals.PPM
        val amplitude = UtilMethods.getRandom(MIN_AMPLITUDE, MAX_AMPLITUDE) * ConstVals.PPM
        val frequency = UtilMethods.getRandom(MIN_FREQUENCY, MAX_FREQUENCY) * ConstVals.PPM
        sine = SineWave(center.cpy().swapped(), speed, amplitude, frequency)

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
                objectSetOf(EventType.PLAYER_SPAWN)
            )
        )
    )

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        sine.update(delta)
        sine.getMotionValue(out).swapped().let { body.setCenter(it) }

        if (!game.getGameCamera().getRotatedBounds().overlaps(body.getBounds())) {
            outOfBoundsTimer.update(delta)
            if (outOfBoundsTimer.isFinished()) fading = true
        } else if (!fading) outOfBoundsTimer.reset()

        if (!fading && body.getY() > maxY) fading = true

        if (fading) {
            fadeTimer.update(delta)
            if (fadeTimer.isFinished()) destroy()
        }
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.125f * ConstVals.PPM)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        addComponent(DrawableShapesComponentBuilder().debug(body, Color.GRAY).build())

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(
            TAG, GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 5))
                .also { sprite -> sprite.setSize(0.25f * ConstVals.PPM) }
        )
        .updatable { _, sprite ->
            sprite.setCenter(body.getCenter())
            sprite.setAlpha(if (fading) max(0f, 1f - fadeTimer.getRatio()) else 1f)
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG).animator(Animator(Animation(region!!, 2, 1, 0.1f, true)))
        .build()

    override fun getType() = EntityType.DECORATION

    override fun getTag() = TAG
}
