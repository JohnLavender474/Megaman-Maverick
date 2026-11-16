package com.megaman.maverick.game.entities.protoman

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.animations.IAnimation
import com.maverick.game.engine.animations.IAnimator
import com.maverick.game.engine.behaviors.BehaviorsComponent
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.extensions.objectMapOf
import com.maverick.game.engine.common.extensions.objectSetOf
import com.maverick.game.engine.common.extensions.orderedMapOf
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.components.require
import com.maverick.game.engine.controller.ControllerComponent
import com.maverick.game.engine.controller.buttons.ButtonActuator
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.drawables.sprites.setPosition
import com.maverick.game.engine.drawables.sprites.setSize
import com.maverick.game.engine.drawables.sprites.splitAndFlatten
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.entities.contracts.ISpritesEntity
import com.maverick.game.engine.entities.input
import com.maverick.game.engine.entities.isBehaviorActive
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.bosses.GutsTank
import com.megaman.maverick.game.entities.contracts.IScalableGravityEntity
import com.megaman.maverick.game.entities.decorations.WhiteBurst
import com.megaman.maverick.game.entities.enemies.SpringHead
import com.megaman.maverick.game.entities.megaman.constants.MegaChargeStatus
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.movement.RunParams
import com.megaman.maverick.game.entities.movement.maxRunSpeed
import com.megaman.maverick.game.entities.state.*
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.players.*
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.utils.misc.FacingUtils
import com.megaman.maverick.game.world.body.*

class Protoman(services: MegaServices) : MegaPlayer(services), ISpritesEntity, IScalableGravityEntity {

    companion object {
        const val TAG = "Protoman"

        private val SHOOT_KEYS = objectMapOf(
            "stand" pairTo "stand_shoot",
            "run_trans" pairTo "run_trans_shoot",
            "run" pairTo "run_shoot",
            "jump" pairTo "jump_shoot",
            "groundslide" pairTo "groundslide_shoot",
            "climb" pairTo "climb_still_shoot",
            "climb_still" pairTo "climb_still_shoot",
            "climb_finish" pairTo "climb_still_shoot"
        )
        private val BASE_POSES = objectMapOf(
            "stand_shoot" pairTo "stand",
            "run_trans_shoot" pairTo "run_trans",
            "run_shoot" pairTo "run",
            "jump_shoot" pairTo "jump",
            "groundslide_shoot" pairTo "groundslide",
            "climb_still_shoot" pairTo "climb_still",
            "crouch_shoot" pairTo "stand",
            "guard_crouch_start" pairTo "stand",
            "guard_crouch" pairTo "stand",
            "guard_crouch_end" pairTo "stand",
            "guard_jump_start" pairTo "jump",
            "guard_jump" pairTo "jump",
            "guard_jump_end" pairTo "jump"
        )
        private val GUARD_PHASES = objectMapOf(
            "guard_crouch_start" pairTo "pull_out",
            "guard_jump_start" pairTo "pull_out",
            "guard_crouch" pairTo "up",
            "guard_jump" pairTo "up",
            "guard_crouch_end" pairTo "put_away",
            "guard_jump_end" pairTo "put_away"
        )
    }

    // TODO: Protoman does not turn the camera when the world gravity changes, as Megaman does; the camera turn moves
    //  to WorldGravity with item 058
    override var direction: Direction
        get() = body.direction
        set(value) {
            body.direction = value
            playerGravity.applyDirection(value)
        }

    override var gravityScalar = 1f

    override val cameraFocusHeight = ProtomanValues.BODY_HEIGHT

    internal val playerGravity = PlayerGravityComponent(
        PlayerGravityValues(
            ground = ProtomanValues.GROUND_GRAVITY,
            jump = ProtomanValues.JUMP_GRAVITY,
            fall = ProtomanValues.FALL_GRAVITY,
            ice = ProtomanValues.ICE_GRAVITY,
            water = ProtomanValues.WATER_GRAVITY,
            waterIce = ProtomanValues.WATER_ICE_GRAVITY
        )
    )

