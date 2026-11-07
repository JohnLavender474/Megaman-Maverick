package com.megaman.maverick.game.players

import com.badlogic.gdx.Input
import com.maverick.game.engine.players.PlayerSlot
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.controllers.SelectButtonAction
import com.megaman.maverick.game.input.GamepadInput
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class PlayerSettingsStoreTest : DescribeSpec({

    val slot0File = "Megaman Maverick - Player Settings - Slot 0"
    val slot1File = "Megaman Maverick - Player Settings - Slot 1"
    val selectKey = "SELECT_BUTTON_ACTION"

    lateinit var files: FakePreferencesFiles
    lateinit var store: PlayerSettingsStore
    lateinit var settings: PlayerSettings

    beforeEach {
        files = FakePreferencesFiles()
        files.install()
        store = PlayerSettingsStore(PlayerSlot(0))
        settings = PlayerSettings()
    }

    describe("files") {
        it("opens one file per slot, and the values of 2 slots do not mix") {
            val other = PlayerSettingsStore(PlayerSlot(1))
            settings.selectButtonAction = SelectButtonAction.AIR_DASH
            val otherSettings = PlayerSettings().apply { selectButtonAction = SelectButtonAction.START }

            store.saveSelectButtonAction(settings)
            other.saveSelectButtonAction(otherSettings)

            files.opened.toSet() shouldBe setOf(slot0File, slot1File)
            store.loadSelectButtonAction() shouldBe SelectButtonAction.AIR_DASH
            other.loadSelectButtonAction() shouldBe SelectButtonAction.START
        }
    }

    describe("keyboard table") {
        it("writes 1 key for each button and flushes once") {
            val file = files.file(slot0File)

            store.saveKeyboard(settings)

            file.keys shouldBe MegaControllerButton.entries.map { "KEYBOARD.${it.name}" }.toSet()
            file.flushCount shouldBe 1
            file.reopen()
            file.keys.size shouldBe MegaControllerButton.entries.size
        }

        it("loads a partial table: each saved key changes, and each missing key keeps the current code") {
            files.file(slot0File).put("KEYBOARD.A", Input.Keys.I.toString())
            settings.keyCodes[MegaControllerButton.B.index] = Input.Keys.Z

            store.loadKeyboard(settings)

            settings.keyCodes[MegaControllerButton.A.index] shouldBe Input.Keys.I
            settings.keyCodes[MegaControllerButton.B.index] shouldBe Input.Keys.Z
            settings.keyCodes[MegaControllerButton.UP.index] shouldBe MegaControllerButton.UP.defaultKeyboardKey
        }

        it("unbinds a button missing from an old save when its default key is already saved for another button") {
            val file = files.file(slot0File)
            MegaControllerButton.entries.filter { it != MegaControllerButton.SELECT }.forEach {
                file.put("KEYBOARD.${it.name}", it.defaultKeyboardKey.toString())
            }
            file.put("KEYBOARD.B", Input.Keys.L.toString())

            store.loadKeyboard(settings)

            settings.keyCodes[MegaControllerButton.B.index] shouldBe Input.Keys.L
            settings.keyCodes[MegaControllerButton.SELECT.index] shouldBe PlayerSettings.NO_CODE
        }

        it("keeps the default of a button missing from an old save when no other button has it") {
            val file = files.file(slot0File)
            MegaControllerButton.entries.filter { it != MegaControllerButton.SELECT }.forEach {
                file.put("KEYBOARD.${it.name}", it.defaultKeyboardKey.toString())
            }

            store.loadKeyboard(settings)

            settings.keyCodes[MegaControllerButton.SELECT.index] shouldBe Input.Keys.L
        }
    }

    describe("gamepad tables") {
        it("gives null for a missing key") {
            store.readGamepadCode("Pad", MegaControllerButton.A) shouldBe null
        }

        it("gives a saved code, also 0") {
            val file = files.file(slot0File)
            file.put("GAMEPAD.Pad.A", "7")
            file.put("GAMEPAD.Pad.B", "0")

            store.readGamepadCode("Pad", MegaControllerButton.A) shouldBe 7
            store.readGamepadCode("Pad", MegaControllerButton.B) shouldBe 0
        }

        it("writes 1 key for each button of the pad name and flushes once") {
            val file = files.file(slot0File)
            MegaControllerButton.entries.forEach { settings.padCodes[it.index] = it.ordinal }

            store.saveGamepad("Pad", settings)

            file.keys shouldBe MegaControllerButton.entries.map { "GAMEPAD.Pad.${it.name}" }.toSet()
            file.flushCount shouldBe 1
        }

        it("does not mix the keyboard table and a gamepad table") {
            settings.keyCodes[MegaControllerButton.A.index] = Input.Keys.I
            settings.padCodes[MegaControllerButton.A.index] = 5

            store.saveKeyboard(settings)
            store.saveGamepad("Pad", settings)

            val loaded = PlayerSettings()
            store.loadKeyboard(loaded)
            store.loadGamepad("Pad", loaded)
            loaded.keyCodes[MegaControllerButton.A.index] shouldBe Input.Keys.I
            loaded.padCodes[MegaControllerButton.A.index] shouldBe 5
        }

        it("does not mix 2 pad names") {
            settings.padCodes[MegaControllerButton.A.index] = 5
            store.saveGamepad("Pad One", settings)
            settings.padCodes[MegaControllerButton.A.index] = 6
            store.saveGamepad("Pad Two", settings)

            store.readGamepadCode("Pad One", MegaControllerButton.A) shouldBe 5
            store.readGamepadCode("Pad Two", MegaControllerButton.A) shouldBe 6
        }

        it("loads a partial table, and a pad name with no table changes no code") {
            files.file(slot0File).put("GAMEPAD.Pad.A", "5")
            settings.padCodes[MegaControllerButton.B.index] = 6

            store.loadGamepad("Pad", settings)

            settings.padCodes[MegaControllerButton.A.index] shouldBe 5
            settings.padCodes[MegaControllerButton.B.index] shouldBe 6

            store.loadGamepad("No Table", settings)

            settings.padCodes[MegaControllerButton.A.index] shouldBe 5
            settings.padCodes[MegaControllerButton.B.index] shouldBe 6
        }

        it("round-trips the trigger codes") {
            settings.padCodes[MegaControllerButton.A.index] = GamepadInput.TRIGGER_LEFT_CODE
            settings.padCodes[MegaControllerButton.B.index] = GamepadInput.TRIGGER_RIGHT_CODE

            store.saveGamepad("Pad", settings)
            files.file(slot0File).reopen()

            store.readGamepadCode("Pad", MegaControllerButton.A) shouldBe 1004
            store.readGamepadCode("Pad", MegaControllerButton.B) shouldBe 1005
        }

        it("keeps pad names through a round trip of the XML") {
            val names = listOf("A&B <Pad>", "PS3 Controller", "Pad.With.Dots")
            names.forEachIndexed { i, name ->
                settings.padCodes[MegaControllerButton.A.index] = i
                store.saveGamepad(name, settings)
            }

            files.file(slot0File).reopen()

            names.forEachIndexed { i, name -> store.readGamepadCode(name, MegaControllerButton.A) shouldBe i }
        }
    }

    describe("selectButtonAction") {
        it("round-trips each value") {
            SelectButtonAction.entries.forEach {
                settings.selectButtonAction = it

                store.saveSelectButtonAction(settings)
                files.file(slot0File).reopen()

                store.loadSelectButtonAction() shouldBe it
            }
        }

        it("gives null for a missing key") {
            store.loadSelectButtonAction() shouldBe null
        }

        it("gives null for an unknown name") {
            files.file(slot0File).put(selectKey, "NOT_AN_ACTION")

            store.loadSelectButtonAction() shouldBe null
        }

        it("keeps one key after a save from each bindings screen") {
            settings.selectButtonAction = SelectButtonAction.AIR_DASH
            store.saveSelectButtonAction(settings)
            settings.selectButtonAction = SelectButtonAction.START
            store.saveSelectButtonAction(settings)

            files.file(slot0File).keys shouldBe setOf(selectKey)
            store.loadSelectButtonAction() shouldBe SelectButtonAction.START
        }
    }

    describe("clear") {
        it("removes the keyboard table, 2 pad tables and the select action, and flushes") {
            val file = files.file(slot0File)
            store.saveKeyboard(settings)
            store.saveGamepad("Pad One", settings)
            store.saveGamepad("Pad Two", settings)
            store.saveSelectButtonAction(settings)
            val flushes = file.flushCount

            store.clear()

            file.keys shouldBe emptySet()
            file.flushCount shouldBe flushes + 1
            file.reopen()
            file.keys shouldBe emptySet()
        }

        it("gives no exception with no file on disk, and leaves an empty file") {
            val file = files.file(slot0File)
            file.onDisk shouldBe false

            store.clear()

            file.onDisk shouldBe true
            file.reopen()
            file.keys shouldBe emptySet()
        }
    }

    describe("table resets") {
        fun saveAll() {
            store.saveKeyboard(settings)
            store.saveGamepad("Pad One", settings)
            store.saveGamepad("Pad Two", settings)
            store.saveSelectButtonAction(settings)
        }

        fun keysOf(prefix: String) = MegaControllerButton.entries.map { "$prefix.${it.name}" }.toSet()

        it("the keyboard reset removes its table and the select action, keeps every pad table, and flushes") {
            val file = files.file(slot0File)
            saveAll()
            val flushes = file.flushCount

            store.clearKeyboard()

            file.keys shouldBe keysOf("GAMEPAD.Pad One") + keysOf("GAMEPAD.Pad Two")
            file.flushCount shouldBe flushes + 1
        }

        it("the gamepad reset removes its table and the select action, keeps the others, and flushes") {
            val file = files.file(slot0File)
            saveAll()
            val flushes = file.flushCount

            store.clearGamepad("Pad One")

            file.keys shouldBe keysOf("KEYBOARD") + keysOf("GAMEPAD.Pad Two")
            file.flushCount shouldBe flushes + 1
        }
    }

    describe("the file name") {
        it("opens only the slot file over all store functions, never an old file") {
            store.saveKeyboard(settings)
            store.loadKeyboard(settings)
            store.saveGamepad("Pad", settings)
            store.loadGamepad("Pad", settings)
            store.readGamepadCode("Pad", MegaControllerButton.A)
            store.saveSelectButtonAction(settings)
            store.loadSelectButtonAction()
            store.clearKeyboard()
            store.clearGamepad("Pad")
            store.clear()
            applyBootSettings(store, settings, true, true)

            files.opened.toSet() shouldBe setOf(slot0File)
        }
    }

    describe("applyBootSettings") {
        val flagCombinations = listOf(false to false, true to false, false to true, true to true)

        fun saveKeyA() = files.file(slot0File).put("KEYBOARD.A", Input.Keys.I.toString())

        fun saveSelect() = files.file(slot0File).put(selectKey, SelectButtonAction.AIR_DASH.name)

        fun saveGamepadTable() {
            val table = PlayerSettings()
            MegaControllerButton.entries.forEach { table.padCodes[it.index] = it.ordinal }
            store.saveGamepad("Pad", table)
        }

        fun PlayerSettings.defaultKeys() =
            keyCodes.toList() == MegaControllerButton.entries.map { it.defaultKeyboardKey }

        fun PlayerSettings.noPadCodes() = padCodes.all { it == PlayerSettings.NO_CODE }

        it("with no flag, gives the default keys and no pad codes, and applies the saved select action") {
            saveKeyA()
            saveSelect()

            applyBootSettings(store, settings, false, false)

            settings.defaultKeys() shouldBe true
            settings.noPadCodes() shouldBe true
            settings.selectButtonAction shouldBe SelectButtonAction.AIR_DASH
        }

        it("with --loadKeyboardPrefs, applies the saved keys and the saved select action") {
            saveKeyA()
            saveSelect()

            applyBootSettings(store, settings, true, false)

            settings.keyCodes[MegaControllerButton.A.index] shouldBe Input.Keys.I
            settings.selectButtonAction shouldBe SelectButtonAction.AIR_DASH
        }

        it("with --resetBindings, gives the defaults and leaves the file with no keys") {
            saveKeyA()
            saveSelect()

            applyBootSettings(store, settings, false, true)

            settings.defaultKeys() shouldBe true
            settings.selectButtonAction shouldBe SelectButtonAction.NEXT_WEAPON
            files.file(slot0File).keys shouldBe emptySet()
        }

        it("with --resetBindings and --loadKeyboardPrefs, clears first and gives the same result") {
            saveKeyA()
            saveSelect()

            applyBootSettings(store, settings, true, true)

            settings.defaultKeys() shouldBe true
            settings.selectButtonAction shouldBe SelectButtonAction.NEXT_WEAPON
            files.file(slot0File).keys shouldBe emptySet()
        }

        it("gives NEXT_WEAPON with no saved select action, with each flag combination") {
            saveKeyA()
            flagCombinations.forEach { (load, reset) ->
                val fresh = PlayerSettings()

                applyBootSettings(store, fresh, load, reset)

                fresh.selectButtonAction shouldBe SelectButtonAction.NEXT_WEAPON
            }
        }

        it("does not apply a saved gamepad table, with each flag combination") {
            saveGamepadTable()
            flagCombinations.forEach { (load, reset) ->
                val fresh = PlayerSettings()

                applyBootSettings(store, fresh, load, reset)

                fresh.noPadCodes() shouldBe true
            }
        }
    }
})
