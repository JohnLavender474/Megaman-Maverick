package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.putAll
import com.mega.game.engine.common.extensions.toGdxArray
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Loop
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.EnemyDamageNegotiations
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.world.body.*

class StagedMoonLandingFlag(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IFaceable, IDirectional {

    companion object {
        const val TAG = "StagedMoonLandingFlag"

        private const val HIDDEN_WIDTH = 0.5f
        private const val HIDDEN_HEIGHT = 0.25f

        private const val RISE_DUR = 0.4f
        private const val FALL_DUR = 0.4f

        private const val UNFURLED_WIDTH = 0.5f
        private const val UNFURLED_HEIGHT = 2f

        private const val GRAVITY = 0.15f
        private const val GROUND_GRAVITY = 0.01f

        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class FlagState { HIDDEN, RISE, STAND, FALL }

    override val damageNegotiations = EnemyDamageNegotiations.getEnemyDmgNegs(Size.SMALL)
    override var direction: Direction
        get() = body.direction
        set(value) {
            body.direction = value
        }
    override lateinit var facing: Facing

    private val loop = Loop(FlagState.entries.toGdxArray())
    private val currentState: FlagState
        get() = loop.getCurrent()
    private val stateTimers = OrderedMap<FlagState, Timer>()

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            FlagState.entries.forEach {
                val key = it.name.lowercase()
                regions.put(key, atlas.findRegion("$TAG/$key"))
            }
        }

        if (stateTimers.isEmpty) {
            stateTimers.put(FlagState.RISE, Timer(RISE_DUR))
            stateTimers.put(FlagState.FALL, Timer(FALL_DUR))
        }

        super.init()

        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")

        super.onSpawn(spawnProps)

        body.setSize(HIDDEN_WIDTH * ConstVals.PPM, HIDDEN_HEIGHT * ConstVals.PPM)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        val impulse = spawnProps.getOrDefault(ConstKeys.IMPULSE, Vector2.Zero, Vector2::class)
        body.physics.velocity.set(impulse)

        loop.reset()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")

        super.onDestroy()
    }

    override fun onHealthDepleted() {
        if (!hasDepletedHealth()) setToBeDestroyed()
    }

    internal fun setToBeDestroyed() {
        GameLogger.debug(TAG, "setToBeDestroyed()")

        loop.setIndex(FlagState.FALL.ordinal)
    }

    private fun resetBodySizeOnUnfurling() {
        val oldPos = when (direction) {
            Direction.UP -> body.getPositionPoint(Position.BOTTOM_CENTER)
            Direction.DOWN -> body.getPositionPoint(Position.TOP_CENTER)
            Direction.LEFT -> body.getPositionPoint(Position.CENTER_RIGHT)
            Direction.RIGHT -> body.getPositionPoint(Position.CENTER_LEFT)
        }

        val oldBounds = body.getBounds()

        body.setSize(UNFURLED_WIDTH * ConstVals.PPM, UNFURLED_HEIGHT * ConstVals.PPM)

        when (direction) {
            Direction.UP -> body.setBottomCenterToPoint(oldPos)
            Direction.DOWN -> body.setTopCenterToPoint(oldPos)
            Direction.LEFT -> body.getCenterRightPoint(oldPos)
            Direction.RIGHT -> body.setCenterLeftToPoint(oldPos)
        }

        val newBounds = body.getBounds()

        GameLogger.debug(TAG, "resetBodySizeOnUnfurling(): oldBounds=$oldBounds, newBounds=$newBounds")
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            when (currentState) {
                FlagState.HIDDEN -> {
                    if (body.isSensing(BodySense.FEET_ON_GROUND)) {
                        resetBodySizeOnUnfurling()
                        loop.next()
                    }
                }

                FlagState.RISE -> {
                    val timer = stateTimers[currentState]
                    timer.update(delta)

                    if (timer.isFinished()) loop.next()
                }

                FlagState.STAND -> {}

                FlagState.FALL -> {
                    val timer = stateTimers[currentState]
                    timer.update(delta)

                    if (timer.isFinished()) destroy()
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        body.preProcess.put(ConstKeys.GRAVITY) {
            val value = if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAVITY else GRAVITY

            val gravity = body.physics.gravity
            when (direction) {
                Direction.UP -> gravity.set(0f, -value)
                Direction.DOWN -> gravity.set(0f, value)
                Direction.LEFT -> gravity.set(value, 0f)
                Direction.RIGHT -> gravity.set(-value, 0f)
            }.scl(ConstVals.PPM.toFloat())
        }

        body.preProcess.put(ConstKeys.FIXTURES) {
            body.forEachFixture { fixture ->
                if (fixture.getType() == FixtureType.FEET) return@forEachFixture

                val bounds = (fixture as Fixture).rawShape as GameRectangle
                bounds.set(body)
            }
        }
        body.preProcess.put(ConstKeys.X) {
            if (body.isSensing(BodySense.FEET_ON_GROUND)) when (direction) {
                Direction.UP, Direction.DOWN -> body.physics.velocity.x = 0f
                else -> body.physics.velocity.y = 0f
            }
        }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.25f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        feetFixture.attachedToBody = false
        body.addFixture(feetFixture)
        feetFixture.drawingColor = Color.GREEN
        debugShapes.add { feetFixture }

        body.preProcess.put(ConstKeys.FEET) {
            val bounds = feetFixture.rawShape as GameRectangle
            val position = DirectionPositionMapper.getInvertedPosition(direction)
            bounds.positionOnPoint(body.getPositionPoint(position), Position.CENTER)
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this, body, BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGEABLE, FixtureType.DAMAGER)
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { sprite -> sprite.setSize(2f * ConstVals.PPM) })
        .updatable { _, sprite ->
            val rotation = when (direction) {
                Direction.UP, Direction.DOWN -> 0f
                Direction.LEFT -> 90f
                Direction.RIGHT -> 270f
            }
            sprite.setOriginCenter()
            sprite.rotation = rotation

            val position = when (direction) {
                Direction.UP -> Position.BOTTOM_CENTER
                Direction.DOWN -> Position.TOP_CENTER
                Direction.LEFT -> Position.CENTER_RIGHT
                Direction.RIGHT -> Position.CENTER_LEFT
            }
            val bodyPosition = body.getPositionPoint(position)
            sprite.setPosition(bodyPosition, position)
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { currentState.name.lowercase() }
                .applyToAnimations { animations ->
                    animations.putAll(
                        "stand" pairTo Animation(regions["stand"]),
                        "rise" pairTo Animation(regions["rise"], 3, 1, 0.1f, false),
                        "fall" pairTo Animation(regions["fall"], 3, 1, 0.1f, false),
                        "hidden" pairTo Animation(regions["hidden"])
                    )
                }
                .build()
        )
        .build()
}
