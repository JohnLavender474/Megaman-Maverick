package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.ProcessState
import com.mega.game.engine.common.extensions.equalsAny
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.orderedMapOf
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.TimeMarkedRunnable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.IScalableGravityEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.projectiles.GreenPelletBlast
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.world.body.*

class GreenUziJoe(game: MegamanMaverickGame) : AbstractEnemy(game), IScalableGravityEntity, IAnimatedEntity, IFaceable,
    IDirectional {

    companion object {
        const val TAG = "GreenUziJoe"

        private const val BLAST_START_DELAY = 0.125f
        private const val BLAST_DUR = 2.25f
        private const val BLAST_END_DELAY = 0.125f
        private const val BLAST_COUNT = 10
        private const val BLAST_SPEED = 10f

        private const val BLAST_DELAY = 2f

        private const val GRAVITY = 0.15f
        private const val GROUND_GRAVITY = 0.01f

        private const val JUMP_IMPULSE = 10f
        private const val JUMP_SENSOR_WIDTH = 2f
        private const val JUMP_SENSOR_HEIGHT = 6f

        private val animDefs = orderedMapOf(
            "jump" pairTo AnimationDef(),
            "jump_shoot" pairTo AnimationDef(),
            "jump_shoot_start" pairTo AnimationDef(),
            "jump_shoot_blast" pairTo AnimationDef(2, 1, 0.1f, true),
            "stand" pairTo AnimationDef(),
            "stand_shoot" pairTo AnimationDef(),
            "stand_shoot_start" pairTo AnimationDef(),
            "stand_shoot_blast" pairTo AnimationDef(2, 1, 0.1f, true),
        )

        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class GreenUziJoeState { STAND, JUMP }

    override var direction: Direction
        get() = body.direction
        set(value) {
            body.direction = value
        }
    override lateinit var facing: Facing
    override var gravityScalar = 0f

    private lateinit var state: GreenUziJoeState

    private var blasting = false
    private val blastDelay = Timer(BLAST_DELAY)
    private val blastTimer = Timer(BLAST_DUR)
        .setRunOnFirstupdate {
            GameLogger.debug(TAG, "runOnFirstUpdate(): set blasting to false")
            blasting = false
        }
        .setRunnables(
            Array<TimeMarkedRunnable>().also { runnables ->
                val startBlasting = TimeMarkedRunnable(BLAST_START_DELAY) {
                    GameLogger.debug(TAG, "runnable(): set blasting to true")
                    blasting = true
                }
                runnables.add(startBlasting)

                val increment = (BLAST_DUR - BLAST_START_DELAY - BLAST_END_DELAY) / BLAST_COUNT
                for (i in 0 until BLAST_COUNT) {
                    val time = BLAST_START_DELAY + i * increment
                    val blast = TimeMarkedRunnable(time) { this@GreenUziJoe.blast() }
                    runnables.add(blast)
                }

                val endBlasting = TimeMarkedRunnable(BLAST_DUR - BLAST_END_DELAY) {
                    GameLogger.debug(TAG, "runnable(): set blasting to false")
                    blasting = false
                }
                runnables.add(endBlasting)
            }
        )

    private var scaleBlastVelocity = true

    private val shielded: Boolean
        get() = blastTimer.isFinished()

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            animDefs.forEach { entry ->
                val key = entry.key
                regions.put(key, atlas.findRegion("$TAG/$key"))
            }
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")

        super.onSpawn(spawnProps)

        direction =
            Direction.valueOf(spawnProps.getOrDefault(ConstKeys.DIRECTION, ConstKeys.UP, String::class).uppercase())

        val spawn = when {
            spawnProps.containsKey(ConstKeys.BOUNDS) ->
                spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)

            else -> spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        }
        val position = DirectionPositionMapper.getInvertedPosition(direction)
        body.positionOnPoint(spawn, position)

        gravityScalar = spawnProps.getOrDefault("${ConstKeys.GRAVITY}_${ConstKeys.SCALAR}", 1f, Float::class)

        scaleBlastVelocity = spawnProps.getOrDefault("${ConstKeys.SCALE}_${ConstKeys.BLAST}", true, Boolean::class)

        GameLogger.debug(TAG, "onSpawn(): set state to STAND")
        state = GreenUziJoeState.STAND

        updateFacing()

        blasting = false
        blastDelay.reset()
        blastTimer.setToEnd(false)
    }

    private fun blast() {
        GameLogger.debug(TAG, "blast()")

        val spawn = GameObjectPools.fetch(Vector2::class)
        when (direction) {
            Direction.UP -> spawn.set(0.5f * facing.value, -0.1f)
            Direction.DOWN -> spawn.set(0.5f * facing.value, 0.1f)
            Direction.LEFT -> spawn.set(-0.1f, 0.5f * facing.value)
            Direction.RIGHT -> spawn.set(0.1f, -0.5f * facing.value)
        }
        spawn.scl(ConstVals.PPM.toFloat()).add(body.getCenter())

        val trajectory = GameObjectPools.fetch(Vector2::class)
        when (direction) {
            Direction.UP, Direction.DOWN ->
                trajectory.set(BLAST_SPEED * ConstVals.PPM * facing.value, 0f)

            Direction.LEFT ->
                trajectory.set(0f, BLAST_SPEED * ConstVals.PPM * facing.value)

            Direction.RIGHT ->
                trajectory.set(0f, -BLAST_SPEED * ConstVals.PPM * facing.value)
        }

        val blast = MegaEntityFactory.fetch(GreenPelletBlast::class)!!
        blast.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.TRAJECTORY pairTo trajectory,
                ConstKeys.DIRECTION pairTo direction
            )
        )

        requestToPlaySound(SoundAsset.BLAST_2_SOUND, false)
    }

    private fun jump() {
        GameLogger.debug(TAG, "jump()")

        body.physics.velocity.let { velocity ->
            when (direction) {
                Direction.UP -> velocity.set(0f, JUMP_IMPULSE)
                Direction.DOWN -> velocity.set(0f, -JUMP_IMPULSE)
                Direction.LEFT -> velocity.set(-JUMP_IMPULSE, 0f)
                Direction.RIGHT -> velocity.set(JUMP_IMPULSE, 0f)
            }.scl(ConstVals.PPM.toFloat())
        }
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            updateFacing()

            if (blastTimer.isFinished()) {
                blastDelay.update(delta)
                if (blastDelay.isJustFinished()) {
                    GameLogger.debug(TAG, "update(): blast delay just finished, resetting blast timer")
                    blastTimer.reset()
                }
            } else if (blastDelay.isFinished()) {
                blastTimer.update(delta)
                if (blastTimer.isJustFinished()) {
                    GameLogger.debug(TAG, "update(): blast timer just finished, resetting blast delay")
                    blastDelay.reset()
                }
            }

            when (state) {
                GreenUziJoeState.STAND -> if (!body.isSensing(BodySense.FEET_ON_GROUND)) {
                    GameLogger.debug(TAG, "update(): set state to JUMP")
                    state = GreenUziJoeState.JUMP
                }

                GreenUziJoeState.JUMP -> {
                    if (!body.isSensing(BodySense.FEET_ON_GROUND)) return@add

                    val shouldStand = body.physics.velocity.let { velocity ->
                        when (direction) {
                            Direction.UP -> velocity.y <= 0f
                            Direction.DOWN -> velocity.y >= 0f
                            Direction.LEFT -> velocity.x >= 0f
                            Direction.RIGHT -> velocity.x <= 0f
                        }
                    }
                    if (shouldStand) {
                        GameLogger.debug(TAG, "update(): set state to STAND")
                        state = GreenUziJoeState.STAND
                    }
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(ConstVals.PPM.toFloat(), 1.5f * ConstVals.PPM)
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        body.addFixture(feetFixture)
        feetFixture.drawingColor = Color.GREEN
        debugShapes.add { feetFixture }

        val headFixture =
            Fixture(body, FixtureType.HEAD, GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.1f * ConstVals.PPM))
        headFixture.offsetFromBodyAttachment.y = body.getHeight() / 2f
        body.addFixture(headFixture)
        headFixture.drawingColor = Color.ORANGE
        debugShapes.add { headFixture }

        val damageableFixture = Fixture(
            body, FixtureType.DAMAGEABLE, GameRectangle().setSize(0.5f * ConstVals.PPM, 1.5f * ConstVals.PPM)
        )
        body.addFixture(damageableFixture)
        damageableFixture.drawingColor = Color.PURPLE
        debugShapes.add { damageableFixture }

        val shieldFixture = Fixture(
            body, FixtureType.SHIELD, GameRectangle().setSize(0.25f * ConstVals.PPM, 1.25f * ConstVals.PPM)
        )
        body.addFixture(shieldFixture)
        shieldFixture.drawingColor = Color.BLUE
        debugShapes.add { shieldFixture }

        val jumpTriggerFixture = Fixture(
            body,
            FixtureType.CONSUMER,
            GameRectangle().setSize(JUMP_SENSOR_WIDTH * ConstVals.PPM, JUMP_SENSOR_HEIGHT * ConstVals.PPM)
        )
        jumpTriggerFixture.offsetFromBodyAttachment.y =
            (JUMP_SENSOR_HEIGHT * ConstVals.PPM / 2f) - (body.getHeight() / 2f)
        jumpTriggerFixture.setFilter { fixture -> fixture.getType() == FixtureType.PLAYER }
        jumpTriggerFixture.setConsumer { processState, fixture ->
            if (state == GreenUziJoeState.STAND && processState == ProcessState.BEGIN) jump()
        }
        body.addFixture(jumpTriggerFixture)
        jumpTriggerFixture.drawingColor = Color.DARK_GRAY
        debugShapes.add { jumpTriggerFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            if (body.physics.velocity.y > 0f && body.isSensing(BodySense.HEAD_TOUCHING_BLOCK))
                body.physics.velocity.y = 0f

            body.physics.gravity.let { gravity ->
                val value = if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAVITY else GRAVITY
                when (direction) {
                    Direction.UP -> gravity.set(0f, -value)
                    Direction.DOWN -> gravity.set(0f, value)
                    Direction.LEFT -> gravity.set(value, 0f)
                    Direction.RIGHT -> gravity.set(-value, 0f)
                }.scl(ConstVals.PPM.toFloat() * gravityScalar)
            }

            shieldFixture.setActive(shielded)
            shieldFixture.offsetFromBodyAttachment.x =
                0.5f * ConstVals.PPM *
                    if (direction.equalsAny(Direction.UP, Direction.LEFT)) facing.value else -facing.value

            when {
                shielded -> damageableFixture.offsetFromBodyAttachment.x = 0.25f * ConstVals.PPM * when {
                    direction.equalsAny(Direction.UP, Direction.LEFT) -> -facing.value
                    else -> facing.value
                }

                else -> damageableFixture.offsetFromBodyAttachment.x = 0f
            }
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.DAMAGER))
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { sprite -> sprite.setSize(3f * ConstVals.PPM, 2f * ConstVals.PPM) })
        .updatable { _, sprite ->
            sprite.hidden = damageBlink

            sprite.setFlip(isFacing(Facing.RIGHT), direction == Direction.DOWN)

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

            if (direction == Direction.LEFT) sprite.translateX(0.15f * ConstVals.PPM)
            else if (direction == Direction.RIGHT) sprite.translateX(-0.15f * ConstVals.PPM)
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier keySupplier@{
                    var key = state.name.lowercase()

                    if (!blastTimer.isFinished()) key += when {
                        blasting -> "_shoot_blast"
                        blastTimer.time <= BLAST_START_DELAY -> "_shoot_start"
                        else -> "_shoot"
                    }

                    return@keySupplier key
                }
                .applyToAnimations { animations ->
                    animDefs.forEach { entry ->
                        val key = entry.key
                        val def = entry.value
                        val animation = Animation(regions[key], def.rows, def.cols, def.durations, def.loop)
                        animations.put(key, animation)
                    }
                }
                .build()
        )
        .build()

    private fun updateFacing() {
        facing = when (direction) {
            Direction.UP, Direction.DOWN -> if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT
            Direction.LEFT -> if (megaman.body.getY() < body.getY()) Facing.LEFT else Facing.RIGHT
            Direction.RIGHT -> if (megaman.body.getY() < body.getY()) Facing.RIGHT else Facing.LEFT
        }
    }

    override fun getTag() = TAG
}
