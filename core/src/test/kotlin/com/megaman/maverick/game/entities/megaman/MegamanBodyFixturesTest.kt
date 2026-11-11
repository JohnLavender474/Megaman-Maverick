package com.megaman.maverick.game.entities.megaman

import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.audio.AudioComponent
import com.maverick.game.engine.behaviors.BehaviorsComponent
import com.maverick.game.engine.behaviors.IBehavior
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.enums.ProcessState
import com.maverick.game.engine.common.shapes.GameCircle
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.locks.LocksComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.maverick.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.bosses.GutsTank
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.megaman.components.defineBodyComponent
import com.megaman.maverick.game.entities.megaman.constants.AButtonTask
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.megaman.constants.MegamanWeapon
import com.megaman.maverick.game.entities.state.FreezeComponent
import com.megaman.maverick.game.entities.state.PlayPhase
import com.megaman.maverick.game.entities.state.PlayPhaseComponent
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.locks.MegaLockGroup
import com.megaman.maverick.game.testServices
import com.megaman.maverick.game.world.body.BodyLabel
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.addBodyLabel
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.Receivers
import com.megaman.maverick.game.world.body.receiver
import com.megaman.maverick.game.world.body.setBodySense
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import java.io.File
import java.util.Locale

class MegamanBodyFixturesTest : DescribeSpec({

    afterEach { unmockkAll() }

    describe("Megaman's body") {
        it("has the same fixtures, sizes, gravity and clamps in each state as the reference file") {
            val actual = snapshotAll()

            if (System.getProperty(UPDATE_PROPERTY) != null) {
                File(REFERENCE_PATH).writeText(actual)
            } else {
                val expected = MegamanBodyFixturesTest::class.java.getResource("/$REFERENCE_FILE")!!.readText()
                actual shouldBe expected
            }
        }
    }

    describe("Megaman's body hooks") {
        it("puts the fixture properties on the body and the sides") {
            val rig = rig()
            val body = rig.body

            (body.getProperty(ConstKeys.BODY) as IFixture).getType() shouldBe FixtureType.BODY
            (body.getProperty(ConstKeys.FEET) as IFixture).getType() shouldBe FixtureType.FEET
            (body.getProperty(ConstKeys.HEAD) as IFixture).getType() shouldBe FixtureType.HEAD
            (body.getProperty(ConstKeys.DAMAGEABLE) as IFixture).getType() shouldBe FixtureType.DAMAGEABLE
            (body.getProperty("${ConstKeys.FEET}_${ConstKeys.GRAVITY}") as IFixture).getType() shouldBe
                FixtureType.CONSUMER
            val left = body.getProperty("${ConstKeys.LEFT}_${ConstKeys.SIDE}") as IFixture
            val right = body.getProperty("${ConstKeys.RIGHT}_${ConstKeys.SIDE}") as IFixture
            left.getProperty(ConstKeys.SIDE) shouldBe ConstKeys.LEFT
            right.getProperty(ConstKeys.SIDE) shouldBe ConstKeys.RIGHT
            (left as Fixture).offsetFromBodyAttachment.y shouldBe 0.1f * ConstVals.PPM
        }

        it("ends the bounce behaviors and restarts the wall slide delay on a head bounce") {
            val rig = rig()
            val wallSlide = mockk<IBehavior>(relaxed = true)
            every { wallSlide.isActive() } returns true
            rig.behaviors.addBehavior(BehaviorType.WALL_SLIDING, wallSlide)
            rig.megaman.wallSlideNotAllowedTimer.setToEnd()

            rig.bounce(FixtureType.HEAD)

            verify(exactly = 1) { wallSlide.reset() }
            rig.megaman.wallSlideNotAllowedTimer.isFinished() shouldBe false
        }

        it("sets the air dash task on a feet bounce out of water, and keeps the task in water") {
            val dry = rig()
            dry.bounce(FixtureType.FEET)
            dry.megaman.aButtonTask shouldBe AButtonTask.AIR_DASH

            val wet = rig()
            wet.body.setBodySense(BodySense.IN_WATER, true)
            wet.bounce(FixtureType.FEET)
            wet.megaman.aButtonTask shouldBe AButtonTask.JUMP
        }

        it("does not stick a side to a block that GutsTank owns, and sticks to other blocks") {
            val rig = rig()
            val side = rig.body.getProperty("${ConstKeys.LEFT}_${ConstKeys.SIDE}") as IFixture
            val stick = side.receiver(Receivers.STICK_TO_BLOCK)!!

            fun blockOwnedBy(owner: Any?): IFixture {
                val entity = mockk<MegaGameEntity>(relaxed = true)
                every { entity.getProperty(ConstKeys.OWNER) } returns owner
                val fixture = Fixture(Body(BodyType.ABSTRACT), FixtureType.BLOCK)
                fixture.putProperty(ConstKeys.ENTITY, entity)
                return fixture
            }

            stick.invoke(ProcessState.BEGIN, blockOwnedBy(mockk<GutsTank>(relaxed = true))) shouldBe false
            stick.invoke(ProcessState.BEGIN, blockOwnedBy(null)) shouldBe true
        }

        it("places the axe shield from the slip slide speed before the out-of-play velocity zero") {
            val rig = rig()
            rig.megaman.currentWeapon = MegamanWeapon.AXE_SWINGER
            rig.megaman.getComponent(PlayPhaseComponent::class)!!.phase = PlayPhase.PRE_PLAY
            rig.body.setBodySense(BodySense.FEET_ON_GROUND, true)
            rig.body.physics.velocity.set(3f * ConstVals.PPM, 0f)

            rig.body.preProcess()

            val axe = rig.fixtures().first { it.getType() == FixtureType.SHIELD && it.hasProperty(ConstKeys.AXE) }
            (axe as Fixture).offsetFromBodyAttachment.x shouldBe 0.5f * ConstVals.PPM
            rig.body.physics.velocity.x shouldBe 0f
        }

        it("sets the jump task and plays the land sound once on a feet block contact") {
            val rig = rig()
            val audio = AudioComponent()
            rig.megaman.addComponent(audio)
            rig.megaman.aButtonTask = AButtonTask.AIR_DASH
            rig.megaman.canMakeLandSound = true
            val contact = rig.feet().receiver(Receivers.FEET_BLOCK_CONTACT)!!

            contact.invoke(ProcessState.BEGIN, block())
            rig.megaman.aButtonTask shouldBe AButtonTask.JUMP
            audio.playSoundRequests.map { it.source } shouldBe listOf(SoundAsset.MEGAMAN_LAND_SOUND)

            contact.invoke(ProcessState.BEGIN, block())
            audio.playSoundRequests.size shouldBe 1

            rig.megaman.aButtonTask = AButtonTask.AIR_DASH
            contact.invoke(ProcessState.CONTINUE, block())
            rig.megaman.aButtonTask shouldBe AButtonTask.JUMP
        }

        it("plays no land sound on a collide-down-only block while he moves up through it") {
            val rig = rig()
            val audio = AudioComponent()
            rig.megaman.addComponent(audio)
            rig.megaman.canMakeLandSound = true
            rig.body.physics.velocity.y = 2f * ConstVals.PPM

            rig.feet().receiver(Receivers.FEET_BLOCK_CONTACT)!!.invoke(ProcessState.BEGIN, block(collideDownOnly = true))

            audio.playSoundRequests.size shouldBe 0
            rig.megaman.canMakeLandSound shouldBe true
        }

        it("sets the air dash or swim task when the feet leave a block with no ground below") {
            val rig = rig()
            rig.overGround(false)
            val contact = rig.feet().receiver(Receivers.FEET_BLOCK_CONTACT)!!

            contact.invoke(ProcessState.END, block())
            rig.megaman.aButtonTask shouldBe AButtonTask.AIR_DASH
            rig.megaman.canMakeLandSound shouldBe true

            rig.body.setBodySense(BodySense.IN_WATER, true)
            contact.invoke(ProcessState.END, block())
            rig.megaman.aButtonTask shouldBe AButtonTask.SWIM

            rig.megaman.aButtonTask = AButtonTask.JUMP
            rig.overGround(true)
            contact.invoke(ProcessState.END, block())
            rig.megaman.aButtonTask shouldBe AButtonTask.JUMP
        }

        it("plays the bump sound on a head hit only in a Mario level while he moves up") {
            val rig = rig()
            val audio = AudioComponent()
            rig.megaman.addComponent(audio)
            val head = rig.fixtures().first { it.getType() == FixtureType.HEAD }
            val hit = head.receiver(Receivers.HIT_BY_BLOCK, ProcessState.BEGIN)!!
            rig.body.physics.velocity.y = 2f * ConstVals.PPM

            hit.invoke(block(), 0f)
            audio.playSoundRequests.size shouldBe 0

            rig.megaman.services.levelState.currentLevel = LevelDefinition.SMB3_1
            hit.invoke(block(), 0f)
            audio.playSoundRequests.map { it.source } shouldBe listOf(SoundAsset.SMB3_BUMP_SOUND)

            rig.body.physics.velocity.y = -2f * ConstVals.PPM
            hit.invoke(block(), 0f)
            audio.playSoundRequests.size shouldBe 1
        }

        it("sticks the feet to a block only out of water") {
            val rig = rig()
            val stick = rig.feet().receiver(Receivers.STICK_TO_BLOCK)!!
            val blockFixture = Fixture(Body(BodyType.ABSTRACT), FixtureType.BLOCK)

            stick.invoke(ProcessState.BEGIN, blockFixture) shouldBe true
            rig.body.setBodySense(BodySense.IN_WATER, true)
            stick.invoke(ProcessState.BEGIN, blockFixture) shouldBe false
        }

        it("filters out the ice friction only on the ground against a side block he faces") {
            val rig = rig()
            val filter = rig.feet().receiver(Receivers.ICE_FRICTION_FILTER)!!

            filter.invoke() shouldBe true
            rig.body.setBodySense(BodySense.FEET_ON_GROUND, true)
            rig.body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_RIGHT, true)
            filter.invoke() shouldBe false
            rig.megaman.facing = Facing.LEFT
            filter.invoke() shouldBe true
            rig.body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_LEFT, true)
            filter.invoke() shouldBe false
            rig.body.setBodySense(BodySense.FEET_ON_GROUND, false)
            filter.invoke() shouldBe true
        }

        it("stops the vertical speed and scales the gravity when he first enters water") {
            val rig = rig()
            rig.body.physics.velocity.set(3f * ConstVals.PPM, 4f * ConstVals.PPM)

            rig.water(ProcessState.BEGIN, wasInWater = true)
            rig.body.physics.velocity.y shouldBe 4f * ConstVals.PPM

            rig.water(ProcessState.BEGIN, wasInWater = false)
            rig.body.physics.velocity.y shouldBe 0f
            rig.body.physics.velocity.x shouldBe 3f * ConstVals.PPM
            rig.megaman.gravityScalar shouldBe MegamanValues.WATER_GRAVITY_SCALAR

            rig.water(ProcessState.END)
            rig.megaman.gravityScalar shouldBe 1f
        }
    }
}) {

    companion object {
        const val REFERENCE_FILE = "megaman-body-fixtures.txt"
        const val REFERENCE_PATH = "src/test/resources/$REFERENCE_FILE"
        const val UPDATE_PROPERTY = "updateMegamanBodyFixtures"
    }
}

