package com.megaman.maverick.game.entities.enemies

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
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.toGdxArray
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Loop
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
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.projectiles.ExplodingBall
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.world.body.*

class PopupCanon(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.MEDIUM), IAnimatedEntity, IFaceable,
    IDirectional {

    companion object {
        const val TAG = "PopupCanon"

        private const val SHOOT_X = 8f
        private const val SHOOT_Y = 2.5f

        private const val REST_DUR = 0.75f

        private const val TRANS_DUR = 0.6f

        private const val SHOOT_DUR = 0.25f
        private const val SHOOT_OFFSET_X = 0.25f
        private const val SHOOT_OFFSET_Y = 0.25f

        private const val BALL_GRAVITY = 0.15f
        private const val DEFAULT_BALL_GRAVITY_SCALAR = 1f

        private const val TRANS_DAMAGEABLE_CUTOFF = 0.5f

        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class PopupCanonState { REST, RISE, SHOOT, FALL }

    override lateinit var direction: Direction
    override lateinit var facing: Facing

    private val canMove: Boolean
        get() = !game.isCameraRotating()
    private val loop = Loop(PopupCanonState.entries.toTypedArray().toGdxArray())
    private val timers = objectMapOf(
        "rest" pairTo Timer(REST_DUR),
        "rise" pairTo Timer(
            TRANS_DUR, gdxArrayOf(
                TimeMarkedRunnable(0f) { transState = Size.SMALL },
                TimeMarkedRunnable(0.25f) { transState = Size.MEDIUM },
                TimeMarkedRunnable(0.5f) { transState = Size.LARGE }
            )),
        "fall" pairTo Timer(
            TRANS_DUR, gdxArrayOf(
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

        direction =
            Direction.valueOf(spawnProps.getOrDefault(ConstKeys.DIRECTION, ConstKeys.UP, String::class).uppercase())

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        when (direction) {
            Direction.UP -> body.setBottomCenterToPoint(bounds.getPositionPoint(Position.BOTTOM_CENTER))
            Direction.DOWN -> body.setTopCenterToPoint(bounds.getPositionPoint(Position.TOP_CENTER))
            Direction.LEFT -> body.setCenterRightToPoint(bounds.getPositionPoint(Position.CENTER_RIGHT))
            Direction.RIGHT -> body.setCenterLeftToPoint(bounds.getPositionPoint(Position.CENTER_LEFT))
        }

        facing = when (direction) {
            Direction.UP -> if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT
            Direction.DOWN -> if (megaman.body.getX() < body.getX()) Facing.RIGHT else Facing.LEFT
            Direction.LEFT -> if (megaman.body.getY() < body.getY()) Facing.LEFT else Facing.RIGHT
            Direction.RIGHT -> if (megaman.body.getX() < body.getX()) Facing.RIGHT else Facing.LEFT
        }
        transState = Size.SMALL
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isHealthDepleted()) explode()
    }

    private fun shoot() {
        val offset = GameObjectPools.fetch(Vector2::class)
        when (direction) {
            Direction.UP -> offset.set(SHOOT_OFFSET_X * facing.value, SHOOT_OFFSET_Y)
            Direction.DOWN -> offset.set(SHOOT_OFFSET_X * facing.value, -SHOOT_OFFSET_Y)
            Direction.LEFT -> offset.set(-SHOOT_OFFSET_Y, SHOOT_OFFSET_X * facing.value)
            Direction.RIGHT -> offset.set(SHOOT_OFFSET_Y, SHOOT_OFFSET_X * facing.value)
        }.scl(ConstVals.PPM.toFloat())

        val spawn = body.getCenter().add(offset)

        val impulse = GameObjectPools.fetch(Vector2::class)
        when (direction) {
            Direction.UP -> impulse.set(SHOOT_X * facing.value, SHOOT_Y)
            Direction.DOWN -> impulse.set(SHOOT_X * facing.value, -SHOOT_Y)
            Direction.LEFT -> impulse.set(-SHOOT_Y, SHOOT_X * facing.value)
            Direction.RIGHT -> impulse.set(SHOOT_Y, SHOOT_X * facing.value)
        }.scl(ConstVals.PPM.toFloat())

        val gravity = GameObjectPools.fetch(Vector2::class)
        when (megaman.direction) {
            Direction.UP -> gravity.set(0f, -BALL_GRAVITY)
            Direction.DOWN -> gravity.set(0f, BALL_GRAVITY)
            Direction.LEFT -> gravity.set(BALL_GRAVITY, 0f)
            Direction.RIGHT -> gravity.set(-BALL_GRAVITY, 0f)
        }.scl(ballGravityScalar * ConstVals.PPM.toFloat())

        val explodingBall = MegaEntityFactory.fetch(ExplodingBall::class)!!
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
            facing = if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT
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
        body.setSize(1.25f * ConstVals.PPM, 1.75f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle(body))
        body.addFixture(bodyFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().setWidth(1.15f * ConstVals.PPM))
        body.addFixture(damagerFixture)
        debugShapes.add { damagerFixture }

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle(body))
        body.addFixture(damageableFixture)
        debugShapes.add { if (damageableFixture.isActive()) damageableFixture.getShape() else null }

        body.preProcess.put(ConstKeys.DEFAULT) {
            val damageable = loop.getCurrent() == PopupCanonState.SHOOT ||
                (loop.getCurrent() == PopupCanonState.RISE && timers["rise"].getRatio() > TRANS_DAMAGEABLE_CUTOFF) ||
                (loop.getCurrent() == PopupCanonState.FALL && timers["fall"].getRatio() < TRANS_DAMAGEABLE_CUTOFF)
            damageableFixture.setActive(damageable)

            (damagerFixture.rawShape as GameRectangle).setHeight(
                when (transState) {
                    Size.LARGE -> 1.5f
                    Size.MEDIUM -> 1f
                    Size.SMALL -> 0.25f
                } * ConstVals.PPM
            )
            damagerFixture.offsetFromBodyAttachment.y = (when (transState) {
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
        sprite.setSize(2f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.setOriginCenter()
            sprite.rotation = direction.rotation

            val position = DirectionPositionMapper.getPosition(direction).opposite()
            sprite.setPosition(body.getPositionPoint(position), position)

            sprite.hidden = damageBlink

            when (direction) {
                Direction.UP -> sprite.setFlip(isFacing(Facing.RIGHT), false)
                Direction.DOWN -> sprite.setFlip(isFacing(Facing.LEFT), false)
                Direction.LEFT -> sprite.setFlip(false, isFacing(Facing.RIGHT))
                Direction.RIGHT -> sprite.setFlip(false, isFacing(Facing.LEFT))
            }
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = {
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
