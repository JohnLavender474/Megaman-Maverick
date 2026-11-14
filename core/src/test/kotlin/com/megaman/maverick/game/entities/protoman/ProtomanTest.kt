package com.megaman.maverick.game.entities.protoman

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.world.body.Fixture
import com.maverick.game.engine.world.body.IFixture
import com.megaman.maverick.game.entities.decorations.WhiteBurst
import com.megaman.maverick.game.entities.blocks.SabreBlock
import com.megaman.maverick.game.entities.contracts.AbstractHealthEntity
import com.megaman.maverick.game.entities.enemies.HoverGunner
import com.megaman.maverick.game.entities.enemies.Junkroid
import com.megaman.maverick.game.entities.explosions.Explosion
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.container.SimpleGridWorldContainer
import com.megaman.maverick.game.entities.projectiles.Picket
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.Receivers
import com.megaman.maverick.game.world.body.receiver
import com.megaman.maverick.game.world.contacts.rules.DamageRule
import com.megaman.maverick.game.world.contacts.rules.ProjectileRule
import com.maverick.game.engine.common.enums.Size
import com.megaman.maverick.game.damage.StandardDamageNegotiator
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.animations.IAnimation
import com.maverick.game.engine.audio.AudioComponent
import com.maverick.game.engine.behaviors.BehaviorsComponent
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.controller.ControllerSystem
import com.maverick.game.engine.controller.buttons.ButtonStatus
import com.maverick.game.engine.entities.EntityExistenceState
import com.maverick.game.engine.input.InputFrame
import com.maverick.game.engine.input.InputSource
import com.maverick.game.engine.locks.LocksComponent
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.points.Points
import com.maverick.game.engine.points.PointsComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.controllers.SelectButtonAction
import com.maverick.game.engine.common.shapes.GameRectangle
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.enemies.MetaWheelF
import com.megaman.maverick.game.entities.state.*
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.entities.IGameEntity
import com.megaman.maverick.game.entities.contracts.IOwnable
import com.megaman.maverick.game.entities.contracts.ITeleporterEntity
import com.megaman.maverick.game.entities.projectiles.ProtoBuster
import com.megaman.maverick.game.entities.players.PlayerSabreSlash
import com.megaman.maverick.game.players.SabreState
import io.kotest.assertions.throwables.shouldThrow
import com.megaman.maverick.game.players.SabreStateMissingException
import com.megaman.maverick.game.players.SabreParams
import com.megaman.maverick.game.players.PlayerSabre
import com.megaman.maverick.game.players.PlayerBuster
import com.megaman.maverick.game.players.BusterParams
import com.megaman.maverick.game.players.BusterShot
import com.megaman.maverick.game.players.BusterClimbOffsetMissingException
import com.megaman.maverick.game.players.BusterCrouchOffsetMissingException
import com.megaman.maverick.game.entities.megaman.constants.MegaChargeStatus
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.maverick.game.engine.common.extensions.objectMapOf
import com.maverick.game.engine.common.objects.props
import com.megaman.maverick.game.events.EventType
import com.maverick.game.engine.events.Event
import com.megaman.maverick.game.players.SabreMove
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.locks.MegaLockGroup
import com.megaman.maverick.game.testServices
import com.maverick.game.engine.components.require
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.floats.shouldBeGreaterThanOrEqual
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll

private const val DELTA = 0.05f
private const val TOLERANCE = 0.001f

