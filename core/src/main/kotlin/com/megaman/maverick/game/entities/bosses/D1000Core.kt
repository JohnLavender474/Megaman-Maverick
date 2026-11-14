package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.animations.IAnimation
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.UtilMethods
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.damage.IDamageableEntity
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.shapes.IDrawableShape
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.entities.contracts.IAnimatedEntity
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.entities.contracts.ISpritesEntity
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.megaman.maverick.game.entities.players.PlayerSabreSlash
import com.megaman.maverick.game.entities.utils.TickFrameFixtures
import com.megaman.maverick.game.entities.utils.TickFrames
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.targetPlayer
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.hazards.D1000Pillar
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.explosions.Explosion
import com.megaman.maverick.game.entities.explosions.SmokePuff
import com.megaman.maverick.game.players.rootPlayer
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.entities.projectiles.EnemyShot
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sqrt
import com.megaman.maverick.game.entities.bosses.d1000.D1000Tuning
import com.megaman.maverick.game.entities.bosses.d1000.D1000Frames
import com.megaman.maverick.game.entities.bosses.d1000.D1000GuidePhase
import com.megaman.maverick.game.entities.bosses.d1000.D1000Orbs
import com.megaman.maverick.game.entities.bosses.d1000.approach
import com.megaman.maverick.game.entities.bosses.d1000.flickerOn

enum class D1000EyeAttack { BURST, SPREAD, DIVE, FLANK }

