package com.megaman.maverick.game.state.saves

import com.badlogic.gdx.utils.JsonReader
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.difficulty.DifficultyMode
import com.megaman.maverick.game.entities.megaman.constants.MegaEnhancement
import com.megaman.maverick.game.entities.megaman.constants.MegaHealthTank
import com.megaman.maverick.game.entities.megaman.constants.MegaHeartTank
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.levels.LevelType
import com.megaman.maverick.game.state.GameState
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class SaveFileJsonTest : DescribeSpec({

    fun saveText(
        version: String = "1",
        levels: String = "[]",
        heartTanks: String = "[]",
        healthTanks: String = "[]",
        enhancements: String = "[]",
        difficulty: String = "\"NORMAL\"",
        currency: String = "0",
        savedAt: String = "\"2026-11-26T12:00:00Z\""
    ) = """
        {
          "version": $version,
          "savedAt": $savedAt,
          "progress": {
            "levelsCompleted": $levels,
            "heartTanks": $heartTanks,
            "healthTanks": $healthTanks,
            "enhancements": $enhancements,
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

    describe("round trip") {
        DifficultyMode.entries.forEach { difficulty ->
            it("gives back every level, tank and enhancement and the difficulty $difficulty") {
                val levels = LevelDefinition.entries.filter { it.type != LevelType.EXTRA_LEVEL }
                val state = GameState().apply {
                    levels.forEach { addLevelCompleted(it) }
                    MegaHeartTank.entries.forEach { addHeartTank(it) }
                    MegaHealthTank.entries.forEach { putHealthTank(it, ConstVals.MAX_HEALTH) }
                    MegaEnhancement.entries.forEach { addEnhancement(it) }
                    addCurrency(ConstVals.MAX_CURRENCY)
                    setDifficultyMode(difficulty)
                }

                val loaded = GameState()
                readUsed(SaveFileJson.write(SaveFile.of(state, savedAt = "t"))).applyTo(loaded)

                LevelDefinition.entries.filter { loaded.isLevelCompleted(it) } shouldBe levels
                MegaHeartTank.entries.filter { loaded.containsHeartTank(it) } shouldBe MegaHeartTank.entries.toList()
                MegaHealthTank.entries.filter { loaded.containsHealthTank(it) } shouldBe MegaHealthTank.entries.toList()
                MegaHealthTank.entries.map { loaded.getHealthTankValue(it) } shouldBe MegaHealthTank.entries.map { 0 }
                MegaEnhancement.entries.filter { loaded.containsEnhancement(it) } shouldBe
                    MegaEnhancement.entries.toList()
                loaded.getCurrency() shouldBe ConstVals.MAX_CURRENCY
                loaded.getDifficultyMode() shouldBe difficulty
            }
        }

        it("gives back an empty state as empty lists, no currency and NORMAL") {
            val save = readUsed(SaveFileJson.write(SaveFile.of(GameState(), savedAt = "2026-11-26T12:00:00Z")))

            save.savedAt shouldBe "2026-11-26T12:00:00Z"
            save.levelsCompleted.size shouldBe 0
            save.heartTanks.size shouldBe 0
            save.healthTanks.size shouldBe 0
            save.enhancements.size shouldBe 0
            save.currency shouldBe 0
            save.difficulty shouldBe DifficultyMode.NORMAL
        }
    }

    describe("unknown names") {
        it("skips a name that the game does not know and keeps the others") {
            val save = readUsed(saveText(levels = "[\"MOON_MAN\", \"NOT_A_LEVEL\"]", heartTanks = "[\"Z\", \"A\"]"))

            save.levelsCompleted.toList() shouldBe listOf(LevelDefinition.MOON_MAN)
            save.heartTanks.toList() shouldBe listOf(MegaHeartTank.A)
        }

        it("skips an unknown health tank or enhancement and keeps the others") {
            val save = readUsed(
                saveText(healthTanks = "[\"Q\", \"D\"]", enhancements = "[\"FLY\", \"JUMP_BOOST\"]")
            )

            save.healthTanks.toList() shouldBe listOf(MegaHealthTank.D)
            save.enhancements.toList() shouldBe listOf(MegaEnhancement.JUMP_BOOST)
        }

        it("keeps a name that is written twice only once") {
            val save = readUsed(saveText(levels = "[\"MOON_MAN\", \"MOON_MAN\"]", heartTanks = "[\"B\", \"B\"]"))

            save.levelsCompleted.toList() shouldBe listOf(LevelDefinition.MOON_MAN)
            save.heartTanks.toList() shouldBe listOf(MegaHeartTank.B)
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
            SaveFileJson.read(saveText(currency = "1.5")).shouldBeInstanceOf<SaveSlotContent.Unreadable>()
            SaveFileJson.read(saveText(difficulty = "1")).shouldBeInstanceOf<SaveSlotContent.Unreadable>()
            SaveFileJson.read(saveText(savedAt = "12")).shouldBeInstanceOf<SaveSlotContent.Unreadable>()
            SaveFileJson.read(saveText(heartTanks = "{}")).shouldBeInstanceOf<SaveSlotContent.Unreadable>()
        }

        it("reads an object with no version as unreadable") {
            SaveFileJson.read("{}").shouldBeInstanceOf<SaveSlotContent.Unreadable>()
            SaveFileJson.read(saveText().replace("\"version\": 1,", ""))
                .shouldBeInstanceOf<SaveSlotContent.Unreadable>()
        }
    }
})
