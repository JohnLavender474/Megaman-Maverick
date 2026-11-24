package com.megaman.maverick.game.entities.hazards

import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.audio.MegaAudioManager
import com.megaman.maverick.game.testServices
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify

class LaserBeamLoopTest : DescribeSpec({

    lateinit var audio: MegaAudioManager
    lateinit var services: MegaServices
    var playing = false

    beforeEach {
        playing = false
        audio = mockk(relaxed = true)
        every { audio.isSoundPlaying(SoundAsset.LASER_LOOP_SOUND) } answers { playing }
        services = testServices(audio = audio)
        LaserBeamLoop.join(services)
    }

    afterEach {
        while (services.updatables.containsKey(LaserBeamLoop.KEY)) LaserBeamLoop.leave(services)
        unmockkAll()
    }

    describe("update") {
        it("starts the loop once when a beamer requests it") {
            LaserBeamLoop.request()
            LaserBeamLoop.update(0f)
            playing = true
            LaserBeamLoop.request()
            LaserBeamLoop.update(0f)
            verify(exactly = 1) { audio.playSound(SoundAsset.LASER_LOOP_SOUND, true) }
        }

        it("stops the loop in the first update with no request") {
            playing = true
            LaserBeamLoop.update(0f)
            verify(exactly = 1) { audio.stopSound(SoundAsset.LASER_LOOP_SOUND) }
        }

        it("does nothing when no beamer requests and the loop is silent") {
            LaserBeamLoop.update(0f)
            verify(exactly = 0) { audio.playSound(any(), any()) }
            verify(exactly = 0) { audio.stopSound(any()) }
        }
    }

    describe("join and leave") {
        it("puts the loop in the session updatables while one beamer is in, and removes it with the last") {
            LaserBeamLoop.join(services)
            LaserBeamLoop.leave(services)
            services.updatables.containsKey(LaserBeamLoop.KEY) shouldBe true
            LaserBeamLoop.leave(services)
            services.updatables.containsKey(LaserBeamLoop.KEY) shouldBe false
        }

        it("stops a playing loop when the last beamer leaves") {
            playing = true
            clearMocks(audio, answers = false)
            LaserBeamLoop.leave(services)
            verify(exactly = 1) { audio.stopSound(SoundAsset.LASER_LOOP_SOUND) }
        }

        it("throws on a leave with no join") {
            LaserBeamLoop.leave(services)
            shouldThrow<IllegalStateException> { LaserBeamLoop.leave(services) }
        }
    }
})
