package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.common.GameLogger
import com.engine.common.enums.Direction
import com.engine.common.enums.Facing
import com.engine.common.enums.Position
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.interfaces.IFaceable
import com.engine.common.interfaces.Updatable
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.IDirectionRotatable
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.BodySense
import com.megaman.maverick.game.world.FixtureType
import com.megaman.maverick.game.world.isSensing
import kotlin.reflect.KClass

class Met(game: MegamanMaverickGame) : AbstractEnemy(game), IFaceable, IDirectionRotatable {

    enum class MetBehavior {
        SHIELDING, POP_UP, RUNNING
    }

    companion object {
        const val TAG = "Met"
        const val RUN_ONLY = "run_only"
        const val RUNNING_ALLOWED = "running_allowed"

        private var atlas: TextureAtlas? = null

        private const val SHIELDING_DURATION = 1.15f
        private const val RUNNING_DURATION = .5f
        private const val POP_UP_DURATION = .5f
        private const val RUN_SPEED = 8f
        private const val GRAVITY_IN_AIR = 18f
        private const val GRAVITY_ON_GROUND = .15f
        private const val BULLET_TRAJECTORY_X = 15f
        private const val BULLET_TRAJECTORY_Y = .25f
        private const val VELOCITY_CLAMP_X = 8f
        private const val VELOCITY_CLAMP_Y = 18f
    }

    override lateinit var facing: Facing

