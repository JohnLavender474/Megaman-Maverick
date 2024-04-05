package com.megaman.maverick.game.entities.bosses.sigmarat

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Queue
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.enums.Position
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.getRandom
import com.engine.common.getRandomBool
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
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.projectiles.SigmaRatElectricBall
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import kotlin.reflect.KClass

class SigmaRat(game: MegamanMaverickGame) : AbstractBoss(game) {

    companion object {
        const val TAG = "SigmaRat"

        private const val ATTACK_DELAY_MIN = 2f
        private const val ATTACK_DELAY_MAX = 5f

        private const val CLAW_ROTATION_SPEED = 5f

        private const val SHOCK_BALL_SPAWN = "shock_ball_spawn"

        private const val ELECTRIC_BALLS_MIN_ANGLE = 225f
        private const val ELECTRIC_BALLS_MAX_ANGLE = 315f
        private const val ELECTRIC_BALLS_COUNT = 12
        private const val ELECTRIC_BALLS_SPEED = 10f
        private const val ELECTRIC_BALL_SHOT_DELAY = 0.5f

        private var bodyRegion: TextureRegion? = null
        private var bodyDamagedRegion: TextureRegion? = null
        private var bodyTittyShootRegion: TextureRegion? = null
        private var bodyTittyShootDamagedRegion: TextureRegion? = null
    }

    enum class SigmaRatAttack {
        TITTY_LASERS, ELECTRIC_BALLS, FIRE_BLASTS, CLAW_SHOCK, CLAW_LAUNCH, DO_NOTHING
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>()

    private val attackTimer = Timer(ATTACK_DELAY_MAX)

    private val electricBalls = Queue<SigmaRatElectricBall>()
    private val electricShotDelayTimer = Timer(ELECTRIC_BALL_SHOT_DELAY)

    private lateinit var shockBallSpawn: Vector2

    private lateinit var leftClawSpawn: Vector2
    private lateinit var rightClawSpawn: Vector2

    private var leftClaw: SigmaRatClaw? = null
    private var rightClaw: SigmaRatClaw? = null

    private var attackState: SigmaRatAttack? = null

    private var electricBallsClockwise = false

    override fun init() {
        if (bodyRegion == null || bodyDamagedRegion == null || bodyTittyShootRegion == null || bodyTittyShootDamagedRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES.source)
            bodyRegion = atlas.findRegion("SigmaRat/Body")
            bodyDamagedRegion = atlas.findRegion("SigmaRat/BodyDamaged")
            bodyTittyShootRegion = atlas.findRegion("SigmaRat/BodyTittyShoot")
            bodyTittyShootDamagedRegion = atlas.findRegion("SigmaRat/BodyTittyShootDamaged")
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
        body.setBottomCenterToPoint(spawn)

        shockBallSpawn = spawnProps.get(SHOCK_BALL_SPAWN, RectangleMapObject::class)!!.rectangle.getCenter()

        leftClawSpawn = spawnProps.get(ConstKeys.LEFT, RectangleMapObject::class)!!.rectangle.getCenter()
        rightClawSpawn = spawnProps.get(ConstKeys.RIGHT, RectangleMapObject::class)!!.rectangle.getCenter()
        leftClaw = SigmaRatClaw(game as MegamanMaverickGame)
        rightClaw = SigmaRatClaw(game as MegamanMaverickGame)
        game.engine.spawn(
            leftClaw!! to props(
                ConstKeys.PARENT to this,
                ConstKeys.SPEED to CLAW_ROTATION_SPEED,
                ConstKeys.POSITION to leftClawSpawn
            ), rightClaw!! to props(
                ConstKeys.PARENT to this,
                ConstKeys.SPEED to -CLAW_ROTATION_SPEED,
                ConstKeys.POSITION to rightClawSpawn
            )
        )

        attackTimer.reset()
    }

    override fun onDestroy() {
        super.onDestroy()
        leftClaw?.kill()
        leftClaw = null
        rightClaw?.kill()
        rightClaw = null
        while (!electricBalls.isEmpty) electricBalls.removeFirst().kill()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (leftClaw?.dead == true) {
                leftClaw?.kill()
                leftClaw = null
            }
            if (rightClaw?.dead == true) {
                rightClaw?.kill()
                rightClaw = null
            }

            if (attackState == null) {
                attackTimer.update(delta)
                if (attackTimer.isFinished()) {
                    startAttack()
                    val newDuration = ATTACK_DELAY_MIN + (ATTACK_DELAY_MAX - ATTACK_DELAY_MIN) * getRandom(0, 1)
                    attackTimer.resetDuration(newDuration)
                }
            } else continueAttack(delta)
        }
    }