    internal val runParams = RunParams(
        impulse = ProtomanValues.RUN_IMPULSE,
        iceImpulse = ProtomanValues.ICE_RUN_IMPULSE,
        ramp1Time = ProtomanValues.RUN_1_TIME,
        ramp1Scalar = ProtomanValues.RUN_1_SCALAR,
        ramp2Time = ProtomanValues.RUN_2_TIME,
        ramp2Scalar = ProtomanValues.RUN_2_SCALAR,
        ramp3Scalar = ProtomanValues.RUN_3_SCALAR,
        maxSpeed = { maxRunSpeed(body, ProtomanValues.RUN_MAX_SPEED, ProtomanValues.WATER_RUN_MAX_SPEED_SCALAR) },
        movementScalar = { 1f }
    )

    override val character = PlayerCharacter.PROTOMAN

    override val shooting: Boolean
        get() = buster.shooting

    internal val buster = PlayerBuster(this, PROTOMAN_BUSTER)
    internal val sabre: PlayerSabre = PlayerSabre(this, protomanSabreParams(this))

    private var canMakeLandSound = false

    private val guardPullOut = Timer(ProtomanValues.GUARD_CROUCH_PULL_OUT_TIME)
    private val guardPutAway = Timer(ProtomanValues.GUARD_CROUCH_PUT_AWAY_TIME)
    private val slideToCrouchDelay =
        Timer(ProtomanValues.GROUND_SLIDE_TO_CROUCH_DELAY).setToEnd(allowJustFinished = false)
    private var guardRaised = false
    private var guardUp = false
    private var shieldUp = false
    private var guardShown = false
    private var crouchShot = false

    internal var puttingAwayGuard = false
        private set

    internal val guarding: Boolean
        get() = shieldUp && guardWanted()

    private val sliding: Boolean
        get() = isBehaviorActive(BehaviorType.GROUND_SLIDING)

    private val climbing: Boolean
        get() = isBehaviorActive(BehaviorType.CLIMBING)

    private val crouchShooting: Boolean
        get() = crouchShot && buster.shooting && !sliding && !climbing

    private val crouched: Boolean
        get() = (guardRaised && guardPlanted()) || crouchShooting

    private val pullingOutGuard: Boolean
        get() = guardRaised && guardWanted() && !guarding

    private val deflects = ShieldDeflects { position ->
        MegaEntityFactory.fetch(WhiteBurst::class)!!.spawn(
            props(
                ConstKeys.POSITION pairTo position,
                ConstKeys.DURATION pairTo ProtomanValues.DEFLECT_BURST_TIME,
                ConstKeys.COLLIDE pairTo false
            )
        )
    }

    override fun init(vararg params: Any) {
        super.init()
        addComponent(playerGravity)
    }

    override fun onPlayerSpawn(spawnProps: Properties) {
        gravityScalar = 1f
        canMakeLandSound = false
        crouchShot = false
        slideToCrouchDelay.setToEnd(allowJustFinished = false)
        dropGuard()
        deflects.clear()
        buster.reset()
        sabre.reset()
    }

    override fun onPlayerDestroy() {
        sabre.reset()
        deflects.clear()
    }

    override fun onPlayerEvent(event: Event) {
        when (event.key) {
            EventType.BEGIN_ROOM_TRANS, EventType.CONTINUE_ROOM_TRANS, EventType.GATE_INIT_OPENING -> endAttacks()
        }
    }

    override fun afterPlayerDamage(damager: IDamagerEntity) = endAttacks()

    private fun endAttacks() {
        buster.endPose()
        crouchShot = false
        sabre.end()
        dropGuard()
    }

    private fun crouchBlocked() = !inPlay || damaged || stunned || frozen || teleporting || sliding ||
        !slideToCrouchDelay.isFinished() || climbing || services.levelState.roomTransition

    private fun guardBroken() = crouchBlocked() || sabre.active || buster.shooting

    private fun guardWanted() = input.isPressed(MegaControllerButton.DOWN) && !guardBroken()

