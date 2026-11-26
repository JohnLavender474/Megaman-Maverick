package com.megaman.maverick.game.state.saves

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue
import com.badlogic.gdx.utils.JsonValue.ValueType
import com.badlogic.gdx.utils.JsonWriter.OutputType
import com.badlogic.gdx.utils.SerializationException
import com.maverick.game.engine.common.GameLogger
import com.megaman.maverick.game.difficulty.DifficultyMode
import com.megaman.maverick.game.entities.megaman.constants.MegaEnhancement
import com.megaman.maverick.game.entities.megaman.constants.MegaHealthTank
import com.megaman.maverick.game.entities.megaman.constants.MegaHeartTank
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.levels.LevelType
import kotlin.enums.EnumEntries

object SaveFileJson {

    const val TAG = "SaveFileJson"
    const val VERSION = 1L

    private const val VERSION_KEY = "version"
    private const val SAVED_AT_KEY = "savedAt"
    private const val PROGRESS_KEY = "progress"
    private const val LEVELS_COMPLETED_KEY = "levelsCompleted"
    private const val HEART_TANKS_KEY = "heartTanks"
    private const val HEALTH_TANKS_KEY = "healthTanks"
    private const val ENHANCEMENTS_KEY = "enhancements"
    private const val CURRENCY_KEY = "currency"
    private const val DIFFICULTY_KEY = "difficulty"
    private const val SINGLE_LINE_COLUMNS = 100

    private class UnreadableFieldException(message: String) : Exception(message)

    fun write(save: SaveFile): String {
        val progress = JsonValue(ValueType.`object`)
        progress.addChild(LEVELS_COMPLETED_KEY, namesOf(save.levelsCompleted))
        progress.addChild(HEART_TANKS_KEY, namesOf(save.heartTanks))
        progress.addChild(HEALTH_TANKS_KEY, namesOf(save.healthTanks))
        progress.addChild(ENHANCEMENTS_KEY, namesOf(save.enhancements))
        progress.addChild(CURRENCY_KEY, JsonValue(save.currency.toLong()))
        progress.addChild(DIFFICULTY_KEY, JsonValue(save.difficulty.name))

        val root = JsonValue(ValueType.`object`)
        root.addChild(VERSION_KEY, JsonValue(VERSION))
        root.addChild(SAVED_AT_KEY, JsonValue(save.savedAt))
        root.addChild(PROGRESS_KEY, progress)
        return root.prettyPrint(OutputType.json, SINGLE_LINE_COLUMNS)
    }

    fun read(text: String): SaveSlotContent {
        val root = try {
            JsonReader().parse(text)
        } catch (e: SerializationException) {
            return SaveSlotContent.Unreadable("not JSON: ${e.message}")
        }
        if (root == null || !root.isObject) return SaveSlotContent.Unreadable("the top level is not a JSON object")

        val version = root.get(VERSION_KEY)
        if (version == null || !version.isLong) return SaveSlotContent.Unreadable("no whole-number \"$VERSION_KEY\"")
        val versionNumber = version.asLong()
        if (versionNumber > VERSION) return SaveSlotContent.NewerVersion(versionNumber)
        if (versionNumber < VERSION) return SaveSlotContent.Unreadable("unknown version $versionNumber")

        return try {
            SaveSlotContent.Used(readVersion1(root))
        } catch (e: UnreadableFieldException) {
            SaveSlotContent.Unreadable(e.message!!)
        }
    }

    private fun readVersion1(root: JsonValue): SaveFile {
        val savedAt = stringField(root, SAVED_AT_KEY)
        val progress = root.get(PROGRESS_KEY)
        if (progress == null || !progress.isObject) throw UnreadableFieldException("no \"$PROGRESS_KEY\" object")

        val levels = Array<LevelDefinition>()
        readNames(progress, LEVELS_COMPLETED_KEY, LevelDefinition.entries).forEach { level ->
            if (level.type == LevelType.EXTRA_LEVEL)
                GameLogger.error(TAG, "read(): an extra level is not recorded as completed: $level")
            else levels.add(level)
        }

        val currency = progress.get(CURRENCY_KEY)
        if (currency == null || !currency.isLong) throw UnreadableFieldException("no whole-number \"$CURRENCY_KEY\"")

        val difficultyName = stringField(progress, DIFFICULTY_KEY)
        val difficulty = DifficultyMode.entries.firstOrNull { it.name == difficultyName }
        if (difficulty == null) GameLogger.error(TAG, "read(): unknown difficulty $difficultyName, so NORMAL is used")

        return SaveFile(
            savedAt = savedAt,
            levelsCompleted = levels,
            heartTanks = readNames(progress, HEART_TANKS_KEY, MegaHeartTank.entries),
            healthTanks = readNames(progress, HEALTH_TANKS_KEY, MegaHealthTank.entries),
            enhancements = readNames(progress, ENHANCEMENTS_KEY, MegaEnhancement.entries),
            currency = currency.asLong().coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt(),
            difficulty = difficulty ?: DifficultyMode.NORMAL
        )
    }

    private fun stringField(parent: JsonValue, key: String): String {
        val value = parent.get(key)
        if (value == null || !value.isString) throw UnreadableFieldException("no text \"$key\"")
        return value.asString()
    }

    private fun <T : Enum<T>> readNames(parent: JsonValue, key: String, entries: EnumEntries<T>): Array<T> {
        val names = parent.get(key)
        if (names == null || !names.isArray) throw UnreadableFieldException("no \"$key\" list")

        val out = Array<T>()
        for (name in names) {
            if (!name.isString) throw UnreadableFieldException("\"$key\" holds a value that is not text: $name")
            val entry = entries.firstOrNull { it.name == name.asString() }
            if (entry == null) GameLogger.error(TAG, "read(): unknown name in \"$key\", skipped: ${name.asString()}")
            else if (!out.contains(entry, true)) out.add(entry)
        }
        return out
    }

    private fun <T : Enum<T>> namesOf(values: Array<T>): JsonValue {
        val array = JsonValue(ValueType.array)
        values.forEach { array.addChild(JsonValue(it.name)) }
        return array
    }
}
