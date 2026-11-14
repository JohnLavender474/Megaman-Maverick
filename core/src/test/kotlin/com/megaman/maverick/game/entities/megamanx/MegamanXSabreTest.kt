package com.megaman.maverick.game.entities.megamanx

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.audio.AudioComponent
import com.maverick.game.engine.behaviors.BehaviorsComponent
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.controller.ControllerSystem
import com.maverick.game.engine.controller.buttons.ButtonStatus
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.entities.EntityExistenceState
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.input.InputFrame
import com.maverick.game.engine.input.InputSource
import com.maverick.game.engine.locks.LocksComponent
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.points.Points
import com.maverick.game.engine.points.PointsComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.Fixture
import com.maverick.game.engine.world.container.SimpleGridWorldContainer
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.blocks.SabreBlock
import com.megaman.maverick.game.entities.bosses.D1000
import com.megaman.maverick.game.entities.contracts.AbstractHealthEntity
import com.megaman.maverick.game.entities.enemies.Junkroid
import com.megaman.maverick.game.entities.projectiles.ProtoBuster
import com.megaman.maverick.game.entities.enemies.HoverGunner
import com.megaman.maverick.game.entities.enemies.MetaWheelF
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.megaman.constants.MegaChargeStatus
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.state.*
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.locks.MegaLockGroup
import com.megaman.maverick.game.testServices
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.setBodySense
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import java.io.File
import javax.imageio.ImageIO
import com.megaman.maverick.game.players.playerChargeStatus
import com.megaman.maverick.game.players.SabreState
import com.megaman.maverick.game.entities.players.PlayerSabreSlash

private const val DELTA = 0.01f
private const val TOLERANCE = 0.001f

private fun PlayerSabreSlash.hitBox() =
    body.getFixtures(com.badlogic.gdx.utils.Array<com.maverick.game.engine.world.body.IFixture>(), FixtureType.DAMAGER).first() as Fixture