    private fun crouchAttackWanted() =
        bodyOverGround && input.isPressed(MegaControllerButton.DOWN) && !crouchBlocked()

    private fun crouchAttack() {
        if (!playerCanAttack || !buster.shoot(MegaChargeStatus.NOT_CHARGED, crouched = true)) return
        crouchShot = true
        running = false
        setSideSpeed(0f)
    }

    private fun slideAttack() {
        when {
            !input.isPressed(MegaControllerButton.UP) -> if (playerCanAttack) buster.shoot(
                MegaChargeStatus.NOT_CHARGED, sliding = true
            )
            body.isSensing(BodySense.HEAD_TOUCHING_BLOCK) || !sabre.canStart() -> Unit
            else -> sabre.start()
        }
    }

    private fun standingShot() {
        if (buster.shoot(MegaChargeStatus.NOT_CHARGED)) crouchShot = false
    }

    private fun guardPlanted() = guardWanted() && bodyOverGround

    private fun dropGuard() {
        lowerGuard()
        puttingAwayGuard = false
    }

    private fun lowerGuard() {
        guardRaised = false
        guardUp = false
        shieldUp = false
        guardShown = false
        guardPullOut.reset()
    }

    private fun putAwayCut() = input.isPressed(MegaControllerButton.LEFT) ||
        input.isPressed(MegaControllerButton.RIGHT) || input.isJustPressed(MegaControllerButton.A) ||
        input.isJustPressed(MegaControllerButton.B) || guardBroken()

    internal fun updateGuard(delta: Float) {
        if (sliding) slideToCrouchDelay.reset() else slideToCrouchDelay.update(delta)
        if (!buster.shooting || !bodyOverGround || sliding || climbing) crouchShot = false
        if (guardWanted()) {
            puttingAwayGuard = false
            raiseGuard(delta)
            guardShown = shieldUp
            return
        }
        if (guardShown && !guardBroken()) {
            puttingAwayGuard = true
            val putAwayTime = if (bodyOverGround) ProtomanValues.GUARD_CROUCH_PUT_AWAY_TIME
            else ProtomanValues.GUARD_JUMP_PUT_AWAY_TIME
            guardPutAway.resetDuration(putAwayTime)
        }
        lowerGuard()
        if (!puttingAwayGuard) return
        if (putAwayCut() || guardPutAway.isFinished()) {
            puttingAwayGuard = false
            return
        }
        guardPutAway.update(delta)
    }

    private fun raiseGuard(delta: Float) {
        if (!guardRaised) {
            guardRaised = true
            val pullOutTime = if (bodyOverGround) ProtomanValues.GUARD_CROUCH_PULL_OUT_TIME
            else ProtomanValues.GUARD_JUMP_PULL_OUT_TIME
            guardPullOut.resetDuration(pullOutTime)
            if (bodyOverGround) {
                running = false
                setSideSpeed(0f)
            }
        }
        guardPullOut.update(delta)
        guardUp = guardPullOut.isFinished()
    }

