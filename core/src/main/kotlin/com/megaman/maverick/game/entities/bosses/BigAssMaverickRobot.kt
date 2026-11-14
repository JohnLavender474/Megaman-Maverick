package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.animations.AnimationsComponentBuilder
import com.maverick.game.engine.animations.AnimatorBuilder
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.UtilMethods
import com.maverick.game.engine.common.UtilMethods.getRandom
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.enums.Position
import com.maverick.game.engine.common.extensions.*
import com.maverick.game.engine.common.interfaces.IFaceable
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameCircle
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.shapes.IDrawableShape
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponentBuilder
import com.maverick.game.engine.drawables.sprites.setPosition
import com.maverick.game.engine.drawables.sprites.setSize
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.entities.contracts.IAnimatedEntity
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.players.IPlayer
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.IDamageNegotiator
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.difficulty.DifficultyMode
import com.megaman.maverick.game.drawables.ui.BossBitsBar
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.blocks.BreakableBlock
import com.megaman.maverick.game.entities.bosses.bigassmaverickrobot.BigAssMaverickRobotMotion
import com.megaman.maverick.game.entities.bosses.bigassmaverickrobot.BigAssMaverickRobotTuning
import com.megaman.maverick.game.entities.bosses.bigassmaverickrobot.ClapAim
import com.megaman.maverick.game.entities.bosses.bigassmaverickrobot.ClapPlan
import com.megaman.maverick.game.entities.bosses.bigassmaverickrobot.HandAttack
import com.megaman.maverick.game.entities.bosses.bigassmaverickrobot.HandChoreography
import com.megaman.maverick.game.entities.bosses.bigassmaverickrobot.HandSide
import com.megaman.maverick.game.entities.bosses.bigassmaverickrobot.HandState
import com.megaman.maverick.game.entities.bosses.bigassmaverickrobot.MotionMode
import com.megaman.maverick.game.entities.bosses.bigassmaverickrobot.OrbAim
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.contracts.targetPlayer
import com.megaman.maverick.game.entities.decorations.BlockPieceColor
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.explosions.Explosion
import com.megaman.maverick.game.entities.explosions.IceBombExplosion
import com.megaman.maverick.game.entities.explosions.MagmaExplosion
import com.megaman.maverick.game.entities.explosions.ReactorExplosion
import com.megaman.maverick.game.entities.hazards.MagmaFlame
import com.megaman.maverick.game.entities.hazards.SmallIceCube
import com.megaman.maverick.game.entities.players.PlayerSabreSlash
import com.megaman.maverick.game.entities.projectiles.Axe
import com.megaman.maverick.game.entities.projectiles.BigAssMaverickRobotOrb
import com.megaman.maverick.game.entities.projectiles.BigAssMaverickRobotOrb.OrbColor
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.entities.projectiles.MagmaWave
import com.megaman.maverick.game.entities.projectiles.MoonScythe
import com.megaman.maverick.game.entities.projectiles.Needle
import com.megaman.maverick.game.entities.projectiles.PreciousGem
import com.megaman.maverick.game.entities.projectiles.PreciousShard
import com.megaman.maverick.game.entities.projectiles.ProtoBuster
import com.megaman.maverick.game.entities.projectiles.ReactorManProjectile
import com.megaman.maverick.game.entities.projectiles.SlashWave
import com.megaman.maverick.game.entities.state.BossPhase
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
import kotlin.reflect.KClass

