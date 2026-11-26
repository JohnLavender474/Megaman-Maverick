package com.megaman.maverick.game.state.saves

import com.megaman.maverick.game.players.FakePreferences
import com.megaman.maverick.game.state.GameState
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class PreferencesSaveStorageTest : DescribeSpec({

    lateinit var prefs: FakePreferences
    lateinit var storage: PreferencesSaveStorage

    beforeEach {
        prefs = FakePreferences()
        storage = PreferencesSaveStorage(prefs)
    }

    describe("read") {
        it("gives null for a name that was never written") {
            storage.read("slot-1.json") shouldBe null
        }

        it("gives the text that was written, also after the file is opened again") {
            storage.write("slot-1.json", "first")
            prefs.reopen()

            storage.read("slot-1.json") shouldBe "first"
        }
    }

    describe("write") {
        it("flushes once for each write and keeps one key for each name") {
            storage.write("slot-1.json", "first")
            storage.write("slot-1.json", "second")
            storage.write("slot-2.json", "other")

            prefs.flushCount shouldBe 3
            prefs.keys shouldBe setOf("slot-1.json", "slot-2.json")
            storage.read("slot-1.json") shouldBe "second"
        }
    }

    describe("with save slots") {
        it("gives back a saved slot and does not flush for a slot with a newer version") {
            val slots = SaveSlots(storage)
            slots.write(1, SaveFile.of(GameState(), savedAt = "t")) shouldBe true
            prefs.put("slot-2.json", """{ "version": 2, "savedAt": "t", "progress": {} }""")

            slots.write(2, SaveFile.of(GameState(), savedAt = "t")) shouldBe false

            prefs.flushCount shouldBe 1
            slots.read(1).shouldBeInstanceOf<SaveSlotContent.Used>()
            slots.read(2).shouldBeInstanceOf<SaveSlotContent.NewerVersion>()
        }
    }
})
