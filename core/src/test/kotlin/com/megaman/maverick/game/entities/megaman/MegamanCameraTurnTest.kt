package com.megaman.maverick.game.entities.megaman

import com.badlogic.gdx.utils.ObjectSet
import com.maverick.game.engine.behaviors.IBehavior
import com.maverick.game.engine.camera.CameraFollow
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.events.IEventListener
import com.maverick.game.engine.locks.isLocked
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.locks.MegaLockGroup
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify

class MegamanCameraTurnTest : DescribeSpec({

    lateinit var rig: MegamanSpawnedRig
    lateinit var follow: CameraFollow
    lateinit var jetpack: IBehavior
    val turns = mutableListOf<Direction>()
    val gravityOnAtJetpackReset = mutableListOf<Boolean>()
    val velocityZeroAtJetpackReset = mutableListOf<Boolean>()

    beforeEach {
        rig = MegamanSpawnedRig()
        turns.clear()
        gravityOnAtJetpackReset.clear()
        velocityZeroAtJetpackReset.clear()
        follow = CameraFollow(0f, { 1f })
        rig.services.cameraController.addMover(follow)
        rig.services.events.addListener(object : IEventListener {
            override val eventKeyMask = ObjectSet<Any>().also { it.add(EventType.START_GAME_CAM_ROTATION) }
            override fun onEvent(event: Event) {
                turns.add(event.getProperty(ConstKeys.DIRECTION, Direction::class)!!)
            }
        })
        jetpack = mockk(relaxed = true)
        every { jetpack.reset() } answers {
            gravityOnAtJetpackReset.add(rig.body.physics.gravityOn)
            velocityZeroAtJetpackReset.add(rig.body.physics.velocity.isZero)
        }
        rig.behaviors.addBehavior(BehaviorType.JETPACKING, jetpack)
        rig.body.physics.gravityOn = true
        rig.body.physics.velocity.set(100f, 100f)
    }

    afterEach {
        unmockkAll()
        clearAllMocks(answers = false, recordedCalls = true, childMocks = false)
    }

    listOf(
        Direction.DOWN to Direction.DOWN,
        Direction.LEFT to Direction.RIGHT,
        Direction.RIGHT to Direction.LEFT
    ).forEach { (gravity, camera) ->
        it("turns the camera to $camera on a gravity change to $gravity: smooth follow, movement lock, stop") {
            rig.megaman.direction = gravity
            rig.services.events.run()
            turns shouldBe listOf(camera)
            follow.smooth shouldBe true
            rig.megaman.isLocked(MegaLockGroup.MOVEMENT) shouldBe true
            rig.body.physics.gravityOn shouldBe false
            rig.body.physics.velocity.isZero shouldBe true
            rig.body.direction shouldBe gravity
        }
    }

    it("resets the jetpack on a camera turn, before gravity is turned off and the velocity is zeroed") {
        rig.megaman.direction = Direction.DOWN
        verify(exactly = 1) { jetpack.reset() }
        gravityOnAtJetpackReset shouldBe listOf(true)
        velocityZeroAtJetpackReset shouldBe listOf(false)
    }

    it("does not turn the camera or reset the jetpack when the direction stays the same") {
        rig.megaman.direction = Direction.UP
        rig.services.events.run()
        turns shouldBe emptyList()
        follow.smooth shouldBe false
        rig.megaman.isLocked(MegaLockGroup.MOVEMENT) shouldBe false
        rig.body.physics.gravityOn shouldBe true
        rig.body.physics.velocity.x shouldBe 100f
        verify(exactly = 0) { jetpack.reset() }
    }
})
