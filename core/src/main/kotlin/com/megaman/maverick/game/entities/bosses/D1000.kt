package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.math.Vector2
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
import com.maverick.game.engine.common.interfaces.IFaceable
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.entities.contracts.IAnimatedEntity
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.megaman.maverick.game.entities.utils.X6Frames
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.difficulty.DifficultyMode
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.targetPlayer
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.D1000Bomb
import com.megaman.maverick.game.entities.state.BossPhase
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.BodyFixtureDef
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class D1000(services: MegaServices) :
    AbstractBoss(services, dmgDuration = DAMAGE_DURATION, size = Size.LARGE), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "D1000"

        const val HALF_HEALTH = ConstVals.MAX_HEALTH / 2
        const val FULL_CHARGE_DAMAGE = 5
        const val STUN_DAMAGE = 8

        private const val DAMAGE_DURATION = 0.5f

        private const val BODY_WIDTH = 3.5f * ConstVals.PPM
        private const val BODY_HEIGHT = 2.875f * ConstVals.PPM
        private const val BODY_CENTER_ABOVE_ORIGIN = 0.5625f * ConstVals.PPM
        private const val HALF_WIDTH = 2f * ConstVals.PPM

        private const val STAND_HEIGHT = 3.5f * ConstVals.PPM
        private const val CROUCH_HEIGHT = 2f * ConstVals.PPM
        private const val ENTRY_HEIGHT = 8f * ConstVals.PPM
        private const val ENTRY_X = 6.5f * ConstVals.PPM

        private const val CORE_NEAR_X = 2.5f * ConstVals.PPM
        private const val CORE_FAR_X = 9f * ConstVals.PPM
        private const val CORE_HEIGHT = 5f * ConstVals.PPM
        private const val ARENA_MIDDLE_X = 5.75f * ConstVals.PPM

        private const val FALL_GRAVITY = 42f * ConstVals.PPM
        private const val CROUCH_GRAVITY = 56f * ConstVals.PPM
        private const val RISE_SPEED = 1.875f * ConstVals.PPM
        private const val WALK_SPEED = 1.875f * ConstVals.PPM
        private const val WIND_UP_SPEED = 3.75f * ConstVals.PPM
        private const val RAM_ACCELERATION = 112.5f * ConstVals.PPM
        private const val RAM_MAX_SPEED = 7.5f * ConstVals.PPM
        private const val SHAKE_STEP = 0.0625f * ConstVals.PPM
        private const val SHAKE_FLIP = 0.05f

        private const val THINK_DUR = 1f
        private const val WALK_DUR = 2f
        private const val NEAR_PLAYER = 1.875f * ConstVals.PPM
        private const val CROUCH_SHAKE_DUR = 1f
        private const val CROUCH_HOLD_DUR = 1.5f
        private const val RAM_WIND_UP_DUR = 3f
        private const val RAM_STUCK_DUR = 3f
        private const val ENTRY_CORE_DELAY = 2f
        private const val STAND_UP_HOLD_DUR = 0.75f
        private const val STUN_DAMAGE_DELAY = 1.1f
        private const val BOMB_END_DUR = 2.7f
        private const val BOMB_END_DUR_HARD = 3.7f
        private val BOMB_TIMES = floatArrayOf(0.4f)
        private val BOMB_TIMES_HARD = floatArrayOf(0.4f, 1.4f)

        private const val BOMB_FORWARD = 1.75f * ConstVals.PPM
        private const val BOMB_UP = 0.75f * ConstVals.PPM

        private const val SHAKE_INTERVAL = 0.1f
        private const val SHAKE_Y = 0.005f
        private const val LAND_SHAKE = 0.33f
        private const val STUN_SHAKE = 1f
        private const val WALL_SHAKE = 0.5f

        fun chooseAction(r: Int, health: Int, afterCrouch: Boolean): D1000Action = when {
            health > HALF_HEALTH && afterCrouch -> if (r < 8) D1000Action.WALK else D1000Action.SUMMON
            health > HALF_HEALTH -> if (r < 8) D1000Action.RAM else D1000Action.SUMMON
            afterCrouch -> if (r < 6) D1000Action.SUMMON else D1000Action.BOMB
            else -> if (r < 6) D1000Action.RAM else D1000Action.BOMB
        }
    }

    enum class D1000Action { WALK, SUMMON, RAM, BOMB }

    private enum class D1000State { ENTRY, THINK, WALK, CROUCH, BOMB, SUMMON, RAM, STUN }

    override val damageNegotiator = object : BossDamageNegotiator() {
        override fun get(damager: IDamagerEntity) = when (damager) {
            is Bullet, is ChargedShotExplosion -> 0
            is ChargedShot -> if (damager.fullyCharged) FULL_CHARGE_DAMAGE else 0
            else -> super.get(damager)
        }
    }

    override lateinit var facing: Facing

    private val arena = GameRectangle()
    private val origin = Vector2()
    private var speedY = 0f
    private var speedX = 0f
    private var shakeOffset = 0f

    private var state = D1000State.ENTRY
    private var step = 0
    private var time = 0f

    private var afterCrouch = false
    private var touchedPlayer = false
    private var bombsThrown = 0
    private var entryDone = false

    private var core: D1000Core? = null
    private var coreSignaled = false
    private var coreFar = true

    private val animations = ObjectMap<String, IAnimation>()
    private var anim = "stand"
    private var animLooping = false
    private lateinit var animator: Animator

    private val stunned: Boolean
        get() = state == D1000State.STUN

    private val floor: Float
        get() = arena.getY()
    private val minX: Float
        get() = arena.getX() + HALF_WIDTH
    private val maxX: Float
        get() = arena.getMaxX() - HALF_WIDTH

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        val atlas = services.assets.getTextureAtlas(TextureAsset.BOSSES_3.source)
        X6Frames.animations(atlas, TAG, D1000Frames.BODY, animations)
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        spawnProps.put(ConstKeys.ORB, true)
        super.onSpawn(spawnProps)

        arena.set(spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!)
        origin.set(arena.getX() + ENTRY_X, floor + ENTRY_HEIGHT)
        speedX = 0f
        speedY = 0f
        shakeOffset = 0f
        facing = Facing.LEFT

        afterCrouch = false
        touchedPlayer = false
        bombsThrown = 0
        entryDone = false

        core = null
        coreSignaled = false
        coreFar = true

        animLooping = false
        anim = "fall"
        setState(D1000State.ENTRY)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        core?.let { if (it.existenceState.liveAfterUpdate) it.destroy() }
        core = null
    }

    override fun isReady(delta: Float) = entryDone

    override fun canBeDamagedBy(damager: IDamagerEntity) = !stunned && super.canBeDamagedBy(damager)

    override fun onPlayerDamaged(damager: IDamagerEntity, player: MegaGameEntity) {
        if (damager == this) touchedPlayer = true
    }

    override fun triggerDefeat() {
        super.triggerDefeat()
        core?.let { if (it.existenceState.liveAfterUpdate) it.destroy() }
        core = null
    }

    fun onCoreSignal() {
        GameLogger.debug(TAG, "onCoreSignal()")
        coreSignaled = true
    }

    fun stun() {
        if (defeated || state == D1000State.ENTRY || stunned) return
        GameLogger.debug(TAG, "stun()")
        coreSignaled = false
        speedX = 0f
        speedY = 0f
        shakeOffset = 0f
        setAnim("stagger")
        setState(D1000State.STUN)
        requestToPlaySound(SoundAsset.ENEMY_DAMAGE_SOUND, false)
    }

    private fun setState(next: D1000State) {
        GameLogger.debug(TAG, "setState(): $state -> $next")
        state = next
        step = 0
        time = 0f
    }

    private fun nextStep() {
        step++
        time = 0f
    }

    private fun setAnim(key: String) {
        if (anim != key) {
            anim = key
            animLooping = false
        }
    }

    private fun animFinished() =
        X6Frames.baseKey(animator.currentKey ?: "") == anim && animator.currentAnimation?.isFinished() == true

    private fun playerX() = targetPlayer.body.getCenter().x

    private fun facePlayer() {
        facing = if (playerX() < origin.x) Facing.LEFT else Facing.RIGHT
    }

    private fun shake(duration: Float) {
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

    private fun land(duration: Float) {
        requestToPlaySound(SoundAsset.THUMP_SOUND, false)
        requestToPlaySound(SoundAsset.QUAKE_SOUND, false)
        shake(duration)
    }

    private fun fall(delta: Float, gravity: Float, target: Float): Boolean {
        speedY += gravity * delta
        origin.y -= speedY * delta
        if (origin.y > target) return false
        origin.y = target
        speedY = 0f
        return true
    }

    private fun rise(delta: Float, target: Float): Boolean {
        origin.y += RISE_SPEED * delta
        if (origin.y < target) return false
        origin.y = target
        return true
    }

    private fun moveX(delta: Float, speed: Float): Boolean {
        origin.x += speed * facing.value * delta
        val clamped = origin.x.coerceIn(minX, maxX)
        val hit = clamped != origin.x
        origin.x = clamped
        return hit
    }

    private fun spawnCore() {
        val x = arena.getX() + if (coreFar) CORE_FAR_X else CORE_NEAR_X
        val spawned = MegaEntityFactory.fetch(D1000Core::class)!!
        spawned.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.POSITION pairTo Vector2(x, floor + CORE_HEIGHT),
                ConstKeys.OTHER pairTo Vector2(arena.getX() + if (coreFar) CORE_NEAR_X else CORE_FAR_X, floor + CORE_HEIGHT)
            )
        )
        core = spawned
    }

    private fun throwBomb() {
        val spawn = Vector2(origin.x + BOMB_FORWARD * facing.value, origin.y + BOMB_UP)
        val bomb = MegaEntityFactory.fetch(D1000Bomb::class)!!
        bomb.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.TARGET pairTo playerX(),
                ConstKeys.BOUNDS pairTo GameRectangle(arena)
            )
        )
        requestToPlaySound(SoundAsset.BLAST_1_SOUND, false)
    }

    private fun updateEntry(delta: Float) {
        when (step) {
            0 -> if (fall(delta, FALL_GRAVITY, floor + CROUCH_HEIGHT)) {
                setAnim("land")
                land(LAND_SHAKE)
                nextStep()
            }

            1 -> if (time >= ENTRY_CORE_DELAY) {
                coreFar = playerX() < arena.getX() + ARENA_MIDDLE_X
                spawnCore()
                coreSignaled = false
                nextStep()
            }

            2 -> if (coreSignaled) {
                setAnim("stand_up")
                nextStep()
            }

            3 -> if (time >= STAND_UP_HOLD_DUR) {
                setAnim("walk")
                nextStep()
            }

            4 -> if (rise(delta, floor + STAND_HEIGHT)) {
                setAnim("stand")
                entryDone = true
                setState(D1000State.THINK)
            }
        }
    }

    private fun updateThink() {
        if (step == 0) {
            core?.vulnerable = true
            setAnim("stand")
            nextStep()
        }
        facePlayer()
        if (time < THINK_DUR) return

        val action = chooseAction(UtilMethods.getRandom(0, 15), getCurrentHealth(), afterCrouch)
        GameLogger.debug(TAG, "updateThink(): action=$action")
        afterCrouch = false
        setState(
            when (action) {
                D1000Action.WALK -> D1000State.WALK
                D1000Action.SUMMON -> D1000State.SUMMON
                D1000Action.RAM -> D1000State.RAM
                D1000Action.BOMB -> D1000State.BOMB
            }
        )
    }

    private fun updateWalk(delta: Float) {
        if (step == 0) {
            facePlayer()
            setAnim("walk")
            nextStep()
        }
        if (abs(playerX() - origin.x) <= NEAR_PLAYER) {
            setState(D1000State.CROUCH)
            return
        }
        moveX(delta, WALK_SPEED)
        if (time >= WALK_DUR) setState(D1000State.THINK)
    }

    private fun updateCrouch(delta: Float) {
        when (step) {
            0 -> {
                touchedPlayer = false
                speedY = 0f
                setAnim("crouch_drop")
                nextStep()
            }

            1 -> if (fall(delta, CROUCH_GRAVITY, floor + CROUCH_HEIGHT)) {
                setAnim("crouch_shake")
                land(LAND_SHAKE)
                nextStep()
            }

            2 -> {
                shakeOffset = if (time < CROUCH_SHAKE_DUR && (time / SHAKE_FLIP).toInt() % 2 == 1) SHAKE_STEP else 0f
                if (time >= CROUCH_HOLD_DUR || touchedPlayer) {
                    shakeOffset = 0f
                    setAnim("walk")
                    nextStep()
                }
            }

            3 -> if (rise(delta, floor + STAND_HEIGHT)) {
                setState(D1000State.THINK)
                afterCrouch = true
            }
        }
    }

    private fun updateBomb() {
        val hard = services.gameState.getDifficultyMode() == DifficultyMode.HARD
        if (step == 0) {
            setAnim("throw")
            bombsThrown = 0
            nextStep()
        }
        facePlayer()
        val bombTimes = if (hard) BOMB_TIMES_HARD else BOMB_TIMES
        if (bombsThrown < bombTimes.size && time >= bombTimes[bombsThrown]) {
            throwBomb()
            bombsThrown++
        }
        if (time >= if (hard) BOMB_END_DUR_HARD else BOMB_END_DUR) setState(D1000State.WALK)
    }

    private fun updateSummon() {
        if (step == 0) {
            setAnim("stand")
            coreSignaled = false
            core?.summon()
            nextStep()
        }
        facePlayer()
        if (coreSignaled || core == null) setState(D1000State.WALK)
    }

    private fun updateRam(delta: Float) {
        when (step) {
            0 -> {
                setAnim("walk")
                nextStep()
            }

            1 -> {
                facePlayer()
                val target = (targetPlayer.body.getCenter().y - BODY_CENTER_ABOVE_ORIGIN)
                    .coerceIn(floor + CROUCH_HEIGHT, floor + STAND_HEIGHT)
                val step = WIND_UP_SPEED * delta
                origin.y = if (origin.y < target) min(origin.y + step, target) else max(origin.y - step, target)
                if (time >= RAM_WIND_UP_DUR) {
                    setAnim("ram")
                    speedX = 0f
                    requestToPlaySound(SoundAsset.BLAST_2_SOUND, false)
                    nextStep()
                }
            }

            2 -> {
                speedX = min(
                    speedX + RAM_ACCELERATION * delta,
                    RAM_MAX_SPEED
                )
                if (moveX(delta, speedX)) {
                    speedX = 0f
                    land(WALL_SHAKE)
                    setAnim("stand")
                    nextStep()
                }
            }

            3 -> if (time >= RAM_STUCK_DUR) {
                setAnim("walk")
                nextStep()
            }

            4 -> if (rise(delta, floor + STAND_HEIGHT)) setState(D1000State.WALK)
        }
    }

    private fun updateStun(delta: Float) {
        when (step) {
            0 -> if (animFinished()) {
                setAnim("fall")
                speedY = 0f
                nextStep()
            }

            1 -> if (fall(delta, FALL_GRAVITY, floor + CROUCH_HEIGHT)) {
                setAnim("land")
                land(STUN_SHAKE)
                nextStep()
            }

            2 -> if (time >= STUN_DAMAGE_DELAY) {
                translateHealth(-STUN_DAMAGE)
                requestToPlaySound(SoundAsset.ENEMY_DAMAGE_SOUND, false)
                nextStep()
            }

            3 -> if (coreSignaled) {
                setAnim("stand_up")
                nextStep()
            }

            4 -> if (time >= STAND_UP_HOLD_DUR) {
                setAnim("walk")
                nextStep()
            }

            5 -> if (rise(delta, floor + STAND_HEIGHT)) setState(D1000State.THINK)
        }
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (defeated) {
                explodeOnDefeat(delta)
                return@add
            }

            if (state != D1000State.ENTRY && bossComponent.phase != BossPhase.FIGHT) return@add

            time += delta
            when (state) {
                D1000State.ENTRY -> updateEntry(delta)
                D1000State.THINK -> updateThink()
                D1000State.WALK -> updateWalk(delta)
                D1000State.CROUCH -> updateCrouch(delta)
                D1000State.BOMB -> updateBomb()
                D1000State.SUMMON -> updateSummon()
                D1000State.RAM -> updateRam(delta)
                D1000State.STUN -> updateStun(delta)
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.setSize(BODY_WIDTH, BODY_HEIGHT)

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.physics.velocity.setZero()
            body.setCenter(origin.x, origin.y + BODY_CENTER_ABOVE_ORIGIN)
            body.forEachFixture { fixture ->
                val active = when (fixture.getType()) {
                    FixtureType.DAMAGER -> !stunned && !defeated && bossComponent.phase != BossPhase.ENTRANCE
                    else -> !defeated
                }
                fixture.setActive(active)
            }
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body.getBounds() }), debug = true))

        return BodyComponentCreator.create(
            this, body, BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 1))
        val component = SpritesComponent(sprite)
        component.putPreProcess { _, _ ->
            val def = D1000Frames.BODY[anim]
            X6Frames.place(sprite, def, origin.x, origin.y + shakeOffset, isFacing(Facing.RIGHT))
            sprite.hidden = damageBlink && !defeated
        }
        return component
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = { current ->
            if (!animLooping && X6Frames.hasLoop(D1000Frames.BODY, anim) && current == anim &&
                animations[anim].isFinished()
            ) animLooping = true
            if (animLooping) anim + X6Frames.LOOP_SUFFIX else anim
        }
        animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    override fun getTag() = TAG
}
