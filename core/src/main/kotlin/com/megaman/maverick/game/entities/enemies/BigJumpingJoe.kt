package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
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
import com.megaman.maverick.game.com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.IScalableGravityEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.EnemiesFactory
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*

class BigJumpingJoe(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.LARGE), IScalableGravityEntity,
    IFaceable, IAnimatedEntity {

    companion object {
        const val TAG = "BigJumpingJoe"

        private const val WAIT_DURATION = 1.5f

        private const val JUMP_DELAY = 0.2f
        private const val X_VEL = 8f
        private const val Y_VEL = 20f

        private const val GROUND_GRAVITY = -0.001f
        private const val GRAVITY = -0.5f

        private const val SHOOT_DURATION = 0.75f
        private const val BULLET_X_VEL = 10f
        private const val FIRST_BULLET_Y_VEL = -0.25f
        private const val SECOND_BULLET_Y_VEL = -0.5f
        private const val THIRD_BULLET_Y_VEL = -0.75f

        private var standRegion: TextureRegion? = null
        private var jumpRegion: TextureRegion? = null
    }

    override lateinit var facing: Facing
    override var gravityScalar = 1f

    private val waitTimer = Timer(WAIT_DURATION)
    private val jumpDelayTimer = Timer(JUMP_DELAY)
    private val shootTimer = Timer(
        SHOOT_DURATION, gdxArrayOf(
            TimeMarkedRunnable(0.25f) { shoot(FIRST_BULLET_Y_VEL) },
            TimeMarkedRunnable(0.5f) { shoot(SECOND_BULLET_Y_VEL) },
            TimeMarkedRunnable(0.75f) { shoot(THIRD_BULLET_Y_VEL) })
    )
    private lateinit var animations: ObjectMap<String, IAnimation>
    private var timesJumped = 0
    private var feetOnGround = false
    private var scaleBullet = true

    override fun init() {
        super.init()
        if (standRegion == null || jumpRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            standRegion = atlas.findRegion("$TAG/Stand")
            jumpRegion = atlas.findRegion("$TAG/Jump")
        }
        addComponent(defineAnimationsComponent())
        runnablesOnDestroy.put(ConstKeys.CHILD) {
            if (hasDepletedHealth()) {
                val sniperJoe = EntityFactories.fetch(EntityType.ENEMY, EnemiesFactory.SNIPER_JOE)!!
                val spawnProps = props(
                    ConstKeys.POSITION pairTo body.getCenter().add(
                        0.15f * ConstVals.PPM * facing.value, 0.15f * ConstVals.PPM
                    )
                )
                sniperJoe.spawn(spawnProps)
            }
        }
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)

        facing = if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT

        waitTimer.reset()
        jumpDelayTimer.setToEnd()
        shootTimer.reset()

        timesJumped = 0
        feetOnGround = true

        gravityScalar = spawnProps.getOrDefault("${ConstKeys.GRAVITY}_${ConstKeys.SCALAR}", 1f, Float::class)
        scaleBullet = spawnProps.getOrDefault("${ConstKeys.SCALE}_${ConstKeys.BULLET}", true, Boolean::class)

        val frameDuration = spawnProps.getOrDefault(ConstKeys.FRAME, 0.2f, Float::class)
        animations["jump"].setFrameDuration(frameDuration)
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add {
            if (body.physics.velocity.y > 0f &&
                body.isSensing(BodySense.HEAD_TOUCHING_BLOCK) &&
                !body.isSensing(BodySense.FEET_ON_GROUND)
            ) body.physics.velocity.y = 0f

            val wasFeetOnGround = feetOnGround
            feetOnGround = body.isSensing(BodySense.FEET_ON_GROUND)
            if (!wasFeetOnGround && feetOnGround) {
                body.physics.velocity.x = 0f
                requestToPlaySound(SoundAsset.TIME_STOPPER_SOUND, false)
            }

            if (timesJumped == 2 && body.isSensing(BodySense.FEET_ON_GROUND)) {
                shootTimer.update(it)
                if (shootTimer.isFinished()) {
                    shootTimer.reset()
                    timesJumped = 0
                }
                return@add
            }

            waitTimer.update(it)
            if (waitTimer.isJustFinished()) {
                jumpDelayTimer.reset()
                return@add
            } else if (!waitTimer.isFinished() && body.isSensing(BodySense.FEET_ON_GROUND)) facing =
                if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT

            jumpDelayTimer.update(it)
            if (jumpDelayTimer.isJustFinished()) {
                val velocity = GameObjectPools.fetch(Vector2::class)
                    .set(X_VEL * facing.value, Y_VEL)
                    .scl(ConstVals.PPM.toFloat())
                body.physics.velocity.set(velocity)

                waitTimer.reset()
                timesJumped++
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(1.5f * ConstVals.PPM, 3f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()

        val feetFixture = Fixture(
            body, FixtureType.FEET, GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.2f * ConstVals.PPM)
        )
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        body.addFixture(feetFixture)
        debugShapes.add { feetFixture }

        val headFixture = Fixture(
            body, FixtureType.HEAD, GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.2f * ConstVals.PPM)
        )
        headFixture.offsetFromBodyAttachment.y = body.getHeight() / 2f
        body.addFixture(headFixture)
        debugShapes.add { headFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.physics.gravity.y =
                ConstVals.PPM * (if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAVITY else GRAVITY) * gravityScalar
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this, body, BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(3.5f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.hidden = damageBlink
            val position = body.getPositionPoint(Position.BOTTOM_CENTER)
            sprite.setPosition(position, Position.BOTTOM_CENTER)
            sprite.setFlip(facing == Facing.RIGHT, false)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = { if (!waitTimer.isFinished()) "stand" else "jump" }
        animations = objectMapOf(
            "stand" pairTo Animation(standRegion!!),
            "jump" pairTo Animation(jumpRegion!!, 1, 2, 0.2f, false)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    private fun shoot(y: Float) {
        val trajectory =
            GameObjectPools.fetch(Vector2::class).set(BULLET_X_VEL * facing.value, y).scl(ConstVals.PPM.toFloat())
        if (scaleBullet) trajectory.x *= gravityScalar

        val props = props(
            ConstKeys.POSITION pairTo body.getCenter().add(0.25f * ConstVals.PPM * facing.value, 0.5f * ConstVals.PPM),
            ConstKeys.TRAJECTORY pairTo trajectory,
            ConstKeys.OWNER pairTo this
        )

        val bullet = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.BULLET)!!
        bullet.spawn(props)

        requestToPlaySound(SoundAsset.ENEMY_BULLET_SOUND, false)
    }
}
