package com.megaman.maverick.game.entities.megamanx

import com.megaman.maverick.game.entities.state.roomTransitionPaused
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.animations.IAnimation
import com.maverick.game.engine.animations.IAnimator
import com.maverick.game.engine.behaviors.BehaviorsComponent
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.enums.ProcessState
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.extensions.objectSetOf
import com.maverick.game.engine.common.extensions.orderedMapOf
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
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
import com.maverick.game.engine.drawables.sprites.splitAndFlatten
import com.maverick.game.engine.entities.contracts.ISpritesEntity
import com.maverick.game.engine.entities.input
import com.maverick.game.engine.entities.isBehaviorActive
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.enemies.SpringHead
import com.megaman.maverick.game.entities.megaman.constants.MegaChargeStatus
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.state.*
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.players.*
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.weapons.BusterCharge
import com.megaman.maverick.game.world.body.*

class MegamanX(services: MegaServices) : MegaPlayer(services), ISpritesEntity, IDamagerEntity {

    companion object {
        const val TAG = "MegamanX"
    }

    // TODO: X does not turn the camera when the world gravity changes; the X6 levels have no gravity change, and the
    //  camera turn moves to WorldGravity with item 058
    override var direction: Direction
        get() = body.direction
        set(value) {
            body.direction = value
        }

    override val character = PlayerCharacter.X

    override val shooting: Boolean
        get() = buster.shooting

    internal val buster = PlayerBuster(this, MEGAMAN_X_BUSTER)
    internal val sabre = PlayerSabre(this, megamanXSabreParams(this))

    // TODO: a teleporter does not stop X's charge, lock damage or stop the smooth follow, as it does for Megaman;
    //  no X level has a teleporter yet (item 081)
    private val busterCharge = BusterCharge(
        fullTime = MegamanXValues.TIME_TO_FULL_CHARGE,
        halfTime = MegamanXValues.TIME_TO_HALF_CHARGE,
        speedScalar = { 1f },
        onHalfCharged = { requestToPlaySound(SoundAsset.MEGA_BUSTER_CHARGING_SOUND, loop = false) },
        onFullyCharged = {},
        onCleared = { stopSound(SoundAsset.MEGA_BUSTER_CHARGING_SOUND) }
    )

    internal val chargeStatus: MegaChargeStatus
        get() = if (windups.active === busterCharge) busterCharge.status() else MegaChargeStatus.NOT_CHARGED

    internal var airDashUsed = false
    internal var dashSpeedInAir = false
    internal var airborne = false
    internal var wallKicked = false
    internal var jumpPressKept = false
    internal var dashPressKept = false
    internal var pressHeldForLanding = false
    internal val landingProbe = GameRectangle()
    internal var groundDash: GroundDash? = null
    internal lateinit var crouch: Crouch
    internal var ladderPose: LadderPose? = null
    internal val ladderPoseTimer = Timer(MegamanXValues.CLIMB_START_TIME).setToEnd()
    internal val wallGrabTimer = Timer(MegamanXValues.WALL_GRAB_TIME)
    internal val kickLockTimer = Timer(MegamanXValues.WALL_KICK_LOCK_TIME).setToEnd()
    internal val pushOffTimer = Timer(MegamanXValues.PUSH_OFF_TIME).setToEnd()
    internal val wallDelayTimer = Timer(MegamanXValues.WALL_DELAY_AFTER_JUMP).setToEnd()

    override val cameraFocusHeight = MegamanXValues.BODY_HEIGHT

    override fun onPlayerSpawn(spawnProps: Properties) {
        buster.reset()
        sabre.reset()
        airDashUsed = false
        dashSpeedInAir = false
        airborne = false
        wallKicked = false
        jumpPressKept = false
        dashPressKept = false
        pressHeldForLanding = false
        kickLockTimer.setToEnd()
        pushOffTimer.setToEnd()
        wallDelayTimer.setToEnd()
        ladderPose = null
        ladderPoseTimer.setToEnd()
    }

    override fun onPlayerDestroy() {
        sabre.reset()
        services.audio.stopSound(SoundAsset.MEGA_BUSTER_CHARGING_SOUND)
    }

    override fun onPlayerEvent(event: Event) {
        when (event.key) {
            EventType.BEGIN_ROOM_TRANS, EventType.CONTINUE_ROOM_TRANS -> {
                stopWindup()
                sabre.end()
            }
            EventType.GATE_INIT_OPENING -> stopWindup()
        }
    }