// TODO: the D-1000 fight (its behaviors, mechanics, tuning and anti-trap rules) was AI-generated from owner play tests; it needs more scrutiny and refinement (see the branch NOTES of 081f, section "AI-generated fight")
class D1000Core(services: MegaServices) : MegaGameEntity(services), IBodyEntity, ISpritesEntity, IAnimatedEntity,
    IDamageableEntity, IDamagerEntity {

    companion object {
        const val TAG = "D1000Core"
        const val PUPIL = "pupil"
        const val GLOW = "glow"
        const val BEAM = "beam"
        const val THRUSTER = "thruster"
        internal const val GLOW_FLASH_DUR = 3f / TickFrames.FPS
        internal const val BEAM_STEP = 4f / TickFrames.FPS
        internal const val BEAM_FLASH_STEP = 2f / TickFrames.FPS
        internal const val BEAM_FLASH_DUR = D1000Frames.BEAM_FLASH_FRAMES * BEAM_FLASH_STEP
        internal const val THRUSTER_STEP = 4f / TickFrames.FPS
        internal const val THRUSTER_FLARE_STEP = 2f / TickFrames.FPS
        internal const val BOUNCE_DIP = 2f * TickFrames.SCALE

        internal fun thrusterFrame(elapsed: Float, flare: Boolean) =
            if (flare) D1000Frames.THRUSTER_IDLE_FRAMES + (elapsed / THRUSTER_FLARE_STEP).toInt() % D1000Frames.THRUSTER_FLARE_FRAMES
            else (elapsed / THRUSTER_STEP).toInt() % D1000Frames.THRUSTER_IDLE_FRAMES

        internal fun bounce(elapsed: Float, signalDur: Float): Float {
            val third = elapsed / signalDur * 3f
            return when {
                third < 0f || third >= 3f -> 0f
                third >= 1f && third < 2f -> -BOUNCE_DIP
                else -> -BOUNCE_DIP / 2f
            }
        }

        internal fun beamFrame(elapsed: Float, lockDur: Float): Int {
            val flashStart = lockDur - BEAM_FLASH_DUR
            if (elapsed < flashStart) return (elapsed / BEAM_STEP).toInt() % D1000Frames.BEAM_LOOP_FRAMES
            val flash = min(((elapsed - flashStart) / BEAM_FLASH_STEP).toInt(), D1000Frames.BEAM_FLASH_FRAMES - 1)
            return D1000Frames.BEAM_LOOP_FRAMES + flash
        }

        internal fun glowFrame(elapsed: Float, tellDur: Float) =
            if (tellDur - elapsed <= GLOW_FLASH_DUR) D1000Frames.EYE_GLOW_FLASH
            else min((elapsed / tellDur * D1000Frames.EYE_GLOW_FLASH).toInt(), D1000Frames.EYE_GLOW_FLASH - 1)

        private const val SIZE = 0.9375f * ConstVals.PPM
        internal const val SHOT_SPEED = 5.625f * ConstVals.PPM
        internal val SHOT_TIMES = floatArrayOf(0f, 0.13f, 0.27f, 0.4f)
        private const val BURST_DUR = 1f
        private const val APPEAR_SIGNAL_DELAY = 1f
        private const val POT_RECHECK = 0.25f
        private const val SPREAD_ANGRY_DUR = 0.3f
        private const val MOVE_DELAY = 0.5f
        internal const val HOME_SPEED = 3.75f * ConstVals.PPM
        internal const val REVERSE_PAUSE = 0.1f
        private const val DAZE_SPEED = 9f * ConstVals.PPM
        private const val DAZED_DUR = 1.5f

        internal const val FACE_HYSTERESIS = 0.5f * ConstVals.PPM

        internal const val DEFEAT_OVER = 1.25f * ConstVals.PPM
        internal const val SPIN_DUR = 2.6f
        private val EYE_EXPLOSIONS = arrayOf(Vector2(0f, 0f), Vector2(-0.5f, 0.25f), Vector2(0.5f, -0.25f))
        private val EYE_PUFFS = arrayOf(Vector2(-0.4f, -0.5f), Vector2(0.4f, -0.5f))

        internal const val RAIL_RISE = 0.5f * ConstVals.PPM
        private const val RAIL_RISE_SPEED = 6f * ConstVals.PPM
        internal const val MIN_MOVE = 0.25f * ConstVals.PPM

        internal const val SPREAD_TELL_DUR = 0.6f
        internal const val SPREAD_SECOND_DELAY = 0.35f
        private const val SPREAD_END_AFTER = 0.9f
        internal val SPREAD_ANGLES = floatArrayOf(-40f, -20f, 0f, 20f, 40f)
        internal val SPREAD_ANGLES_SECOND = floatArrayOf(-30f, -10f, 10f, 30f)

        private const val NEAR_X = 4f * ConstVals.PPM
        private const val NEAR_Y = 1.5f * ConstVals.PPM
        private const val DIVE_SPEED = 10f * ConstVals.PPM
        private const val DIVE_MAX = 3.5f * ConstVals.PPM
        private const val DIVE_HOLD_DUR = 0.5f
        private const val JITTER = 0.0625f * ConstVals.PPM

        internal const val FLANK_SIDE = 2.5f * ConstVals.PPM
        internal const val FLANK_UP = 1f * ConstVals.PPM
        internal const val FLANK_SPEED = 7.5f * ConstVals.PPM
        internal const val FLANK_SHOT_GAP = 0.2f

        private const val DRIFT_X = 0.375f * ConstVals.PPM
        private const val DRIFT_Y = 0.25f * ConstVals.PPM
        private const val DRIFT_SPEED = 1.875f * ConstVals.PPM
        private const val DRIFT_HOLD = 0.4f
        private val DRIFT_CORNERS = arrayOf(Vector2(1f, 1f), Vector2(-1f, 1f), Vector2(-1f, -1f), Vector2(1f, -1f))

        const val HURT_DUR = 0.5f

        private const val DODGE_DIST = 1.25f * ConstVals.PPM
        private const val DODGE_SPEED = 12f * ConstVals.PPM
        private const val DODGE_HOLD = 0.4f
        private const val SHOT_SENSE = 5f * ConstVals.PPM
        private const val SHOT_LANE = 1f * ConstVals.PPM
        private const val SABRE_REACH_X = 3f * ConstVals.PPM
        private const val SABRE_REACH_Y = 2.5f * ConstVals.PPM
        private const val EDGE_MARGIN = 0.75f * ConstVals.PPM
        private val SHOT_TAGS = arrayOf(Bullet.TAG, ChargedShot.TAG)

        internal fun flankSide(arena: GameRectangle, playerX: Float): Int {
            val away = if (playerX - arena.getX() < arena.getMaxX() - playerX) 1 else -1
            return when {
                flankSideFree(arena, playerX, away) -> away
                flankSideFree(arena, playerX, -away) -> -away
                else -> 0
            }
        }

        private fun flankSideFree(arena: GameRectangle, playerX: Float, side: Int): Boolean {
            val x = playerX + side * FLANK_SIDE
            if (x < arena.getX() + EDGE_MARGIN || x > arena.getMaxX() - EDGE_MARGIN) return false
            val low = min(playerX, x)
            val high = maxOf(playerX, x)
            MegaGameEntities.getOfTag(D1000Pillar.TAG).forEach { pillar ->
                if (pillar !is D1000Pillar || pillar.horizontal || !pillar.existenceState.liveAfterUpdate || pillar.dangerUntil < 0f) return@forEach
                if (pillar.baseX + D1000Pillar.WIDTH / 2f > low && pillar.baseX - D1000Pillar.WIDTH / 2f < high) return false
            }
            return true
        }

        internal fun flankPoint(arena: GameRectangle, player: Vector2, side: Int, out: Vector2): Vector2 = out.set(
            (player.x + side * FLANK_SIDE).coerceIn(arena.getX() + EDGE_MARGIN, arena.getMaxX() - EDGE_MARGIN),
            (player.y + FLANK_UP).coerceIn(arena.getY() + EDGE_MARGIN, arena.getMaxY() - EDGE_MARGIN)
        )
    }

    private enum class D1000CoreState { APPEAR, IDLE, SUMMON, HIT, AWAY, DAZED, GUIDE, SPREAD, DIVE, FLANK, DEFEAT }

    override val invincible: Boolean
        get() = !vulnerable || hurtTime > 0f

    val vulnerable: Boolean
        get() = showing() && owner?.stunned != true && owner?.defeated != true

    var facingRight = false
        private set
    val spinning: Boolean
        get() = state == D1000CoreState.DEFEAT && phase == 1

    private var owner: D1000? = null
    private val arena = GameRectangle()
    private val position = Vector2()
    private val home = Vector2()
    private val other = Vector2()
    private val target = Vector2()
    private var arrived = false

    private var state = D1000CoreState.APPEAR
    private var time = 0f
    private var phase = 0
    private var signalPending = false
    private var shotsFired = 0
    private var potShotTime = 0f
    private var potShotCleared = false
    private var hidden = false
    private var driftCorner = 0
    private var driftHold = 0f
    private var driftReturning = true
    private var hurtTime = 0f
    private var dodging = false
    private var dodgeTime = 0f
    private var dodgeCooldown = 0f
    private val dodgeTarget = Vector2()
    private val dodgeStep = Vector2()
    private val seenThreats = ObjectSet<MegaGameEntity>()
    private var thrusterTime = 0f
    private var inSignal = false
    private var bounceArmed = false
    private var flankSide = 0
    private val flankAim = Vector2()
    private var stepX = 0
    private var stepY = 0
    private var sinceStepX = 0f
    private var sinceStepY = 0f
    private var lastDrawnX = 0f
    private var lastDrawnY = 0f
    private var spinLooping = false
    private val pupilOffset = IntArray(2)
    private val toPlayer = Vector2()

    private lateinit var eyeFixtures: TickFrameFixtures
    private lateinit var ballFixtures: TickFrameFixtures

    private val animations = ObjectMap<String, IAnimation>()
    private lateinit var animator: Animator
    private lateinit var pupilFrames: kotlin.Array<TextureRegion>
    private lateinit var glowFrames: kotlin.Array<TextureRegion>
    private lateinit var beamFrames: kotlin.Array<TextureRegion>
    private lateinit var thrusterFrames: kotlin.Array<TextureRegion>
    private val pupilPlace = Vector2()

    override fun init(vararg params: Any) {
        super.init()
        GameLogger.debug(TAG, "init()")
        val atlas = services.assets.getTextureAtlas(TextureAsset.BOSSES_3.source)
        TickFrames.animations(atlas, TAG, D1000Frames.CORE, animations)
        val pupil = atlas.findRegion("$TAG/$PUPIL") ?: throw IllegalStateException("Missing region $TAG/$PUPIL")
        pupilFrames = kotlin.Array(D1000Frames.PUPIL_FRAMES) {
            TextureRegion(pupil, 0, it * D1000Frames.PUPIL_SIZE, D1000Frames.PUPIL_SIZE, D1000Frames.PUPIL_SIZE)
        }
        val glow = atlas.findRegion("$TAG/$GLOW") ?: throw IllegalStateException("Missing region $TAG/$GLOW")
        glowFrames = kotlin.Array(D1000Frames.EYE_GLOW_FRAMES) {
            TextureRegion(glow, 0, it * D1000Frames.EYE_GLOW_SIZE, D1000Frames.EYE_GLOW_SIZE, D1000Frames.EYE_GLOW_SIZE)
        }
        val beam = atlas.findRegion("$TAG/$BEAM") ?: throw IllegalStateException("Missing region $TAG/$BEAM")
        beamFrames = kotlin.Array(D1000Frames.BEAM_LOOP_FRAMES + D1000Frames.BEAM_FLASH_FRAMES) {
            TextureRegion(beam, 0, it * D1000Frames.BEAM_HEIGHT, D1000Frames.BEAM_WIDTH, D1000Frames.BEAM_HEIGHT)
        }
        val thruster = atlas.findRegion("$TAG/$THRUSTER") ?: throw IllegalStateException("Missing region $TAG/$THRUSTER")
        thrusterFrames = kotlin.Array(D1000Frames.THRUSTER_IDLE_FRAMES + D1000Frames.THRUSTER_FLARE_FRAMES) {
            TextureRegion(thruster, 0, it * D1000Frames.THRUSTER_SIZE, D1000Frames.THRUSTER_SIZE, D1000Frames.THRUSTER_SIZE)
        }
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
        owner = spawnProps.get(ConstKeys.OWNER, D1000::class)!!
        arena.set(spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!)
        home.set(spawnProps.get(ConstKeys.POSITION, Vector2::class)!!)
        position.set(home)
        other.set(spawnProps.get(ConstKeys.OTHER, Vector2::class)!!)
        body.setCenter(position)
        potShotTime = 0f
        potShotCleared = false
        hidden = false
        driftCorner = 0
        driftHold = 0f
        driftReturning = true
        hurtTime = 0f
        dodging = false
        dodgeTime = 0f
        dodgeCooldown = 0f
        seenThreats.clear()
        thrusterTime = 0f
        flankSide = 0
        stepX = 0
        stepY = 0
        sinceStepX = 0f
        sinceStepY = 0f
        lastDrawnX = snap(position.x)
        lastDrawnY = snap(position.y)
        spinLooping = false
        facingRight = targetPlayer.body.getCenter().x > position.x
        appear()
        placeFixtures()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        owner = null
        seenThreats.clear()
    }

    fun summon(attack: D1000EyeAttack) {
        GameLogger.debug(TAG, "summon(): attack=$attack")
        if (!canAct()) {
            signal()
            return
        }
        shotsFired = 0
        setState(
            when (attack) {
                D1000EyeAttack.BURST -> D1000CoreState.SUMMON
                D1000EyeAttack.SPREAD -> D1000CoreState.SPREAD
                D1000EyeAttack.DIVE -> D1000CoreState.DIVE
                D1000EyeAttack.FLANK -> {
                    val player = targetPlayer.body.getCenter()
                    flankSide = flankSide(arena, player.x)
                    if (flankSide == 0) D1000CoreState.SPREAD else {
                        flankPoint(arena, player, flankSide, target)
                        D1000CoreState.FLANK
                    }
                }
            }
        )
    }

    fun guide(): Boolean {
        GameLogger.debug(TAG, "guide()")
        if (!canAct()) return false
        dodging = false
        inSignal = false
        bounceArmed = false
        setState(D1000CoreState.GUIDE)
        return true
    }

    private fun guidePhase() = if (state == D1000CoreState.GUIDE) owner?.guide()?.phase else null

    fun daze(point: Vector2) {
        if (state != D1000CoreState.IDLE || hidden) return
        GameLogger.debug(TAG, "daze(): point=$point")
        target.set(point)
        arrived = false
        setState(D1000CoreState.DAZED)
    }

    fun isNear(point: Vector2) =
        !hidden && abs(point.x - position.x) <= NEAR_X && abs(point.y - position.y) <= NEAR_Y

    override fun canBeDamagedBy(damager: IDamagerEntity) =
        vulnerable && (hurtTime <= 0f || damager is PlayerSabreSlash) && D1000.weaponDamage(damager) > 0

    override fun canDamage(damageable: IDamageableEntity) = owner?.defeated != true

    fun beginDefeat() {
        GameLogger.debug(TAG, "beginDefeat()")
        val body = owner ?: throw IllegalStateException("$TAG began its defeat with no D1000 owner")
        hidden = false
        dodging = false
        hurtTime = 0f
        spinLooping = false
        val bounds = body.body.getBounds()
        target.set(
            (bounds.getX() + bounds.getWidth() / 2f).coerceIn(arena.getX() + EDGE_MARGIN, arena.getMaxX() - EDGE_MARGIN),
            (bounds.getMaxY() + DEFEAT_OVER).coerceIn(arena.getY() + EDGE_MARGIN, arena.getMaxY() - EDGE_MARGIN)
        )
        setState(D1000CoreState.DEFEAT)
        placeFixtures()
    }

    private fun updateDefeat(delta: Float) {
        when (phase) {
            0 -> if (glide(target, DIVE_SPEED, delta, true)) nextPhase()

            1 -> if (time >= SPIN_DUR) explode()
        }
    }

    private fun explode() {
        GameLogger.debug(TAG, "explode()")
        EYE_EXPLOSIONS.forEach { offset ->
            val explosion = MegaEntityFactory.fetch(Explosion::class)!!
            explosion.spawn(
                props(
                    ConstKeys.OWNER pairTo this,
                    ConstKeys.DAMAGER pairTo false,
                    ConstKeys.POSITION pairTo Vector2(position).mulAdd(offset, ConstVals.PPM.toFloat())
                )
            )
        }
        EYE_PUFFS.forEach { offset ->
            val puff = MegaEntityFactory.fetch(SmokePuff::class)!!
            puff.spawn(props(ConstKeys.POSITION pairTo Vector2(position).mulAdd(offset, ConstVals.PPM.toFloat())))
        }
        services.audio.playSound(SoundAsset.EXPLOSION_2_SOUND, false)
        owner?.onCoreExploded()
        destroy()
    }

    private fun mayTurn() = when (state) {
        D1000CoreState.APPEAR, D1000CoreState.DAZED -> true
        D1000CoreState.IDLE -> !dodging
        D1000CoreState.GUIDE -> guidePhase() != D1000GuidePhase.LOCK
        D1000CoreState.FLANK -> phase == 0
        else -> false
    }

    private fun updateFacing() {
        if (!mayTurn()) return
        val dx = targetPlayer.body.getCenter().x - position.x
        if (dx > FACE_HYSTERESIS) facingRight = true else if (dx < -FACE_HYSTERESIS) facingRight = false
    }

    override fun takeDamageFrom(damager: IDamagerEntity): Boolean {
        if (!canBeDamagedBy(damager)) return false
        GameLogger.debug(TAG, "takeDamageFrom(): damager=$damager")
        val body = owner ?: throw IllegalStateException("$TAG took damage with no D1000 owner")
        services.audio.playSound(SoundAsset.ENEMY_DAMAGE_SOUND, false)
        body.takeEyeDamage(damager, D1000.weaponDamage(damager))
        if (damager !is PlayerSabreSlash) {
            hurtTime = HURT_DUR
            return true
        }
        setState(D1000CoreState.HIT)
        body.stun()
        return true
    }

    private val tuning: D1000Tuning
        get() = (owner ?: throw IllegalStateException("$TAG reads its tuning with no D1000 owner")).tuning

    private fun canAct() = state == D1000CoreState.IDLE || state == D1000CoreState.DAZED

    private fun showing() =
        !hidden && state != D1000CoreState.APPEAR && state != D1000CoreState.HIT && state != D1000CoreState.AWAY

    private fun telling() = (state == D1000CoreState.DIVE && phase == 0)

    private fun setState(next: D1000CoreState) {
        potShotTime = min(potShotTime, tuning.potShotInterval - tuning.potTellDur)
        potShotCleared = false
        state = next
        time = 0f
        phase = 0
    }

    private fun nextPhase() {
        phase++
        time = 0f
    }

    private fun appear() {
        signalPending = true
        setState(D1000CoreState.APPEAR)
        services.audio.playSound(SoundAsset.ELECTRIC_1_SOUND, false)
    }

    private fun signal() {
        owner?.onCoreSignal()
    }

    private fun reverses(d: Float, lastStep: Int, sinceStep: Float) =
        lastStep != 0 && d != 0f && (d > 0f) != (lastStep > 0) && sinceStep < REVERSE_PAUSE

    private fun glide(to: Vector2, speed: Float, delta: Float, reflex: Boolean = false): Boolean {
        val dx = to.x - position.x
        val dy = to.y - position.y
        val distance = sqrt(dx * dx + dy * dy)
        val step = speed * delta
        val sameCell = snap(to.x) == snap(position.x) && snap(to.y) == snap(position.y)
        if (!reflex && !sameCell && (reverses(dx, stepX, sinceStepX) || reverses(dy, stepY, sinceStepY))) return false
        if (distance <= step) {
            position.set(to)
            return true
        }
        position.add(dx / distance * step, dy / distance * step)
        return false
    }

    private fun trackSteps(delta: Float) {
        val x = drawX()
        val y = snap(position.y)
        if (x != lastDrawnX) {
            stepX = if (x > lastDrawnX) 1 else -1
            sinceStepX = 0f
            lastDrawnX = x
        } else sinceStepX += delta
        if (y != lastDrawnY) {
            stepY = if (y > lastDrawnY) 1 else -1
            sinceStepY = 0f
            lastDrawnY = y
        } else sinceStepY += delta
    }

    private fun shoot(trajectory: Vector2) {
        val shot = MegaEntityFactory.fetch(EnemyShot::class)!!
        shot.spawn(
            props(
                ConstKeys.OWNER pairTo owner,
                ConstKeys.TYPE pairTo EnemyShot.Kind.CORE,
                ConstKeys.POSITION pairTo position.cpy(),
                ConstKeys.TRAJECTORY pairTo trajectory
            )
        )
        services.audio.playSound(SoundAsset.BLAST_2_SOUND, false)
    }

    private fun shootAtPlayer() = shoot(targetPlayer.body.getCenter().sub(position).nor().scl(SHOT_SPEED))

    private fun fan(angles: FloatArray) {
        val player = targetPlayer.body.getCenter()
        val aim = atan2(player.y - position.y, player.x - position.x) * MathUtils.radiansToDegrees
        angles.forEach { shoot(Vector2(SHOT_SPEED, 0f).setAngleDeg(aim + it)) }
    }

    private fun updateGuide(delta: Float) {
        val volley = owner?.guide()
        if (volley == null) {
            dodgeCooldown = maxOf(dodgeCooldown, REVERSE_PAUSE)
            setState(D1000CoreState.IDLE)
            return
        }
        position.x = volley.guideX
        val signal = volley.phase == D1000GuidePhase.SIGNAL
        if (volley.horizontal) {
            position.y = volley.guideY
            if (signal && !inSignal) bounceArmed = true
            inSignal = signal
            return
        }
        val rail = home.y + RAIL_RISE
        if (signal && !inSignal) bounceArmed = position.y == rail
        inSignal = signal
        if (!reverses(rail - position.y, stepY, sinceStepY)) position.y = approach(position.y, rail, RAIL_RISE_SPEED, delta)
    }

    private fun updateSpread() {
        when (phase) {
            0 -> if (time >= SPREAD_TELL_DUR) {
                fan(SPREAD_ANGLES)
                nextPhase()
            }

            1 -> {
                if (shotsFired == 0 && tuning.spreadSecondFan && time >= SPREAD_SECOND_DELAY) {
                    fan(SPREAD_ANGLES_SECOND)
                    shotsFired = 1
                }
                if (time >= SPREAD_END_AFTER) {
                    setState(D1000CoreState.IDLE)
                    signal()
                }
            }
        }
    }

    private fun updateDive(delta: Float) {
        when (phase) {
            0 -> if (time >= tuning.diveTellDur) {
                val player = targetPlayer.body.getCenter()
                val dx = player.x - position.x
                val dy = player.y - position.y
                val distance = sqrt(dx * dx + dy * dy)
                val reach = if (distance > DIVE_MAX) DIVE_MAX / distance else 1f
                target.set(position.x + dx * reach, position.y + dy * reach)
                nextPhase()
            }

            1 -> if (glide(target, DIVE_SPEED, delta, true)) nextPhase()

            2 -> if (time >= DIVE_HOLD_DUR) {
                setState(D1000CoreState.IDLE)
                signal()
            }
        }
    }

    private fun updateFlank(delta: Float) {
        when (phase) {
            0 -> {
                flankPoint(arena, targetPlayer.body.getCenter(), flankSide, flankAim)
                if (flankAim.dst(target) >= MIN_MOVE) target.set(flankAim)
                if (glide(target, FLANK_SPEED, delta)) nextPhase()
            }

            1 -> if (time >= tuning.flankPause) nextPhase()

            2 -> if (time >= shotsFired * FLANK_SHOT_GAP) {
                if (owner?.eyeMayShoot(position, 0f, SHOT_SPEED) != false) shootAtPlayer()
                shotsFired++
                if (shotsFired >= tuning.flankShots) nextPhase()
            }

            3 -> if (glide(home, DIVE_SPEED, delta, true)) {
                setState(D1000CoreState.IDLE)
                signal()
            }
        }
    }

    private fun currentAnimation() = animator.currentAnimation

    private fun angry() = when (state) {
        D1000CoreState.IDLE -> !hidden && owner?.fighting == true &&
            potShotTime >= tuning.potShotInterval - tuning.potTellDur

        D1000CoreState.SUMMON -> shotsFired < SHOT_TIMES.size
        D1000CoreState.SPREAD -> if (phase == 0) time >= SPREAD_TELL_DUR - SPREAD_ANGRY_DUR
        else tuning.spreadSecondFan && shotsFired == 0

        D1000CoreState.FLANK -> (phase == 1 && time >= tuning.flankPause / 2f) || phase == 2

        else -> false
    }

    private fun updateDrift(delta: Float) {
        val corner = DRIFT_CORNERS[driftCorner]
        target.set(home.x + corner.x * DRIFT_X, home.y + corner.y * DRIFT_Y)
        if (!glide(target, if (driftReturning) HOME_SPEED else DRIFT_SPEED, delta)) return
        driftReturning = false
        driftHold += delta
        if (driftHold < DRIFT_HOLD) return
        driftHold = 0f
        driftCorner = (driftCorner + 1) % DRIFT_CORNERS.size
    }

    private fun dodgeState() = !hidden && when (state) {
        D1000CoreState.IDLE, D1000CoreState.SUMMON, D1000CoreState.SPREAD -> true
        else -> false
    }

    private fun canDodge() = dodgeState() && owner?.fighting == true && owner?.enraging != true

    private fun forgetGoneThreats() {
        val iter = seenThreats.iterator()
        while (iter.hasNext()) if (!iter.next().existenceState.liveNow) iter.remove()
    }

    private fun updateDodge(delta: Float) {
        forgetGoneThreats()
        if (dodgeCooldown > 0f) dodgeCooldown -= delta
        if (dodging) {
            dodgeTime += delta
            if (dodgeTime >= DODGE_HOLD || !dodgeState()) dodging = false
        }
        if (dodging || dodgeCooldown > 0f || !canDodge()) return
        val threat = findThreat() ?: return
        seenThreats.add(threat)
        if (UtilMethods.random.nextFloat() >= tuning.dodgeChance) return
        if (!aimDodge(dodgeStep.x, dodgeStep.y) && !aimDodge(-dodgeStep.x, -dodgeStep.y)) return
        GameLogger.debug(TAG, "updateDodge(): threat=$threat, target=$dodgeTarget")
        dodging = true
        dodgeTime = 0f
        dodgeCooldown = tuning.dodgeCooldown
    }

    private fun findThreat(): MegaGameEntity? {
        SHOT_TAGS.forEach { tag ->
            MegaGameEntities.getOfTag(tag).forEach { shot -> if (shotThreatens(shot)) return shot }
        }
        MegaGameEntities.getOfTag(PlayerSabreSlash.TAG).forEach { slash -> if (slashThreatens(slash)) return slash }
        return null
    }

    private fun fresh(entity: MegaGameEntity) =
        entity.existenceState.liveNow && !seenThreats.contains(entity) && entity.rootPlayer != null

    private fun shotThreatens(shot: MegaGameEntity): Boolean {
        if (!fresh(shot) || shot !is IBodyEntity) return false
        val velocity = shot.body.physics.velocity
        if (velocity.isZero) return false
        val speed = velocity.len()
        val dirX = velocity.x / speed
        val dirY = velocity.y / speed
        val center = shot.body.getCenter()
        val toX = position.x - center.x
        val toY = position.y - center.y
        val along = toX * dirX + toY * dirY
        val side = dirX * toY - dirY * toX
        if (along <= 0f || along > SHOT_SENSE || abs(side) > SHOT_LANE) return false
        val sign = if (side < 0f) -1f else 1f
        dodgeStep.set(-dirY * sign, dirX * sign)
        if (side == 0f && dodgeStep.y < 0f) dodgeStep.scl(-1f)
        return true
    }

    private fun slashThreatens(slash: MegaGameEntity): Boolean {
        if (!fresh(slash)) return false
        val center = (slash.rootPlayer as IBodyEntity).body.getCenter()
        if (abs(position.x - center.x) > SABRE_REACH_X || abs(position.y - center.y) > SABRE_REACH_Y) return false
        dodgeStep.set(if (position.x < center.x) -0.8f else 0.8f, 0.6f)
        return true
    }

    private fun aimDodge(dirX: Float, dirY: Float): Boolean {
        dodgeTarget.set(position.x + dirX * DODGE_DIST, position.y + dirY * DODGE_DIST)
        dodgeTarget.x = dodgeTarget.x.coerceIn(arena.getX() + EDGE_MARGIN, arena.getMaxX() - EDGE_MARGIN)
        dodgeTarget.y = dodgeTarget.y.coerceIn(arena.getY() + EDGE_MARGIN, arena.getMaxY() - EDGE_MARGIN)
        val view = services.gameCamera.toGameRectangle()
        if (view.contains(position)) {
            dodgeTarget.x = dodgeTarget.x.coerceIn(view.getX() + EDGE_MARGIN, maxOf(view.getX() + EDGE_MARGIN, view.getMaxX() - EDGE_MARGIN))
            dodgeTarget.y = dodgeTarget.y.coerceIn(view.getY() + EDGE_MARGIN, maxOf(view.getY() + EDGE_MARGIN, view.getMaxY() - EDGE_MARGIN))
        }
        return dodgeTarget.dst(position) >= DODGE_DIST / 2f
    }

    private fun updatePotShot(delta: Float) {
        if (hidden || owner?.fighting != true) {
            potShotTime = 0f
            potShotCleared = false
            return
        }
        potShotTime += delta
        val tellStart = tuning.potShotInterval - tuning.potTellDur
        if (potShotTime >= tellStart && !potShotCleared) {
            if (owner?.eyeMayShoot(position, tuning.potShotInterval - potShotTime, SHOT_SPEED) == false) {
                potShotTime = tellStart - POT_RECHECK
                return
            }
            potShotCleared = true
        }
        if (potShotTime < tuning.potShotInterval) return
        potShotCleared = false
        if (owner?.eyeMayShoot(position, 0f, SHOT_SPEED) == false) {
            potShotTime = tellStart - POT_RECHECK
            return
        }
        potShotTime = 0f
        shootAtPlayer()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        time += delta
        thrusterTime += delta
        if (hurtTime > 0f) hurtTime = maxOf(0f, hurtTime - delta)
        updateFacing()
        updateDodge(delta)
        if (state != D1000CoreState.IDLE || dodging) {
            driftReturning = true
            driftHold = 0f
        }
        when (state) {
            D1000CoreState.APPEAR -> if (currentAnimation()?.isFinished() == true && animator.currentKey == "core_appear")
                setState(D1000CoreState.IDLE)

            D1000CoreState.IDLE -> {
                if (dodging) glide(dodgeTarget, DODGE_SPEED, delta, true) else updateDrift(delta)
                if (signalPending && time >= APPEAR_SIGNAL_DELAY) {
                    signalPending = false
                    signal()
                }
                updatePotShot(delta)
            }

            D1000CoreState.SUMMON -> {
                if (dodging) glide(dodgeTarget, DODGE_SPEED, delta, true) else glide(home, HOME_SPEED, delta)
                if (shotsFired < SHOT_TIMES.size && time >= tuning.burstTellDur + SHOT_TIMES[shotsFired]) {
                    shootAtPlayer()
                    shotsFired++
                }
                if (time >= BURST_DUR) {
                    setState(D1000CoreState.IDLE)
                    signal()
                }
            }

            D1000CoreState.DAZED -> if (!arrived) {
                arrived = glide(target, DAZE_SPEED, delta)
                if (arrived) time = 0f
            } else if (time >= DAZED_DUR) setState(D1000CoreState.IDLE)

            D1000CoreState.GUIDE -> updateGuide(delta)

            D1000CoreState.SPREAD -> {
                if (dodging) glide(dodgeTarget, DODGE_SPEED, delta, true) else glide(home, HOME_SPEED, delta)
                updateSpread()
            }

            D1000CoreState.DIVE -> updateDive(delta)

            D1000CoreState.FLANK -> updateFlank(delta)

            D1000CoreState.DEFEAT -> updateDefeat(delta)

            D1000CoreState.HIT -> if (currentAnimation()?.isFinished() == true && animator.currentKey == "core_vanish") {
                hidden = true
                setState(D1000CoreState.AWAY)
            }

            D1000CoreState.AWAY -> if (time >= MOVE_DELAY) {
                val swap = home.cpy()
                home.set(other)
                other.set(swap)
                position.set(home)
                hidden = false
                appear()
            }
        }
        trackSteps(delta)
        body.setCenter(position)
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(SIZE)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        eyeFixtures = TickFrameFixtures(body, FixtureType.DAMAGEABLE, TickFrames.maxBoxes(D1000Frames.EYE_BOXES))
        ballFixtures = TickFrameFixtures(body, FixtureType.DAMAGER, TickFrames.maxBoxes(D1000Frames.CORE_BOXES))

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.physics.velocity.setZero()
            placeFixtures()
        }

        val debugShapes = gdxArrayOf<() -> IDrawableShape?>({ body.getBounds() })
        eyeFixtures.addDebugShapes(debugShapes)
        ballFixtures.addDebugShapes(debugShapes)
        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun drawnKey() = animator.currentKey ?: "core"

    private fun drawnFrame(key: String) =
        TickFrames.artFrame(D1000Frames.CORE[TickFrames.baseKey(key)], key, animator.currentAnimation?.getIndex() ?: 0)

    private fun placeFixtures() {
        val key = drawnKey()
        val base = TickFrames.baseKey(key)
        val def = D1000Frames.CORE[base]
        val frame = drawnFrame(key)
        val x = drawX()
        val y = drawY()
        val active = !hidden && owner?.defeated != true
        eyeFixtures.place(def, D1000Frames.EYE_BOXES[base][frame], x, y, facingRight, active)
        ballFixtures.place(def, D1000Frames.CORE_BOXES[base][frame], x, y, facingRight, active)
    }

    private fun pupilFrame(base: String, frame: Int) = when (base) {
        "core" -> if (frame == D1000Frames.CORE_SHUT_FRAME) null else D1000Frames.PUPIL_OPEN
        "core_stare" -> D1000Frames.PUPIL_OPEN
        "core_angry" -> if (frame == 0) D1000Frames.PUPIL_OPEN else D1000Frames.PUPIL_ANGRY
        "core_hit" -> D1000Frames.PUPIL_HIT
        else -> null
    }

    private fun placePupil(sprite: GameSprite, eyeHidden: Boolean) {
        val key = drawnKey()
        val frame = pupilFrame(TickFrames.baseKey(key), drawnFrame(key))
        sprite.hidden = eyeHidden || frame == null
        if (frame == null) return
        placeOfPupil(pupilPlace)
        sprite.setRegion(pupilFrames[frame])
        sprite.setSize(D1000Frames.PUPIL_SIZE * TickFrames.SCALE, D1000Frames.PUPIL_SIZE * TickFrames.SCALE)
        sprite.setPosition(pupilPlace.x, pupilPlace.y)
    }

    private fun placeOfPupil(out: Vector2): Vector2 {
        val wall = if (beaming()) owner?.guide()?.takeIf { it.horizontal }?.wallX else null
        if (wall != null) toPlayer.set(if (wall > position.x) 1f else -1f, 0f)
        else if (beaming()) toPlayer.set(0f, -1f) else toPlayer.set(targetPlayer.body.getCenter()).sub(position).nor()
        D1000Frames.pupilOffset(if (facingRight) -toPlayer.x else toPlayer.x, -toPlayer.y, pupilOffset)
        val restX = D1000Frames.PUPIL_REST_X + pupilOffset[0]
        val artLeft = if (facingRight) D1000Frames.CORE["core"].width - restX - D1000Frames.PUPIL_SIZE else restX
        val artTop = D1000Frames.PUPIL_REST_Y + pupilOffset[1]
        val def = D1000Frames.CORE["core"]
        return out.set(
            drawX() - def.originX * TickFrames.SCALE + artLeft * TickFrames.SCALE,
            drawY() + def.originY * TickFrames.SCALE - (artTop + D1000Frames.PUPIL_SIZE) * TickFrames.SCALE
        )
    }

    private fun beaming() = guidePhase() == D1000GuidePhase.LOCK

    private fun flaring() = when (state) {
        D1000CoreState.GUIDE -> guidePhase().let { it == D1000GuidePhase.SIGNAL || it == D1000GuidePhase.FLIGHT }
        D1000CoreState.DIVE -> phase == 1
        D1000CoreState.FLANK -> phase == 0 || phase == 3
        D1000CoreState.DEFEAT -> phase == 0
        else -> false
    }

    private fun placeThruster(sprite: GameSprite) {
        sprite.hidden = !showing() || hurtBlink() || spinning
        if (sprite.hidden) return
        sprite.setRegion(thrusterFrames[thrusterFrame(thrusterTime, flaring())])
        val size = D1000Frames.THRUSTER_SIZE * TickFrames.SCALE
        val def = D1000Frames.CORE["core"]
        sprite.setSize(size, size)
        sprite.setPosition(
            drawX() - size / 2f,
            drawY() + (def.originY - D1000Frames.THRUSTER_TOP) * TickFrames.SCALE - size
        )
    }

    private fun placeBeam(sprite: GameSprite) {
        sprite.hidden = !beaming() || !showing() || hurtBlink()
        if (sprite.hidden) return
        placeOfPupil(pupilPlace)
        val width = D1000Frames.BEAM_WIDTH * TickFrames.SCALE
        val volley = owner?.guide()
        sprite.setRegion(beamFrames[beamFrame(volley?.phaseTime ?: 0f, D1000Orbs.MARK_DUR)])
        val pupilX = pupilPlace.x + D1000Frames.PUPIL_SIZE * TickFrames.SCALE / 2f
        if (volley != null && volley.horizontal) {
            val pupilY = pupilPlace.y + D1000Frames.PUPIL_SIZE * TickFrames.SCALE / 2f
            val length = abs(volley.wallX - pupilX)
            sprite.setSize(width, length)
            sprite.setPosition(pupilX - width / 2f, pupilY - length)
            sprite.setOrigin(width / 2f, length)
            sprite.rotation = if (volley.wallX > pupilX) 90f else -90f
            return
        }
        val floor = arena.getY()
        sprite.rotation = 0f
        sprite.setSize(width, pupilPlace.y + TickFrames.SCALE - floor)
        sprite.setPosition(pupilX - width / 2f, floor)
    }

    private fun glowFrame(): Int? {
        if (!showing() || hurtBlink()) return null
        val tellDur: Float
        val elapsed: Float
        when (state) {
            D1000CoreState.IDLE -> {
                if (!angry()) return null
                tellDur = tuning.potTellDur
                elapsed = potShotTime - (tuning.potShotInterval - tellDur)
            }

            D1000CoreState.SUMMON -> {
                if (shotsFired > 0) return null
                tellDur = tuning.burstTellDur + SHOT_TIMES[0]
                elapsed = time
            }

            D1000CoreState.SPREAD -> {
                if (phase != 0) return null
                tellDur = SPREAD_TELL_DUR
                elapsed = time
            }

            D1000CoreState.FLANK -> {
                if (phase != 1) return null
                tellDur = tuning.flankPause / 2f
                elapsed = time - tellDur
            }

            else -> return null
        }
        if (elapsed < 0f || elapsed >= tellDur) return null
        return glowFrame(elapsed, tellDur)
    }

    private fun placeGlow(sprite: GameSprite) {
        val frame = glowFrame()
        sprite.hidden = frame == null
        if (frame == null) return
        sprite.setRegion(glowFrames[frame])
        TickFrames.place(sprite, D1000Frames.EYE_GLOW_DEF, drawX(), drawY(), facingRight)
    }

    private fun jitter() = if (telling() && flickerOn(time)) JITTER else 0f

    private fun snap(value: Float) = MathUtils.round(value / TickFrames.SCALE) * TickFrames.SCALE

    private fun drawX() = snap(position.x) + jitter()

    private fun drawY() = snap(position.y + bounceY())

    private fun bounceY(): Float {
        val volley = if (state == D1000CoreState.GUIDE) owner?.guide() else null
        if (volley == null || volley.phase != D1000GuidePhase.SIGNAL || !bounceArmed) return 0f
        return bounce(volley.phaseTime, tuning.guideSignalDur)
    }

    private fun hurtBlink() = hurtTime > 0f && flickerOn(hurtTime)

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 6))
        val component = SpritesComponent(sprite)
        component.putPreProcess { _, _ ->
            TickFrames.place(sprite, D1000Frames.CORE[TickFrames.baseKey(drawnKey())], drawX(), drawY(), facingRight)
            sprite.hidden = hidden || hurtBlink()
        }
        val beamSprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 7))
        component.putSprite(BEAM, beamSprite)
        component.putPreProcess(BEAM) { _, _ -> placeBeam(beamSprite) }
        val pupilSprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 8))
        component.putSprite(PUPIL, pupilSprite)
        component.putPreProcess(PUPIL) { _, _ -> placePupil(pupilSprite, hidden || hurtBlink()) }
        val thrusterSprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 4))
        component.putSprite(THRUSTER, thrusterSprite)
        component.putPreProcess(THRUSTER) { _, _ -> placeThruster(thrusterSprite) }
        val glowSprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 5))
        component.putSprite(GLOW, glowSprite)
        component.putPreProcess(GLOW) { _, _ -> placeGlow(glowSprite) }
        return component
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = { current ->
            if (!spinLooping && current == "core_spin" && animations[current].isFinished()) spinLooping = true
            when {
                state == D1000CoreState.APPEAR -> "core_appear"
                state == D1000CoreState.HIT || state == D1000CoreState.AWAY -> "core_vanish"
                spinning -> if (spinLooping) "core_spin" + TickFrames.LOOP_SUFFIX else "core_spin"
                hurtTime > 0f -> "core_hit"
                angry() -> "core_angry"
                state == D1000CoreState.SPREAD && phase == 0 -> "core_appear"
                state == D1000CoreState.IDLE -> "core"
                else -> "core_stare"
            }
        }
        animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    override fun getType() = EntityType.BOSS
}
