package com.megaman.maverick.game.state.saves

import com.badlogic.gdx.files.FileHandle
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
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
    }
})
