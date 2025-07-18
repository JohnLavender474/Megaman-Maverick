package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.bosses.GutsTankFist
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.projectiles.BigAssMaverickRobotOrb
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.PurpleBlast
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*

class Met(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.SMALL), IFaceable, IDirectional {

    enum class MetBehavior { SHIELDING, POP_UP, RUNNING }

    companion object {
        const val TAG = "Met"

        const val RUN_ONLY = "run_only"
        const val RUNNING_ALLOWED = "running_allowed"

        private var atlas: TextureAtlas? = null

        private const val SHIELDING_DURATION = 1.15f
        private const val RUNNING_DURATION = 0.5f
        private const val POP_UP_DURATION = 0.5f

        private const val RUN_SPEED = 8f

        private const val GRAVITY_IN_AIR = 0.25f
        private const val GRAVITY_ON_GROUND = 0.01f

        private const val BULLET_TRAJECTORY_X = 15f
        private const val BULLET_TRAJECTORY_Y = 0.25f

        private const val VELOCITY_CLAMP_X = 8f
        private const val VELOCITY_CLAMP_Y = 18f
    }

    override lateinit var facing: Facing
    override var direction: Direction
        get() = body.direction
        set(value) {
            body.direction = value
        }

    private val metBehaviorTimers = objectMapOf(
        MetBehavior.SHIELDING pairTo Timer(SHIELDING_DURATION),
        MetBehavior.POP_UP pairTo Timer(POP_UP_DURATION),
        MetBehavior.RUNNING pairTo Timer(RUNNING_DURATION)
    )
    private lateinit var type: String
    private var behavior: MetBehavior = MetBehavior.SHIELDING
        set(value) {
            field = value
            metBehaviorTimers.values().forEach { it.reset() }
        }
    private var runOnly = false
    private var runningAllowed = false
    private var runSpeed = RUN_SPEED

