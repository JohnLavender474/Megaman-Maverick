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
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.*
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
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.world.body.*
import kotlin.reflect.KClass

class SpikeBot(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "SpikeBot"
        private const val STAND_DUR = 0.25f

        private const val SHOOT_DUR = 0.5f
        private const val SHOOT_TIME = 0.25f

        private const val WALK_DUR = 0.75f
        private const val WALK_SPEED = 4f

        private const val NEEDLES = 3
        private const val NEEDLE_GRAV = -0.1f
        private const val NEEDLE_IMPULSE = 10f
        private const val NEEDLE_Y_OFFSET = 0.1f

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
        Bullet::class pairTo dmgNeg(15),
        Fireball::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShot::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShotExplosion::class pairTo dmgNeg(ConstVals.MAX_HEALTH)
    )
    override lateinit var facing: Facing

    private val loop = Loop(SpikeBotState.values().toGdxArray())
    private val timers = objectMapOf(
        "stand" pairTo Timer(STAND_DUR),
        "shoot" pairTo Timer(SHOOT_DUR, gdxArrayOf(TimeMarkedRunnable(SHOOT_TIME) { shoot() })),
        "walk" pairTo Timer(WALK_DUR)
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

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)
        loop.reset()
        timers.values().forEach { it.reset() }
        facing = if (megaman().body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT
        val frameDuration = 0.1f / movementScalar
        animations.values().forEach { it.setFrameDuration(frameDuration) }
    }

    private fun shoot() {
        for (i in 0 until NEEDLES) {
            val xOffset = xOffsets[i]
            val position = body.getPositionPoint(Position.TOP_CENTER).add(xOffset * ConstVals.PPM, NEEDLE_Y_OFFSET * ConstVals.PPM)

            val angle = angles[i]
            val impulse = Vector2(0f, NEEDLE_IMPULSE * ConstVals.PPM).rotateDeg(angle).scl(movementScalar)

            val needle = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.NEEDLE)!!
            needle.spawn(
                props(
                    ConstKeys.OWNER pairTo this,
                    ConstKeys.POSITION pairTo position,
                    ConstKeys.IMPULSE pairTo impulse,
                    ConstKeys.GRAVITY pairTo NEEDLE_GRAV * ConstVals.PPM
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
                        if (megaman().body.getX() < body.getX()) jump() else swapFacing()
                    } else if (isFacing(Facing.RIGHT) && !body.isProperty(RIGHT_FOOT, true)) {
                        if (megaman().body.getX() > body.getX()) jump() else swapFacing()
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
                    facing = if (megaman().body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(0.75f * ConstVals.PPM)
        body.physics.applyFrictionX = false
body.physics.applyFrictionY = false
        body.putProperty(LEFT_FOOT, false)
        body.putProperty(RIGHT_FOOT, false)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle(body))
        body.addFixture(bodyFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle(body))
        body.addFixture(damagerFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle(body))
        body.addFixture(damageableFixture)

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.5f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -0.375f * ConstVals.PPM
        body.addFixture(feetFixture)
        feetFixture.getShape().color = Color.GREEN
        debugShapes.add { feetFixture.getShape() }

        val leftSideFixture = Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM))
        leftSideFixture.offsetFromBodyAttachment.x = -0.375f * ConstVals.PPM
        leftSideFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        body.addFixture(leftSideFixture)
        leftSideFixture.getShape().color = Color.YELLOW
        debugShapes.add { leftSideFixture.getShape() }

        val rightSideFixture = Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM))
        rightSideFixture.offsetFromBodyAttachment.x = 0.375f * ConstVals.PPM
        rightSideFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        body.addFixture(rightSideFixture)
        rightSideFixture.getShape().color = Color.YELLOW
        debugShapes.add { rightSideFixture.getShape() }

        val leftFootFixture = Fixture(body, FixtureType.CONSUMER, GameRectangle().setSize(0.1f * ConstVals.PPM))
        leftFootFixture.setConsumer { _, fixture ->
            if (fixture.getType() == FixtureType.BLOCK)
                body.putProperty("${ConstKeys.LEFT}_${ConstKeys.FOOT}", true)
        }
        leftFootFixture.offsetFromBodyAttachment = vector2Of(-0.375f * ConstVals.PPM)
        body.addFixture(leftFootFixture)
        leftFootFixture.getShape().color = Color.ORANGE
        debugShapes.add { leftFootFixture.getShape() }

        val rightFootFixture = Fixture(body, FixtureType.CONSUMER, GameRectangle().setSize(0.1f * ConstVals.PPM))
        rightFootFixture.setConsumer { _, fixture ->
            if (fixture.getType() == FixtureType.BLOCK)
                body.putProperty("${ConstKeys.RIGHT}_${ConstKeys.FOOT}", true)
        }
        rightFootFixture.offsetFromBodyAttachment.x = 0.375f * ConstVals.PPM
        rightFootFixture.offsetFromBodyAttachment.y = -0.375f * ConstVals.PPM
        body.addFixture(rightFootFixture)
        rightFootFixture.getShape().color = Color.ORANGE
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
            _sprite.setPosition(body.getPositionPoint(Position.BOTTOM_CENTER), Position.BOTTOM_CENTER)
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
            "jump" pairTo Animation(regions["jump"]),
            "stand" pairTo Animation(regions["stand"]),
            "walk" pairTo Animation(regions["walk"], 2, 2, 0.1f, true),
            "shoot" pairTo Animation(regions["shoot"], 5, 1, 0.1f, false)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
