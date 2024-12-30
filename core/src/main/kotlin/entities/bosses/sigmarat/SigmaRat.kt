package com.megaman.maverick.game.entities.bosses.sigmarat

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Queue
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.UtilMethods.getRandomBool
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.*
import com.mega.game.engine.common.objects.*
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
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.entities.projectiles.SigmaRatElectricBall

import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*
import kotlin.reflect.KClass

class SigmaRat(game: MegamanMaverickGame) : AbstractBoss(game) {

    companion object {
        const val TAG = "SigmaRat"

        private const val HIGH_CHANCE = 6f
        private const val MEDIUM_CHANCE = 3f
        private const val LOW_CHANCE = 1f

        private const val ATTACK_DELAY_MIN = 0.25f
        private const val ATTACK_DELAY_MAX = 1.25f

        private const val CLAW_ROTATION_SPEED = 5f

        private const val HEAD_POSITION = "head_position"

        private val ELECTRIC_BALL_ANGLES = gdxArrayOf(
            225f,
            232.5f,
            240f,
            247.5f,
            255f,
            262.5f,
            270f,
            277.5f,
            285f,
            292.5f,
            300f,
            307.5f,
            315f,
        )
        private val FIRE_BALL_ANGLES = gdxArrayOf(
            180f,
            195f,
            210f,
            225f,
            240f,
            255f,
            270f,
            285f,
            300f,
            315f,
            330f,
            345f,
            360f
        )

        private const val ELECTRIC_BALLS_SPEED = 10f
        private const val ELECTRIC_BALL_SHOT_DELAY = 0.5f
        private val INDICES_TO_TRY_CLAW_DURING_ELECTRIC_BALLS = objectSetOf(3, 7)

        private const val FIREBALL_DELAY = 0.5f
        private const val FIREBALL_SPEED = 7.5f
        private const val FIREBALL_CULL_TIME = 3f

        private var bodyRegion: TextureRegion? = null
        private var bodyDamagedRegion: TextureRegion? = null
        private var bodyTittyShootRegion: TextureRegion? = null
        private var bodyTittyShootDamagedRegion: TextureRegion? = null
    }

    enum class SigmaRatAttack {
        ELECTRIC_BALLS, FIRE_BLASTS, CLAW_SHOCK, CLAW_LAUNCH
    }

    override val damageNegotiations =
        objectMapOf<KClass<out IDamager>, DamageNegotiation>(ChargedShot::class pairTo dmgNeg {
            if ((it as ChargedShot).fullyCharged) 1 else 0
        })

    private val weightedAttackSelector = WeightedRandomSelector(
        SigmaRatAttack.ELECTRIC_BALLS pairTo HIGH_CHANCE,
        SigmaRatAttack.FIRE_BLASTS pairTo MEDIUM_CHANCE,
        SigmaRatAttack.CLAW_SHOCK pairTo MEDIUM_CHANCE,
        SigmaRatAttack.CLAW_LAUNCH pairTo HIGH_CHANCE
    )
    private val attackTimer = Timer(ATTACK_DELAY_MAX)

    private val electricBalls = Queue<SigmaRatElectricBall>()
    private val electricShotDelayTimer = Timer(ELECTRIC_BALL_SHOT_DELAY)

    private val fireballs = Queue<GamePair<Fireball, Float>>()
    private val fireballDelayTimer = Timer(FIREBALL_DELAY)

    private val headPosition = Vector2()
    private val leftClawSpawn = Vector2()
    private val rightClawSpawn = Vector2()

    private var leftClaw: SigmaRatClaw? = null
    private var rightClaw: SigmaRatClaw? = null
    private var attackState: SigmaRatAttack? = null

    private var electricBallsClockwise = false

    override fun init() {
        if (bodyRegion == null || bodyDamagedRegion == null || bodyTittyShootRegion == null || bodyTittyShootDamagedRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_1.source)
            bodyRegion = atlas.findRegion("SigmaRat/Body")
            bodyDamagedRegion = atlas.findRegion("SigmaRat/BodyDamaged")
            bodyTittyShootRegion = atlas.findRegion("SigmaRat/BodyTittyShoot")
            bodyTittyShootDamagedRegion = atlas.findRegion("SigmaRat/BodyTittyShootDamaged")
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)

        headPosition.set(spawnProps.get(HEAD_POSITION, RectangleMapObject::class)!!.rectangle.getCenter())

        leftClawSpawn.set(spawnProps.get(ConstKeys.LEFT, RectangleMapObject::class)!!.rectangle.getCenter())
        rightClawSpawn.set(spawnProps.get(ConstKeys.RIGHT, RectangleMapObject::class)!!.rectangle.getCenter())

        leftClaw = SigmaRatClaw(game)
        rightClaw = SigmaRatClaw(game)

