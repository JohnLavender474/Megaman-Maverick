package com.megaman.maverick.game

import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.screens.ScreenEnum

sealed interface LaunchStart {

    data object Default : LaunchStart

    data class OpenScreen(val screen: ScreenEnum, val stagedLevel: LevelDefinition?) : LaunchStart

    data class StartLevel(val level: LevelDefinition) : LaunchStart
}

data class ResolvedLaunch(
    val start: LaunchStart,
    val room: String?,
    val logTags: Set<String>,
    val logAllTags: Boolean,
    val autoPerf: Boolean
)

class LaunchResult(val resolved: ResolvedLaunch?, val errors: List<String>) {

    val isValid: Boolean
        get() = errors.isEmpty()
}

private const val LOG_ALL_TAGS = "all"

private val SCREENS_THAT_NEED_A_LEVEL = setOf(ScreenEnum.LEVEL_SCREEN, ScreenEnum.ROBOT_MASTER_INTRO_SCREEN)

fun resolveLaunchOptions(options: LaunchOptions): LaunchResult {
    val errors = ArrayList<String>()

    val screen = options.screen?.takeIf { it.isNotBlank() }
        ?.let { parseEnumOption<ScreenEnum>("--screen", it, errors) }
    val level = options.level?.takeIf { it.isNotBlank() }
        ?.let { parseEnumOption<LevelDefinition>("--level", it, errors) }

    val levelGiven = !options.level.isNullOrBlank()
    val room = options.room?.trim()?.takeIf { it.isNotEmpty() }
    if (room != null && !levelGiven) errors.add("--room '$room' needs --level.")
    if (screen in SCREENS_THAT_NEED_A_LEVEL && !levelGiven) errors.add("--screen $screen needs --level.")

    if (errors.isNotEmpty()) return LaunchResult(null, errors)

    val start = when {
        screen == null && level == null -> LaunchStart.Default
        screen == null || screen == ScreenEnum.LEVEL_SCREEN -> LaunchStart.StartLevel(level!!)
        else -> LaunchStart.OpenScreen(screen, level)
    }

    val tags = options.logTags?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
    val logAllTags = tags.any { it.equals(LOG_ALL_TAGS, ignoreCase = true) }
    val logTags = tags.filterNot { it.equals(LOG_ALL_TAGS, ignoreCase = true) }.toSet()

    return LaunchResult(ResolvedLaunch(start, room, logTags, logAllTags, options.autoPerf), errors)
}

inline fun <reified E : Enum<E>> parseEnumOption(optionName: String, value: String, errors: MutableList<String>): E? {
    val entries = enumValues<E>()
    entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }?.let { return it }

    val names = entries.map { it.name }
    val suggestion = nearestName(value, names)?.let { " Did you mean $it?" } ?: ""
    errors.add("Invalid $optionName '$value'.$suggestion Allowed: ${names.joinToString(", ")}")
    return null
}

fun nearestName(value: String, names: List<String>): String? {
    fun normalize(text: String) = text.uppercase().filter { it.isLetterOrDigit() }

    val wanted = normalize(value)
    if (wanted.isEmpty()) return null

    names.filter { normalize(it).contains(wanted) }.minByOrNull { it.length }?.let { return it }

    return names
        .map { it to editDistance(wanted, normalize(it)) }
        .filter { (name, distance) -> distance <= maxOf(2, normalize(name).length / 3) }
        .minByOrNull { (_, distance) -> distance }
        ?.first
}

private fun editDistance(a: String, b: String): Int {
    var previous = IntArray(b.length + 1) { it }
    for (i in 1..a.length) {
        val current = IntArray(b.length + 1)
        current[0] = i
        for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            current[j] = minOf(current[j - 1] + 1, previous[j] + 1, previous[j - 1] + cost)
        }
        previous = current
    }
    return previous[b.length]
}
