package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.ProcessState
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.Pool
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.DecorationsFactory
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getBoundingRectangle
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getPosition
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*
import kotlin.math.abs

class GroundSnow(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity, ICullableEntity {

    companion object {
        const val TAG = "GroundSnow"
        private const val FLUFF_IMPULSE = 2f
        private const val FLUFF_GRAVITY = -0.15f
        private const val FLUFF_OFFSET = 0.1f
        private const val FLUFF_MOVE_X_THRESHOLD = 0.05f
        private const val FLUFF_DEFAULT_CONTINUE_SPAWN_DELAY = 0.5f
        private const val FLUFF_MEGAMAN_GROUNDSLIDE_SPAWN_DELAY = 0.1f
        private val TIMER_POOL = Pool<Timer>(supplier = { Timer() }, startAmount = 3)
        private val FLUFF_OFFSET_SCALARS = gdxArrayOf(-2, -1, 1, 2)
        private val FLUFF_ANGLES = gdxArrayOf(60f, 30f, 330f, 300f)
        private val regions = ObjectMap<String, TextureRegion>()

        fun spawnSnowFluff(x: Float, y: Float) {
            GameLogger.debug(TAG, "spawnSnowFluffAtFeet()")

            for (i in 0 until FLUFF_ANGLES.size) {
                val offsetX = FLUFF_OFFSET_SCALARS[i] * FLUFF_OFFSET * ConstVals.PPM
                val position = GameObjectPools.fetch(Vector2::class).set(x + offsetX, y)

                val impulse = GameObjectPools.fetch(Vector2::class)
                    .set(0f, FLUFF_IMPULSE * ConstVals.PPM)
                    .rotateDeg(FLUFF_ANGLES[i])

                val gravity = FLUFF_GRAVITY * ConstVals.PPM

                val fluff = EntityFactories.fetch(EntityType.DECORATION, DecorationsFactory.SNOW_FLUFF)!!
                fluff.spawn(
                    props(
                        ConstKeys.POSITION pairTo position,
                        ConstKeys.IMPULSE pairTo impulse,
                        ConstKeys.GRAVITY pairTo gravity
                    )
                )

            }
        }
    }

    private val feetTimers = OrderedMap<IGameEntity, Timer>()

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.DECORATIONS_1.source)
            gdxArrayOf("left", "middle", "right", "back").forEach { regions.put(it, atlas.findRegion("${TAG}/$it")) }
        }
        super.init()
        addComponent(defineBodyComponent())
        addComponent(defineCullablesComponent())
        addComponent(SpritesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.set(bounds)

        val count = bounds.getWidth().div(ConstVals.PPM).toInt()
        defineDrawables(bounds.getPosition(), count)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        feetTimers.clear()
        sprites.clear()
    }

    private fun defineDrawables(position: Vector2, count: Int) = (0 until count).forEach { i ->
        val position = position.cpy().add(i * ConstVals.PPM.toFloat(), 0f)

        val key = when (i) {
            0 -> "left"
            count - 1 -> "right"
            else -> "middle"
        }
        val frontSprite = GameSprite(regions[key], DrawingPriority(DrawingSection.FOREGROUND, 5))
        frontSprite.setSize(ConstVals.PPM.toFloat())
        frontSprite.setPosition(position)
        sprites.put("front_$i", frontSprite)

        if (i < count - 1) {
            val backSprite = GameSprite(regions["back"], DrawingPriority(DrawingSection.BACKGROUND, 1))
            backSprite.setSize(ConstVals.PPM.toFloat())
            backSprite.setPosition(position.add(0.5f * ConstVals.PPM, 0f))
            sprites.put("back_$i", backSprite)
        }
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)

        val snowFixture = Fixture(body, FixtureType.SNOW, GameRectangle())
        snowFixture.setHitByFeetReceiver(ProcessState.BEGIN) { feet, _ ->
            val pos = feet.getShape()
                .getBoundingRectangle()
                .getPositionPoint(Position.BOTTOM_CENTER)
            spawnSnowFluff(pos.x, pos.y)

            val timer = TIMER_POOL.fetch()
            timer.resetDuration(FLUFF_DEFAULT_CONTINUE_SPAWN_DELAY)
            feetTimers.put(feet.getEntity(), timer)
        }
        snowFixture.setHitByFeetReceiver(ProcessState.CONTINUE) { feet, delta ->
            val entity = feet.getEntity()

            val timer = feetTimers.get(entity)
            if (timer == null) {
                GameLogger.error(TAG, "hitByFeet(): CONTINUE: no timer for entity=$entity in map=$feetTimers")
                return@setHitByFeetReceiver
            }

            if (entity is Megaman) {
                val groundslide = entity.isBehaviorActive(BehaviorType.GROUND_SLIDING)
                when {
                    groundslide && timer.duration != FLUFF_MEGAMAN_GROUNDSLIDE_SPAWN_DELAY -> {
                        GameLogger.debug(TAG, "hitByFeet(): CONTINUE: reset timer dur to GROUND_SLIDE")
                        timer.resetDuration(FLUFF_MEGAMAN_GROUNDSLIDE_SPAWN_DELAY)
                    }

                    !groundslide && timer.duration != FLUFF_DEFAULT_CONTINUE_SPAWN_DELAY -> {
                        GameLogger.debug(TAG, "hitByFeet(): CONTINUE: reset timer dur to DEFAULT")
                        timer.resetDuration(FLUFF_DEFAULT_CONTINUE_SPAWN_DELAY)
                    }
                }
            }

            val body = feet.getBody()
            if (abs(body.physics.velocity.x) < FLUFF_MOVE_X_THRESHOLD * ConstVals.PPM) return@setHitByFeetReceiver

            val feetBounds = feet.getShape().getBoundingRectangle()

            timer.update(delta)
            if (timer.isFinished()) {
                val x = feetBounds.getCenter().x
                val y = when {
                    entity is Megaman -> body.getCenter().y
                    else -> feetBounds.getCenter().y
                }

                spawnSnowFluff(x, y)

                timer.reset()
            }
        }
        snowFixture.setHitByFeetReceiver(ProcessState.END) { feet, _ ->
            val entity = feet.getEntity()
            val feetBounds = feet.getShape().getBoundingRectangle()

            val x = feetBounds.getCenter().x
            val y = when {
                entity is Megaman -> body.getCenter().y
                else -> feetBounds.getCenter().y
            }

            spawnSnowFluff(x, y)

            val timer = feetTimers.remove(entity)
            if (timer != null) TIMER_POOL.free(timer)
        }
        body.addFixture(snowFixture)

        body.preProcess.put(ConstKeys.DEFAULT) { (snowFixture.rawShape as GameRectangle).set(body) }

        return BodyComponentCreator.create(this, body)
    }

    private fun defineCullablesComponent() = CullablesComponent(
        objectMapOf(ConstKeys.CULL_OUT_OF_BOUNDS pairTo getGameCameraCullingLogic(this))
    )

    override fun getEntityType() = EntityType.SPECIAL
}