        leftClaw!!.spawn(
            props(
                ConstKeys.PARENT pairTo this,
                ConstKeys.SPEED pairTo CLAW_ROTATION_SPEED,
                ConstKeys.POSITION pairTo leftClawSpawn,
                ConstKeys.MAX_Y pairTo headPosition.y
            )
        )
        rightClaw!!.spawn(
            props(
                ConstKeys.PARENT pairTo this,
                ConstKeys.SPEED pairTo -CLAW_ROTATION_SPEED,
                ConstKeys.POSITION pairTo rightClawSpawn,
                ConstKeys.MAX_Y pairTo headPosition.y
            )
        )

        attackTimer.reset()
        attackState = null
    }

    override fun isReady(delta: Float) = true // TODO

    override fun onDestroy() {
        super.onDestroy()
        leftClaw?.destroy()
        leftClaw = null
        rightClaw?.destroy()
        rightClaw = null
        while (!electricBalls.isEmpty) electricBalls.removeFirst().destroy()
    }

    override fun triggerDefeat() {
        super.triggerDefeat()

        val explosions = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.EXPLOSION, 2)

        explosions[0].spawn(
            props(
                ConstKeys.POSITION pairTo leftClaw!!.body.getCenter(),
                ConstKeys.SOUND pairTo SoundAsset.EXPLOSION_1_SOUND
            )
        )

        explosions[1].spawn(
            props(
                ConstKeys.POSITION pairTo rightClaw!!.body.getCenter(),
                ConstKeys.SOUND pairTo SoundAsset.EXPLOSION_1_SOUND
            )
        )

        leftClaw!!.destroy()
        leftClaw = null
        rightClaw!!.destroy()
        rightClaw = null
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (!ready) return@add
            if (defeated) {
                explodeOnDefeat(delta)
                return@add
            }

            if (leftClaw != null && leftClaw!!.dead) {
                leftClaw?.destroy()
                leftClaw = null
            }
            if (rightClaw != null && rightClaw!!.dead) {
                rightClaw?.destroy()
                rightClaw = null
            }

            if (attackState == null) {
                attackTimer.update(delta)
                if (attackTimer.isFinished()) {
                    startAttack()
                    val newDuration = ATTACK_DELAY_MIN + (ATTACK_DELAY_MAX - ATTACK_DELAY_MIN) * getHealthRatio()
                    attackTimer.resetDuration(newDuration)
                }
            } else continueAttack(delta)
        }
    }

    private fun startAttack() {
        if (megaman.body.getMaxY() >= body.getCenter().y) {
            weightedAttackSelector.putItem(SigmaRatAttack.CLAW_SHOCK, HIGH_CHANCE)
            weightedAttackSelector.putItem(SigmaRatAttack.CLAW_LAUNCH, LOW_CHANCE)
            weightedAttackSelector.putItem(SigmaRatAttack.ELECTRIC_BALLS, LOW_CHANCE)
            weightedAttackSelector.removeItem(SigmaRatAttack.FIRE_BLASTS)
        } else {
            weightedAttackSelector.putItem(SigmaRatAttack.CLAW_SHOCK, MEDIUM_CHANCE)
            weightedAttackSelector.putItem(SigmaRatAttack.CLAW_LAUNCH, HIGH_CHANCE)
            weightedAttackSelector.putItem(SigmaRatAttack.ELECTRIC_BALLS, HIGH_CHANCE)
            weightedAttackSelector.putItem(SigmaRatAttack.FIRE_BLASTS, MEDIUM_CHANCE)
        }

        val attackState = weightedAttackSelector.getRandomItem()
        when (attackState) {
            SigmaRatAttack.ELECTRIC_BALLS -> {
                electricShotDelayTimer.reset()

                (0 until ELECTRIC_BALL_ANGLES.size).forEach { i ->
                    val electricBall =
                        EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.SIGMA_RAT_ELECTRIC_BALL)!!
                    electricBall.spawn(props(ConstKeys.POSITION pairTo headPosition))
                    electricBalls.addLast(electricBall as SigmaRatElectricBall)
                }

                electricBallsClockwise = getRandomBool()

                requestToPlaySound(SoundAsset.LIFT_OFF_SOUND, false)
            }

            SigmaRatAttack.FIRE_BLASTS -> {
                fireballDelayTimer.reset()

                val angles = FIRE_BALL_ANGLES.getRandomElements(3)
                for (angle in angles) {
                    val fireball =
                        EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.FIREBALL)!! as Fireball

                    fireball.spawn(
                        props(
                            ConstKeys.POSITION pairTo headPosition, ConstKeys.CULL_TIME pairTo FIREBALL_CULL_TIME
                        )
                    )

                    fireballs.addLast(fireball pairTo angle)
                }
            }

            SigmaRatAttack.CLAW_SHOCK -> {
                val claw = when {
                    leftClaw!!.shocking || leftClaw!!.launched -> rightClaw
                    rightClaw!!.shocking || rightClaw!!.launched -> leftClaw
                    else -> {
                        val distToLeft = megaman.body.getCenter().dst2(leftClaw!!.body.getCenter())
                        val distToRight = megaman.body.getCenter().dst2(rightClaw!!.body.getCenter())
                        if (distToLeft > distToRight) rightClaw else leftClaw
                    }
                }

                if (claw!!.shocking || claw.launched) return

                claw.enterShockState()
            }

            SigmaRatAttack.CLAW_LAUNCH -> {
                val claw = when {
                    leftClaw!!.shocking || leftClaw!!.launched -> rightClaw
                    rightClaw!!.shocking || rightClaw!!.launched -> leftClaw
                    else -> {
                        val distToLeft = megaman.body.getCenter().dst2(leftClaw!!.body.getCenter())
                        val distToRight = megaman.body.getCenter().dst2(rightClaw!!.body.getCenter())
                        if (distToLeft > distToRight) leftClaw else rightClaw
                    }
                }

                if (claw!!.shocking || claw.launched) return

                claw.enterLaunchState()
            }
        }

        this.attackState = attackState
    }

    private fun continueAttack(delta: Float) {
        when (attackState) {
            SigmaRatAttack.ELECTRIC_BALLS -> {
                electricShotDelayTimer.update(delta)
                if (electricShotDelayTimer.isFinished()) {
                    electricShotDelayTimer.reset()

                    val electricBall = electricBalls.removeFirst()

                    var index = if (electricBallsClockwise) electricBalls.size
                    else ELECTRIC_BALL_ANGLES.size - electricBalls.size - 1
                    index = index.coerceIn(0, ELECTRIC_BALL_ANGLES.size - 1)
                    val angle = ELECTRIC_BALL_ANGLES[index]

                    val trajectory = Vector2(0f, ELECTRIC_BALLS_SPEED * ConstVals.PPM).setAngleDeg(angle)
                    electricBall.launch(trajectory)

                    if (INDICES_TO_TRY_CLAW_DURING_ELECTRIC_BALLS.contains(index)) {
                        val attack = weightedAttackSelector.getRandomItem()
                        when (attack) {
                            SigmaRatAttack.CLAW_SHOCK -> {
                                val distToLeft = megaman.body.getCenter().dst2(leftClaw!!.body.getCenter())
                                val distToRight = megaman.body.getCenter().dst2(rightClaw!!.body.getCenter())
                                val claw = if (distToLeft > distToRight) leftClaw else rightClaw
                                if (!claw!!.shocking && !claw.launched) claw.enterShockState()
                            }

                            SigmaRatAttack.CLAW_LAUNCH -> {
                                val distToLeft = megaman.body.getCenter().dst2(leftClaw!!.body.getCenter())
                                val distToRight = megaman.body.getCenter().dst2(rightClaw!!.body.getCenter())
                                val claw = if (distToLeft > distToRight) leftClaw else rightClaw
                                if (!claw!!.shocking && !claw.launched) claw.enterLaunchState()
                            }

                            else -> {}
                        }
                    }

                    if (electricBalls.isEmpty) endAttack()
                }
            }

            SigmaRatAttack.FIRE_BLASTS -> {
                fireballDelayTimer.update(delta)
                if (fireballDelayTimer.isFinished()) {
                    val (fireball, angle) = fireballs.removeLast()

                    val trajectory = GameObjectPools.fetch(Vector2::class)
                        .set(0f, FIREBALL_SPEED * ConstVals.PPM)
                        .setAngleDeg(angle)
                    fireball.body.physics.velocity.set(trajectory)

                    fireballDelayTimer.reset()
                }
                if (fireballs.isEmpty) endAttack()
            }

            SigmaRatAttack.CLAW_LAUNCH -> if (leftClaw?.launched != true && rightClaw?.launched != true) endAttack()

            SigmaRatAttack.CLAW_SHOCK -> if (leftClaw?.shocking != true && rightClaw?.shocking != true) endAttack()

            null -> {}
        }
    }

    private fun endAttack() {
        attackState = null
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(7.5f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(0.85f * ConstVals.PPM))
        damagerFixture.offsetFromBodyAttachment.y = 3f * ConstVals.PPM
        body.addFixture(damagerFixture)
        debugShapes.add { damagerFixture }

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().setSize(0.85f * ConstVals.PPM))
        damageableFixture.offsetFromBodyAttachment.y = 3f * ConstVals.PPM
        body.addFixture(damageableFixture)
        debugShapes.add { damageableFixture }

        val shieldFixture = Fixture(body, FixtureType.SHIELD, GameRectangle().setSize(0.65f * ConstVals.PPM))
        shieldFixture.offsetFromBodyAttachment.y = 3f * ConstVals.PPM
        body.addFixture(shieldFixture)
        debugShapes.add { shieldFixture }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 0))
        sprite.setSize(10f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.setPosition(body.getPositionPoint(Position.BOTTOM_CENTER), Position.BOTTOM_CENTER)
            sprite.hidden = damageBlink || !ready
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = {
            when {
                damageBlink -> "BodyDamaged"
                else -> "Body"
            }
        }
        val animations = objectMapOf<String, IAnimation>(
            "Body" pairTo Animation(bodyRegion!!),
            "BodyDamaged" pairTo Animation(bodyDamagedRegion!!, 1, 2, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
