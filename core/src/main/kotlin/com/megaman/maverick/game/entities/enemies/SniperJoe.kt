package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.UtilMethods.normalizedTrajectory
import com.mega.game.engine.common.enums.*
import com.mega.game.engine.common.extensions.equalsAny
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
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
import com.megaman.maverick.game.com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.EnemyDamageNegotiations
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.IScalableGravityEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.world.body.*

class SniperJoe(game: MegamanMaverickGame) : AbstractEnemy(game), IScalableGravityEntity, IFaceable, IDirectional {

    companion object {
        const val TAG = "SniperJoe"

        private const val DEFAULT_TYPE = "Orange"
        private const val SNOW_TYPE = "Snow"
        private const val LAVA_TYPE = "Lava"

        private val TIMES_TO_SHOOT = floatArrayOf(0.15f, 0.75f, 1.35f)

        private const val BULLET_SPEED = 10f
        private const val SNOWBALL_X = 8f
        private const val SNOWBALL_Y = 5f
        private const val LAVA_X = 10f
        private const val SNOWBALL_GRAV = 0.15f
        private const val JUMP_IMPULSE = 15f

        private const val SHIELD_DUR = 1.75f
        private const val SHOOT_DUR = 1.5f
        private const val LAVA_SHOOT_DUR = 0.5f
        private const val THROW_SHIELD_DUR = 0.5f
        private const val SHIELD_VEL = 10f

        private const val GROUND_GRAVITY = 0.001f
        private const val GRAVITY = 0.375f

        private val regions = ObjectMap<String, TextureRegion>()
        private val joeTypes = gdxArrayOf(DEFAULT_TYPE, SNOW_TYPE, LAVA_TYPE)
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

    override val damageNegotiations = EnemyDamageNegotiations.getEnemyDmgNegs(Size.MEDIUM)
    override var direction: Direction
        get() = body.direction
        set(value) {
            body.direction = value
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

    private val shouldUpdate: Boolean
        get() = !game.isCameraRotating()
    private val waitTimer = Timer(SHIELD_DUR)
    private val shootTimer = Timer(SHOOT_DUR)
    private val throwShieldTimer = Timer(THROW_SHIELD_DUR)

    private var canJump = true
    private var canThrowShield = false
    private var setToThrowShield = false
    private var scaleBullet = true

    override fun getTag() = TAG

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            joeTypes.forEach { joeType ->
                regionKeys.forEach { regionKey ->
                    val region = atlas.findRegion("$TAG/$joeType/$regionKey")
                    regions.put("$joeType/$regionKey", region)
                }
            }
        }
        super.init()
        addComponent(defineAnimationsComponent())
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

        when {
            spawnProps.containsKey(ConstKeys.TRIGGER) -> {
                canThrowShield = true
                throwShieldTrigger =
                    spawnProps.get(ConstKeys.TRIGGER, RectangleMapObject::class)!!.rectangle.toGameRectangle()
            }

            else -> {
                canThrowShield = false
                throwShieldTrigger = null
            }
        }

        canJump = spawnProps.getOrDefault(ConstKeys.JUMP, true, Boolean::class)

        type = spawnProps.getOrDefault(ConstKeys.TYPE, DEFAULT_TYPE, String::class)
        sniperJoeState = SniperJoeState.WAITING_SHIELDED

        waitTimer.reset()
        throwShieldTimer.setToEnd()

        shootTimer.clearRunnables()
        when (type) {
            LAVA_TYPE -> {
                shootTimer.resetDuration(LAVA_SHOOT_DUR)
                val shootRunnable = TimeMarkedRunnable(TIMES_TO_SHOOT[0]) { shoot() }
                shootTimer.setRunnables(gdxArrayOf(shootRunnable))
            }

            else -> {
                shootTimer.resetDuration(SHOOT_DUR)
                val shootRunnables = Array<TimeMarkedRunnable>()
                TIMES_TO_SHOOT.forEach { shootRunnables.add(TimeMarkedRunnable(it) { shoot() }) }
                shootTimer.setRunnables(shootRunnables)
            }
        }
        shootTimer.setToEnd()

        gravityScalar = spawnProps.getOrDefault("${ConstKeys.GRAVITY}_${ConstKeys.SCALAR}", 1f, Float::class)
        scaleBullet = spawnProps.getOrDefault("${ConstKeys.SCALE}_${ConstKeys.BULLET}", true, Boolean::class)
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(1.5f * ConstVals.PPM, 2f * ConstVals.PPM)

        val shapes = Array<() -> IDrawableShape?>()

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().set(body))
        body.addFixture(bodyFixture)

        val feetFixture = Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        body.addFixture(feetFixture)
        // shapes.add { feetFixture }

        val headFixture = Fixture(body, FixtureType.HEAD, GameRectangle().setSize(0.1f * ConstVals.PPM))
        headFixture.offsetFromBodyAttachment.y = body.getHeight() / 2f
        body.addFixture(headFixture)
        // shapes.add { headFixture }

