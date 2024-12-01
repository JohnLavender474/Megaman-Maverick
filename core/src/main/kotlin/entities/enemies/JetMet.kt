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
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
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
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.*
import java.util.*
import kotlin.reflect.KClass

class JetMet(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IDirectional, IFaceable {

    companion object {
        const val TAG = "JetMet"
        private const val STAND_DUR = 0.15f
        private const val LIFTOFF_DUR = 0.1f
        private const val SHOOT_DELAY = 1f
        private const val JET_SPEED = 4f
        private const val BULLET_SPEED = 10f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class JetMetState { STAND, LIFT_OFF, JET }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class pairTo dmgNeg(15),
        Fireball::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShot::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShotExplosion::class pairTo dmgNeg(ConstVals.MAX_HEALTH)
    )
    override var direction = Direction.UP
    override lateinit var facing: Facing

    private val standTimer = Timer(STAND_DUR)
    private val liftoffTimer = Timer(LIFTOFF_DUR)
    private val shootTimer = Timer(SHOOT_DELAY)

    private val canMove: Boolean
        get() = !game.isCameraRotating()

    private lateinit var animations: ObjectMap<String, IAnimation>
    private lateinit var jetMetState: JetMetState

    private val target = Vector2()
    val targetPQ = PriorityQueue { o1: Vector2, o2: Vector2 ->
        val d1 = o1.dst2(megaman().body.getCenter())
        val d2 = o2.dst2(megaman().body.getCenter())
        d1.compareTo(d2)
    }

    private var applyMovementScalarToBullet = false
    private var targetReached = false

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            regions.put("stand", atlas.findRegion("$TAG/Stand"))
            regions.put("take_off", atlas.findRegion("$TAG/TakeOff"))
            regions.put("jet", atlas.findRegion("$TAG/Jet"))
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        spawnProps.forEach { key, value ->
            if (key.toString().contains(ConstKeys.TARGET) && value is RectangleMapObject)
                targetPQ.add(value.rectangle.getCenter())
        }
        target.set(targetPQ.poll())
        targetPQ.clear()

        applyMovementScalarToBullet = spawnProps.getOrDefault(ConstKeys.APPLY_SCALAR_TO_CHILDREN, false, Boolean::class)
        direction = megaman().direction

        standTimer.reset()
        liftoffTimer.reset()
        shootTimer.reset()

        jetMetState = JetMetState.STAND
        facing = when (direction) {
            Direction.UP -> if (megaman().body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT
            Direction.DOWN -> if (megaman().body.getX() > body.getX()) Facing.LEFT else Facing.RIGHT
            Direction.LEFT -> if (megaman().body.getY() < body.getY()) Facing.LEFT else Facing.RIGHT
            Direction.RIGHT -> if (megaman().body.getY() > body.getY()) Facing.LEFT else Facing.RIGHT
        }

        targetReached = false

        val animDuration = spawnProps.getOrDefault("${ConstKeys.ANIMATION}_${ConstKeys.DURATION}", 0.1f, Float::class)
        animations.get("jet")!!.setFrameDuration(animDuration)
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            direction = megaman().direction

            if (!canMove) {
                body.physics.velocity.setZero()
                return@add
            }

            when (jetMetState) {
                JetMetState.STAND -> {
                    standTimer.update(delta)
                    if (standTimer.isFinished()) jetMetState = JetMetState.LIFT_OFF
                }

                JetMetState.LIFT_OFF -> {
                    liftoffTimer.update(delta)
                    if (liftoffTimer.isFinished()) jetMetState = JetMetState.JET
                }

                JetMetState.JET -> {
                    facing = when (megaman().direction) {
                        Direction.UP -> if (megaman().body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT
                        Direction.DOWN -> if (megaman().body.getX() > body.getX()) Facing.LEFT else Facing.RIGHT
                        Direction.LEFT -> if (megaman().body.getY() < body.getY()) Facing.LEFT else Facing.RIGHT
                        Direction.RIGHT -> if (megaman().body.getY() > body.getY()) Facing.LEFT else Facing.RIGHT
                    }

                    if (!targetReached) {
                        val velocity = GameObjectPools.fetch(Vector2::class)
                            .set(target)
                            .sub(body.getCenter())
                            .nor()
                            .scl(JET_SPEED * ConstVals.PPM * movementScalar)
                        body.physics.velocity.set(velocity)

                        targetReached = body.getCenter().epsilonEquals(target, 0.1f * ConstVals.PPM)
                    } else body.physics.velocity.setZero()

                    shootTimer.update(delta)
                    if (shootTimer.isFinished()) {
                        shoot()
                        shootTimer.reset()
                    }
                }
            }
        }
    }

    private fun shoot() {
        val bullet = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.BULLET)!!

        val trajectory = GameObjectPools.fetch(Vector2::class)
            .set(megaman().body.getCenter())
            .sub(body.getCenter())
            .nor()
            .scl(BULLET_SPEED * ConstVals.PPM)

        val offset = ConstVals.PPM / 64f
        val spawn = GameObjectPools.fetch(Vector2::class)
            .set(body.getCenter())
            .add(offset * facing.value, if (direction == Direction.DOWN) -offset else offset)

        val props = props(
            ConstKeys.OWNER pairTo this,
            ConstKeys.POSITION pairTo spawn,
            ConstKeys.TRAJECTORY pairTo trajectory,
            ConstKeys.DIRECTION pairTo direction
        )
        if (applyMovementScalarToBullet) props.put("${ConstKeys.MOVEMENT}_${ConstKeys.SCALAR}", movementScalar)
        bullet.spawn(props)

        requestToPlaySound(SoundAsset.ENEMY_BULLET_SOUND, false)
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.85f * ConstVals.PPM)

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle(body))
        bodyFixture.putProperty(ConstKeys.GRAVITY_ROTATABLE, false)
        body.addFixture(bodyFixture)

        val debugShapes = Array<() -> IDrawableShape?>()
        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this, body, BodyFixtureDef.of(FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 4))
        sprite.setSize(1.5f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.hidden = damageBlink
            sprite.setFlip(isFacing(Facing.RIGHT), false)
            sprite.setOriginCenter()
            sprite.rotation = direction.rotation
            val position = when (direction) {
                Direction.UP -> Position.BOTTOM_CENTER
                Direction.DOWN -> Position.TOP_CENTER
                Direction.LEFT -> Position.CENTER_RIGHT
                Direction.RIGHT -> Position.CENTER_LEFT
            }
            val bodyPosition = body.getPositionPoint(position)
            sprite.setPosition(bodyPosition, position)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = {
            if (!body.isSensing(BodySense.FEET_ON_GROUND)) "jet"
            else when (jetMetState) {
                JetMetState.STAND -> "stand"
                JetMetState.LIFT_OFF -> "take_off"
                JetMetState.JET -> "jet"
            }
        }
        animations = objectMapOf(
            "stand" pairTo Animation(regions.get("stand")),
            "take_off" pairTo Animation(regions.get("take_off")),
            "jet" pairTo Animation(regions.get("jet"), 2, 2, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
