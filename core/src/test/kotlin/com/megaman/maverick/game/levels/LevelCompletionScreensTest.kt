package com.megaman.maverick.game.levels

import com.badlogic.gdx.utils.OrderedSet
import com.megaman.maverick.game.entities.megaman.constants.MegamanWeapon
import com.megaman.maverick.game.screens.ScreenEnum
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

private fun completionScreen(level: LevelDefinition, levelState: LevelState) =
    level.screenOnCompletion.invoke(levelState)

class LevelCompletionScreensTest : DescribeSpec({

    val robotMasterLevels = setOf(
        LevelDefinition.INTRO_STAGE,
        LevelDefinition.TIMBER_WOMAN,
        LevelDefinition.MOON_MAN,
        LevelDefinition.RODENT_MAN,
        LevelDefinition.DESERT_MAN,
        LevelDefinition.INFERNO_MAN,
        LevelDefinition.REACTOR_MAN,
        LevelDefinition.GLACIER_MAN,
        LevelDefinition.PRECIOUS_WOMAN
    )

    val constantScreens = mapOf(
        LevelDefinition.WILY_STAGE_1 to ScreenEnum.SAVE_GAME_SCREEN,
        LevelDefinition.WILY_STAGE_2 to ScreenEnum.SAVE_GAME_SCREEN,
        LevelDefinition.WILY_STAGE_3 to ScreenEnum.SAVE_GAME_SCREEN,
        LevelDefinition.TEST_1 to ScreenEnum.SAVE_GAME_SCREEN,
        LevelDefinition.TEST_2 to ScreenEnum.SAVE_GAME_SCREEN,
        LevelDefinition.REFACTOR_TEST_029 to ScreenEnum.SAVE_GAME_SCREEN,
        LevelDefinition.REFACTOR_TEST_031 to ScreenEnum.SAVE_GAME_SCREEN,
        LevelDefinition.REFACTOR_TEST_016 to ScreenEnum.SAVE_GAME_SCREEN,
        LevelDefinition.SMB3_1 to ScreenEnum.LEVEL_SCREEN,
        LevelDefinition.GAUNTLET_1 to ScreenEnum.EXTRAS_SCREEN,
        LevelDefinition.GAUNTLET_2 to ScreenEnum.EXTRAS_SCREEN
    )

    fun noWeapons() = LevelState()

    fun withWeapons() = LevelState().apply {
        weaponsAttained = OrderedSet<MegamanWeapon>().apply { add(MegamanWeapon.NEEDLE_SPIN) }
    }

    describe("screenOnCompletion") {
        it("has an expected screen for every level") {
            (robotMasterLevels + constantScreens.keys) shouldBe LevelDefinition.entries.toSet()
            robotMasterLevels.intersect(constantScreens.keys) shouldBe emptySet()
        }

        it("gives the save screen with no weapons attained and the get-weapons screen with weapons attained for a robot master level") {
            robotMasterLevels.forEach { level ->
                completionScreen(level, noWeapons()) shouldBe ScreenEnum.SAVE_GAME_SCREEN
                completionScreen(level, withWeapons()) shouldBe ScreenEnum.GET_WEAPONS_SCREEN
            }
        }

        it("gives the same screen with and without weapons attained for every other level") {
            constantScreens.forEach { (level, screen) ->
                completionScreen(level, noWeapons()) shouldBe screen
                completionScreen(level, withWeapons()) shouldBe screen
            }
        }
    }
})