        val damagerFixture = Fixture(
            body, FixtureType.DAMAGER, GameRectangle().setSize(0.75f * ConstVals.PPM, 1.25f * ConstVals.PPM)
        )
        body.addFixture(damagerFixture)
        // shapes.add { damagerFixture }

        val damageableFixture = Fixture(
            body, FixtureType.DAMAGEABLE, GameRectangle().setSize(0.8f * ConstVals.PPM, 1.35f * ConstVals.PPM)
        )
        body.addFixture(damageableFixture)
        // shapes.add { damageableFixture }

        val shieldFixture = Fixture(
            body, FixtureType.SHIELD, GameRectangle().setSize(0.25f * ConstVals.PPM, 1.25f * ConstVals.PPM)
        )
        body.addFixture(shieldFixture)
        shapes.add { shieldFixture }

        val triggerFixture = Fixture(body, FixtureType.CONSUMER, GameRectangle())
        triggerFixture.setConsumer { processState, fixture ->
            if (hasShield && processState == ProcessState.BEGIN && fixture.getType() == FixtureType.PLAYER)
                setToThrowShield = true
        }
        triggerFixture.attachedToBody = false
        body.addFixture(triggerFixture)
        // shapes.add { triggerFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            when {
                canThrowShield && throwShieldTrigger != null -> {
                    triggerFixture.setActive(true)
                    triggerFixture.setShape(throwShieldTrigger!!)
                }

