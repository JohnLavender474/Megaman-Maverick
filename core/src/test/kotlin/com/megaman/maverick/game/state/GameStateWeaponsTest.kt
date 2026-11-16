package com.megaman.maverick.game.state

import com.megaman.maverick.game.entities.megaman.weapons.MegamanWeapons
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.levels.LevelRewards
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class GameStateWeaponsTest : DescribeSpec({

    lateinit var state: GameState

    fun owned() = MegamanWeapons.all.filter { state.containsWeapon(it) }.toSet()

    beforeEach { state = GameState() }

    describe("owned weapons") {
        it("are only the buster in a new state") {
            owned() shouldBe setOf(MegamanWeapons.MEGA_BUSTER)
        }

        it("get the reward weapons when a level is completed, and lose them when it is removed") {
            state.addLevelCompleted(LevelDefinition.MOON_MAN)
            owned() shouldBe setOf(MegamanWeapons.MEGA_BUSTER, MegamanWeapons.MOON_SCYTHES, MegamanWeapons.RUSH_JET)

            state.removeLevelCompleted(LevelDefinition.MOON_MAN)
            owned() shouldBe setOf(MegamanWeapons.MEGA_BUSTER)
        }

        it("give each robot master level its reward") {
            LevelRewards.weaponsFor(LevelDefinition.GLACIER_MAN).toSet() shouldBe setOf(MegamanWeapons.FRIGID_SHOT)
            LevelRewards.weaponsFor(LevelDefinition.INTRO_STAGE).isEmpty shouldBe true
        }

        it("keep the weapons that a level override added when reset removes the levels") {
            state.addWeapon(MegamanWeapons.FRIGID_SHOT)
            state.addLevelCompleted(LevelDefinition.DESERT_MAN)

            state.reset()

            owned() shouldBe setOf(MegamanWeapons.MEGA_BUSTER, MegamanWeapons.FRIGID_SHOT)
        }

        it("keep only the buster after clearWeapons") {
            state.addWeapon(MegamanWeapons.FRIGID_SHOT)
            state.addLevelCompleted(LevelDefinition.DESERT_MAN)

            state.clearWeapons()

            owned() shouldBe setOf(MegamanWeapons.MEGA_BUSTER)
            state.containsWeapon(MegamanWeapons.MEGA_BUSTER) shouldBe true
        }

        it("come back from the save string through the levels completed, and are not in it") {
            state.addLevelCompleted(LevelDefinition.GLACIER_MAN)
            state.addWeapon(MegamanWeapons.AXE_SWINGER)
            val saved = state.toString()
            saved shouldBe "GLACIER_MAN;;;;0;NORMAL"

            val loaded = GameState()
            loaded.fromString(saved)

            MegamanWeapons.all.filter { loaded.containsWeapon(it) }.toSet() shouldBe
                setOf(MegamanWeapons.MEGA_BUSTER, MegamanWeapons.FRIGID_SHOT)
        }
    }
})
