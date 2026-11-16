package com.megaman.maverick.game.state

import com.megaman.maverick.game.difficulty.DifficultyMode
import com.megaman.maverick.game.entities.megaman.constants.MegaEnhancement
import com.megaman.maverick.game.entities.megaman.constants.MegaHealthTank
import com.megaman.maverick.game.entities.megaman.constants.MegaHeartTank
import com.megaman.maverick.game.levels.LevelDefinition
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class GameStateSaveStringTest : DescribeSpec({

    val saveString = "MOON_MAN,GLACIER_MAN;A,C;B,D;JUMP_BOOST,AIR_DASH_BOOST;42;HARD"

    fun filledState() = GameState().apply {
        addLevelCompleted(LevelDefinition.GLACIER_MAN)
        addLevelCompleted(LevelDefinition.MOON_MAN)
        addHeartTank(MegaHeartTank.C)
        addHeartTank(MegaHeartTank.A)
        putHealthTank(MegaHealthTank.D, 10)
        putHealthTank(MegaHealthTank.B)
        addEnhancement(MegaEnhancement.AIR_DASH_BOOST)
        addEnhancement(MegaEnhancement.JUMP_BOOST)
        addCurrency(42)
        setDifficultyMode(DifficultyMode.HARD)
    }

    describe("toString") {
        it("gives the save string") {
            filledState().toString() shouldBe saveString
        }

        it("gives the empty save string for a new state") {
            GameState().toString() shouldBe ";;;;0;NORMAL"
        }
    }

    describe("fromString") {
        it("gives back the same data") {
            val state = GameState()
            state.fromString(saveString)

            state.toString() shouldBe saveString
            state.isLevelCompleted(LevelDefinition.MOON_MAN) shouldBe true
            state.isLevelCompleted(LevelDefinition.GLACIER_MAN) shouldBe true
            state.containsHeartTank(MegaHeartTank.A) shouldBe true
            state.containsHeartTank(MegaHeartTank.C) shouldBe true
            state.getHealthTankValue(MegaHealthTank.B) shouldBe 0
            state.getHealthTankValue(MegaHealthTank.D) shouldBe 0
            state.containsEnhancement(MegaEnhancement.JUMP_BOOST) shouldBe true
            state.getCurrency() shouldBe 42
            state.getDifficultyMode() shouldBe DifficultyMode.HARD
        }

        it("ignores whitespace in the string") {
            val state = GameState()
            state.fromString(" MOON_MAN, GLACIER_MAN ;A,\tC;B ,D;JUMP_BOOST,\nAIR_DASH_BOOST; 42 ; HARD")

            state.toString() shouldBe saveString
        }
    }
})
