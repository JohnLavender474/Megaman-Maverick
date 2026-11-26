package com.megaman.maverick.game.state.saves

import com.megaman.maverick.game.difficulty.DifficultyMode
import com.megaman.maverick.game.entities.megaman.constants.MegaEnhancement
import com.megaman.maverick.game.entities.megaman.constants.MegaHealthTank
import com.megaman.maverick.game.entities.megaman.constants.MegaHeartTank
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.state.GameState
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class SaveSlotsTest : DescribeSpec({

    lateinit var storage: InMemorySaveStorage
    lateinit var slots: SaveSlots

    beforeEach {
        storage = InMemorySaveStorage()
        slots = SaveSlots(storage)
    }

    fun filledState() = GameState().apply {
        addLevelCompleted(LevelDefinition.INTRO_STAGE)
        addLevelCompleted(LevelDefinition.MOON_MAN)
        addHeartTank(MegaHeartTank.C)
        putHealthTank(MegaHealthTank.B, 10)
        addEnhancement(MegaEnhancement.JUMP_BOOST)
        addCurrency(42)
        setDifficultyMode(DifficultyMode.HARD)
    }

    fun newerVersionText(version: Int) =
        """{ "version": $version, "savedAt": "2099-01-01T00:00:00Z", "progress": { "anything": true } }"""

    describe("slot count") {
        it("is 5") {
            SaveSlots.SLOT_COUNT shouldBe 5
        }

        it("reads every slot from 1 to 5 as empty at first") {
            for (slot in 1..5) slots.read(slot) shouldBe SaveSlotContent.Empty
        }

        it("throws for a slot outside 1 to 5") {
            shouldThrow<IllegalArgumentException> { slots.read(0) }
            shouldThrow<IllegalArgumentException> { slots.read(6) }
            shouldThrow<IllegalArgumentException> { slots.write(0, SaveFile.of(GameState(), savedAt = "t")) }
            shouldThrow<IllegalArgumentException> { slots.write(6, SaveFile.of(GameState(), savedAt = "t")) }
            storage.files.size shouldBe 0
        }
    }

    describe("round trip") {
        it("gives back the same progress in a new game state") {
            slots.write(3, SaveFile.of(filledState(), savedAt = "2026-11-26T12:00:00Z")) shouldBe true

            val content = slots.read(3)
            content.shouldBeInstanceOf<SaveSlotContent.Used>()
            content.save.savedAt shouldBe "2026-11-26T12:00:00Z"
            val loaded = GameState()
            content.save.applyTo(loaded)

            loaded.isLevelCompleted(LevelDefinition.INTRO_STAGE) shouldBe true
            loaded.isLevelCompleted(LevelDefinition.MOON_MAN) shouldBe true
            loaded.isLevelCompleted(LevelDefinition.DESERT_MAN) shouldBe false
            loaded.containsHeartTank(MegaHeartTank.C) shouldBe true
            loaded.containsHeartTank(MegaHeartTank.A) shouldBe false
            loaded.containsHealthTank(MegaHealthTank.B) shouldBe true
            loaded.getHealthTankValue(MegaHealthTank.B) shouldBe 0
            loaded.containsEnhancement(MegaEnhancement.JUMP_BOOST) shouldBe true
            loaded.getCurrency() shouldBe 42
            loaded.getDifficultyMode() shouldBe DifficultyMode.HARD
        }

        it("writes only the file of its slot") {
            slots.write(3, SaveFile.of(filledState(), savedAt = "t"))

            storage.files.keys().toList() shouldBe listOf("slot-3.json")
            for (slot in listOf(1, 2, 4, 5)) slots.read(slot) shouldBe SaveSlotContent.Empty
        }

        it("names the file of each slot slot-N.json") {
            for (slot in 1..5) slots.write(slot, SaveFile.of(GameState(), savedAt = "t"))

            storage.files.keys().toList().sorted() shouldBe (1..5).map { "slot-$it.json" }
        }

        it("leaves the other slots as they were when it writes a slot") {
            slots.write(1, SaveFile.of(filledState(), savedAt = "first"))
            val slot1Text = storage.files.get("slot-1.json")

            slots.write(2, SaveFile.of(GameState(), savedAt = "second"))

            storage.files.get("slot-1.json") shouldBe slot1Text
            (slots.read(1) as SaveSlotContent.Used).save.savedAt shouldBe "first"
            (slots.read(2) as SaveSlotContent.Used).save.savedAt shouldBe "second"
        }

        it("replaces the saved text when a used slot is written again") {
            slots.write(1, SaveFile.of(filledState(), savedAt = "first"))
            slots.write(1, SaveFile.of(GameState(), savedAt = "second"))

            val save = (slots.read(1) as SaveSlotContent.Used).save
            save.savedAt shouldBe "second"
            save.levelsCompleted.size shouldBe 0
        }

        it("replaces the progress that the game state had before") {
            slots.write(1, SaveFile.of(GameState().apply { addHeartTank(MegaHeartTank.A) }, savedAt = "t"))
            val loaded = filledState()

            (slots.read(1) as SaveSlotContent.Used).save.applyTo(loaded)

            loaded.isLevelCompleted(LevelDefinition.MOON_MAN) shouldBe false
            loaded.isLevelCompleted(LevelDefinition.INTRO_STAGE) shouldBe false
            loaded.containsHeartTank(MegaHeartTank.A) shouldBe true
            loaded.containsHeartTank(MegaHeartTank.C) shouldBe false
            loaded.containsHealthTank(MegaHealthTank.B) shouldBe false
            loaded.containsEnhancement(MegaEnhancement.JUMP_BOOST) shouldBe false
            loaded.getCurrency() shouldBe 0
            loaded.getDifficultyMode() shouldBe DifficultyMode.NORMAL
        }
    }

    describe("a newer version") {
        it("is read as a newer version and is not overwritten") {
            storage.files.put("slot-2.json", newerVersionText(2))

            val content = slots.read(2)
            content.shouldBeInstanceOf<SaveSlotContent.NewerVersion>()
            content.version shouldBe 2L

            slots.write(2, SaveFile.of(filledState(), savedAt = "t")) shouldBe false
            storage.files.get("slot-2.json") shouldBe newerVersionText(2)
            storage.files.size shouldBe 1
        }

        it("does not stop a write to another slot") {
            storage.files.put("slot-2.json", newerVersionText(9))

            slots.write(1, SaveFile.of(filledState(), savedAt = "t")) shouldBe true

            slots.read(1).shouldBeInstanceOf<SaveSlotContent.Used>()
            storage.files.get("slot-2.json") shouldBe newerVersionText(9)
        }
    }

    describe("an unreadable slot") {
        it("is read as unreadable and can be overwritten") {
            storage.files.put("slot-4.json", "not json {")

            slots.read(4).shouldBeInstanceOf<SaveSlotContent.Unreadable>()
            slots.write(4, SaveFile.of(filledState(), savedAt = "t")) shouldBe true
            slots.read(4).shouldBeInstanceOf<SaveSlotContent.Used>()
        }
    }
})
