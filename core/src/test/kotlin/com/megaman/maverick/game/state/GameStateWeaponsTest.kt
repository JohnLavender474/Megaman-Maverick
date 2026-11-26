package com.megaman.maverick.game.state

import com.megaman.maverick.game.entities.megaman.weapons.MegamanWeapons
import com.megaman.maverick.game.levels.LevelDefinition
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import com.megaman.maverick.game.state.saves.SaveFile
import com.megaman.maverick.game.state.saves.SaveFileJson
import com.megaman.maverick.game.state.saves.SaveSlotContent
import com.megaman.maverick.game.entities.megaman.weapons.MEGAMAN_LOADOUT
import com.megaman.maverick.game.players.PlayerCharacter

class GameStateWeaponsTest : DescribeSpec({

    lateinit var state: GameState

    fun owned() = MEGAMAN_LOADOUT.weapons.filter { state.ownsWeapon(PlayerCharacter.MEGAMAN, it) }.toSet()

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
            val rewards = LevelDefinition.entries.associateWith { MEGAMAN_LOADOUT.rewardsFor(it).toList() }
                .filterValues { it.isNotEmpty() }
            rewards shouldBe mapOf(
                LevelDefinition.MOON_MAN to listOf(MegamanWeapons.MOON_SCYTHES, MegamanWeapons.RUSH_JET),
                LevelDefinition.PRECIOUS_WOMAN to listOf(MegamanWeapons.PRECIOUS_GUARD),
                LevelDefinition.INFERNO_MAN to listOf(MegamanWeapons.INFERNAL_BARRAGE),
                LevelDefinition.GLACIER_MAN to listOf(MegamanWeapons.FRIGID_SHOT),
                LevelDefinition.TIMBER_WOMAN to listOf(MegamanWeapons.AXE_SWINGER),
                LevelDefinition.DESERT_MAN to listOf(MegamanWeapons.NEEDLE_SPIN),
                LevelDefinition.RODENT_MAN to listOf(MegamanWeapons.RODENT_CLAWS),
                LevelDefinition.REACTOR_MAN to listOf(MegamanWeapons.REACTOR_SHOT)
            )
        }

        it("keep the weapons that a level override added when reset removes the levels") {
            state.addWeapon(PlayerCharacter.MEGAMAN, MegamanWeapons.FRIGID_SHOT)
            state.addLevelCompleted(LevelDefinition.DESERT_MAN)

            state.reset()

            owned() shouldBe setOf(MegamanWeapons.MEGA_BUSTER, MegamanWeapons.FRIGID_SHOT)
        }

        it("keep only the buster and the level rewards after clearWeapons") {
            state.addWeapon(PlayerCharacter.MEGAMAN, MegamanWeapons.FRIGID_SHOT)
            state.addLevelCompleted(LevelDefinition.DESERT_MAN)

            state.clearWeapons()

            owned() shouldBe setOf(MegamanWeapons.MEGA_BUSTER, MegamanWeapons.NEEDLE_SPIN)
            state.ownsWeapon(PlayerCharacter.MEGAMAN, MegamanWeapons.MEGA_BUSTER) shouldBe true
        }

        it("give a completed level's reward only to the character whose loadout has it") {
            state.addLevelCompleted(LevelDefinition.DESERT_MAN)

            state.ownsWeapon(PlayerCharacter.MEGAMAN, MegamanWeapons.NEEDLE_SPIN) shouldBe true
            state.ownsWeapon(PlayerCharacter.X, MegamanWeapons.NEEDLE_SPIN) shouldBe false
            state.ownsWeapon(PlayerCharacter.PROTOMAN, MegamanWeapons.NEEDLE_SPIN) shouldBe false
        }

        it("count alwaysOwned only in the loadout that lists the weapon") {
            state.ownsWeapon(PlayerCharacter.MEGAMAN, MegamanWeapons.MEGA_BUSTER) shouldBe true
            state.ownsWeapon(PlayerCharacter.X, MegamanWeapons.MEGA_BUSTER) shouldBe false
            state.ownsWeapon(PlayerCharacter.PROTOMAN, MegamanWeapons.MEGA_BUSTER) shouldBe false
        }

        it("reject a given weapon that is not in the character's loadout") {
            shouldThrow<IllegalArgumentException> { state.addWeapon(PlayerCharacter.X, MegamanWeapons.AXE_SWINGER) }
        }

        it("give a weapon to each character whose loadout lists it, and to no other") {
            state.addWeaponToEachLoadout(MegamanWeapons.AXE_SWINGER)

            state.ownsWeapon(PlayerCharacter.MEGAMAN, MegamanWeapons.AXE_SWINGER) shouldBe true
            state.ownsWeapon(PlayerCharacter.X, MegamanWeapons.AXE_SWINGER) shouldBe false
            state.ownsWeapon(PlayerCharacter.PROTOMAN, MegamanWeapons.AXE_SWINGER) shouldBe false
        }

        it("come back from the save file through the levels completed, and are not in it") {
            state.addLevelCompleted(LevelDefinition.GLACIER_MAN)
            state.addWeapon(PlayerCharacter.MEGAMAN, MegamanWeapons.AXE_SWINGER)
            val saved = SaveFileJson.write(SaveFile.of(state, savedAt = "2026-11-26T00:00:00Z"))
            saved shouldNotContain "AXE_SWINGER"

            val loaded = GameState()
            (SaveFileJson.read(saved) as SaveSlotContent.Used).save.applyTo(loaded)

            MEGAMAN_LOADOUT.weapons.filter { loaded.ownsWeapon(PlayerCharacter.MEGAMAN, it) }.toSet() shouldBe
                setOf(MegamanWeapons.MEGA_BUSTER, MegamanWeapons.FRIGID_SHOT)
        }
    }
})
