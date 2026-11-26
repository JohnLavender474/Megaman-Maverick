package com.megaman.maverick.game.state.saves

import com.badlogic.gdx.utils.JsonReader
import com.megaman.maverick.game.difficulty.DifficultyMode
import com.megaman.maverick.game.entities.megaman.constants.MegaHeartTank
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.state.GameState
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class SaveFileJsonTest : DescribeSpec({

    fun saveText(
        version: String = "1",
        levels: String = "[]",
        heartTanks: String = "[]",
        difficulty: String = "\"NORMAL\"",
        currency: String = "0"
    ) = """
        {
          "version": $version,
          "savedAt": "2026-11-26T12:00:00Z",
          "progress": {
            "levelsCompleted": $levels,
            "heartTanks": $heartTanks,
            "healthTanks": [],
            "enhancements": [],
            "currency": $currency,
            "difficulty": $difficulty
          }
        }
    """.trimIndent()

    fun readUsed(text: String): SaveFile {
        val content = SaveFileJson.read(text)
        content.shouldBeInstanceOf<SaveSlotContent.Used>()
        return content.save
    }

    describe("write") {
        it("writes version 1, the time and the names in enum order") {
            val state = GameState().apply {
                addLevelCompleted(LevelDefinition.MOON_MAN)
                addLevelCompleted(LevelDefinition.INTRO_STAGE)
                addHeartTank(MegaHeartTank.C)
                addHeartTank(MegaHeartTank.A)
            }

            val root = JsonReader().parse(SaveFileJson.write(SaveFile.of(state, savedAt = "2026-11-26T12:00:00Z")))

            root.getLong("version") shouldBe 1L
            root.getString("savedAt") shouldBe "2026-11-26T12:00:00Z"
            val progress = root.get("progress")
            progress.get("levelsCompleted").asStringArray().toList() shouldBe listOf("INTRO_STAGE", "MOON_MAN")
            progress.get("heartTanks").asStringArray().toList() shouldBe listOf("A", "C")
            progress.getString("difficulty") shouldBe "NORMAL"
        }

        it("gives the same text for the same state") {
            val save = SaveFile.of(GameState().apply { addHeartTank(MegaHeartTank.B) }, savedAt = "t")

            SaveFileJson.write(save) shouldBe SaveFileJson.write(save)
        }
    }

    describe("unknown names") {
        it("skips a name that the game does not know and keeps the others") {
            val save = readUsed(saveText(levels = "[\"MOON_MAN\", \"NOT_A_LEVEL\"]", heartTanks = "[\"Z\", \"A\"]"))

            save.levelsCompleted.toList() shouldBe listOf(LevelDefinition.MOON_MAN)
            save.heartTanks.toList() shouldBe listOf(MegaHeartTank.A)
        }

        it("uses NORMAL for a difficulty that the game does not know") {
            readUsed(saveText(difficulty = "\"IMPOSSIBLE\"")).difficulty shouldBe DifficultyMode.NORMAL
        }

        it("does not record an extra level as completed") {
            val save = readUsed(saveText(levels = "[\"GAUNTLET_1\", \"MOON_MAN\"]"))

            save.levelsCompleted.toList() shouldBe listOf(LevelDefinition.MOON_MAN)
        }
    }

    describe("versions") {
        it("refuses a newer version") {
            val content = SaveFileJson.read(saveText(version = "7"))

            content.shouldBeInstanceOf<SaveSlotContent.NewerVersion>()
            content.version shouldBe 7L
        }

        it("reads a version that is not a whole number, or below 1, as unreadable") {
            SaveFileJson.read(saveText(version = "\"1\"")).shouldBeInstanceOf<SaveSlotContent.Unreadable>()
            SaveFileJson.read(saveText(version = "1.5")).shouldBeInstanceOf<SaveSlotContent.Unreadable>()
            SaveFileJson.read(saveText(version = "0")).shouldBeInstanceOf<SaveSlotContent.Unreadable>()
        }
    }

    describe("damaged files") {
        it("reads text that is not a JSON object as unreadable") {
            SaveFileJson.read("").shouldBeInstanceOf<SaveSlotContent.Unreadable>()
            SaveFileJson.read("{ \"version\": ").shouldBeInstanceOf<SaveSlotContent.Unreadable>()
            SaveFileJson.read("[1, 2]").shouldBeInstanceOf<SaveSlotContent.Unreadable>()
        }

        it("reads a missing or wrongly typed field as unreadable") {
            SaveFileJson.read("{ \"version\": 1, \"savedAt\": \"t\" }")
                .shouldBeInstanceOf<SaveSlotContent.Unreadable>()
            SaveFileJson.read(saveText(levels = "\"MOON_MAN\"")).shouldBeInstanceOf<SaveSlotContent.Unreadable>()
            SaveFileJson.read(saveText(levels = "[1]")).shouldBeInstanceOf<SaveSlotContent.Unreadable>()
            SaveFileJson.read(saveText(currency = "\"lots\"")).shouldBeInstanceOf<SaveSlotContent.Unreadable>()
        }
    }
})
