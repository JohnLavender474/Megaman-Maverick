package com.megaman.maverick.game.entities.megaman

import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.audio.AudioComponent
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.entities.megaman.weapons.MegamanWeapons
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.state.GameState
import com.megaman.maverick.game.testServices
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.unmockkAll

class MegamanProgressTest : DescribeSpec({

    lateinit var state: GameState
    lateinit var services: MegaServices
    lateinit var audio: AudioComponent

    beforeEach {
        state = GameState()
        services = testServices(engine = mockk<GameEngine>(relaxed = true), gameState = state)

        val megaman = Megaman(services)
        audio = AudioComponent()
        megaman.addComponent(audio)
        state.addListener(megaman.definePlayerProgressComponent())
    }

    afterEach { unmockkAll() }

    describe("a newly completed level") {
        it("sets the reward weapons of that level") {
            state.addLevelCompleted(LevelDefinition.DESERT_MAN)

            services.levelState.weaponsAttained!!.contains(MegamanWeapons.NEEDLE_SPIN) shouldBe true
        }

        it("does not set them again for a level that is already completed") {
            state.addLevelCompleted(LevelDefinition.DESERT_MAN)
            services.levelState.weaponsAttained = null

            state.addLevelCompleted(LevelDefinition.DESERT_MAN)

            services.levelState.weaponsAttained shouldBe null
        }
    }

    describe("added currency") {
        it("plays the pickup sound while the currency is below the max") {
            state.addCurrency(1)

            audio.playSoundRequests.size shouldBe 1
            audio.playSoundRequests[0].source shouldBe SoundAsset.CURRENCY_PICKUP_SOUND
        }

        it("plays no sound when the currency reaches the max") {
            state.addCurrency(ConstVals.MAX_CURRENCY)

            audio.playSoundRequests.size shouldBe 0
        }
    }
})