    internal fun updateDeflects() = deflects.update()

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        // TODO: share the updatables list with Megaman and X when one frame order fits all classes (item 060)
        updatablesComponent.add { delta ->
            killPlayerIfOutsideLevel()
            require(StunComponent::class).update(delta, body.isSensing(BodySense.FEET_ON_GROUND))
            if (stunned || damaged) endAttacks()
            damageRecovery.update(delta, damaged, damageTimer.isJustFinished())
            require(RoomTransitionCarryComponent::class).update(delta)
            updatePostActionMomentum(delta, running)
            require(FreezeComponent::class).update(delta)
            buster.update(delta)
            sabre.update(delta)
            updateGuard(delta)
            updateDeflects()
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = playerBody(
            this,
            PlayerBodyParams(
                width = ProtomanValues.BODY_WIDTH,
                height = ProtomanValues.BODY_HEIGHT,
                crouchHeight = ProtomanValues.SLIDE_HEIGHT,
                gravity = playerGravity,
                waterGravityScalar = ProtomanValues.WATER_GRAVITY_SCALAR,
                clampX = ProtomanValues.CLAMP_X,
                clampY = ProtomanValues.CLAMP_Y,
                onBounce = {},
                onFeetBounce = {},
                // TODO: share the land sound with Megaman when a third player class needs it (item 060)
                onFeetBlockBegin = { makesSound ->
                    if (makesSound && canMakeLandSound) {
                        requestToPlaySound(SoundAsset.MEGAMAN_LAND_SOUND, loop = false)
                        canMakeLandSound = false
                    }
                },
                onFeetBlockContinue = {},
                onFeetBlockEnd = { if (!body.hasAnyFeetBlock()) canMakeLandSound = true },
                onHeadHitBlock = {},
                // TODO: share the GutsTank side-stick rule with Megaman when a third player class needs it (item 060)
                sideSticksTo = { blockFixture ->
                    val owner = blockFixture.getEntity().getProperty(ConstKeys.OWNER) as IGameEntity?
                    owner !is GutsTank
                },
                // TODO: no swim; A in mid-water does nothing; decided in 060g
                onWater = {},
                onPreStep = {}
            )
        )
        body.addFixture(defineShieldFixture(body))
        defineCrouchBox(body)
        return BodyComponentCreator.create(this, body)
    }

    private fun defineCrouchBox(body: Body) {
        val bodyFixture = body.getProperty(ConstKeys.BODY, Fixture::class)!!
        val damageable = body.getProperty(ConstKeys.DAMAGEABLE, Fixture::class)!!
        body.preProcess.put(ConstKeys.DAMAGEABLE) {
            bodyFixture.offsetFromBodyAttachment.setZero()
            if (!crouched) return@put
            val height = ProtomanValues.GUARD_CROUCH_HEIGHT * ConstVals.PPM
            (bodyFixture.rawShape as GameRectangle).setHeight(height)
            bodyFixture.offsetFromBodyAttachment.y = (height - ProtomanValues.BODY_HEIGHT * ConstVals.PPM) / 2f
            val damageBox = damageable.rawShape as GameRectangle
            if (direction.isVertical()) damageBox.setHeight(height) else damageBox.setWidth(height)
            val feet = DirectionPositionMapper.getInvertedPosition(direction)
            damageBox.positionOnPoint(body.getBounds().getPositionPoint(feet), feet)
        }
    }

    // TODO: a fast shot that reaches the shield and the body in the same frame also hurts him and shows a spark (item 060e)
    private fun defineShieldFixture(body: Body): Fixture {
        val shield = Fixture(body, FixtureType.SHIELD, GameRectangle())
        shield.setReceiver(Receivers.HIT_BY_PROJECTILE) { projectile ->
            if (projectile.owner != this) deflects.record(projectile)
        }
        body.preProcess.put(ConstKeys.SHIELD) {
            shieldUp = guardUp && guardWanted()
            shield.setActive(shieldUp)
            val box = if (bodyOverGround) ProtomanValues.GUARD_CROUCH_SHIELD_BOX
            else ProtomanValues.GUARD_JUMP_SHIELD_BOX
            val artPx = ProtomanValues.SPRITE_SIZE * ConstVals.PPM / ProtomanValues.ART_FRAME_PX
            (shield.rawShape as GameRectangle).setSize(box.width * artPx, box.height * artPx)
            shield.offsetFromBodyAttachment.set(
                (box.centerX - ProtomanValues.ART_FRAME_PX / 2f) * artPx * facing.value,
                (ProtomanValues.ART_FRAME_PX - box.centerY) * artPx + spriteOffsetY() -
                    ProtomanValues.BODY_HEIGHT * ConstVals.PPM / 2f
            )
        }
        return shield
    }

    override fun defineMomentumComponent() = PostActionMomentumComponent(
        ProtomanValues.POST_ACTION_MOMENTUM_DUR,
        ProtomanValues.POST_ACTION_BURST_RESISTANCE,
        maxSpeed = { runParams.maxSpeed() },
        overGround = { bodyOverGround },
        facingBlock = { FacingUtils.isFacingBlock(this) },
        groundSliding = { sliding }
    )