    override fun afterPlayerDamage(damager: IDamagerEntity) {
        dashSpeedInAir = false
        stopWindup()
        sabre.end()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            killPlayerIfOutsideLevel()

            require(StunComponent::class).update(delta, body.isSensing(BodySense.FEET_ON_GROUND))
            if (stunned || damaged) stopWindup()

            damageRecovery.update(delta, damaged, damageTimer.isJustFinished())

            require(RoomTransitionCarryComponent::class).update(delta)
            require(FreezeComponent::class).update(delta)

            buster.update(delta)
            sabre.update(delta)

            kickLockTimer.update(delta)
            pushOffTimer.update(delta)
            wallDelayTimer.update(delta)
            updateLadderPose(delta)
            endKickLockOnBlock()
            keepStartPresses()
            resetAirMovesOnLanding()
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(MegamanXValues.BODY_WIDTH * ConstVals.PPM, MegamanXValues.BODY_HEIGHT * ConstVals.PPM)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        val passThroughOneWay: () -> Boolean = { isBehaviorActive(BehaviorType.CLIMBING) }
        body.putProperty(BodyKeys.PASS_THROUGH_ONE_WAY, passThroughOneWay)
        val fallThroughPressUp: () -> Boolean =
            { isBehaviorActive(BehaviorType.CLIMBING) && input.isJustPressed(MegaControllerButton.UP) }
        body.putProperty(BodyKeys.FALL_THROUGH_PRESS_UP, fallThroughPressUp)

        val playerFixture = Fixture(body, FixtureType.PLAYER, GameRectangle())
        body.addFixture(playerFixture)

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle())
        body.addFixture(bodyFixture)
        body.putProperty(ConstKeys.BODY, bodyFixture)

        val feetFixture = Fixture(
            body, FixtureType.FEET, GameRectangle().setSize(0.5f * ConstVals.PPM, 0.25f * ConstVals.PPM)
        )
        body.addFixture(feetFixture)
        body.putProperty(ConstKeys.FEET, feetFixture)

        val feetGravityRect = GameRectangle().setSize(
            (MegamanXValues.BODY_WIDTH - 0.1f) * ConstVals.PPM, 0.1f * ConstVals.PPM
        )
        val feetGravityFixture = Fixture(body, FixtureType.CONSUMER, feetGravityRect)
        feetGravityFixture.setReceiver(Receivers.FILTER) {
            it.getType() == FixtureType.BLOCK && it.receiver(Receivers.FILTER)?.invoke(feetGravityFixture) != false
        }
        val feetGravitySet = ObjectSet<IFixture>()
        feetGravityFixture.setReceiver(Receivers.CONSUMER) consumer@{ processState, fixture ->
            when (processState) {
                ProcessState.BEGIN, ProcessState.CONTINUE -> {
                    val block = fixture.getEntity() as Block
                    // TODO: the rising test reads velocity.y, so it assumes that the body points up, as Megaman's does
                    //  (item 058)
                    if (block.body.hasBodyLabel(BodyLabel.COLLIDE_DOWN_ONLY) &&
                        (body.physics.velocity.y > 0f || passThroughOneWay())
                    ) {
                        feetGravitySet.remove(fixture)
                        return@consumer
                    }
                    feetGravitySet.add(fixture)
                }

                ProcessState.END -> feetGravitySet.remove(fixture)
            }
        }
        val isBodyOverGround: () -> Boolean = { !feetGravitySet.isEmpty }
        body.putProperty(ConstKeys.BODY_OVER_GROUND, isBodyOverGround)
        body.onReset.put("${ConstKeys.FEET}_${ConstKeys.GRAVITY}") { feetGravitySet.clear() }
        body.addFixture(feetGravityFixture)
        body.putProperty("${ConstKeys.FEET}_${ConstKeys.GRAVITY}", feetGravityFixture)

        val headFixture = Fixture(
            body, FixtureType.HEAD, GameRectangle().setSize(0.5f * ConstVals.PPM, 0.25f * ConstVals.PPM)
        )
        body.addFixture(headFixture)
        body.putProperty(ConstKeys.HEAD, headFixture)

        val leftFixture = Fixture(
            body, FixtureType.SIDE, GameRectangle().setSize(0.2f * ConstVals.PPM, 0.75f * ConstVals.PPM)
        )
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        body.addFixture(leftFixture)
        body.putProperty("${ConstKeys.LEFT}_${ConstKeys.SIDE}", leftFixture)

        val rightFixture = Fixture(
            body, FixtureType.SIDE, GameRectangle().setSize(0.2f * ConstVals.PPM, 0.75f * ConstVals.PPM)
        )
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        body.addFixture(rightFixture)
        body.putProperty("${ConstKeys.RIGHT}_${ConstKeys.SIDE}", rightFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle())
        body.addFixture(damageableFixture)
        body.putProperty(ConstKeys.DAMAGEABLE, damageableFixture)