class ProtomanTest : DescribeSpec({

    lateinit var proto: Protoman
    lateinit var body: Body
    lateinit var controllerSystem: ControllerSystem
    lateinit var spawned: MutableList<Pair<IGameEntity, Properties>>
    var frame = InputFrame.RELEASED
    var worldStep: () -> Unit = {}
    var overGround = true

    val impulse = ProtomanValues.RUN_IMPULSE * DELTA * ConstVals.PPM

    fun step(vararg buttons: Pair<MegaControllerButton, ButtonStatus>) {
        frame = InputFrame.RELEASED
        buttons.forEach { (button, status) -> frame = frame.with(button, status) }
        controllerSystem.update(DELTA)
        worldStep()
        proto.buster.update(DELTA)
        proto.sabre.update(DELTA)
        proto.updateGuard(DELTA)
        proto.updateDeflects()
    }

    fun hold(button: MegaControllerButton, seconds: Float) {
        step(button to ButtonStatus.JUST_PRESSED)
        var time = 0f
        while (time < seconds - TOLERANCE) {
            step(button to ButtonStatus.PRESSED)
            time += DELTA
        }
    }

    fun hurt() {
        val damager = MetaWheelF(proto.services).also { it.addComponent(TagsComponent(MetaWheelF.TAG)) }
        proto.takeDamageFrom(damager) shouldBe true
    }

    beforeEach {
        frame = InputFrame.RELEASED
        worldStep = {}
        overGround = true
        spawned = mutableListOf()
        val engine = mockk<GameEngine>(relaxed = true)
        every { engine.spawn(any(), any()) } answers {
            val entity = firstArg<IGameEntity>()
            val props = secondArg<Properties>()
            entity.existenceState = EntityExistenceState.LIVE
            @Suppress("UNCHECKED_CAST")
            (entity as? IOwnable<IGameEntity>)?.owner = props.get(ConstKeys.OWNER) as IGameEntity?
            if (entity is PlayerSabreSlash) {
                if (!entity.initialized) {
                    entity.init()
                    entity.initialized = true
                }
                entity.onSpawn(props)
            }
            spawned.add(entity to props)
            true
        }
        val services = testServices(engine = engine)
        val atlas = mockk<TextureAtlas>()
        every { atlas.findRegion(any()) } returns AtlasRegion(mockk<Texture>(relaxed = true), 0, 0, 64, 64)
        every { services.assets.get(any<String>(), TextureAtlas::class.java) } returns atlas
        services.levelState.currentLevel = LevelDefinition.REFACTOR_TEST_060
        MegaEntityFactory.init(services)
        proto = Protoman(services)
        proto.existenceState = EntityExistenceState.LIVE
        proto.facing = Facing.RIGHT
        proto.addComponent(proto.defineBodyComponent())
        body = (proto.getComponent(BodyComponent::class)!!).body
        body.putProperty(ConstKeys.BODY_OVER_GROUND, { overGround })
        proto.direction = Direction.UP
        services.players.register(PlayerSlot(0), proto)
        proto.addComponent(proto.defineControllerComponent().also { it.input = InputSource { frame } })
        proto.addComponent(RunComponent())
        proto.addComponent(LocksComponent(MegaLockGroup.entries.size))
        proto.addComponent(PlayPhaseComponent(PlayPhase.IN_PLAY))
        proto.addComponent(TeleportableComponent())
        proto.addComponent(proto.damageRecovery)
        proto.addComponent(proto.defineStunComponent())
        proto.addComponent(proto.defineFreezeComponent())
        proto.addComponent(proto.defineMomentumComponent())
        proto.addComponent(proto.defineBehaviorsComponent())
        proto.addComponent(AudioComponent())
        proto.addComponent(
            PointsComponent(ConstKeys.HEALTH pairTo Points(0, ConstVals.MAX_HEALTH, ConstVals.MAX_HEALTH))
        )

        controllerSystem = ControllerSystem()
        controllerSystem.add(proto)
    }

    afterEach {
        MegaEntityFactory.reset()
        unmockkAll()
    }

    describe("the run") {
        it("adds the run impulse to the right on each held step") {
            step(MegaControllerButton.RIGHT to ButtonStatus.JUST_PRESSED)
            step(MegaControllerButton.RIGHT to ButtonStatus.PRESSED)
            body.physics.velocity.x shouldBe (impulse plusOrMinus TOLERANCE)
            proto.running shouldBe true
            proto.facing shouldBe Facing.RIGHT
        }

        it("faces and runs left") {
            step(MegaControllerButton.LEFT to ButtonStatus.JUST_PRESSED)
            step(MegaControllerButton.LEFT to ButtonStatus.PRESSED)
            proto.facing shouldBe Facing.LEFT
            body.physics.velocity.x shouldBe (-impulse plusOrMinus TOLERANCE)
        }

        it("stops adding speed at the max run speed") {
            hold(MegaControllerButton.RIGHT, 1f)
            body.physics.velocity.x shouldBeGreaterThanOrEqual ProtomanValues.RUN_MAX_SPEED * ConstVals.PPM
            body.physics.velocity.x shouldBeLessThan ProtomanValues.RUN_MAX_SPEED * ConstVals.PPM + impulse
        }

        it("stops running on the release") {
            hold(MegaControllerButton.RIGHT, 0.3f)
            step(MegaControllerButton.RIGHT to ButtonStatus.JUST_RELEASED)
            proto.running shouldBe false
        }

        it("does not run while damaged") {
            hurt()
            hold(MegaControllerButton.RIGHT, 0.2f)
            body.physics.velocity.x shouldBe 0f
            proto.running shouldBe false
        }
    }

    describe("the jump") {
        fun jump() = proto.defineBehaviorsComponent().getBehavior(BehaviorType.JUMPING)!!

        it("starts on a new press of A over the ground with the jump speed") {
            val jump = jump()
            frame = InputFrame.RELEASED.with(MegaControllerButton.A, ButtonStatus.JUST_PRESSED)
            jump.evaluate(0f) shouldBe true
            jump.init()
            body.physics.velocity.y shouldBe (ProtomanValues.JUMP_VEL * ConstVals.PPM plusOrMinus TOLERANCE)
        }

        it("jumps down when the gravity points down") {
            proto.direction = Direction.DOWN
            jump().init()
            body.physics.velocity.y shouldBe (-ProtomanValues.JUMP_VEL * ConstVals.PPM plusOrMinus TOLERANCE)
        }

        it("does not jump on A while DOWN is held on the ground") {
            frame = InputFrame.RELEASED
                .with(MegaControllerButton.A, ButtonStatus.JUST_PRESSED)
                .with(MegaControllerButton.DOWN, ButtonStatus.PRESSED)
            jump().evaluate(0f) shouldBe false
        }

        it("does not start in the air") {
            overGround = false
            frame = InputFrame.RELEASED.with(MegaControllerButton.A, ButtonStatus.JUST_PRESSED)
            jump().evaluate(0f) shouldBe false
        }

        it("does not start on a held A") {
            frame = InputFrame.RELEASED.with(MegaControllerButton.A, ButtonStatus.PRESSED)
            jump().evaluate(0f) shouldBe false
        }
    }

    describe("the gravity") {
        it("uses the fall gravity in the air") {
            overGround = false
            body.preProcess()
            body.physics.gravity.y shouldBe (ProtomanValues.FALL_GRAVITY * ConstVals.PPM plusOrMinus TOLERANCE)
        }

        it("signs the gravity by the direction") {
            overGround = false
            proto.direction = Direction.DOWN
            body.preProcess()
            body.physics.gravity.y shouldBe (-ProtomanValues.FALL_GRAVITY * ConstVals.PPM plusOrMinus TOLERANCE)
        }
    }

    describe("the weapon slots") {
        fun select() = step(MegaControllerButton.SELECT to ButtonStatus.JUST_RELEASED)

        fun pings() =
            proto.require(AudioComponent::class).playSoundRequests.count { it.source == SoundAsset.SELECT_PING_SOUND }

        it("starts with the sabre") {
            proto.weapon shouldBe ProtomanWeapon.SABRE
        }

        it("switches sabre, buster, sabre on SELECT, with a ping each time") {
            select()
            proto.weapon shouldBe ProtomanWeapon.BUSTER
            select()
            proto.weapon shouldBe ProtomanWeapon.SABRE
            pings() shouldBe 2
        }

        it("does not switch on the SELECT press") {
            step(MegaControllerButton.SELECT to ButtonStatus.JUST_PRESSED)
            proto.weapon shouldBe ProtomanWeapon.SABRE
        }

        it("does not switch when SELECT has another action") {
            proto.services.players.settings(PlayerSlot(0)).selectButtonAction = SelectButtonAction.AIR_DASH
            select()
            proto.weapon shouldBe ProtomanWeapon.SABRE
            pings() shouldBe 0
        }

        it("does not switch during a slash") {
            step(MegaControllerButton.B to ButtonStatus.JUST_PRESSED)
            proto.sabre.active shouldBe true
            select()
            proto.weapon shouldBe ProtomanWeapon.SABRE
        }

        it("does not switch while damaged") {
            hurt()
            select()
            proto.weapon shouldBe ProtomanWeapon.SABRE
        }

        it("goes back to the sabre at a spawn") {
            select()
            proto.addComponent(TagsComponent(Protoman.TAG))
            proto.onSpawn(props(ConstKeys.BOUNDS pairTo GameRectangle(0f, 0f, ConstVals.PPM.toFloat(), ConstVals.PPM.toFloat())))
            proto.weapon shouldBe ProtomanWeapon.SABRE
        }
    }

    describe("the buster slot") {
        fun useBuster() = step(MegaControllerButton.SELECT to ButtonStatus.JUST_RELEASED)

        fun shoot(vararg held: MegaControllerButton) =
            step(MegaControllerButton.B to ButtonStatus.JUST_PRESSED, *held.map { it to ButtonStatus.PRESSED }.toTypedArray())

        fun shots() = spawned.count { it.first is ProtoBuster }

        it("fires one pellet on B on the ground") {
            useBuster()
            shoot()
            shots() shouldBe 1
            spawned[0].first.shouldBeInstanceOf<ProtoBuster>()
            spawned[0].second.get(ConstKeys.OWNER) shouldBe proto
            proto.buster.shooting shouldBe true
            proto.sabre.active shouldBe false
        }

        it("fires while he runs, and the run goes on") {
            useBuster()
            hold(MegaControllerButton.RIGHT, 0.3f)
            val speed = body.physics.velocity.x
            shoot(MegaControllerButton.RIGHT)
            shots() shouldBe 1
            step(MegaControllerButton.RIGHT to ButtonStatus.PRESSED)
            proto.running shouldBe true
            body.physics.velocity.x shouldBeGreaterThanOrEqual speed
        }

        it("fires in the air") {
            useBuster()
            overGround = false
            shoot()
            shots() shouldBe 1
        }

        it("lets him turn and run during the shot frame") {
            useBuster()
            shoot()
            step(MegaControllerButton.LEFT to ButtonStatus.JUST_PRESSED)
            step(MegaControllerButton.LEFT to ButtonStatus.PRESSED)
            proto.buster.shooting shouldBe true
            proto.facing shouldBe Facing.LEFT
            proto.running shouldBe true
        }

        it("fires nothing on the release") {
            useBuster()
            step(MegaControllerButton.B to ButtonStatus.JUST_RELEASED)
            shots() shouldBe 0
        }

        it("waits the cooldown before the next shot") {
            useBuster()
            shoot()
            shoot()
            shots() shouldBe 1
            proto.buster.update(ProtomanValues.SHOT_WAIT)
            shoot()
            shots() shouldBe 2
        }

        it("fires no shot while damaged") {
            useBuster()
            hurt()
            shoot()
            shots() shouldBe 0
        }

        it("starts one sabre slash on UP and B, with no shot and no chain") {
            useBuster()
            shoot(MegaControllerButton.UP)
            shots() shouldBe 0
            proto.sabre.move shouldBe ProtomanSabreMoves.SLASH_1
            shoot()
            var time = 0f
            while (time < ProtomanSabreMoves.SLASH_1.duration) {
                step()
                time += DELTA
            }
            proto.sabre.active shouldBe false
            shots() shouldBe 0
        }

        it("ends the shot frame on a hit") {
            useBuster()
            shoot()
            hurt()
            proto.buster.shooting shouldBe false
        }

        it("throws on a crouched or a ladder shot when the params have no offset for it") {
            val buster = PlayerBuster(
                proto, BusterParams(
                    notCharged = BusterShot(
                        speed = ProtomanValues.SHOT_SPEED,
                        wait = ProtomanValues.SHOT_WAIT,
                        poseTime = ProtomanValues.SHOT_POSE_TIME,
                        limit = null,
                        sound = SoundAsset.MEGA_BUSTER_BULLET_SHOT_SOUND,
                        fetch = { MegaEntityFactory.fetch(ProtoBuster::class)!! }
                    ),
                    halfCharged = null,
                    fullyCharged = null,
                    offsetX = ProtomanValues.SHOT_OFFSET_X,
                    offsetY = ProtomanValues.SHOT_OFFSET_Y,
                    runOffsetX = 0f,
                    airOffsetY = 0f,
                    crouchOffset = null,
                    climbOffset = null
                )
            )
            shouldThrow<BusterCrouchOffsetMissingException> {
                buster.shoot(MegaChargeStatus.NOT_CHARGED, crouched = true)
            }
            shouldThrow<BusterClimbOffsetMissingException> {
                buster.shoot(MegaChargeStatus.NOT_CHARGED, climbing = true)
            }
            shots() shouldBe 0
        }
    }

    describe("the sabre slot") {
        it("starts the sabre on UP and B on the ground, with no shot") {
            step(MegaControllerButton.UP to ButtonStatus.PRESSED, MegaControllerButton.B to ButtonStatus.JUST_PRESSED)
            proto.sabre.move shouldBe ProtomanSabreMoves.SLASH_1
            spawned.none { it.first is ProtoBuster } shouldBe true
        }
    }

    describe("the sabre") {
        fun pressB() = step(MegaControllerButton.B to ButtonStatus.JUST_PRESSED)

        fun runOut(move: SabreMove) {
            var time = 0f
            while (time < move.duration) {
                step()
                time += DELTA
            }
        }

        fun slashes() = spawned.count { it.first is PlayerSabreSlash }

        it("starts the first floor slash on B on the ground") {
            pressB()
            proto.sabre.state shouldBe SabreState.FLOOR
            proto.sabre.move shouldBe ProtomanSabreMoves.SLASH_1
            slashes() shouldBe 1
        }

        it("ends after one slash when B is not pressed again") {
            pressB()
            runOut(ProtomanSabreMoves.SLASH_1)
            proto.sabre.active shouldBe false
        }

        it("chains the 3 floor slashes on a press during each slash, and no fourth") {
            pressB()
            pressB()
            runOut(ProtomanSabreMoves.SLASH_1)
            proto.sabre.move shouldBe ProtomanSabreMoves.SLASH_2
            pressB()
            runOut(ProtomanSabreMoves.SLASH_2)
            proto.sabre.move shouldBe ProtomanSabreMoves.SLASH_3
            pressB()
            runOut(ProtomanSabreMoves.SLASH_3)
            proto.sabre.active shouldBe false
            slashes() shouldBe 1
        }

        it("starts the air slash on B in the air, also with UP held") {
            overGround = false
            step(MegaControllerButton.UP to ButtonStatus.PRESSED, MegaControllerButton.B to ButtonStatus.JUST_PRESSED)
            proto.sabre.state shouldBe SabreState.AIR
            proto.sabre.move shouldBe ProtomanSabreMoves.AIR
            spawned.none { it.first is ProtoBuster } shouldBe true
        }

        it("ends a floor combo when he leaves the ground") {
            pressB()
            pressB()
            overGround = false
            step()
            proto.sabre.active shouldBe false
            pressB()
            proto.sabre.move shouldBe ProtomanSabreMoves.AIR
        }

        it("goes on with the swing on the ground when he lands before the follow-through") {
            overGround = false
            pressB()
            step()
            val time = proto.sabre.time
            overGround = true
            step()
            proto.sabre.move shouldBe ProtomanSabreMoves.AIR_LAND
            proto.sabre.state shouldBe SabreState.FLOOR
            proto.sabre.time shouldBe (time + DELTA plusOrMinus TOLERANCE)
            slashes() shouldBe 1
        }

        it("does not chain the landed swing into the second floor slash") {
            overGround = false
            pressB()
            overGround = true
            step()
            pressB()
            runOut(ProtomanSabreMoves.AIR_LAND)
            proto.sabre.active shouldBe false
        }

        it("ends the air slash when he lands in its follow-through") {
            overGround = false
            pressB()
            var time = 0f
            while (ProtomanSabreMoves.AIR.frameAt(time) <= ProtomanSabreMoves.AIR.swingEndFrame) {
                step()
                time += DELTA
            }
            overGround = true
            step()
            proto.sabre.active shouldBe false
        }

        it("does not run, but turns, during a floor slash, and the hit box turns with him") {
            pressB()
            val slash = proto.sabre.slash!!
            slash.body.preProcess()
            slash.body.getCenter(Vector2()).x shouldBeGreaterThan proto.body.getCenter(Vector2()).x
            step(MegaControllerButton.LEFT to ButtonStatus.JUST_PRESSED)
            step(MegaControllerButton.LEFT to ButtonStatus.PRESSED)
            proto.running shouldBe false
            proto.facing shouldBe Facing.LEFT
            proto.sabre.active shouldBe true
            slash.body.preProcess()
            slash.body.getCenter(Vector2()).x shouldBeLessThan proto.body.getCenter(Vector2()).x
        }

        it("ends the slash on a hit") {
            pressB()
            hurt()
            proto.sabre.active shouldBe false
        }

        it("ends the slash and the shot pose when a gate opens") {
            pressB()
            proto.onEvent(Event(EventType.GATE_INIT_OPENING, props()))
            proto.sabre.active shouldBe false
            step(MegaControllerButton.SELECT to ButtonStatus.JUST_RELEASED)
            step(MegaControllerButton.B to ButtonStatus.JUST_PRESSED)
            proto.buster.shooting shouldBe true
            proto.onEvent(Event(EventType.GATE_INIT_OPENING, props()))
            proto.buster.shooting shouldBe false
        }

        it("throws when the state has no move list") {
            val sabre = PlayerSabre(
                proto, SabreParams(
                    moves = objectMapOf(SabreState.FLOOR pairTo gdxArrayOf(ProtomanSabreMoves.SLASH_1)),
                    atlas = TextureAsset.PROTOMAN,
                    spriteSize = ProtomanValues.SPRITE_SIZE,
                    artPxPerBodyFrame = 48,
                    sound = SoundAsset.WHIP_V2_SOUND,
                    canStart = { true },
                    chooseState = { SabreState.AIR },
                    spriteOffsetY = { 0f },
                    endOnStateChange = true,
                    landingMoves = ObjectMap()
                )
            )
            shouldThrow<SabreStateMissingException> { sabre.start() }
        }

        it("does the half-charge damage on hits 1 and 2, and the full-charge damage on hit 3") {
            fun damageOf(): List<Int> {
                val slash = proto.sabre.slash!!
                slash.body.preProcess()
                return listOf(Size.LARGE, Size.MEDIUM, Size.SMALL).map { StandardDamageNegotiator.get(it, slash) }
            }
            pressB()
            damageOf() shouldBe listOf(4, 12, 20)
            pressB()
            runOut(ProtomanSabreMoves.SLASH_1)
            proto.sabre.move shouldBe ProtomanSabreMoves.SLASH_2
            damageOf() shouldBe listOf(4, 12, 20)
            pressB()
            runOut(ProtomanSabreMoves.SLASH_2)
            proto.sabre.move shouldBe ProtomanSabreMoves.SLASH_3
            damageOf() shouldBe listOf(5, 15, ConstVals.MAX_HEALTH)
        }

        it("does the half-charge damage on the air slash") {
            overGround = false
            pressB()
            val slash = proto.sabre.slash!!
            slash.body.preProcess()
            StandardDamageNegotiator.get(Size.MEDIUM, slash) shouldBe 12
        }

        it("shows the body frame of the slash") {
            val animations = ObjectMap<String, IAnimation>()
            pressB()
            proto.animationKey("stand", animations) shouldBe "sabre_1"
        }
    }

    describe("the shield") {
        fun shield() = body.getFixtures(Array<IFixture>(), FixtureType.SHIELD).first() as Fixture

        fun damageable() = body.getProperty(ConstKeys.DAMAGEABLE, Fixture::class)!!

        fun guardStep(vararg others: Pair<MegaControllerButton, ButtonStatus>) =
            step(MegaControllerButton.DOWN to ButtonStatus.PRESSED, *others)

        beforeEach { worldStep = { body.preProcess() } }

        fun waitForGuard() {
            step(MegaControllerButton.DOWN to ButtonStatus.JUST_PRESSED)
            var time = DELTA
            while (time < ProtomanValues.GUARD_CROUCH_PULL_OUT_TIME + DELTA + TOLERANCE) {
                guardStep()
                time += DELTA
            }
        }

        fun bodyBox() = body.getProperty(ConstKeys.BODY, Fixture::class)!!

        fun place() {
            body.setSize(ProtomanValues.BODY_WIDTH * ConstVals.PPM, ProtomanValues.BODY_HEIGHT * ConstVals.PPM)
            body.setCenter(0f, 0f)
            body.preProcess()
        }

        fun enemyBullet(x: Float, y: Float, speedX: Float, trajectory: Boolean = false): Bullet {
            val bullet = Bullet(proto.services)
            bullet.init()
            bullet.initialized = true
            bullet.onSpawn(
                props(
                    ConstKeys.OWNER pairTo MetaWheelF(proto.services),
                    ConstKeys.POSITION pairTo Vector2(x, y),
                    (if (trajectory) ConstKeys.TRAJECTORY else ConstKeys.IMPULSE) pairTo Vector2(speedX, 0f),
                    "${ConstKeys.SPAWN}_${ConstKeys.RESIDUAL}" pairTo false
                )
            )
            bullet.existenceState = EntityExistenceState.LIVE
            return bullet
        }

        fun sparks() = spawned.count { it.first is WhiteBurst }

        fun Bullet.fixture(type: FixtureType) =
            this.body.getFixtures(Array<IFixture>(), type).first()

        fun health() = proto.getCurrentHealth()

        fun IFixture.rect() = getShape() as GameRectangle

        fun GameRectangle.getCenterY() = getY() + getHeight() / 2f

        it("is off at the spawn and with no DOWN, and on after the pull-out while DOWN is held") {
            proto.guarding shouldBe false
            repeat(10) { step() }
            proto.guarding shouldBe false
            waitForGuard()
            proto.guarding shouldBe true
        }

        it("shows the pull-out frames first, and the guard pose only in a frame whose world step had the shield on") {
            val animations = ObjectMap<String, IAnimation>()
            step(MegaControllerButton.DOWN to ButtonStatus.JUST_PRESSED)
            var key = proto.animationKey("stand", animations)
            key shouldBe "guard_crouch_start"
            shield().isActive() shouldBe false
            var shown = false
            repeat(6) {
                guardStep()
                key = proto.animationKey(key, animations)
                (key == "guard_crouch") shouldBe shield().isActive()
                if (key == "guard_crouch") shown = true
            }
            shown shouldBe true
        }

        fun stepsToGuard(): Int {
            step(MegaControllerButton.DOWN to ButtonStatus.JUST_PRESSED)
            var steps = 1
            while (!proto.guarding && steps < 20) {
                guardStep()
                steps++
            }
            return steps
        }

        it("pulls the shield out in 4 frames of 0.05 s on the ground, with the shield on from the next world step") {
            stepsToGuard() shouldBe 4 + 1
        }

        it("pulls the shield out in 3 frames of 0.05 s in the air") {
            overGround = false
            stepsToGuard() shouldBe 3 + 1
        }

        it("keeps the frame time between the air and ground guard keys") {
            fun twoFrames() = Animation(gdxArrayOf(TextureRegion(), TextureRegion()), 0.05f, false)
            val animations = ObjectMap<String, IAnimation>()
            listOf(
                "guard_jump_start", "guard_crouch_start", "guard_jump", "guard_crouch", "guard_jump_end",
                "guard_crouch_end", "stand"
            ).forEach { animations.put(it, twoFrames()) }
            animations["guard_jump_start"].update(0.06f)
            proto.carryFrameTime("guard_jump_start", "guard_crouch_start", animations)
            animations["guard_crouch_start"].getIndex() shouldBe 1
            animations["guard_jump"].update(0.07f)
            proto.carryFrameTime("guard_jump", "guard_crouch", animations)
            animations["guard_crouch"].getCurrentTime() shouldBe (0.07f plusOrMinus TOLERANCE)
            proto.carryFrameTime("guard_crouch_start", "guard_crouch", animations)
            animations["guard_crouch"].getCurrentTime() shouldBe (0.07f plusOrMinus TOLERANCE)
            animations["stand"].update(0.03f)
            proto.carryFrameTime("stand", "guard_jump_start", animations)
            animations["guard_jump_start"].getCurrentTime() shouldBe (0.06f plusOrMinus TOLERANCE)
            proto.carryFrameTime("guard_crouch", "guard_crouch_end", animations)
            animations["guard_crouch_end"].getCurrentTime() shouldBe 0f
            animations["guard_jump_end"].update(0.06f)
            proto.carryFrameTime("guard_jump_end", "guard_crouch_end", animations)
            animations["guard_crouch_end"].getIndex() shouldBe 1
        }

        it("drops at once when DOWN is released") {
            waitForGuard()
            shield().isActive() shouldBe true
            step(MegaControllerButton.DOWN to ButtonStatus.JUST_RELEASED)
            proto.guarding shouldBe false
            shield().isActive() shouldBe false
        }

        fun release() = step(MegaControllerButton.DOWN to ButtonStatus.JUST_RELEASED)

        fun putAwaySteps(): Int {
            var steps = 1
            while (proto.puttingAwayGuard && steps < 20) {
                step()
                steps++
            }
            return steps - 1
        }

        it("plays the ground put-away for 4 frames after the release, and then the stand") {
            waitForGuard()
            release()
            proto.puttingAwayGuard shouldBe true
            proto.animationKey("guard_crouch", ObjectMap()) shouldBe "guard_crouch_end"
            putAwaySteps() shouldBe 4
            proto.animationKey("guard_crouch_end", ObjectMap()) shouldBe "stand"
        }

        it("plays the air put-away for 3 frames after the release") {
            overGround = false
            waitForGuard()
            release()
            proto.animationKey("guard_jump", ObjectMap()) shouldBe "guard_jump_end"
            putAwaySteps() shouldBe 3
            proto.animationKey("guard_jump_end", ObjectMap()) shouldBe "jump"
        }

        it("plays no put-away for a release in the pull-out") {
            step(MegaControllerButton.DOWN to ButtonStatus.JUST_PRESSED)
            guardStep()
            release()
            proto.puttingAwayGuard shouldBe false
            proto.animationKey("guard_crouch_start", ObjectMap()) shouldBe "stand"
        }

        it("plays no put-away for a release on the step after the pull-out ends, before the guard pose shows") {
            step(MegaControllerButton.DOWN to ButtonStatus.JUST_PRESSED)
            repeat(ProtomanValues.GUARD_CROUCH_TURN_FRAMES - 1) { guardStep() }
            proto.guarding shouldBe false
            release()
            proto.puttingAwayGuard shouldBe false
        }

        listOf(
            MegaControllerButton.B to ButtonStatus.JUST_PRESSED,
            MegaControllerButton.A to ButtonStatus.JUST_PRESSED,
            MegaControllerButton.LEFT to ButtonStatus.PRESSED,
            MegaControllerButton.RIGHT to ButtonStatus.PRESSED
        ).forEach { input ->
            it("cuts the put-away at once on ${input.first}") {
                waitForGuard()
                release()
                step(input)
                proto.puttingAwayGuard shouldBe false
                proto.animationKey("guard_crouch_end", ObjectMap()) shouldNotBe "guard_crouch_end"
            }
        }

        it("does not cut the put-away on an A held from before the release") {
            waitForGuard()
            step(MegaControllerButton.DOWN to ButtonStatus.JUST_RELEASED, MegaControllerButton.A to ButtonStatus.PRESSED)
            proto.puttingAwayGuard shouldBe true
        }

        it("ends the put-away on a hit") {
            waitForGuard()
            release()
            hurt()
            proto.puttingAwayGuard shouldBe false
        }

        mapOf<String, () -> Unit>(
            "a freeze" to { proto.require(FreezeComponent::class).freeze() },
            "a teleport" to {
                proto.require(TeleportableComponent::class).teleporter = mockk<ITeleporterEntity>(relaxed = true)
            },
            "a room transition" to { proto.services.levelState.roomTransition = true },
            "out of play" to { proto.setPlayPhase(PlayPhase.PRE_PLAY) }
        ).forEach { (name, breakGuard) ->
            it("ends the put-away at once on $name") {
                waitForGuard()
                release()
                breakGuard()
                step()
                proto.puttingAwayGuard shouldBe false
            }
        }

        it("plays no put-away when a break ends the guard") {
            waitForGuard()
            proto.require(FreezeComponent::class).freeze()
            step()
            proto.puttingAwayGuard shouldBe false
        }

        it("crouches while he guards on the ground: the damage box and the body box are 18 art px high, feet down") {
            place()
            damageable().rect().getHeight() shouldBe (48f plusOrMinus TOLERANCE)
            step(MegaControllerButton.DOWN to ButtonStatus.JUST_PRESSED)
            guardStep()
            proto.guarding shouldBe false
            place()
            damageable().rect().getHeight() shouldBe (36f plusOrMinus TOLERANCE)
            damageable().rect().getY() shouldBe (-24f plusOrMinus TOLERANCE)
            bodyBox().rect().getHeight() shouldBe (36f plusOrMinus TOLERANCE)
            bodyBox().rect().getY() shouldBe (-24f plusOrMinus TOLERANCE)
            waitForGuard()
            place()
            damageable().rect().getHeight() shouldBe (36f plusOrMinus TOLERANCE)
            body.getHeight() shouldBe (ProtomanValues.BODY_HEIGHT * ConstVals.PPM plusOrMinus TOLERANCE)
        }

        it("stands up at once on the release, also while the put-away plays") {
            waitForGuard()
            release()
            proto.puttingAwayGuard shouldBe true
            place()
            damageable().rect().getHeight() shouldBe (48f plusOrMinus TOLERANCE)
            bodyBox().rect().getHeight() shouldBe (48f plusOrMinus TOLERANCE)
            bodyBox().rect().getY() shouldBe (-24f plusOrMinus TOLERANCE)
        }

        it("does not crouch in the air guard") {
            overGround = false
            waitForGuard()
            place()
            damageable().rect().getHeight() shouldBe (48f plusOrMinus TOLERANCE)
            bodyBox().rect().getHeight() shouldBe (48f plusOrMinus TOLERANCE)
        }

        it("lets a shot at head height pass over the crouch, and not over the stand") {
            fun hitsHim(bullet: Bullet) =
                bullet.fixture(FixtureType.DAMAGER).getShape().overlaps(damageable().getShape()) ||
                    bullet.fixture(FixtureType.PROJECTILE).getShape().overlaps(bodyBox().getShape()) ||
                    bullet.fixture(FixtureType.PROJECTILE).getShape().overlaps(shield().getShape())
            place()
            val headHeight = 20f
            hitsHim(enemyBullet(0f, headHeight, -12f * ConstVals.PPM)) shouldBe true
            waitForGuard()
            place()
            hitsHim(enemyBullet(0f, headHeight, -12f * ConstVals.PPM)) shouldBe false
        }

        fun IFixture.feetEdge(gravity: Direction) = rect().let {
            when (gravity) {
                Direction.UP -> it.getY()
                Direction.DOWN -> it.getMaxY()
                Direction.LEFT -> it.getMaxX()
                Direction.RIGHT -> it.getX()
            }
        }

        Direction.entries.forEach { gravity ->
            it("keeps the feet of the crouch boxes on the floor with the gravity $gravity") {
                proto.direction = gravity
                place()
                val damageFeet = damageable().feetEdge(gravity)
                val bodyFeet = bodyBox().feetEdge(gravity)
                waitForGuard()
                place()
                damageable().feetEdge(gravity) shouldBe (damageFeet plusOrMinus TOLERANCE)
                bodyBox().feetEdge(gravity) shouldBe (bodyFeet plusOrMinus TOLERANCE)
                val across = if (gravity.isVertical()) damageable().rect().getHeight() else damageable().rect().getWidth()
                across shouldBe (36f plusOrMinus TOLERANCE)
                val bodyAcross = if (gravity.isVertical()) bodyBox().rect().getHeight() else bodyBox().rect().getWidth()
                bodyAcross shouldBe (36f plusOrMinus TOLERANCE)
            }
        }

        it("is up in the air with the jump keys, and keeps his arc") {
            overGround = false
            body.physics.velocity.set(3f * ConstVals.PPM, 5f * ConstVals.PPM)
            step(MegaControllerButton.DOWN to ButtonStatus.JUST_PRESSED)
            body.physics.velocity.x shouldBe (3f * ConstVals.PPM plusOrMinus TOLERANCE)
            proto.animationKey("jump", ObjectMap()) shouldBe "guard_jump_start"
            var time = DELTA
            while (time < ProtomanValues.GUARD_JUMP_PULL_OUT_TIME + DELTA + TOLERANCE) {
                guardStep()
                time += DELTA
            }
            proto.guarding shouldBe true
            proto.animationKey("guard_jump_start", ObjectMap()) shouldBe "guard_jump"
        }

        it("stops a run on the ground, and left and right only turn him") {
            hold(MegaControllerButton.RIGHT, 0.3f)
            step(
                MegaControllerButton.DOWN to ButtonStatus.JUST_PRESSED,
                MegaControllerButton.RIGHT to ButtonStatus.PRESSED
            )
            proto.running shouldBe false
            body.physics.velocity.x shouldBe 0f
            guardStep(MegaControllerButton.RIGHT to ButtonStatus.PRESSED)
            guardStep(MegaControllerButton.LEFT to ButtonStatus.JUST_PRESSED)
            guardStep(MegaControllerButton.LEFT to ButtonStatus.PRESSED)
            guardStep(MegaControllerButton.LEFT to ButtonStatus.PRESSED)
            proto.facing shouldBe Facing.LEFT
            proto.running shouldBe false
            body.physics.velocity.x shouldBe 0f
            proto.guarding shouldBe true
        }

        it("does not attack on B while DOWN is held in the air, with either slot") {
            overGround = false
            step(MegaControllerButton.DOWN to ButtonStatus.JUST_PRESSED, MegaControllerButton.B to ButtonStatus.JUST_PRESSED)
            proto.sabre.active shouldBe false
            waitForGuard()
            guardStep(MegaControllerButton.B to ButtonStatus.JUST_PRESSED)
            proto.sabre.active shouldBe false
            guardStep(MegaControllerButton.SELECT to ButtonStatus.JUST_RELEASED)
            proto.weapon shouldBe ProtomanWeapon.BUSTER
            guardStep(MegaControllerButton.B to ButtonStatus.JUST_PRESSED)
            spawned.none { it.first is ProtoBuster } shouldBe true
            proto.guarding shouldBe true
        }

        describe("the crouch attacks") {
            fun pressB() = guardStep(MegaControllerButton.B to ButtonStatus.JUST_PRESSED)

            fun useBuster() = step(MegaControllerButton.SELECT to ButtonStatus.JUST_RELEASED)

            fun shots() = spawned.filter { it.first is ProtoBuster }

            fun crouchedBoxes(): Boolean {
                place()
                return damageable().rect().getHeight() == 36f && bodyBox().rect().getHeight() == 36f
            }

            fun runOutAttack(held: Boolean, check: () -> Unit = {}) {
                var steps = 0
                while ((proto.sabre.active || proto.buster.shooting) && steps < 40) {
                    check()
                    if (held) guardStep() else step()
                    steps++
                }
            }

            it("starts one crouch slash on B with DOWN held, in the pull-out and in the guard, at half strength") {
                step(MegaControllerButton.DOWN to ButtonStatus.JUST_PRESSED)
                pressB()
                proto.sabre.state shouldBe SabreState.CROUCH
                proto.sabre.move shouldBe ProtomanSabreMoves.CROUCH
                ProtomanSabreMoves.CROUCH.fullStrength shouldBe false
                runOutAttack(held = true)
                waitForGuard()
                pressB()
                proto.sabre.move shouldBe ProtomanSabreMoves.CROUCH
                spawned.count { it.first is PlayerSabreSlash } shouldBe 2
            }

            it("does not chain the crouch slash, and B after it ends starts another") {
                step(MegaControllerButton.DOWN to ButtonStatus.JUST_PRESSED)
                pressB()
                pressB()
                proto.sabre.chain() shouldBe false
                runOutAttack(held = true)
                proto.sabre.active shouldBe false
                pressB()
                proto.sabre.move shouldBe ProtomanSabreMoves.CROUCH
                spawned.count { it.first is PlayerSabreSlash } shouldBe 2
            }

            it("neither runs nor turns during the crouch slash") {
                step(MegaControllerButton.DOWN to ButtonStatus.JUST_PRESSED)
                pressB()
                guardStep(MegaControllerButton.LEFT to ButtonStatus.JUST_PRESSED)
                guardStep(MegaControllerButton.LEFT to ButtonStatus.PRESSED)
                proto.sabre.active shouldBe true
                proto.facing shouldBe Facing.RIGHT
                proto.running shouldBe false
                body.physics.velocity.x shouldBe 0f
            }

            it("fires the standing shot from the crouch muzzle on B with DOWN held, in the crouch shoot pose") {
                useBuster()
                place()
                waitForGuard()
                pressB()
                val shot = shots().single()
                val position = shot.second.get(ConstKeys.POSITION, Vector2::class)!!
                position.x shouldBe (ProtomanValues.SHOT_CROUCH_OFFSET_X * ConstVals.PPM plusOrMinus TOLERANCE)
                position.y shouldBe (ProtomanValues.SHOT_CROUCH_OFFSET_Y * ConstVals.PPM plusOrMinus TOLERANCE)
                ProtomanValues.SHOT_CROUCH_OFFSET_X shouldBe (0.8125f plusOrMinus TOLERANCE)
                ProtomanValues.SHOT_CROUCH_OFFSET_Y shouldBe (-0.1875f plusOrMinus TOLERANCE)
                val trajectory = shot.second.get(ConstKeys.TRAJECTORY, Vector2::class)!!
                trajectory.x shouldBe (ProtomanValues.SHOT_SPEED * ConstVals.PPM plusOrMinus TOLERANCE)
                proto.animationKey("guard_crouch", ObjectMap()) shouldBe "crouch_shoot"
            }

            it("fires the crouch shot to the left from the mirrored muzzle") {
                useBuster()
                proto.facing = Facing.LEFT
                place()
                step(MegaControllerButton.DOWN to ButtonStatus.JUST_PRESSED)
                pressB()
                val position = shots().single().second.get(ConstKeys.POSITION, Vector2::class)!!
                position.x shouldBe (-ProtomanValues.SHOT_CROUCH_OFFSET_X * ConstVals.PPM plusOrMinus TOLERANCE)
            }

            it("keeps the buster cooldown for the crouch shot") {
                useBuster()
                step(MegaControllerButton.DOWN to ButtonStatus.JUST_PRESSED)
                pressB()
                pressB()
                shots().size shouldBe 1
                proto.buster.update(ProtomanValues.SHOT_WAIT)
                pressB()
                shots().size shouldBe 2
            }

            it("lets left and right only turn him during the crouch shot") {
                useBuster()
                step(MegaControllerButton.DOWN to ButtonStatus.JUST_PRESSED)
                pressB()
                guardStep(MegaControllerButton.LEFT to ButtonStatus.JUST_PRESSED)
                guardStep(MegaControllerButton.LEFT to ButtonStatus.PRESSED)
                proto.buster.shooting shouldBe true
                proto.facing shouldBe Facing.LEFT
                proto.running shouldBe false
                body.physics.velocity.x shouldBe 0f
            }

            listOf(ProtomanWeapon.SABRE, ProtomanWeapon.BUSTER).forEach { weapon ->
                it("drops the guard at the $weapon crouch attack and keeps the crouch boxes during it") {
                    if (weapon == ProtomanWeapon.BUSTER) useBuster()
                    waitForGuard()
                    proto.guarding shouldBe true
                    pressB()
                    runOutAttack(held = true) {
                        shield().isActive() shouldBe false
                        proto.guarding shouldBe false
                        proto.puttingAwayGuard shouldBe false
                        crouchedBoxes() shouldBe true
                    }
                }

                it("runs the full pull-out again after the $weapon crouch attack with DOWN still held") {
                    if (weapon == ProtomanWeapon.BUSTER) useBuster()
                    waitForGuard()
                    pressB()
                    runOutAttack(held = true)
                    proto.guarding shouldBe false
                    proto.animationKey("stand", ObjectMap()) shouldBe "guard_crouch_start"
                    crouchedBoxes() shouldBe true
                    var steps = 0
                    while (!proto.guarding && steps < 20) {
                        guardStep()
                        steps++
                    }
                    steps shouldBe ProtomanValues.GUARD_CROUCH_TURN_FRAMES
                }

                it("stays crouched to the end of the $weapon crouch attack after DOWN is released, then stands") {
                    if (weapon == ProtomanWeapon.BUSTER) useBuster()
                    waitForGuard()
                    pressB()
                    release()
                    runOutAttack(held = false) { crouchedBoxes() shouldBe true }
                    step()
                    crouchedBoxes() shouldBe false
                    proto.puttingAwayGuard shouldBe false
                    proto.animationKey("stand", ObjectMap()) shouldBe "stand"
                }
            }

            it("shows the crouch slash body frame") {
                step(MegaControllerButton.DOWN to ButtonStatus.JUST_PRESSED)
                pressB()
                proto.animationKey("guard_crouch_start", ObjectMap()) shouldBe "crouch_slash"
            }

            it("ends the crouch slash on a jump after DOWN is released") {
                step(MegaControllerButton.DOWN to ButtonStatus.JUST_PRESSED)
                pressB()
                release()
                proto.sabre.state shouldBe SabreState.CROUCH
                frame = InputFrame.RELEASED.with(MegaControllerButton.A, ButtonStatus.JUST_PRESSED)
                val jump = proto.defineBehaviorsComponent().getBehavior(BehaviorType.JUMPING)!!
                jump.evaluate(0f) shouldBe true
                jump.init()
                overGround = false
                step()
                proto.sabre.active shouldBe false
                crouchedBoxes() shouldBe false
            }

            it("fires a crouch shot on DOWN and B during a standing shot pose") {
                useBuster()
                place()
                step(MegaControllerButton.B to ButtonStatus.JUST_PRESSED)
                proto.buster.update(ProtomanValues.SHOT_WAIT)
                proto.buster.shooting shouldBe true
                step(MegaControllerButton.DOWN to ButtonStatus.JUST_PRESSED, MegaControllerButton.B to ButtonStatus.JUST_PRESSED)
                shots().size shouldBe 2
                val position = shots()[1].second.get(ConstKeys.POSITION, Vector2::class)!!
                position.x shouldBe (ProtomanValues.SHOT_CROUCH_OFFSET_X * ConstVals.PPM plusOrMinus TOLERANCE)
                position.y shouldBe (ProtomanValues.SHOT_CROUCH_OFFSET_Y * ConstVals.PPM plusOrMinus TOLERANCE)
                proto.animationKey("stand", ObjectMap()) shouldBe "crouch_shoot"
            }

            it("does a crouch slash, not the UP slash, on UP, DOWN and B on the ground with the sabre slot") {
                step(MegaControllerButton.DOWN to ButtonStatus.JUST_PRESSED)
                guardStep(MegaControllerButton.UP to ButtonStatus.PRESSED, MegaControllerButton.B to ButtonStatus.JUST_PRESSED)
                proto.sabre.state shouldBe SabreState.CROUCH
                proto.sabre.move shouldBe ProtomanSabreMoves.CROUCH
            }

            it("fires a crouch shot, not the UP slash, on UP, DOWN and B on the ground with the buster slot") {
                useBuster()
                place()
                step(MegaControllerButton.DOWN to ButtonStatus.JUST_PRESSED)
                guardStep(MegaControllerButton.UP to ButtonStatus.PRESSED, MegaControllerButton.B to ButtonStatus.JUST_PRESSED)
                proto.sabre.active shouldBe false
                val position = shots().single().second.get(ConstKeys.POSITION, Vector2::class)!!
                position.y shouldBe (ProtomanValues.SHOT_CROUCH_OFFSET_Y * ConstVals.PPM plusOrMinus TOLERANCE)
            }

            mapOf<String, () -> Unit>(
                "a hit" to { hurt() },
                "a room transition" to {
                    proto.addComponent(RoomTransitionCarryComponent(ConstVals.ROOM_TRANS_DELAY_DURATION) { false })
                    proto.services.levelState.roomTransition = true
                    proto.onEvent(Event(EventType.BEGIN_ROOM_TRANS, props(ConstKeys.POSITION pairTo Vector2())))
                }
            ).forEach { (name, breakAttack) ->
                listOf(ProtomanWeapon.SABRE, ProtomanWeapon.BUSTER).forEach { weapon ->
                    it("ends the $weapon crouch attack on $name and puts the stand boxes back") {
                        if (weapon == ProtomanWeapon.BUSTER) useBuster()
                        step(MegaControllerButton.DOWN to ButtonStatus.JUST_PRESSED)
                        pressB()
                        crouchedBoxes() shouldBe true
                        breakAttack()
                        guardStep()
                        proto.sabre.active shouldBe false
                        proto.buster.shooting shouldBe false
                        crouchedBoxes() shouldBe false
                    }
                }
            }
        }

        it("is off during a slash") {
            step(MegaControllerButton.B to ButtonStatus.JUST_PRESSED)
            step(MegaControllerButton.DOWN to ButtonStatus.JUST_PRESSED)
            guardStep()
            guardStep()
            proto.sabre.active shouldBe true
            proto.guarding shouldBe false
        }

        it("is off during a shot") {
            step(MegaControllerButton.SELECT to ButtonStatus.JUST_RELEASED)
            step(MegaControllerButton.B to ButtonStatus.JUST_PRESSED)
            step(MegaControllerButton.DOWN to ButtonStatus.JUST_PRESSED)
            proto.buster.shooting shouldBe true
            while (proto.buster.shooting) {
                proto.guarding shouldBe false
                guardStep()
            }
            guardStep()
            proto.guarding shouldBe false
        }

        it("is off after a hit, and stays off while he is damaged") {
            waitForGuard()
            hurt()
            proto.guarding shouldBe false
            waitForGuard()
            proto.guarding shouldBe false
        }

        it("is off while frozen, teleporting or in a room transition") {
            waitForGuard()
            proto.require(FreezeComponent::class).freeze()
            proto.guarding shouldBe false
            proto.require(FreezeComponent::class).reset()
            waitForGuard()
            proto.require(TeleportableComponent::class).teleporter = mockk<ITeleporterEntity>(relaxed = true)
            proto.guarding shouldBe false
            proto.require(TeleportableComponent::class).teleporter = null
            waitForGuard()
            proto.services.levelState.roomTransition = true
            proto.guarding shouldBe false
            guardStep()
            proto.services.levelState.roomTransition = false
            proto.guarding shouldBe false
        }

        fun shieldCenter(): Vector2 = shield().rect().getCenter(Vector2())

        it("is the round 34 crouch box (7x18 art px), in front of him, on the ground with the gravity up") {
            waitForGuard()
            place()
            shield().rect().getWidth() shouldBe (14f plusOrMinus TOLERANCE)
            shield().rect().getHeight() shouldBe (36f plusOrMinus TOLERANCE)
            shieldCenter().x shouldBe (21f plusOrMinus TOLERANCE)
            shieldCenter().y shouldBe (-6f plusOrMinus TOLERANCE)
        }

        it("is the round 34 jump box (7x18 art px) in the air, with the air sprite drop") {
            overGround = false
            waitForGuard()
            place()
            shield().rect().getWidth() shouldBe (14f plusOrMinus TOLERANCE)
            shield().rect().getHeight() shouldBe (36f plusOrMinus TOLERANCE)
            shieldCenter().x shouldBe (23f plusOrMinus TOLERANCE)
            shieldCenter().y shouldBe (2f plusOrMinus TOLERANCE)
        }

        it("covers his crouched body height: its top edge is the top edge of the damage box") {
            waitForGuard()
            place()
            shield().rect().getMaxY() shouldBe (damageable().rect().getMaxY() plusOrMinus TOLERANCE)
        }

        Direction.entries.forEach { gravity ->
            it("turns with the gravity $gravity, on his facing side") {
                waitForGuard()
                proto.direction = gravity
                place()
                val expected = Vector2(21f, -6f).rotateDeg(gravity.rotation)
                val center = shieldCenter()
                center.x shouldBe (expected.x plusOrMinus TOLERANCE)
                center.y shouldBe (expected.y plusOrMinus TOLERANCE)
                val (width, height) = if (gravity.isHorizontal()) 36f to 14f else 14f to 36f
                shield().rect().getWidth() shouldBe (width plusOrMinus TOLERANCE)
                shield().rect().getHeight() shouldBe (height plusOrMinus TOLERANCE)
            }
        }

        it("puts the shield on his facing side, past the front of his body") {
            waitForGuard()
            place()
            shield().rect().getX() shouldBeGreaterThanOrEqual 0f
            shield().rect().getMaxX() shouldBeGreaterThanOrEqual
                damageable().rect().getMaxX() + ConstVals.PPM / 8f
            proto.facing = Facing.LEFT
            place()
            shield().rect().getMaxX() shouldBeLessThan 0f
        }

        it("bounces a Bullet from the front, with a white burst and no damage") {
            waitForGuard()
            place()
            val box = shield().rect()
            val bullet = enemyBullet(box.getMaxX(), box.getCenterY(), -12f * ConstVals.PPM)
            bullet.fixture(FixtureType.PROJECTILE).getShape().overlaps(shield().getShape()) shouldBe true
            bullet.fixture(FixtureType.DAMAGER).getShape().overlaps(damageable().getShape()) shouldBe false

            ProjectileRule(proto.services.rules).onBegin(bullet.fixture(FixtureType.PROJECTILE), shield(), 0f)
            proto.updateDeflects()

            bullet.body.physics.velocity.x shouldBeGreaterThanOrEqual 12f * ConstVals.PPM
            bullet.require(AudioComponent::class).playSoundRequests.count { it.source == SoundAsset.DINK_SOUND } shouldBe 1
            sparks() shouldBe 1
            health() shouldBe ConstVals.MAX_HEALTH
        }

        it("shows the spark with no active explosion fixture") {
            waitForGuard()
            place()
            val box = shield().rect()
            val bullet = enemyBullet(box.getMaxX(), box.getCenterY(), -12f * ConstVals.PPM)
            ProjectileRule(proto.services.rules).onBegin(bullet.fixture(FixtureType.PROJECTILE), shield(), 0f)
            proto.updateDeflects()

            val (spark, sparkProps) = spawned.first { it.first is WhiteBurst }
            spark as WhiteBurst
            spark.init()
            spark.onSpawn(sparkProps)
            spark.body.preProcess()
            spark.body.getFixtures(Array<IFixture>(), FixtureType.EXPLOSION).first().isActive() shouldBe false
        }

        it("shows the spark one frame later for a bullet that follows its trajectory") {
            waitForGuard()
            place()
            val box = shield().rect()
            val bullet = enemyBullet(box.getMaxX(), box.getCenterY(), -12f * ConstVals.PPM, trajectory = true)
            bullet.body.preProcess()
            ProjectileRule(proto.services.rules).onBegin(bullet.fixture(FixtureType.PROJECTILE), shield(), 0f)
            proto.updateDeflects()
            sparks() shouldBe 0
            bullet.body.preProcess()
            proto.updateDeflects()
            sparks() shouldBe 1
        }

        it("shows no spark for a projectile with no shield reaction") {
            waitForGuard()
            place()
            val box = shield().rect()
            val picket = Picket(proto.services)
            picket.init()
            picket.initialized = true
            picket.onSpawn(
                props(
                    ConstKeys.OWNER pairTo MetaWheelF(proto.services),
                    ConstKeys.POSITION pairTo Vector2(box.getMaxX(), box.getCenterY()),
                    ConstKeys.IMPULSE pairTo Vector2(-8f * ConstVals.PPM, 0f)
                )
            )
            picket.existenceState = EntityExistenceState.LIVE
            val picketFixture = picket.body.getFixtures(Array<IFixture>(), FixtureType.PROJECTILE).first()
            ProjectileRule(proto.services.rules).onBegin(picketFixture, shield(), 0f)
            proto.updateDeflects()
            picket.body.preProcess()
            proto.updateDeflects()
            sparks() shouldBe 0
        }

        it("lets a Bullet from behind reach his body and hurt him") {
            waitForGuard()
            place()
            val back = damageable().rect()
            val bullet = enemyBullet(back.getX(), back.getCenterY() - 0.125f * ConstVals.PPM, 12f * ConstVals.PPM)
            bullet.fixture(FixtureType.PROJECTILE).getShape().overlaps(shield().getShape()) shouldBe false
            bullet.fixture(FixtureType.DAMAGER).getShape().overlaps(damageable().getShape()) shouldBe true

            DamageRule { _, _ -> false }.onBegin(bullet.fixture(FixtureType.DAMAGER), damageable(), 0f)

            (health() < ConstVals.MAX_HEALTH) shouldBe true
            spawned.none { it.first is WhiteBurst } shouldBe true
        }

        it("makes no burst for his own shot") {
            waitForGuard()
            place()
            val shot = enemyBullet(0f, 0f, -12f * ConstVals.PPM).also { it.owner = proto }
            shield().receiver(Receivers.HIT_BY_PROJECTILE)!!.invoke(shot)
            shot.body.physics.velocity.x *= -1f
            proto.updateDeflects()
            sparks() shouldBe 0
        }
    }

    describe("the X6 sabre targets") {
        fun pressB() = step(MegaControllerButton.B to ButtonStatus.JUST_PRESSED)

        fun runOut(move: SabreMove) {
            var time = 0f
            while (time < move.duration) {
                step()
                time += DELTA
            }
        }

        fun halfSlash(): PlayerSabreSlash {
            pressB()
            return proto.sabre.slash!!.also { it.body.preProcess() }
        }

        fun fullSlash(): PlayerSabreSlash {
            pressB()
            pressB()
            runOut(ProtomanSabreMoves.SLASH_1)
            pressB()
            runOut(ProtomanSabreMoves.SLASH_2)
            proto.sabre.move shouldBe ProtomanSabreMoves.SLASH_3
            return proto.sabre.slash!!.also { it.body.preProcess() }
        }

        fun <T : AbstractHealthEntity> spawnTarget(target: T): T {
            target.init()
            target.initialized = true
            target.onSpawn(
                props(ConstKeys.BOUNDS pairTo GameRectangle(0f, 0f, 2f * ConstVals.PPM, 2f * ConstVals.PPM))
            )
            return target
        }

        fun damageOf(target: AbstractHealthEntity, slash: PlayerSabreSlash): Int {
            target.setHealthToMax()
            target.takeDamageFrom(slash) shouldBe true
            return target.getMaxHealth() - target.getCurrentHealth()
        }

        beforeEach {
            proto.services.levelState.worldContainer = SimpleGridWorldContainer(ConstVals.PPM)
        }

        it("Junkroid takes 16 from a half hit and 23 from a full hit") {
            damageOf(spawnTarget(Junkroid(proto.services)), halfSlash()) shouldBe 16
            proto.sabre.reset()
            damageOf(spawnTarget(Junkroid(proto.services)), fullSlash()) shouldBe 23
        }

        it("HoverGunner takes 20 from a half hit and 30 from a full hit") {
            damageOf(spawnTarget(HoverGunner(proto.services)), halfSlash()) shouldBe 20
            proto.sabre.reset()
            damageOf(spawnTarget(HoverGunner(proto.services)), fullSlash()) shouldBe 30
        }

        it("MetaWheelF takes 6 from a half hit and 9 from a full hit") {
            damageOf(spawnTarget(MetaWheelF(proto.services)), halfSlash()) shouldBe 6
            proto.sabre.reset()
            damageOf(spawnTarget(MetaWheelF(proto.services)), fullSlash()) shouldBe 9
        }

        fun hitsToBreak(slash: () -> PlayerSabreSlash): Int {
            val block = SabreBlock(proto.services)
            block.init()
            block.initialized = true
            block.onSpawn(
                props(ConstKeys.BOUNDS pairTo GameRectangle(0f, 0f, 2f * ConstVals.PPM, 2f * ConstVals.PPM))
            )
            val damager = slash()
            var hits = 0
            while (hits < ConstVals.MAX_HEALTH) {
                block.takeDamageFrom(damager) shouldBe true
                hits++
                if (spawned.any { it.first is Explosion }) return hits
                block.require(UpdatablesComponent::class).updatables.values().forEach { it.update(10f) }
            }
            return hits
        }

        it("SabreBlock takes 6 from a half hit and 9 from a full hit") {
            hitsToBreak { halfSlash() } shouldBe 5
            proto.sabre.reset()
            spawned.clear()
            hitsToBreak { fullSlash() } shouldBe 4
        }
    }

    describe("the animation key") {
        val animations = ObjectMap<String, IAnimation>()
        animations.put("run_trans", Animation(gdxArrayOf(TextureRegion()), ProtomanValues.STEP_TIME, false))

        it("shows the step when he stops, then stands") {
            animations["run_trans"].reset()
            proto.animationKey("run", animations) shouldBe "run_trans"
            proto.animationKey("run_trans", animations) shouldBe "run_trans"
            animations["run_trans"].update(ProtomanValues.STEP_TIME)
            proto.animationKey("run_trans", animations) shouldBe "stand"
            proto.animationKey("stand", animations) shouldBe "stand"
        }

        it("shows the step at the spawn and on a landing") {
            animations["run_trans"].reset()
            proto.animationKey(null, animations) shouldBe "run_trans"
            proto.animationKey("jump", animations) shouldBe "run_trans"
        }

        it("runs and jumps") {
            hold(MegaControllerButton.RIGHT, 0.1f)
            proto.animationKey("stand", animations) shouldBe "run"
            overGround = false
            proto.animationKey("run", animations) shouldBe "jump"
        }

        it("shows the damage key while damaged") {
            hurt()
            proto.animationKey("stand", animations) shouldBe "damaged"
        }

        it("shows the shoot key of each pose while the shot frame lasts") {
            step(MegaControllerButton.SELECT to ButtonStatus.JUST_RELEASED)
            step(MegaControllerButton.B to ButtonStatus.JUST_PRESSED)
            animations["run_trans"].reset()
            proto.animationKey("stand", animations) shouldBe "stand_shoot"
            proto.animationKey("stand_shoot", animations) shouldBe "stand_shoot"
            proto.animationKey("run", animations) shouldBe "run_trans_shoot"
            hold(MegaControllerButton.RIGHT, 0.1f)
            proto.animationKey("run_shoot", animations) shouldBe "run_shoot"
            overGround = false
            proto.animationKey("run_shoot", animations) shouldBe "jump_shoot"
            proto.buster.update(ProtomanValues.SHOT_POSE_TIME)
            proto.animationKey("jump_shoot", animations) shouldBe "jump"
        }
    }

    describe("the sprite") {
        fun drawn(): GameSprite {
            val sprites = proto.defineSpritesComponent()
            sprites.preProcess(0f)
            return sprites.sprites[Protoman.TAG]!!
        }

        it("shows in play") {
            val sprite = drawn()
            sprite.hidden shouldBe false
            sprite.color.a shouldBe 1f
        }

        it("hides out of play") {
            proto.setPlayPhase(PlayPhase.PRE_PLAY)
            drawn().hidden shouldBe true
        }

        it("shows while frozen, also out of play") {
            proto.require(FreezeComponent::class).freeze()
            proto.setPlayPhase(PlayPhase.PRE_PLAY)
            drawn().hidden shouldBe false
        }

        it("flashes in the damage recovery") {
            proto.damageRecovery.update(MegamanValues.DAMAGE_FLASH_DURATION, false, true)
            drawn().color.a shouldBe 0f
        }
    }
})