    private fun startAttack() {
        val attackState = SigmaRatAttack.ELECTRIC_BALLS // SigmaRatAttack.values().random()
        when (attackState) {
            SigmaRatAttack.TITTY_LASERS -> {}
            SigmaRatAttack.ELECTRIC_BALLS -> {
                electricShotDelayTimer.reset()
                for (i in 0 until ELECTRIC_BALLS_COUNT) {
                    val electricBall =
                        EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.SIGMA_RAT_ELECTRIC_BALL)!!
                    game.engine.spawn(electricBall, props(ConstKeys.POSITION to shockBallSpawn))
                    electricBalls.addLast(electricBall as SigmaRatElectricBall)
                }
                electricBallsClockwise = getRandomBool()
                requestToPlaySound(SoundAsset.LIFT_OFF_SOUND, false)
            }

            SigmaRatAttack.FIRE_BLASTS -> {}
            SigmaRatAttack.CLAW_SHOCK -> {}
            SigmaRatAttack.CLAW_LAUNCH -> {}
            SigmaRatAttack.DO_NOTHING -> {}
        }
        this.attackState = attackState
    }

    private fun continueAttack(delta: Float) {
        when (attackState) {
            SigmaRatAttack.TITTY_LASERS -> {}
            SigmaRatAttack.ELECTRIC_BALLS -> {
                electricShotDelayTimer.update(delta)
                if (electricShotDelayTimer.isFinished()) {
                    electricShotDelayTimer.reset()

                    val electricBall = electricBalls.removeFirst()
                    val currentIndex = ELECTRIC_BALLS_COUNT - electricBalls.size - 1
                    val angleIncrement =
                        (ELECTRIC_BALLS_MAX_ANGLE - ELECTRIC_BALLS_MIN_ANGLE) / (ELECTRIC_BALLS_COUNT - 1)
                    val angle = if (electricBallsClockwise) {
                        ELECTRIC_BALLS_MAX_ANGLE - angleIncrement * currentIndex
                    } else {
                        ELECTRIC_BALLS_MIN_ANGLE + angleIncrement * currentIndex
                    }
                    val trajectory = Vector2(0f, ELECTRIC_BALLS_SPEED * ConstVals.PPM).setAngleDeg(angle)
                    electricBall.launch(trajectory)

                    if (electricBalls.isEmpty) endAttack()
                }
            }

            SigmaRatAttack.FIRE_BLASTS -> {}
            SigmaRatAttack.CLAW_SHOCK -> {}
            SigmaRatAttack.CLAW_LAUNCH -> {}
            SigmaRatAttack.DO_NOTHING -> {}
            else -> throw IllegalStateException("Attack state cannot be null in 'continue attack' method")
        }
    }

    private fun endAttack() {
        attackState = null
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(7.5f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()
        body.color = Color.YELLOW
        debugShapes.add { body }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(0.85f * ConstVals.PPM))
        damagerFixture.offsetFromBodyCenter.y = 3f * ConstVals.PPM
        body.addFixture(damagerFixture)
        damagerFixture.rawShape.color = Color.RED
        debugShapes.add { damagerFixture.getShape() }

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().setSize(0.85f * ConstVals.PPM))
        damageableFixture.offsetFromBodyCenter.y = 3f * ConstVals.PPM
        body.addFixture(damageableFixture)
        damageableFixture.rawShape.color = Color.PURPLE
        debugShapes.add { damageableFixture.getShape() }

        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 0))
        sprite.setSize(10f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(this, sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setPosition(body.getBottomCenterPoint(), Position.BOTTOM_CENTER)
            _sprite.hidden = damageBlink || !ready
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = {
            when {
                damageBlink -> when {
                    attackState == SigmaRatAttack.TITTY_LASERS -> "BodyTittyShootDamaged"
                    else -> "BodyDamaged"
                }

                attackState == SigmaRatAttack.TITTY_LASERS -> "BodyTittyShoot"
                else -> "Body"
            }
        }
        val animations = objectMapOf<String, IAnimation>(
            "Body" to Animation(bodyRegion!!),
            "BodyTittyShoot" to Animation(bodyTittyShootRegion!!),
            "BodyDamaged" to Animation(bodyDamagedRegion!!, 1, 2, 0.1f, true),
            "BodyTittyShootDamaged" to Animation(bodyTittyShootDamagedRegion!!, 1, 2, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}