    override var directionRotation: Direction
        get() = body.cardinalRotation
        set(value) {
            body.cardinalRotation = value
        }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class to dmgNeg(10),
        Fireball::class to dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShot::class to dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShotExplosion::class to dmgNeg(ConstVals.MAX_HEALTH)
    )

    private val metBehaviorTimers = objectMapOf(
        MetBehavior.SHIELDING to Timer(SHIELDING_DURATION),
        MetBehavior.POP_UP to Timer(POP_UP_DURATION),
        MetBehavior.RUNNING to Timer(RUNNING_DURATION)
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
        super.init()
        if (atlas == null) atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "Spawn props = $spawnProps")

        super.spawn(spawnProps)
        behavior = MetBehavior.SHIELDING

        val spawn = if (spawnProps.containsKey(ConstKeys.BOUNDS)) {
            val bounds = spawnProps.get(ConstKeys.BOUNDS) as GameRectangle
            bounds.getBottomCenterPoint()
        } else spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setBottomCenterToPoint(spawn)

        runningAllowed = spawnProps.getOrDefault(RUNNING_ALLOWED, true, Boolean::class)
        runOnly = spawnProps.getOrDefault(RUN_ONLY, false, Boolean::class)
        runSpeed = spawnProps.getOrDefault(ConstKeys.SPEED, RUN_SPEED, Float::class)
        type = spawnProps.getOrDefault(ConstKeys.TYPE, "", String::class)
        val right = spawnProps.getOrDefault(ConstKeys.RIGHT, megaman.body.x > body.x, Boolean::class)
        facing = if (right) Facing.RIGHT else Facing.LEFT

        directionRotation = Direction.UP
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "Destroying Met")
        super.onDestroy()
    }

    override fun getTag() = TAG

    private fun shoot() {
        GameLogger.debug(TAG, "Met is shooting")

        val trajectory = (when (directionRotation) {
            Direction.UP -> Vector2(BULLET_TRAJECTORY_X * facing.value, BULLET_TRAJECTORY_Y)
            Direction.DOWN -> Vector2(BULLET_TRAJECTORY_X * facing.value, -BULLET_TRAJECTORY_Y)
            Direction.LEFT -> Vector2(BULLET_TRAJECTORY_Y, BULLET_TRAJECTORY_X * facing.value)
            Direction.RIGHT -> Vector2(BULLET_TRAJECTORY_Y, -BULLET_TRAJECTORY_X * facing.value)
        }).scl(ConstVals.PPM.toFloat())

        val offset = ConstVals.PPM / 64f
        val spawn = body.getCenter().add(offset * facing.value, if (isDirectionRotatedDown()) -offset else offset)

        val spawnProps = props(
            ConstKeys.OWNER to this, ConstKeys.TRAJECTORY to trajectory, ConstKeys.POSITION to spawn
        )
        val bullet = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.BULLET)
        game.engine.spawn(bullet!!, spawnProps)
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)

        updatablesComponent.add {
            if (runOnly) behavior = MetBehavior.RUNNING

            when (behavior) {
                MetBehavior.SHIELDING -> {
                    when (directionRotation) {
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

                    facing = when (directionRotation) {
                        Direction.UP, Direction.DOWN -> if (megaman.body.x > body.x) Facing.RIGHT else Facing.LEFT
                        Direction.LEFT, Direction.RIGHT -> if (megaman.body.y > body.y) Facing.RIGHT else Facing.LEFT
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
                    when (directionRotation) {
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
        body.setSize(0.75f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().setSize(0.75f * ConstVals.PPM))
        body.addFixture(bodyFixture)
        debugShapes.add { bodyFixture.getShape() }

        val feetFixture = Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.15f * ConstVals.PPM))
        feetFixture.offsetFromBodyCenter.y = -0.375f * ConstVals.PPM
        body.addFixture(feetFixture)

        val shieldFixture = Fixture(
            body, FixtureType.SHIELD, GameRectangle().setSize(0.75f * ConstVals.PPM, 0.5f * ConstVals.PPM)
        )
        body.addFixture(shieldFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().setSize(0.75f * ConstVals.PPM))
        body.addFixture(damageableFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(0.75f * ConstVals.PPM))
        body.addFixture(damagerFixture)

        body.preProcess.put(ConstKeys.DEFAULT, Updatable {
            body.physics.velocityClamp =
                (if (isDirectionRotatedVertically()) Vector2(VELOCITY_CLAMP_X, VELOCITY_CLAMP_Y)
                else Vector2(VELOCITY_CLAMP_Y, VELOCITY_CLAMP_X)).scl(ConstVals.PPM.toFloat())

            val gravity = (if (body.isSensing(BodySense.FEET_ON_GROUND)) GRAVITY_ON_GROUND else GRAVITY_IN_AIR)
            body.physics.gravity = (when (directionRotation) {
                Direction.UP -> Vector2(0f, -gravity)
                Direction.DOWN -> Vector2(0f, gravity)
                Direction.LEFT -> Vector2(gravity, 0f)
                Direction.RIGHT -> Vector2(-gravity, 0f)
            }).scl(ConstVals.PPM.toFloat())

            shieldFixture.active = behavior == MetBehavior.SHIELDING
            damageableFixture.active = behavior != MetBehavior.SHIELDING

            shieldFixture.putProperty(ConstKeys.DIRECTION, directionRotation)
        })

        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 4))
        sprite.setSize(1.5f * ConstVals.PPM)

        val spritesComponent = SpritesComponent(this, sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.hidden = damageBlink

            val flipX = facing == Facing.LEFT
            val flipY = directionRotation == Direction.DOWN
            _sprite.setFlip(flipX, flipY)

            val rotation = when (directionRotation) {
                Direction.UP, Direction.DOWN -> 0f

                Direction.LEFT -> 90f
                Direction.RIGHT -> 270f
            }
            sprite.setOriginCenter()
            _sprite.rotation = rotation

            val position = when (directionRotation) {
                Direction.UP -> Position.BOTTOM_CENTER
                Direction.DOWN -> Position.TOP_CENTER
                Direction.LEFT -> Position.CENTER_RIGHT
                Direction.RIGHT -> Position.CENTER_LEFT
            }
            val bodyPosition = body.getPositionPoint(position)
            _sprite.setPosition(bodyPosition, position)
        }

        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier = {
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
                "Run" to Animation(atlas!!.findRegion("Met/Run"), 1, 2, 0.125f, true),
                "PopUp" to Animation(atlas!!.findRegion("Met/PopUp"), false),
                "LayDown" to Animation(atlas!!.findRegion("Met/LayDown"), false),
                "SnowRun" to Animation(atlas!!.findRegion("SnowMet/Run"), 1, 2, 0.125f, true),
                "SnowPopUp" to Animation(atlas!!.findRegion("SnowMet/PopUp"), false),
                "SnowLayDown" to Animation(atlas!!.findRegion("SnowMet/LayDown"), false)
            )
        )
        return AnimationsComponent(this, animator)
    }
}
