package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.animations.IAnimation
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.UtilMethods
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.enums.Size
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.extensions.objectSetOf
import com.maverick.game.engine.common.extensions.orderedMapOf
import com.maverick.game.engine.common.interfaces.IFaceable
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.shapes.IDrawableShape
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.entities.contracts.IAnimatedEntity
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.players.IPlayer
import com.maverick.game.engine.players.registeredSlots
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.megaman.maverick.game.entities.players.PlayerSabreSlash
import com.megaman.maverick.game.entities.utils.TickFrameDef
import com.megaman.maverick.game.entities.utils.TickFrameFixtures
import com.megaman.maverick.game.entities.utils.TickFrames
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.difficulty.DifficultyMode
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.contracts.targetPlayer
import com.megaman.maverick.game.entities.decorations.D1000BombMarker
import com.megaman.maverick.game.entities.decorations.D1000DebrisWarning
import com.megaman.maverick.game.entities.decorations.FallingPiece
import com.megaman.maverick.game.entities.decorations.D1000RubblePile
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.hazards.D1000Pillar
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.D1000Bomb
import com.megaman.maverick.game.entities.state.BossPhase
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.events.PlayerEventKeys
import com.megaman.maverick.game.utils.misc.StunType
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import com.megaman.maverick.game.entities.bosses.d1000.D1000TrapGuard
import com.megaman.maverick.game.entities.bosses.d1000.D1000Lamp
import com.megaman.maverick.game.entities.bosses.d1000.D1000Escape
import com.megaman.maverick.game.entities.bosses.d1000.D1000BombPattern
import com.megaman.maverick.game.entities.bosses.d1000.D1000Orbs
import com.megaman.maverick.game.entities.bosses.d1000.D1000Tier
import com.megaman.maverick.game.entities.bosses.d1000.D1000Distance
import com.megaman.maverick.game.entities.bosses.d1000.D1000Action
import com.megaman.maverick.game.entities.bosses.d1000.D1000Context
import com.megaman.maverick.game.entities.bosses.d1000.D1000Plan
import com.megaman.maverick.game.entities.bosses.d1000.D1000Director
import com.megaman.maverick.game.entities.bosses.d1000.D1000Tuning
import com.megaman.maverick.game.entities.bosses.d1000.D1000Frames
import com.megaman.maverick.game.entities.bosses.d1000.D1000Defeat
import com.megaman.maverick.game.entities.bosses.d1000.flickerOn
import com.megaman.maverick.game.entities.bosses.d1000.approach
import com.megaman.maverick.game.entities.bosses.d1000.D1000Move
import com.megaman.maverick.game.entities.bosses.d1000.D1000Ram
import com.megaman.maverick.game.entities.bosses.d1000.D1000Volley
import com.megaman.maverick.game.entities.bosses.d1000.D1000SlamWaves
import com.megaman.maverick.game.entities.bosses.d1000.D1000Crouch
import com.megaman.maverick.game.entities.bosses.d1000.D1000Ceiling

internal enum class D1000State { ENTRY, THINK, WALK, CROUCH, BOMB, SUMMON, RAM, STUN, CEILING, ENRAGE }

