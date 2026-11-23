package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.enums.Position
import com.maverick.game.engine.common.enums.Size
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.interfaces.IFaceable
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.damage.IDamageableEntity
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.shapes.IDrawableShape
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.locks.lock
import com.maverick.game.engine.locks.unlock
import com.maverick.game.engine.players.IPlayer
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.bosses.timberwalker.PxBox
import com.megaman.maverick.game.entities.bosses.timberwalker.TimberWalkerAnchors
import com.megaman.maverick.game.entities.bosses.timberwalker.TimberWalkerAnimDef
import com.megaman.maverick.game.entities.bosses.timberwalker.TimberWalkerFrames
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.contracts.IProjectileEntity
import com.megaman.maverick.game.entities.contracts.targetPlayer
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.TimberWalkerChipWave
import com.megaman.maverick.game.entities.projectiles.TimberWalkerLog
import com.megaman.maverick.game.entities.state.BossPhase
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.locks.MegaLockGroup
import com.megaman.maverick.game.locks.MegaLockReason
import com.megaman.maverick.game.players.rootPlayer
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.Receivers
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter
import com.megaman.maverick.game.world.body.getPositionPoint
import com.megaman.maverick.game.world.body.setReceiver
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

// TODO: first pass, spawned only in RefactorTest078; art with an artist and the Timber Woman stage place (item 180)
class TimberWalker(services: MegaServices) :
    AbstractBoss(services, dmgDuration = DEFAULT_MINI_BOSS_DMG_DURATION, size = Size.LARGE), IFaceable {

    companion object {
        const val TAG = "TimberWalker"
        const val FAR_START = "far_start"
        private const val LEFT_WALL = "${ConstKeys.LEFT}_${ConstKeys.WALL}"
        private const val RIGHT_WALL = "${ConstKeys.RIGHT}_${ConstKeys.WALL}"

        private const val PX = ConstVals.PPM / 16f
        private const val LEG_WIDTH_PX = 30
        private const val HOME_EPSILON_PX = 1f

        private val FIGHT_FACING = Facing.LEFT

        private const val FAR_WALK_SPEED = 2f
        private const val FAR_JUMP_GAP = 2f
        private const val FAR_CROUCH_DUR = 0.4f
        private const val JUMP_IN_DUR = 1.1f
        private const val JUMP_IN_ARC = 5f
        private const val ROAR_DUR = 1.2f

        private const val IDLE_DUR = 0.7f
        private const val IDLE_DUR_LOW_HEALTH = 0.45f
        private const val WALK_SPEED = 2f
        private const val WALK_RANGE = 2f
        private const val LOW_HEALTH_RATIO = 0.5f
        private const val MAX_SAME_ATTACK_IN_A_ROW = 2

        private const val CHOP_REACH = 7.5f
        private const val CLAW_HIGH_REACH = 7.9f
        private const val REACH_TOLERANCE = 1f
        private const val GRAB_MIN_DIST = 6.5f
        private const val GRAB_MAX_DIST = 9f
        private const val GRAB_MAX_HEIGHT = 1f
        private const val HIGH_PLAYER_HEIGHT = 2.5f
        private const val CHOP_MIN_DIST = 6f
        private const val FAR_PLAYER_DIST = 8.5f

        private const val GUARD_HITS_TO_OPEN = 4
        private const val CHARGED_SHOT_GUARD_HITS = 2
        private const val GUARD_HIT_FLASH_DUR = 0.1f
        private const val MET_STUN_DUR = 2f

        private const val HOP_RISE = 3f
        private const val HOP_AIR_TIME = 1.6f
        private const val HOP_MIN_DIST = 2f
        private const val CHIP_WAVE_SPEED = 8f

        private const val LOG_FLIGHT_TIME = 1f
        private const val LOG_GRAVITY = -25f

        private const val GRIP_EASE_SPEED = 25f
        private const val THROW_IMPULSE_X = 12f
        private const val THROW_IMPULSE_Y = 10f

        private const val DEFEAT_EXPLOSIONS_DUR = 1f

        private const val SHAKE_DUR = 0.5f
        private const val SHAKE_INTERVAL = 0.1f
        private const val SHAKE_X = 0.002f
        private const val SHAKE_Y = 0.01f

        private const val GUARD_KEY = "guard"
        private const val SHADOW_KEY = "shadow"
        private const val GUARD_HIT_REGION = "guard_hit"
        private const val GUARD_CRACK_REGION = "guard_crack_"
        private const val GUARD_CRACK_STAGES = 3
        private const val SHADOW_REGION = "hop_shadow"
        private const val DAMAGED_PREFIX = "damaged_"
        private val STANCE_ANIMATIONS = gdxArrayOf("idle")

        private val regions = ObjectMap<String, TextureRegion>()
    }

    enum class TimberWalkerState {
        FAR_WALK, FAR_CROUCH, JUMP_IN, LAND, TURN, ROAR,
        IDLE, WALK, TELL,
        SAW_THRUST_LOW, SAW_THRUST_LEVEL, SAW_CHOP, LOG_THROW,
        HOP_CROUCH, HOP_AIR, HOP_LAND,
        CLAW_HIGH, CLAW_GRAB, GRAB_MISS, GRAB_THROW,
        GUARD_OPEN, MET_STUNNED, MET_RECOVER, GUARD_CLOSE,
        DEFEAT
    }

    override lateinit var facing: Facing

    var state = TimberWalkerState.FAR_WALK
        private set

    private val animations = ObjectMap<String, Animation>()
    private lateinit var currentDef: TimberWalkerAnimDef
    private lateinit var currentAnim: Animation
    private var currentAnimKey: String? = null
    private var currentFrameIndex = -1
    private var currentFrame = ""
    private var currentMirrored = false
    private var currentRegion: TextureRegion? = null

    private var rearPx = 0
    private var frontPx = 0
    private var tuckGapPx = 0

    private val position = Vector2()
    private val home = Vector2()
    private val farStart = Vector2()
    private val jumpStart = Vector2()
    private val hopVelocity = Vector2()
    private val scratch = Vector2()
    private val holdPoint = Vector2()
    private val clawBounds = GameRectangle()
    private var minX = 0f

    private val stateTimer = Timer()
    private var introDone = false
    private var lastAttack: TimberWalkerState? = null
    private var sameAttackCount = 0
    private var pendingAttack: TimberWalkerState? = null
    private var tellAttack = TimberWalkerState.SAW_CHOP
    private var walkTargetX = 0f
    private var hopTargetX = 0f
    private var hopHome = false
    private var awayByHop = false
    private val candidates = Array<TimberWalkerState>()

    private var guardHits = 0
    private var guardBroken = false
    private val guardHitFlashTimer = Timer(GUARD_HIT_FLASH_DUR)

    private var heldPlayer: IPlayer? = null

    private lateinit var hullShield: Fixture
    private lateinit var hullDamager: Fixture
    private lateinit var hullRearTopShield: Fixture
    private lateinit var hullRearTopDamager: Fixture
    private lateinit var frontLegShield: Fixture
    private lateinit var frontLegDamager: Fixture
    private lateinit var rearLegShield: Fixture
    private lateinit var rearLegDamager: Fixture
    private lateinit var cabShield: Fixture
    private lateinit var cabDamageable: Fixture
    private lateinit var sawDamager: Fixture
    private lateinit var clawDamager: Fixture

    override fun init(vararg params: Any) {
        if (regions.isEmpty) {
            val atlas = services.assets.getTextureAtlas(TextureAsset.TIMBER_WALKER.source)
            TimberWalkerFrames.ANIMATIONS.values().forEach { def ->
                def.frames.forEach { frame ->
                    if (!regions.containsKey(frame)) regions.put(
                        frame, atlas.findRegion("$TAG/$frame")
                            ?: throw IllegalStateException("$TAG: no atlas region \"$TAG/$frame\"; rerun the asset build")
                    )
                }
            }
        }
        super.init()
        TimberWalkerFrames.ANIMATIONS.forEach { entry ->
            val frames = Array<TextureRegion>()
            entry.value.frames.forEach { frames.add(regions[it]) }
            animations.put(entry.key, Animation(frames, entry.value.durations, entry.value.loop))
        }
        rearPx = 0
        frontPx = 0
        STANCE_ANIMATIONS.forEach { key ->
            val def = TimberWalkerFrames.ANIMATIONS[key]
                ?: throw IllegalStateException("$TAG: no animation \"$key\" in TimberWalkerFrames; rerun the asset build")
            def.frames.forEach { frame ->
                val anchors = TimberWalkerFrames.ANCHORS[frame]
                    ?: throw IllegalStateException("$TAG: no anchors for the stance frame \"$frame\"")
                rearPx = max(rearPx, anchors.rightX - TimberWalkerFrames.BODY_X)
                frontPx = max(frontPx, TimberWalkerFrames.BODY_X - anchors.hull.x)
            }
        }
        val hopAir = TimberWalkerFrames.ANIMATIONS["hop_air"]
            ?: throw IllegalStateException("$TAG: no animation \"hop_air\" in TimberWalkerFrames; rerun the asset build")
        tuckGapPx = 0
        hopAir.frames.forEach { frame ->
            val anchors = TimberWalkerFrames.ANCHORS[frame]
                ?: throw IllegalStateException("$TAG: no anchors for the hop frame \"$frame\"")
            tuckGapPx = max(tuckGapPx, TimberWalkerFrames.FLOOR_Y - anchors.footY)
        }
        cabShield.setReceiver(Receivers.HIT_BY_PROJECTILE) { projectile -> onGuardHitByProjectile(projectile) }
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val floorY = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getY()
        val left = spawnProps.get(LEFT_WALL, RectangleMapObject::class)!!.rectangle
        val right = spawnProps.get(RIGHT_WALL, RectangleMapObject::class)!!.rectangle
        home.set(right.x - rearPx * PX, floorY)
        minX = left.x + left.width + frontPx * PX
        val far = spawnProps.get(FAR_START, RectangleMapObject::class)!!.rectangle
        farStart.set(far.x + far.width / 2f, far.y)

        position.set(farStart)
        body.setBottomCenterToPoint(position)
        facing = if (farStart.x > home.x) Facing.LEFT else Facing.RIGHT

        introDone = false
        lastAttack = null
        sameAttackCount = 0
        pendingAttack = null
        hopHome = false
        awayByHop = false
        guardHits = 0
        guardBroken = false
        guardHitFlashTimer.setToEnd()
        heldPlayer = null
        currentAnimKey = null
        setState(TimberWalkerState.FAR_WALK)
    }

    override fun isReady(delta: Float) = introDone

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        releaseHeldPlayer()
        super.onDestroy()
    }

    override fun canDamage(damageable: IDamageableEntity) = damageable != heldPlayer && super.canDamage(damageable)

    override fun takeDamageFrom(damager: IDamagerEntity): Boolean {
        val damaged = super.takeDamageFrom(damager)
        if (damaged && !defeated && state == TimberWalkerState.MET_STUNNED) setAnimation("met_hit", restart = true)
        return damaged
    }

    override fun triggerDefeat() {
        super.triggerDefeat()
        setState(TimberWalkerState.DEFEAT)
    }

    // TODO: the Met does not jump out, cry and run away at the defeat; the wreck stays until destroy (078g TODO.md)
    override fun onDefeated(delta: Float) {
        updateAnimation(delta)
        if (stateTimer.time < DEFEAT_EXPLOSIONS_DUR) explodeOnDefeat(delta)
        stateTimer.update(delta)
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (defeated) return@add
            updateAnimation(delta)
            guardHitFlashTimer.update(delta)
            when (bossComponent.phase) {
                BossPhase.ENTRANCE -> updateIntro(delta)
                BossPhase.INTRO -> {}
                BossPhase.FIGHT -> updateFight(delta)
            }
            holdPlayer(delta)
        }
    }

    private fun updateIntro(delta: Float) {
        when (state) {
            TimberWalkerState.FAR_WALK -> {
                val stopX = home.x + FAR_JUMP_GAP * ConstVals.PPM * sign(farStart.x - home.x)
                val step = FAR_WALK_SPEED * ConstVals.PPM * delta
                if (abs(stopX - position.x) <= step) {
                    position.x = stopX
                    setState(TimberWalkerState.FAR_CROUCH)
                } else position.x += step * sign(stopX - position.x)
            }

            TimberWalkerState.FAR_CROUCH -> {
                stateTimer.update(delta)
                if (stateTimer.isFinished()) {
                    jumpStart.set(position)
                    setState(TimberWalkerState.JUMP_IN)
                }
            }

            TimberWalkerState.JUMP_IN -> {
                stateTimer.update(delta)
                val t = stateTimer.getRatio()
                position.x = jumpStart.x + (home.x - jumpStart.x) * t
                position.y = jumpStart.y + (home.y - jumpStart.y) * t + JUMP_IN_ARC * ConstVals.PPM * 4f * t * (1f - t)
                setAnimation(
                    when {
                        t < 0.25f -> "far_jump"
                        t < 0.5f -> "jump_mid_1"
                        t < 0.75f -> "jump_mid_2"
                        else -> "jump"
                    }
                )
                if (stateTimer.isFinished()) {
                    position.set(home)
                    shakeRoom()
                    requestToPlaySound(SoundAsset.QUAKE_SOUND, false)
                    setState(TimberWalkerState.LAND)
                }
            }

            TimberWalkerState.LAND -> if (currentAnim.isFinished())
                setState(if (isFacing(FIGHT_FACING)) TimberWalkerState.ROAR else TimberWalkerState.TURN)

            TimberWalkerState.TURN -> if (currentAnim.isFinished()) {
                facing = facing.opposite()
                setState(TimberWalkerState.ROAR)
            }

            TimberWalkerState.ROAR -> when (currentAnimKey) {
                "roar_start" -> if (currentAnim.isFinished()) setAnimation("roar", restart = true)
                "roar" -> {
                    stateTimer.update(delta)
                    if (stateTimer.isFinished()) setAnimation("roar_end", restart = true)
                }
                else -> if (currentAnim.isFinished()) {
                    introDone = true
                    setState(TimberWalkerState.IDLE)
                }
            }

            else -> {}
        }
    }

    private fun updateFight(delta: Float) {
        when (state) {
            TimberWalkerState.IDLE -> {
                setAnimation(animKey("idle"))
                stateTimer.update(delta)
                if (stateTimer.isFinished()) pickNextState()
            }

            TimberWalkerState.WALK -> {
                if (guardBroken) {
                    setState(TimberWalkerState.IDLE)
                    return
                }
                setAnimation(animKey("walk"))
                val step = WALK_SPEED * ConstVals.PPM * delta
                if (abs(walkTargetX - position.x) <= step) {
                    position.x = walkTargetX
                    val attack = pendingAttack
                    pendingAttack = null
                    if (attack != null) startAttack(attack) else setState(TimberWalkerState.IDLE)
                } else position.x += step * sign(walkTargetX - position.x)
            }

            TimberWalkerState.TELL -> if (currentAnim.isFinished())
                setState(if (guardBroken) TimberWalkerState.IDLE else tellAttack)

            TimberWalkerState.HOP_CROUCH -> if (currentAnim.isFinished()) {
                val rise = HOP_RISE * ConstVals.PPM
                hopVelocity.set((hopTargetX - position.x) / HOP_AIR_TIME, 4f * rise / HOP_AIR_TIME)
                setState(TimberWalkerState.HOP_AIR)
            }

            TimberWalkerState.HOP_AIR -> {
                val gravity = -8f * HOP_RISE * ConstVals.PPM / (HOP_AIR_TIME * HOP_AIR_TIME)
                hopVelocity.y += gravity * delta
                position.x += hopVelocity.x * delta
                position.y += hopVelocity.y * delta
                if (hopVelocity.y < 0f && currentAnimKey == "hop_air" && position.y - home.y < tuckGapPx * PX)
                    setAnimation("jump", restart = true)
                if (position.y <= home.y) {
                    position.set(hopTargetX, home.y)
                    landHop()
                }
            }

            TimberWalkerState.CLAW_GRAB -> {
                if (heldPlayer == null && (currentFrame == "grab_3" || currentFrame == "grab_hold")) tryGrab()
                if (currentAnim.isFinished())
                    setState(if (heldPlayer != null) TimberWalkerState.GRAB_THROW else TimberWalkerState.GRAB_MISS)
            }

            TimberWalkerState.GUARD_OPEN -> if (currentAnim.isFinished()) {
                setState(TimberWalkerState.MET_STUNNED)
                stateTimer.resetDuration(MET_STUN_DUR)
            }

            TimberWalkerState.MET_STUNNED -> {
                if (currentAnimKey == "met_hit" && currentAnim.isFinished()) setAnimation("met_stunned", restart = true)
                stateTimer.update(delta)
                if (stateTimer.isFinished()) setState(TimberWalkerState.MET_RECOVER)
            }

            TimberWalkerState.MET_RECOVER -> if (currentAnim.isFinished()) setState(TimberWalkerState.GUARD_CLOSE)

            TimberWalkerState.SAW_THRUST_LOW, TimberWalkerState.SAW_THRUST_LEVEL, TimberWalkerState.SAW_CHOP,
            TimberWalkerState.LOG_THROW, TimberWalkerState.HOP_LAND, TimberWalkerState.CLAW_HIGH,
            TimberWalkerState.GRAB_MISS, TimberWalkerState.GRAB_THROW, TimberWalkerState.GUARD_CLOSE ->
                if (currentAnim.isFinished()) setState(TimberWalkerState.IDLE)

            else -> {}
        }
    }

    private fun pickNextState() {
        if (abs(position.x - home.x) > HOME_EPSILON_PX * PX) {
            if (awayByHop) startHop(toHome = true)
            else {
                walkTargetX = home.x
                pendingAttack = null
                setState(TimberWalkerState.WALK)
            }
            return
        }

        val dist = playerDistance()
        val height = playerHeight()
        candidates.clear()
        when {
            playerIsUnder() -> {
                candidates.add(TimberWalkerState.HOP_CROUCH)
                candidates.add(TimberWalkerState.LOG_THROW)
            }

            height >= HIGH_PLAYER_HEIGHT -> {
                candidates.add(TimberWalkerState.LOG_THROW)
                if (dist <= CLAW_HIGH_REACH + REACH_TOLERANCE + WALK_RANGE) candidates.add(TimberWalkerState.CLAW_HIGH)
            }

            else -> {
                when {
                    dist >= FAR_PLAYER_DIST -> {
                        candidates.add(TimberWalkerState.SAW_THRUST_LOW)
                        candidates.add(TimberWalkerState.SAW_THRUST_LEVEL)
                        candidates.add(TimberWalkerState.LOG_THROW)
                    }
                    dist >= CHOP_MIN_DIST -> {
                        candidates.add(TimberWalkerState.SAW_CHOP)
                        candidates.add(TimberWalkerState.HOP_CROUCH)
                    }
                    else -> {
                        candidates.add(TimberWalkerState.SAW_THRUST_LOW)
                        candidates.add(TimberWalkerState.SAW_THRUST_LEVEL)
                        candidates.add(TimberWalkerState.HOP_CROUCH)
                    }
                }
                if (height < GRAB_MAX_HEIGHT && dist in GRAB_MIN_DIST..GRAB_MAX_DIST)
                    candidates.add(TimberWalkerState.CLAW_GRAB)
            }
        }
        val repeated = lastAttack
        if (repeated != null && sameAttackCount >= MAX_SAME_ATTACK_IN_A_ROW) {
            candidates.removeValue(repeated, true)
            if (candidates.isEmpty) candidates.add(
                if (repeated == TimberWalkerState.LOG_THROW) TimberWalkerState.HOP_CROUCH
                else TimberWalkerState.LOG_THROW
            )
        }
        val next = services.random.pick(candidates)

        val reach = when (next) {
            TimberWalkerState.SAW_CHOP -> CHOP_REACH
            TimberWalkerState.CLAW_HIGH -> CLAW_HIGH_REACH
            else -> 0f
        }
        if (reach > 0f && dist > reach + REACH_TOLERANCE) {
            val walk = min(dist - reach, WALK_RANGE) * ConstVals.PPM
            walkTargetX = max(position.x - walk, max(minX, home.x - WALK_RANGE * ConstVals.PPM))
            if (position.x - walkTargetX > HOME_EPSILON_PX * PX) {
                pendingAttack = next
                setState(TimberWalkerState.WALK)
                return
            }
        }
        startAttack(next)
    }

    private fun startAttack(attack: TimberWalkerState) {
        GameLogger.debug(TAG, "startAttack(): attack=$attack, lastAttack=$lastAttack, sameAttackCount=$sameAttackCount")
        if (guardBroken) {
            setState(TimberWalkerState.IDLE)
            return
        }
        if (attack == lastAttack) sameAttackCount++
        else {
            lastAttack = attack
            sameAttackCount = 1
        }
        when (attack) {
            TimberWalkerState.HOP_CROUCH -> startHop(toHome = false)
            TimberWalkerState.SAW_THRUST_LOW, TimberWalkerState.SAW_THRUST_LEVEL, TimberWalkerState.SAW_CHOP,
            TimberWalkerState.CLAW_HIGH, TimberWalkerState.CLAW_GRAB -> {
                tellAttack = attack
                setState(TimberWalkerState.TELL)
            }
            else -> setState(attack)
        }
    }

    private fun startHop(toHome: Boolean) {
        hopHome = toHome
        hopTargetX = if (toHome) home.x
        else targetPlayer.body.getCenter().x.coerceIn(minX, home.x - HOP_MIN_DIST * ConstVals.PPM)
        setState(TimberWalkerState.HOP_CROUCH)
    }

    private fun setState(requested: TimberWalkerState) {
        val next = if (requested == TimberWalkerState.IDLE && guardBroken) TimberWalkerState.GUARD_OPEN else requested
        GameLogger.debug(TAG, "setState(): $state -> $next")
        if (next != TimberWalkerState.CLAW_GRAB && next != TimberWalkerState.GRAB_THROW) releaseHeldPlayer()
        state = next
        when (next) {
            TimberWalkerState.FAR_WALK -> setAnimation("far_walk", restart = true)
            TimberWalkerState.FAR_CROUCH -> {
                setAnimation("far_crouch", restart = true)
                stateTimer.resetDuration(FAR_CROUCH_DUR)
            }
            TimberWalkerState.JUMP_IN -> {
                setAnimation("far_jump", restart = true)
                stateTimer.resetDuration(JUMP_IN_DUR)
            }
            TimberWalkerState.LAND -> setAnimation(animKey("land"), restart = true)
            TimberWalkerState.TURN -> setAnimation(animKey("turn"), restart = true)
            TimberWalkerState.ROAR -> {
                setAnimation("roar_start", restart = true)
                stateTimer.resetDuration(ROAR_DUR)
                requestToPlaySound(SoundAsset.MM2_MECHA_DRAGON_SOUND, false)
            }
            TimberWalkerState.IDLE -> {
                setAnimation(animKey("idle"))
                stateTimer.resetDuration(if (lowHealth()) IDLE_DUR_LOW_HEALTH else IDLE_DUR)
            }
            TimberWalkerState.WALK -> setAnimation(animKey("walk"), restart = true)
            TimberWalkerState.TELL -> {
                setAnimation(tellAnimation(tellAttack), restart = true)
                requestToPlaySound(tellSound(tellAttack), false)
            }
            TimberWalkerState.SAW_THRUST_LOW -> setAnimation(animKey("saw_low"), restart = true)
            TimberWalkerState.SAW_THRUST_LEVEL -> setAnimation(animKey("saw_level"), restart = true)
            TimberWalkerState.SAW_CHOP -> setAnimation(animKey("attack_saw"), restart = true)
            TimberWalkerState.LOG_THROW -> setAnimation(animKey("attack_log"), restart = true)
            TimberWalkerState.HOP_CROUCH -> setAnimation(animKey("hop_crouch"), restart = true)
            TimberWalkerState.HOP_AIR -> setAnimation("hop_air", restart = true)
            TimberWalkerState.HOP_LAND -> setAnimation(animKey("hop_land"), restart = true)
            TimberWalkerState.CLAW_HIGH -> setAnimation(animKey("claw_extend"), restart = true)
            TimberWalkerState.CLAW_GRAB -> setAnimation(animKey("grab_reach"), restart = true)
            TimberWalkerState.GRAB_MISS -> setAnimation(animKey("grab_miss"), restart = true)
            TimberWalkerState.GRAB_THROW -> setAnimation(animKey("grab_throw"), restart = true)
            TimberWalkerState.GUARD_OPEN -> {
                guardBroken = false
                guardHits = 0
                setAnimation("guard_open", restart = true)
                requestToPlaySound(SoundAsset.BOSS_DOOR_SOUND, false)
            }
            TimberWalkerState.MET_STUNNED -> setAnimation("met_stunned", restart = true)
            TimberWalkerState.MET_RECOVER -> setAnimation("met_recover", restart = true)
            TimberWalkerState.GUARD_CLOSE -> {
                setAnimation(animKey("guard_close"), restart = true)
                requestToPlaySound(SoundAsset.BOSS_DOOR_SOUND, false)
            }
            TimberWalkerState.DEFEAT -> {
                setAnimation("defeat", restart = true)
                stateTimer.resetDuration(defeatTimer.duration)
            }
        }
    }

    private fun tellAnimation(attack: TimberWalkerState) = when (attack) {
        TimberWalkerState.SAW_THRUST_LOW -> "tell_lever_low"
        TimberWalkerState.SAW_THRUST_LEVEL -> "tell_lever_level"
        TimberWalkerState.SAW_CHOP -> "tell_lever_chop"
        TimberWalkerState.CLAW_HIGH, TimberWalkerState.CLAW_GRAB -> "tell_button_claw"
        else -> throw IllegalStateException("$TAG: the attack $attack has no tell")
    }

    private fun tellSound(attack: TimberWalkerState) = when (attack) {
        TimberWalkerState.CLAW_HIGH, TimberWalkerState.CLAW_GRAB -> SoundAsset.BUTTON_SOUND
        else -> SoundAsset.REV_SOUND
    }

    private fun animKey(key: String): String {
        if (!lowHealth()) return key
        val damaged = "$DAMAGED_PREFIX$key"
        return if (TimberWalkerFrames.ANIMATIONS.containsKey(damaged)) damaged else key
    }

    private fun setAnimation(key: String, restart: Boolean = false) {
        if (key == currentAnimKey && !restart) return
        currentAnimKey = key
        currentDef = TimberWalkerFrames.ANIMATIONS[key]
            ?: throw IllegalStateException("$TAG: no animation \"$key\" in TimberWalkerFrames; rerun the asset build")
        currentAnim = animations[key]
        currentAnim.setIndex(0)
        currentFrameIndex = -1
        refreshFrame()
    }

    private fun updateAnimation(delta: Float) {
        currentAnim.update(delta)
        refreshFrame()
    }

    private fun refreshFrame() {
        val index = currentAnim.getIndex()
        if (index == currentFrameIndex) return
        currentFrameIndex = index
        currentFrame = currentDef.frames[index]
        currentMirrored = currentDef.mirrored[index]
        currentRegion = regions[currentFrame]
        onFrameEntered()
    }

    private fun onFrameEntered() {
        when (currentFrame) {
            "saw_low_2", "saw_level_2" -> if (state == TimberWalkerState.SAW_THRUST_LOW ||
                state == TimberWalkerState.SAW_THRUST_LEVEL
            ) requestToPlaySound(SoundAsset.MM3_ELECTRIC_SAW_SOUND, false)
            "attack_strike" -> if (state == TimberWalkerState.SAW_CHOP) {
                requestToPlaySound(SoundAsset.POUND_SOUND, false)
                shakeRoom()
            }
            "log_throw" -> if (state == TimberWalkerState.LOG_THROW) throwLog()
            "grab_throw" -> if (state == TimberWalkerState.GRAB_THROW) throwHeldPlayer()
        }
    }

    private fun currentAnchors(): TimberWalkerAnchors? = TimberWalkerFrames.ANCHORS[currentFrame]

    private fun drawFlipped() = isFacing(Facing.RIGHT) != currentMirrored

    private fun lowHealth() = bossComponent.phase == BossPhase.FIGHT && getHealthRatio() < LOW_HEALTH_RATIO

    private fun playerIsUnder() = targetPlayer.body.getCenter().x > position.x - frontPx * PX

    private fun playerDistance() = abs(targetPlayer.body.getCenter().x - position.x) / ConstVals.PPM

    private fun playerHeight() = (targetPlayer.body.getY() - home.y) / ConstVals.PPM

    private fun guardClosed() = introDone && !defeated && when (state) {
        TimberWalkerState.GUARD_OPEN, TimberWalkerState.MET_STUNNED, TimberWalkerState.MET_RECOVER,
        TimberWalkerState.GUARD_CLOSE, TimberWalkerState.DEFEAT -> false
        else -> true
    }

    private fun onGuardHitByProjectile(projectile: IProjectileEntity) {
        if (bossComponent.phase != BossPhase.FIGHT || !guardClosed() || guardBroken) return
        if (projectile.rootPlayer == null) return
        val hits = if (projectile is ChargedShot) CHARGED_SHOT_GUARD_HITS else 1
        guardHits = min(guardHits + hits, GUARD_HITS_TO_OPEN)
        GameLogger.debug(TAG, "onGuardHitByProjectile(): projectile=$projectile, guardHits=$guardHits")
        guardHitFlashTimer.reset()
        requestToPlaySound(SoundAsset.ENEMY_DAMAGE_SOUND, false)
        if (guardHits >= GUARD_HITS_TO_OPEN) guardBroken = true
    }

    private fun anchorToWorld(px: Float, py: Float, out: Vector2): Vector2 {
        val dx = (px - TimberWalkerFrames.BODY_X) * PX
        return out.set(position.x + if (drawFlipped()) -dx else dx, position.y + (TimberWalkerFrames.FLOOR_Y - py) * PX)
    }

    private fun boxToWorld(box: PxBox, out: GameRectangle): GameRectangle {
        anchorToWorld(box.x.toFloat(), (box.y + box.h).toFloat(), scratch)
        val w = box.w * PX
        return out.set(if (drawFlipped()) scratch.x - w else scratch.x, scratch.y, w, box.h * PX)
    }

    private fun throwLog() {
        val anchors = currentAnchors() ?: return
        val grip = anchors.grip ?: return
        val spawn = GameObjectPools.fetch(Vector2::class)
        anchorToWorld(grip.x.toFloat(), grip.y.toFloat(), spawn)
        val gravity = LOG_GRAVITY * ConstVals.PPM
        val t = LOG_FLIGHT_TIME
        val trajectory = targetPlayer.body.getCenter().sub(spawn).scl(1f / t)
        trajectory.y -= 0.5f * gravity * t
        MegaEntityFactory.fetch(TimberWalkerLog::class)!!.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.TRAJECTORY pairTo trajectory,
                ConstKeys.GRAVITY pairTo gravity
            )
        )
        requestToPlaySound(SoundAsset.WHOOSH_SOUND, false)
    }

    private fun landHop() {
        shakeRoom()
        requestToPlaySound(SoundAsset.QUAKE_SOUND, false)
        awayByHop = !hopHome
        setState(TimberWalkerState.HOP_LAND)
        if (hopHome) return
        val anchors = currentAnchors() ?: return
        val hull = anchors.hull
        for (edge in 0..1) {
            val edgePx = (hull.x + edge * hull.w).toFloat()
            val spawn = GameObjectPools.fetch(Vector2::class)
            anchorToWorld(edgePx, TimberWalkerFrames.FLOOR_Y.toFloat(), spawn)
            val artSide = if (edge == 0) -1f else 1f
            val side = if (drawFlipped()) -artSide else artSide
            val trajectory = GameObjectPools.fetch(Vector2::class).set(side * CHIP_WAVE_SPEED * ConstVals.PPM, 0f)
            MegaEntityFactory.fetch(TimberWalkerChipWave::class)!!.spawn(
                props(
                    ConstKeys.OWNER pairTo this,
                    ConstKeys.POSITION pairTo spawn,
                    ConstKeys.TRAJECTORY pairTo trajectory
                )
            )
        }
    }

    private fun tryGrab() {
        val anchors = currentAnchors() ?: return
        val claw = anchors.claw ?: return
        boxToWorld(claw, clawBounds)
        players.forEachLive { player ->
            if (heldPlayer == null && player.existenceState.liveAfterUpdate &&
                clawBounds.overlaps(player.body.getBounds())
            ) grab(player)
        }
    }

    private fun grab(player: IPlayer) {
        GameLogger.debug(TAG, "grab(): player=$player")
        heldPlayer = player
        player.lock(MegaLockGroup.MOVEMENT, MegaLockReason.BOSS_GRAB)
        player.lock(MegaLockGroup.BEHAVIORS, MegaLockReason.BOSS_GRAB)
        player.body.physics.velocity.setZero()
        player.body.physics.gravityOn = false
        if (!currentGrip(holdPoint)) holdPoint.set(player.body.getCenter())
        requestToPlaySound(SoundAsset.CHOMP_SOUND, false)
    }

    private fun currentGrip(out: Vector2): Boolean {
        val anchors = currentAnchors() ?: return false
        val grip = anchors.grip ?: return false
        anchorToWorld(grip.x.toFloat(), grip.y.toFloat(), out)
        return true
    }

    private fun holdPlayer(delta: Float) {
        val player = heldPlayer ?: return
        if (!player.existenceState.liveAfterUpdate) {
            releaseHeldPlayer()
            return
        }
        val target = GameObjectPools.fetch(Vector2::class)
        if (currentGrip(target)) {
            val step = GRIP_EASE_SPEED * ConstVals.PPM * delta
            val dx = target.x - holdPoint.x
            val dy = target.y - holdPoint.y
            val length = Vector2.len(dx, dy)
            if (length <= step) holdPoint.set(target) else holdPoint.add(dx / length * step, dy / length * step)
        }
        player.body.physics.velocity.setZero()
        player.body.setCenter(holdPoint)
    }

    private fun throwHeldPlayer() {
        val player = heldPlayer ?: return
        if (currentGrip(holdPoint)) player.body.setCenter(holdPoint)
        releaseHeldPlayer()
        if (player is IDamageableEntity && player.canBeDamagedBy(this) && canDamage(player)) player.takeDamageFrom(this)
        player.body.physics.velocity.set(
            THROW_IMPULSE_X * ConstVals.PPM * facing.value, THROW_IMPULSE_Y * ConstVals.PPM
        )
        requestToPlaySound(SoundAsset.WHIP_SOUND, false)
    }

    private fun releaseHeldPlayer() {
        val player = heldPlayer ?: return
        GameLogger.debug(TAG, "releaseHeldPlayer(): player=$player")
        heldPlayer = null
        player.unlock(MegaLockGroup.MOVEMENT, MegaLockReason.BOSS_GRAB)
        player.unlock(MegaLockGroup.BEHAVIORS, MegaLockReason.BOSS_GRAB)
        player.body.physics.gravityOn = true
    }

    private fun shakeRoom() = services.events.submitEvent(
        Event(
            EventType.SHAKE_CAM, props(
                ConstKeys.INTERVAL pairTo SHAKE_INTERVAL,
                ConstKeys.DURATION pairTo SHAKE_DUR,
                ConstKeys.X pairTo SHAKE_X * ConstVals.PPM,
                ConstKeys.Y pairTo SHAKE_Y * ConstVals.PPM
            )
        )
    )

    private fun clawGrabs() = state == TimberWalkerState.CLAW_GRAB || state == TimberWalkerState.GRAB_MISS ||
        state == TimberWalkerState.GRAB_THROW

    private fun placeFixture(fixture: Fixture, active: Boolean, x: Float, y: Float, w: Float, h: Float) {
        fixture.setActive(active && w > 0f && h > 0f)
        if (!fixture.isActive()) return
        (fixture.rawShape as GameRectangle).setSize(w * PX, h * PX)
        val dx = (x + w / 2f - TimberWalkerFrames.BODY_X) * PX
        fixture.offsetFromBodyAttachment.set(
            if (drawFlipped()) -dx else dx, (TimberWalkerFrames.FLOOR_Y - (y + h / 2f)) * PX
        )
    }

    private fun placeBox(fixture: Fixture, active: Boolean, box: PxBox?) {
        if (box == null) {
            fixture.setActive(false)
            return
        }
        placeFixture(fixture, active, box.x.toFloat(), box.y.toFloat(), box.w.toFloat(), box.h.toFloat())
    }

    private fun updateFixtures() {
        val anchors = currentAnchors()
        if (anchors == null) {
            body.forEachFixture { it.setActive(false) }
            return
        }
        val on = !defeated
        val hull = anchors.hull
        val cab = anchors.cab
        body.setSize(hull.w * PX, max(TimberWalkerFrames.FLOOR_Y - hull.y, 1) * PX)
        body.setBottomCenterToPoint(position)

        val hullTop = if (cab != null) maxOf(hull.y, cab.y + cab.h) else hull.y
        val hullX = hull.x.toFloat()
        val hullTopF = hullTop.toFloat()
        val hullW = hull.w.toFloat()
        val hullH = (hull.y + hull.h - hullTop).toFloat()
        placeFixture(hullShield, on, hullX, hullTopF, hullW, hullH)
        placeFixture(hullDamager, on, hullX, hullTopF, hullW, hullH)

        val rearTopX = if (cab != null) (cab.x + cab.w).toFloat() else hullX
        val rearTopW = (hull.x + hull.w) - rearTopX
        val rearTopH = (hullTop - hull.y).toFloat()
        placeFixture(hullRearTopShield, on, rearTopX, hull.y.toFloat(), rearTopW, rearTopH)
        placeFixture(hullRearTopDamager, on, rearTopX, hull.y.toFloat(), rearTopW, rearTopH)

        val legTop = (hull.y + hull.h).toFloat()
        val legH = anchors.footY - legTop
        val rearX = (anchors.rightX - LEG_WIDTH_PX).toFloat()
        val legW = LEG_WIDTH_PX.toFloat()
        placeFixture(frontLegShield, on, hullX, legTop, legW, legH)
        placeFixture(frontLegDamager, on, hullX, legTop, legW, legH)
        placeFixture(rearLegShield, on, rearX, legTop, legW, legH)
        placeFixture(rearLegDamager, on, rearX, legTop, legW, legH)

        val open = state == TimberWalkerState.MET_STUNNED
        placeBox(cabShield, on && !open, cab)
        placeBox(cabDamageable, on && open, cab)
        placeBox(sawDamager, on, anchors.saw)
        placeBox(clawDamager, on && !clawGrabs(), anchors.claw)
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat())

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        fun part(type: FixtureType): Fixture {
            val fixture = Fixture(body, type, GameRectangle(), bodyAttachmentPosition = Position.BOTTOM_CENTER)
            body.addFixture(fixture)
            debugShapes.add { if (fixture.isActive()) fixture else null }
            return fixture
        }
        hullShield = part(FixtureType.SHIELD)
        hullDamager = part(FixtureType.DAMAGER)
        hullRearTopShield = part(FixtureType.SHIELD)
        hullRearTopDamager = part(FixtureType.DAMAGER)
        frontLegShield = part(FixtureType.SHIELD)
        frontLegDamager = part(FixtureType.DAMAGER)
        rearLegShield = part(FixtureType.SHIELD)
        rearLegDamager = part(FixtureType.DAMAGER)
        cabShield = part(FixtureType.SHIELD)
        cabDamageable = part(FixtureType.DAMAGEABLE)
        sawDamager = part(FixtureType.DAMAGER)
        clawDamager = part(FixtureType.DAMAGER)

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.physics.velocity.setZero()
            body.setBottomCenterToPoint(position)
            updateFixtures()
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 1))
        val guardSprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 2))
        val shadowSprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 0))
        val component = SpritesComponent(
            SpritesComponent.DEFAULT_KEY pairTo sprite,
            GUARD_KEY pairTo guardSprite,
            SHADOW_KEY pairTo shadowSprite
        )
        component.putPreProcess { _, _ ->
            val region = currentRegion ?: return@putPreProcess
            sprite.setRegion(region)
            val w = region.regionWidth * PX
            val h = region.regionHeight * PX
            sprite.setSize(w, h)
            val flip = drawFlipped()
            val bodyFraction = TimberWalkerFrames.BODY_X.toFloat() / TimberWalkerFrames.CANVAS_WIDTH
            val floorFraction = (TimberWalkerFrames.CANVAS_HEIGHT - TimberWalkerFrames.FLOOR_Y).toFloat() /
                TimberWalkerFrames.CANVAS_HEIGHT
            val bottomCenter = body.getPositionPoint(Position.BOTTOM_CENTER)
            sprite.setPosition(
                bottomCenter.x - w * (if (flip) 1f - bodyFraction else bodyFraction), bottomCenter.y - h * floorFraction
            )
            sprite.setFlip(flip, false)
            sprite.priority.section = if (drawnFar()) DrawingSection.BACKGROUND else DrawingSection.PLAYGROUND
            sprite.hidden = damageBlink && !defeated
        }
        component.putPreProcess(GUARD_KEY) { _, _ ->
            val anchors = currentAnchors()
            val cab = anchors?.cab
            val flashing = !guardHitFlashTimer.isFinished()
            if (anchors == null || cab == null || !guardClosed() || (guardHits == 0 && !flashing) ||
                (damageBlink && !defeated)
            ) {
                guardSprite.hidden = true
                return@putPreProcess
            }
            guardSprite.hidden = false
            val key = if (flashing) GUARD_HIT_REGION else "$GUARD_CRACK_REGION${min(guardHits, GUARD_CRACK_STAGES)}"
            guardSprite.setRegion(regions[key])
            val bounds = boxToWorld(cab, GameObjectPools.fetch(GameRectangle::class))
            guardSprite.setBounds(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight())
            guardSprite.setFlip(drawFlipped(), false)
        }
        component.putPreProcess(SHADOW_KEY) { _, _ ->
            val shown = state == TimberWalkerState.HOP_CROUCH || state == TimberWalkerState.HOP_AIR
            shadowSprite.hidden = !shown
            if (!shown) return@putPreProcess
            val region = regions[SHADOW_REGION]
            shadowSprite.setRegion(region)
            val w = region.regionWidth * PX
            shadowSprite.setBounds(hopTargetX - w / 2f, home.y, w, region.regionHeight * PX)
        }
        return component
    }

    private fun drawnFar() = currentFrame.startsWith("far_") || currentFrame == "jump_mid_1"
}
