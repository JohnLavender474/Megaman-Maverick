package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.enums.Direction
import com.engine.common.enums.Facing
import com.engine.common.enums.Position
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.interfaces.IFaceable
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.shapes.getCenter
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
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
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
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import java.util.*
import kotlin.reflect.KClass

class JetMet(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IDirectionRotatable, IFaceable {

    companion object {
        const val TAG = "JetMet"
        private const val STAND_DUR = 0.15f
        private const val LIFTOFF_DUR = 0.1f
        private const val SHOOT_DELAY = 0.85f
        private const val JET_SPEED = 4f
        private const val BULLET_SPEED = 15f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class JetMetState {
        STAND, LIFT_OFF, JET
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class to dmgNeg(15),
        Fireball::class to dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShot::class to dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShotExplosion::class to dmgNeg(ConstVals.MAX_HEALTH)
    )

    override var directionRotation: Direction
        get() = body.cardinalRotation
        set(value) {
            body.cardinalRotation = value
        }

    override lateinit var facing: Facing

    private val standTimer = Timer(STAND_DUR)
    private val liftoffTimer = Timer(LIFTOFF_DUR)
    private val shootTimer = Timer(SHOOT_DELAY)

    private lateinit var animations: ObjectMap<String, IAnimation>
    private lateinit var state: JetMetState
    private lateinit var liftTarget: Vector2

    private var applyMovementScalarToBullet = false
    private var targetReached = false

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            regions.put("stand", atlas.findRegion("JetMet/Stand"))
            regions.put("take_off", atlas.findRegion("JetMet/TakeOff"))
            regions.put("jet", atlas.findRegion("JetMet/Jet"))
        }
        super<AbstractEnemy>.init()
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        val targets = PriorityQueue { o1: Vector2, o2: Vector2 ->
            val d1 = o1.dst2(megaman.body.getCenter())
            val d2 = o2.dst2(megaman.body.getCenter())
            d1.compareTo(d2)
        }
        spawnProps.forEach { key, value ->
            if (key.toString().contains(ConstKeys.TARGET) && value is RectangleMapObject)
                targets.add(value.rectangle.getCenter())
        }
        liftTarget = targets.poll()

        applyMovementScalarToBullet = spawnProps.getOrDefault(ConstKeys.APPLY_SCALAR_TO_CHILDREN, false, Boolean::class)

        directionRotation = Direction.valueOf(
            spawnProps.getOrDefault(ConstKeys.DIRECTION, "up", String::class).uppercase()
        )

        standTimer.reset()
        liftoffTimer.reset()
        shootTimer.reset()

        state = JetMetState.STAND
        facing = if (megaman.body.x < body.x) Facing.LEFT else Facing.RIGHT

        targetReached = false

        val animDuration = spawnProps.getOrDefault("${ConstKeys.ANIMATION}_${ConstKeys.DURATION}", 0.1f, Float::class)
        animations.get("jet")!!.setFrameDuration(animDuration)
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            when (state) {
                JetMetState.STAND -> {
                    standTimer.update(delta)
                    if (standTimer.isFinished()) state = JetMetState.LIFT_OFF
                }

                JetMetState.LIFT_OFF -> {
                    liftoffTimer.update(delta)
                    if (liftoffTimer.isFinished()) state = JetMetState.JET
                }

                JetMetState.JET -> {
                    facing = if (megaman.body.x < body.x) Facing.LEFT else Facing.RIGHT

                    if (!targetReached) {
                        val direction = liftTarget.cpy().sub(body.getCenter()).nor()
                        body.physics.velocity.set(direction.scl(JET_SPEED * ConstVals.PPM * movementScalar))
                        targetReached = body.getCenter().epsilonEquals(liftTarget, 0.1f * ConstVals.PPM)
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
        val bulletTrajectory = megaman.body.getCenter().sub(body.getCenter()).nor().scl(BULLET_SPEED * ConstVals.PPM)

        val offset = ConstVals.PPM / 64f
        val spawn = body.getCenter().add(offset * facing.value, if (isDirectionRotatedDown()) -offset else offset)

        val bulletProps = props(
            ConstKeys.POSITION to spawn,
            ConstKeys.TRAJECTORY to bulletTrajectory,
            ConstKeys.OWNER to this
        )
        if (applyMovementScalarToBullet) bulletProps.put("${ConstKeys.MOVEMENT}_${ConstKeys.SCALAR}", movementScalar)

        game.engine.spawn(bullet, bulletProps)

        requestToPlaySound(SoundAsset.ENEMY_BULLET_SOUND, false)
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.85f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle(body))
        body.addFixture(bodyFixture)
        bodyFixture.rawShape.color = Color.GRAY
        debugShapes.add { bodyFixture.getShape() }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle(body))
        body.addFixture(damagerFixture)
        damagerFixture.rawShape.color = Color.RED
        debugShapes.add { damagerFixture.getShape() }

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle(body))
        body.addFixture(damageableFixture)
        damageableFixture.rawShape.color = Color.PURPLE
        debugShapes.add { damageableFixture.getShape() }

        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 4))
        sprite.setSize(1.5f * ConstVals.PPM)

        val spritesComponent = SpritesComponent(this, sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.hidden = damageBlink

            val flipX = facing == Facing.RIGHT
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
        val keySupplier: () -> String? = {
            when (state) {
                JetMetState.STAND -> "stand"
                JetMetState.LIFT_OFF -> "take_off"
                JetMetState.JET -> "jet"
            }
        }
        animations = objectMapOf(
            "stand" to Animation(regions.get("stand")),
            "take_off" to Animation(regions.get("take_off")),
            "jet" to Animation(regions.get("jet"), 2, 2, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}