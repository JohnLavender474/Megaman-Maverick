package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.animations.IAnimation
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.enums.Position
import com.maverick.game.engine.common.enums.Size
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.extensions.objectMapOf
import com.maverick.game.engine.common.interfaces.IDirectional
import com.maverick.game.engine.common.interfaces.IFaceable
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.shapes.IDrawableShape
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.drawables.sprites.setPosition
import com.maverick.game.engine.drawables.sprites.setSize
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.state.EnumStateMachineBuilder
import com.maverick.game.engine.state.StateMachine
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
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.difficulty.DifficultyMode
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.bosses.GutsTankFist
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.IFreezableEntity
import com.megaman.maverick.game.entities.contracts.IProjectileEntity
import com.megaman.maverick.game.entities.contracts.targetPlayer
import com.megaman.maverick.game.entities.hazards.MetHelmet
import com.megaman.maverick.game.entities.projectiles.BigAssMaverickRobotOrb
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.PurpleBlast
import com.megaman.maverick.game.entities.utils.FreezableEntityHandler
import com.megaman.maverick.game.entities.utils.GravityFrame
import com.megaman.maverick.game.entities.utils.GroundAhead
import com.megaman.maverick.game.entities.utils.GroundProbe
import com.megaman.maverick.game.entities.utils.HalfDomeShield
import com.megaman.maverick.game.entities.utils.JumpPlan
import com.megaman.maverick.game.entities.utils.JumpPlanner
import com.megaman.maverick.game.players.asPlayer
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*
import kotlin.math.abs

class Met(services: MegaServices) : AbstractEnemy(services, size = Size.SMALL), IFreezableEntity, IFaceable, IDirectional {

    enum class MetState {
        SHIELDING, POP_UP, RUNNING, JUMPING, STUNNED, HELMET_LAND, FETCH_HELMET, PICKUP_HELMET, CRY, DESPERATE_RUN,
        DESPERATE_PAUSE
    }

    companion object {
        const val TAG = "Met"

        const val RUN_ONLY = "run_only"
        const val RUNNING_ALLOWED = "running_allowed"

        private var atlas: TextureAtlas? = null

        private const val SHIELDING_DURATION = 1.15f
        private const val SHIELDING_DURATION_HARD = 0.8f
        private const val RUNNING_DURATION = 0.5f
        private const val RUNNING_DURATION_HARD = 1f
        private const val POP_UP_DURATION = 0.5f
        private const val MAX_JUMP_DURATION = 2f

        private const val HELMET_APEX_HALF = 1.5f
        private const val HELMET_APEX_FULL = 3f
        private const val STUN_MAX_MARGIN = 0.5f
        private val STUN_MIN = MetHelmet.flightTime(HELMET_APEX_HALF)
        private val STUN_MAX = MetHelmet.flightTime(HELMET_APEX_FULL) + STUN_MAX_MARGIN
        private const val HELMET_LAND_FRAME_DURATION = 0.08f
        private const val HELMET_LAND_FRAMES = 3
        private const val FETCH_TIMEOUT = 4f
        private const val PICKUP_FRAME_DURATION = 0.1f
        private const val PICKUP_FRAMES = 3
        private const val CRY_DURATION = 1f

        private const val DESPERATE_RUN_DURATION = 2f
        private const val DESPERATE_PAUSE_DURATION = 0.3f
        private const val DESPERATE_SPEED_SCALAR = 1.25f

        private const val JUMP_CHECK_DELAY = 0.25f
        private const val JUMP_CHECK_DELAY_DESPERATE = 0.1f
        private const val MAX_SAFE_DROP = 4f
        private const val PLAYER_ABOVE_MIN = 1.5f
        private const val PLAYER_ABOVE_MAX_DX = 6f
        private const val GAP_MIN_PROGRESS = 1.5f
        private const val STEP_MIN_RISE = 0.5f
        private const val PLATFORM_MIN_RISE = 1f

        private const val DODGE_RANGE = 4f
        private const val DODGE_CHANCE_DENOMINATOR = 3

        private const val RUN_SPEED = 8f

        private const val GRAVITY_IN_AIR = 37.5f
        private const val GRAVITY_ON_GROUND = 1.5f

        private const val BULLET_TRAJECTORY_X = 15f
        private const val BULLET_TRAJECTORY_Y = 0.25f

        private const val VELOCITY_CLAMP_X = 8f
        private const val VELOCITY_CLAMP_Y = 18f

        private val GAP_JUMPS = gdxArrayOf(Vector2(4f, 10f), Vector2(6f, 10f), Vector2(8f, 10f), Vector2(8f, 12f))
        private val STEP_JUMPS = gdxArrayOf(Vector2(2f, 10f), Vector2(3f, 12f), Vector2(4f, 13f))
        private val PLATFORM_JUMPS = gdxArrayOf(Vector2(2f, 16f), Vector2(3f, 16f), Vector2(4f, 16f), Vector2(5f, 16f))
        private val HOP_JUMPS = gdxArrayOf(Vector2(0f, 10f), Vector2(2f, 10f), Vector2(-2f, 10f))

        private val TRANSITIONS = objectMapOf(
            MetState.SHIELDING pairTo gdxArrayOf(
                MetState.POP_UP, MetState.RUNNING, MetState.STUNNED, MetState.DESPERATE_RUN
            ),
            MetState.POP_UP pairTo gdxArrayOf(
                MetState.SHIELDING, MetState.RUNNING, MetState.JUMPING, MetState.STUNNED, MetState.DESPERATE_RUN
            ),
            MetState.RUNNING pairTo gdxArrayOf(
                MetState.SHIELDING, MetState.JUMPING, MetState.STUNNED, MetState.DESPERATE_RUN
            ),
            MetState.JUMPING pairTo gdxArrayOf(
                MetState.SHIELDING, MetState.STUNNED, MetState.DESPERATE_RUN, MetState.FETCH_HELMET, MetState.CRY
            ),
            MetState.STUNNED pairTo gdxArrayOf(
                MetState.HELMET_LAND, MetState.FETCH_HELMET, MetState.CRY, MetState.DESPERATE_RUN
            ),
            MetState.HELMET_LAND pairTo gdxArrayOf(MetState.SHIELDING, MetState.STUNNED, MetState.DESPERATE_RUN),
            MetState.FETCH_HELMET pairTo gdxArrayOf(MetState.PICKUP_HELMET, MetState.JUMPING, MetState.CRY),
            MetState.PICKUP_HELMET pairTo gdxArrayOf(MetState.SHIELDING, MetState.STUNNED, MetState.DESPERATE_RUN),
            MetState.CRY pairTo gdxArrayOf(MetState.DESPERATE_RUN),
            MetState.DESPERATE_RUN pairTo gdxArrayOf(
                MetState.DESPERATE_PAUSE, MetState.JUMPING, MetState.PICKUP_HELMET
            ),
            MetState.DESPERATE_PAUSE pairTo gdxArrayOf(
                MetState.DESPERATE_RUN, MetState.JUMPING, MetState.PICKUP_HELMET
            )
        )
    }

