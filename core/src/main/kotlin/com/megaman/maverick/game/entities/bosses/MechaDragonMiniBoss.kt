package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods.normalizedTrajectory
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Loop
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
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
import com.megaman.maverick.game.com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.utils.GameObjectPools

import com.megaman.maverick.game.utils.extensions.getBoundingRectangle
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.*
import kotlin.math.max
import kotlin.reflect.KClass

class MechaDragonMiniBoss(game: MegamanMaverickGame) : AbstractBoss(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "MechaDragonMiniBoss"

        private const val TARGETS_COUNT = 4

        private const val HOVER_SPEED = 3f
        private const val HOVER_X_SWAY_SPEED = 0.5f

        private const val FIRE_DUR = 0.5f
        private const val FIRE_DELAY = 1.5f
        private const val FIRES_TO_SHOOT = 2
        private const val FIRE_SPEED = 6f

        private const val CHARGE_SPEED = 6f
        private const val CHARGE_FIRST_DELAY_SPEED = 4f
        private const val CHARGE_FIRST_DELAY = 0.75f
        private const val CHARGE_SECOND_DELAY = 0.5f

        private const val HOVER_TO_MEGAMAN_EPSILON = 3.5f

        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class MechaDragonState {
        IDLE, HOVER_TO_MEGAMAN, HOVER_TO_RANDOM_SPOT, HOVER_TO_ROOM_CENTER, CHARGE
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class pairTo dmgNeg(1),
        ChargedShot::class pairTo dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) 2 else 1
        },
        ChargedShotExplosion::class pairTo dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) 2 else 1
        })
    override lateinit var facing: Facing

    private val loop = Loop(
        gdxArrayOf(
            MechaDragonState.HOVER_TO_RANDOM_SPOT,
            MechaDragonState.IDLE,
            MechaDragonState.HOVER_TO_MEGAMAN,
            MechaDragonState.IDLE,
            MechaDragonState.HOVER_TO_ROOM_CENTER,
            MechaDragonState.CHARGE
        )
    )

    private val fireTimer = Timer(FIRE_DUR)
    private val fireDelayTimer = Timer(FIRE_DELAY)
    private val chargeFirstDelayTimer = Timer(CHARGE_FIRST_DELAY)
    private val chargeSecondDelayTimer = Timer(CHARGE_SECOND_DELAY)

    private val shooting: Boolean
        get() = !fireTimer.isFinished()

    private val targets = Array<Vector2>()
    private val currentTarget = Vector2()
    private val returnSpot = Vector2()
    private val roomCenter = Vector2()

    private var maxX = 0f
    private var minX = 0f
    private var maxY = 0f
    private var minY = 0f

    private var firesShot = 0

    init {
        (0 until TARGETS_COUNT).forEach { targets.add(Vector2()) }
    }

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_1.source)
            regions.put("fly", atlas.findRegion("$TAG/fly"))
            regions.put("shoot", atlas.findRegion("$TAG/shoot"))
            regions.put("defeated", atlas.findRegion("$TAG/defeated"))
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.ORB, false)
        putProperty(ConstKeys.ENTITY_KILLED_BY_DEATH_FIXTURE, false)
        super.onSpawn(spawnProps)

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

        loop.reset()
    }

    override fun isReady(delta: Float) = true // TODO

    override fun onDefeated(delta: Float) {
        super.onDefeated(delta)

        if (body.getCenter().epsilonEquals(roomCenter, 0.1f * ConstVals.PPM)) {
            body.physics.velocity.setZero()
            return
        }

        val velocity = GameObjectPools.fetch(Vector2::class)
            .set(roomCenter)
            .sub(body.getCenter())
            .nor()
            .scl(HOVER_SPEED * ConstVals.PPM)
        body.physics.velocity.set(velocity)
    }

    private fun fire() {
        GameLogger.debug(TAG, "Shoot fire!")

        val fireball = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.SPIT_FIREBALL)!!
        val spawn = body.getCenter().add(2.15f * ConstVals.PPM * facing.value, ConstVals.PPM.toFloat())
        fireball.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.TRAJECTORY pairTo normalizedTrajectory(
                    spawn, megaman.body.getCenter(), FIRE_SPEED * ConstVals.PPM, GameObjectPools.fetch(Vector2::class)
                )
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

            when (loop.getCurrent()) {
                MechaDragonState.IDLE -> {
                    if (megaman.body.getMaxX() < body.getX()) facing = Facing.LEFT
                    else if (megaman.body.getX() > body.getMaxX()) facing = Facing.RIGHT

                    body.physics.velocity.x = HOVER_X_SWAY_SPEED * facing.value * ConstVals.PPM
                    body.physics.velocity.y = 0f

                    fireTimer.update(delta)
                    if (fireTimer.isFinished()) {
                        when {
                            firesShot < FIRES_TO_SHOOT -> {
                                fireDelayTimer.update(delta)
                                if (fireDelayTimer.isFinished()) {
                                    fire()
                                    firesShot++
                                    fireTimer.reset()
                                    fireDelayTimer.reset()
                                }
                            }

                            else -> {
                                fireDelayTimer.reset()
                                firesShot = 0

                                GameLogger.debug(TAG, "From state: ${loop.getCurrent()}")
                                loop.next()
                                GameLogger.debug(TAG, "To state: ${loop.getCurrent()}")

                                currentTarget.set(when {
                                    loop.getCurrent() == MechaDragonState.HOVER_TO_RANDOM_SPOT -> targets.random()
                                    loop.getCurrent() == MechaDragonState.HOVER_TO_ROOM_CENTER -> roomCenter
                                    else -> megaman.body.getCenter()
                                })
                                currentTarget.y = currentTarget.y.coerceIn(minY, maxY)
                            }
                        }
                    }
                }

                MechaDragonState.HOVER_TO_MEGAMAN, MechaDragonState.HOVER_TO_RANDOM_SPOT, MechaDragonState.HOVER_TO_ROOM_CENTER -> {
                    when {
                        megaman.body.getMaxX() < body.getX() -> facing = Facing.LEFT
                        megaman.body.getX() > body.getMaxX() -> facing = Facing.RIGHT
                    }

                    val trajectory = normalizedTrajectory(
                        body.getCenter(),
                        currentTarget,
                        HOVER_SPEED * ConstVals.PPM,
                        GameObjectPools.fetch(Vector2::class)
                    )
                    body.physics.velocity.set(trajectory)

                    val stopPredicate = when (MechaDragonState.HOVER_TO_MEGAMAN) {
                        loop.getCurrent() -> body.getCenter()
                            .epsilonEquals(currentTarget, HOVER_TO_MEGAMAN_EPSILON * ConstVals.PPM)

                        else -> body.getCenter().epsilonEquals(currentTarget, 0.1f * ConstVals.PPM)
                    }

                    if (stopPredicate) {
                        body.physics.velocity.setZero()

                        GameLogger.debug(TAG, "from state: ${loop.getCurrent()}")
                        loop.next()
                        GameLogger.debug(TAG, "to state: ${loop.getCurrent()}")
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

                        when {
                            megaman.body.getMaxX() < body.getX() -> facing = Facing.LEFT
                            megaman.body.getX() > body.getMaxX() -> facing = Facing.RIGHT
                        }

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

                        GameLogger.debug(TAG, "From state: ${loop.getCurrent()}")
                        loop.next()
                        GameLogger.debug(TAG, "To state: ${loop.getCurrent()}")
                    }
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(2f * ConstVals.PPM, 3f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle(body))
        bodyFixture.addFixtureLabel(FixtureLabel.NO_PROJECTILE_COLLISION)
        body.addFixture(bodyFixture)
        debugShapes.add { bodyFixture }

        val headDamagerFixture = Fixture(body, FixtureType.DAMAGER, GameCircle().setRadius(0.75f * ConstVals.PPM))
        headDamagerFixture.offsetFromBodyAttachment.y = 1.85f * ConstVals.PPM
        body.addFixture(headDamagerFixture)
        debugShapes.add { headDamagerFixture }

        val headDamageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().setSize(1.5f * ConstVals.PPM))
        headDamageableFixture.offsetFromBodyAttachment.y = 1.85f * ConstVals.PPM
        body.addFixture(headDamageableFixture)
        debugShapes.add { headDamageableFixture }

        val neckDamagerFixture = Fixture(body, FixtureType.DAMAGER, GameCircle().setRadius(0.75f * ConstVals.PPM))
        neckDamagerFixture.offsetFromBodyAttachment.y = 1.35f * ConstVals.PPM
        body.addFixture(neckDamagerFixture)
        debugShapes.add { neckDamagerFixture }

        val neckDamageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().setSize(1.5f * ConstVals.PPM))
        neckDamageableFixture.offsetFromBodyAttachment.y = 1.35f * ConstVals.PPM
        body.addFixture(neckDamageableFixture)
        debugShapes.add { neckDamageableFixture }

        val bodyDamagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle(body))
        bodyDamagerFixture.offsetFromBodyAttachment.y = 0.5f * ConstVals.PPM
        body.addFixture(bodyDamagerFixture)
        debugShapes.add { bodyDamagerFixture }

        val bodyDamageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle(body))
        bodyDamageableFixture.offsetFromBodyAttachment.y = 0.5f * ConstVals.PPM
        body.addFixture(bodyDamageableFixture)
        debugShapes.add { bodyDamageableFixture }

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
        debugShapes.add { tailDamageableFixture1 }

        val tailDamagerFixture2 =
            Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(0.5f * ConstVals.PPM, 0.25f * ConstVals.PPM))
        tailDamagerFixture2.offsetFromBodyAttachment.y = -2.5f * ConstVals.PPM
        body.addFixture(tailDamagerFixture2)
        debugShapes.add { tailDamagerFixture2 }

        val tailDamageableFixture2 =
            Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().setSize(0.5f * ConstVals.PPM, 0.5f * ConstVals.PPM))
        tailDamageableFixture2.offsetFromBodyAttachment.y = -2.5f * ConstVals.PPM
        body.addFixture(tailDamageableFixture2)
        debugShapes.add { tailDamageableFixture2 }

        val tailDamagerFixture3 = Fixture(body, FixtureType.DAMAGER, GameCircle().setRadius(0.35f * ConstVals.PPM))
        tailDamagerFixture3.offsetFromBodyAttachment.y = -2.25f * ConstVals.PPM
        body.addFixture(tailDamagerFixture3)
        debugShapes.add { tailDamagerFixture3 }

        val tailDamageableFixture3 =
            Fixture(body, FixtureType.DAMAGEABLE, GameCircle().setRadius(0.35f * ConstVals.PPM))
        tailDamageableFixture3.offsetFromBodyAttachment.y = -2.25f * ConstVals.PPM
        body.addFixture(tailDamageableFixture3)
        debugShapes.add { tailDamageableFixture3 }

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
            bodyFixture1.rawShape.setWithProps(props("width" pairTo width, "height" pairTo height))

            body.addFixture(bodyFixture1)
        }

        body.preProcess.put(ConstKeys.DEFAULT) {
            headDamagerFixture.offsetFromBodyAttachment.x = 1.25f * ConstVals.PPM * facing.value
            headDamageableFixture.offsetFromBodyAttachment.x = 1.25f * ConstVals.PPM * facing.value

            neckDamagerFixture.offsetFromBodyAttachment.x = 0.25f * ConstVals.PPM * -facing.value
            neckDamageableFixture.offsetFromBodyAttachment.x = 0.25f * ConstVals.PPM * -facing.value

            bodyDamagerFixture.offsetFromBodyAttachment.x = 0.65f * ConstVals.PPM * -facing.value
            bodyDamageableFixture.offsetFromBodyAttachment.x = 0.65f * ConstVals.PPM * -facing.value

            tailDamagerFixture1.offsetFromBodyAttachment.x = 0.65f * ConstVals.PPM * -facing.value
            tailDamageableFixture1.offsetFromBodyAttachment.x = 0.65f * ConstVals.PPM * -facing.value

            tailDamagerFixture2.offsetFromBodyAttachment.x = 1.75f * ConstVals.PPM * -facing.value
            tailDamageableFixture2.offsetFromBodyAttachment.x = 1.75f * ConstVals.PPM * -facing.value

            tailDamagerFixture3.offsetFromBodyAttachment.x = 1.15f * ConstVals.PPM * -facing.value
            tailDamageableFixture3.offsetFromBodyAttachment.x = 1.15f * ConstVals.PPM * -facing.value
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 0))
        sprite.setSize(9f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.setCenter(body.getCenter())
            sprite.setFlip(isFacing(Facing.LEFT), false)
            sprite.hidden = damageBlink || !ready
            sprite.setAlpha(if (defeated) 1f - defeatTimer.getRatio() else 1f)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = {
            when {
                defeated -> "defeated"
                else -> {
                    val prefix = if (shooting) "shoot" else "fly"
                    val suffix = when (loop.getCurrent()) {
                        MechaDragonState.IDLE -> "slow"
                        MechaDragonState.HOVER_TO_MEGAMAN, MechaDragonState.HOVER_TO_ROOM_CENTER, MechaDragonState.HOVER_TO_RANDOM_SPOT -> "medium"

                        MechaDragonState.CHARGE -> "fast"
                    }
                    "${prefix}_${suffix}"
                }
            }
        }

        val flyRegion = regions.get("fly")
        val shootRegion = regions.get("shoot")
        val animations = objectMapOf<String, IAnimation>(
            "defeated" pairTo Animation(regions.get("defeated"), 2, 2, 0.1f, true),
            "fly_slow" pairTo Animation(flyRegion, 1, 2, 0.15f, true),
            "fly_medium" pairTo Animation(flyRegion, 1, 2, 0.1f, true),
            "fly_fast" pairTo Animation(flyRegion, 1, 2, 0.05f, true),
            "shoot_slow" pairTo Animation(shootRegion, 1, 2, 0.15f, true),
            "shoot_medium" pairTo Animation(shootRegion, 1, 2, 0.1f, true),
            "shoot_fast" pairTo Animation(shootRegion, 1, 2, 0.05f, true)
        )

        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
