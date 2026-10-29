package com.megaman.maverick.game

import com.megaman.maverick.game.entities.megaman.constants.MegaHealthTank
import com.megaman.maverick.game.entities.megaman.constants.MegaHeartTank
import com.megaman.maverick.game.entities.megaman.constants.MegamanWeapon
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.screens.ScreenEnum
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class LaunchOptionsResolverTest : DescribeSpec({

    fun resolve(options: LaunchOptions) = resolveLaunchOptions(options)

    describe("no option") {
        it("uses the default start") {
            val result = resolve(LaunchOptions())
            result.isValid shouldBe true
            val resolved = result.resolved!!
            resolved.start shouldBe LaunchStart.Default
            resolved.room shouldBe null
            resolved.autoPerf shouldBe true
        }
    }

    describe("--level") {
        it("starts the level, case-insensitively") {
            val result = resolve(LaunchOptions(level = "moon_man"))
            result.resolved!!.start shouldBe LaunchStart.StartLevel(LevelDefinition.MOON_MAN)
        }

        it("rejects an unknown level and suggests the nearest name") {
            val result = resolve(LaunchOptions(level = "MOONMAN"))
            result.isValid shouldBe false
            result.errors.size shouldBe 1
            result.errors[0] shouldContain "Invalid --level 'MOONMAN'"
            result.errors[0] shouldContain "Did you mean MOON_MAN?"
            result.errors[0] shouldContain "Allowed:"
        }
    }

    describe("--screen") {
        it("opens a screen, case-insensitively") {
            val result = resolve(LaunchOptions(screen = "main_menu_screen"))
            result.resolved!!.start shouldBe LaunchStart.OpenScreen(ScreenEnum.MAIN_MENU_SCREEN, null)
        }

        it("stages the level when the screen is not the level screen") {
            val result = resolve(LaunchOptions(screen = "LEVEL_SELECT_SCREEN", level = "MOON_MAN"))
            result.resolved!!.start shouldBe
                LaunchStart.OpenScreen(ScreenEnum.LEVEL_SELECT_SCREEN, LevelDefinition.MOON_MAN)
        }

        it("starts the level when the screen is the level screen") {
            val result = resolve(LaunchOptions(screen = "LEVEL_SCREEN", level = "MOON_MAN"))
            result.resolved!!.start shouldBe LaunchStart.StartLevel(LevelDefinition.MOON_MAN)
        }

        it("rejects the level screen without a level") {
            val result = resolve(LaunchOptions(screen = "LEVEL_SCREEN"))
            result.isValid shouldBe false
            result.errors[0] shouldContain "--screen LEVEL_SCREEN needs --level"
        }

        it("rejects the robot master intro screen without a level") {
            val result = resolve(LaunchOptions(screen = "ROBOT_MASTER_INTRO_SCREEN"))
            result.isValid shouldBe false
            result.errors[0] shouldContain "needs --level"
        }

        it("rejects an unknown screen") {
            val result = resolve(LaunchOptions(screen = "LEVEL_SELECT"))
            result.isValid shouldBe false
            result.errors[0] shouldContain "Did you mean LEVEL_SELECT_SCREEN?"
        }
    }

    describe("--room") {
        it("keeps the room when a level is given") {
            val result = resolve(LaunchOptions(level = "MOON_MAN", room = " room3 "))
            result.resolved!!.room shouldBe "room3"
        }

        it("rejects a room without a level") {
            val result = resolve(LaunchOptions(room = "room3"))
            result.isValid shouldBe false
            result.errors[0] shouldContain "--room 'room3' needs --level"
        }
    }

    describe("--logTags") {
        it("splits and trims the tags") {
            val resolved = resolve(LaunchOptions(logTags = "Megaman, MegaLevelScreen ,")).resolved!!
            resolved.logTags shouldBe setOf("Megaman", "MegaLevelScreen")
            resolved.logAllTags shouldBe false
        }

        it("turns on all tags with 'all'") {
            val resolved = resolve(LaunchOptions(logTags = "ALL")).resolved!!
            resolved.logAllTags shouldBe true
            resolved.logTags shouldBe emptySet()
        }
    }

    describe("--weapons") {
        it("gives no weapon when the option is not given") {
            resolve(LaunchOptions()).resolved!!.weapons shouldBe emptySet()
        }

        it("splits, trims and parses the weapons, case-insensitively") {
            val result = resolve(LaunchOptions(weapons = "moon_scythes, RUSH_JET ,"))
            result.isValid shouldBe true
            result.resolved!!.weapons shouldBe setOf(MegamanWeapon.MOON_SCYTHES, MegamanWeapon.RUSH_JET)
        }

        it("gives every weapon with 'all'") {
            resolve(LaunchOptions(weapons = "ALL")).resolved!!.weapons shouldBe MegamanWeapon.entries.toSet()
        }

        it("rejects an unknown weapon and suggests the nearest name") {
            val result = resolve(LaunchOptions(weapons = "MOON_SCYTHE,RUSH_JET"))
            result.isValid shouldBe false
            result.errors.size shouldBe 1
            result.errors[0] shouldContain "Invalid --weapons 'MOON_SCYTHE'"
            result.errors[0] shouldContain "Did you mean MOON_SCYTHES?"
            result.errors[0] shouldContain "Allowed:"
        }

        it("rejects an unknown weapon also when 'all' is given") {
            resolve(LaunchOptions(weapons = "all,NOPE")).isValid shouldBe false
        }
    }

    describe("--heartTanks") {
        it("gives no heart tank when the option is not given") {
            resolve(LaunchOptions()).resolved!!.heartTanks shouldBe emptySet()
        }

        it("splits, trims and parses the heart tanks, case-insensitively") {
            val result = resolve(LaunchOptions(heartTanks = "a, H ,"))
            result.isValid shouldBe true
            result.resolved!!.heartTanks shouldBe setOf(MegaHeartTank.A, MegaHeartTank.H)
        }

        it("gives every heart tank with 'all'") {
            resolve(LaunchOptions(heartTanks = "All")).resolved!!.heartTanks shouldBe MegaHeartTank.entries.toSet()
        }

        it("rejects an unknown heart tank") {
            val result = resolve(LaunchOptions(heartTanks = "A,Z"))
            result.isValid shouldBe false
            result.errors.size shouldBe 1
            result.errors[0] shouldContain "Invalid --heartTanks 'Z'"
            result.errors[0] shouldContain "Allowed: A, B, C, D, E, F, G, H"
        }
    }

    describe("--healthTanks") {
        it("gives no health tank when the option is not given") {
            resolve(LaunchOptions()).resolved!!.healthTanks shouldBe emptySet()
        }

        it("splits, trims and parses the health tanks, case-insensitively") {
            val result = resolve(LaunchOptions(healthTanks = " b,D"))
            result.isValid shouldBe true
            result.resolved!!.healthTanks shouldBe setOf(MegaHealthTank.B, MegaHealthTank.D)
        }

        it("gives every health tank with 'all'") {
            resolve(LaunchOptions(healthTanks = "all")).resolved!!.healthTanks shouldBe MegaHealthTank.entries.toSet()
        }

        it("rejects an unknown health tank") {
            val result = resolve(LaunchOptions(healthTanks = "E"))
            result.isValid shouldBe false
            result.errors.size shouldBe 1
            result.errors[0] shouldContain "Invalid --healthTanks 'E'"
            result.errors[0] shouldContain "Allowed: A, B, C, D"
        }
    }

    describe("savesAllowed") {
        it("allows a save without tank options, also with --weapons") {
            resolve(LaunchOptions(weapons = "all")).resolved!!.savesAllowed shouldBe true
        }

        it("blocks a save with --heartTanks") {
            resolve(LaunchOptions(heartTanks = "A")).resolved!!.savesAllowed shouldBe false
        }

        it("blocks a save with --healthTanks") {
            resolve(LaunchOptions(healthTanks = "A")).resolved!!.savesAllowed shouldBe false
        }
    }

    describe("--autoPerf") {
        it("passes the value through") {
            resolve(LaunchOptions(autoPerf = false)).resolved!!.autoPerf shouldBe false
        }
    }

    describe("errors") {
        it("lists every error at once") {
            val result = resolve(LaunchOptions(screen = "NOPE", level = "NOPE", room = "room1"))
            result.isValid shouldBe false
            result.errors.size shouldBe 2
        }
    }
})