    // TODO: Megaman's knockback and stun values until Protoman's own damage reaction (item 060e)
    override fun defineKnockbackComponent() = KnockbackComponent(
        MegamanValues.DMG_X * ConstVals.PPM,
        MegamanValues.DMG_Y * ConstVals.PPM,
        objectSetOf(SpringHead::class)
    )

    override fun defineStunComponent() = StunComponent(
        MegamanValues.STUN_DUR,
        onSurface = { body.isSensing(BodySense.FEET_ON_GROUND) },
        onStun = {
            // TODO: the stun bounce assumes that the body points up, as Megaman's does (item 058)
            body.physics.velocity.let {
                it.x = MegamanValues.STUN_IMPULSE_X * ConstVals.PPM * -facing.value
                it.y = MegamanValues.STUN_IMPULSE_Y * ConstVals.PPM
            }
        }
    )

    override fun defineBehaviorsComponent(): BehaviorsComponent {
        val component = BehaviorsComponent()
        component.addBehavior(
            BehaviorType.JUMPING, playerJumpBehavior(
                this,
                JumpParams(
                    canStart = { true },
                    blocked = { false },
                    jumpSpeed = { ProtomanValues.JUMP_VEL * direction.upSign() },
                    wallJumpSpeed = { 0f },
                    wallJumpHorizontal = 0f,
                    wallJumpAllowed = { false },
                    ignoreRelease = { false },
                    startPressed = { input.isJustPressed(MegaControllerButton.A) },
                    downBlocksGroundJump = true,
                    endKeepsFall = false,
                    onJump = { canMakeLandSound = true }
                )
            )
        )
        component.addBehavior(
            BehaviorType.GROUND_SLIDING, playerGroundSlideBehavior(
                this,
                GroundSlideParams(
                    minTime = ProtomanValues.GROUND_SLIDE_MIN_TIME,
                    maxTime = ProtomanValues.GROUND_SLIDE_MAX_TIME,
                    cooldown = ProtomanValues.GROUND_SLIDE_COOLDOWN,
                    speed = {
                        if (body.isSensing(BodySense.IN_WATER)) ProtomanValues.WATER_GROUND_SLIDE_VEL
                        else ProtomanValues.GROUND_SLIDE_VEL
                    },
                    blocked = { sabre.active }
                )
            )
        )
        component.addBehavior(
            BehaviorType.CLIMBING, playerClimbBehavior(
                this,
                ClimbParams(
                    speed = { ProtomanValues.CLIMB_VEL },
                    blocked = { sabre.active },
                    feetGrabAllowed = { true },
                    holdStill = { buster.shooting },
                    leavesOnFloor = false,
                    onStart = { canMakeLandSound = false },
                    onEnd = {}
                )
            )
        )
        return component
    }

    // TODO: SELECT has no action for Proto Man until his special weapons (item 060, branch 060h)
    override fun defineControllerComponent() = ControllerComponent(
        MegaControllerButton.LEFT pairTo playerRunButton(
            this, Facing.LEFT, runParams, blocked = { holdsStill() }, onBlocked = { turnInPlace(it) }
        ),
        MegaControllerButton.RIGHT pairTo playerRunButton(
            this, Facing.RIGHT, runParams, blocked = { holdsStill() }, onBlocked = { turnInPlace(it) }
        ),
        MegaControllerButton.B pairTo ButtonActuator(
            onJustPressed = {
                when {
                    sabre.active -> sabre.chain()
                    climbing -> if (playerCanAttack) buster.shoot(MegaChargeStatus.NOT_CHARGED, climbing = true)
                    sliding -> slideAttack()
                    crouchAttackWanted() -> crouchAttack()
                    guardWanted() -> Unit
                    input.isPressed(MegaControllerButton.UP) -> if (sabre.canStart()) sabre.start()
                    playerCanAttack -> standingShot()
                }
            }
        )
    )

    private fun holdsStill() = sabre.state == SabreState.FLOOR || crouchShooting || guardPlanted()

