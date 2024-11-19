package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.toGdxArray
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Loop
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.TimeMarkedRunnable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
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
import com.megaman.maverick.game.entities.contracts.IDirectionRotatable
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.projectiles.Asteroid
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import kotlin.reflect.KClass

class PopupCanon(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IFaceable, IDirectionRotatable {

    companion object {
        const val TAG = "PopupCanon"
        private const val SHOOT_X = 8f
        private const val SHOOT_Y = 2.5f
        private const val REST_DUR = 0.75f
        private const val TRANS_DUR = 0.6f
        private const val SHOOT_DUR = 0.25f
        private const val BALL_GRAVITY = 0.15f
        private const val TRANS_DAMAGEABLE_CUTOFF = 0.5f
        private const val DEFAULT_BALL_GRAVITY_SCALAR = 1f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class PopupCanonState { REST, RISE, SHOOT, FALL }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class pairTo dmgNeg(10),
        Fireball::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShot::class pairTo dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) ConstVals.MAX_HEALTH else 15
        },
        ChargedShotExplosion::class pairTo dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) 10 else 5
        },
        Asteroid::class pairTo dmgNeg(15)
    )
    override var directionRotation = Direction.UP
    override lateinit var facing: Facing

    private val canMove: Boolean
        get() = !game.isCameraRotating()
    private val loop = Loop(PopupCanonState.entries.toTypedArray().toGdxArray())
    private val timers = objectMapOf(
        "rest" pairTo Timer(REST_DUR),
        "rise" pairTo Timer(TRANS_DUR, gdxArrayOf(
            TimeMarkedRunnable(0f) { transState = Size.SMALL },
            TimeMarkedRunnable(0.25f) { transState = Size.MEDIUM },
            TimeMarkedRunnable(0.5f) { transState = Size.LARGE }
        )),
        "fall" pairTo Timer(TRANS_DUR, gdxArrayOf(
            TimeMarkedRunnable(0f) { transState = Size.LARGE },
            TimeMarkedRunnable(0.25f) { transState = Size.MEDIUM },
            TimeMarkedRunnable(0.5f) { transState = Size.SMALL }
        )),
        "shoot" pairTo Timer(SHOOT_DUR, gdxArrayOf(TimeMarkedRunnable(0.25f) { shoot() }))
    )
    private var ballGravityScalar = DEFAULT_BALL_GRAVITY_SCALAR
    private lateinit var transState: Size

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            regions.put("rest", atlas.findRegion("$TAG/Down"))
            regions.put("trans", atlas.findRegion("$TAG/Rise"))
            regions.put("shoot", atlas.findRegion("$TAG/Up"))
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        loop.reset()
        timers.values().forEach { it.reset() }

        ballGravityScalar = spawnProps.getOrDefault(
            "${ConstKeys.GRAVITY}_${ConstKeys.SCALAR}",
            DEFAULT_BALL_GRAVITY_SCALAR,
            Float::class
        )

        directionRotation =
            Direction.valueOf(spawnProps.getOrDefault(ConstKeys.DIRECTION, "up", String::class).uppercase())

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        when (directionRotation) {
            Direction.UP -> body.setBottomCenterToPoint(bounds.getBottomCenterPoint())
            Direction.DOWN -> body.setTopCenterToPoint(bounds.getTopCenterPoint())
            Direction.LEFT -> body.setCenterRightToPoint(bounds.getCenterRightPoint())
            Direction.RIGHT -> body.setCenterLeftToPoint(bounds.getCenterLeftPoint())
        }

        facing = when (directionRotation) {
            Direction.UP -> if (getMegaman().body.x < body.x) Facing.LEFT else Facing.RIGHT
            Direction.DOWN -> if (getMegaman().body.x < body.x) Facing.RIGHT else Facing.LEFT
            Direction.LEFT -> if (getMegaman().body.y < body.y) Facing.LEFT else Facing.RIGHT
            Direction.RIGHT -> if (getMegaman().body.x < body.x) Facing.RIGHT else Facing.LEFT
        }
        transState = Size.SMALL
    }

    override fun onDestroy() {
        super.onDestroy()
        if (hasDepletedHealth()) explode()
    }

    private fun shoot() {
        val explodingBall = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.EXPLODING_BALL)!!

        val offset = when (directionRotation) {
            Direction.UP -> Vector2(0.25f * facing.value, 0.125f)
            Direction.DOWN -> Vector2(0.25f * facing.value, -0.125f)
            Direction.LEFT -> Vector2(-0.125f, 0.25f * facing.value)
            Direction.RIGHT -> Vector2(0.125f, 0.25f * facing.value)
        }.scl(ConstVals.PPM.toFloat())

        val spawn = body.getCenter().add(offset)

        val impulse = when (directionRotation) {
            Direction.UP -> Vector2(SHOOT_X * facing.value, SHOOT_Y)
            Direction.DOWN -> Vector2(SHOOT_X * facing.value, -SHOOT_Y)
            Direction.LEFT -> Vector2(-SHOOT_Y, SHOOT_X * facing.value)
            Direction.RIGHT -> Vector2(SHOOT_Y, SHOOT_X * facing.value)
        }.scl(ConstVals.PPM.toFloat())

        val gravity = when (directionRotation) {
            Direction.UP -> Vector2(0f, -BALL_GRAVITY)
            Direction.DOWN -> Vector2(0f, BALL_GRAVITY)
            Direction.LEFT -> Vector2(BALL_GRAVITY, 0f)
            Direction.RIGHT -> Vector2(-BALL_GRAVITY, 0f)
        }.scl(ballGravityScalar * ConstVals.PPM.toFloat())

        explodingBall.spawn(
            props(
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.IMPULSE pairTo impulse,
                ConstKeys.GRAVITY pairTo gravity
            )
        )

        requestToPlaySound(SoundAsset.CHILL_SHOOT_SOUND, false)
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (!canMove) return@add
            facing = if (getMegaman().body.x < body.x) Facing.LEFT else Facing.RIGHT
            val timerKey = when (loop.getCurrent()) {
                PopupCanonState.REST -> "rest"
                PopupCanonState.RISE -> "rise"
                PopupCanonState.FALL -> "fall"
                PopupCanonState.SHOOT -> "shoot"
            }
            val timer = timers.get(timerKey)
            timer.update(delta)
            if (timer.isFinished()) {
                timer.reset()
                loop.next()
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(1.15f * ConstVals.PPM, 1.5f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBodyBounds() }

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle(body))
        body.addFixture(bodyFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().setWidth(1.15f * ConstVals.PPM))
        body.addFixture(damagerFixture)
        damagerFixture.rawShape.color = Color.RED
        debugShapes.add { damagerFixture.getShape() }

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle(body))
        body.addFixture(damageableFixture)
        damageableFixture.rawShape.color = Color.GREEN
        debugShapes.add { if (damageableFixture.active) damageableFixture.getShape() else null }

        body.preProcess.put(ConstKeys.DEFAULT) {
            val damageable = loop.getCurrent() == PopupCanonState.SHOOT ||
                    (loop.getCurrent() == PopupCanonState.RISE && timers["rise"].getRatio() > TRANS_DAMAGEABLE_CUTOFF) ||
                    (loop.getCurrent() == PopupCanonState.FALL && timers["fall"].getRatio() < TRANS_DAMAGEABLE_CUTOFF)
            damageableFixture.active = damageable

            (damagerFixture.rawShape as GameRectangle).height = (when (transState) {
                Size.LARGE -> 1.5f
                Size.MEDIUM -> 1f
                Size.SMALL -> 0.25f
            }) * ConstVals.PPM
            damagerFixture.offsetFromBodyCenter.y = (when (transState) {
                Size.LARGE -> 0f
                Size.MEDIUM -> -0.25f
                Size.SMALL -> -0.525f
            }) * ConstVals.PPM
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.75f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setOriginCenter()
            _sprite.rotation = directionRotation.rotation
            val position = DirectionPositionMapper.getPosition(directionRotation).opposite()
            _sprite.setPosition(body.getPositionPoint(position), position)
            _sprite.hidden = damageBlink
            when (directionRotation) {
                Direction.UP -> _sprite.setFlip(isFacing(Facing.RIGHT), false)
                Direction.DOWN -> _sprite.setFlip(isFacing(Facing.LEFT), false)
                Direction.LEFT -> _sprite.setFlip(false, isFacing(Facing.RIGHT))
                Direction.RIGHT -> _sprite.setFlip(false, isFacing(Facing.LEFT))
            }
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier = {
            when (loop.getCurrent()) {
                PopupCanonState.REST -> "rest"
                PopupCanonState.RISE -> "rise"
                PopupCanonState.SHOOT -> "shoot"
                PopupCanonState.FALL -> "fall"
            }
        }
        val animations = objectMapOf<String, IAnimation>(
            "rest" pairTo Animation(regions.get("rest")),
            "rise" pairTo Animation(regions.get("trans"), 2, 3, 0.1f, false),
            "shoot" pairTo Animation(regions.get("shoot")),
            "fall" pairTo Animation(regions.get("trans"), 2, 3, 0.1f, false).reversed()
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