        val waterListenerFixture = Fixture(body, FixtureType.WATER_LISTENER, GameRectangle())
        body.addFixture(waterListenerFixture)

        val teleporterListenerFixture = Fixture(body, FixtureType.TELEPORTER_LISTENER, GameRectangle())
        body.addFixture(teleporterListenerFixture)

        val fixturesToSizeToBody = gdxArrayOf(
            bodyFixture, playerFixture, damageableFixture, waterListenerFixture, teleporterListenerFixture
        )

        body.preProcess.put(ConstKeys.DEFAULT) {
            val height = ConstVals.PPM * when {
                isBehaviorActive(BehaviorType.CROUCHING) -> MegamanXValues.CROUCH_HEIGHT
                else -> MegamanXValues.BODY_HEIGHT
            }
            if (body.getHeight() != height) {
                val top = body.getMaxY()
                body.setSize(body.getWidth(), height)
                if (direction == Direction.DOWN) body.setMaxY(top)
            }

            fixturesToSizeToBody.forEach { fixture -> (fixture.rawShape as GameRectangle).set(body) }

            feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
            feetGravityFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
            headFixture.offsetFromBodyAttachment.y = body.getHeight() / 2f
            leftFixture.offsetFromBodyAttachment.x = -body.getWidth() / 2f
            rightFixture.offsetFromBodyAttachment.x = body.getWidth() / 2f

            if (!inPlay) {
                body.physics.velocity.setZero()
                body.physics.gravity.setZero()
                return@put
            }

            val vertical = direction.isVertical()
            body.physics.applyFrictionX = damaged && vertical
            body.physics.applyFrictionY = damaged && !vertical
            if (vertical) body.physics.defaultFrictionOnSelf.set(MegamanXValues.KNOCKBACK_DRAG, 0f)
            else body.physics.defaultFrictionOnSelf.set(0f, MegamanXValues.KNOCKBACK_DRAG)

            if (!running && !damaged && !stunned && !holdsSideSpeed) when (direction) {
                Direction.UP, Direction.DOWN -> body.physics.velocity.x = 0f
                Direction.LEFT, Direction.RIGHT -> body.physics.velocity.y = 0f
            }

            val gravity = direction.upSign() * ConstVals.PPM * when {
                bodyOverGround -> MegamanXValues.GROUND_GRAVITY
                upSpeed() > 0f -> MegamanXValues.JUMP_GRAVITY
                else -> MegamanXValues.FALL_GRAVITY
            }

            when (direction) {
                Direction.UP, Direction.DOWN -> {
                    body.physics.gravity.set(0f, gravity)
                    body.physics.velocityClamp.set(MegamanXValues.CLAMP_X, MegamanXValues.FALL_MAX)
                        .scl(ConstVals.PPM.toFloat())
                }

                Direction.LEFT, Direction.RIGHT -> {
                    body.physics.gravity.set(gravity, 0f)
                    body.physics.velocityClamp.set(MegamanXValues.FALL_MAX, MegamanXValues.CLAMP_X)
                        .scl(ConstVals.PPM.toFloat())
                }
            }
        }