    private fun turnInPlace(side: Facing) {
        if (guardPlanted() || crouchShooting || sabre.state == SabreState.FLOOR) facing = side
    }

    // TODO: the air offset is Megaman's for a body that points up; the other directions wait for item 058
    internal fun spriteOffsetY() =
        if (direction == Direction.UP && !bodyOverGround) ProtomanValues.AIR_SPRITE_OFFSET_Y * ConstVals.PPM else 0f

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 1))
        val component = SpritesComponent(TAG pairTo sprite)
        component.putPreProcess(TAG) { _, player ->
            player.setSize(ProtomanValues.SPRITE_SIZE * ConstVals.PPM)
            player.setFlip(facing == Facing.LEFT, false)
            player.setOriginCenter()
            player.rotation = direction.rotation
            val position = DirectionPositionMapper.getInvertedPosition(direction)
            player.setPosition(body.getPositionPoint(position), position)
            player.translateY(spriteOffsetY())
            applyHideAndFlash(player)
        }
        return component
    }

    override fun defineAnimationsComponent(): AnimationsComponent {
        // TODO: each new Proto Man frame needs the v2 restyle (Megaman_v3 physique) before it joins the atlas (060)
        val atlas = services.assets.getTextureAtlas(TextureAsset.PROTOMAN.source)
        fun frames(name: String, count: Int): Array<TextureRegion> =
            atlas.findRegion(name)!!.splitAndFlatten(count, 1, Array())

        val standFrames = frames("stand", 2)
        val runFrames = frames("run", 4)
        val jumpFrames = frames("jump", 1)
        val animations = ObjectMap<String, IAnimation>()
        animations.put(
            "stand", Animation(standFrames, gdxArrayOf(ProtomanValues.STAND_OPEN_TIME, ProtomanValues.STAND_FLICK_TIME))
        )
        animations.put("run_trans", Animation(frames("run_trans", 1), ProtomanValues.STEP_TIME, false))
        animations.put("run", Animation(runFrames, ProtomanValues.RUN_FRAME_TIME))
        animations.put("jump", Animation(jumpFrames, 1f))
        animations.put("stand_shoot", Animation(frames("stand_shoot", 1), 1f))
        animations.put("run_trans_shoot", Animation(frames("run_trans_shoot", 1), ProtomanValues.STEP_TIME, false))
        animations.put("run_shoot", Animation(frames("run_shoot", 4), ProtomanValues.RUN_FRAME_TIME))
        animations.put("jump_shoot", Animation(frames("jump_shoot", 1), 1f))
        animations.put("crouch_shoot", Animation(frames("crouch_shoot", 1), 1f))
        animations.put("groundslide", Animation(frames("groundslide", 2), ProtomanValues.GROUND_SLIDE_FRAME_TIME))
        animations.put("groundslide_shoot", Animation(frames("groundslide_shoot", 1), 1f))
        animations.put("climb", Animation(frames("climb", 2), ProtomanValues.CLIMB_FRAME_TIME))
        animations.put("climb_still", Animation(frames("climb_still", 1), 1f))
        animations.put("climb_still_shoot", Animation(frames("climb_still_shoot", 1), 1f))
        animations.put("climb_finish", Animation(frames("climb_finish", 1), 1f))
        animations.put(
            "guard_crouch",
            Animation(frames("guard_crouch", 2), gdxArrayOf(ProtomanValues.STAND_OPEN_TIME, ProtomanValues.STAND_FLICK_TIME))
        )
        animations.put("guard_jump", Animation(frames("guard_jump", 1), 1f))
        fun turn(name: String, count: Int) = Animation(frames(name, count), ProtomanValues.GUARD_TURN_FRAME_TIME, loop = false)
        animations.put("guard_crouch_start", turn("guard_crouch_start", ProtomanValues.GUARD_CROUCH_TURN_FRAMES))
        animations.put("guard_crouch_end", turn("guard_crouch_end", ProtomanValues.GUARD_CROUCH_TURN_FRAMES))
        animations.put("guard_jump_start", turn("guard_jump_start", ProtomanValues.GUARD_JUMP_TURN_FRAMES))
        animations.put("guard_jump_end", turn("guard_jump_end", ProtomanValues.GUARD_JUMP_TURN_FRAMES))
        ProtomanSabreMoves.ALL.forEach { animations.put(it.bodyRegion, Animation(frames(it.bodyRegion, it.stripFrames), 1f)) }
        // TODO: the stand frame shows while damaged or stunned until Protoman's damage and stun art (item 060e)
        animations.put("damaged", Animation(standFrames[0]))
        val animator = Animator(
            keySupplier = { current -> animationKey(current, animations) },
            animations = animations,
            onChangeKey = { _, currentKey, nextKey -> carryFrameTime(currentKey, nextKey, animations) },
            shouldUpdateAnimationPredicate = { key, animation, _ ->
                val move = sabre.move
                if (move != null && key == move.bodyRegion) {
                    animation.setIndex(sabre.frame)
                    false
                } else true
            }
        )
        return AnimationsComponent(orderedMapOf<Any, IAnimator>(TAG pairTo animator), sprites)
    }

    private fun isSabreKey(key: String) = ProtomanSabreMoves.ALL.any { it.bodyRegion == key }

    internal fun carryFrameTime(currentKey: String?, nextKey: String?, animations: ObjectMap<String, IAnimation>) {
        if (currentKey == null || nextKey == null || isSabreKey(currentKey) || isSabreKey(nextKey)) return
        val currentPhase = GUARD_PHASES[currentKey]
        val nextPhase = GUARD_PHASES[nextKey]
        val carries = if (currentPhase != null || nextPhase != null) currentPhase == nextPhase
        else basePose(currentKey) == basePose(nextKey)
        if (carries) animations[currentKey]?.getCurrentTime()?.let { animations[nextKey]?.setCurrentTime(it) }
    }

    private fun basePose(key: String) = if (isSabreKey(key)) "stand" else BASE_POSES[key] ?: key

    internal fun animationKey(current: String?, animations: ObjectMap<String, IAnimation>): String? {
        val pose = poseKey(current?.let { basePose(it) }, current?.let { animations[it] }) ?: return null
        return when {
            pose == "damaged" -> pose
            sabre.active -> sabre.move!!.bodyRegion
            crouchShooting -> "crouch_shoot"
            buster.shooting -> SHOOT_KEYS[pose] ?: throw IllegalStateException("No shoot key for the pose $pose")
            pullingOutGuard -> if (pose == "jump") "guard_jump_start" else "guard_crouch_start"
            guarding -> if (pose == "jump") "guard_jump" else "guard_crouch"
            puttingAwayGuard -> if (pose == "jump") "guard_jump_end" else "guard_crouch_end"
            else -> pose
        }
    }

    private fun poseKey(current: String?, playing: IAnimation?): String? = when {
        roomTransitionPaused -> null
        current != null && services.levelState.roomTransition ->
            if (current == "stand" || current == "run_trans") "run" else current
        // TODO: Megaman's blue beam-in plays for Protoman (screens/levels/respawn/BeamDown.kt); his own beam color
        //  and teleport art come with item 060e
        !inPlay -> null
        // TODO: no frozen key; the normal frame shows while frozen until his frozen art (item 060e)
        damaged || stunned -> "damaged"
        climbing -> climbPose()
        sliding -> "groundslide"
        isBehaviorActive(BehaviorType.JUMPING) || !bodyOverGround -> "jump"
        running -> "run"
        current == "run_trans" && playing?.isFinished() == false -> "run_trans"
        current == "run_trans" || current == "stand" -> "stand"
        else -> "run_trans"
    }

    private fun climbPose(): String {
        val ladder = body.getProperty(ConstKeys.LADDER, IBodyEntity::class)
        val inLadder = ladder != null && ladder.body.getBounds().contains(body.getCenter())
        if (!inLadder && !buster.shooting) return "climb_finish"
        val movement = if (direction.isHorizontal()) body.physics.velocity.x else body.physics.velocity.y
        return if (movement != 0f) "climb" else "climb_still"
    }
}
