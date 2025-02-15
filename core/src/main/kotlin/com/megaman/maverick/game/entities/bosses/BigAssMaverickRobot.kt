package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods.getRandom
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.TimeMarkedRunnable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.damage.StandardDamageNegotiator
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.bosses.BigAssMaverickRobotHand.BigAddMaverickRobotHandState
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.projectiles.BigAssMaverickRobotOrb
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter
import java.util.*

class BigAssMaverickRobot(game: MegamanMaverickGame) : AbstractBoss(game), IAnimatedEntity {

    companion object {
        const val TAG = "BigAssMaverickRobot"

        private const val BODY_WIDTH = 7f
        private const val BODY_HEIGHT = 10f

        private const val HEAD_SIZE = 3f

        private const val INIT_DUR = 5f

        private const val SHOOT_ORBS_DELAY = 3f
        private const val SHOOT_ORBS_DUR = 4f
        private const val DELAY_BETWEEN_ORBS = 1f
        private const val ORB_MOVE_DELAY = 0.5f
        private const val ORBS_COUNT = 2
        private const val ORB_SPEED = 6f

        private const val LAUNCH_HAND_DELAY = 3f
        private const val HAND_RADIUS = 2f
        private const val HAND_ROTATION_SPEED = 5f
        private const val CLOSEST_HAND_CHANCE = 75
    }

    override val damageNegotiator = StandardDamageNegotiator(
        objectMapOf(
            Bullet::class pairTo dmgNeg(2),
            ChargedShot::class pairTo dmgNeg {
                it as ChargedShot
                if (it.fullyCharged) 4 else 3
            },
            ChargedShotExplosion::class pairTo dmgNeg {
                it as ChargedShotExplosion
                if (it.fullyCharged) 2 else 1
            }
        )
    )

    private val allHands = Array<BigAssMaverickRobotHand>(2)
    private var launchedHand: BigAssMaverickRobotHand? = null

    private val initTimer = Timer(INIT_DUR)
    private val shootOrbsDelay = Timer(SHOOT_ORBS_DELAY)
    private val shootOrbsTimer = Timer(SHOOT_ORBS_DUR).also { timer ->
        for (i in 1..ORBS_COUNT) {
            val time = i * DELAY_BETWEEN_ORBS
            val runnable = TimeMarkedRunnable(time) { shootOrb() }
            timer.addRunnable(runnable)
        }
    }
    private val launchHandDelay = Timer(LAUNCH_HAND_DELAY)

    override fun init() {
        GameLogger.debug(TAG, "init()")
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.MUSIC, MusicAsset.MMX7_BOSS_FIGHT_MUSIC.name)

        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)

        initTimer.reset()
        shootOrbsDelay.reset()
        shootOrbsTimer.setToEnd()
        launchHandDelay.reset()

        val leftHandPosition = spawnProps.get(ConstKeys.LEFT, RectangleMapObject::class)!!.rectangle.getCenter()
        val leftHand = MegaEntityFactory.fetch(BigAssMaverickRobotHand::class)!!
        leftHand.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.ORIGIN pairTo leftHandPosition,
                ConstKeys.RADIUS pairTo HAND_RADIUS * ConstVals.PPM,
                ConstKeys.SPEED pairTo -HAND_ROTATION_SPEED * ConstVals.PPM
            )
        )
        allHands.add(leftHand)

        val rightHandPosition = spawnProps.get(ConstKeys.RIGHT, RectangleMapObject::class)!!.rectangle.getCenter()
        val rightHand = MegaEntityFactory.fetch(BigAssMaverickRobotHand::class)!!
        rightHand.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.ORIGIN pairTo rightHandPosition,
                ConstKeys.RADIUS pairTo HAND_RADIUS * ConstVals.PPM,
                ConstKeys.SPEED pairTo HAND_ROTATION_SPEED * ConstVals.PPM
            )
        )
        allHands.add(rightHand)
    }

    override fun isReady(delta: Float) = initTimer.isFinished()

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        allHands.forEach { it.destroy() }
        allHands.clear()

        launchedHand = null
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (defeated) {
                explodeOnDefeat(delta)
                return@add
            }

            initTimer.update(delta)
            if (!initTimer.isFinished()) return@add

            shootOrbsTimer.update(delta)
            if (shootOrbsTimer.isJustFinished()) {
                GameLogger.debug(TAG, "update(): shoot orbs timer just finished")
                shootOrbsDelay.reset()
            } else if (shootOrbsTimer.isFinished()) shootOrbsDelay.update(delta)

            if (shootOrbsDelay.isJustFinished() && shootOrbsTimer.isFinished()) {
                GameLogger.debug(TAG, "update(): shoot orbs delay just finished")
                shootOrbsTimer.reset()
            }

            if (launchedHand == null) {
                launchHandDelay.update(delta)
                if (launchHandDelay.isFinished()) {
                    val handToLaunch = allHands
                        .filter { !it.isBeingStoodUpon() }
                        .let { hands ->
                            when {
                                hands.isEmpty() -> throw IllegalStateException(
                                    "Megaman cannot be standing on both hands at the same time when calculating " +
                                        "which hand to launch. The hands need to be spaced more apart."
                                )

                                hands.size == 1 -> hands[0]

                                getRandom(0, 100) <= CLOSEST_HAND_CHANCE -> {
                                    Collections.sort(hands) { h1, h2 ->
                                        h1.body.getCenter().dst2(megaman.body.getCenter())
                                            .compareTo(h2.body.getCenter().dst2(megaman.body.getCenter()))
                                    }

                                    hands[0]
                                }

                                else -> hands.random()
                            }
                        }

                    handToLaunch.launch()

                    launchedHand = handToLaunch
                }
            } else if (launchedHand!!.state == BigAddMaverickRobotHandState.ROTATE) {
                launchedHand = null

                launchHandDelay.reset()
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.setSize(BODY_WIDTH * ConstVals.PPM, BODY_HEIGHT * ConstVals.PPM)
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val headDamageable = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().setSize(HEAD_SIZE * ConstVals.PPM))
        headDamageable.offsetFromBodyAttachment.y = body.getHeight() / 2f
        body.addFixture(headDamageable)
        headDamageable.drawingColor = Color.PURPLE
        debugShapes.add { headDamageable }

        // TODO
        addComponent(
            DrawableShapesComponent(
                prodShapeSuppliers = debugShapes,
                /* TODO: debugShapeSuppliers = debugShapes*/
                debug = true
            )
        )

        return BodyComponentCreator.create(this, body)
    }

    // TODO
    override fun defineSpritesComponent() = SpritesComponentBuilder().build()

    // TODO
    private fun defineAnimationsComponent() = AnimationsComponentBuilder().build()

    private fun shootOrb() {
        val spawn = body.getCenter().add(0f, body.getHeight() / 2f)

        val trajectory = megaman.body.getCenter().sub(spawn).nor().scl(ORB_SPEED * ConstVals.PPM)

        val orb = MegaEntityFactory.fetch(BigAssMaverickRobotOrb::class)!!
        orb.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.DELAY pairTo ORB_MOVE_DELAY,
                ConstKeys.TRAJECTORY pairTo trajectory
            )
        )

        GameLogger.debug(TAG, "shootOrb(): spawn=$spawn, trajectory=$trajectory")
    }

    override fun getTag() = TAG
}

