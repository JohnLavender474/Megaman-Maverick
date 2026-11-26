package com.megaman.maverick.game.state.saves

import com.badlogic.gdx.files.FileHandle
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.state.GameState
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.io.File
import java.nio.file.Files

class FileSaveStorageTest : DescribeSpec({

    lateinit var root: File
    lateinit var folder: File
    lateinit var storage: FileSaveStorage

    beforeEach {
        root = Files.createTempDirectory("save-storage-test").toFile()
        folder = File(root, "saves")
        storage = FileSaveStorage(FileHandle(folder))
    }

    afterEach {
        root.deleteRecursively()
    }

    fun fileNames() = folder.list()?.sorted() ?: emptyList()

    fun slots() = SaveSlots(storage)

    fun saveOf(level: LevelDefinition, savedAt: String) =
        SaveFile.of(GameState().apply { addLevelCompleted(level) }, savedAt = savedAt)

    describe("read") {
        it("gives null when the file does not exist") {
            storage.read("slot-1.json") shouldBe null
        }

        it("reads the backup when the file is missing") {
            folder.mkdirs()
            File(folder, "slot-1.json.bak").writeText("old")

            storage.read("slot-1.json") shouldBe "old"
        }

        it("reads the file, not the backup, when both exist") {
            storage.write("slot-1.json", "first")
            storage.write("slot-1.json", "second")

            storage.read("slot-1.json") shouldBe "second"
        }
    }

    describe("write") {
        it("makes the folder and leaves no temp file and no backup on the first write") {
            storage.write("slot-1.json", "first")

            fileNames() shouldBe listOf("slot-1.json")
            File(folder, "slot-1.json").readText() shouldBe "first"
        }

        it("keeps the last text as the backup") {
            storage.write("slot-1.json", "first")
            storage.write("slot-1.json", "second")
            storage.write("slot-1.json", "third")

            fileNames() shouldBe listOf("slot-1.json", "slot-1.json.bak")
            File(folder, "slot-1.json").readText() shouldBe "third"
            File(folder, "slot-1.json.bak").readText() shouldBe "second"
        }

        it("replaces a temp file that a crash left behind") {
            folder.mkdirs()
            File(folder, "slot-1.json.tmp").writeText("half")

            storage.write("slot-1.json", "first")

            fileNames() shouldBe listOf("slot-1.json")
            File(folder, "slot-1.json").readText() shouldBe "first"
        }

        it("keeps the backup when the file is missing, as after a crash between the two moves") {
            folder.mkdirs()
            File(folder, "slot-1.json.bak").writeText("old")
            File(folder, "slot-1.json.tmp").writeText("half")

            storage.write("slot-1.json", "new")

            fileNames() shouldBe listOf("slot-1.json", "slot-1.json.bak")
            File(folder, "slot-1.json").readText() shouldBe "new"
            File(folder, "slot-1.json.bak").readText() shouldBe "old"
        }

        it("writes each name to its own files") {
            storage.write("slot-1.json", "one")
            storage.write("slot-2.json", "two")
            storage.write("slot-2.json", "two again")

            fileNames() shouldBe listOf("slot-1.json", "slot-2.json", "slot-2.json.bak")
            File(folder, "slot-1.json").readText() shouldBe "one"
        }
    }

    describe("with save slots") {
        it("loads the backup save when the slot file is missing") {
            slots().write(1, saveOf(LevelDefinition.MOON_MAN, savedAt = "first"))
            slots().write(1, saveOf(LevelDefinition.DESERT_MAN, savedAt = "second"))
            File(folder, "slot-1.json").delete()

            val content = slots().read(1)

            content.shouldBeInstanceOf<SaveSlotContent.Used>()
            content.save.savedAt shouldBe "first"
            content.save.levelsCompleted.toList() shouldBe listOf(LevelDefinition.MOON_MAN)
        }

        it("reads a damaged slot file as unreadable and does not fall back to the backup") {
            slots().write(1, saveOf(LevelDefinition.MOON_MAN, savedAt = "first"))
            slots().write(1, saveOf(LevelDefinition.DESERT_MAN, savedAt = "second"))
            val backupText = File(folder, "slot-1.json.bak").readText()
            File(folder, "slot-1.json").writeText("{ \"version\": ")

            slots().read(1).shouldBeInstanceOf<SaveSlotContent.Unreadable>()
            File(folder, "slot-1.json.bak").readText() shouldBe backupText
        }

        it("overwrites an unreadable slot and keeps the damaged text as the backup") {
            folder.mkdirs()
            File(folder, "slot-4.json").writeText("not json {")

            slots().write(4, saveOf(LevelDefinition.MOON_MAN, savedAt = "t")) shouldBe true

            fileNames() shouldBe listOf("slot-4.json", "slot-4.json.bak")
            File(folder, "slot-4.json.bak").readText() shouldBe "not json {"
            slots().read(4).shouldBeInstanceOf<SaveSlotContent.Used>()
        }

        it("does not touch the files of a slot with a newer version") {
            val newer = """{ "version": 2, "savedAt": "2099-01-01T00:00:00Z", "progress": {} }"""
            folder.mkdirs()
            File(folder, "slot-2.json").writeText(newer)

            slots().write(2, saveOf(LevelDefinition.MOON_MAN, savedAt = "t")) shouldBe false

            fileNames() shouldBe listOf("slot-2.json")
            File(folder, "slot-2.json").readText() shouldBe newer
        }
    }
})
