package com.megaman.maverick.game.entities.bosses

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
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IFaceable

import com.mega.game.engine.common.normalizedTrajectory
import com.mega.game.engine.common.objects.Loop
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.getCenter
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
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureLabel
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.addFixtureLabel
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KClass

class MechaDragonMiniBoss(game: MegamanMaverickGame) : AbstractBoss(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "MechaDragonMiniBoss"

        private const val TARGETS_COUNT = 4

        private const val HOVER_SPEED = 3f
        private const val HOVER_X_SWAY_SPEED = 0.5f

        private const val FIRE_DUR = 0.25f
        private const val FIRE_DELAY = 1.5f
        private const val FIRES_TO_SHOOT = 2
        private const val FIRE_SPEED = 6f

        private const val CHARGE_SPEED = 6f
        private const val CHARGE_FIRST_DELAY_SPEED = 4f
        private const val CHARGE_FIRST_DELAY = 0.75f
        private const val CHARGE_SECOND_DELAY = 0.5f

        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class MechaDragonState {
        IDLE, HOVER_TO_MEGAMAN, HOVER_TO_RANDOM_SPOT, HOVER_TO_ROOM_CENTER, CHARGE
    }

    override val damageNegotiations =
        objectMapOf<KClass<out IDamager>, DamageNegotiation>(
            Bullet::class to dmgNeg(1),
            ChargedShot::class to dmgNeg {
                it as ChargedShot
                if (it.fullyCharged) 2 else 1
            }, ChargedShotExplosion::class to dmgNeg {
                it as ChargedShotExplosion
                if (it.fullyCharged) 2 else 1
            }
        )
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
    private lateinit var currentTarget: Vector2
    private lateinit var returnSpot: Vector2
    private lateinit var roomCenter: Vector2

    private var maxX = 0f
    private var minX = 0f
    private var maxY = 0f

    private var firesShot = 0

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES.source)
            regions.put("fly", atlas.findRegion("$TAG/Fly"))
            regions.put("shoot", atlas.findRegion("$TAG/Shoot"))
            regions.put("defeated", atlas.findRegion("$TAG/Defeated"))
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.ENTTIY_KILLED_BY_DEATH_FIXTURE, false)
        super.onSpawn(spawnProps)

        facing = if (getMegaman().body.x < body.x) Facing.LEFT else Facing.RIGHT

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setBottomCenterToPoint(spawn)

        for (i in 1..TARGETS_COUNT) {
            val key = "${ConstKeys.SPOT}_$i"
            val targetObject = spawnProps.get(key, RectangleMapObject::class)!!
            val spot = targetObject.rectangle.getCenter()
            if (targetObject.properties.get(ConstKeys.START, false, Boolean::class.java)) currentTarget = spot
            targets.add(spot)
            maxY = max(maxY, spot.y)
        }

        roomCenter = spawnProps.get(ConstKeys.CENTER, RectangleMapObject::class)!!.rectangle.getCenter()

        maxX = spawnProps.get("${ConstKeys.MAX}_${ConstKeys.X}", RectangleMapObject::class)!!.rectangle.getCenter().x
        minX = spawnProps.get("${ConstKeys.MIN}_${ConstKeys.X}", RectangleMapObject::class)!!.rectangle.getCenter().x

        returnSpot =
            spawnProps.get("${ConstKeys.RETURN}_${ConstKeys.SPOT}", RectangleMapObject::class)!!.rectangle.getCenter()

        firesShot = 0

        fireTimer.setToEnd()
        fireDelayTimer.reset()
        chargeFirstDelayTimer.reset()
        chargeSecondDelayTimer.reset()

        loop.reset()
    }

    override fun onDefeated(delta: Float) {
        super.onDefeated(delta)
        if (body.getCenter().epsilonEquals(roomCenter, 0.1f * ConstVals.PPM)) {
            body.physics.velocity.setZero()
            return
        }
        val velocity = roomCenter.cpy().sub(body.getCenter()).nor().scl(HOVER_SPEED * ConstVals.PPM)
        body.physics.velocity = velocity
    }

    private fun fire() {
        GameLogger.debug(TAG, "Shoot fire!")

        val fireball = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.SPIT_FIREBALL)!!
        val spawn = body.getCenter().add(2.15f * ConstVals.PPM * facing.value, 1.25f * ConstVals.PPM)
        fireball.spawn(
            props(
                ConstKeys.OWNER to this, ConstKeys.POSITION to spawn, ConstKeys.TRAJECTORY to normalizedTrajectory(
                    spawn, getMegaman().body.getCenter(), FIRE_SPEED * ConstVals.PPM
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
                    if (getMegaman().body.getMaxX() < body.x) facing = Facing.LEFT
                    else if (getMegaman().body.x > body.getMaxX()) facing = Facing.RIGHT

                    body.physics.velocity.x = HOVER_X_SWAY_SPEED * facing.value * ConstVals.PPM
                    body.physics.velocity.y = 0f

                    if (firesShot < FIRES_TO_SHOOT) {
                        if (!fireTimer.isFinished()) fireTimer.update(delta)
                        else {
                            fireDelayTimer.update(delta)
                            if (fireDelayTimer.isFinished()) {
                                fire()
                                firesShot++

                                fireTimer.reset()
                                fireDelayTimer.reset()
                            }
                        }
                    } else {
                        fireTimer.setToEnd()
                        fireDelayTimer.reset()

                        firesShot = 0

                        GameLogger.debug(TAG, "From state: ${loop.getCurrent()}")
                        loop.next()
                        GameLogger.debug(TAG, "To state: ${loop.getCurrent()}")

                        currentTarget = if (loop.getCurrent() == MechaDragonState.HOVER_TO_RANDOM_SPOT) targets.random()
                        else if (loop.getCurrent() == MechaDragonState.HOVER_TO_ROOM_CENTER) roomCenter
                        else getMegaman().body.getCenter()
                        currentTarget.y = min(currentTarget.y, maxY)
                    }
                }

                MechaDragonState.HOVER_TO_MEGAMAN,
                MechaDragonState.HOVER_TO_RANDOM_SPOT,
                MechaDragonState.HOVER_TO_ROOM_CENTER -> {
                    if (currentTarget.x < body.getX()) facing = Facing.LEFT
                    else if (currentTarget.x > body.getMaxX()) facing = Facing.RIGHT

                    val trajectory = normalizedTrajectory(body.getCenter(), currentTarget, HOVER_SPEED * ConstVals.PPM)
                    body.physics.velocity.set(trajectory)

                    val stopPredicate = if (loop.getCurrent() == MechaDragonState.HOVER_TO_MEGAMAN)
                        body.getCenter().epsilonEquals(currentTarget, 2f * ConstVals.PPM)
                    else body.getCenter().epsilonEquals(currentTarget, 0.1f * ConstVals.PPM)

                    if (stopPredicate) {
                        body.physics.velocity.setZero()

                        GameLogger.debug(TAG, "From state: ${loop.getCurrent()}")
                        loop.next()
                        GameLogger.debug(TAG, "To state: ${loop.getCurrent()}")
                    }
                }

                MechaDragonState.CHARGE -> {
                    if (!chargeFirstDelayTimer.isFinished()) {
                        body.physics.velocity.x = 0f
                        val megamanCenterY = getMegaman().body.getCenter().y
                        body.physics.velocity.y = (if (megamanCenterY > body.y && megamanCenterY < body.getMaxY()) 0f
                        else CHARGE_FIRST_DELAY_SPEED * if (megamanCenterY > body.getMaxY()) 1f else -1f) * ConstVals.PPM

                        if (getMegaman().body.getMaxX() < body.x) facing = Facing.LEFT
                        else if (getMegaman().body.x > body.getMaxX()) facing = Facing.RIGHT

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

                    if (body.x > maxX || body.getMaxX() < minX) {
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
        body.setSize(3f * ConstVals.PPM, 4f * ConstVals.PPM)
        body.color = Color.BROWN

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBodyBounds() }

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle(body))
        bodyFixture.addFixtureLabel(FixtureLabel.NO_PROJECTILE_COLLISION)
        body.addFixture(bodyFixture)
        bodyFixture.rawShape.color = Color.GRAY
        debugShapes.add { bodyFixture.getShape() }

        val headDamagerFixture = Fixture(body, FixtureType.DAMAGER, GameCircle().setRadius(0.5f * ConstVals.PPM))
        headDamagerFixture.offsetFromBodyCenter.y = 1.75f * ConstVals.PPM
        body.addFixture(headDamagerFixture)
        headDamagerFixture.rawShape.color = Color.RED
        debugShapes.add { headDamagerFixture.getShape() }

        val headDamageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameCircle().setRadius(0.75f * ConstVals.PPM))
        headDamageableFixture.offsetFromBodyCenter.y = 1.75f * ConstVals.PPM
        body.addFixture(headDamageableFixture)
        headDamageableFixture.rawShape.color = Color.PURPLE
        debugShapes.add { headDamageableFixture.getShape() }

        val neckDamagerFixture = Fixture(body, FixtureType.DAMAGER, GameCircle().setRadius(0.5f * ConstVals.PPM))
        neckDamagerFixture.offsetFromBodyCenter.y = 1.5f * ConstVals.PPM
        body.addFixture(neckDamagerFixture)
        neckDamagerFixture.rawShape.color = Color.RED
        debugShapes.add { neckDamagerFixture.getShape() }

        val neckDamageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameCircle().setRadius(0.75f * ConstVals.PPM))
        neckDamageableFixture.offsetFromBodyCenter.y = 1.5f * ConstVals.PPM
        body.addFixture(neckDamageableFixture)
        neckDamageableFixture.rawShape.color = Color.PURPLE
        debugShapes.add { neckDamageableFixture.getShape() }

        val bodyDamagerFixture = Fixture(
            body, FixtureType.DAMAGER, GameRectangle().setSize(1.75f * ConstVals.PPM)
        )
        bodyDamagerFixture.offsetFromBodyCenter.y = 0.25f * ConstVals.PPM
        body.addFixture(bodyDamagerFixture)
        bodyDamagerFixture.rawShape.color = Color.RED
        debugShapes.add { bodyDamagerFixture.getShape() }

        val bodyDamageableFixture = Fixture(
            body, FixtureType.DAMAGEABLE, GameRectangle().setSize(2f * ConstVals.PPM)
        )
        bodyDamageableFixture.offsetFromBodyCenter.y = 0.25f * ConstVals.PPM
        body.addFixture(bodyDamageableFixture)
        bodyDamageableFixture.rawShape.color = Color.PURPLE
        debugShapes.add { bodyDamageableFixture.getShape() }

        val tailDamagerFixture = Fixture(
            body, FixtureType.DAMAGER, GameRectangle().setSize(0.5f * ConstVals.PPM, 1f * ConstVals.PPM)
        )
        tailDamagerFixture.offsetFromBodyCenter.y = -1.5f * ConstVals.PPM
        body.addFixture(tailDamagerFixture)
        tailDamagerFixture.rawShape.color = Color.RED
        debugShapes.add { tailDamagerFixture.getShape() }

        val tailDamageableFixture = Fixture(
            body, FixtureType.DAMAGEABLE, GameRectangle().setSize(0.75f * ConstVals.PPM, 1.15f * ConstVals.PPM)
        )
        tailDamageableFixture.offsetFromBodyCenter.y = -1.5f * ConstVals.PPM
        body.addFixture(tailDamageableFixture)
        tailDamageableFixture.rawShape.color = Color.PURPLE
        debugShapes.add { tailDamageableFixture.getShape() }

        body.preProcess.put(ConstKeys.DEFAULT) {
            headDamagerFixture.offsetFromBodyCenter.x = ConstVals.PPM.toFloat() * facing.value
            headDamageableFixture.offsetFromBodyCenter.x = ConstVals.PPM.toFloat() * facing.value

            neckDamagerFixture.offsetFromBodyCenter.x = 0.5f * ConstVals.PPM * -facing.value
            neckDamageableFixture.offsetFromBodyCenter.x = 0.5f * ConstVals.PPM * -facing.value

            bodyDamagerFixture.offsetFromBodyCenter.x = 0.65f * ConstVals.PPM * -facing.value
            bodyDamageableFixture.offsetFromBodyCenter.x = 0.65f * ConstVals.PPM * -facing.value

            tailDamagerFixture.offsetFromBodyCenter.x = 1.25f * ConstVals.PPM * -facing.value
            tailDamageableFixture.offsetFromBodyCenter.x = 1.25f * ConstVals.PPM * -facing.value
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 0))
        sprite.setSize(7f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setCenter(body.getCenter())
            _sprite.setFlip(isFacing(Facing.LEFT), false)
            _sprite.hidden = damageBlink || !ready
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = {
            if (defeated) "defeated"
            else {
                val prefix = if (shooting) "shoot" else "fly"
                val suffix = when (loop.getCurrent()) {
                    MechaDragonState.IDLE -> "slow"
                    MechaDragonState.HOVER_TO_MEGAMAN,
                    MechaDragonState.HOVER_TO_ROOM_CENTER,
                    MechaDragonState.HOVER_TO_RANDOM_SPOT -> "medium"

                    MechaDragonState.CHARGE -> "fast"
                }
                "${prefix}_${suffix}"
            }
        }

        val flyRegion = regions.get("fly")
        val shootRegion = regions.get("shoot")
        val animations = objectMapOf<String, IAnimation>(
            "defeated" to Animation(regions.get("defeated"), 1, 2, 0.1f, true),
            "fly_slow" to Animation(flyRegion, 1, 2, 0.15f, true),
            "fly_medium" to Animation(flyRegion, 1, 2, 0.1f, true),
            "fly_fast" to Animation(flyRegion, 1, 2, 0.05f, true),
            "shoot_slow" to Animation(shootRegion, 1, 2, 0.15f, true),
            "shoot_medium" to Animation(shootRegion, 1, 2, 0.1f, true),
            "shoot_fast" to Animation(shootRegion, 1, 2, 0.05f, true)
        )

        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}