    override lateinit var facing: Facing

    override var direction: Direction
        get() = body.direction
        set(value) {
            body.direction = value
        }

    override var frozen: Boolean
        get() = freezeHandler.isFrozen()
        set(value) {
            freezeHandler.setFrozen(value)
        }

    private val freezeHandler = FreezableEntityHandler(this)

    private lateinit var stateMachine: StateMachine<MetState>
    private val state: MetState
        get() = stateMachine.getCurrentElement()
    private var requestedState: MetState? = null
    private val noParams = Array<Any?>()

    private val stateTimer = Timer(SHIELDING_DURATION)
    private val jumpCheckTimer = Timer(JUMP_CHECK_DELAY)

    private lateinit var groundProbe: GroundProbe
    private lateinit var jumpPlanner: JumpPlanner
    private val jumpPlan = JumpPlan()
    private val planStart = GameRectangle()
    private val localLanding = GameRectangle()
    private val localTarget = GameRectangle()
    private val worldBounds = GameRectangle()
    private val helmetBounds = GameRectangle()
    private val playerBounds = GameRectangle()
    private val stunShotBounds = GameRectangle()
    private val stunMetBounds = GameRectangle()

    private val gapGoal: (GameRectangle) -> Boolean = { landing ->
        GravityFrame.toLocal(direction, landing, localLanding)
        (localLanding.getX() - planStart.getX()) * facing.value >= GAP_MIN_PROGRESS * ConstVals.PPM
    }
    private val stepGoal: (GameRectangle) -> Boolean = { landing ->
        GravityFrame.toLocal(direction, landing, localLanding)
        localLanding.getY() - planStart.getY() >= STEP_MIN_RISE * ConstVals.PPM &&
            (localLanding.getX() - planStart.getX()) * facing.value > 0f
    }
    private val platformGoal: (GameRectangle) -> Boolean = { landing ->
        GravityFrame.toLocal(direction, landing, localLanding)
        localLanding.getY() - planStart.getY() >= PLATFORM_MIN_RISE * ConstVals.PPM &&
            abs(centerX(localLanding) - centerX(localTarget)) <
            abs(centerX(planStart) - centerX(localTarget))
    }
    private val hopGoal: (GameRectangle) -> Boolean = { true }

    private val judgedShots = ObjectSet<MegaGameEntity>()
    private val shotsToForget = Array<MegaGameEntity>()

    private lateinit var type: String
    private var runOnly = false
    private var runningAllowed = false
    private var runSpeed = RUN_SPEED
    private var helmetHits = 0
    private var wearingHelmet = true
    private var helmetBroken = false
    private var fetching = false
    private var stunFull = false
    private var stunTime = 0f
    private var helmetSeated = false
    private var helmet: MetHelmet? = null
    private var stunShot: ChargedShot? = null
    private var leftGround = false
    private var blockedAhead = false

