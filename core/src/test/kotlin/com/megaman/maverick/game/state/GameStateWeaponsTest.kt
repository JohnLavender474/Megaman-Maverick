package com.megaman.maverick.game.state

import com.megaman.maverick.game.entities.megaman.constants.MegamanWeapon
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.levels.LevelRewards
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class GameStateWeaponsTest : DescribeSpec({

    lateinit var state: GameState

    fun owned() = MegamanWeapon.entries.filter { state.containsWeapon(it) }.toSet()

    beforeEach { state = GameState() }

    describe("owned weapons") {
        it("are only the buster in a new state") {
            owned() shouldBe setOf(MegamanWeapon.MEGA_BUSTER)
        }

        it("get the reward weapons when a level is defeated, and lose them when it is removed") {
            state.addLevelDefeated(LevelDefinition.MOON_MAN)
            owned() shouldBe setOf(MegamanWeapon.MEGA_BUSTER, MegamanWeapon.MOON_SCYTHES, MegamanWeapon.RUSH_JET)

            state.removeLevelDefeated(LevelDefinition.MOON_MAN)
            owned() shouldBe setOf(MegamanWeapon.MEGA_BUSTER)
        }

        it("give each robot master level its reward") {
            LevelRewards.weaponsFor(LevelDefinition.GLACIER_MAN).toSet() shouldBe setOf(MegamanWeapon.FRIGID_SHOT)
            LevelRewards.weaponsFor(LevelDefinition.INTRO_STAGE).isEmpty shouldBe true
        }

        it("keep the weapons that a level override added when reset removes the levels") {
            state.addWeapon(MegamanWeapon.FRIGID_SHOT)
            state.addLevelDefeated(LevelDefinition.DESERT_MAN)

            state.reset()

            owned() shouldBe setOf(MegamanWeapon.MEGA_BUSTER, MegamanWeapon.FRIGID_SHOT)
        }

        it("keep only the buster after clearWeapons") {
            state.addWeapon(MegamanWeapon.FRIGID_SHOT)
            state.addLevelDefeated(LevelDefinition.DESERT_MAN)

            state.clearWeapons()

            owned() shouldBe setOf(MegamanWeapon.MEGA_BUSTER)
            state.containsWeapon(MegamanWeapon.MEGA_BUSTER) shouldBe true
        }

        it("come back from the save string through the levels defeated, and are not in it") {
            state.addLevelDefeated(LevelDefinition.GLACIER_MAN)
            state.addWeapon(MegamanWeapon.AXE_SWINGER)
            val saved = state.toString()
            saved shouldBe "GLACIER_MAN;;;;0;NORMAL"

            val loaded = GameState()
            loaded.fromString(saved)

            MegamanWeapon.entries.filter { loaded.containsWeapon(it) }.toSet() shouldBe
                setOf(MegamanWeapon.MEGA_BUSTER, MegamanWeapon.FRIGID_SHOT)
        }
    }
})