// TODO: the D-1000 fight (its behaviors, mechanics, tuning and anti-trap rules) was AI-generated from owner play tests; it needs more scrutiny and refinement (see the branch NOTES of 081f, section "AI-generated fight")
class D1000(services: MegaServices) :
    AbstractBoss(services, dmgDuration = DAMAGE_DURATION, defeatDur = D1000Defeat.DEFEAT_DUR, size = Size.LARGE), IAnimatedEntity,
    IFaceable {

    companion object {
        const val TAG = "D1000"
        const val LAMP = "lamp"
        const val FACE = "face"
        const val GLOW = "glow"
        const val MOUTH = "mouth"
        const val SCREW = "screw"
        const val CHARGE = "charge"
        const val CHARGE_GLOW = "charge_glow"
        const val PUFF = "puff"

        const val HALF_HEALTH = ConstVals.MAX_HEALTH / 2
        const val FULL_CHARGE_DAMAGE = 4
        const val HALF_CHARGE_DAMAGE = 2
        const val BULLET_DAMAGE = 1
        const val SABRE_DAMAGE = 2
        const val SHIELDED_SABRE_DAMAGE = 1
        const val SHIELDED_CHARGE_DAMAGE = 2
        const val STUN_DAMAGE = 4

        private const val DAMAGE_DURATION = 0.5f

        fun weaponDamage(damager: IDamagerEntity) = when (damager) {
            is Bullet -> BULLET_DAMAGE
            is ChargedShot -> if (damager.fullyCharged) FULL_CHARGE_DAMAGE else HALF_CHARGE_DAMAGE
            is PlayerSabreSlash -> SABRE_DAMAGE
            else -> 0
        }

        private const val BODY_WIDTH = 4.375f * ConstVals.PPM
        private const val BODY_HEIGHT = 3.59375f * ConstVals.PPM
        internal const val BODY_CENTER_ABOVE_ORIGIN = 0.703125f * ConstVals.PPM
        internal const val HALF_WIDTH = 2.1875f * ConstVals.PPM

        internal const val CROUCH_HEIGHT = 1.5f * ConstVals.PPM
        private const val HOVER_MARGIN = 0.5f * ConstVals.PPM

        private fun artBelowOrigin(anim: String) =
            D1000Frames.BODY[anim]!!.let { (it.height - it.originY) * TickFrames.SCALE }

        private val HOVER_ANIMS = listOf("stand", "walk", "throw", "ram")
        private val HOVER_ART_BELOW_ORIGIN = HOVER_ANIMS.maxOf { artBelowOrigin(it) }
        private val STAGGER_ART_BELOW_ORIGIN = artBelowOrigin("stagger")
        private const val ENTRY_HEIGHT = 8f * ConstVals.PPM

        private const val CORE_CENTER_GAP = 3.25f * ConstVals.PPM
        private const val CORE_HEIGHT = 8f * ConstVals.PPM

        private const val FALL_GRAVITY = 42f * ConstVals.PPM
        internal const val RISE_SPEED = 4.25f * ConstVals.PPM
        internal const val SHAKE_STEP = 0.0625f * ConstVals.PPM

        private const val ENRAGE_DUR = 1.2f
        private const val WALK_DUR = 1.5f
        private const val NEAR_PLAYER = 2.34375f * ConstVals.PPM
        private const val ENTRY_CORE_DELAY = 1f
        private const val STAND_UP_HOLD_DUR = 0.5f
        private const val STUN_DAMAGE_DELAY = 0.6f

        const val BOMB_FORWARD = 2.1875f * ConstVals.PPM
        const val BOMB_UP = 0.9375f * ConstVals.PPM

        internal const val DEBRIS_TOP = 0.25f * ConstVals.PPM

        private const val SHAKE_INTERVAL = 0.1f
        private const val SHAKE_Y = 0.005f
        internal const val LAND_SHAKE = 0.33f
        private const val STUN_SHAKE = 1f

        private const val IMPACT_SHAKE_INTERVAL = 0f

        private const val MID_PLAYER = 5f * ConstVals.PPM
        private const val WALL_TOUCH = 0.25f * ConstVals.PPM
        private const val OFF_FLOOR = 0.5f * ConstVals.PPM
        internal const val AIR_WINDOW = 6f
        internal const val AIR_SHARE = 0.4f

        const val WALK_GAP = 1.5f * ConstVals.PPM
        const val FAIR_POCKET = 7f * ConstVals.PPM
        const val RETREAT_MAX_DUR = 2f
        private val POCKET_ANIMS = arrayOf("stand", "walk", "throw")
        private val ORB_ACTIONS = objectSetOf(D1000Action.BOMB, D1000Action.BRACKET, D1000Action.SWEEP)
        private const val WALK_SLICES = 6
    }

    private enum class EntryStep { FALL, CORE_DELAY, CORE_SIGNAL, STAND_UP, RISE }

    private enum class ThinkStep { START, RETREAT, CHOOSE }

    private enum class WalkStep { START, GO }

    private enum class SummonStep { START, WAIT }

    private enum class StunStep(val down: Boolean) {
        STAGGER(false), FALL(false), DAMAGE(true), SIGNAL(true), STAND_UP(true), RISE(false)
    }

    private enum class EnrageStep { START, SHAKE }

    private enum class MoveStep { MOVE }

    override val damageNegotiator = object : BossDamageNegotiator() {
        override fun get(damager: IDamagerEntity) = when {
            damager is ChargedShotExplosion -> 0
            damager is ChargedShot && !downed -> SHIELDED_CHARGE_DAMAGE
            damager is PlayerSabreSlash && !downed -> SHIELDED_SABRE_DAMAGE
            damager is Bullet || damager is ChargedShot || damager is PlayerSabreSlash -> weaponDamage(damager)
            else -> super.get(damager)
        }
    }

    override lateinit var facing: Facing

    internal val arena = GameRectangle()
    internal val origin = Vector2()
    internal var speedY = 0f
    internal var speedX = 0f
    internal var shakeOffset = 0f

    private var state = D1000State.ENTRY
    private var step: Enum<*> = EntryStep.FALL
    private var time = 0f

    private var lastAction: D1000Action? = null
    private var beforeLastAction: D1000Action? = null
    private var followUp: D1000Action? = null
    private var cyclesSinceCombo = 0
    internal var airShare = 0f
        private set
    internal var touchedPlayer = false
    var tuning = D1000Tuning.NORMAL
        private set
    private var eyeAttack = D1000EyeAttack.BURST
    private val ram = D1000Ram(this)
    private val crouch = D1000Crouch(this)
    private val ceiling = D1000Ceiling(this)
    private val volley = D1000Volley(this)
    internal val slamWaves = D1000SlamWaves(this)
    private var entryDone = false
    private var plan: D1000Plan? = null
    private var planTried = false
    private var followUpTold = false
    private val lamp = D1000Lamp()

    val guard = D1000TrapGuard(this)
    private var xStunLeft = 0f
    private val liveBoxes = Array<GameRectangle>()
    private val boxPool = Array<GameRectangle>()
    private val forecastBoxes = Array<GameRectangle>()
    private val pocketBox = GameRectangle()

    internal var core: D1000Core? = null
        private set
    private var coreSignaled = false
    private val defeat = D1000Defeat(this)
    val pile: D1000RubblePile?
        get() = defeat.pile

    private lateinit var bodyFixtures: TickFrameFixtures
    private lateinit var damagerFixtures: TickFrameFixtures
    private lateinit var damageableFixtures: TickFrameFixtures

    private val animations = ObjectMap<String, IAnimation>()
    private var anim = "stand"
    private var animLooping = false
    private lateinit var animator: Animator

    val stunned: Boolean
        get() = state == D1000State.STUN
    private val downed: Boolean
        get() = stunned && (step as StunStep).down

    internal val floor: Float
        get() = arena.getY()
    internal val lowestHover: Float
        get() = max(floor + HOVER_MARGIN - (BODY_CENTER_ABOVE_ORIGIN - BODY_HEIGHT / 2f), floor + HOVER_ART_BELOW_ORIGIN)
    internal val highestHover: Float
        get() = arena.getMaxY() - HOVER_MARGIN - (BODY_CENTER_ABOVE_ORIGIN + BODY_HEIGHT / 2f)
    internal val minX: Float
        get() = arena.getX() + HALF_WIDTH
    internal val maxX: Float
        get() = arena.getMaxX() - HALF_WIDTH
    internal val middleX: Float
        get() = arena.getX() + arena.getWidth() / 2f

    private val move: D1000Move?
        get() = when (state) {
            D1000State.RAM -> ram
            D1000State.CROUCH -> crouch
            D1000State.CEILING -> ceiling
            D1000State.BOMB -> volley
            else -> null
        }

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        val atlas = services.assets.getTextureAtlas(TextureAsset.BOSSES_3.source)
        TickFrames.animations(atlas, TAG, D1000Frames.BODY, animations)
        lamp.load(atlas)
        FallingPiece.load(services, D1000Defeat.DEBRIS, TextureAsset.BOSSES_3)
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        spawnProps.put(ConstKeys.ORB, true)
        super.onSpawn(spawnProps)

        arena.set(spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!)
        origin.set(middleX, floor + ENTRY_HEIGHT)
        placeBody()
        speedX = 0f
        speedY = 0f
        shakeOffset = 0f
        slamWaves.reset()
        facing = Facing.LEFT

        lastAction = null
        beforeLastAction = null
        followUp = null
        tuning = D1000Tuning.of(D1000Tier.NORMAL, services.gameState.getDifficultyMode() == DifficultyMode.HARD)
        cyclesSinceCombo = D1000Director.comboGap(tuning.tier, tuning.hard)
        airShare = 0f
        touchedPlayer = false
        volley.reset()
        entryDone = false
        plan = null
        planTried = false
        xStunLeft = 0f
        guard.refused.clear()
        lamp.tell(null, false)

        core = null
        coreSignaled = false
        defeat.reset()

        animLooping = false
        anim = "fall"
        setState(D1000State.ENTRY)
        placeFixtures()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        core?.let { if (it.existenceState.liveAfterUpdate) it.destroy() }
        core = null
        defeat.reset()
        volley.dropChargeMark()
    }

    override fun isReady(delta: Float) = entryDone

    override fun canBeDamagedBy(damager: IDamagerEntity) = (!stunned || downed) && super.canBeDamagedBy(damager)

    override fun onPlayerDamaged(damager: IDamagerEntity, player: IPlayer) {
        if (damager == this) touchedPlayer = true
    }

    override fun triggerDefeat() {
        super.triggerDefeat()
        volley.dropChargeMark()
        lamp.tell(null, false)
        speedX = 0f
        speedY = 0f
        shakeOffset = 0f
        val eye = core
        if (eye != null && eye.existenceState.liveAfterUpdate) eye.beginDefeat() else core = null
        placeFixtures()
        defeat.begin()
    }

    fun onCoreExploded() {
        GameLogger.debug(TAG, "onCoreExploded()")
        core = null
    }

    val broken: Boolean
        get() = defeated && defeat.broken

    override fun onDefeated(delta: Float) {
        if (!defeat.broken) explodeOnDefeat(delta)
        if (defeat.update(delta, core == null)) defeatTimer.setToEnd()
    }

    override fun spawnDefeatExplosion() = defeat.explosionInBody()

    override fun spawnExplosionOrbs(spawn: Vector2) = super.spawnExplosionOrbs(defeat.pileCenter())

    val overdrive: Boolean
        get() = tuning.tier == D1000Tier.OVERDRIVE

    val fighting: Boolean
        get() = bossComponent.phase == BossPhase.FIGHT && !defeated && !stunned && state != D1000State.ENTRY

    val enraging: Boolean
        get() = state == D1000State.ENRAGE

    val shielded: Boolean
        get() = !downed && !defeated

    val retreating: Boolean
        get() = state == D1000State.THINK && step == ThinkStep.RETREAT

    fun pocket(): Float {
        val player = targetPlayer.body
        val left = player.getCenter().x < origin.x
        var front = Float.NaN
        POCKET_ANIMS.forEach { anim ->
            val def = D1000Frames.BODY[anim]
            D1000Frames.BODY_BOXES[anim].forEach { boxes ->
                for (i in 0 until boxes.size / 4) {
                    val box = TickFrames.placeBox(pocketBox, def, boxes, i * 4, origin.x, origin.y, !left)
                    if (box.getY() >= player.getMaxY() || box.getMaxY() <= player.getY()) continue
                    front = when {
                        front.isNaN() -> if (left) box.getX() else box.getMaxX()
                        left -> min(front, box.getX())
                        else -> max(front, box.getMaxX())
                    }
                }
            }
        }
        if (front.isNaN()) return arena.getWidth()
        return if (left) front - arena.getX() else arena.getMaxX() - front
    }

    fun cornered() = pocket() < FAIR_POCKET

    fun takeEyeDamage(damager: IDamagerEntity, damage: Int) {
        if (defeated) return
        GameLogger.debug(TAG, "takeEyeDamage(): damager=$damager, damage=$damage")
        translateHealth(-editDamageFrom(damager, damage))
    }

    private fun trackAir(delta: Float) {
        val air = if (targetPlayer.body.getY() >= floor + OFF_FLOOR) 1f else 0f
        airShare += (air - airShare) * min(delta / AIR_WINDOW, 1f)
    }

    internal fun guide(): D1000Volley? = if (state == D1000State.BOMB && volley.guiding) volley else null

    internal val followUpQueued: Boolean
        get() = followUp != null

    fun onCoreSignal() {
        GameLogger.debug(TAG, "onCoreSignal()")
        coreSignaled = true
    }

    fun stun() {
        if (defeated || state == D1000State.ENTRY || stunned) return
        GameLogger.debug(TAG, "stun()")
        coreSignaled = false
        followUp = null
        speedX = 0f
        speedY = 0f
        shakeOffset = 0f
        lamp.tell(null, false)
        setAnim("stagger")
        setState(D1000State.STUN)
        requestToPlaySound(SoundAsset.ENEMY_DAMAGE_SOUND, false)
    }

    private fun setState(next: D1000State) {
        GameLogger.debug(TAG, "setState(): $state -> $next")
        if (state == D1000State.BOMB) volley.dropChargeMark()
        state = next
        goTo(
            when (next) {
                D1000State.ENTRY -> EntryStep.FALL
                D1000State.THINK -> ThinkStep.START
                D1000State.WALK -> WalkStep.START
                D1000State.SUMMON -> SummonStep.START
                D1000State.STUN -> StunStep.STAGGER
                D1000State.ENRAGE -> EnrageStep.START
                D1000State.RAM, D1000State.CROUCH, D1000State.CEILING, D1000State.BOMB -> MoveStep.MOVE
            }
        )
        move?.begin()
    }

    private fun goTo(next: Enum<*>) {
        step = next
        time = 0f
    }

    internal fun setAnim(key: String) {
        if (anim != key) {
            anim = key
            animLooping = false
        }
    }

    private fun animFinished() =
        TickFrames.baseKey(animator.currentKey ?: "") == anim && animator.currentAnimation?.isFinished() == true

    internal fun playerX() = targetPlayer.body.getCenter().x

    internal fun hoverTarget() =
        (targetPlayer.body.getCenter().y - BODY_CENTER_ABOVE_ORIGIN).coerceIn(lowestHover, highestHover)

    private fun placeBody() = body.setCenter(origin.x, origin.y + BODY_CENTER_ABOVE_ORIGIN)

    internal fun facePlayer() {
        facing = if (playerX() < origin.x) Facing.LEFT else Facing.RIGHT
    }

    internal fun shake(duration: Float) {
        services.events.submitEvent(
            Event(
                EventType.SHAKE_CAM, props(
                    ConstKeys.DURATION pairTo duration,
                    ConstKeys.INTERVAL pairTo SHAKE_INTERVAL,
                    ConstKeys.X pairTo 0f,
                    ConstKeys.Y pairTo SHAKE_Y * ConstVals.PPM
                )
            )
        )
    }

    internal fun land(duration: Float) {
        requestToPlaySound(SoundAsset.THUMP_SOUND, false)
        requestToPlaySound(SoundAsset.QUAKE_SOUND, false)
        shake(duration)
    }

    internal fun impact(duration: Float, shakeX: Float, shakeY: Float, stun: Boolean) {
        requestToPlaySound(SoundAsset.THUMP_SOUND, false)
        requestToPlaySound(SoundAsset.QUAKE_SOUND, false)
        services.events.submitEvent(
            Event(
                EventType.SHAKE_CAM, props(
                    ConstKeys.DURATION pairTo duration,
                    ConstKeys.INTERVAL pairTo IMPACT_SHAKE_INTERVAL,
                    ConstKeys.X pairTo shakeX,
                    ConstKeys.Y pairTo shakeY
                )
            )
        )
        if (!stun) return
        xStunLeft = tuning.xStunDur
        services.events.submitEvent(
            Event(
                EventType.STUN_PLAYER, props(
                    ConstKeys.TYPE pairTo StunType.STUN_BOUNCE_IF_ON_SURFACE,
                    PlayerEventKeys.SLOTS pairTo players.registeredSlots(),
                    ConstKeys.DURATION pairTo tuning.xStunDur
                )
            )
        )
    }

    internal fun fall(delta: Float, gravity: Float, target: Float): Boolean {
        speedY += gravity * delta
        origin.y -= speedY * delta
        if (origin.y > target) return false
        origin.y = target
        speedY = 0f
        return true
    }

    internal fun moveToHover(delta: Float, hold: Boolean = false): Boolean {
        val target = hoverTarget()
        if (hold && approachBlocked(target)) return true
        origin.y = approach(origin.y, target, RISE_SPEED, delta)
        return origin.y == target
    }

    internal fun moveX(delta: Float, speed: Float): Boolean {
        origin.x += speed * facing.value * delta
        val clamped = origin.x.coerceIn(minX, maxX)
        val hit = clamped != origin.x
        origin.x = clamped
        return hit
    }

    private fun coreX(far: Boolean) = middleX + if (far) CORE_CENTER_GAP else -CORE_CENTER_GAP

    private fun spawnCore(far: Boolean) {
        val x = coreX(far)
        val spawned = MegaEntityFactory.fetch(D1000Core::class)!!
        spawned.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.POSITION pairTo Vector2(x, floor + CORE_HEIGHT),
                ConstKeys.OTHER pairTo Vector2(coreX(!far), floor + CORE_HEIGHT),
                ConstKeys.BOUNDS pairTo GameRectangle(arena)
            )
        )
        core = spawned
    }

    fun chargeX() = origin.x + BOMB_FORWARD * facing.value

    fun chargeY() = origin.y + BOMB_UP + shakeOffset

    internal fun throwBomb(targetX: Float, marker: D1000BombMarker? = null, wallY: Float? = null) {
        val spawn = Vector2(origin.x + BOMB_FORWARD * facing.value, origin.y + BOMB_UP)
        val bomb = MegaEntityFactory.fetch(D1000Bomb::class)!!
        val spawnProps = props(
            ConstKeys.OWNER pairTo this,
            ConstKeys.POSITION pairTo spawn,
            ConstKeys.TARGET pairTo if (wallY == null) D1000Orbs.clampLane(arena, targetX) else targetX,
            ConstKeys.BOUNDS pairTo GameRectangle(arena)
        )
        if (wallY != null) spawnProps.put(ConstKeys.Y, wallY)
        if (marker != null) spawnProps.put(ConstKeys.CHILD, marker)
        bomb.spawn(spawnProps)
        requestToPlaySound(SoundAsset.BLAST_1_SOUND, false)
    }

    private fun updateEntry(delta: Float) {
        when (step) {
            EntryStep.FALL -> if (fall(delta, FALL_GRAVITY, floor + CROUCH_HEIGHT)) {
                setAnim("land")
                land(LAND_SHAKE)
                goTo(EntryStep.CORE_DELAY)
            }

            EntryStep.CORE_DELAY -> if (time >= ENTRY_CORE_DELAY) {
                spawnCore(playerX() < middleX)
                coreSignaled = false
                goTo(EntryStep.CORE_SIGNAL)
            }

            EntryStep.CORE_SIGNAL -> if (coreSignaled) {
                setAnim("stand_up")
                goTo(EntryStep.STAND_UP)
            }

            EntryStep.STAND_UP -> if (time >= STAND_UP_HOLD_DUR) {
                setAnim("walk")
                goTo(EntryStep.RISE)
            }

            EntryStep.RISE -> if (moveToHover(delta)) {
                setAnim("stand")
                entryDone = true
                setState(D1000State.THINK)
            }
        }
    }

    private fun updateThink(delta: Float) {
        if (step == ThinkStep.START) {
            plan = null
            planTried = false
            lamp.tell(null, false)
            if (!overdrive && getCurrentHealth() <= HALF_HEALTH) {
                setState(D1000State.ENRAGE)
                return
            }
            facePlayer()
            if (cornered()) {
                GameLogger.debug(TAG, "updateThink(): X is cornered, retreat")
                setAnim("walk")
                goTo(ThinkStep.RETREAT)
                return
            }
            setAnim("stand")
            goTo(ThinkStep.CHOOSE)
        }
        if (step == ThinkStep.RETREAT) {
            facePlayer()
            val atWall = moveX(delta, -tuning.walkSpeed)
            if (!cornered() || atWall || time >= RETREAT_MAX_DUR) {
                setAnim("stand")
                goTo(ThinkStep.CHOOSE)
            }
            return
        }
        facePlayer()
        val thinkDur = tuning.thinkDur
        if (!planTried && time >= thinkDur - D1000Lamp.LEAD_DUR) {
            planTried = true
            plan = choosePlan(max(thinkDur - time, 0f))
            plan?.let { lamp.tell(it.action, true) }
        }
        if (time < thinkDur) return

        val plan = this.plan
        if (plan == null) {
            GameLogger.debug(TAG, "updateThink(): no safe action, think again")
            setState(D1000State.THINK)
            return
        }
        this.plan = null
        GameLogger.debug(TAG, "updateThink(): plan=$plan")
        followUp = plan.followUp
        cyclesSinceCombo = if (plan.followUp != null) 0 else cyclesSinceCombo + 1
        startAction(plan.action)
    }

    private fun context(r: Int): D1000Context {
        val player = targetPlayer.body
        val distance = abs(playerX() - origin.x)
        val onWall = player.getY() >= floor + OFF_FLOOR &&
            (player.getX() <= arena.getX() + WALL_TOUCH || player.getMaxX() >= arena.getMaxX() - WALL_TOUCH)
        return D1000Context(
            r = r,
            tier = tuning.tier,
            last = lastAction,
            beforeLast = beforeLastAction,
            distance = when {
                distance <= NEAR_PLAYER -> D1000Distance.NEAR
                distance <= MID_PLAYER -> D1000Distance.MID
                else -> D1000Distance.FAR
            },
            playerOnWall = onWall,
            playerAbove = player.getCenter().y > body.getMaxY(),
            playerNearEye = core?.isNear(player.getCenter()) == true,
            cyclesSinceCombo = cyclesSinceCombo,
            hard = tuning.hard,
            playerAirborne = airShare > AIR_SHARE
        )
    }

    private fun startAction(action: D1000Action) {
        GameLogger.debug(TAG, "startAction(): $action")
        beforeLastAction = lastAction
        lastAction = action
        lamp.tell(action, false)
        followUpTold = false
        setState(
            when (action) {
                D1000Action.WALK -> D1000State.WALK
                D1000Action.CROUCH -> D1000State.CROUCH
                D1000Action.SUMMON -> D1000State.SUMMON
                D1000Action.RAM -> D1000State.RAM
                D1000Action.CEILING -> D1000State.CEILING
                D1000Action.SPREAD, D1000Action.DIVE, D1000Action.FLANK -> D1000State.SUMMON
                D1000Action.BOMB, D1000Action.BRACKET, D1000Action.SWEEP, D1000Action.FENCE, D1000Action.WALL -> D1000State.BOMB
            }
        )
        eyeAttack = when (action) {
            D1000Action.SPREAD -> D1000EyeAttack.SPREAD
            D1000Action.DIVE -> D1000EyeAttack.DIVE
            D1000Action.FLANK -> D1000EyeAttack.FLANK
            else -> D1000EyeAttack.BURST
        }
        volley.pattern = D1000BombPattern.of(action)
    }

    internal fun finishAction(next: D1000State) {
        val queued = followUp
        followUp = null
        if (queued != null && (followUpTold || allowsNow(queued, 0f))) {
            startAction(queued)
        } else {
            lamp.tell(null, false)
            setState(next)
        }
    }

    private fun updateWalk(delta: Float) {
        facePlayer()
        if (step == WalkStep.START) {
            setAnim("walk")
            goTo(WalkStep.GO)
        }
        if (walkGap(bodyBoxes()) <= WALK_GAP) {
            finishAction(D1000State.THINK)
            return
        }
        moveX(delta, tuning.walkSpeed)
        if (time >= WALK_DUR) finishAction(D1000State.THINK)
    }

    fun chargeProgress(): Float? = if (state != D1000State.BOMB || defeated) null else volley.chargeProgress()

    private fun updateSummon() {
        if (step == SummonStep.START) {
            setAnim("stand")
            coreSignaled = false
            core?.summon(eyeAttack)
            goTo(SummonStep.WAIT)
        }
        facePlayer()
        // TODO: a SUMMON follow-up (RAM) shows its lamp color only when it starts, because SUMMON ends at the core
        //  signal, which the body cannot see ahead (see the 081f NOTES.md "Telegraphs", row "Combo follow-ups")
        if (coreSignaled || core == null) finishAction(D1000State.WALK)
    }

    internal fun laneInArena(x: Float) = D1000Orbs.laneInArena(arena, x)

    internal fun spawnDebris(x: Float, y: Float, spread: Float = 0f) {
        val warning = MegaEntityFactory.fetch(D1000DebrisWarning::class)!!
        warning.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.POSITION pairTo Vector2(x, y),
                ConstKeys.WIDTH pairTo spread,
                ConstKeys.BOUNDS pairTo GameRectangle(arena),
                ConstKeys.MAX_Y pairTo arena.getMaxY()
            )
        )
    }

    private fun updateEnrage() {
        if (step == EnrageStep.START) {
            setAnim("stand")
            followUp = null
            shake(ENRAGE_DUR)
            requestToPlaySound(SoundAsset.QUAKE_SOUND, false)
            goTo(EnrageStep.SHAKE)
        }
        shakeOffset = if (flickerOn(time)) SHAKE_STEP else 0f
        if (time >= ENRAGE_DUR) {
            shakeOffset = 0f
            tuning = D1000Tuning.of(D1000Tier.OVERDRIVE, tuning.hard)
            setState(D1000State.THINK)
        }
    }

    internal fun tellFollowUp() {
        val next = followUp ?: return
        if (followUpTold) return
        if (!allowsNow(next, D1000Lamp.LEAD_DUR)) {
            GameLogger.debug(TAG, "tellFollowUp(): drop $next, it leaves no escape")
            followUp = null
            return
        }
        followUpTold = true
        lamp.tell(next, true)
    }

    private fun updateStun(delta: Float) {
        when (step) {
            StunStep.STAGGER -> if (animFinished() || origin.y - STAGGER_ART_BELOW_ORIGIN < floor) {
                setAnim("fall")
                speedY = 0f
                goTo(StunStep.FALL)
            }

            StunStep.FALL -> if (fall(delta, FALL_GRAVITY, floor + CROUCH_HEIGHT)) {
                setAnim("land")
                land(STUN_SHAKE)
                goTo(StunStep.DAMAGE)
            }

            StunStep.DAMAGE -> if (time >= STUN_DAMAGE_DELAY) {
                translateHealth(-STUN_DAMAGE)
                requestToPlaySound(SoundAsset.ENEMY_DAMAGE_SOUND, false)
                goTo(StunStep.SIGNAL)
            }

            StunStep.SIGNAL -> if (coreSignaled) {
                setAnim("stand_up")
                goTo(StunStep.STAND_UP)
            }

            StunStep.STAND_UP -> if (time >= STAND_UP_HOLD_DUR) {
                setAnim("walk")
                goTo(StunStep.RISE)
            }

            StunStep.RISE -> if (moveToHover(delta)) setState(D1000State.THINK)
        }
    }

    fun eyeMayShoot(from: Vector2, delay: Float, speed: Float): Boolean {
        if (move?.holdsEyeShots == true) return false
        if (state == D1000State.THINK && plan?.action.let { it == D1000Action.RAM || D1000Director.guided(it) }) return false
        beginGuard()
        forecastCurrentAction()
        guard.addFootprint(bodyBoxes(), 0f, 0f, D1000Escape.HORIZON, true)
        if (!guard.escapes()) return false
        val first = guard.escape.count
        addAimedShot(from.x, from.y, speed, delay)
        var landsBy = 0f
        for (i in first until guard.escape.count) landsBy = max(landsBy, guard.escape.danger(i).to)
        if (guard.stunsBetween(0f, landsBy)) return false
        return guard.escapes()
    }

    private fun choosePlan(startIn: Float): D1000Plan? {
        val base = context(UtilMethods.getRandom(0, D1000Director.ROLLS - 1))
        beginGuard()
        val liveOk = liveEscape()
        guard.refused.clear()
        D1000Action.entries.forEach { action ->
            val considered = D1000Director.weight(action, base) > 0 ||
                (action == D1000Action.CROUCH && base.distance == D1000Distance.NEAR)
            if (considered && (!liveOk || !allows(action, startIn))) guard.refused.add(action)
        }
        if (guard.refused.isNotEmpty()) GameLogger.debug(TAG, "choosePlan(): refused=${guard.refused}")
        return D1000Director.choose(base.copy(refused = guard.refused.toSet()))
    }

    internal fun beginGuard() {
        guard.begin(arena, targetPlayer.body, tuning.xMoves)
        if (xStunLeft > 0f) guard.escape.stun(0f, xStunLeft)
    }

    internal fun liveEscape(): Boolean {
        val mark = guard.escape.count
        guard.addFootprint(bodyBoxes(), 0f, 0f, D1000Escape.HORIZON, true)
        val ok = guard.escapes()
        guard.escape.rollBack(mark)
        return ok
    }

    private fun allowsNow(action: D1000Action, startIn: Float): Boolean {
        beginGuard()
        return liveEscape() && allows(action, startIn)
    }

    private fun allows(action: D1000Action, startIn: Float): Boolean {
        if (ORB_ACTIONS.contains(action) && cornered()) return false
        val mark = guard.escape.count
        val ok = allowed(action, startIn)
        val stunUnderShot = guard.stunsBetween(0f, guard.shotsAirUntil, mark)
        guard.escape.rollBack(mark)
        return ok && !stunUnderShot
    }

    internal fun bodyBoxes(): Array<GameRectangle> {
        liveBoxes.clear()
        var used = 0
        damagerFixtures.fixtures.forEach { fixture ->
            if (!fixture.isActive()) return@forEach
            if (used == boxPool.size) boxPool.add(GameRectangle())
            val box = boxPool[used++]
            box.set(fixture.getShape() as GameRectangle)
            liveBoxes.add(box)
        }
        return liveBoxes
    }

    private fun walkGap(boxes: Array<GameRectangle>): Float {
        if (boxes.isEmpty) return Float.POSITIVE_INFINITY
        val player = targetPlayer.body
        return if (player.getCenter().x >= origin.x) player.getX() - boxes.maxOf { it.getMaxX() }
        else boxes.minOf { it.getX() } - player.getMaxX()
    }

    internal fun approachBlocked(target: Float): Boolean {
        val boxes = bodyBoxes()
        if (boxes.isEmpty) return false
        val player = targetPlayer.body
        if (player.getMaxX() <= boxes.minOf { it.getX() } || player.getX() >= boxes.maxOf { it.getMaxX() }) return false
        val above = player.getCenter().y > origin.y + BODY_CENTER_ABOVE_ORIGIN
        return (above && target > origin.y) || (!above && target < origin.y)
    }

    private fun debrisHangY() = arena.getMaxY() - DEBRIS_TOP - D1000DebrisWarning.HANG_DROP

    internal fun addDebrisAt(x: Float, from: Float, spread: Float = 0f) = guard.addDebris(x, debrisHangY(), spread, from)

    private fun addAimedShot(fromX: Float, fromY: Float, speed: Float, delay: Float) {
        val player = targetPlayer.body.getCenter()
        val dx = player.x - fromX
        val dy = player.y - fromY
        val length = max(sqrt(dx * dx + dy * dy), 0.001f)
        guard.addBallistic(fromX, fromY, dx / length * speed, dy / length * speed, 0f, D1000TrapGuard.SHOT_RADIUS, delay)
    }

    private fun addFan(fromX: Float, fromY: Float, angles: FloatArray, delay: Float) {
        val player = targetPlayer.body.getCenter()
        val aim = Vector2(player.x - fromX, player.y - fromY).angleDeg()
        angles.forEach { angle ->
            val velocity = Vector2(D1000Core.SHOT_SPEED, 0f).setAngleDeg(aim + angle)
            guard.addBallistic(fromX, fromY, velocity.x, velocity.y, 0f, D1000TrapGuard.SHOT_RADIUS, delay)
        }
    }

    internal fun crouchBoxes(x: Float, facingRight: Boolean): Array<GameRectangle> {
        val def = D1000Frames.BODY["crouch_shake"]!!
        val boxes = D1000Frames.BODY_BOXES["crouch_shake"]!![def.order[0]]
        forecastBoxes.clear()
        for (i in 0 until boxes.size / 4) {
            if (i == boxPool.size - liveBoxes.size) boxPool.add(GameRectangle())
            val box = boxPool[liveBoxes.size + i]
            TickFrames.placeBox(box, def, boxes, i * 4, x, floor + CROUCH_HEIGHT, facingRight)
            forecastBoxes.add(box)
        }
        return forecastBoxes
    }

    fun forecastCurrentAction() {
        if (state == D1000State.THINK) {
            plan?.let { if (it.action != D1000Action.RAM) forecast(it.action, max(tuning.thinkDur - time, 0f)) }
            return
        }
        move?.forecastNow()
    }

    private fun allowed(action: D1000Action, startIn: Float): Boolean = when (action) {
        D1000Action.RAM -> ram.allowed(startIn)
        D1000Action.CROUCH -> crouch.allowed(startIn)
        D1000Action.CEILING -> ceiling.allowed(startIn)
        D1000Action.BOMB, D1000Action.BRACKET, D1000Action.SWEEP, D1000Action.FENCE, D1000Action.WALL ->
            volley.allowed(D1000BombPattern.of(action), startIn)

        else -> {
            forecast(action, startIn)
            guard.escapes()
        }
    }

    private fun forecast(action: D1000Action, startIn: Float) {
        val boxes = bodyBoxes()
        when (action) {
            D1000Action.WALK -> forecastWalk(boxes, startIn)
            D1000Action.CROUCH -> crouch.forecast(startIn)
            D1000Action.CEILING -> ceiling.forecast(startIn)
            D1000Action.BOMB, D1000Action.BRACKET, D1000Action.SWEEP, D1000Action.FENCE, D1000Action.WALL ->
                volley.forecast(D1000BombPattern.of(action), startIn)

            D1000Action.SUMMON -> {
                guard.addFootprint(boxes, 0f, 0f, D1000Escape.HORIZON, true)
                core?.let { eye ->
                    val center = eye.body.getCenter()
                    val tell = tuning.burstTellDur
                    val x = center.x
                    val y = center.y
                    D1000Core.SHOT_TIMES.forEach { addAimedShot(x, y, D1000Core.SHOT_SPEED, startIn + tell + it) }
                }
            }

            D1000Action.SPREAD -> {
                guard.addFootprint(boxes, 0f, 0f, D1000Escape.HORIZON, true)
                core?.let { eye ->
                    val center = eye.body.getCenter()
                    val x = center.x
                    val y = center.y
                    addFan(x, y, D1000Core.SPREAD_ANGLES, startIn + D1000Core.SPREAD_TELL_DUR)
                    if (tuning.spreadSecondFan) addFan(
                        x, y, D1000Core.SPREAD_ANGLES_SECOND,
                        startIn + D1000Core.SPREAD_TELL_DUR + D1000Core.SPREAD_SECOND_DELAY
                    )
                }
            }

            D1000Action.DIVE -> guard.addFootprint(boxes, 0f, 0f, D1000Escape.HORIZON, true)

            D1000Action.FLANK -> {
                val player = targetPlayer.body.getCenter()
                val side = D1000Core.flankSide(arena, player.x)
                if (side == 0) forecast(D1000Action.SPREAD, startIn) else {
                    guard.addFootprint(boxes, 0f, 0f, D1000Escape.HORIZON, true)
                    core?.let { eye ->
                        val point = D1000Core.flankPoint(arena, player, side, Vector2())
                        val arrive = startIn + eye.body.getCenter().dst(point) / D1000Core.FLANK_SPEED
                        for (i in 0 until tuning.flankShots) addAimedShot(
                            point.x, point.y, D1000Core.SHOT_SPEED, arrive + tuning.flankPause + i * D1000Core.FLANK_SHOT_GAP
                        )
                    }
                }
            }

            D1000Action.RAM -> ram.forecast(startIn)
        }
    }

    private fun forecastWalk(boxes: Array<GameRectangle>, startIn: Float) {
        guard.addFootprint(boxes, 0f, 0f, startIn, true)
        val toward = if (playerX() >= origin.x) 1f else -1f
        val room = if (toward > 0f) maxX - origin.x else origin.x - minX
        val distance = max(0f, minOf(tuning.walkSpeed * WALK_DUR, walkGap(boxes) - WALK_GAP, room))
        val duration = distance / tuning.walkSpeed
        for (i in 1..WALK_SLICES) {
            val from = startIn + duration * (i - 1) / WALK_SLICES
            guard.addFootprint(boxes, toward * distance * i / WALK_SLICES, from, from + duration / WALK_SLICES, true)
        }
        guard.addFootprint(boxes, toward * distance, startIn + duration, D1000Escape.HORIZON, true)
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (defeated) return@add

            if (state != D1000State.ENTRY && bossComponent.phase != BossPhase.FIGHT) return@add

            time += delta
            trackAir(delta)
            if (xStunLeft > 0f) xStunLeft -= delta
            lamp.update(delta)
            slamWaves.update(delta)
            when (state) {
                D1000State.ENTRY -> updateEntry(delta)
                D1000State.THINK -> updateThink(delta)
                D1000State.WALK -> updateWalk(delta)
                D1000State.SUMMON -> updateSummon()
                D1000State.STUN -> updateStun(delta)
                D1000State.ENRAGE -> updateEnrage()
                D1000State.RAM -> ram.update(delta)
                D1000State.CROUCH -> crouch.update(delta)
                D1000State.CEILING -> ceiling.update(delta)
                D1000State.BOMB -> volley.update(delta)
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.setSize(BODY_WIDTH, BODY_HEIGHT)

        val count = TickFrames.maxBoxes(D1000Frames.BODY_BOXES)
        bodyFixtures = TickFrameFixtures(body, FixtureType.BODY, count)
        damagerFixtures = TickFrameFixtures(body, FixtureType.DAMAGER, count)
        damageableFixtures = TickFrameFixtures(body, FixtureType.DAMAGEABLE, count)

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.physics.velocity.setZero()
            placeBody()
            placeFixtures()
        }

        val debugShapes = gdxArrayOf<() -> IDrawableShape?>({ body.getBounds() })
        bodyFixtures.addDebugShapes(debugShapes)
        damagerFixtures.addDebugShapes(debugShapes)
        damageableFixtures.addDebugShapes(debugShapes)
        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body, doUpdate = { true })
    }

    private fun drawnAnim(): String {
        val key = animator.currentKey
        return if (key == null) anim else TickFrames.baseKey(key)
    }

    val hovering: Boolean
        get() = HOVER_ANIMS.contains(drawnAnim())

    private fun drawnFrame(base: String): Int {
        val key = animator.currentKey
        val def = D1000Frames.BODY[base]
        return if (key == null) def.order[0] else TickFrames.artFrame(def, key, animator.currentAnimation!!.getIndex())
    }

    private fun placeFixtures() {
        val base = drawnAnim()
        val def = D1000Frames.BODY[base]
        val frame = drawnFrame(base)
        val boxes = D1000Frames.BODY_BOXES[base][frame]
        val right = isFacing(Facing.RIGHT)
        val y = origin.y + shakeOffset
        bodyFixtures.place(def, boxes, origin.x, y, right, !defeated)
        damageableFixtures.place(def, boxes, origin.x, y, right, !defeated)
        damagerFixtures.place(
            def, boxes, origin.x, y, right, !stunned && !defeated && bossComponent.phase != BossPhase.ENTRANCE
        )
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 1))
        val component = SpritesComponent(sprite)
        component.putPreProcess { _, _ ->
            val def = D1000Frames.BODY[anim]
            TickFrames.place(sprite, def, origin.x, origin.y + shakeOffset, isFacing(Facing.RIGHT))
            sprite.hidden = (damageBlink && !defeated) || broken
        }
        val lampSprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 2))
        component.putSprite(LAMP, lampSprite)
        component.putPreProcess(LAMP) { _, _ ->
            val base = drawnAnim()
            lamp.place(lampSprite, base, drawnFrame(base), origin.x, origin.y + shakeOffset, isFacing(Facing.RIGHT))
            lampSprite.hidden = (damageBlink && !defeated) || broken
        }
        val faceSprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 2))
        component.putSprite(FACE, faceSprite)
        component.putPreProcess(FACE) { _, _ ->
            val base = drawnAnim()
            val y = origin.y + shakeOffset
            lamp.placeFace(faceSprite, base, drawnFrame(base), origin.x, y, isFacing(Facing.RIGHT))
            faceSprite.hidden = (damageBlink && !defeated) || broken
        }
        val glowSprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 3))
        component.putSprite(GLOW, glowSprite)
        component.putPreProcess(GLOW) { _, _ ->
            val base = drawnAnim()
            val y = origin.y + shakeOffset
            lamp.placeGlow(glowSprite, base, drawnFrame(base), origin.x, y, isFacing(Facing.RIGHT))
            glowSprite.hidden = damageBlink || defeated
        }
        val mouthSprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 3))
        component.putSprite(MOUTH, mouthSprite)
        component.putPreProcess(MOUTH) { _, _ ->
            val base = drawnAnim()
            val y = origin.y + shakeOffset
            val shown = lamp.placeMouth(mouthSprite, base, drawnFrame(base), origin.x, y, isFacing(Facing.RIGHT))
            mouthSprite.hidden = !shown || damageBlink || defeated
        }
        val screwSprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 3))
        component.putSprite(SCREW, screwSprite)
        component.putPreProcess(SCREW) { _, _ ->
            val base = drawnAnim()
            val y = origin.y + shakeOffset
            val shown = lamp.placeScrew(screwSprite, base, drawnFrame(base), origin.x, y, isFacing(Facing.RIGHT))
            screwSprite.hidden = !shown || damageBlink || defeated
        }
        val chargeGlowSprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 3))
        component.putSprite(CHARGE_GLOW, chargeGlowSprite)
        component.putPreProcess(CHARGE_GLOW) { _, _ ->
            val progress = chargeProgress()
            if (progress != null) lamp.placeChargeGlow(
                chargeGlowSprite, progress, volley.clock, chargeX(), chargeY(), isFacing(Facing.RIGHT)
            )
            chargeGlowSprite.hidden = progress == null || damageBlink
        }
        val chargeSprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 4))
        component.putSprite(CHARGE, chargeSprite)
        component.putPreProcess(CHARGE) { _, _ ->
            val progress = chargeProgress()
            if (progress != null) lamp.placeCharge(chargeSprite, progress, volley.clock, chargeX(), chargeY(), isFacing(Facing.RIGHT))
            chargeSprite.hidden = progress == null || damageBlink
        }
        val puffSprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 4))
        component.putSprite(PUFF, puffSprite)
        component.putPreProcess(PUFF) { _, _ ->
            val base = drawnAnim()
            lamp.placePuff(puffSprite, base, drawnFrame(base), origin.x, origin.y + shakeOffset, isFacing(Facing.RIGHT))
            puffSprite.hidden = !hovering || defeated
        }
        return component
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = { current ->
            if (!animLooping && TickFrames.hasLoop(D1000Frames.BODY, anim) && current == anim &&
                animations[anim].isFinished()
            ) animLooping = true
            if (animLooping) anim + TickFrames.LOOP_SUFFIX else anim
        }
        animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