class BigAssMaverickRobot(services: MegaServices) : AbstractBoss(services, defeatDur = DEFEAT_DUR), IAnimatedEntity,
    IFaceable {

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

        private const val STUNNED_DUR = 0.5f

        const val LEFT_WALL = "${ConstKeys.LEFT}_${ConstKeys.WALL}"
        const val RIGHT_WALL = "${ConstKeys.RIGHT}_${ConstKeys.WALL}"
        const val CLAPS = 2
        const val CLAP_SHAKE_DUR = 0.2f

        private const val FALL_GRAVITY = 15.0f
        private const val FALL_IMPULSE = 10f

        private const val SHAKE_X = 0f
        private const val SHAKE_Y = 0.005f
        private const val SHAKE_DUR = 1f
        private const val SHAKE_INTERVAL = 0.1f

        const val PHASE_SHAKE_DUR = 0.5f
        const val PHASE_FLASH_DUR = 0.6f

        const val DEFEAT_DUR = 4f
        const val DEFEAT_SHAKE_END = 1.5f
        const val DEFEAT_SINK_END = 3.5f
        const val DEFEAT_SINK = 1f
        const val DEFEAT_SHAKE = 2f
        const val DEFEAT_BLINK = 0.05f
        const val LAST_BLAST_SPREAD = 0.75f

        private var headAnimDefs = orderedMapOf(
            "defeated" pairTo AnimationDef(2, 1, 0.1f, true),
            "stunned" pairTo AnimationDef(2, 1, 0.1f, true),
            "down" pairTo AnimationDef(2, 1, gdxArrayOf(1.5f, 0.15f), true),
            "turn" pairTo AnimationDef(2, 1, gdxArrayOf(1.5f, 0.15f), true),
            "turn_up" pairTo AnimationDef(2, 1, gdxArrayOf(1.5f, 0.15f), true)
        )
        private var regions = ObjectMap<String, TextureRegion>()

        private val DAMAGE = objectMapOf<KClass<out IDamagerEntity>, DamageNegotiation>(
            Bullet::class pairTo dmgNeg(3),
            ProtoBuster::class pairTo dmgNeg(4),
            PlayerSabreSlash::class pairTo dmgNeg {
                it as PlayerSabreSlash
                if (it.fullStrength) 6 else 4
            },
            ChargedShot::class pairTo dmgNeg {
                it as ChargedShot
                if (it.fullyCharged) 6 else 4
            },
            ChargedShotExplosion::class pairTo dmgNeg {
                it as ChargedShotExplosion
                if (it.fullyCharged) 4 else 3
            },
            SmallIceCube::class pairTo dmgNeg(3),
            IceBombExplosion::class pairTo dmgNeg(2),
            MagmaWave::class pairTo dmgNeg(3),
            Fireball::class pairTo dmgNeg(3),
            MagmaExplosion::class pairTo dmgNeg(2),
            MagmaFlame::class pairTo dmgNeg(2),
            MoonScythe::class pairTo dmgNeg(3),
            PreciousGem::class pairTo dmgNeg(3),
            PreciousShard::class pairTo dmgNeg(2),
            Axe::class pairTo dmgNeg(4),
            Needle::class pairTo dmgNeg(3),
            SlashWave::class pairTo dmgNeg(3),
            ReactorManProjectile::class pairTo dmgNeg {
                it as ReactorManProjectile
                if (it.big) 6 else 3
            },
            ReactorExplosion::class pairTo dmgNeg(3)
        )
    }

    override val damageNegotiator: IDamageNegotiator = object : BossDamageNegotiator() {
        override fun get(damager: IDamagerEntity) = DAMAGE[damager::class]?.get(damager) ?: super.get(damager)
    }
    override val healthPhases = 2
    override val healthBarRegions = gdxArrayOf(ConstKeys.STANDARD, BossBitsBar.PHASE_2_BITS)
    override val invincible: Boolean
        get() = super.invincible || stunned
    override lateinit var facing: Facing

    internal val tuning: BigAssMaverickRobotTuning
        get() = BigAssMaverickRobotTuning.of(services.gameState.getDifficultyMode() == DifficultyMode.HARD, healthPhase)
    internal val arena = GameRectangle()
    internal val motionMode: MotionMode
        get() = motion.mode

    private var fallTargetY = 0f
    private var wasFallTargetReached = false
    private val fallTargetReached: Boolean
        get() = body.getBounds().getY() <= fallTargetY

    private val allHands = Array<BigAssMaverickRobotHand>()
    private val freeHands = Array<BigAssMaverickRobotHand>()
    private var attack: HandAttack? = null
    private val motion = BigAssMaverickRobotMotion()

    private var attacker: BigAssMaverickRobotHand? = null
    private var partner: BigAssMaverickRobotHand? = null
    private var attackTuning = BigAssMaverickRobotTuning.NORMAL
    private val attackWait = Timer()
    private var handTarget: IPlayer? = null
    private val clapPlan = ClapPlan()
    private var clapsDone = 0

    private var volleyTuning = BigAssMaverickRobotTuning.NORMAL
    private var volleyRunning = false
    private var volleyTime = 0f
    private var orbsShot = 0
    private val volleyDelay = Timer()
    private var orbTarget: IPlayer? = null

    private var bigAssBody: BigAssMaverickRobotBody? = null

    private val shakeTimer = Timer(SHAKE_DUR)
    private val stunnedTimer = Timer(STUNNED_DUR)
    private val stunned: Boolean
        get() = !stunnedTimer.isFinished()
    private val phaseFlashTimer = Timer(PHASE_FLASH_DUR)

    private var defeatTime = 0f
    private var lastBlast = false

    private val turnHeadDelay = Timer(TURN_HEAD_DELAY)
    private val headAngleIndicesSorted = PriorityQueue<Int> comparator@{ index1, index2 ->
        val headAnglesArray = if (isFacing(Facing.LEFT)) HEAD_ANGLES_LEFT else HEAD_ANGLES_RIGHT

        val angle1 = headAnglesArray[index1]
        val angle2 = headAnglesArray[index2]

        val angleToMegaman =
            body.getPositionPoint(Position.TOP_CENTER).sub(targetPlayer.body.getCenter()).nor().angleDeg() + 90f
        val valToCompare1 = abs(angle1 - angleToMegaman)
        var valToCompare2 = abs(angle2 - angleToMegaman)

        return@comparator valToCompare1.compareTo(valToCompare2)
    }

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = services.assets.getTextureAtlas(TextureAsset.BOSSES_3.source)
            headAnimDefs.keys().forEach { key -> regions.put(key, atlas.findRegion("$TAG/$HEAD/$key")) }
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)

        val bigAssBody = MegaEntityFactory.fetch(BigAssMaverickRobotBody::class)!!
        bigAssBody.spawn(props(ConstKeys.BOUNDS pairTo body.getBounds(), ConstKeys.OWNER pairTo this))
        this.bigAssBody = bigAssBody

        fallTargetY = spawnProps.get(
            ConstKeys.TARGET, RectangleMapObject::class
        )!!.rectangle.getPositionPoint(Position.BOTTOM_CENTER).y

        val left = spawnProps.get(LEFT_WALL, RectangleMapObject::class)!!.rectangle
        val right = spawnProps.get(RIGHT_WALL, RectangleMapObject::class)!!.rectangle
        val ground = spawnProps.get(ConstKeys.GROUND, RectangleMapObject::class)!!.rectangle
        val ceiling = spawnProps.get(ConstKeys.CEILING, RectangleMapObject::class)!!.rectangle
        val floor = ground.y + ground.height
        arena.set(left.x + left.width, floor, right.x - (left.x + left.width), ceiling.y - floor)

        wasFallTargetReached = false

        body.physics.gravityOn = true
        body.physics.gravity.y = -FALL_GRAVITY * ConstVals.PPM
        body.physics.velocity.y = -FALL_IMPULSE * ConstVals.PPM

        shakeTimer.reset()

        motion.reset()
        attack = null
        attacker = null
        partner = null
        handTarget = null
        orbTarget = null
        clapsDone = 0
        attackTuning = tuning
        attackWait.resetDuration(attackTuning.attackWait)

        volleyTuning = tuning
        volleyRunning = false
        volleyTime = 0f
        orbsShot = 0
        volleyDelay.resetDuration(volleyTuning.volleyDelay)

        stunnedTimer.setToEnd()
        phaseFlashTimer.setToEnd()
        turnHeadDelay.setToEnd()
        defeatTime = 0f
        lastBlast = false

        facing = if (targetPlayer.body.getCenter().x < body.getCenter().x) Facing.LEFT else Facing.RIGHT

        spawnProps.forEach { key, value ->
            if (key.toString().contains(ConstKeys.PIECE) && value is RectangleMapObject) {
                val center = value.rectangle.getCenter()
                BreakableBlock.breakApart(center, BlockPieceColor.BROWN)
            }
        }

        spawnHands(spawnProps)

        requestToPlaySound(SoundAsset.THUMP_SOUND, false)
        requestToPlaySound(SoundAsset.SHAKE_SOUND, false)
    }

    override fun preReady(delta: Float) {
        if (fallTargetReached) {
            if (!wasFallTargetReached) {
                GameLogger.debug(TAG, "preReady(): just reached fall target")
                allHands.forEach { it.moveToOrigin() }
                shakeRoom(SHAKE_DUR)
            }

            shakeTimer.update(delta)
            if (shakeTimer.isJustFinished()) GameLogger.debug(TAG, "preReady(): shake timer just finished")

            body.setY(fallTargetY)
            body.physics.gravity.y = 0f
            body.physics.gravityOn = false
            body.physics.velocity.setZero()
        }

        wasFallTargetReached = fallTargetReached
    }

    private fun shakeRoom(duration: Float) {
        GameLogger.debug(TAG, "shakeRoom(): duration=$duration")
        shakeCamera(duration)
        requestToPlaySound(SoundAsset.QUAKE_SOUND, false)
    }

    private fun shakeCamera(duration: Float) =
        services.events.submitEvent(
            Event(
                EventType.SHAKE_CAM, props(
                    ConstKeys.DURATION pairTo duration,
                    ConstKeys.INTERVAL pairTo SHAKE_INTERVAL,
                    ConstKeys.X pairTo SHAKE_X * ConstVals.PPM,
                    ConstKeys.Y pairTo SHAKE_Y * ConstVals.PPM
                )
            )
        )

    private fun spawnHands(spawnProps: Properties) {
        GameLogger.debug(TAG, "spawnHands()")
        spawnHand(spawnProps, HandSide.LEFT, ConstKeys.LEFT)
        spawnHand(spawnProps, HandSide.RIGHT, ConstKeys.RIGHT)
    }

    private fun spawnHand(spawnProps: Properties, side: HandSide, key: String) {
        val origin = spawnProps.get(key, RectangleMapObject::class)!!.rectangle.getCenter()
        val armOrigin = spawnProps.get(
            "${key}_${ConstKeys.ARM}_${ConstKeys.ORIGIN}", RectangleMapObject::class
        )!!.rectangle.getCenter()
        val hand = MegaEntityFactory.fetch(BigAssMaverickRobotHand::class)!!
        hand.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.SIDE pairTo side,
                ConstKeys.ORIGIN pairTo origin,
                "${ConstKeys.ARM}_${ConstKeys.ORIGIN}" pairTo armOrigin
            )
        )
        allHands.add(hand)
    }

    internal fun slotOf(origin: Vector2, side: HandSide, out: Vector2) =
        motion.slot(origin, side, tuning.hoverAmplitudeScale, out)

    internal fun flashAt(point: Vector2) {
        val flash = MegaEntityFactory.fetch(BigAssMaverickRobotOrb::class)!!
        flash.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.POSITION pairTo point,
                ConstKeys.ACTIVE pairTo false,
                ConstKeys.HIT pairTo true,
                ConstKeys.CULL_OUT_OF_BOUNDS pairTo false,
                ConstKeys.COLOR pairTo OrbColor.ORANGE
            )
        )
        requestToPlaySound(SoundAsset.SMB3_BUMP_SOUND, loop = false)
    }

    override fun isReady(delta: Float) =
        fallTargetReached && !allHands.isEmpty && allHands.all { it.state == HandState.IDLE }

    override fun onReady() {
        GameLogger.debug(TAG, "onReady()")
        super.onReady()
    }

    override fun onHealthPhaseStart(phase: Int) {
        GameLogger.debug(TAG, "onHealthPhaseStart(): phase=$phase")
        shakeRoom(PHASE_SHAKE_DUR)
        phaseFlashTimer.reset()
        allHands.forEach { it.setArmOrbColor(OrbColor.ORANGE) }
    }

    override fun triggerDefeat() {
        super.triggerDefeat()
        attack = null
        defeatTime = 0f
        lastBlast = false
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

    private fun spawnLastBlast() {
        GameLogger.debug(TAG, "spawnLastBlast()")
        val center = body.fixtures[FixtureType.DAMAGEABLE].first().getShape().getCenter()
        val spread = LAST_BLAST_SPREAD * ConstVals.PPM
        for (i in -1..1) for (j in -1..1) {
            if (i != 0 && j != 0) continue
            val explosion = MegaEntityFactory.fetch(Explosion::class)!!
            explosion.spawn(
                props(
                    ConstKeys.DAMAGER pairTo false,
                    ConstKeys.POSITION pairTo GameObjectPools.fetch(Vector2::class)
                        .set(center.x + i * spread, center.y + j * spread)
                )
            )
        }
        requestToPlaySound(SoundAsset.BASSY_BLAST_SOUND, loop = false)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        bigAssBody = null

        allHands.forEach { it.destroy() }
        allHands.clear()

        attack = null
        attacker = null
        partner = null
        handTarget = null
        orbTarget = null

        headAngleIndicesSorted.clear()
    }

    override fun takeDamageFrom(damager: IDamagerEntity): Boolean {
        GameLogger.debug(TAG, "takeDamageFrom(): damager=$damager")
        val damaged = super.takeDamageFrom(damager)
        if (damaged && damager is ChargedShot && damager.fullyCharged) stunnedTimer.reset()
        return damaged
    }

    private fun orbTrajectory(aim: OrbAim, target: IPlayer, from: Vector2, out: Vector2): Vector2 {
        val point = OrbAim.point(
            aim,
            target.body.getCenter(),
            target.body.physics.velocity,
            arena,
            { min, max -> getRandom(min, max) },
            GameObjectPools.fetch(Vector2::class)
        )
        val speed = UtilMethods.interpolate(volleyTuning.orbMinSpeed, volleyTuning.orbMaxSpeed, 1f - getHealthRatio())
        return out.set(point).sub(from).nor().scl(speed * ConstVals.PPM)
    }

    private fun nextTarget(last: IPlayer?): IPlayer {
        var first: IPlayer? = null
        var next: IPlayer? = null
        players.forEachLive { player ->
            if (first == null) first = player
            if (next == null && last != null && player.slot.index > last.slot.index) next = player
        }
        return next ?: first ?: targetPlayer
    }

    private fun nearestHand(free: Array<BigAssMaverickRobotHand>, target: IPlayer): BigAssMaverickRobotHand {
        val point = target.body.getCenter()
        var nearest = free[0]
        for (i in 1 until free.size)
            if (free[i].body.getCenter().dst2(point) < nearest.body.getCenter().dst2(point)) nearest = free[i]
        return nearest
    }

    private fun orderAttack() {
        val tuning = tuning
        val target = nextTarget(handTarget)

        val free = freeHands
        free.clear()
        allHands.forEach { if (it.state == HandState.IDLE && !it.isBeingStoodUpon()) free.add(it) }

        val total = tuning.totalWeight(free.size)
        val kind = if (total <= 0) null else tuning.attackFor(getRandom(0, total - 1), free.size)
        if (kind == null) {
            attackWait.resetDuration(tuning.attackWait)
            return
        }

        GameLogger.debug(TAG, "orderAttack(): kind=$kind, target=$target, free=${free.size}")
        handTarget = target
        attackTuning = tuning
        attack = kind
        val center = target.body.getCenter()
        when (kind) {
            HandAttack.PUNCH -> {
                val hand = nearestHand(free, target)
                attacker = hand
                hand.punch(center, tuning)
            }

            HandAttack.SLAM -> {
                val hand = nearestHand(free, target)
                attacker = hand
                hand.slam(center, tuning)
            }

            HandAttack.PINCER_CLAP -> {
                attacker = allHands.first { it.side == HandSide.LEFT }
                clapsDone = 0
                startClap(target)
            }
        }
        partner = allHands.first { it != attacker }
    }

    private fun startClap(target: IPlayer) {
        val fist = allHands.first().body
        ClapAim.plan(
            target.body.getCenter(),
            arena,
            fist.getWidth() / 2f,
            fist.getHeight() / 2f - BigAssMaverickRobotHand.FEET_OVER_BOTTOM * ConstVals.PPM,
            clapPlan
        )
        GameLogger.debug(TAG, "startClap(): meet=${clapPlan.meet}")
        allHands.forEach {
            if (it.side == HandSide.LEFT) it.clap(clapPlan.leftStart, clapPlan.leftStop, attackTuning)
            else it.clap(clapPlan.rightStart, clapPlan.rightStop, attackTuning)
        }
    }

    private fun conductClap() {
        if (!allHands.all { it.clapStepDone }) return
        if (allHands.first().state == HandState.CLAP_IMPACT) {
            clapsDone++
            if (clapsDone < CLAPS) startClap(handTarget!!) else allHands.forEach { it.returnToSlot() }
            return
        }
        allHands.forEach { it.advanceClap() }
        if (allHands.first().state == HandState.CLAP_IMPACT) {
            flashAt(GameObjectPools.fetch(Vector2::class).set(clapPlan.meet))
            shakeCamera(CLAP_SHAKE_DUR)
        }
    }

    private fun conductAttack() {
        val attacker = attacker!!
        val partner = partner!!
        when (attack!!) {
            HandAttack.PUNCH, HandAttack.SLAM -> {
                if (partner.state == HandState.IDLE) {
                    val towardHead = if (body.getCenter().x > partner.body.getCenter().x) 1f else -1f
                    HandChoreography.partnerOffset(
                        attacker.state,
                        attacker.stateTime,
                        attacker.stateDuration,
                        attacker.attackDirection,
                        towardHead,
                        partner.isBeingStoodUpon(),
                        attackTuning.partnerScale,
                        partner.partnerOffset
                    )
                } else partner.partnerOffset.setZero()
            }

            HandAttack.PINCER_CLAP -> conductClap()
        }

        if (allHands.all { it.state == HandState.IDLE }) {
            GameLogger.debug(TAG, "conductAttack(): attack $attack done")
            allHands.forEach { it.partnerOffset.setZero() }
            attack = null
            this.attacker = null
            this.partner = null
            attackWait.resetDuration(tuning.attackWait)
        }
    }

    private fun updateVolley(delta: Float) {
        if (volleyRunning) {
            volleyTime += delta
            val aims = volleyTuning.orbAims
            while (orbsShot < aims.size && volleyTime >= (orbsShot + 1) * volleyTuning.orbGap) {
                shootOrb(aims[orbsShot])
                orbsShot++
            }
            if (volleyTime >= volleyTuning.volleyDur) {
                volleyRunning = false
                volleyDelay.resetDuration(volleyTuning.volleyDelay)
            }
        } else {
            volleyDelay.update(delta)
            if (volleyDelay.isFinished()) {
                volleyTuning = tuning
                volleyRunning = true
                volleyTime = 0f
                orbsShot = 0
            }
        }
    }

    private fun handsArrived(): Boolean {
        val slot = GameObjectPools.fetch(Vector2::class)
        val distance = BigAssMaverickRobotMotion.ARRIVED_DISTANCE * ConstVals.PPM
        return allHands.all {
            it.state != HandState.IDLE || it.body.getCenter().dst2(it.slotTarget(slot)) <= distance * distance
        }
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            bigAssBody?.bounds?.setCenter(body.getCenter())

            if (defeated) {
                val before = defeatTime
                defeatTime += delta
                explodeOnDefeat(delta)
                if (before < DEFEAT_SINK_END && defeatTime >= DEFEAT_SINK_END && !lastBlast) {
                    lastBlast = true
                    spawnLastBlast()
                }
                return@add
            }

            phaseFlashTimer.update(delta)

            val fighting = bossComponent.phase == BossPhase.FIGHT
            val attacking = fighting && attack != null
            if (fighting) {
                val tuning = tuning
                val orbitSpeed = tuning.orbitSpeedScale * UtilMethods.interpolate(
                    BigAssMaverickRobotMotion.ORBIT_MIN_SPEED,
                    BigAssMaverickRobotMotion.ORBIT_MAX_SPEED,
                    1f - getHealthRatio()
                )
                motion.update(delta, orbitSpeed, tuning.hoverTimeScale, tuning.schedule, handsArrived())
                if (attacking) conductAttack()
            }

            stunnedTimer.update(delta)
            if (!stunnedTimer.isFinished()) return@add
            if (stunnedTimer.isJustFinished()) {
                GameLogger.debug(TAG, "update(): stunned timer just finished")
                damageTimer.reset()
            }

            if (!stunned) facing = if (targetPlayer.body.getCenter().x < body.getCenter().x) Facing.LEFT else Facing.RIGHT

            turnHeadDelay.update(delta)

            if (fighting) {
                if (attack != HandAttack.PINCER_CLAP) updateVolley(delta)

                if (!attacking) {
                    attackWait.update(delta)
                    if (attackWait.isFinished()) orderAttack()
                }
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
        ).preProcess { _, sprite ->
            val position = Position.TOP_CENTER
            sprite.setPosition(body.getPositionPoint(position), position)
            if (defeated) {
                if (defeatTime < DEFEAT_SHAKE_END) sprite.translateX(HandChoreography.shake(defeatTime) / 2f * DEFEAT_SHAKE)
                val sink = MathUtils.clamp(
                    (defeatTime - DEFEAT_SHAKE_END) / (DEFEAT_SINK_END - DEFEAT_SHAKE_END), 0f, 1f
                )
                sprite.translateY(-sink * DEFEAT_SINK * ConstVals.PPM)
            }
            sprite.hidden = if (defeated) (defeatTime / DEFEAT_BLINK).toInt() % 2 == 1 else damageBlink && !stunned
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

                            stunned || !phaseFlashTimer.isFinished() -> {
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

    private fun shootOrb(aim: OrbAim) {
        val target = nextTarget(orbTarget)
        orbTarget = target
        GameLogger.debug(TAG, "shootOrb(): aim=$aim, target=$target")

        val spawn = body.getPositionPoint(Position.TOP_CENTER).sub(0f, 1.25f * ConstVals.PPM)
        val hold = UtilMethods.interpolate(volleyTuning.orbMinHold, volleyTuning.orbMaxHold, getHealthRatio())

        val orb = MegaEntityFactory.fetch(BigAssMaverickRobotOrb::class)!!
        orb.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.DELAY pairTo hold,
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.SUPPLIER pairTo { from: Vector2, out: Vector2 -> orbTrajectory(aim, target, from, out) },
                ConstKeys.COLOR pairTo OrbColor.ORANGE
            )
        )

        requestToPlaySound(SoundAsset.BLAST_2_SOUND, loop = false)
    }
}