    private val shielded: Boolean
        get() = state == MetState.SHIELDING || state == MetState.HELMET_LAND ||
            (state == MetState.STUNNED && stunShotTouchesMe())
    private val desperate: Boolean
        get() = state == MetState.DESPERATE_RUN || state == MetState.DESPERATE_PAUSE
    private val hardMode: Boolean
        get() = services.gameState.getDifficultyMode() == DifficultyMode.HARD

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        super.init()
        if (atlas == null) atlas = services.assets.getTextureAtlas(TextureAsset.ENEMIES_1.source)
        MetHelmet.loadPieces(services)
        addComponent(defineAnimationsComponent())
        damageOverrides.put(GutsTankFist::class, dmgNeg(ConstVals.MAX_HEALTH))
        damageOverrides.put(BigAssMaverickRobotOrb::class, dmgNeg(ConstVals.MAX_HEALTH))
        damageOverrides.put(PurpleBlast::class, dmgNeg(ConstVals.MAX_HEALTH))
        groundProbe = GroundProbe(this, services.levelState)
        jumpPlanner = JumpPlanner(
            owner = this,
            probe = groundProbe,
            gravity = GRAVITY_IN_AIR,
            maxSpeedForward = VELOCITY_CLAMP_X,
            maxSpeedUp = VELOCITY_CLAMP_Y,
            maxFlightTime = MAX_JUMP_DURATION,
            fixedStep = { services.settings.performance.fixedStep }
        )
        stateMachine = buildStateMachine()
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = if (spawnProps.containsKey(ConstKeys.BOUNDS)) {
            val bounds = spawnProps.get(ConstKeys.BOUNDS) as GameRectangle
            bounds.getPositionPoint(Position.BOTTOM_CENTER)
        } else spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setBottomCenterToPoint(spawn)

        runningAllowed = spawnProps.getOrDefault(RUNNING_ALLOWED, true, Boolean::class)
        runOnly = spawnProps.getOrDefault(RUN_ONLY, false, Boolean::class)
        runSpeed = spawnProps.getOrDefault(ConstKeys.SPEED, RUN_SPEED, Float::class)

        type = spawnProps.getOrDefault(ConstKeys.TYPE, "", String::class)

        val right = spawnProps.getOrDefault(ConstKeys.RIGHT, targetPlayer.body.getX() > body.getX(), Boolean::class)
        facing = if (right) Facing.RIGHT else Facing.LEFT

        direction = Direction.UP

        frozen = false

