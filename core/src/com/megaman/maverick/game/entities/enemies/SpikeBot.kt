package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.enums.Facing
import com.engine.common.enums.Position
import com.engine.common.extensions.*
import com.engine.common.interfaces.IFaceable
import com.engine.common.interfaces.isFacing
import com.engine.common.interfaces.swapFacing
import com.engine.common.objects.Loop
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.TimeMarkedRunnable
import com.engine.common.time.Timer
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
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
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.world.*
import kotlin.reflect.KClass

class SpikeBot(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "SpikeBot"
        private const val STAND_DUR = 0.25f
        private const val SHOOT_DUR = 0.5f
        private const val SHOOT_TIME = 0.3f
        private const val WALK_DUR = 0.25f
        private const val WALK_SPEED = 5f
        private const val NEEDLES = 3
        private const val Y_OFFSET = 0.1f
        private const val NEEDLE_SPEED = 10f
        private const val JUMP_IMPULSE = 10f
        private const val LEFT_FOOT = "${ConstKeys.LEFT}_${ConstKeys.FOOT}"
        private const val RIGHT_FOOT = "${ConstKeys.RIGHT}_${ConstKeys.FOOT}"
        private const val GRAVITY = -0.15f
        private const val GROUND_GRAVITY = -0.01f
        private val angles = gdxArrayOf(45f, 0f, 315f)
        private val xOffsets = gdxArrayOf(-0.1f, 0f, 0.1f)
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class SpikeBotState { STAND, WALK, SHOOT }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class to dmgNeg(15),
        Fireball::class to dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShot::class to dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShotExplosion::class to dmgNeg(ConstVals.MAX_HEALTH)
    )
    override lateinit var facing: Facing

    private val loop = Loop(SpikeBotState.values().toGdxArray())
    private val timers = objectMapOf(
        "stand" to Timer(STAND_DUR),
        "shoot" to Timer(SHOOT_DUR, gdxArrayOf(TimeMarkedRunnable(SHOOT_TIME) { shoot() })),
        "walk" to Timer(WALK_DUR)
    )
    private lateinit var animations: ObjectMap<String, IAnimation>

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            regions.put("jump", atlas.findRegion("$TAG/jump"))
            regions.put("walk", atlas.findRegion("$TAG/walk"))
            regions.put("shoot", atlas.findRegion("$TAG/shoot"))
            regions.put("stand", atlas.findRegion("$TAG/stand"))
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
        body.setBottomCenterToPoint(spawn)
        loop.reset()
        timers.values().forEach { it.reset() }
        facing = if (getMegaman().body.x < body.x) Facing.LEFT else Facing.RIGHT
        val frameDuration = 0.1f / movementScalar
        animations.values().forEach { it.setFrameDuration(frameDuration) }
    }

    private fun shoot() {
        for (i in 0 until NEEDLES) {
            val xOffset = xOffsets[i]
            val position = body.getTopCenterPoint().add(xOffset * ConstVals.PPM, Y_OFFSET * ConstVals.PPM)

            val angle = angles[i]
            val trajectory = Vector2(0f, NEEDLE_SPEED * ConstVals.PPM).rotateDeg(angle).scl(movementScalar)

            val needle = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.NEEDLE)!!
            game.engine.spawn(
                needle, props(
                    ConstKeys.OWNER to this,
                    ConstKeys.POSITION to position,
                    ConstKeys.TRAJECTORY to trajectory
                )
            )
        }

        if (overlapsGameCamera()) requestToPlaySound(SoundAsset.THUMP_SOUND, false)
    }

    private fun jump() {
        body.physics.velocity.y = JUMP_IMPULSE * ConstVals.PPM * movementScalar
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (!body.isSensing(BodySense.FEET_ON_GROUND)) return@add

            when (loop.getCurrent()) {
                SpikeBotState.STAND, SpikeBotState.SHOOT -> body.physics.velocity.x = 0f
                SpikeBotState.WALK -> {
                    if ((isFacing(Facing.LEFT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT)) ||
                        (isFacing(Facing.RIGHT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT))
                    ) swapFacing()
                    else if (isFacing(Facing.LEFT) && !body.isProperty(LEFT_FOOT, true)) {
                        if (getMegaman().body.x < body.x) jump() else swapFacing()
                    } else if (isFacing(Facing.RIGHT) && !body.isProperty(RIGHT_FOOT, true)) {
                        if (getMegaman().body.x > body.x) jump() else swapFacing()
                    }

                    body.physics.velocity.x = WALK_SPEED * ConstVals.PPM * facing.value * movementScalar
                }
            }

            val timer = timers[loop.getCurrent().name.lowercase()]
            timer.update(delta)
            if (timer.isFinished()) {
                timer.reset()
                loop.next()
                if (loop.getCurrent() != SpikeBotState.WALK)
                    facing = if (getMegaman().body.x < body.x) Facing.LEFT else Facing.RIGHT
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(0.75f * ConstVals.PPM)
        body.putProperty(LEFT_FOOT, false)
        body.putProperty(RIGHT_FOOT, false)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBodyBounds() }

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle(body))
        body.addFixture(bodyFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle(body))
        body.addFixture(damagerFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle(body))
        body.addFixture(damageableFixture)

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.5f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyCenter.y = -0.375f * ConstVals.PPM
        body.addFixture(feetFixture)
        feetFixture.rawShape.color = Color.GREEN
        debugShapes.add { feetFixture.getShape() }

        val leftSideFixture = Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM))
        leftSideFixture.offsetFromBodyCenter.x = -0.375f * ConstVals.PPM
        leftSideFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        body.addFixture(leftSideFixture)
        leftSideFixture.rawShape.color = Color.YELLOW
        debugShapes.add { leftSideFixture.getShape() }

        val rightSideFixture = Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM))
        rightSideFixture.offsetFromBodyCenter.x = 0.375f * ConstVals.PPM
        rightSideFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        body.addFixture(rightSideFixture)
        rightSideFixture.rawShape.color = Color.YELLOW
        debugShapes.add { rightSideFixture.getShape() }

        val leftFootFixture = Fixture(body, FixtureType.CONSUMER, GameRectangle().setSize(0.1f * ConstVals.PPM))
        leftFootFixture.setConsumer { _, fixture ->
            if (fixture.getFixtureType() == FixtureType.BLOCK)
                body.putProperty("${ConstKeys.LEFT}_${ConstKeys.FOOT}", true)
        }
        leftFootFixture.offsetFromBodyCenter = vector2Of(-0.375f * ConstVals.PPM)
        body.addFixture(leftFootFixture)
        leftFootFixture.rawShape.color = Color.ORANGE
        debugShapes.add { leftFootFixture.getShape() }

        val rightFootFixture = Fixture(body, FixtureType.CONSUMER, GameRectangle().setSize(0.1f * ConstVals.PPM))
        rightFootFixture.setConsumer { _, fixture ->
            if (fixture.getFixtureType() == FixtureType.BLOCK)
                body.putProperty("${ConstKeys.RIGHT}_${ConstKeys.FOOT}", true)
        }
        rightFootFixture.offsetFromBodyCenter.x = 0.375f * ConstVals.PPM
        rightFootFixture.offsetFromBodyCenter.y = -0.375f * ConstVals.PPM
        body.addFixture(rightFootFixture)
        rightFootFixture.rawShape.color = Color.ORANGE
        debugShapes.add { rightFootFixture.getShape() }

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.putProperty(LEFT_FOOT, false)
            body.putProperty(RIGHT_FOOT, false)

            val gravity = if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAVITY else GRAVITY
            body.physics.gravity.y = gravity * ConstVals.PPM * movementScalar
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.15f * ConstVals.PPM, 1.25f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setPosition(body.getBottomCenterPoint(), Position.BOTTOM_CENTER)
            _sprite.hidden = damageBlink
            _sprite.setFlip(isFacing(Facing.LEFT), false)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String = {
            if (!body.isSensing(BodySense.FEET_ON_GROUND)) "jump"
            else when (loop.getCurrent()) {
                SpikeBotState.STAND -> "stand"
                SpikeBotState.WALK -> "walk"
                SpikeBotState.SHOOT -> "shoot"
            }
        }
        animations = objectMapOf(
            "jump" to Animation(regions["jump"]),
            "stand" to Animation(regions["stand"]),
            "walk" to Animation(regions["walk"], 2, 2, 0.1f, true),
            "shoot" to Animation(regions["shoot"], 5, 1, 0.1f, false)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}