private class Rig(val megaman: Megaman, val body: Body, val behaviors: BehaviorsComponent)

private fun rig(): Rig {
    val services = testServices(engine = mockk<GameEngine>(relaxed = true))
    services.levelState.currentLevel = LevelDefinition.DESERT_MAN

    val megaman = Megaman(services)
    megaman.weaponsHandler = mockk(relaxed = true)
    megaman.currentWeapon = MegamanWeapon.MEGA_BUSTER
    megaman.facing = Facing.RIGHT
    megaman.aButtonTask = AButtonTask.JUMP

    val behaviors = BehaviorsComponent()
    megaman.addComponent(behaviors)
    megaman.addComponent(LocksComponent(MegaLockGroup.entries.size))
    megaman.addComponent(PlayPhaseComponent(PlayPhase.IN_PLAY))
    megaman.addComponent(FreezeComponent(mockk(relaxed = true)) {})

    val bodyComponent = megaman.defineBodyComponent()
    megaman.addComponent(bodyComponent)

    return Rig(megaman, bodyComponent.body, behaviors)
}

private fun Rig.activate(type: BehaviorType) {
    val behavior = mockk<IBehavior>(relaxed = true)
    every { behavior.isActive() } returns true
    behaviors.addBehavior(type, behavior)
}

private fun Rig.overGround(value: Boolean) = body.putProperty(ConstKeys.BODY_OVER_GROUND, { value })