        helmetHits = 0
        wearingHelmet = true
        helmetBroken = false
        fetching = false
        stunFull = false
        stunTime = 0f
        helmetSeated = false
        helmet = null
        stunShot = null
        leftGround = false
        requestedState = null
        judgedShots.clear()
        jumpCheckTimer.resetDuration(JUMP_CHECK_DELAY)
        stateMachine.reset()
        enterState(MetState.SHIELDING)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        frozen = false
        judgedShots.clear()
        helmet?.release()
        helmet = null
    }

    override fun canBeDamagedBy(damager: IDamagerEntity): Boolean {
        if (damageOverrides.containsKey(damager::class)) return true
        return super.canBeDamagedBy(damager)
    }

    override fun takeDamageFrom(damager: IDamagerEntity): Boolean {
        if (damageOverrides.containsKey(damager::class)) explode()
        val damaged = super.takeDamageFrom(damager)
        if (damaged && damager is ChargedShot && !isHealthDepleted()) onChargedHit(damager)
        return damaged
    }

    fun onHelmetBroken() {
        GameLogger.debug(TAG, "onHelmetBroken()")
        helmet = null
        helmetBroken = true
        if (state == MetState.FETCH_HELMET) requestState(MetState.CRY)
    }

    fun onHelmetLost(lost: MetHelmet) {
        if (lost !== helmet) return
        GameLogger.debug(TAG, "onHelmetLost()")
        helmet = null
        if (state == MetState.FETCH_HELMET) requestState(MetState.CRY)
    }

    fun onHelmetSeated() {
        GameLogger.debug(TAG, "onHelmetSeated()")
        helmetSeated = true
    }

    private fun onHitByProjectile(projectile: IProjectileEntity) {
        if (projectile is ChargedShot && shielded) onChargedHit(projectile)
    }

    private fun onChargedHit(shot: ChargedShot) {
        if (runOnly || frozen || !wearingHelmet) return
        GameLogger.debug(TAG, "onChargedHit(): fullyCharged=${shot.fullyCharged}")
        helmetHits++
        if (helmetHits >= MetHelmet.HITS_TO_BREAK) breakHelmet() else stun(shot)
    }

    private fun stun(shot: ChargedShot) {
        stunFull = shot.fullyCharged
        stunShot = shot
        requestState(MetState.STUNNED)
    }

    private fun stunShotTouchesMe(): Boolean {
        val shot = stunShot ?: return false
        if (!shot.existenceState.liveNow) return false
        return shot.body.getBounds(stunShotBounds).overlaps(body.getBounds(stunMetBounds))
    }

    private fun launchHelmet() {
        val launched = MegaEntityFactory.fetch(MetHelmet::class)!!
        launched.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.TYPE pairTo type,
                ConstKeys.SOURCE pairTo stunShot!!,
                ConstKeys.COUNT pairTo helmetHits,
                ConstKeys.HEIGHT pairTo if (stunFull) HELMET_APEX_FULL else HELMET_APEX_HALF
            )
        )
        helmet = launched
        wearingHelmet = false
    }

    private fun putOnHelmet() {
        val worn = helmet!!
        helmetHits = worn.hits
        wearingHelmet = true
        fetching = false
        worn.consume()
        helmet = null
    }

    private fun claimFreeHelmet(): Boolean {
        body.getBounds(worldBounds)
        for (entity in MegaGameEntities.getOfTag(MetHelmet.TAG)) {
            if (entity !is MetHelmet || entity.owner != null || entity.status == MetHelmet.HelmetStatus.SEATED) continue
            if (!entity.existenceState.liveAfterUpdate) continue
            if (!entity.body.getBounds(helmetBounds).overlaps(worldBounds)) continue
            entity.claim(this)
            helmet = entity
            return true
        }
        return false
    }

    private fun breakHelmet() {
        GameLogger.debug(TAG, "breakHelmet()")
        MetHelmet.throwPieces(type == "Snow", body.getCenter(), services.random)
        wearingHelmet = false
        requestToPlaySound(SoundAsset.ENEMY_DAMAGE_SOUND, loop = false)
        requestState(MetState.DESPERATE_RUN)
    }

    private fun shoot() {
        GameLogger.debug(TAG, "shoot()")

        val trajectory = (when (direction) {
            Direction.UP -> GameObjectPools.fetch(Vector2::class).set(BULLET_TRAJECTORY_X * facing.value, BULLET_TRAJECTORY_Y)
            Direction.DOWN -> GameObjectPools.fetch(Vector2::class).set(BULLET_TRAJECTORY_X * facing.value, -BULLET_TRAJECTORY_Y)
            Direction.LEFT -> GameObjectPools.fetch(Vector2::class).set(BULLET_TRAJECTORY_Y, BULLET_TRAJECTORY_X * facing.value)
            Direction.RIGHT -> GameObjectPools.fetch(Vector2::class).set(BULLET_TRAJECTORY_Y, -BULLET_TRAJECTORY_X * facing.value)
        }).scl(ConstVals.PPM.toFloat())

        val offset = 0.1f * ConstVals.PPM
        val spawn = body.getCenter().add(offset * facing.value, if (direction == Direction.DOWN) offset else -offset)

        val spawnProps = props(
            ConstKeys.OWNER pairTo this, ConstKeys.TRAJECTORY pairTo trajectory, ConstKeys.POSITION pairTo spawn
        )
        val bullet = MegaEntityFactory.fetch(Bullet::class)!!
        bullet.spawn(spawnProps)

        requestToPlaySound(SoundAsset.ENEMY_BULLET_SOUND, loop = false)
    }

    private fun buildStateMachine(): StateMachine<MetState> {
        val builder = EnumStateMachineBuilder.create<MetState>()
            .initialState(MetState.SHIELDING)
            .onChangeState { current, _ -> enterState(current) }
        TRANSITIONS.forEach { entry ->
            entry.value.forEach { to -> builder.transition(entry.key, to) { requestedState == to } }
        }
        return builder.build()
    }

    private fun requestState(next: MetState) {
        val previous = state
        requestedState = next
        stateMachine.next(noParams)
        requestedState = null
        if (state != next)
            throw IllegalStateException("$TAG: no transition $previous -> $next; add it to TRANSITIONS")
    }

    private fun enterState(next: MetState) {
        GameLogger.debug(TAG, "enterState(): $next")
        when (next) {
            MetState.SHIELDING -> {
                if (!runOnly) stopRunning()
                stateTimer.resetDuration(if (hardMode) SHIELDING_DURATION_HARD else SHIELDING_DURATION)
            }
            MetState.POP_UP -> stateTimer.resetDuration(POP_UP_DURATION)
            MetState.RUNNING -> {
                if (hardMode && !runOnly) facePlayer()
                stateTimer.resetDuration(if (hardMode) RUNNING_DURATION_HARD else RUNNING_DURATION)
            }
            MetState.JUMPING -> {
                leftGround = false
                body.physics.velocity.set(jumpPlan.worldVelocity)
                stateTimer.resetDuration(MAX_JUMP_DURATION)
            }
            MetState.STUNNED -> {
                stopRunning()
                stunTime = 0f
                helmetSeated = false
                helmetBroken = false
                launchHelmet()
            }
            MetState.HELMET_LAND -> {
                stopRunning()
                putOnHelmet()
                stateTimer.resetDuration(HELMET_LAND_FRAME_DURATION * HELMET_LAND_FRAMES)
            }
            MetState.FETCH_HELMET -> {
                fetching = true
                jumpCheckTimer.resetDuration(JUMP_CHECK_DELAY)
                stateTimer.resetDuration(FETCH_TIMEOUT)
            }
            MetState.PICKUP_HELMET -> {
                stopRunning()
                putOnHelmet()
                stateTimer.resetDuration(PICKUP_FRAME_DURATION * PICKUP_FRAMES)
            }
            MetState.CRY -> {
                stopRunning()
                fetching = false
                helmet?.release()
                helmet = null
                stateTimer.resetDuration(CRY_DURATION)
            }
            MetState.DESPERATE_RUN -> {
                blockedAhead = false
                facePlayer()
                stateTimer.resetDuration(DESPERATE_RUN_DURATION)
            }
            MetState.DESPERATE_PAUSE -> {
                stopRunning()
                stateTimer.resetDuration(DESPERATE_PAUSE_DURATION)
            }
        }
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            freezeHandler.update(delta)
            if (stunShot?.existenceState?.liveNow == false) stunShot = null

            if (runOnly) {
                updateRunOnly()
                return@add
            }

            if (frozen) {
                if (wearingHelmet && state != MetState.SHIELDING && state != MetState.STUNNED)
                    requestState(MetState.SHIELDING)
                if (body.isSensing(BodySense.FEET_ON_GROUND)) stopRunning()
                return@add
            }

            if (hardMode) dodgeIncomingShots()

            when (state) {
                MetState.SHIELDING -> updateShielding(delta)
                MetState.POP_UP -> updatePopUp(delta)
                MetState.RUNNING -> updateRunning(delta)
                MetState.JUMPING -> updateJumping(delta)
                MetState.STUNNED -> updateStunned(delta)
                MetState.HELMET_LAND -> updateHelmetLand(delta)
                MetState.FETCH_HELMET -> updateFetchHelmet(delta)
                MetState.PICKUP_HELMET -> updatePickupHelmet(delta)
                MetState.CRY -> updateCry(delta)
                MetState.DESPERATE_RUN -> updateDesperateRun(delta)
                MetState.DESPERATE_PAUSE -> updateDesperatePause(delta)
            }
        }
    }

    private fun updateRunOnly() {
        if (!body.isSensing(BodySense.FEET_ON_GROUND)) {
            if (state != MetState.SHIELDING) requestState(MetState.SHIELDING)
            return
        }
        if (state != MetState.RUNNING) requestState(MetState.RUNNING)
        setRunVelocity(runSpeed)
    }

    private fun updateShielding(delta: Float) {
        stopRunning()
        if (!isMegamanShootingAtMe() && body.isSensing(BodySense.FEET_ON_GROUND)) stateTimer.update(delta)
        if (stateTimer.isFinished()) requestState(MetState.POP_UP)
    }

    private fun updatePopUp(delta: Float) {
        if (!body.isSensing(BodySense.FEET_ON_GROUND)) {
            requestState(MetState.SHIELDING)
            return
        }

        facePlayer()
        if (stateTimer.isAtBeginning()) shoot()
        stateTimer.update(delta)
        if (!stateTimer.isFinished()) return

        when {
            !runningAllowed -> requestState(MetState.SHIELDING)
            tryJumpUpTo(targetPlayer.body.getBounds(playerBounds)) -> requestState(MetState.JUMPING)
            !runOrJumpAhead(runSpeed, delta, targetPlayer.body.getBounds(playerBounds)) -> requestState(MetState.SHIELDING)
            state == MetState.POP_UP -> requestState(MetState.RUNNING)
        }
    }

    private fun updateRunning(delta: Float) {
        if (!body.isSensing(BodySense.FEET_ON_GROUND)) {
            requestState(MetState.SHIELDING)
            return
        }

        jumpCheckTimer.update(delta)
        if (jumpCheckTimer.isFinished()) {
            jumpCheckTimer.resetDuration(JUMP_CHECK_DELAY)
            if (tryJumpUpTo(targetPlayer.body.getBounds(playerBounds))) {
                requestState(MetState.JUMPING)
                return
            }
        }

        if (!runOrJumpAhead(runSpeed, delta, targetPlayer.body.getBounds(playerBounds))) {
            requestState(MetState.SHIELDING)
            return
        }
        if (state != MetState.RUNNING) return

        stateTimer.update(delta)
        if (stateTimer.isFinished()) requestState(MetState.SHIELDING)
    }

    private fun updateJumping(delta: Float) {
        stateTimer.update(delta)
        val onGround = body.isSensing(BodySense.FEET_ON_GROUND)
        if (!onGround) leftGround = true
        if (!(leftGround && onGround) && !stateTimer.isFinished()) return

        stopRunning()
        requestState(
            when {
                wearingHelmet -> MetState.SHIELDING
                fetching && helmet != null -> MetState.FETCH_HELMET
                fetching -> MetState.CRY
                else -> MetState.DESPERATE_RUN
            }
        )
    }

    private fun updateStunned(delta: Float) {
        stopRunning()

        stunTime += delta
        if (stunTime < STUN_MIN) return
        val flying = helmet
        when {
            flying == null -> requestState(if (helmetBroken) MetState.DESPERATE_RUN else MetState.CRY)
            helmetSeated -> requestState(MetState.HELMET_LAND)
            flying.status == MetHelmet.HelmetStatus.RESTING || stunTime >= STUN_MAX ->
                requestState(MetState.FETCH_HELMET)
        }
    }

    private fun updateFetchHelmet(delta: Float) {
        val target = helmet
        if (target == null) {
            requestState(MetState.CRY)
            return
        }
        if (target.body.getBounds(helmetBounds).overlaps(body.getBounds(worldBounds))) {
            requestState(MetState.PICKUP_HELMET)
            return
        }
        if (!body.isSensing(BodySense.FEET_ON_GROUND)) return

        stateTimer.update(delta)
        if (stateTimer.isFinished()) {
            requestState(MetState.CRY)
            return
        }

        jumpCheckTimer.update(delta)
        if (jumpCheckTimer.isFinished()) {
            jumpCheckTimer.resetDuration(JUMP_CHECK_DELAY)
            if (tryJumpUpTo(helmetBounds)) {
                requestState(MetState.JUMPING)
                return
            }
        }

        faceTarget(helmetBounds)
        if (!runOrJumpAhead(runSpeed, delta, helmetBounds)) requestState(MetState.CRY)
    }

    private fun updatePickupHelmet(delta: Float) {
        stopRunning()
        stateTimer.update(delta)
        if (stateTimer.isFinished()) requestState(MetState.SHIELDING)
    }

    private fun updateCry(delta: Float) {
        stopRunning()
        stateTimer.update(delta)
        if (stateTimer.isFinished()) requestState(MetState.DESPERATE_RUN)
    }

    private fun updateHelmetLand(delta: Float) {
        stopRunning()
        stateTimer.update(delta)
        if (stateTimer.isFinished()) requestState(MetState.SHIELDING)
    }

    private fun updateDesperateRun(delta: Float) {
        if (!body.isSensing(BodySense.FEET_ON_GROUND)) return
        if (claimFreeHelmet()) {
            requestState(MetState.PICKUP_HELMET)
            return
        }

        jumpCheckTimer.update(delta)
        val planNow = jumpCheckTimer.isFinished()
        if (planNow) {
            jumpCheckTimer.resetDuration(JUMP_CHECK_DELAY_DESPERATE)
            if (tryJumpUpTo(targetPlayer.body.getBounds(playerBounds))) {
                requestState(MetState.JUMPING)
                return
            }
        }

        blockedAhead = !runOrJumpAhead(
            runSpeed * DESPERATE_SPEED_SCALAR, delta, targetPlayer.body.getBounds(playerBounds), mayPlanJumps = planNow
        )
        if (state != MetState.DESPERATE_RUN) return
        if (blockedAhead) facePlayer()

        stateTimer.update(delta)
        if (stateTimer.isFinished()) requestState(MetState.DESPERATE_PAUSE)
    }

    private fun updateDesperatePause(delta: Float) {
        stopRunning()
        if (claimFreeHelmet()) {
            requestState(MetState.PICKUP_HELMET)
            return
        }
        facePlayer()
        stateTimer.update(delta)
        if (stateTimer.isFinished()) requestState(MetState.DESPERATE_RUN)
    }

    private fun runOrJumpAhead(speed: Float, delta: Float, target: GameRectangle, mayPlanJumps: Boolean = true): Boolean {
        val step = maxOf(delta, services.settings.performance.fixedStep)
        val ahead = groundProbe.checkAhead(
            facing, direction, lead = speed * step * ConstVals.PPM, maxDrop = MAX_SAFE_DROP * ConstVals.PPM
        )
        when (ahead) {
            GroundAhead.FLOOR, GroundAhead.SAFE_DROP -> {
                setRunVelocity(speed)
                return true
            }
            GroundAhead.PIT -> if (mayPlanJumps && isAhead(target) && planJump(GAP_JUMPS, gapGoal, target)) {
                requestState(MetState.JUMPING)
                return true
            }
            GroundAhead.WALL -> if (mayPlanJumps && planJump(STEP_JUMPS, stepGoal, target)) {
                requestState(MetState.JUMPING)
                return true
            }
        }

        stopRunning()
        return false
    }

    private fun tryJumpUpTo(target: GameRectangle): Boolean {
        if (!body.isSensing(BodySense.FEET_ON_GROUND)) return false
        GravityFrame.toLocal(direction, target, localTarget)
        GravityFrame.toLocal(direction, body.getBounds(worldBounds), planStart)
        if (localTarget.getY() - planStart.getY() < PLAYER_ABOVE_MIN * ConstVals.PPM) return false
        if (abs(centerX(localTarget) - centerX(planStart)) > PLAYER_ABOVE_MAX_DX * ConstVals.PPM) return false
        val previousFacing = facing
        facing = if (centerX(localTarget) > centerX(planStart)) Facing.RIGHT else Facing.LEFT
        if (jumpPlanner.plan(facing, direction, PLATFORM_JUMPS, platformGoal, jumpPlan)) return true
        facing = previousFacing
        return false
    }

    private fun planJump(candidates: Array<Vector2>, goal: (GameRectangle) -> Boolean, target: GameRectangle): Boolean {
        GravityFrame.toLocal(direction, target, localTarget)
        GravityFrame.toLocal(direction, body.getBounds(worldBounds), planStart)
        return jumpPlanner.plan(facing, direction, candidates, goal, jumpPlan)
    }

    private fun dodgeIncomingShots() {
        val exposed = state == MetState.POP_UP || state == MetState.RUNNING || desperate
        forgetDeadShots()
        if (!exposed || !body.isSensing(BodySense.FEET_ON_GROUND)) return

        GravityFrame.toLocal(direction, body.getBounds(worldBounds), planStart)
        for (entity in MegaGameEntities.getOfType(EntityType.PROJECTILE)) {
            if (entity !is IProjectileEntity || entity !is IBodyEntity || judgedShots.contains(entity)) continue
            if (entity.owner.asPlayer == null || !isShotComing(entity)) continue
            judgedShots.add(entity)
            if (services.random.nextInt(1, DODGE_CHANCE_DENOMINATOR) != 1) continue
            dodge()
            return
        }
    }

    private fun isShotComing(shot: IBodyEntity): Boolean {
        GravityFrame.toLocal(direction, shot.body.getBounds(worldBounds), localLanding)
        val dx = centerX(localLanding) - centerX(planStart)
        if (abs(dx) > DODGE_RANGE * ConstVals.PPM) return false
        if (localLanding.getMaxY() < planStart.getY() || localLanding.getY() > planStart.getMaxY()) return false
        val localVelocity =
            GravityFrame.velocityToLocal(direction, shot.body.physics.velocity, GameObjectPools.fetch(Vector2::class))
        return localVelocity.x * dx < 0f
    }

    private fun dodge() {
        GameLogger.debug(TAG, "dodge(): wearingHelmet=$wearingHelmet")
        if (wearingHelmet) {
            requestState(MetState.SHIELDING)
            return
        }
        if (planJump(HOP_JUMPS, hopGoal, targetPlayer.body.getBounds(playerBounds))) requestState(MetState.JUMPING)
    }

    private fun forgetDeadShots() {
        judgedShots.forEach { if (!it.existenceState.liveNow) shotsToForget.add(it) }
        shotsToForget.forEach { judgedShots.remove(it) }
        shotsToForget.clear()
    }

    private fun isAhead(target: GameRectangle): Boolean {
        GravityFrame.toLocal(direction, target, localTarget)
        GravityFrame.toLocal(direction, body.getBounds(worldBounds), planStart)
        return (centerX(localTarget) - centerX(planStart)) * facing.value > 0f
    }

    private fun faceTarget(target: GameRectangle) {
        GravityFrame.toLocal(direction, target, localTarget)
        GravityFrame.toLocal(direction, body.getBounds(worldBounds), planStart)
        facing = if (centerX(localTarget) > centerX(planStart)) Facing.RIGHT else Facing.LEFT
    }

    private fun centerX(rect: GameRectangle) = rect.getX() + rect.getWidth() / 2f

    private fun facePlayer() {
        facing = when (direction) {
            Direction.UP, Direction.DOWN -> if (targetPlayer.body.getX() > body.getX()) Facing.RIGHT else Facing.LEFT
            Direction.LEFT, Direction.RIGHT -> if (targetPlayer.body.getY() > body.getY()) Facing.RIGHT else Facing.LEFT
        }
    }

    private fun setRunVelocity(speed: Float) {
        val impulse = ConstVals.PPM * facing.value * if (body.isSensing(BodySense.IN_WATER)) (speed / 2f) else speed
        when (direction) {
            Direction.UP, Direction.DOWN -> body.physics.velocity.x = impulse
            Direction.LEFT, Direction.RIGHT -> body.physics.velocity.y = impulse
        }
    }

    private fun stopRunning() {
        when (direction) {
            Direction.UP, Direction.DOWN -> body.physics.velocity.x = 0f
            Direction.LEFT, Direction.RIGHT -> body.physics.velocity.y = 0f
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.setSize(ConstVals.PPM.toFloat())

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        body.addFixture(feetFixture)
        feetFixture.drawingColor = Color.GREEN
        debugShapes.add { feetFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            val clampX = if (desperate) VELOCITY_CLAMP_X * DESPERATE_SPEED_SCALAR else VELOCITY_CLAMP_X
            body.physics.velocityClamp.set(
                when {
                    direction.isVertical() -> GameObjectPools.fetch(Vector2::class).set(clampX, VELOCITY_CLAMP_Y)
                    else -> GameObjectPools.fetch(Vector2::class).set(VELOCITY_CLAMP_Y, clampX)
                }
            ).scl(ConstVals.PPM.toFloat())

            val gravity = if (body.isSensing(BodySense.FEET_ON_GROUND)) GRAVITY_ON_GROUND else GRAVITY_IN_AIR
            val gravityVec = GameObjectPools.fetch(Vector2::class)
            when (direction) {
                Direction.UP -> gravityVec.set(0f, -gravity)
                Direction.DOWN -> gravityVec.set(0f, gravity)
                Direction.LEFT -> gravityVec.set(gravity, 0f)
                Direction.RIGHT -> gravityVec.set(-gravity, 0f)
            }.scl(ConstVals.PPM.toFloat())
            body.physics.gravity.set(gravityVec)

            body.fixtures.get(FixtureType.SHIELD).first().setActive(shielded)
            body.fixtures.get(FixtureType.DAMAGEABLE).first().setActive(!shielded)
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        HalfDomeShield(body, MetHelmet.DOME_RADIUS * ConstVals.PPM, direction = { direction }, passes = { false })
        val component = BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
        body.fixtures.get(FixtureType.SHIELD).first().setReceiver(Receivers.HIT_BY_PROJECTILE, ::onHitByProjectile)
        return component
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 4))
        sprite.setSize(2f * ConstVals.PPM)
        val component = SpritesComponent(sprite)
        component.putPreProcess { _, _ ->
            sprite.hidden = damageBlink

            val flipX = facing == Facing.LEFT
            val flipY = direction == Direction.DOWN
            sprite.setFlip(flipX, flipY)

            val rotation = when (direction) {
                Direction.UP, Direction.DOWN -> 0f
                Direction.LEFT -> 90f
                Direction.RIGHT -> 270f
            }
            sprite.setOriginCenter()
            sprite.rotation = rotation

            val position = when (direction) {
                Direction.UP -> Position.BOTTOM_CENTER
                Direction.DOWN -> Position.TOP_CENTER
                Direction.LEFT -> Position.CENTER_RIGHT
                Direction.RIGHT -> Position.CENTER_LEFT
            }
            val bodyPosition = body.getPositionPoint(position)
            sprite.setPosition(bodyPosition, position)
        }

        return component
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = {
            if (frozen) "frozen"
            else "$type${
                when (state) {
                    MetState.SHIELDING -> "LayDown"
                    MetState.POP_UP -> "PopUp"
                    MetState.RUNNING -> "Run"
                    MetState.JUMPING -> if (wearingHelmet) "Jump" else "NoHelmetJump"
                    MetState.STUNNED -> "Stunned"
                    MetState.HELMET_LAND -> "HelmetLand"
                    MetState.FETCH_HELMET -> "WorriedRun"
                    MetState.PICKUP_HELMET -> "Pickup"
                    MetState.CRY -> "Cry"
                    MetState.DESPERATE_RUN -> if (blockedAhead) "NoHelmetStand" else "NoHelmetRun"
                    MetState.DESPERATE_PAUSE -> "NoHelmetStand"
                }
            }"
        }
        val animations = objectMapOf<String, IAnimation>("frozen" pairTo Animation(atlas!!.findRegion("Met/frozen"), loop = false))
        arrayOf("" to "Met", "Snow" to "SnowMet").forEach { (prefix, folder) ->
            animations.put("${prefix}Run", Animation(atlas!!.findRegion("$folder/Run"), 1, 2, 0.125f, loop = true))
            animations.put("${prefix}PopUp", Animation(atlas!!.findRegion("$folder/PopUp"), loop = false))
            animations.put("${prefix}LayDown", Animation(atlas!!.findRegion("$folder/LayDown"), loop = false))
            animations.put("${prefix}Jump", Animation(atlas!!.findRegion("$folder/Jump"), loop = false))
            animations.put("${prefix}Stunned", Animation(atlas!!.findRegion("$folder/Stunned"), 1, 4, 0.1f, loop = true))
            animations.put(
                "${prefix}HelmetLand",
                Animation(atlas!!.findRegion("$folder/HelmetLand"), 1, HELMET_LAND_FRAMES, HELMET_LAND_FRAME_DURATION, loop = false)
            )
            animations.put("${prefix}NoHelmetRun", Animation(atlas!!.findRegion("$folder/NoHelmetRun"), 1, 2, 0.1f, loop = true))
            animations.put("${prefix}NoHelmetStand", Animation(atlas!!.findRegion("$folder/NoHelmetStand"), loop = false))
            animations.put("${prefix}WorriedRun", Animation(atlas!!.findRegion("$folder/WorriedRun"), 1, 2, 0.1f, loop = true))
            animations.put(
                "${prefix}Pickup",
                Animation(atlas!!.findRegion("$folder/Pickup"), 1, PICKUP_FRAMES, PICKUP_FRAME_DURATION, loop = false)
            )
            animations.put("${prefix}Cry", Animation(atlas!!.findRegion("$folder/Cry"), 1, 2, 0.15f, loop = true))
            animations.put("${prefix}NoHelmetJump", Animation(atlas!!.findRegion("$folder/NoHelmetJump"), loop = false))
        }
        return AnimationsComponent(this, Animator(keySupplier, animations))
    }
}
