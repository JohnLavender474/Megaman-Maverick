package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods.getRandom
import com.mega.game.engine.common.UtilMethods.normalizedTrajectory
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.putAll
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Loop
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setCenter
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
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.projectiles.SpitFireball
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getBoundingRectangle
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.misc.FacingUtils
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter
import kotlin.math.max

class MechaDragon_OLD(game: MegamanMaverickGame) : AbstractBoss(game, size = Size.LARGE), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "MechaDragon"

        private const val TARGETS_COUNT = 4

        private const val HOVER_SPEED = 3f
        private const val HOVER_X_SWAY_SPEED = 0.5f

        private const val FIRE_DUR = 0.5f
        private const val FIRE_DELAY = 1.5f
        private const val FIRES_TO_SHOOT = 2
        private const val FIRE_SPEED = 10f
        private const val FIRE_ANGLE_DELTA = 45f

        private const val CHARGE_SPEED = 6f
        private const val CHARGE_FIRST_DELAY_SPEED = 4f
        private const val CHARGE_FIRST_DELAY = 0.75f
        private const val CHARGE_SECOND_DELAY = 0.5f

        private const val TURN_AROUND_DUR = 0.5f

        private const val HOVER_TO_MEGAMAN_EPSILON = 3.5f

        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class MechaDragonState {
        IDLE, HOVER_TO_MEGAMAN, HOVER_TO_RANDOM_SPOT, CHARGE
    }

    override lateinit var facing: Facing

    private val loop = Loop(
        MechaDragonState.HOVER_TO_RANDOM_SPOT,
        MechaDragonState.IDLE,
        MechaDragonState.HOVER_TO_MEGAMAN,
        MechaDragonState.IDLE,
        MechaDragonState.CHARGE
    )
    private val currentState: MechaDragonState
        get() = loop.getCurrent()

    private val fireTimer = Timer(FIRE_DUR)
    private val fireDelayTimer = Timer(FIRE_DELAY)
    private val chargeFirstDelayTimer = Timer(CHARGE_FIRST_DELAY)
    private val chargeSecondDelayTimer = Timer(CHARGE_SECOND_DELAY)

    private val turnAroundTimer = Timer(TURN_AROUND_DUR)
    private val turningAround: Boolean
        get() = !turnAroundTimer.isFinished()

    private val shooting: Boolean
        get() = !fireTimer.isFinished()

    private val targets = Array<Vector2>().also { array ->
        (0 until TARGETS_COUNT).forEach { array.add(Vector2()) }
    }
    private val currentTarget = Vector2()
    private val returnSpot = Vector2()
    private val roomCenter = Vector2()

    private var maxX = 0f
    private var minX = 0f
    private var maxY = 0f
    private var minY = 0f

    private var firesShot = 0

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_1.source)
            gdxArrayOf("fly", "shoot", "turning", "defeated").forEach { key ->
                val region = atlas.findRegion("$TAG/$key")
                regions.put(key, region)
            }
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.MINI, true)
        spawnProps.put(ConstKeys.ORB, false)

        super.onSpawn(spawnProps)

        putProperty(ConstKeys.ENTITY_KILLED_BY_DEATH_FIXTURE, false)

        facing = if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setBottomCenterToPoint(spawn)

        for (i in 1..TARGETS_COUNT) {
            val key = "${ConstKeys.SPOT}_$i"
            val targetObject = spawnProps.get(key, RectangleMapObject::class)!!
            val spot = targetObject.rectangle.getCenter()
            if (targetObject.properties.get(ConstKeys.START, false, Boolean::class.java)) currentTarget.set(spot)
            targets[i - 1].set(spot)
            maxY = max(maxY, spot.y)
        }

        roomCenter.set(spawnProps.get(ConstKeys.CENTER, RectangleMapObject::class)!!.rectangle.getCenter())

        maxX = spawnProps.get("${ConstKeys.MAX}_${ConstKeys.X}", RectangleMapObject::class)!!.rectangle.getCenter().x
        minX = spawnProps.get("${ConstKeys.MIN}_${ConstKeys.X}", RectangleMapObject::class)!!.rectangle.getCenter().x
        minY = spawnProps.get("${ConstKeys.MIN}_${ConstKeys.Y}", RectangleMapObject::class)!!.rectangle.y

        returnSpot.set(
            spawnProps.get("${ConstKeys.RETURN}_${ConstKeys.SPOT}", RectangleMapObject::class)!!.rectangle.getCenter()
        )

        firesShot = 0

        fireTimer.setToEnd()
        fireDelayTimer.reset()
        chargeFirstDelayTimer.reset()
        chargeSecondDelayTimer.reset()
        turnAroundTimer.setToEnd()

        loop.reset()
    }

    override fun isReady(delta: Float) = true

    override fun onDefeated(delta: Float) {
        super.onDefeated(delta)

        if (body.getCenter().epsilonEquals(roomCenter, 0.1f * ConstVals.PPM)) {
            body.physics.velocity.setZero()
            return
        }

        body.physics.velocity
            .set(roomCenter)
            .sub(body.getCenter())
            .nor()
            .scl(HOVER_SPEED * ConstVals.PPM)
    }

    private fun spitFireball() {
        val spawn = body.getCenter().add(3f * ConstVals.PPM * facing.value, 0f)

        val maxAngle: Float
        val minAngle: Float
        when (facing) {
            Facing.LEFT -> {
                maxAngle = 90f + FIRE_ANGLE_DELTA
                minAngle = 90f - FIRE_ANGLE_DELTA
            }

            Facing.RIGHT -> {
                maxAngle = 270f + FIRE_ANGLE_DELTA
                minAngle = 270f - FIRE_ANGLE_DELTA
            }
        }
        val megamanToSpawnAngle = megaman.body.getCenter().sub(spawn).nor().angleDeg() - 90f
        var angle = megamanToSpawnAngle
        if (angle > maxAngle) angle = maxAngle else if (angle < minAngle) angle = minAngle

        val trajectory = GameObjectPools.fetch(Vector2::class)
            .set(0f, FIRE_SPEED * ConstVals.PPM)
            .rotateDeg(angle)

        GameLogger.debug(
            TAG,
            "spitFireball(): " +
                "spawn=$spawn, " +
                "megamanToSpawnAngle=${megamanToSpawnAngle}, " +
                "angle=$angle, " +
                "trajectory=$trajectory"
        )

        val fireball = MegaEntityFactory.fetch(SpitFireball::class)!!
        fireball.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.TRAJECTORY pairTo trajectory
            )
        )

        requestToPlaySound(SoundAsset.MM2_MECHA_DRAGON_SOUND, false)
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (!ready) return@add

            if (defeated) {
                explodeOnDefeat(delta)
                return@add
            }

            when (currentState) {
                MechaDragonState.IDLE -> {
                    facing = if (body.getCenter().x < roomCenter.x) Facing.RIGHT else Facing.LEFT

                    body.physics.velocity.x = HOVER_X_SWAY_SPEED * facing.value * ConstVals.PPM
                    body.physics.velocity.y = 0f

                    fireTimer.update(delta)
                    if (fireTimer.isFinished()) {
                        when {
                            firesShot < FIRES_TO_SHOOT -> {
                                fireDelayTimer.update(delta)
                                if (fireDelayTimer.isFinished()) {
                                    spitFireball()
                                    firesShot++
                                    fireTimer.reset()
                                    fireDelayTimer.reset()
                                }
                            }

                            else -> {
                                fireDelayTimer.reset()
                                firesShot = 0

                                GameLogger.debug(TAG, "from=${loop.getCurrent()}")
                                loop.next()
                                GameLogger.debug(TAG, "to=${loop.getCurrent()}")

                                currentTarget.set(
                                    when (currentState) {
                                        MechaDragonState.HOVER_TO_RANDOM_SPOT -> {
                                            val random = when {
                                                megaman.body.getX() > roomCenter.x -> getRandom(0, 3)
                                                else -> getRandom(4, 7)
                                            }

                                            targets[random]
                                        }

                                        else -> megaman.body.getCenter()
                                    }
                                )
                                currentTarget.y = currentTarget.y.coerceIn(minY, maxY)
                            }
                        }
                    }
                }

                MechaDragonState.HOVER_TO_MEGAMAN,
                MechaDragonState.HOVER_TO_RANDOM_SPOT -> {
                    facing = when {
                        turningAround -> if (body.getCenter().x < roomCenter.x) Facing.RIGHT else Facing.LEFT
                        else -> if (body.getCenter().x < currentTarget.x) Facing.RIGHT else Facing.LEFT
                    }

                    val trajectory = normalizedTrajectory(
                        body.getCenter(),
                        currentTarget,
                        HOVER_SPEED * ConstVals.PPM,
                        GameObjectPools.fetch(Vector2::class)
                    )
                    body.physics.velocity.set(trajectory)

                    val stopPredicate = when (currentState) {
                        MechaDragonState.HOVER_TO_MEGAMAN ->
                            body.getCenter().epsilonEquals(currentTarget, HOVER_TO_MEGAMAN_EPSILON * ConstVals.PPM)

                        else -> body.getCenter().epsilonEquals(currentTarget, 0.1f * ConstVals.PPM)
                    }

                    if (stopPredicate) {
                        body.physics.velocity.setZero()

                        if (!turningAround) {
                            if ((isFacing(Facing.LEFT) && body.getCenter().x < roomCenter.x) ||
                                (isFacing(Facing.RIGHT) && body.getCenter().x > roomCenter.x)
                            ) turnAroundTimer.reset()
                        }

                        if (turningAround) turnAroundTimer.update(delta)

                        else {
                            GameLogger.debug(TAG, "from=${loop.getCurrent()}")
                            loop.next()
                            GameLogger.debug(TAG, "to=${loop.getCurrent()}")
                        }
                    }
                }

                MechaDragonState.CHARGE -> {
                    if (!chargeFirstDelayTimer.isFinished()) {
                        body.physics.velocity.x = 0f

                        val megamanCenterY = megaman.body.getCenter().y
                        body.physics.velocity.y = (when {
                            megamanCenterY > body.getY() && megamanCenterY < body.getMaxY() -> 0f
                            else -> CHARGE_FIRST_DELAY_SPEED * if (megamanCenterY > body.getMaxY()) 1f else -1f
                        }) * ConstVals.PPM

                        FacingUtils.setFacingOf(this)

                        chargeFirstDelayTimer.update(delta)

                        return@add
                    }

                    if (!chargeSecondDelayTimer.isFinished()) {
                        body.physics.velocity.setZero()
                        chargeSecondDelayTimer.update(delta)
                        return@add
                    }

                    body.physics.velocity.x = CHARGE_SPEED * ConstVals.PPM * facing.value
                    body.physics.velocity.y = 0f

                    if (body.getX() > maxX || body.getMaxX() < minX) {
                        body.setBottomCenterToPoint(returnSpot)
                        body.physics.velocity.setZero()

                        chargeFirstDelayTimer.reset()
                        chargeSecondDelayTimer.reset()

                        GameLogger.debug(TAG, "from=${loop.getCurrent()}")
                        loop.next()
                        GameLogger.debug(TAG, "to=${loop.getCurrent()}")
                    }
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(2f * ConstVals.PPM, 5f * ConstVals.PPM)
        body.drawingColor = Color.DARK_GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val headDamagerFixture = Fixture(
            body,
            FixtureType.DAMAGER,
            GameRectangle().setSize(1.5f * ConstVals.PPM, 2.5f * ConstVals.PPM)
        )
        headDamagerFixture.offsetFromBodyAttachment.y = 1.25f * ConstVals.PPM
        body.addFixture(headDamagerFixture)
        debugShapes.add { headDamagerFixture }

        val headDamageableFixture = Fixture(
            body,
            FixtureType.DAMAGEABLE,
            GameRectangle().setSize(1.5f * ConstVals.PPM, 1.5f * ConstVals.PPM)
        )
        headDamageableFixture.offsetFromBodyAttachment.y = 1.25f * ConstVals.PPM
        body.addFixture(headDamageableFixture)
        // debugShapes.add { headDamageableFixture }

        val neckDamagerFixture =
            Fixture(body, FixtureType.DAMAGER, GameCircle().setRadius(ConstVals.PPM.toFloat()))
        neckDamagerFixture.offsetFromBodyAttachment.y = 1.25f * ConstVals.PPM
        body.addFixture(neckDamagerFixture)
        debugShapes.add { neckDamagerFixture }

        val neckDamageableFixture =
            Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().setSize(ConstVals.PPM.toFloat()))
        neckDamageableFixture.offsetFromBodyAttachment.y = 1.25f * ConstVals.PPM
        body.addFixture(neckDamageableFixture)
        // debugShapes.add { neckDamageableFixture }

        val bodyDamagerFixture =
            Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(2.5f * ConstVals.PPM, 6f * ConstVals.PPM))
        body.addFixture(bodyDamagerFixture)
        debugShapes.add { bodyDamagerFixture }

        val bodyDamageableFixture =
            Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().setSize(2.5f * ConstVals.PPM, 6f * ConstVals.PPM))
        body.addFixture(bodyDamageableFixture)
        // debugShapes.add { bodyDamageableFixture }

        val tailDamagerFixture1 = Fixture(
            body, FixtureType.DAMAGER, GameRectangle().setSize(0.75f * ConstVals.PPM, 2f * ConstVals.PPM)
        )
        tailDamagerFixture1.offsetFromBodyAttachment.y = -1.25f * ConstVals.PPM
        body.addFixture(tailDamagerFixture1)
        debugShapes.add { tailDamagerFixture1 }

        val tailDamageableFixture1 = Fixture(
            body, FixtureType.DAMAGEABLE, GameRectangle().setSize(0.75f * ConstVals.PPM, 2.25f * ConstVals.PPM)
        )
        tailDamageableFixture1.offsetFromBodyAttachment.y = -1.25f * ConstVals.PPM
        body.addFixture(tailDamageableFixture1)
        // debugShapes.add { tailDamageableFixture1 }

        val tailDamagerFixture2 =
            Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.5f * ConstVals.PPM))
        tailDamagerFixture2.offsetFromBodyAttachment.y = -0.25f * ConstVals.PPM
        body.addFixture(tailDamagerFixture2)
        debugShapes.add { tailDamagerFixture2 }

        val tailDamageableFixture2 =
            Fixture(
                body,
                FixtureType.DAMAGEABLE,
                GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.5f * ConstVals.PPM)
            )
        tailDamageableFixture2.offsetFromBodyAttachment.y = -0.25f * ConstVals.PPM
        body.addFixture(tailDamageableFixture2)
        // debugShapes.add { tailDamageableFixture2 }

        gdxArrayOf(
            tailDamageableFixture1,
            bodyDamageableFixture,
            neckDamageableFixture,
            headDamageableFixture
        ).forEach { t ->
            val bodyFixture1 = Fixture(
                body = body,
                type = FixtureType.BODY,
                rawShape = t.rawShape.copy(),
                offsetFromBodyAttachment = t.offsetFromBodyAttachment.cpy()
            )

            val width = bodyFixture1.getShape().getBoundingRectangle().getWidth() * 0.9f
            val height = bodyFixture1.getShape().getBoundingRectangle().getHeight() * 0.9f
            bodyFixture1.rawShape.setWithProps(props(ConstKeys.WIDTH pairTo width, ConstKeys.HEIGHT pairTo height))

            body.addFixture(bodyFixture1)
        }

        body.preProcess.put(ConstKeys.DEFAULT) {
            headDamagerFixture.offsetFromBodyAttachment.x = 2.5f * ConstVals.PPM * facing.value
            headDamageableFixture.offsetFromBodyAttachment.x = 2.5f * ConstVals.PPM * facing.value

            neckDamagerFixture.offsetFromBodyAttachment.x = 0.25f * ConstVals.PPM * facing.value
            neckDamageableFixture.offsetFromBodyAttachment.x = 0.25f * ConstVals.PPM * facing.value

            tailDamagerFixture1.offsetFromBodyAttachment.x = 2f * ConstVals.PPM * -facing.value
            tailDamageableFixture1.offsetFromBodyAttachment.x = 2f * ConstVals.PPM * -facing.value

            tailDamagerFixture2.offsetFromBodyAttachment.x = 3f * ConstVals.PPM * -facing.value
            tailDamageableFixture2.offsetFromBodyAttachment.x = 3f * ConstVals.PPM * -facing.value
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(
            TAG, GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 1))
                .also { sprite -> sprite.setSize(8f * ConstVals.PPM) }
        )
        .updatable { _, sprite ->
            sprite.setCenter(body.getCenter())
            sprite.hidden = damageBlink || !ready
            sprite.setFlip(isFacing(Facing.LEFT), false)
            sprite.setAlpha(if (defeated) 1f - defeatTimer.getRatio() else 1f)
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier {
                    when {
                        turningAround -> "turning"
                        defeated -> "defeated"
                        shooting -> "shoot"
                        else -> "fly"
                    }
                }
                .applyToAnimations { animations ->
                    val flyRegion = regions.get("fly")
                    val shootRegion = regions.get("shoot")
                    val turningRegion = regions.get("turning")
                    val defeatedRegion = regions.get("defeated")

                    animations.putAll(
                        "fly" pairTo Animation(flyRegion, 3, 2, 0.1f, true),
                        "shoot" pairTo Animation(shootRegion, 3, 2, 0.15f, true),
                        "turning" pairTo Animation(turningRegion, 5, 1, 0.1f, false),
                        "defeated" pairTo Animation(defeatedRegion, 3, 2, 0.1f, true)
                    )
                }
                .build()
        )
        .build()
}
