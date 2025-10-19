package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods.getRandom
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.*
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.TimeMarkedRunnable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.common.utils.OrbitUtils
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.state.StateMachine
import com.mega.game.engine.state.StateMachineBuilder
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.projectiles.Axe
import com.megaman.maverick.game.entities.projectiles.PreciousGem
import com.megaman.maverick.game.entities.projectiles.PreciousGem.PreciousGemColor
import com.megaman.maverick.game.entities.projectiles.PreciousGemCluster
import com.megaman.maverick.game.entities.projectiles.SlashWave
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.MegaUtilMethods
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.misc.FacingUtils
import com.megaman.maverick.game.utils.misc.HeadUtils
import com.megaman.maverick.game.world.body.*
import kotlin.math.abs

class PreciousWoman(game: MegamanMaverickGame) : AbstractBoss(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "PreciousWoman"

        const val SPRITE_SIZE = 3f

        const val SHIELD_GEM_SPIN_SPEED = 0.25f
        const val SHIELD_GEM_CLUSTER_SPEED = 8f
        const val SHIELD_GEM_MAX_DIST_FROM_ORIGIN = 20f
        const val SHIELD_GEM_START_OFFSET = 1.5f
        const val GEM_THROW_SPEED = 10f

        val SHIELD_GEMS_ANGLES = gdxArrayOf(0f, 90f, 180f, 270f)
        val GEM_COLORS = PreciousGemColor.entries.toGdxArray()

        private const val BODY_WIDTH = 1f
        private const val BODY_HEIGHT = 2f

        private const val RUN_DUR = 1.5f
        private const val RUN_CHANCE = 20f
        private const val RUN_IMPULSE_X = 35f
        private const val MAX_RUN_SPEED = 7.5f

        private const val VEL_CLAMP_X = 50f
        private const val VEL_CLAMP_Y = 25f

        private const val GRAVITY = -0.15f
        private const val GROUND_GRAVITY = -0.01f

        private const val DEFAULT_FRICTION_X = 1f
        private const val DEFAULT_FRICTION_Y = 1f
        private const val WALLSLIDE_FRICTION_Y = 10f

        private const val INIT_DUR = 1f
        private const val STAND_DUR = 1f
        private const val WALL_SLIDE_DUR = 1f
        private const val GROUND_SLIDE_DUR = 0.5f
        private const val LAUGH_1_DUR = 0.25f
        private const val LAUGH_2_DUR = 1.25f
        private const val THROW_GEMS_DUR = 1f
        private const val THROW_SHIELD_GEMS_DUR = 1f
        private const val THROW_TIME = 0.1f
        private const val SPAWN_SHIELDS_DUR = 1f
        private const val JUMP_UPDATE_FACING_DELAY = 0.5f

        private const val STUNNED_DUR = 0.75f
        private const val STUNNED_DAMAGE_DUR = 2f

        private const val GROUNDSLIDE_CHANCE = 20f
        private const val GROUNDSLIDE_VEL_X = 9f

        private const val AIRPUNCH_DELAY = 0.25f
        private const val AIRPUNCH_MAX_DUR = 1f
        private const val AIRPUNCH_COOLDOWN = 3f
        private const val AIRPUNCH_VEL_X = 12f
        private const val AIRPUNCH_CHANCE = 50f

        private const val JUMP_CHANCE_FIRST_CHECK = 20f
        private const val JUMP_CHANCE_SECOND_CHECK = 75f
        private const val JUMP_MAX_IMPULSE_X = 10f
        private const val JUMP_IMPULSE_Y = 16f
        private const val WALL_JUMP_IMPULSE_X = 5f

        private const val GEMS_TO_THROW = 3
        private const val THROW_GEM_OFFSET_X = 3f
        private const val THROW_GEM_OFFSET_Y = 1.5f
        private val THROW_OFFSETS = gdxArrayOf(
            Vector2(THROW_GEM_OFFSET_X, THROW_GEM_OFFSET_Y),
            Vector2(THROW_GEM_OFFSET_X, 0f),
            Vector2(THROW_GEM_OFFSET_X, -THROW_GEM_OFFSET_Y)
        )
        private val THROW_NOT_ALLOWED_STATES = objectSetOf(
            PreciousWomanState.GROUNDSLIDE,
            PreciousWomanState.WALLSLIDE,
            PreciousWomanState.AIRPUNCH,
        )

        private const val CAN_SPAWN_SHIELD_GEMS_DELAY = 4f
        private const val SPAWN_SHIELD_START_CHANCE = 20f
        private const val SPAWN_SHIELD_CHANCE_DELTA = 10f

        // the amount of times she should enter stand/jump state before throwing gems
        private const val STATES_BETWEEN_THROW = 4
        private const val MIN_THROW_COOLDOWN = 3f

        private val animDefs = orderedMapOf(
            "airpunch1" pairTo AnimationDef(3, 1, 0.1f, false),
            "airpunch2" pairTo AnimationDef(2, 1, 0.1f, true),
            "damaged" pairTo AnimationDef(2, 2, 0.1f, true),
            "defeated" pairTo AnimationDef(2, 2, 0.1f, true),
            "groundslide" pairTo AnimationDef(),
            "jump" pairTo AnimationDef(),
            "jump_throw" pairTo AnimationDef(5, 1, 0.1f, false),
            "run" pairTo AnimationDef(2, 2, 0.1f, true),
            "stand" pairTo AnimationDef(2, 1, gdxArrayOf(1f, 0.15f), true),
            "stand_laugh1" pairTo AnimationDef(2, 1, 0.1f, false),
            "stand_laugh2" pairTo AnimationDef(2, 1, 0.1f, true),
            "stand_throw" pairTo AnimationDef(5, 1, 0.1f, false),
            "wallslide" pairTo AnimationDef(),
            "wink" pairTo AnimationDef(3, 3, 0.1f, false)
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class PreciousWomanState {
        INIT, STAND, RUN, JUMP, GROUNDSLIDE, WALLSLIDE, AIRPUNCH, SPAWN_SHIELD_GEMS, THROW_SHIELD_GEMS
    }

    data class ShieldGemDef(var angle: Float, var distance: Float, var released: Boolean) {

        fun set(angle: Float, distance: Float, released: Boolean) {
            this.angle = angle
            this.distance = distance
            this.released = released
        }
    }

    override val invincible: Boolean
        get() = super.invincible || stunned

    override lateinit var facing: Facing

    private lateinit var stateMachine: StateMachine<PreciousWomanState>
    private val currentState: PreciousWomanState
        get() = stateMachine.getCurrentElement()
    private val stateTimers = OrderedMap<PreciousWomanState, Timer>()

    private val laughTimer = Timer(LAUGH_1_DUR + LAUGH_2_DUR)
    private val laughing: Boolean
        get() = !laughTimer.isFinished()

    private val throwingTimer = Timer(THROW_GEMS_DUR).addRunnables(
        TimeMarkedRunnable(THROW_TIME) { throwHomingGems() }.setToRunOnlyWhenJustPassedTime(true)
    )
    private val throwing: Boolean
        get() = !throwingTimer.isFinished() || !throwShieldGemsTimer.isFinished()
    private val throwMinCooldown = Timer(MIN_THROW_COOLDOWN)
    private var statesSinceLastThrow = 0
    private val throwShieldGemsTimer = Timer(THROW_GEMS_DUR)

    private val stunnedTimer = Timer(STUNNED_DUR)
    private var stunned: Boolean
        get() = !stunnedTimer.isFinished()
        set(value) {
            if (value) stunnedTimer.reset() else stunnedTimer.setToEnd()
        }

    private val spawnShieldsDelay = Timer(CAN_SPAWN_SHIELD_GEMS_DELAY)
    private var spawnShieldsChance = SPAWN_SHIELD_START_CHANCE

    private val jumpUpdateFacingDelay = Timer(JUMP_UPDATE_FACING_DELAY)

    private val airpunchCooldown = Timer(AIRPUNCH_COOLDOWN)

    // declared as var so that reference can be detached when passed to shield gem cluster
    private var shieldGems = OrderedMap<PreciousGem, ShieldGemDef>()

    // launched gems are culled once they reach a certain distance away from the boss room's center
    private val roomCenter = Vector2()

    // if the current update is the first update for this spawned boss
    private var firstUpdate = true

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_3.source)
            animDefs.keys().forEach { key -> regions.put(key, atlas.findRegion("$TAG/$key")) }
        }
        if (stateTimers.isEmpty) stateTimers.putAll(
            PreciousWomanState.RUN pairTo Timer(RUN_DUR),
            PreciousWomanState.INIT pairTo Timer(INIT_DUR),
            PreciousWomanState.STAND pairTo Timer(STAND_DUR),
            PreciousWomanState.WALLSLIDE pairTo Timer(WALL_SLIDE_DUR),
            PreciousWomanState.AIRPUNCH pairTo Timer(AIRPUNCH_MAX_DUR),
            PreciousWomanState.GROUNDSLIDE pairTo Timer(GROUND_SLIDE_DUR),
            PreciousWomanState.SPAWN_SHIELD_GEMS pairTo Timer(SPAWN_SHIELDS_DUR),
            PreciousWomanState.THROW_SHIELD_GEMS pairTo Timer(THROW_SHIELD_GEMS_DUR)
        )
        super.init()
        stateMachine = buildStateMachine()
        addComponent(defineAnimationsComponent())
        damageOverrides.put(Axe::class, dmgNeg(4))
        damageOverrides.put(SlashWave::class, dmgNeg(2))
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val position = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(position)
        body.physics.gravityOn = true
        body.physics.applyFrictionX = true

        stateMachine.reset()
        stateTimers.values().forEach { timer -> timer.reset() }

        laughTimer.setToEnd()
        throwingTimer.setToEnd()

        stunnedTimer.setToEnd()

        throwMinCooldown.reset()
        throwShieldGemsTimer.setToEnd()

        airpunchCooldown.reset()
        jumpUpdateFacingDelay.reset()

        FacingUtils.setFacingOf(this)

        statesSinceLastThrow = 0

        spawnShieldsDelay.reset()
        spawnShieldsChance = SPAWN_SHIELD_START_CHANCE

        roomCenter.set(spawnProps.get(ConstKeys.ROOM, RectangleMapObject::class)!!.rectangle.getCenter())

        firstUpdate = true
    }

    override fun triggerDefeat() {
        GameLogger.debug(TAG, "triggerDefeat()")
        super.triggerDefeat()

        shieldGems.keys().forEach { gem -> gem.destroy() }
        shieldGems.clear()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        shieldGems.keys().forEach { gem -> gem.destroy() }
        shieldGems.clear()
    }

    override fun onMegamanDamaged(damager: IDamager, megaman: Megaman) {
        GameLogger.debug(TAG, "onMegamanDamaged(): damager=$damager, megaman=$megaman")
        if (currentState == PreciousWomanState.STAND) laughTimer.reset()
    }

    override fun takeDamageFrom(damager: IDamager): Boolean {
        GameLogger.debug(TAG, "takeDamageFrom(): damager=$damager")

        val damaged = super.takeDamageFrom(damager)

        if (damaged) {
            if (damager is Axe && !stunned) {
                stunned = true

                laughTimer.setToEnd()

                body.physics.velocity.setZero()

                damageTimer.resetDuration(STUNNED_DAMAGE_DUR)

                if (!shieldGems.isEmpty) {
                    throwShieldGems()
                    stateMachine.next()
                } else if (currentState.equalsAny(PreciousWomanState.GROUNDSLIDE, PreciousWomanState.AIRPUNCH))
                    stateMachine.next()
            } else damageTimer.resetDuration(DEFAULT_BOSS_DMG_DURATION)
        }

        return damaged
    }

    override fun isReady(delta: Float) = stateTimers[PreciousWomanState.INIT].isFinished()

    override fun onReady() {
        GameLogger.debug(TAG, "onReady()")
        super.onReady()
        body.physics.gravityOn = true
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (betweenReadyAndEndBossSpawnEvent) return@add

            if (defeated) {
                body.physics.velocity.setZero()
                body.physics.gravityOn = false
                explodeOnDefeat(delta)
                return@add
            }

            val shieldGemIter = shieldGems.iterator()
            while (shieldGemIter.hasNext) {
                val shieldGemEntry = shieldGemIter.next()
                if (shieldGemEntry.key.dead) shieldGemIter.remove()
            }

            if (shieldGems.isEmpty) spawnShieldsDelay.update(delta)
            if (spawnShieldsDelay.isJustFinished()) GameLogger.debug(TAG, "update(): spawn shields delay just finished")
            if (spawnShieldsDelay.isFinished()) {
                spawnShieldsChance += SPAWN_SHIELD_CHANCE_DELTA * delta
                if (spawnShieldsChance > 100f) spawnShieldsChance = 100f
            }
            if (!shieldGems.isEmpty) spinShieldGems(delta)
            throwShieldGemsTimer.update(delta)

            airpunchCooldown.update(delta)

            stunnedTimer.update(delta)
            if (stunned) {
                if (body.physics.velocity.y > 0f) body.physics.velocity.y = 0f
                if (stunnedTimer.isJustFinished()) damageTimer.reset()
                return@add
            }

            if (!THROW_NOT_ALLOWED_STATES.contains(currentState)) {
                throwingTimer.update(delta)
                if (throwingTimer.isJustFinished()) GameLogger.debug(TAG, "update(): throwing timer just finished")

                throwMinCooldown.update(delta)
                if (throwMinCooldown.isJustFinished()) GameLogger.debug(
                    TAG,
                    "update(): throw min cooldown just finished"
                )
            }

            laughTimer.update(delta)
            if (laughTimer.isJustFinished()) GameLogger.debug(TAG, "update(): laugh timer just finished")

            if (stateTimers.containsKey(currentState)) {
                val stateTimer = stateTimers[currentState]

                if (shouldUpdateStateTimer()) stateTimer.update(delta)

                if (stateTimer.isFinished()) {
                    val next = stateMachine.next()
                    GameLogger.debug(TAG, "update(): state timer finished, set state machine to next=$next")
                }
            }

            when (currentState) {
                PreciousWomanState.AIRPUNCH -> {
                    val time = stateTimers[PreciousWomanState.AIRPUNCH].time

                    body.physics.velocity.x = when {
                        time <= AIRPUNCH_DELAY -> 0f
                        else -> AIRPUNCH_VEL_X * ConstVals.PPM * facing.value
                    }
                    body.physics.velocity.y = 0f

                    if (shouldEndAirPunch()) stateMachine.next()
                }

                PreciousWomanState.INIT, PreciousWomanState.STAND -> {
                    if (!throwing) FacingUtils.setFacingOf(this)
                    if (body.isSensing(BodySense.FEET_ON_GROUND)) body.physics.velocity.x = 0f
                }

                PreciousWomanState.RUN -> {
                    if (FacingUtils.isFacingBlock(this)) swapFacing()

                    body.physics.velocity.let { velocity ->
                        if ((isFacing(Facing.LEFT) && velocity.x > 0f) || (isFacing(Facing.RIGHT) && velocity.x < 0f))
                            velocity.x = 0f

                        if (abs(velocity.x) < MAX_RUN_SPEED * ConstVals.PPM)
                            velocity.x += RUN_IMPULSE_X * ConstVals.PPM * facing.value * delta

                        velocity.x = velocity.x.coerceIn(-MAX_RUN_SPEED * ConstVals.PPM, MAX_RUN_SPEED * ConstVals.PPM)

                        if (velocity.y > 0f) velocity.y = 0f
                    }
                }

                PreciousWomanState.JUMP -> {
                    jumpUpdateFacingDelay.update(delta)

                    if (jumpUpdateFacingDelay.isFinished() && !throwing) FacingUtils.setFacingOf(this)

                    if (shouldEndJump()) {
                        val next = stateMachine.next()
                        GameLogger.debug(TAG, "update(): ended jump, set state machine to next=$next")

                        jumpUpdateFacingDelay.reset()
                    }
                }

                PreciousWomanState.WALLSLIDE -> if (shouldEndWallslide()) {
                    val next = stateMachine.next()
                    GameLogger.debug(TAG, "update(): ended wall slide, set state machine to next=$next")
                }

                PreciousWomanState.GROUNDSLIDE -> {
                    body.physics.velocity.x = GROUNDSLIDE_VEL_X * ConstVals.PPM * facing.value

                    if (shouldEndGroundslide()) {
                        val next = stateMachine.next()
                        GameLogger.debug(TAG, "update(): ended ground slide, set state machine to next=$next")
                    }
                }

                PreciousWomanState.THROW_SHIELD_GEMS ->
                    if (shieldGems.isEmpty || shieldGems.keys().all { it.dead }) stateMachine.next()

                else -> {}
            }
        }
    }

    private fun shouldUpdateStateTimer() = when (currentState) {
        PreciousWomanState.INIT -> body.isSensing(BodySense.FEET_ON_GROUND)
        PreciousWomanState.STAND -> !laughing && !throwing
        else -> true
    }

    private fun spawnShieldGems() {
        GameLogger.debug(TAG, "spawnShieldGems()")

        for (i in 0 until SHIELD_GEMS_ANGLES.size) {
            val spawn = body.getCenter()

            val angle = SHIELD_GEMS_ANGLES[i]
            val target = OrbitUtils.calculateOrbitalPosition(
                angle, SHIELD_GEM_START_OFFSET * ConstVals.PPM, spawn,
                GameObjectPools.fetch(Vector2::class)
            )

            val color = GEM_COLORS[i % GEM_COLORS.size]

            val speed = GEM_THROW_SPEED * ConstVals.PPM

            val gem = MegaEntityFactory.fetch(PreciousGem::class)!!
            gem.spawn(
                props(
                    ConstKeys.OWNER pairTo this,
                    ConstKeys.COLOR pairTo color,
                    ConstKeys.SPEED pairTo speed,
                    ConstKeys.POSITION pairTo spawn,
                    ConstKeys.CULL_OUT_OF_BOUNDS pairTo false,
                    "${ConstKeys.FIRST}_${ConstKeys.TARGET}" pairTo target,
                )
            )
            gem.putProperty(ConstKeys.SPIN, false)
            gem.onFirstTargetReached = { gem.putProperty(ConstKeys.SPIN, true) }
            gem.runnablesOnDestroy.put(ConstKeys.SPIN) { gem.removeProperty(ConstKeys.SPIN) }

            shieldGems.put(gem, ShieldGemDef(angle, SHIELD_GEM_START_OFFSET * ConstVals.PPM, false))
        }
    }

    private fun spinShieldGems(delta: Float) = shieldGems.forEachIndexed { gem, def, _ ->
        if (!gem.isProperty(ConstKeys.SPIN, true)) return@forEachIndexed

        var (angle, distance, released) = def

        angle += SHIELD_GEM_SPIN_SPEED * 360f * delta
        val center = OrbitUtils.calculateOrbitalPosition(
            angle,
            distance,
            body.getCenter(),
            GameObjectPools.fetch(Vector2::class)
        )
        gem.body.setCenter(center)

        def.set(angle, distance, released)
    }

    private fun throwHomingGems() {
        FacingUtils.setFacingOf(this)

        GameLogger.debug(TAG, "throwGems(): facing=$facing")

        for (i in 0 until GEMS_TO_THROW) {
            val spawn = body.getCenter().add(ConstVals.PPM.toFloat() * facing.value, 0f)

            val offset = GameObjectPools.fetch(Vector2::class)
                .set(THROW_OFFSETS[i].x * facing.value, THROW_OFFSETS[i].y)

            val target = GameObjectPools.fetch(Vector2::class)
                .set(spawn)
                .add(offset.x * ConstVals.PPM, offset.y * ConstVals.PPM)

            val color = GEM_COLORS[i % GEM_COLORS.size]

            val speed = GEM_THROW_SPEED * ConstVals.PPM

            val gem = MegaEntityFactory.fetch(PreciousGem::class)!!
            gem.spawn(
                props(
                    ConstKeys.OWNER pairTo this,
                    ConstKeys.COLOR pairTo color,
                    ConstKeys.SPEED pairTo speed,
                    ConstKeys.POSITION pairTo spawn,
                    "${ConstKeys.FIRST}_${ConstKeys.TARGET}" pairTo target,
                )
            )
            gem.putProperty(ConstKeys.SPIN, false)
            gem.secondTargetSupplier = { megaman.body.getCenter() }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.physics.receiveFrictionY = false
        body.physics.defaultFrictionOnSelf.x = DEFAULT_FRICTION_X
        body.setSize(BODY_WIDTH * ConstVals.PPM, BODY_HEIGHT * ConstVals.PPM)
        body.physics.velocityClamp.set(VEL_CLAMP_X, VEL_CLAMP_Y).scl(ConstVals.PPM.toFloat())

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.5f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -BODY_HEIGHT * ConstVals.PPM / 2f
        body.addFixture(feetFixture)
        debugShapes.add { feetFixture }

        val headFixture =
            Fixture(body, FixtureType.HEAD, GameRectangle().setSize(0.5f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        headFixture.offsetFromBodyAttachment.y = BODY_HEIGHT * ConstVals.PPM / 2f
        body.addFixture(headFixture)
        debugShapes.add { headFixture }

        val leftFixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.2f * ConstVals.PPM, ConstVals.PPM.toFloat()))
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        leftFixture.offsetFromBodyAttachment.x = -BODY_WIDTH * ConstVals.PPM / 2f
        body.addFixture(leftFixture)
        leftFixture.drawingColor = Color.YELLOW
        debugShapes.add { leftFixture }

        val rightFixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.2f * ConstVals.PPM, ConstVals.PPM.toFloat()))
        rightFixture.offsetFromBodyAttachment.x = BODY_WIDTH * ConstVals.PPM / 2f
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        body.addFixture(rightFixture)
        rightFixture.drawingColor = Color.YELLOW
        debugShapes.add { rightFixture }

        val shieldFixture = Fixture(body, FixtureType.SHIELD, GameRectangle().setSize(ConstVals.PPM.toFloat()))
        body.addFixture(shieldFixture)
        debugShapes.add debugShapes@{
            shieldFixture.drawingColor = if (shieldFixture.isActive()) Color.BLUE else Color.GRAY
            return@debugShapes shieldFixture
        }

        body.preProcess.put(ConstKeys.DEFAULT) {
            HeadUtils.stopJumpingIfHitHead(body)

            val gravity = if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAVITY else GRAVITY
            body.physics.gravity.y = gravity * ConstVals.PPM

            body.physics.defaultFrictionOnSelf.y = when (currentState) {
                PreciousWomanState.INIT, PreciousWomanState.AIRPUNCH -> 0f
                PreciousWomanState.WALLSLIDE -> WALLSLIDE_FRICTION_Y
                else -> DEFAULT_FRICTION_Y
            }

            val shieldActive = currentState == PreciousWomanState.AIRPUNCH
            shieldFixture.setActive(shieldActive)
            shieldFixture.offsetFromBodyAttachment.x = ConstVals.PPM.toFloat() * facing.value
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { sprite -> sprite.setSize(SPRITE_SIZE * ConstVals.PPM) })
        .updatable { _, sprite ->
            val position = Position.BOTTOM_CENTER
            sprite.setPosition(body.getPositionPoint(position), position)

            val flipX = when (currentState) {
                PreciousWomanState.WALLSLIDE -> body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT)
                else -> isFacing(Facing.LEFT)
            }
            sprite.setFlip(flipX, false)

            val offsetX = when (currentState) {
                PreciousWomanState.WALLSLIDE -> 0.25f * facing.value
                else -> 0f
            }
            sprite.translate(offsetX * ConstVals.PPM, 0f)

            sprite.hidden = damageBlink || game.isProperty(ConstKeys.ROOM_TRANSITION, true)
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier {
                    when {
                        defeated -> "defeated"

                        stunned -> "damaged"

                        !ready || betweenReadyAndEndBossSpawnEvent -> when {
                            !body.isSensing(BodySense.FEET_ON_GROUND) -> "jump"
                            else -> "wink"
                        }

                        else -> when (currentState) {
                            PreciousWomanState.STAND -> when {
                                laughing -> "stand${if (laughTimer.time <= LAUGH_1_DUR) "_laugh1" else "_laugh2"}"
                                throwing -> "stand_throw"
                                else -> "stand"
                            }

                            PreciousWomanState.JUMP -> when {
                                throwing -> "jump_throw"
                                else -> "jump"
                            }

                            PreciousWomanState.SPAWN_SHIELD_GEMS, PreciousWomanState.THROW_SHIELD_GEMS -> when {
                                body.isSensing(BodySense.FEET_ON_GROUND) -> "stand_throw"
                                else -> "jump_throw"
                            }

                            PreciousWomanState.RUN -> "run"

                            PreciousWomanState.WALLSLIDE -> "wallslide"

                            PreciousWomanState.GROUNDSLIDE -> "groundslide"

                            PreciousWomanState.AIRPUNCH -> when {
                                stateTimers[PreciousWomanState.AIRPUNCH].time <= AIRPUNCH_DELAY -> "airpunch1"
                                else -> "airpunch2"
                            }

                            else -> null
                        }
                    }
                }
                .applyToAnimations { animations ->
                    animDefs.forEach { entry ->
                        val key = entry.key
                        val def = entry.value
                        try {
                            val animation = Animation(regions[key], def.rows, def.cols, def.durations, def.loop)
                            animations.put(key, animation)
                        } catch (e: Exception) {
                            throw IllegalStateException("Failed to create animation for region $key and def $def", e)
                        }
                    }
                }
                .build()
        )
        .build()

    private fun buildStateMachine() = StateMachineBuilder<PreciousWomanState>()
        .states { states -> PreciousWomanState.entries.forEach { state -> states.put(state.name, state) } }
        .initialState(PreciousWomanState.INIT.name)
        .setTriggerChangeWhenSameElement(true)
        .setOnChangeState(this::onChangeState)
        // init
        .transition(PreciousWomanState.INIT.name, PreciousWomanState.STAND.name) { true }
        // stand
        .transition(PreciousWomanState.STAND.name, PreciousWomanState.STAND.name) { stunned }
        .transition(PreciousWomanState.STAND.name, PreciousWomanState.JUMP.name) { shouldJump() }
        .transition(PreciousWomanState.STAND.name, PreciousWomanState.GROUNDSLIDE.name) { shouldGroundslide() }
        .transition(PreciousWomanState.STAND.name, PreciousWomanState.RUN.name) { shouldStartRunning() }
        .transition(
            PreciousWomanState.STAND.name,
            PreciousWomanState.THROW_SHIELD_GEMS.name
        ) { shouldThrowShieldGems() }
        .transition(
            PreciousWomanState.STAND.name,
            PreciousWomanState.SPAWN_SHIELD_GEMS.name
        ) { shouldSpawnShieldGems() }
        .transition(
            PreciousWomanState.STAND.name,
            PreciousWomanState.AIRPUNCH.name
        ) { shouldAirPunch() }
        .transition(PreciousWomanState.STAND.name, PreciousWomanState.JUMP.name) {
            getRandom(0f, 100f) <= JUMP_CHANCE_SECOND_CHECK
        }
        .transition(PreciousWomanState.STAND.name, PreciousWomanState.RUN.name) { true }
        // run
        .transition(PreciousWomanState.RUN.name, PreciousWomanState.STAND.name) { stunned }
        .transition(PreciousWomanState.RUN.name, PreciousWomanState.JUMP.name) { true }
        // ground slide
        .transition(PreciousWomanState.GROUNDSLIDE.name, PreciousWomanState.STAND.name) { stunned }
        .transition(PreciousWomanState.GROUNDSLIDE.name, PreciousWomanState.JUMP.name) { true }
        // jump
        .transition(PreciousWomanState.JUMP.name, PreciousWomanState.STAND.name) { stunned }
        .transition(PreciousWomanState.JUMP.name, PreciousWomanState.WALLSLIDE.name) { shouldStartWallSliding() }
        .transition(PreciousWomanState.JUMP.name, PreciousWomanState.AIRPUNCH.name) { shouldAirPunch() }
        .transition(PreciousWomanState.JUMP.name, PreciousWomanState.STAND.name) { shouldStand() }
        .transition(PreciousWomanState.JUMP.name, PreciousWomanState.SPAWN_SHIELD_GEMS.name) { shouldSpawnShieldGems() }
        // wallslide
        .transition(PreciousWomanState.WALLSLIDE.name, PreciousWomanState.STAND.name) { stunned }
        .transition(PreciousWomanState.WALLSLIDE.name, PreciousWomanState.JUMP.name) {
            !body.isSensing(BodySense.FEET_ON_GROUND)
        }
        .transition(PreciousWomanState.WALLSLIDE.name, PreciousWomanState.STAND.name) { true }
        // air punch
        .transition(PreciousWomanState.AIRPUNCH.name, PreciousWomanState.STAND.name) { stunned }
        .transition(PreciousWomanState.AIRPUNCH.name, PreciousWomanState.JUMP.name) {
            !body.isSensing(BodySense.FEET_ON_GROUND)
        }
        .transition(PreciousWomanState.AIRPUNCH.name, PreciousWomanState.STAND.name) { true }
        // spawn shield gems
        .transition(
            PreciousWomanState.SPAWN_SHIELD_GEMS.name,
            PreciousWomanState.STAND.name
        ) { stunned || shouldStand() }
        .transition(PreciousWomanState.SPAWN_SHIELD_GEMS.name, PreciousWomanState.JUMP.name) { true }
        // throw shield gems
        .transition(
            PreciousWomanState.THROW_SHIELD_GEMS.name,
            PreciousWomanState.STAND.name
        ) { stunned || shouldStand() }
        .transition(PreciousWomanState.THROW_SHIELD_GEMS.name, PreciousWomanState.JUMP.name) { true }
        // build
        .build()

    private fun onChangeState(current: PreciousWomanState, previous: PreciousWomanState) {
        GameLogger.debug(TAG, "onChangeState(): current=$current, previous=$previous")

        stateTimers[current]?.reset()

        when (current) {
            PreciousWomanState.STAND, PreciousWomanState.JUMP -> {
                if (firstUpdate && current == PreciousWomanState.JUMP) firstUpdate = false

                FacingUtils.setFacingOf(this)

                statesSinceLastThrow++

                GameLogger.debug(TAG, "onChangeState(): statesSinceLastThrow=$statesSinceLastThrow")

                if (shieldGems.isEmpty && shouldThrowGems()) {
                    GameLogger.debug(TAG, "onChangeState(): should throw gems")

                    throwingTimer.reset()

                    throwMinCooldown.reset()
                    statesSinceLastThrow = 0
                }
            }

            PreciousWomanState.AIRPUNCH -> {
                GameLogger.debug(TAG, "onChangeState(): set vel y to zero when air punching")
                body.physics.velocity.y = 0f
                body.physics.gravityOn = false
            }

            PreciousWomanState.SPAWN_SHIELD_GEMS -> {
                GameLogger.debug(TAG, "onChangeState(): spawn shield gems")

                FacingUtils.setFacingOf(this)

                spawnShieldGems()

                body.physics.gravityOn = false
                body.physics.velocity.setZero()

                throwMinCooldown.reset()
                statesSinceLastThrow = 0

                spawnShieldsDelay.reset()
                spawnShieldsChance = SPAWN_SHIELD_START_CHANCE
            }

            PreciousWomanState.THROW_SHIELD_GEMS -> {
                GameLogger.debug(TAG, "onChangeState(): setting all shield gems to be released")
                FacingUtils.setFacingOf(this)
                throwShieldGems()
            }

            else -> GameLogger.debug(TAG, "onChangeState(): no action when current=$current")
        }

        when (previous) {
            PreciousWomanState.JUMP -> {
                GameLogger.debug(TAG, "onChangeState(): turn on friction x")
                body.physics.applyFrictionX = true
            }

            PreciousWomanState.SPAWN_SHIELD_GEMS -> {
                GameLogger.debug(TAG, "onChangeState(): turn on gravity")
                body.physics.gravityOn = true
            }

            PreciousWomanState.AIRPUNCH -> {
                GameLogger.debug(TAG, "onChangeState(): turn gravity back on, reset air punch cooldown")
                airpunchCooldown.reset()
                body.physics.gravityOn = true
            }

            else -> GameLogger.debug(TAG, "onChangeState(): no action when previous=$previous")
        }

        if (current == PreciousWomanState.JUMP && previous.equalsAny(
                PreciousWomanState.STAND, PreciousWomanState.GROUNDSLIDE, PreciousWomanState.WALLSLIDE
            )
        ) {
            body.physics.applyFrictionX = false

            jump(megaman.body.getCenter())

            if (previous == PreciousWomanState.WALLSLIDE) {
                var impulseX = WALL_JUMP_IMPULSE_X * ConstVals.PPM
                if (body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT)) impulseX *= -1f
                body.physics.velocity.x = impulseX

                GameLogger.debug(
                    TAG,
                    "onChangeState(): apply x impulse when jumping from wallslide: impulseX=$impulseX"
                )
            }

            GameLogger.debug(TAG, "onChangeState(): jump vel=${body.physics.velocity}")
        }
    }

    private fun shouldGroundslide() = getRandom(0f, 100f) <= GROUNDSLIDE_CHANCE && !FacingUtils.isFacingBlock(this)

    private fun shouldStartRunning() = getRandom(0f, 100f) <= RUN_CHANCE && !FacingUtils.isFacingBlock(this)

    private fun shouldSpawnShieldGems() =
        shieldGems.isEmpty && spawnShieldsDelay.isFinished() && getRandom(0f, 100f) <= spawnShieldsChance

    private fun shouldThrowShieldGems() = !shieldGems.isEmpty && shouldThrowGems()

    private fun throwShieldGems() {
        shieldGems.forEach { entry ->
            val gem = entry.key
            gem.shieldShatter = true

            val def = entry.value
            def.released = true
        }

        val position = body.getCenter()

        val trajectory = megaman.body.getCenter()
            .sub(body.getCenter())
            .nor()
            .scl(SHIELD_GEM_CLUSTER_SPEED * ConstVals.PPM)

        val cluster = MegaEntityFactory.fetch(PreciousGemCluster::class)!!
        cluster.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                PreciousGem.TAG pairTo shieldGems,
                ConstKeys.ORIGIN pairTo roomCenter,
                ConstKeys.POSITION pairTo position,
                ConstKeys.TRAJECTORY pairTo trajectory,
            )
        )

        // re-initialize to detach this class's ref from the ref passed into the `cluster`
        shieldGems = OrderedMap()

        throwShieldGemsTimer.reset()

        throwMinCooldown.reset()
        statesSinceLastThrow = 0

        spawnShieldsDelay.reset()
        spawnShieldsChance = SPAWN_SHIELD_START_CHANCE
    }

    private fun shouldThrowGems() = throwMinCooldown.isFinished() && statesSinceLastThrow >= STATES_BETWEEN_THROW

    // Precious Woman's very first attack when spawned should always be to jump towards Megaman
    private fun shouldJump() =
        firstUpdate || (getRandom(0f, 100f) <= JUMP_CHANCE_FIRST_CHECK && !FacingUtils.isFacingBlock(this))

    private fun jump(target: Vector2): Vector2 {
        GameLogger.debug(TAG, "jump(): target=$target")

        val impulse = MegaUtilMethods.calculateJumpImpulse(body.getCenter(), target, JUMP_IMPULSE_Y * ConstVals.PPM)
        impulse.x = impulse.x.coerceIn(-JUMP_MAX_IMPULSE_X * ConstVals.PPM, JUMP_MAX_IMPULSE_X * ConstVals.PPM)
        body.physics.velocity.set(impulse.x, impulse.y)

        return impulse
    }

    private fun shouldEndJump() =
        shouldStand() || shouldStartWallSliding() || shouldAirPunch() || shouldSpawnShieldGems()

    private fun shouldAirPunch() = airpunchCooldown.isFinished() && getRandom(0f, 100f) <= AIRPUNCH_CHANCE &&
        megaman.body.getY() <= body.getMaxY() && megaman.body.getMaxY() >= body.getY()

    private fun shouldEndAirPunch() = body.getBounds().overlaps(megaman.body.getBounds()) ||
        ((isFacing(Facing.LEFT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT)) ||
            (isFacing(Facing.RIGHT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT)))

    private fun shouldStand() = body.isSensing(BodySense.FEET_ON_GROUND) && body.physics.velocity.y <= 0f

    private fun shouldStartWallSliding() = body.physics.velocity.y < 0f &&
        !body.isSensing(BodySense.FEET_ON_GROUND) && FacingUtils.isFacingBlock(this)

    private fun shouldEndGroundslide() =
        body.getBounds().overlaps(megaman.body.getBounds()) || FacingUtils.isFacingBlock(this)

    private fun shouldEndWallslide() = body.isSensing(BodySense.FEET_ON_GROUND) ||
        !body.isSensingAny(BodySense.SIDE_TOUCHING_BLOCK_LEFT, BodySense.SIDE_TOUCHING_BLOCK_RIGHT)

    override fun getTag() = TAG
}