                else -> triggerFixture.setActive(false)
            }

            when {
                direction.equalsAny(Direction.UP, Direction.DOWN) -> body.physics.velocity.x = 0f
                else -> body.physics.velocity.y = 0f
            }

            val gravity = if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAVITY else GRAVITY
            val gravityVec = GameObjectPools.fetch(Vector2::class)
            when (direction) {
                Direction.UP -> gravityVec.set(0f, -gravity)
                Direction.DOWN -> gravityVec.set(0f, gravity)
                Direction.LEFT -> gravityVec.set(gravity, 0f)
                Direction.RIGHT -> gravityVec.set(-gravity, 0f)
            }.scl(ConstVals.PPM.toFloat() * gravityScalar)
            body.physics.gravity.set(gravityVec)

            shieldFixture.setActive(shielded)
            shieldFixture.offsetFromBodyAttachment.x =
                0.5f * ConstVals.PPM * if (direction.equalsAny(Direction.UP, Direction.LEFT)) facing.value
                else -facing.value

            when {
                shielded -> damageableFixture.offsetFromBodyAttachment.x = 0.25f * ConstVals.PPM * when {
                    direction.equalsAny(Direction.UP, Direction.LEFT) -> -facing.value
                    else -> facing.value
                }

                else -> damageableFixture.offsetFromBodyAttachment.x = 0f
            }
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = shapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
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

            if (direction == Direction.LEFT) sprite.translateX(0.15f * ConstVals.PPM)
            else if (direction == Direction.RIGHT) sprite.translateX(-0.15f * ConstVals.PPM)
        }
        return spritesComponent
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add {
            if (!shouldUpdate) return@add

            facing = when (direction) {
                Direction.UP, Direction.DOWN -> if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT
                Direction.LEFT -> if (megaman.body.getY() < body.getY()) Facing.LEFT else Facing.RIGHT
                Direction.RIGHT -> if (megaman.body.getY() < body.getY()) Facing.RIGHT else Facing.LEFT
            }

            if (canJump && shouldJump()) jump()
            when (direction) {
                Direction.UP -> if (body.physics.velocity.y > 0f &&
                    body.isSensing(BodySense.HEAD_TOUCHING_BLOCK) &&
                    !body.isSensing(BodySense.FEET_ON_GROUND)
                ) body.physics.velocity.y = 0f

                Direction.DOWN -> if (body.physics.velocity.y < 0f &&
                    body.isSensing(BodySense.HEAD_TOUCHING_BLOCK) &&
                    !body.isSensing(BodySense.FEET_ON_GROUND)
                ) body.physics.velocity.y = 0f

                Direction.LEFT -> if (body.physics.velocity.x < 0f &&
                    body.isSensing(BodySense.HEAD_TOUCHING_BLOCK) &&
                    !body.isSensing(BodySense.FEET_ON_GROUND)
                ) body.physics.velocity.x = 0f

                Direction.RIGHT -> if (body.physics.velocity.x > 0f &&
                    body.isSensing(BodySense.HEAD_TOUCHING_BLOCK) &&
                    !body.isSensing(BodySense.FEET_ON_GROUND)
                ) body.physics.velocity.x = 0f
            }

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
                SniperJoeState.WAITING_SHIELDED ->
                    if (body.isSensing(BodySense.FEET_ON_GROUND)) "StandShielded" else "JumpWithShield"

                SniperJoeState.WAITING_NO_SHIELD ->
                    if (body.isSensing(BodySense.FEET_ON_GROUND)) "StandNoShield" else "JumpNoShield"

                SniperJoeState.SHOOTING_WITH_SHIELD ->
                    if (body.isSensing(BodySense.FEET_ON_GROUND)) "ShootingWithShield" else "JumpWithShield"

                SniperJoeState.SHOOTING_NO_SHIELD ->
                    if (body.isSensing(BodySense.FEET_ON_GROUND)) "ShootingNoShield" else "JumpNoShield"

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
                ConstKeys.POSITION pairTo body.getCenter(), ConstKeys.TRAJECTORY pairTo normalizedTrajectory(
                    body.getCenter(),
                    megaman.body.getCenter(),
                    SHIELD_VEL * ConstVals.PPM,
                    GameObjectPools.fetch(Vector2::class)
                ), ConstKeys.OWNER pairTo this
            )
        )
    }

    private fun shouldJump(): Boolean {
        if (!body.isSensing(BodySense.FEET_ON_GROUND)) return false
        return when (direction) {
            Direction.UP -> megaman.body.getY() > body.getMaxY() &&
                megaman.body.getX() >= body.getX() &&
                megaman.body.getMaxX() <= body.getMaxX()

            Direction.DOWN -> megaman.body.getMaxY() < body.getY() &&
                megaman.body.getX() >= body.getX() &&
                megaman.body.getMaxX() <= body.getMaxX()

            Direction.LEFT -> megaman.body.getMaxX() < body.getX() &&
                megaman.body.getY() >= body.getY() &&
                megaman.body.getMaxY() <= body.getMaxY()

            Direction.RIGHT -> megaman.body.getX() > body.getMaxX() &&
                megaman.body.getY() >= body.getY() &&
                megaman.body.getMaxY() <= body.getMaxY()
        }
    }

    private fun jump() {
        val impulse = GameObjectPools.fetch(Vector2::class)
        when (direction) {
            Direction.UP -> impulse.set(0f, JUMP_IMPULSE)
            Direction.DOWN -> impulse.set(0f, -JUMP_IMPULSE)
            Direction.LEFT -> impulse.set(-JUMP_IMPULSE, 0f)
            Direction.RIGHT -> impulse.set(JUMP_IMPULSE, 0f)
        }.scl(ConstVals.PPM.toFloat())
        body.physics.velocity.set(impulse)
    }

    private fun shoot() {
        val spawn = GameObjectPools.fetch(Vector2::class)
        when (direction) {
            Direction.UP -> spawn.set(0.35f * facing.value, -0.275f)
            Direction.DOWN -> spawn.set(0.35f * facing.value, 0.275f)
            Direction.LEFT -> spawn.set(0.275f, 0.35f * facing.value)
            Direction.RIGHT -> spawn.set(-0.275f, 0.35f * -facing.value)
        }
        spawn.scl(ConstVals.PPM.toFloat()).add(body.getCenter())

        val trajectory = GameObjectPools.fetch(Vector2::class)

        val props = props(
            ConstKeys.OWNER pairTo this,
            ConstKeys.POSITION pairTo spawn,
            ConstKeys.TRAJECTORY pairTo trajectory,
            ConstKeys.DIRECTION pairTo direction
        )

        val sound: SoundAsset
        val entity: GameEntity = when (type) {
            SNOW_TYPE -> {
                trajectory.x = SNOWBALL_X * ConstVals.PPM * facing.value
                trajectory.y = SNOWBALL_Y * ConstVals.PPM

                props.put(ConstKeys.GRAVITY_ON, true)
                props.put(ConstKeys.GRAVITY, Vector2(0f, -SNOWBALL_GRAV * ConstVals.PPM))

                sound = SoundAsset.CHILL_SHOOT_SOUND

                EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.SNOWBALL)!!
            }

            LAVA_TYPE -> {
                trajectory.x = LAVA_X * ConstVals.PPM * facing.value

                val rotation = if (isFacing(Facing.LEFT)) 90f else 270f
                props.put(ConstKeys.ROTATION, rotation)

                sound = SoundAsset.BLAST_2_SOUND

                EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.MAGMA_GOOP)!!
            }

            else -> {
                when (direction) {
                    Direction.UP, Direction.DOWN ->
                        trajectory.set(BULLET_SPEED * ConstVals.PPM * facing.value, 0f)

                    Direction.LEFT ->
                        trajectory.set(0f, BULLET_SPEED * ConstVals.PPM * facing.value)

                    Direction.RIGHT ->
                        trajectory.set(0f, -BULLET_SPEED * ConstVals.PPM * facing.value)
                }
                sound = SoundAsset.ENEMY_BULLET_SOUND
                EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.BULLET)!!
            }
        }

        if (scaleBullet) trajectory.scl(gravityScalar)
        if (overlapsGameCamera()) requestToPlaySound(sound, false)

        entity.spawn(props)
    }
}