class MegamanXSabreTest : DescribeSpec({

    lateinit var x: MegamanX
    lateinit var services: MegaServices
    lateinit var engine: GameEngine
    lateinit var body: Body
    lateinit var behaviors: BehaviorsComponent
    lateinit var controllerSystem: ControllerSystem
    lateinit var spawned: MutableList<Pair<IGameEntity, Properties>>
    var frame = InputFrame.RELEASED
    var overGround = true

    val order = listOf(
        BehaviorType.WALL_SLIDING, BehaviorType.GROUND_DASHING, BehaviorType.JUMPING, BehaviorType.AIR_DASHING
    )

    fun hold(vararg pairs: Pair<MegaControllerButton, ButtonStatus>) {
        frame = InputFrame.RELEASED
        pairs.forEach { (button, status) -> frame = frame.with(button, status) }
    }

    fun tick(delta: Float = DELTA) {
        controllerSystem.update(delta)
        order.forEach { behaviors.getBehavior(it)!!.update(delta) }
        x.buster.update(delta)
        x.sabre.update(delta)
        x.kickLockTimer.update(delta)
        x.pushOffTimer.update(delta)
        x.wallDelayTimer.update(delta)
        x.endKickLockOnBlock()
        x.keepStartPresses()
        x.resetAirMovesOnLanding()
    }

    fun ticks(seconds: Float, vararg pairs: Pair<MegaControllerButton, ButtonStatus>) {
        hold(*pairs)
        var time = 0f
        while (time < seconds - TOLERANCE) {
            tick()
            time += DELTA
        }
    }

    fun active(type: BehaviorType) = behaviors.getBehavior(type)!!.isActive()

    fun hurtX() {
        x.takeDamageFrom(MetaWheelF(services).also { it.init() }) shouldBe true
    }

    fun slashes() = spawned.count { it.first is PlayerSabreSlash }
    fun bullets() = spawned.count { it.first is Bullet }
    fun chargedShots() = spawned.count { it.first is ChargedShot }

    fun slashUp() {
        hold(MegaControllerButton.UP to ButtonStatus.PRESSED, MegaControllerButton.B to ButtonStatus.JUST_PRESSED)
        tick()
    }

    fun slideOnLeftWall() {
        overGround = false
        body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_LEFT, true)
        hold(MegaControllerButton.LEFT to ButtonStatus.JUST_PRESSED)
        tick()
        ticks(MegamanXValues.WALL_GRAB_TIME + 2 * DELTA, MegaControllerButton.LEFT to ButtonStatus.PRESSED)
    }

    beforeEach {
        frame = InputFrame.RELEASED
        overGround = true
        spawned = mutableListOf()

        engine = mockk<GameEngine>(relaxed = true)
        every { engine.spawn(any(), any()) } answers {
            val entity = firstArg<IGameEntity>()
            val props = secondArg<Properties>()
            if (!entity.initialized) {
                entity.init()
                entity.initialized = true
            }
            entity.onSpawn(props)
            entity.existenceState = EntityExistenceState.LIVE
            spawned.add(entity to props)
            true
        }
        services = testServices(engine = engine)
        val atlas = mockk<TextureAtlas>()
        every { atlas.findRegion(any()) } returns AtlasRegion(mockk<Texture>(relaxed = true), 0, 0, 64, 64)
        every { services.assets.get(any<String>(), TextureAtlas::class.java) } returns atlas
        services.levelState.currentLevel = LevelDefinition.X6_INTRO
        services.levelState.worldContainer = SimpleGridWorldContainer(ConstVals.PPM)
        MegaEntityFactory.init(services)

        x = MegamanX(services)
        x.existenceState = EntityExistenceState.LIVE
        x.facing = Facing.RIGHT

        x.addComponent(x.defineBodyComponent())
        body = x.getComponent(BodyComponent::class)!!.body
        body.putProperty(ConstKeys.BODY_OVER_GROUND, { overGround })
        x.addComponent(RunComponent())
        x.addComponent(LocksComponent(MegaLockGroup.entries.size))
        x.addComponent(PlayPhaseComponent(PlayPhase.IN_PLAY))
        x.addComponent(TeleportableComponent())
        x.addComponent(x.damageRecovery)
        x.addComponent(x.defineStunComponent())
        x.addComponent(x.defineFreezeComponent())
        x.addComponent(x.defineWeaponChargeComponent())
        x.addComponent(PostActionMomentumComponent(0f, 0f, { 0f }, { overGround }, { false }, { false }))
        x.addComponent(AudioComponent())
        x.addComponent(PointsComponent(ConstKeys.HEALTH pairTo Points(0, ConstVals.MAX_HEALTH, ConstVals.MAX_HEALTH)))
        behaviors = x.defineBehaviorsComponent()
        x.addComponent(behaviors)

        services.players.register(PlayerSlot(0), x)
        x.addComponent(x.defineControllerComponent().also { it.input = InputSource { frame } })

        controllerSystem = ControllerSystem()
        controllerSystem.add(x)
    }

    afterEach {
        MegaEntityFactory.reset()
        unmockkAll()
    }

    describe("the input") {
        it("slashes on B with UP held, and fires no shot") {
            slashUp()

            x.sabre.state shouldBe SabreState.FLOOR
            slashes() shouldBe 1
            bullets() shouldBe 0
            spawned[0].second.get(ConstKeys.OWNER) shouldBe x
        }

        it("fires the buster on B alone") {
            hold(MegaControllerButton.B to ButtonStatus.JUST_PRESSED)
            tick()

            bullets() shouldBe 1
            slashes() shouldBe 0
        }

        it("does not charge while the slash press is held, and its release fires nothing") {
            slashUp()
            ticks(MegamanXValues.TIME_TO_FULL_CHARGE + DELTA, MegaControllerButton.B to ButtonStatus.PRESSED)
            x.playerChargeStatus shouldBe MegaChargeStatus.NOT_CHARGED

            hold(MegaControllerButton.B to ButtonStatus.JUST_RELEASED)
            tick()

            chargedShots() shouldBe 0
            bullets() shouldBe 0
        }

        it("charges again after the slash press is released") {
            slashUp()
            hold(MegaControllerButton.B to ButtonStatus.JUST_RELEASED)
            tick()
            ticks(1f)
            hold(MegaControllerButton.B to ButtonStatus.JUST_PRESSED)
            tick()
            ticks(MegamanXValues.TIME_TO_FULL_CHARGE + DELTA, MegaControllerButton.B to ButtonStatus.PRESSED)

            x.playerChargeStatus shouldBe MegaChargeStatus.FULLY_CHARGED
        }

        it("forgets the slash press when B is up, also with no release frame") {
            slashUp()
            hold()
            tick()

            x.sabre.pressHeld shouldBe false
        }

        it("does nothing when UP is pressed while B is held") {
            hold(MegaControllerButton.B to ButtonStatus.JUST_PRESSED)
            tick()
            hold(MegaControllerButton.B to ButtonStatus.PRESSED, MegaControllerButton.UP to ButtonStatus.JUST_PRESSED)
            tick()

            slashes() shouldBe 0
            x.sabre.active shouldBe false
        }

        it("fires no shot on a B press during a slash") {
            slashUp()
            hold(MegaControllerButton.B to ButtonStatus.JUST_RELEASED)
            tick()
            hold(MegaControllerButton.B to ButtonStatus.JUST_PRESSED)
            tick()

            bullets() shouldBe 0
            slashes() shouldBe 1
        }
    }

    describe("the start") {
        it("is an air slash in the air") {
            overGround = false
            slashUp()

            x.sabre.state shouldBe SabreState.AIR
        }

        it("is a wall slash on a wall slide") {
            slideOnLeftWall()
            hold(
                MegaControllerButton.LEFT to ButtonStatus.PRESSED, MegaControllerButton.UP to ButtonStatus.PRESSED,
                MegaControllerButton.B to ButtonStatus.JUST_PRESSED
            )
            tick()

            x.sabre.state shouldBe SabreState.WALL
        }

        it("does not start during the wall grab") {
            overGround = false
            body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_LEFT, true)
            hold(MegaControllerButton.LEFT to ButtonStatus.JUST_PRESSED)
            tick()
            hold(
                MegaControllerButton.LEFT to ButtonStatus.PRESSED, MegaControllerButton.UP to ButtonStatus.PRESSED,
                MegaControllerButton.B to ButtonStatus.JUST_PRESSED
            )
            tick()

            x.sabre.active shouldBe false
        }

        fun dashThenSlash(dashTime: Float) {
            hold(MegaControllerButton.RIGHT to ButtonStatus.JUST_PRESSED, MegaControllerButton.SELECT to ButtonStatus.JUST_PRESSED)
            tick()
            active(BehaviorType.GROUND_DASHING) shouldBe true
            ticks(dashTime, MegaControllerButton.RIGHT to ButtonStatus.PRESSED, MegaControllerButton.SELECT to ButtonStatus.PRESSED)
            hold(
                MegaControllerButton.RIGHT to ButtonStatus.PRESSED, MegaControllerButton.SELECT to ButtonStatus.PRESSED,
                MegaControllerButton.UP to ButtonStatus.PRESSED, MegaControllerButton.B to ButtonStatus.JUST_PRESSED
            )
            tick()
            body.preProcess()
        }

        it("ends the ground dash and starts a floor slash") {
            dashThenSlash(0.2f)

            x.sabre.state shouldBe SabreState.FLOOR
            active(BehaviorType.GROUND_DASHING) shouldBe false
            x.running shouldBe false
            body.physics.velocity.x shouldBe 0f
            bullets() shouldBe 0
        }

        it("ends the dash end slide and starts a floor slash") {
            dashThenSlash(MegamanXValues.DASH_START_DELAY + MegamanXValues.DASH_TIME + 2 * DELTA)

            x.sabre.state shouldBe SabreState.FLOOR
            active(BehaviorType.GROUND_DASHING) shouldBe false
            body.physics.velocity.x shouldBe 0f
            bullets() shouldBe 0
        }

        it("does not start while damaged") {
            hurtX()
            slashUp()

            x.sabre.active shouldBe false
        }
    }

    describe("the end") {
        it("ends after the floor frames, and destroys the slash") {
            slashUp()
            val slash = x.sabre.slash!!
            ticks(MegamanXSabreMoves.FLOOR.duration + DELTA)

            x.sabre.active shouldBe false
            x.sabre.slash shouldBe null
            verify { engine.destroy(slash) }
        }

        it("ends on damage") {
            slashUp()
            hurtX()
            tick()

            x.sabre.active shouldBe false
        }

        it("ends on the reset of X") {
            slashUp()
            val slash = x.sabre.slash!!
            x.sabre.reset()

            x.sabre.active shouldBe false
            verify { engine.destroy(slash) }
        }
    }

    describe("the floor slash") {
        it("stops X and does not turn X") {
            hold(MegaControllerButton.RIGHT to ButtonStatus.JUST_PRESSED)
            tick()
            hold(
                MegaControllerButton.RIGHT to ButtonStatus.PRESSED, MegaControllerButton.UP to ButtonStatus.PRESSED,
                MegaControllerButton.B to ButtonStatus.JUST_PRESSED
            )
            tick()
            hold(MegaControllerButton.LEFT to ButtonStatus.JUST_PRESSED)
            tick()

            x.facing shouldBe Facing.RIGHT
            x.running shouldBe false
            body.physics.velocity.x shouldBe 0f
        }

        it("blocks a jump before the cancel time") {
            slashUp()
            hold(MegaControllerButton.A to ButtonStatus.JUST_PRESSED)
            tick()

            active(BehaviorType.JUMPING) shouldBe false
            x.sabre.active shouldBe true
        }

        it("ends on a jump after the cancel time") {
            slashUp()
            ticks(MegamanXValues.SABRE_FLOOR_JUMP_CANCEL_TIME + DELTA)
            hold(MegaControllerButton.A to ButtonStatus.JUST_PRESSED)
            tick()

            active(BehaviorType.JUMPING) shouldBe true
            x.sabre.active shouldBe false
        }

        it("shows the body frames 1 to 7 by time") {
            MegamanXSabreMoves.FLOOR.frameAt(0f) shouldBe 1
            MegamanXSabreMoves.FLOOR.frameAt(0.06f) shouldBe 2
            MegamanXSabreMoves.FLOOR.frameAt(0.17f) shouldBe 5
            MegamanXSabreMoves.FLOOR.frameAt(MegamanXSabreMoves.FLOOR.duration) shouldBe 7
        }
    }

    describe("the air slash") {
        it("lets X turn") {
            overGround = false
            slashUp()
            hold(MegaControllerButton.LEFT to ButtonStatus.JUST_PRESSED)
            tick()

            x.facing shouldBe Facing.LEFT
            x.sabre.state shouldBe SabreState.AIR
        }

        it("does not cut the rise on a jump release") {
            hold(MegaControllerButton.A to ButtonStatus.JUST_PRESSED)
            tick()
            overGround = false
            hold(
                MegaControllerButton.A to ButtonStatus.PRESSED, MegaControllerButton.UP to ButtonStatus.PRESSED,
                MegaControllerButton.B to ButtonStatus.JUST_PRESSED
            )
            tick()
            x.sabre.state shouldBe SabreState.AIR
            hold(MegaControllerButton.A to ButtonStatus.JUST_RELEASED)
            tick()

            active(BehaviorType.JUMPING) shouldBe true
            (body.physics.velocity.y > 0f) shouldBe true
        }

        it("gives no air dash") {
            overGround = false
            slashUp()
            hold(MegaControllerButton.SELECT to ButtonStatus.JUST_PRESSED)
            tick()

            active(BehaviorType.AIR_DASHING) shouldBe false
        }
    }

    describe("the wall slash") {
        fun wallSlash() {
            slideOnLeftWall()
            hold(
                MegaControllerButton.LEFT to ButtonStatus.PRESSED, MegaControllerButton.UP to ButtonStatus.PRESSED,
                MegaControllerButton.B to ButtonStatus.JUST_PRESSED
            )
            tick()
        }

        it("holds X still on the wall, also with the side released") {
            wallSlash()
            ticks(0.1f)

            active(BehaviorType.WALL_SLIDING) shouldBe true
            body.physics.velocity.y shouldBe 0f
            x.sabre.state shouldBe SabreState.WALL
        }

        it("ends on a wall kick") {
            wallSlash()
            hold(MegaControllerButton.LEFT to ButtonStatus.PRESSED, MegaControllerButton.A to ButtonStatus.JUST_PRESSED)
            tick()

            active(BehaviorType.JUMPING) shouldBe true
            x.kickLocked shouldBe true
            x.sabre.active shouldBe false
        }
    }

    describe("the kind switch") {
        fun sameSlash(slash: PlayerSabreSlash, state: SabreState) {
            x.sabre.slash shouldBe slash
            x.sabre.state shouldBe state
            slashes() shouldBe 1
            verify(exactly = 0) { engine.destroy(slash) }
        }

        it("turns a floor slash into an air slash off a ledge, in the same swing phase") {
            slashUp()
            ticks(0.12f)
            val slash = x.sabre.slash!!
            x.sabre.frame shouldBe 4
            overGround = false
            tick()

            x.sabre.state shouldBe SabreState.AIR
            (x.sabre.frame in 4..5) shouldBe true
            sameSlash(slash, SabreState.AIR)
        }

        it("turns an air slash into a floor slash on a landing, and stops X") {
            overGround = false
            hold(MegaControllerButton.RIGHT to ButtonStatus.JUST_PRESSED)
            tick()
            hold(
                MegaControllerButton.RIGHT to ButtonStatus.PRESSED, MegaControllerButton.UP to ButtonStatus.PRESSED,
                MegaControllerButton.B to ButtonStatus.JUST_PRESSED
            )
            tick()
            val slash = x.sabre.slash!!
            overGround = true
            hold(MegaControllerButton.RIGHT to ButtonStatus.PRESSED)
            tick()

            x.sabre.state shouldBe SabreState.FLOOR
            x.running shouldBe false
            body.physics.velocity.x shouldBe 0f
            sameSlash(slash, SabreState.FLOOR)
        }

        it("turns an air slash into a wall slash on a wall slide") {
            overGround = false
            slashUp()
            val slash = x.sabre.slash!!
            body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_LEFT, true)
            hold(MegaControllerButton.LEFT to ButtonStatus.JUST_PRESSED)
            tick()

            active(BehaviorType.WALL_SLIDING) shouldBe true
            x.sabre.state shouldBe SabreState.WALL
            sameSlash(slash, SabreState.WALL)
        }

        it("turns a wall slash into an air slash when the wall slide ends") {
            slideOnLeftWall()
            hold(
                MegaControllerButton.LEFT to ButtonStatus.PRESSED, MegaControllerButton.UP to ButtonStatus.PRESSED,
                MegaControllerButton.B to ButtonStatus.JUST_PRESSED
            )
            tick()
            val slash = x.sabre.slash!!
            body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_LEFT, false)
            hold()
            tick()

            active(BehaviorType.WALL_SLIDING) shouldBe false
            x.sabre.state shouldBe SabreState.AIR
            sameSlash(slash, SabreState.AIR)
        }

        it("keeps the largest arc as the largest arc") {
            MegamanXSabreMoves.FLOOR.frameAt(MegamanXSabreMoves.AIR.timeIn(MegamanXSabreMoves.FLOOR, 0.205f)) shouldBe 4
            MegamanXSabreMoves.AIR.frameAt(MegamanXSabreMoves.FLOOR.timeIn(MegamanXSabreMoves.AIR, 0.14f)) shouldBe 5
        }

        it("maps a wind-up to the first frame with art, and a recovery to the end") {
            MegamanXSabreMoves.AIR.timeIn(MegamanXSabreMoves.FLOOR, 0.04f) shouldBe 0f
            MegamanXSabreMoves.AIR.timeIn(MegamanXSabreMoves.FLOOR, 0.25f) shouldBe (MegamanXSabreMoves.FLOOR.duration plusOrMinus TOLERANCE)
        }

        it("never moves the swing back") {
            for (i in 0 until MegamanXSabreMoves.ALL.size) for (j in 0 until MegamanXSabreMoves.ALL.size) {
                val from = MegamanXSabreMoves.ALL[i]
                val to = MegamanXSabreMoves.ALL[j]
                var last = -1f
                var time = 0f
                while (time < from.duration) {
                    val mapped = from.timeIn(to, time)
                    (mapped >= last) shouldBe true
                    last = mapped
                    time += DELTA
                }
            }
        }
    }

    describe("the animation key") {
        it("is the body strip of the kind") {
            slashUp()
            x.animationKey() shouldBe "sabre_floor"
        }
    }

    describe("the hit box") {
        fun slashAtFrame3(facing: Facing): PlayerSabreSlash {
            x.facing = facing
            body.set(0f, 0f, MegamanXValues.BODY_WIDTH * ConstVals.PPM, MegamanXValues.BODY_HEIGHT * ConstVals.PPM)
            slashUp()
            ticks(0.09f)
            x.sabre.frame shouldBe 3
            return x.sabre.slash!!.also { it.body.preProcess() }
        }

        it("is the art bounds of the frame, in front of X") {
            val box = slashAtFrame3(Facing.RIGHT).hitBox().rawShape as GameRectangle
            val artPx = x.sabre.params.artPx
            val centerX = MegamanXValues.BODY_WIDTH * ConstVals.PPM / 2f

            box.getX() shouldBe ((53 - 56) * artPx + centerX plusOrMinus TOLERANCE)
            box.getWidth() shouldBe (52 * artPx plusOrMinus TOLERANCE)
            box.getHeight() shouldBe (34 * artPx plusOrMinus TOLERANCE)
        }

        it("is mirrored when X faces left") {
            val box = slashAtFrame3(Facing.LEFT).hitBox().rawShape as GameRectangle
            val artPx = x.sabre.params.artPx
            val centerX = MegamanXValues.BODY_WIDTH * ConstVals.PPM / 2f

            box.getMaxX() shouldBe ((56 - 53) * artPx + centerX plusOrMinus TOLERANCE)
        }

        it("is off on a frame with no slash art") {
            overGround = false
            slashUp()
            val slash = x.sabre.slash!!
            slash.body.preProcess()

            slash.hitBox().isActive() shouldBe false
        }

        it("matches the alpha bounds of the slash strips") {
            MegamanXSabreMoves.ALL.forEach { kind ->
                val image = ImageIO.read(File("../assets/sprites/frames/MegamanX/${kind.arcRegion}.png"))
                val boxes = kind.hitBoxes
                boxes.size shouldBe kind.stripFrames
                for (index in 0 until kind.stripFrames) {
                    var minX = Int.MAX_VALUE
                    var minY = Int.MAX_VALUE
                    var maxX = -1
                    var maxY = -1
                    for (py in 0 until kind.arcHeight) for (px in 0 until kind.arcWidth) {
                        val alpha = image.getRGB(px, index * kind.arcHeight + py) ushr 24
                        if (alpha != 0) {
                            minX = minOf(minX, px); maxX = maxOf(maxX, px)
                            minY = minOf(minY, py); maxY = maxOf(maxY, py)
                        }
                    }
                    val expected = if (maxX < 0) null
                    else listOf(minX, kind.arcHeight - 1 - maxY, maxX - minX + 1, maxY - minY + 1)
                    boxes[index]?.toList() shouldBe expected
                }
            }
        }
    }

    fun spawnTarget(target: AbstractHealthEntity) = target.also {
        it.spawn(props(ConstKeys.BOUNDS pairTo GameRectangle(0f, 0f, 2f * ConstVals.PPM, 2f * ConstVals.PPM)))
    }

    fun slash() = PlayerSabreSlash(services).also {
        it.spawn(props(ConstKeys.OWNER pairTo x, ConstKeys.PARENT pairTo x.sabre))
    }

    fun damageOf(target: AbstractHealthEntity, damager: IDamagerEntity): Int {
        target.setHealthToMax()
        target.takeDamageFrom(damager) shouldBe true
        return target.getMaxHealth() - target.getCurrentHealth()
    }

    describe("the boss") {
        it("D1000 takes the shielded sabre damage") {
            val boss = spawnTarget(D1000(services))

            damageOf(boss, slash()) shouldBe D1000.SHIELDED_SABRE_DAMAGE
        }
    }

    describe("the damage") {
        it("gives the slash its target's sabre damage") {
            val wheel = spawnTarget(MetaWheelF(services))

            damageOf(wheel, slash()) shouldBe 9
        }

        it("lets only the slash damage a SabreBlock") {
            val block = SabreBlock(services)
            block.spawn(props(ConstKeys.BOUNDS pairTo GameRectangle(0f, 0f, 2f * ConstVals.PPM, 2f * ConstVals.PPM)))

            block.canBeDamagedBy(Bullet(services)) shouldBe false
            block.canBeDamagedBy(ChargedShot(services)) shouldBe false
            block.canBeDamagedBy(ChargedShotExplosion(services)) shouldBe false
            block.canBeDamagedBy(slash()) shouldBe true
            block.takeDamageFrom(slash()) shouldBe true
            block.invincible shouldBe true
        }

        it("lets only the slash damage a MetaWheelF") {
            val wheel = spawnTarget(MetaWheelF(services))

            wheel.canBeDamagedBy(Bullet(services)) shouldBe false
            wheel.canBeDamagedBy(ChargedShot(services)) shouldBe false
            wheel.canBeDamagedBy(ChargedShotExplosion(services)) shouldBe false
            wheel.canBeDamagedBy(slash()) shouldBe true
        }

        it("gives the slash and the buster their damage on a normal target") {
            val junkroid = spawnTarget(Junkroid(services))

            damageOf(junkroid, slash()) shouldBe 23
            damageOf(junkroid, Bullet(services)) shouldBe 8
        }

        it("keeps the HoverGunner's own slash and ProtoBuster damage") {
            val gunner = spawnTarget(HoverGunner(services))

            damageOf(gunner, slash()) shouldBe 30
            damageOf(gunner, ProtoBuster(services)) shouldBe 30
        }
    }
})
