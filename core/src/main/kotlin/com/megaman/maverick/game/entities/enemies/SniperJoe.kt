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
import com.mega.game.engine.common.extensions.*
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
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.state.EnumStateMachineBuilder
import com.mega.game.engine.state.StateMachine
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
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.MagmaGoop
import com.megaman.maverick.game.entities.projectiles.Snowball
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.utils.misc.FacingUtils
import com.megaman.maverick.game.utils.misc.GravityUtils
import com.megaman.maverick.game.utils.misc.HeadUtils
import com.megaman.maverick.game.world.body.*

class SniperJoe(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IScalableGravityEntity,
    IDirectional, IFaceable {

    companion object {
        const val TAG = "SniperJoe"

        private const val BULLET_SPEED = 10f

        private const val SNOWBALL_X = 8f
        private const val SNOWBALL_Y = 5f
        private const val SNOWBALL_GRAV = 0.15f

        private const val FIREBALL_X = 10f

        private const val JUMP_IMPULSE = 15f

        private const val SHIELD_OFFSET = 0.675f

        private const val IDLE_DUR = 2f
        private const val SHOOT_DUR = 2f
        private const val TURN_DUR = 0.5f

        private const val GROUND_GRAVITY = 0.001f
        private const val GRAVITY = 0.375f

        private val TIMES_TO_SHOOT = floatArrayOf(0.5f, 1f, 1.5f)

        private val animDefs = orderedMapOf<String, AnimationDef>(
            "idle" pairTo AnimationDef(2, 1, gdxArrayOf(1f, 0.15f), true),
            "shoot" pairTo AnimationDef(3, 1, 0.1f, false),
            "turn" pairTo AnimationDef(2, 1, 0.1f, false),
            "jump" pairTo AnimationDef(),
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class SniperJoeType { ORANGE, SNOW, FIRE }

    private enum class SniperJoeState { IDLE, TURN, SHOOT, JUMP }

    override var direction: Direction
        get() = body.direction
        set(value) {
            body.direction = value
        }
    override lateinit var facing: Facing
    override var gravityScalar = 1f

    private lateinit var stateMachine: StateMachine<SniperJoeState>
    private val currentState: SniperJoeState
        get() = stateMachine.getCurrent()
    private val stateTimers = orderedMapOf(
        SniperJoeState.IDLE pairTo Timer(IDLE_DUR),
        SniperJoeState.TURN pairTo Timer(TURN_DUR),
        SniperJoeState.SHOOT pairTo Timer(SHOOT_DUR).also { timer ->
            TIMES_TO_SHOOT.forEach { time -> timer.addRunnable(TimeMarkedRunnable(time) { shoot() }) }
        }
    )

    private lateinit var type: SniperJoeType

    private var scaleBullet = true

    private val shouldUpdate: Boolean
        get() = !game.isCameraRotating()
    private val shielded: Boolean
        get() = currentState != SniperJoeState.SHOOT

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            SniperJoeType.entries.forEach { type ->
                animDefs.keys().forEach { key ->
                    val fullKey = "${type.name.lowercase()}/${key}"
                    regions.put(fullKey, atlas.findRegion("$TAG/$fullKey"))
                }
            }
        }
        super.init()
        addComponent(defineAnimationsComponent())
        stateMachine = buildStateMachine()
    }

    override fun onSpawn(spawnProps: Properties) {
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

        type = SniperJoeType.valueOf(
            spawnProps.getOrDefault(ConstKeys.TYPE, SniperJoeType.ORANGE.name, String::class).uppercase()
        )

        gravityScalar = spawnProps.getOrDefault("${ConstKeys.GRAVITY}_${ConstKeys.SCALAR}", 1f, Float::class)
        scaleBullet = spawnProps.getOrDefault("${ConstKeys.SCALE}_${ConstKeys.BULLET}", true, Boolean::class)

        stateMachine.reset()
        stateTimers.values().forEach { it.reset() }

        FacingUtils.setFacingOf(this)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (!shouldUpdate) return@add

            val timer = stateTimers[currentState]
            if (timer != null) {
                timer.update(delta)

                if (timer.isFinished()) stateMachine.next()
            }

            if ((currentState == SniperJoeState.JUMP && shouldEndJumping()) ||
                (!currentState.equalsAny(SniperJoeState.JUMP, SniperJoeState.TURN) &&
                    (shouldStartTurning() || shouldStartJumping()))
            ) stateMachine.next()
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(ConstVals.PPM.toFloat(), 1.5f * ConstVals.PPM)
        body.drawingColor = Color.GRAY

        val shapes = Array<() -> IDrawableShape?>()
        shapes.add { body.getBounds() }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.5f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        body.addFixture(feetFixture)
        shapes.add { feetFixture }

        val headFixture =
            Fixture(body, FixtureType.HEAD, GameRectangle().setSize(0.5f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        headFixture.offsetFromBodyAttachment.y = body.getHeight() / 2f
        body.addFixture(headFixture)
        shapes.add { headFixture }

        val shieldFixture =
            Fixture(body, FixtureType.SHIELD, GameRectangle().setSize(0.25f * ConstVals.PPM, 1.25f * ConstVals.PPM))
        body.addFixture(shieldFixture)
        shapes.add { shieldFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            when (direction) {
                Direction.UP, Direction.DOWN -> body.physics.velocity.x = 0f
                else -> body.physics.velocity.y = 0f
            }

            val gravity = (if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAVITY else GRAVITY)
                .times(ConstVals.PPM * gravityScalar)
            GravityUtils.setGravity(body, gravity)

            shieldFixture.setActive(shielded)
            shieldFixture.offsetFromBodyAttachment.x = SHIELD_OFFSET * ConstVals.PPM * when (direction) {
                Direction.UP, Direction.LEFT -> facing.value
                else -> -facing.value
            }

            if (body.isSensing(BodySense.HEAD_TOUCHING_BLOCK)) HeadUtils.stopJumpingIfHitHead(body)
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = shapes, debug = true))

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { sprite -> sprite.setSize(2f * ConstVals.PPM) })
        .updatable { _, sprite ->
            sprite.hidden = damageBlink

            sprite.setFlip(isFacing(Facing.LEFT), false)

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

            when (direction) {
                Direction.LEFT -> sprite.translateX(0.15f * ConstVals.PPM)
                Direction.RIGHT -> sprite.translateX(-0.15f * ConstVals.PPM)
                else -> {}
            }
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier keySupplier@{
                    val key = "${type.name.lowercase()}/${currentState.name.lowercase()}"
                    return@keySupplier key
                }
                .applyToAnimations { animations ->
                    SniperJoeType.entries.forEach { type ->
                        animDefs.forEach { entry ->
                            val key = entry.key
                            val (rows, columns, durations, loop) = entry.value
                            val fullKey = "${type.name.lowercase()}/$key"
                            try {
                                animations.put(fullKey, Animation(regions[fullKey], rows, columns, durations, loop))
                            } catch (e: Exception) {
                                throw Exception(
                                    "Failed to put animation: fullKey=$fullKey, regions=${regions.keys().toGdxArray()}",
                                    e
                                )
                            }
                        }
                    }
                }
                .build()
        )
        .build()

    private fun buildStateMachine() = EnumStateMachineBuilder
        .create<SniperJoeState>()
        .initialState(SniperJoeState.IDLE)
        .setOnChangeState(this::onChangeState)
        // idle
        .transition(SniperJoeState.IDLE, SniperJoeState.JUMP) { shouldStartJumping() }
        .transition(SniperJoeState.IDLE, SniperJoeState.TURN) { shouldStartTurning() }
        .transition(SniperJoeState.IDLE, SniperJoeState.SHOOT) { true }
        // turn
        .transition(SniperJoeState.TURN, SniperJoeState.IDLE) { true }
        // jump
        .transition(SniperJoeState.JUMP, SniperJoeState.IDLE) { shouldEndJumping() }
        // shoot
        .transition(SniperJoeState.SHOOT, SniperJoeState.JUMP) { shouldStartJumping() }
        .transition(SniperJoeState.SHOOT, SniperJoeState.TURN) { shouldStartTurning() }
        .transition(SniperJoeState.SHOOT, SniperJoeState.IDLE) { true }
        // build
        .build()

    private fun onChangeState(current: SniperJoeState, previous: SniperJoeState) {
        GameLogger.debug(TAG, "onChangeState(): current=$current, previous=$previous")

        val timer = stateTimers[previous]
        timer?.reset()

        if (previous == SniperJoeState.TURN) FacingUtils.setFacingOf(this)

        if (current == SniperJoeState.JUMP) jump()
    }

    private fun shouldStartTurning() =
        currentState == SniperJoeState.IDLE && facing != FacingUtils.getPreferredFacingFor(this)

    private fun shouldStartJumping() =
        body.isSensing(BodySense.FEET_ON_GROUND) &&
            body.physics.velocity.y <= 0f &&
            when (direction) {
                Direction.UP -> megaman.body.getY() > body.getMaxY() &&
                    megaman.body.getX() <= body.getMaxX() &&
                    megaman.body.getMaxX() >= body.getX()

                Direction.DOWN -> megaman.body.getMaxY() < body.getY() &&
                    megaman.body.getX() <= body.getMaxX() &&
                    megaman.body.getMaxX() >= body.getX()

                Direction.LEFT -> megaman.body.getMaxX() < body.getX() &&
                    megaman.body.getY() <= body.getMaxY() &&
                    megaman.body.getMaxY() >= body.getY()

                Direction.RIGHT -> megaman.body.getX() > body.getMaxX() &&
                    megaman.body.getY() <= body.getMaxY() &&
                    megaman.body.getMaxY() >= body.getY()
            }

    private fun jump() {
        val impulse = GameObjectPools.fetch(Vector2::class)

        when (direction) {
            Direction.UP -> impulse.set(0f, JUMP_IMPULSE)
            Direction.DOWN -> impulse.set(0f, -JUMP_IMPULSE)
            Direction.LEFT -> impulse.set(-JUMP_IMPULSE, 0f)
            Direction.RIGHT -> impulse.set(JUMP_IMPULSE, 0f)
        }

        body.physics.velocity.set(impulse).scl(ConstVals.PPM.toFloat())
    }

    private fun shouldEndJumping() = body.isSensing(BodySense.FEET_ON_GROUND) && body.physics.velocity.y <= 0f

    private fun shoot() {
        val spawn = GameObjectPools.fetch(Vector2::class)
        when (direction) {
            Direction.UP -> spawn.set(0.75f * facing.value, -0.15f)
            Direction.DOWN -> spawn.set(0.75f * facing.value, 0.15f)
            Direction.LEFT -> spawn.set(0.15f, 0.75f * facing.value)
            Direction.RIGHT -> spawn.set(-0.15f, -0.75f * facing.value)
        }
        spawn.scl(ConstVals.PPM.toFloat()).add(body.getCenter())

        val trajectory = GameObjectPools.fetch(Vector2::class)

        val props = props(
            ConstKeys.OWNER pairTo this,
            ConstKeys.POSITION pairTo spawn,
            ConstKeys.TRAJECTORY pairTo trajectory,
            ConstKeys.DIRECTION pairTo direction
        )

        when (type) {
            SniperJoeType.ORANGE -> {
                when (direction) {
                    Direction.UP, Direction.DOWN ->
                        trajectory.set(BULLET_SPEED * ConstVals.PPM * facing.value, 0f)

                    Direction.LEFT ->
                        trajectory.set(0f, BULLET_SPEED * ConstVals.PPM * facing.value)

                    Direction.RIGHT ->
                        trajectory.set(0f, -BULLET_SPEED * ConstVals.PPM * facing.value)
                }
                if (scaleBullet) trajectory.scl(gravityScalar)

                val bullet = MegaEntityFactory.fetch(Bullet::class)!!
                bullet.spawn(props)

                if (overlapsGameCamera()) requestToPlaySound(SoundAsset.ENEMY_BULLET_SOUND, false)
            }

            SniperJoeType.SNOW -> {
                trajectory.x = SNOWBALL_X * ConstVals.PPM * facing.value
                trajectory.y = SNOWBALL_Y * ConstVals.PPM

                val gravity = GameObjectPools.fetch(Vector2::class).set(0f, -SNOWBALL_GRAV * ConstVals.PPM)
                props.put(ConstKeys.GRAVITY, gravity)
                props.put(ConstKeys.GRAVITY_ON, true)

                val snowball = MegaEntityFactory.fetch(Snowball::class)!!
                snowball.spawn(props)

                if (overlapsGameCamera()) requestToPlaySound(SoundAsset.CHILL_SHOOT_SOUND, false)
            }

            SniperJoeType.FIRE -> {
                trajectory.x = FIREBALL_X * ConstVals.PPM * facing.value

                val rotation = if (isFacing(Facing.LEFT)) 90f else 270f
                props.put(ConstKeys.ROTATION, rotation)

                val fireball = MegaEntityFactory.fetch(MagmaGoop::class)!!
                fireball.spawn(props)

                if (overlapsGameCamera()) requestToPlaySound(SoundAsset.BLAST_2_SOUND, false)
            }
        }
    }
}