private fun Rig.fixtures(): Array<IFixture> = body.getFixtures(Array())

private fun Rig.feet(): IFixture = fixtures().first { it.getType() == FixtureType.FEET }

private fun block(collideDownOnly: Boolean = false): Block {
    val blockBody = Body(BodyType.STATIC)
    if (collideDownOnly) blockBody.addBodyLabel(BodyLabel.COLLIDE_DOWN_ONLY)
    val block = mockk<Block>(relaxed = true)
    every { block.body } returns blockBody
    every { block.getOrDefaultProperty(any(), any<Boolean>(), Boolean::class) } answers { secondArg() }
    return block
}

private fun Rig.water(state: ProcessState, wasInWater: Boolean = false) {
    val water = fixtures().first { it.getType() == FixtureType.WATER_LISTENER }
    water.receiver(Receivers.WATER_CONTACT)!!.invoke(state, wasInWater)
}

private fun Rig.bounce(type: FixtureType) {
    fixtures().first { it.getType() == type }.receiver(Receivers.RUNNABLE)!!.invoke()
}

private fun f(value: Float) = String.format(Locale.ROOT, "%.3f", value / ConstVals.PPM)

private fun Rig.snapshot(name: String): String {
    body.physics.velocity.set(3f * ConstVals.PPM, 4f * ConstVals.PPM)
    body.preProcess()

    val out = StringBuilder()
    out.append("## $name\n")
    out.append("body ${f(body.getWidth())} x ${f(body.getHeight())}, direction ${body.direction}\n")
    out.append("velocity ${f(body.physics.velocity.x)}, ${f(body.physics.velocity.y)}\n")
    out.append("gravity ${f(body.physics.gravity.x)}, ${f(body.physics.gravity.y)}\n")
    out.append("clamp ${f(body.physics.velocityClamp.x)}, ${f(body.physics.velocityClamp.y)}\n")
    out.append("friction ${body.physics.defaultFrictionOnSelf.x}, ${body.physics.defaultFrictionOnSelf.y}\n")
    out.append("gravityScalar ${megaman.gravityScalar}, aButtonTask ${megaman.aButtonTask}\n")
    fixtures().forEach { fixture ->
        fixture as Fixture
        val shape = when (val raw = fixture.rawShape) {
            is GameRectangle -> "rect ${f(raw.getWidth())} x ${f(raw.getHeight())}" +
                if (fixture.attachedToBody) "" else " at ${f(raw.getX())}, ${f(raw.getY())}"
            is GameCircle -> "circle r ${f(raw.getRadius())}"
            else -> raw::class.simpleName
        }
        out.append(
            "  ${fixture.getType()} $shape, offset ${f(fixture.offsetFromBodyAttachment.x)}, " +
                "${f(fixture.offsetFromBodyAttachment.y)}, active ${fixture.isActive()}, " +
                "attached ${fixture.attachedToBody}" +
                (fixture.getProperty("${ConstKeys.REFLECT}_${ConstKeys.DIRECTION}")?.let { ", reflect $it" } ?: "") +
                "\n"
        )
    }
    return out.toString()
}

