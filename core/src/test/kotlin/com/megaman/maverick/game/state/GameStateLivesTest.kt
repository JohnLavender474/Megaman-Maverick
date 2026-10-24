package com.megaman.maverick.game.state

import com.maverick.game.engine.players.PlayerSlot
import com.megaman.maverick.game.ConstVals
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class GameStateLivesTest : DescribeSpec({

    val slot0 = PlayerSlot(0)
    val slot1 = PlayerSlot(1)

    lateinit var state: GameState

    beforeEach { state = GameState() }

    describe("lives") {
        it("start at START_LIVES") {
            state.getLives(slot0) shouldBe ConstVals.START_LIVES
        }

        it("are one shared count for every slot") {
            state.addLife(slot1) shouldBe true
            state.getLives(slot0) shouldBe ConstVals.START_LIVES + 1
            state.removeLife(slot0)
            state.getLives(slot1) shouldBe ConstVals.START_LIVES
        }

        it("do not go above MAX_LIVES, and addLife returns false there") {
            state.setLives(ConstVals.MAX_LIVES)
            state.addLife(slot0) shouldBe false
            state.getLives(slot0) shouldBe ConstVals.MAX_LIVES
        }

        it("are exhausted at MIN_LIVES and do not go below it") {
            state.setLives(1)
            state.livesExhausted() shouldBe false
            state.removeLife(slot0)
            state.livesExhausted() shouldBe true
            state.removeLife(slot0)
            state.getLives(slot0) shouldBe ConstVals.MIN_LIVES
        }

        it("go back to START_LIVES on resetLives") {
            state.setLives(7)
            state.resetLives()
            state.getLives(slot0) shouldBe ConstVals.START_LIVES
        }

        it("are not changed by reset and are not in the save string") {
            state.setLives(7)
            state.reset()
            state.getLives(slot0) shouldBe 7
            state.toString() shouldBe ";;;;0;NORMAL"
        }
    }
})