    override fun init() {
        GameLogger.debug(TAG, "init()")
        super.init()
        if (atlas == null) atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
        addComponent(defineAnimationsComponent())
        damageOverrides.put(GutsTankFist::class, dmgNeg(ConstVals.MAX_HEALTH))
        damageOverrides.put(BigAssMaverickRobotOrb::class, dmgNeg(ConstVals.MAX_HEALTH))
        damageOverrides.put(PurpleBlast::class, dmgNeg(ConstVals.MAX_HEALTH))
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        behavior = MetBehavior.SHIELDING

        val spawn = if (spawnProps.containsKey(ConstKeys.BOUNDS)) {
            val bounds = spawnProps.get(ConstKeys.BOUNDS) as GameRectangle
            bounds.getPositionPoint(Position.BOTTOM_CENTER)
        } else spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setBottomCenterToPoint(spawn)

        runningAllowed = spawnProps.getOrDefault(RUNNING_ALLOWED, true, Boolean::class)
        runOnly = spawnProps.getOrDefault(RUN_ONLY, false, Boolean::class)
        runSpeed = spawnProps.getOrDefault(ConstKeys.SPEED, RUN_SPEED, Float::class)

        type = spawnProps.getOrDefault(ConstKeys.TYPE, "", String::class)

        val right = spawnProps.getOrDefault(ConstKeys.RIGHT, megaman.body.getX() > body.getX(), Boolean::class)
        facing = if (right) Facing.RIGHT else Facing.LEFT

        direction = Direction.UP
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    override fun canBeDamagedBy(damager: IDamager) =
        damageOverrides.containsKey(damager::class) || super.canBeDamagedBy(damager)

    override fun takeDamageFrom(damager: IDamager): Boolean {
        if (damageOverrides.containsKey(damager::class)) explode()
        return super.takeDamageFrom(damager)
    }

    private fun shoot() {
        GameLogger.debug(TAG, "shoot()")

        val trajectory = (when (direction) {
            Direction.UP -> Vector2(BULLET_TRAJECTORY_X * facing.value, BULLET_TRAJECTORY_Y)
            Direction.DOWN -> Vector2(BULLET_TRAJECTORY_X * facing.value, -BULLET_TRAJECTORY_Y)
            Direction.LEFT -> Vector2(BULLET_TRAJECTORY_Y, BULLET_TRAJECTORY_X * facing.value)
            Direction.RIGHT -> Vector2(BULLET_TRAJECTORY_Y, -BULLET_TRAJECTORY_X * facing.value)
        }).scl(ConstVals.PPM.toFloat())

        val offset = 0.1f * ConstVals.PPM
        val spawn = body.getCenter().add(offset * facing.value, if (direction == Direction.DOWN) offset else -offset)

        val spawnProps = props(
            ConstKeys.OWNER pairTo this, ConstKeys.TRAJECTORY pairTo trajectory, ConstKeys.POSITION pairTo spawn
        )
        val bullet = MegaEntityFactory.fetch(Bullet::class)!!
        bullet.spawn(spawnProps)

        requestToPlaySound(SoundAsset.ENEMY_BULLET_SOUND, false)
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add {
            if (runOnly) behavior = MetBehavior.RUNNING

            when (behavior) {
                MetBehavior.SHIELDING -> {
                    when (direction) {
                        Direction.UP, Direction.DOWN -> body.physics.velocity.x = 0f
                        Direction.LEFT, Direction.RIGHT -> body.physics.velocity.y = 0f
                    }
                    val shieldTimer = metBehaviorTimers.get(MetBehavior.SHIELDING)
                    if (!isMegamanShootingAtMe() && body.isSensing(BodySense.FEET_ON_GROUND)) shieldTimer.update(it)
                    if (shieldTimer.isFinished()) behavior = MetBehavior.POP_UP
                }

                MetBehavior.POP_UP -> {
                    if (!body.isSensing(BodySense.FEET_ON_GROUND)) {
                        behavior = MetBehavior.SHIELDING
                        return@add
                    }

                    facing = when (direction) {
                        Direction.UP, Direction.DOWN -> if (megaman.body.getX() > body.getX()) Facing.RIGHT else Facing.LEFT
                        Direction.LEFT, Direction.RIGHT -> if (megaman.body.getY() > body.getY()) Facing.RIGHT else Facing.LEFT
                    }

                    val popUpTimer = metBehaviorTimers.get(MetBehavior.POP_UP)
                    if (popUpTimer.isAtBeginning()) shoot()
                    popUpTimer.update(it)
                    if (popUpTimer.isFinished()) behavior =
                        if (runningAllowed) MetBehavior.RUNNING else MetBehavior.SHIELDING
                }

                MetBehavior.RUNNING -> {
                    if (!body.isSensing(BodySense.FEET_ON_GROUND)) {
                        behavior = MetBehavior.SHIELDING
                        return@add
                    }

                    val runningTimer = metBehaviorTimers.get(MetBehavior.RUNNING)

                    val runImpulse =
                        ConstVals.PPM * facing.value * if (body.isSensing(BodySense.IN_WATER)) (runSpeed / 2f) else runSpeed
                    when (direction) {
                        Direction.UP, Direction.DOWN -> body.physics.velocity.x = runImpulse
                        Direction.LEFT, Direction.RIGHT -> body.physics.velocity.y = runImpulse
                    }

                    if (!runOnly) {
                        runningTimer.update(it)
                        if (runningTimer.isFinished()) {
                            if (body.isSensing(BodySense.FEET_ON_GROUND)) body.physics.velocity.setZero()
                            behavior = MetBehavior.SHIELDING
                        }
                    }
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.setSize(ConstVals.PPM.toFloat())

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        body.addFixture(feetFixture)
        feetFixture.drawingColor = Color.GREEN
        debugShapes.add { feetFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.physics.velocityClamp.set(
                when {
                    direction.isVertical() -> Vector2(VELOCITY_CLAMP_X, VELOCITY_CLAMP_Y)
                    else -> Vector2(VELOCITY_CLAMP_Y, VELOCITY_CLAMP_X)
                }
            ).scl(ConstVals.PPM.toFloat())

            val gravity = if (body.isSensing(BodySense.FEET_ON_GROUND)) GRAVITY_ON_GROUND else GRAVITY_IN_AIR
            val gravityVec = GameObjectPools.fetch(Vector2::class)
            when (direction) {
                Direction.UP -> gravityVec.set(0f, -gravity)
                Direction.DOWN -> gravityVec.set(0f, gravity)
                Direction.LEFT -> gravityVec.set(gravity, 0f)
                Direction.RIGHT -> gravityVec.set(-gravity, 0f)
            }.scl(ConstVals.PPM.toFloat())
            body.physics.gravity.set(gravityVec)

            body.fixtures.get(FixtureType.SHIELD).first().let {
                it.setActive(behavior == MetBehavior.SHIELDING)
                it.putProperty(ConstKeys.DIRECTION, direction)
            }
            body.fixtures.get(FixtureType.DAMAGEABLE).first().setActive(behavior != MetBehavior.SHIELDING)
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE, FixtureType.SHIELD)
        )
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 4))
        sprite.setSize(2f * ConstVals.PPM)
        val component = SpritesComponent(sprite)
        component.putUpdateFunction { _, _ ->
            sprite.hidden = damageBlink

            val flipX = facing == Facing.LEFT
            val flipY = direction == Direction.DOWN
            sprite.setFlip(flipX, flipY)

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

        return component
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = {
            "$type${
                when (behavior) {
                    MetBehavior.SHIELDING -> "LayDown"
                    MetBehavior.POP_UP -> "PopUp"
                    MetBehavior.RUNNING -> "Run"
                }
            }"
        }
        val animator = Animator(
            keySupplier, objectMapOf(
                "Run" pairTo Animation(atlas!!.findRegion("Met/Run"), 1, 2, 0.125f, true),
                "PopUp" pairTo Animation(atlas!!.findRegion("Met/PopUp"), false),
                "LayDown" pairTo Animation(atlas!!.findRegion("Met/LayDown"), false),
                "SnowRun" pairTo Animation(atlas!!.findRegion("SnowMet/Run"), 1, 2, 0.125f, true),
                "SnowPopUp" pairTo Animation(atlas!!.findRegion("SnowMet/PopUp"), false),
                "SnowLayDown" pairTo Animation(atlas!!.findRegion("SnowMet/LayDown"), false)
            )
        )
        return AnimationsComponent(this, animator)
    }

    override fun getTag() = TAG
}