private fun scenario(direction: Direction, name: String, setup: Rig.() -> Unit): String {
    val rig = rig()
    rig.megaman.direction = direction
    rig.overGround(false)
    rig.setup()
    return rig.snapshot("$direction $name")
}

private fun snapshotAll(): String {
    val out = StringBuilder()
    Direction.entries.forEach { direction ->
        out.append(scenario(direction, "stand on ground") { overGround(true) })
        out.append(scenario(direction, "jump") { activate(BehaviorType.JUMPING) })
        out.append(scenario(direction, "fall") { })
        out.append(scenario(direction, "fall frozen") { megaman.getComponent(FreezeComponent::class)!!.freeze() })
        out.append(scenario(direction, "ground slide") {
            overGround(true)
            activate(BehaviorType.GROUND_SLIDING)
        })
        out.append(scenario(direction, "crouch") {
            overGround(true)
            activate(BehaviorType.CROUCHING)
        })
        out.append(scenario(direction, "wall slide on ice") {
            activate(BehaviorType.WALL_SLIDING)
            body.setBodySense(BodySense.SIDE_TOUCHING_ICE_LEFT, true)
            body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_LEFT, true)
        })
        out.append(scenario(direction, "climb") { activate(BehaviorType.CLIMBING) })
        out.append(scenario(direction, "in water") {
            water(ProcessState.BEGIN)
            body.setBodySense(BodySense.IN_WATER, true)
        })
        out.append(scenario(direction, "in water, frozen") {
            water(ProcessState.BEGIN)
            body.setBodySense(BodySense.IN_WATER, true)
            megaman.getComponent(FreezeComponent::class)!!.freeze()
        })
        out.append(scenario(direction, "water end") {
            water(ProcessState.BEGIN)
            water(ProcessState.END)
        })
        out.append(scenario(direction, "needle spin") {
            megaman.currentWeapon = MegamanWeapon.NEEDLE_SPIN
            megaman.shooting = true
        })
        out.append(scenario(direction, "axe, facing left") {
            megaman.currentWeapon = MegamanWeapon.AXE_SWINGER
            megaman.facing = Facing.LEFT
        })
        out.append(scenario(direction, "axe, climbing") {
            megaman.currentWeapon = MegamanWeapon.AXE_SWINGER
            activate(BehaviorType.CLIMBING)
        })
        out.append(scenario(direction, "side touching block right") {
            body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_RIGHT, true)
        })
        out.append(scenario(direction, "head bounce") {
            activate(BehaviorType.WALL_SLIDING)
            bounce(FixtureType.HEAD)
        })
        out.append(scenario(direction, "not in play") {
            megaman.getComponent(PlayPhaseComponent::class)!!.phase = PlayPhase.PRE_PLAY
        })
    }
    return out.toString()
}
