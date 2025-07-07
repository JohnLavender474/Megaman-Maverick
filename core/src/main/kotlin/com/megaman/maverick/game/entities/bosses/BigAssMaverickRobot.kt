package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods
import com.mega.game.engine.common.UtilMethods.getRandom
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.*
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.TimeMarkedRunnable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.events.Event
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.StandardDamageNegotiator
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.blocks.BreakableBlock
import com.megaman.maverick.game.entities.bosses.BigAssMaverickRobotHand.BigAssMaverickRobotHandState
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.decorations.BlockPiece.BlockPieceColor
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.explosions.Explosion
import com.megaman.maverick.game.entities.projectiles.BigAssMaverickRobotOrb
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getBoundingRectangle
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getPositionPoint
import java.util.*
import kotlin.math.abs

class BigAssMaverickRobot(game: MegamanMaverickGame) : AbstractBoss(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "BigAssMaverickRobot"

        private const val HEAD = "Head"

        private const val BODY_WIDTH = 8f
        private const val BODY_HEIGHT = 12f

        private const val TURN_HEAD_DELAY = 0.5f

        private const val HEAD_SPRITE_SIZE = 5f
        private const val HEAD_RADIUS = 1.25f
        private const val HEAD_DAMAGE_RADIUS = 1.5f
        private const val HEAD_ANGLES_SIZE = 3
        private val HEAD_ANGLES_LEFT = gdxArrayOf(180f, 135f, 90f)
        private val HEAD_ANGLES_RIGHT = gdxArrayOf(180f, 225f, 270f)

        private const val INIT_DUR = 1f
        private const val STUNNED_DUR = 0.5f

        private const val SHOOT_ORBS_DELAY = 3f
        private const val SHOOT_ORBS_DUR = 4f
        private const val DELAY_BETWEEN_ORBS = 1f
        private const val ORB_MAX_MOVE_DELAY = 1f
        private const val ORB_MIN_MOVE_DELAY = 0.25f
        private const val ORBS_COUNT = 2
        private const val ORB_MIN_SPEED = 6f
        private const val ORB_MAX_SPEED = 10f

        private const val LAUNCH_HAND_DELAY = 3f
        private const val HAND_RADIUS = 2f
        private const val HAND_ROTATION_MIN_SPEED = 5f
        private const val HAND_ROTATION_MAX_SPEED = 8f
        private const val HAND_LAUNCH_MIN_SPEED = 8f
        private const val HAND_LAUNCH_MAX_SPEED = 12f
        private const val CLOSEST_HAND_CHANCE = 75

        private const val FALL_GRAVITY = 0.1f
        private const val FALL_IMPULSE = 10f

        private const val SHAKE_X = 0f
        private const val SHAKE_Y = 0.005f
        private const val SHAKE_DUR = 1f
        private const val SHAKE_INTERVAL = 0.1f

        private var headAnimDefs = orderedMapOf(
            "stunned" pairTo AnimationDef(2, 1, 0.1f, true),
            "defeated" pairTo AnimationDef(2, 1, 0.05f, true),
            "down" pairTo AnimationDef(2, 1, gdxArrayOf(1.5f, 0.15f), true),
            "turn" pairTo AnimationDef(2, 1, gdxArrayOf(1.5f, 0.15f), true),
            "turn_up" pairTo AnimationDef(2, 1, gdxArrayOf(1.5f, 0.15f), true)
        )
        private var regions = ObjectMap<String, TextureRegion>()
    }

    override val damageNegotiator = StandardDamageNegotiator(
        objectMapOf(
            Bullet::class pairTo dmgNeg(1),
            ChargedShot::class pairTo dmgNeg {
                it as ChargedShot
                if (it.fullyCharged) 3 else 2
            },
            ChargedShotExplosion::class pairTo dmgNeg {
                it as ChargedShotExplosion
                if (it.fullyCharged) 2 else 1
            }
        )
    )
    override val invincible: Boolean
        get() = super.invincible || stunned
    override lateinit var facing: Facing

    private var fallTargetY = 0f
    private var wasFallTargetReached = false
    private val fallTargetReached: Boolean
        get() = body.getBounds().getY() <= fallTargetY

    private val allHands = Array<BigAssMaverickRobotHand>(2)
    private var launchedHand: BigAssMaverickRobotHand? = null

    private var bigAssBody: BigAssMaverickRobotBody? = null

    private val initTimer = Timer(INIT_DUR)
    private val shakeTimer = Timer(SHAKE_DUR)
    private val stunnedTimer = Timer(STUNNED_DUR)
    private val stunned: Boolean
        get() = !stunnedTimer.isFinished()

    private val shootOrbsDelay = Timer(SHOOT_ORBS_DELAY)
    private val shootOrbsTimer = Timer(SHOOT_ORBS_DUR).also { timer ->
        for (i in 1..ORBS_COUNT) {
            val time = i * DELAY_BETWEEN_ORBS
            val runnable = TimeMarkedRunnable(time) { shootOrb() }
            timer.addRunnable(runnable)
        }
    }
    private val launchHandDelay = Timer(LAUNCH_HAND_DELAY)

    private val turnHeadDelay = Timer(TURN_HEAD_DELAY)
    private val headAngleIndicesSorted = PriorityQueue<Int> comparator@{ index1, index2 ->
        val headAnglesArray = if (isFacing(Facing.LEFT)) HEAD_ANGLES_LEFT else HEAD_ANGLES_RIGHT

        val angle1 = headAnglesArray[index1]
        val angle2 = headAnglesArray[index2]

        val angleToMegaman =
            body.getPositionPoint(Position.TOP_CENTER).sub(megaman.body.getCenter()).nor().angleDeg() + 90f
        val valToCompare1 = abs(angle1 - angleToMegaman)
        var valToCompare2 = abs(angle2 - angleToMegaman)

        return@comparator valToCompare1.compareTo(valToCompare2)
    }

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_3.source)
            headAnimDefs.keys().forEach { key -> regions.put(key, atlas.findRegion("$TAG/$HEAD/$key")) }
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.MUSIC, MusicAsset.MMX7_BOSS_FIGHT_MUSIC.name)
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        gdxArrayOf(
            ConstKeys.LEFT,
            ConstKeys.RIGHT,
            "${ConstKeys.LEFT}_${ConstKeys.ARM}_${ConstKeys.ORIGIN}",
            "${ConstKeys.RIGHT}_${ConstKeys.ARM}_${ConstKeys.ORIGIN}"
        ).forEach { key -> putProperty(key, spawnProps.get(key)) }

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)

        val bigAssBody = MegaEntityFactory.fetch(BigAssMaverickRobotBody::class)!!
        bigAssBody.spawn(props(ConstKeys.BOUNDS pairTo body.getBounds()))
        this.bigAssBody = bigAssBody

        fallTargetY = spawnProps.get(
            ConstKeys.TARGET, RectangleMapObject::class
        )!!.rectangle.getPositionPoint(Position.BOTTOM_CENTER).y

        wasFallTargetReached = false

        body.physics.gravityOn = true
        body.physics.gravity.y = -FALL_GRAVITY * ConstVals.PPM
        body.physics.velocity.y = -FALL_IMPULSE * ConstVals.PPM

        initTimer.reset()
        shakeTimer.reset()
        shootOrbsDelay.reset()
        launchHandDelay.reset()

        stunnedTimer.setToEnd()
        turnHeadDelay.setToEnd()
        shootOrbsTimer.setToEnd()

        facing = if (megaman.body.getCenter().x < body.getCenter().x) Facing.LEFT else Facing.RIGHT

        spawnProps.forEach { key, value ->
            if (key.toString().contains(ConstKeys.PIECE) && value is RectangleMapObject) {
                val center = value.rectangle.getCenter()
                BreakableBlock.breakApart(center, BlockPieceColor.BROWN)
            }
        }
        requestToPlaySound(SoundAsset.THUMP_SOUND, false)
        requestToPlaySound(SoundAsset.SHAKE_SOUND, false)
    }

    override fun preReady(delta: Float) {
        if (fallTargetReached) {
            if (!wasFallTargetReached) {
                GameLogger.debug(TAG, "preReady(): just reached fall target")
                spawnHands()
                shakeRoom()
            }

            shakeTimer.update(delta)
            if (shakeTimer.isFinished()) initTimer.update(delta)
            if (shakeTimer.isJustFinished()) GameLogger.debug(TAG, "preReady(): shake timer just finished")

            body.setY(fallTargetY)
            body.physics.gravity.y = 0f
            body.physics.gravityOn = false
            body.physics.velocity.setZero()
        }

        wasFallTargetReached = fallTargetReached
    }

    private fun shakeRoom() {
        GameLogger.debug(TAG, "shakeRoom()")

        game.eventsMan.submitEvent(
            Event(
                EventType.SHAKE_CAM, props(
                    ConstKeys.DURATION pairTo SHAKE_DUR,
                    ConstKeys.INTERVAL pairTo SHAKE_INTERVAL,
                    ConstKeys.X pairTo SHAKE_X * ConstVals.PPM,
                    ConstKeys.Y pairTo SHAKE_Y * ConstVals.PPM
                )
            )
        )

        requestToPlaySound(SoundAsset.QUAKE_SOUND, false)
    }

    private fun spawnHands() {
        GameLogger.debug(TAG, "spawnHands()")

        val leftHandPosition = removeProperty(ConstKeys.LEFT, RectangleMapObject::class)!!.rectangle.getCenter()
        val leftArmOrigin = removeProperty(
            "${ConstKeys.LEFT}_${ConstKeys.ARM}_${ConstKeys.ORIGIN}", RectangleMapObject::class
        )!!.rectangle.getCenter()
        val leftLaunchSpeedSupplier: () -> Float = speed@{
            return@speed ConstVals.PPM * UtilMethods.interpolate(
                HAND_LAUNCH_MIN_SPEED, HAND_LAUNCH_MAX_SPEED, 1f - getHealthRatio()
            )
        }
        val leftRotationSpeedSupplier: () -> Float = speed@{
            return@speed ConstVals.PPM * UtilMethods.interpolate(
                -HAND_ROTATION_MIN_SPEED, -HAND_ROTATION_MAX_SPEED, 1f - getHealthRatio()
            )
        }
        val leftHand = MegaEntityFactory.fetch(BigAssMaverickRobotHand::class)!!
        leftHand.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.ORIGIN pairTo leftHandPosition,
                ConstKeys.RADIUS pairTo HAND_RADIUS * ConstVals.PPM,
                "${ConstKeys.ARM}_${ConstKeys.ORIGIN}" pairTo leftArmOrigin,
                "${ConstKeys.LAUNCH}_${ConstKeys.SPEED}_${ConstKeys.SUPPLIER}" pairTo leftLaunchSpeedSupplier,
                "${ConstKeys.ROTATION}_${ConstKeys.SPEED}_${ConstKeys.SUPPLIER}" pairTo leftRotationSpeedSupplier
            )
        )
        allHands.add(leftHand)

        val rightHandPosition = removeProperty(ConstKeys.RIGHT, RectangleMapObject::class)!!.rectangle.getCenter()
        val rightArmOrigin = removeProperty(
            "${ConstKeys.RIGHT}_${ConstKeys.ARM}_${ConstKeys.ORIGIN}", RectangleMapObject::class
        )!!.rectangle.getCenter()
        val rightLaunchSpeedSupplier: () -> Float = speed@{
            return@speed ConstVals.PPM * UtilMethods.interpolate(
                HAND_LAUNCH_MIN_SPEED, HAND_LAUNCH_MAX_SPEED, 1f - getHealthRatio()
            )
        }
        val rightRotationSpeedSupplier: () -> Float = speed@{
            return@speed ConstVals.PPM * UtilMethods.interpolate(
                HAND_ROTATION_MIN_SPEED, HAND_ROTATION_MAX_SPEED, 1f - getHealthRatio()
            )
        }
        val rightHand = MegaEntityFactory.fetch(BigAssMaverickRobotHand::class)!!
        rightHand.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.ORIGIN pairTo rightHandPosition,
                ConstKeys.RADIUS pairTo HAND_RADIUS * ConstVals.PPM,
                "${ConstKeys.ARM}_${ConstKeys.ORIGIN}" pairTo rightArmOrigin,
                "${ConstKeys.LAUNCH}_${ConstKeys.SPEED}_${ConstKeys.SUPPLIER}" pairTo rightLaunchSpeedSupplier,
                "${ConstKeys.ROTATION}_${ConstKeys.SPEED}_${ConstKeys.SUPPLIER}" pairTo rightRotationSpeedSupplier
            )
        )
        allHands.add(rightHand)
    }

    override fun isReady(delta: Float) = fallTargetReached && initTimer.isFinished()

    override fun onReady() {
        GameLogger.debug(TAG, "onReady()")
        super.onReady()
    }

    override fun spawnExplosionOrbs(spawn: Vector2) {
        val center = body.fixtures.get(FixtureType.BODY).first().getShape().getCenter()
        GameLogger.debug(TAG, "spawnExplosionOrbs(): center=$center")
        super.spawnExplosionOrbs(center)
    }

    override fun spawnDefeatExplosion() {
        GameLogger.debug(TAG, "spawnDefeatExplosion()")
        super.spawnDefeatExplosion()

        val position = GameObjectPools.fetch(Vector2::class)
        body.fixtures[FixtureType.DAMAGEABLE].first()
            .getShape()
            .getBoundingRectangle()
            .getRandomPositionInBounds(position)

        val explosion = MegaEntityFactory.fetch(Explosion::class)!!
        explosion.spawn(
            props(
                ConstKeys.DAMAGER pairTo false,
                ConstKeys.POSITION pairTo position,
                ConstKeys.SOUND pairTo SoundAsset.EXPLOSION_2_SOUND
            )
        )
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        // big ass body should handle destroying itself
        bigAssBody = null

        allHands.forEach { it.destroy() }
        allHands.clear()

        launchedHand = null

        headAngleIndicesSorted.clear()
    }

    override fun takeDamageFrom(damager: IDamager): Boolean {
        GameLogger.debug(TAG, "takeDamageFrom(): damager=$damager")
        val damaged = super.takeDamageFrom(damager)
        if (damaged && damager is ChargedShot && damager.fullyCharged) stunnedTimer.reset()
        return damaged
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            bigAssBody?.bounds?.setCenter(body.getCenter())

            if (defeated) {
                explodeOnDefeat(delta)
                return@add
            }

            if (!initTimer.isFinished()) return@add

            stunnedTimer.update(delta)
            if (!stunnedTimer.isFinished()) return@add
            if (stunnedTimer.isJustFinished()) {
                GameLogger.debug(TAG, "update(): stunned timer just finished")
                damageTimer.reset()
            }

            if (!stunned) facing = if (megaman.body.getCenter().x < body.getCenter().x) Facing.LEFT else Facing.RIGHT

            turnHeadDelay.update(delta)

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
                    val handToLaunch = allHands.filter { !it.isBeingStoodUpon() }.let { hands ->
                        when {
                            hands.isEmpty -> throw IllegalStateException(
                                "Megaman cannot be standing on both hands at the same time when calculating " + "which hand to launch. The hands need to be spaced more apart."
                            )

                            hands.size == 1 -> hands[0]

                            getRandom(0, 100) <= CLOSEST_HAND_CHANCE -> {
                                hands.sort { h1, h2 ->
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
            } else if (launchedHand!!.state == BigAssMaverickRobotHandState.ROTATE) {
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

        val circle = GameCircle().setRadius(HEAD_DAMAGE_RADIUS * ConstVals.PPM)
        val offsetY = (body.getHeight() / 2f) - (2.5f * ConstVals.PPM)

        val headDamageable = Fixture(body, FixtureType.DAMAGEABLE, circle.copy())
        headDamageable.offsetFromBodyAttachment.y = offsetY
        body.addFixture(headDamageable)
        headDamageable.drawingColor = Color.PURPLE
        debugShapes.add { headDamageable }

        val headFixture = Fixture(body, FixtureType.BODY, GameCircle().setRadius(HEAD_RADIUS * ConstVals.PPM))
        headFixture.offsetFromBodyAttachment.y = offsetY
        body.addFixture(headFixture)
        headFixture.drawingColor = Color.DARK_GRAY
        debugShapes.add { headFixture }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(
            HEAD, GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, -2))
                .also { sprite -> sprite.setSize(HEAD_SPRITE_SIZE * ConstVals.PPM) }
        ).updatable { _, sprite ->
            val position = Position.TOP_CENTER
            sprite.setPosition(body.getPositionPoint(position), position)
            sprite.hidden = damageBlink && !defeated && !stunned
            sprite.setFlip(isFacing(Facing.LEFT), false)
        }
        .build()

    private fun defineAnimationsComponent() =
        AnimationsComponentBuilder(this)
            .key(HEAD)
            .animator(
                AnimatorBuilder()
                    .setKeySupplier keySupplier@{ oldKey ->
                        when {
                            defeated -> {
                                turnHeadDelay.setToEnd()
                                return@keySupplier "defeated"
                            }

                            stunned -> {
                                turnHeadDelay.setToEnd()
                                return@keySupplier "stunned"
                            }

                            !turnHeadDelay.isFinished() -> oldKey

                            else -> {
                                (0 until HEAD_ANGLES_SIZE).forEach { headAngleIndicesSorted.add(it) }

                                val index = headAngleIndicesSorted.peek()

                                headAngleIndicesSorted.clear()

                                val key = when (index) {
                                    0 -> "down"
                                    1 -> "turn"
                                    else -> "turn_up"
                                }

                                if (key != oldKey) turnHeadDelay.reset()

                                return@keySupplier key
                            }
                        }
                    }
                    .applyToAnimations { animations ->
                        headAnimDefs.forEach { entry ->
                            val key = entry.key
                            val (rows, columns, durations, loop) = entry.value
                            animations.put(key, Animation(regions[key], rows, columns, durations, loop))
                        }
                    }
                    .build()
            )
            .build()

    private fun shootOrb() {
        GameLogger.debug(TAG, "shootOrb()")

        val spawn = body.getPositionPoint(Position.TOP_CENTER).sub(0f, 1.25f * ConstVals.PPM)

        val speed = UtilMethods.interpolate(ORB_MIN_SPEED, ORB_MAX_SPEED, 1f - getHealthRatio())
        val trajectory = megaman.body.getCenter().sub(spawn).nor().scl(speed * ConstVals.PPM)

        val delay = UtilMethods.interpolate(ORB_MIN_MOVE_DELAY, ORB_MAX_MOVE_DELAY, 1f - getHealthRatio())

        val orb = MegaEntityFactory.fetch(BigAssMaverickRobotOrb::class)!!
        orb.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.DELAY pairTo delay,
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.TRAJECTORY pairTo trajectory
            )
        )

        // requestToPlaySound(SoundAsset.BLAST_2_SOUND, false)

        GameLogger.debug(TAG, "shootOrb(): spawn=$spawn, trajectory=$trajectory")
    }

    override fun getTag() = TAG
}

