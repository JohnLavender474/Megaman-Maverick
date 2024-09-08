package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.ProcessState
import com.mega.game.engine.common.extensions.equalsAny
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.normalizedTrajectory
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.toGameRectangle
import com.mega.game.engine.common.time.TimeMarkedRunnable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.GameEntity
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
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.IDirectionRotatable
import com.megaman.maverick.game.entities.contracts.IScalableGravityEntity
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.world.body.*
import kotlin.reflect.KClass

class SniperJoe(game: MegamanMaverickGame) : AbstractEnemy(game), IScalableGravityEntity, IFaceable,
    IDirectionRotatable {

    companion object {
        const val TAG = "SniperJoe"

        private const val DEFAULT_TYPE = "Orange"
        private const val SNOW_TYPE = "Snow"

        private val TIMES_TO_SHOOT = floatArrayOf(0.15f, 0.75f, 1.35f)

        private const val BULLET_SPEED = 7.5f
        private const val SNOWBALL_X = 8f
        private const val SNOWBALL_Y = 5f
        private const val SNOWBALL_GRAV = 0.15f
        private const val JUMP_IMPULSE = 15f

        private const val SHIELD_DUR = 1.75f
        private const val SHOOT_DUR = 1.5f
        private const val THROW_SHIELD_DUR = 0.5f
        private const val SHIELD_VEL = 10f

        private const val GROUND_GRAVITY = 0.015f
        private const val GRAVITY = 0.375f

        private val regions = ObjectMap<String, TextureRegion>()
        private val joeTypes = gdxArrayOf(DEFAULT_TYPE, SNOW_TYPE)
        private val regionKeys = gdxArrayOf(
            "JumpNoShield",
            "JumpWithShield",
            "ShootingNoShield",
            "ShootingWithShield",
            "StandNoShield",
            "StandShielded",
            "ThrowShield"
        )
    }

    enum class SniperJoeState {
        WAITING_SHIELDED, WAITING_NO_SHIELD, SHOOTING_WITH_SHIELD, SHOOTING_NO_SHIELD, THROWING_SHIELD
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class to dmgNeg(5),
        Fireball::class to dmgNeg(15),
        ChargedShot::class to dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) 15 else 10
        },
        ChargedShotExplosion::class to dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) 10 else 5
        }
    )
    override var directionRotation: Direction?
        get() = body.cardinalRotation
        set(value) {
            body.cardinalRotation = value
        }
    override var facing = Facing.RIGHT
    override var gravityScalar = 1f

    private lateinit var type: String
    private lateinit var sniperJoeState: SniperJoeState

    private var throwShieldTrigger: GameRectangle? = null

    private val shielded: Boolean
        get() = sniperJoeState == SniperJoeState.WAITING_SHIELDED
    private val hasShield: Boolean
        get() = sniperJoeState == SniperJoeState.WAITING_SHIELDED || sniperJoeState == SniperJoeState.SHOOTING_WITH_SHIELD

    private val waitTimer = Timer(SHIELD_DUR)
    private val shootTimer = Timer(SHOOT_DUR)
    private val throwShieldTimer = Timer(THROW_SHIELD_DUR)

    private var canJump = true
    private var canThrowShield = false
    private var setToThrowShield = false

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            joeTypes.forEach { joeType ->
                regionKeys.forEach { regionKey ->
                    val region = atlas.findRegion("SniperJoe/$joeType/$regionKey")
                    regions.put("$joeType/$regionKey", region)
                }
            }
        }
        super.init()
        val shootRunnables = Array<TimeMarkedRunnable>()
        TIMES_TO_SHOOT.forEach { shootRunnables.add(TimeMarkedRunnable(it) { shoot() }) }
        shootTimer.setRunnables(shootRunnables)
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn =
            if (spawnProps.containsKey(ConstKeys.BOUNDS)) spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
                .getBottomCenterPoint()
            else spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.positionOnPoint(spawn, Position.BOTTOM_CENTER)

        if (spawnProps.containsKey(ConstKeys.TRIGGER)) {
            canThrowShield = true
            throwShieldTrigger =
                spawnProps.get(ConstKeys.TRIGGER, RectangleMapObject::class)!!.rectangle.toGameRectangle()
        } else {
            canThrowShield = false
            throwShieldTrigger = null
        }

        canJump = spawnProps.getOrDefault(ConstKeys.JUMP, true, Boolean::class)

        type = spawnProps.getOrDefault(ConstKeys.TYPE, DEFAULT_TYPE, String::class)
        sniperJoeState = SniperJoeState.WAITING_SHIELDED
        directionRotation = Direction.valueOf(
            spawnProps.getOrDefault(ConstKeys.DIRECTION, "up", String::class).uppercase()
        )

        waitTimer.reset()
        shootTimer.setToEnd()
        throwShieldTimer.setToEnd()

        gravityScalar = 1f
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(ConstVals.PPM.toFloat(), 1.25f * ConstVals.PPM)

        val shapes = Array<() -> IDrawableShape?>()

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().set(body))
        body.addFixture(bodyFixture)

        val feetFixture = Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyCenter.y = -0.75f * ConstVals.PPM
        body.addFixture(feetFixture)
        feetFixture.rawShape.color = Color.GREEN
        shapes.add { feetFixture.getShape() }

        val damagerFixture = Fixture(
            body, FixtureType.DAMAGER, GameRectangle().setSize(0.75f * ConstVals.PPM, 1.15f * ConstVals.PPM)
        )
        body.addFixture(damagerFixture)
        damagerFixture.rawShape.color = Color.RED
        shapes.add { damagerFixture.getShape() }

        val damageableFixture = Fixture(
            body, FixtureType.DAMAGEABLE, GameRectangle().setSize(0.8f * ConstVals.PPM, 1.35f * ConstVals.PPM)
        )
        body.addFixture(damageableFixture)
        damageableFixture.getShape().color = Color.PURPLE
        shapes.add { damageableFixture.getShape() }

        val shieldFixture = Fixture(
            body, FixtureType.SHIELD, GameRectangle().setSize(0.4f * ConstVals.PPM, 0.9f * ConstVals.PPM)
        )
        body.addFixture(shieldFixture)
        shieldFixture.getShape().color = Color.BLUE
        shapes.add { shieldFixture.getShape() }

        val triggerFixture = Fixture(body, FixtureType.CONSUMER, GameRectangle())
        triggerFixture.setConsumer { processState, fixture ->
            if (hasShield && processState == ProcessState.BEGIN && fixture.getFixtureType() == FixtureType.PLAYER)
                setToThrowShield = true
        }
        triggerFixture.attachedToBody = false
        body.addFixture(triggerFixture)
        triggerFixture.getShape().color = Color.YELLOW
        shapes.add { triggerFixture.getShape() }

        body.preProcess.put(ConstKeys.DEFAULT, Updatable {
            if (canThrowShield && throwShieldTrigger != null) {
                triggerFixture.active = true
                triggerFixture.rawShape = throwShieldTrigger!!
            } else triggerFixture.active = false

            if (directionRotation!!.equalsAny(Direction.UP, Direction.DOWN)) body.physics.velocity.x = 0f
            else body.physics.velocity.y = 0f

            val gravity = if (body.isSensing(BodySense.FEET_ON_GROUND)) -GROUND_GRAVITY else -GRAVITY
            body.physics.gravity = (when (directionRotation!!) {
                Direction.UP -> Vector2(0f, gravity)
                Direction.DOWN -> Vector2(0f, -gravity)
                Direction.LEFT -> Vector2(-gravity, 0f)
                Direction.RIGHT -> Vector2(gravity, 0f)
            }).scl(ConstVals.PPM.toFloat() * gravityScalar)

            shieldFixture.active = shielded
            shieldFixture.offsetFromBodyCenter.x =
                0.35f * ConstVals.PPM * if (isDirectionRotatedUp() || isDirectionRotatedLeft()) facing.value
                else -facing.value

            if (shielded) damageableFixture.offsetFromBodyCenter.x =
                0.25f * ConstVals.PPM * if (isDirectionRotatedUp() || isDirectionRotatedLeft()) -facing.value
                else facing.value
            else damageableFixture.offsetFromBodyCenter.x = 0f
        })

        addComponent(DrawableShapesComponent(debugShapeSuppliers = shapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.35f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.hidden = damageBlink

            val flipX = facing == Facing.LEFT
            val flipY = directionRotation == Direction.DOWN
            _sprite.setFlip(flipX, flipY)

            val rotation = when (directionRotation!!) {
                Direction.UP, Direction.DOWN -> 0f

                Direction.LEFT -> 90f
                Direction.RIGHT -> 270f
            }
            sprite.setOriginCenter()
            _sprite.rotation = rotation

            val position = when (directionRotation!!) {
                Direction.UP -> Position.BOTTOM_CENTER
                Direction.DOWN -> Position.TOP_CENTER
                Direction.LEFT -> Position.CENTER_RIGHT
                Direction.RIGHT -> Position.CENTER_LEFT
            }
            val bodyPosition = body.getPositionPoint(position)
            _sprite.setPosition(bodyPosition, position)

            if (directionRotation == Direction.LEFT) _sprite.translateX(0.15f * ConstVals.PPM)
            else if (directionRotation == Direction.RIGHT) _sprite.translateX(-0.15f * ConstVals.PPM)
        }
        return spritesComponent
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add {
            facing = when (directionRotation!!) {
                Direction.UP, Direction.DOWN -> if (getMegaman().body.x > body.x) Facing.RIGHT else Facing.LEFT
                Direction.LEFT, Direction.RIGHT -> if (getMegaman().body.y > body.y) Facing.RIGHT else Facing.LEFT
            }

            if (canJump && shouldJump()) jump()

            if (!overlapsGameCamera()) {
                sniperJoeState = if (hasShield) SniperJoeState.WAITING_SHIELDED else SniperJoeState.WAITING_NO_SHIELD
                waitTimer.reset()
                return@add
            }

            when (sniperJoeState) {
                SniperJoeState.WAITING_SHIELDED -> {
                    if (setToThrowShield) {
                        throwShield()
                        throwShieldTimer.reset()
                        sniperJoeState = SniperJoeState.THROWING_SHIELD
                        setToThrowShield = false
                    } else if (body.isSensing(BodySense.FEET_ON_GROUND)) {
                        waitTimer.update(it)
                        if (waitTimer.isJustFinished()) {
                            shootTimer.reset()
                            sniperJoeState = SniperJoeState.SHOOTING_WITH_SHIELD
                        }
                    }
                }

                SniperJoeState.SHOOTING_WITH_SHIELD -> {
                    shootTimer.update(it)
                    if (shootTimer.isJustFinished()) {
                        waitTimer.reset()
                        sniperJoeState = SniperJoeState.WAITING_SHIELDED
                    }
                }

                SniperJoeState.THROWING_SHIELD -> {
                    throwShieldTimer.update(it)
                    if (throwShieldTimer.isJustFinished()) {
                        waitTimer.reset()
                        sniperJoeState = SniperJoeState.WAITING_NO_SHIELD
                    }
                }

                SniperJoeState.WAITING_NO_SHIELD -> {
                    waitTimer.update(it)
                    if (waitTimer.isJustFinished()) {
                        shootTimer.reset()
                        sniperJoeState = SniperJoeState.SHOOTING_NO_SHIELD
                    }
                }

                SniperJoeState.SHOOTING_NO_SHIELD -> {
                    shootTimer.update(it)
                    if (shootTimer.isJustFinished()) {
                        waitTimer.reset()
                        sniperJoeState = SniperJoeState.WAITING_NO_SHIELD
                    }
                }
            }
        }
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String = {
            val regionKey = when (sniperJoeState) {
                SniperJoeState.WAITING_SHIELDED -> {
                    if (body.isSensing(BodySense.FEET_ON_GROUND)) "StandShielded"
                    else "JumpWithShield"
                }

                SniperJoeState.WAITING_NO_SHIELD -> {
                    if (body.isSensing(BodySense.FEET_ON_GROUND)) "StandNoShield"
                    else "JumpNoShield"
                }

                SniperJoeState.SHOOTING_WITH_SHIELD -> {
                    if (body.isSensing(BodySense.FEET_ON_GROUND)) "ShootingWithShield"
                    else "JumpWithShield"
                }

                SniperJoeState.SHOOTING_NO_SHIELD -> {
                    if (body.isSensing(BodySense.FEET_ON_GROUND)) "ShootingNoShield"
                    else "JumpNoShield"
                }

                SniperJoeState.THROWING_SHIELD -> "ThrowShield"
            }
            "$type/$regionKey"
        }

        val animations = ObjectMap<String, IAnimation>()
        joeTypes.forEach { joeType ->
            regionKeys.forEach { regionKey ->
                val region = regions.get("$joeType/$regionKey")
                animations.put("$joeType/$regionKey", Animation(region))
            }
        }

        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    private fun throwShield() {
        val shield = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.SNIPER_JOE_SHIELD)!!
        shield.spawn(
            props(
                ConstKeys.POSITION to body.getCenter(), ConstKeys.TRAJECTORY to normalizedTrajectory(
                    body.getCenter(), getMegaman().body.getCenter(), SHIELD_VEL * ConstVals.PPM
                ), ConstKeys.OWNER to this
            )
        )
    }

    private fun shouldJump() = body.isSensing(BodySense.FEET_ON_GROUND) && (when (directionRotation!!) {
        Direction.UP, Direction.DOWN -> getMegaman().body.x >= body.x && getMegaman().body.getMaxX() <= body.getMaxX()
        Direction.LEFT, Direction.RIGHT -> getMegaman().body.y >= body.y && getMegaman().body.getMaxY() <= body.getMaxY()
    })

    private fun jump() {
        val impulse = (when (directionRotation!!) {
            Direction.UP -> Vector2(0f, JUMP_IMPULSE)
            Direction.DOWN -> Vector2(0f, -JUMP_IMPULSE)
            Direction.LEFT -> Vector2(-JUMP_IMPULSE, 0f)
            Direction.RIGHT -> Vector2(JUMP_IMPULSE, 0f)
        }).scl(ConstVals.PPM.toFloat())
        body.physics.velocity = impulse
    }

    private fun shoot() {
        val spawn = (when (directionRotation!!) {
            Direction.UP -> Vector2(0.25f * facing.value, -0.15f)
            Direction.DOWN -> Vector2(0.25f * facing.value, 0.15f)
            Direction.LEFT -> Vector2(0.2f, 0.25f * facing.value)
            Direction.RIGHT -> Vector2(-0.2f, 0.25f * facing.value)
        }).scl(ConstVals.PPM.toFloat()).add(body.getCenter())

        val trajectory = Vector2()

        val props = props(
            ConstKeys.OWNER to this,
            ConstKeys.POSITION to spawn,
            ConstKeys.TRAJECTORY to trajectory,
            ConstKeys.DIRECTION to directionRotation
        )

        val entity: GameEntity = if (type == SNOW_TYPE) {
            trajectory.x = SNOWBALL_X * ConstVals.PPM * facing.value
            trajectory.y = SNOWBALL_Y * ConstVals.PPM

            props.put(ConstKeys.GRAVITY_ON, true)
            props.put(ConstKeys.GRAVITY, Vector2(0f, -SNOWBALL_GRAV * ConstVals.PPM))

            if (overlapsGameCamera()) requestToPlaySound(SoundAsset.CHILL_SHOOT_SOUND, false)

            EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.SNOWBALL)!!
        } else {
            if (isDirectionRotatedVertically()) trajectory.set(BULLET_SPEED * ConstVals.PPM * facing.value, 0f)
            else trajectory.set(0f, BULLET_SPEED * ConstVals.PPM * facing.value)

            if (overlapsGameCamera()) requestToPlaySound(SoundAsset.ENEMY_BULLET_SOUND, false)

            EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.BULLET)!!
        }

        entity.spawn(props)
    }
}