        return BodyComponentCreator.create(this, body)
    }

    override fun defineMomentumComponent() =
        PostActionMomentumComponent(0f, 0f, { 0f }, { bodyOverGround }, { false }, { false })

    override fun defineKnockbackComponent() = KnockbackComponent(
        MegamanXValues.KNOCKBACK_X * ConstVals.PPM,
        MegamanXValues.KNOCKBACK_Y * ConstVals.PPM,
        objectSetOf(SpringHead::class)
    )

    override fun defineStunComponent() = StunComponent(
        MegamanValues.STUN_DUR,
        onSurface = { body.isSensing(BodySense.FEET_ON_GROUND) },
        onStun = {
            // TODO: the stun bounce assumes that the body points up, as Megaman's does; the X6 levels have no gravity
            //  change (item 058)
            body.physics.velocity.let {
                it.x = MegamanValues.STUN_IMPULSE_X * ConstVals.PPM * -facing.value
                it.y = MegamanValues.STUN_IMPULSE_Y * ConstVals.PPM
            }
        }
    )

    // TODO: the wall grab, the wall kick and the air dash play no sound yet (owner decision D4 of 081c)
    override fun defineBehaviorsComponent(): BehaviorsComponent {
        val component = BehaviorsComponent()
        component.addBehavior(BehaviorType.WALL_SLIDING, wallSlideBehavior())
        component.addBehavior(BehaviorType.GROUND_DASHING, groundDashBehavior())
        component.addBehavior(BehaviorType.JUMPING, jumpBehavior())
        component.addBehavior(BehaviorType.AIR_DASHING, airDashBehavior())
        component.addBehavior(BehaviorType.CROUCHING, crouchBehavior())
        component.addBehavior(BehaviorType.CLIMBING, climbBehavior())
        return component
    }

    private fun shoot(status: MegaChargeStatus) = buster.shoot(
        status,
        crouched = isBehaviorActive(BehaviorType.CROUCHING),
        climbing = isBehaviorActive(BehaviorType.CLIMBING)
    )

    override fun defineControllerComponent(): ControllerComponent {
        fun side(facing: Facing) = ButtonActuator(
            onJustPressed = {
                require(RunComponent::class).lastPressedSide = facing
                run(0f, facing)
            },
            onPressContinued = { delta -> run(delta, facing) },
            onJustReleased = { if (!input.isPressed(facing.opposite().runButton())) stopRunning() },
            onReleaseContinued = { if (!input.isPressed(facing.opposite().runButton())) running = false }
        )

        val attack = ButtonActuator(
            onJustPressed = {
                if (input.isPressed(MegaControllerButton.UP) && sabre.canStart()) {
                    stopWindup()
                    sabre.start()
                }
                when {
                    sabre.active -> sabre.pressHeld = true
                    playerCanAttack -> shoot(MegaChargeStatus.NOT_CHARGED)
                }
            },
            onPressContinued = { delta ->
                if (playerCanAttack && !sabre.pressHeld) windups.hold(busterCharge, delta)
                else stopWindup()
            },
            onJustReleased = {
                val status = chargeStatus
                if (playerCanAttack && status != MegaChargeStatus.NOT_CHARGED) shoot(status)
                sabre.pressHeld = false
                stopWindup()
            },
            onReleaseContinued = { sabre.pressHeld = false }
        )

        return ControllerComponent(
            MegaControllerButton.LEFT pairTo side(Facing.LEFT),
            MegaControllerButton.RIGHT pairTo side(Facing.RIGHT),
            MegaControllerButton.B pairTo attack
        )
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 1))
        val component = SpritesComponent(MEGAMAN_X_SPRITE_KEY pairTo sprite)
        var flashTime = 0f
        component.putPreProcess(MEGAMAN_X_SPRITE_KEY) { delta, player ->
            player.setSize(MegamanXValues.SPRITE_SIZE * ConstVals.PPM, MegamanXValues.SPRITE_SIZE * ConstVals.PPM)
            val stripFacesLeft = sabre.move?.stripFacesLeft ?: (isBehaviorActive(BehaviorType.WALL_SLIDING) && !kickLocked)
            player.setFlip(if (stripFacesLeft) facing == Facing.RIGHT else facing == Facing.LEFT, false)
            player.setOriginCenter()
            player.rotation = direction.rotation
            val position = DirectionPositionMapper.getInvertedPosition(direction)
            player.setPosition(body.getPositionPoint(position), position)

            // TODO: the charge shows as a tint flash in code, because the 1LMX sheet has no charge frames (item 081)
            val tint = when (chargeStatus) {
                MegaChargeStatus.FULLY_CHARGED -> FULL_CHARGE_TINT
                MegaChargeStatus.HALF_CHARGED -> HALF_CHARGE_TINT
                MegaChargeStatus.NOT_CHARGED -> null
            }
            flashTime = if (tint == null) 0f else flashTime + delta
            val flashOn = tint != null && (flashTime / MegamanXValues.CHARGE_FLASH_TIME).toInt() % 2 == 0
            player.setColor(if (flashOn) tint else Color.WHITE)

            applyHideAndFlash(player)
        }
        return component
    }

    override fun defineAnimationsComponent(): AnimationsComponent {
        val atlas = services.assets.getTextureAtlas(TextureAsset.MEGAMAN_X.source)
        fun frames(name: String, count: Int): Array<TextureRegion> =
            atlas.findRegion(name)!!.splitAndFlatten(count, 1, Array())

        fun part(name: String, count: Int, from: Int, until: Int, duration: Float, loop: Boolean): Animation {
            val all = frames(name, count)
            val out = Array<TextureRegion>()
            for (i in from until until) out.add(all[i])
            return Animation(out, duration, loop)
        }

        val animations = ObjectMap<String, IAnimation>()
        // TODO: the stand uses the simplified 2-frame strip (eyes open, blink) with Megaman's timing; the 5-frame
        //  "stand" strip from the 1LMX sheet needs more work before X uses it (item 081)
        animations.put("stand", Animation(frames("stand_simplified", 2), gdxArrayOf(1.5f, 0.15f)))
        animations.put("land_shoot", Animation(frames("land_shoot", 1), 1f))
        animations.put("run_trans", Animation(frames("run_trans", 1), 1f))
        animations.put("run_trans_shoot", Animation(frames("run_trans_shoot", 1), 1f))
        animations.put("run", Animation(frames("run", 8), 0.08f))
        animations.put("run_shoot", Animation(frames("run_shoot", 8), 0.08f))
        animations.put("jump", part("jump_v2", 5, 0, 4, 0.08f, false))
        animations.put("fall", part("jump_v2", 5, 4, 5, 0.1f, true))
        animations.put("jump_shoot", part("jump_shoot_v2", 5, 0, 4, 0.08f, false))
        animations.put("fall_shoot", part("jump_shoot_v2", 5, 4, 5, 0.1f, true))
        animations.put("damaged", Animation(frames("damaged", 3), 0.1f, false))
        val holdThenStay = gdxArrayOf(MegamanXValues.AIR_DASH_HOVER_TIME, 1f)
        animations.put("dash", Animation(frames("dash", 2), holdThenStay, false))
        animations.put("dash_shoot", Animation(frames("dash_shoot", 2), holdThenStay, false))
        val brake = gdxArrayOf(
            MegamanXValues.DASH_END_FRAME_1,
            MegamanXValues.DASH_END_FRAME_2,
            MegamanXValues.DASH_END_FRAME_3,
            MegamanXValues.DASH_END_FRAME_4
        )
        fun sheet2x2(name: String) = atlas.findRegion(name)!!.splitAndFlatten(2, 2, Array())
        animations.put("dash_end", Animation(sheet2x2("dash_end"), brake, false))
        animations.put("dash_end_shoot", Animation(sheet2x2("dash_end_shoot"), brake, false))
        val grabThenSlide = gdxArrayOf(MegamanXValues.WALL_GRAB_TIME, 1f)
        animations.put("wallslide", Animation(frames("wallslide", 2), grabThenSlide, false))
        animations.put("wallslide_shoot", Animation(frames("wallslide_shoot", 2), grabThenSlide, false))
        animations.put("walljump", Animation(frames("walljump", 3), MegamanXValues.WALL_KICK_LOCK_TIME / 3f, false))
        animations.put("walljump_shoot", Animation(frames("walljump_shoot", 3), MegamanXValues.WALL_KICK_LOCK_TIME / 3f, false))
        animations.put("air_damaged", Animation(frames("air_damaged", 3), 0.1f, false))
        val crouchDown = gdxArrayOf(MegamanXValues.CROUCH_FRAME_TIME, 1f)
        animations.put("crouch", Animation(frames("crouch", 2), crouchDown, false))
        animations.put("crouch_shoot", Animation(frames("crouch_shoot", 2), crouchDown, false))
        animations.put("crouch_rise", Animation(frames("crouch", 2)[0]))
        animations.put(
            "climb",
            Animation(atlas.findRegion("climb")!!.splitAndFlatten(3, 2, Array()), MegamanXValues.CLIMB_FRAME_TIME)
        )
        animations.put("climb_shoot", Animation(frames("climb_shoot", 1), 1f))
        animations.put("climb_start", Animation(frames("climb_start", 1), 1f))
        animations.put(
            "climb_finish",
            Animation(frames("climb_finish", 2), MegamanXValues.CLIMB_FINISH_TIME / 2f, false)
        )
        MegamanXSabreMoves.ALL.forEach { animations.put(it.bodyRegion, Animation(frames(it.bodyRegion, it.stripFrames), 1f)) }

        val animator = Animator(
            keySupplier = { priorKey -> animationKey(priorKey) },
            animations = animations,
            onChangeKey = { _, currentKey, nextKey ->
                if (currentKey != null && nextKey != null &&
                    currentKey.removeSuffix("_shoot") == nextKey.removeSuffix("_shoot")
                ) animations[currentKey]?.getCurrentTime()?.let { animations[nextKey]?.setCurrentTime(it) }
            },
            shouldUpdateAnimationPredicate = { key, animation, _ ->
                val kind = sabre.move
                when {
                    roomTransitionPaused || (services.levelState.roomTransition && key != "run") -> false
                    key == "climb" && body.physics.velocity.isZero -> false
                    kind != null && key == kind.bodyRegion -> {
                        animation.setIndex(sabre.frame)
                        false
                    }
                    else -> true
                }
            }
        )
        return AnimationsComponent(orderedMapOf<Any, IAnimator>(MEGAMAN_X_SPRITE_KEY pairTo animator), sprites)
    }
}
