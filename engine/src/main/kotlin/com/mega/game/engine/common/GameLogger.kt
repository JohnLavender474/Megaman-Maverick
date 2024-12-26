package com.mega.game.engine.common

import com.badlogic.gdx.ApplicationLogger
import com.badlogic.gdx.utils.ObjectSet

enum class GameLogLevel {
    OFF,
    LOG,
    DEBUG,
    ERROR
}

data class GameLogDef(
    var filterByTag: Boolean = false,
    val tagsToLog: ObjectSet<String> = ObjectSet()
)

object GameLogger : ApplicationLogger {

    val DEFAULT_LOG_FORMATTER: (level: GameLogLevel, tag: String, message: String, throwable: Throwable?) -> String =
        { level, tag, message, throwable ->
            var log = "$level | $tag | $message"
            if (throwable != null) {
                log += " | ${throwable.message}"
                throwable.stackTrace.forEach { line -> log += "\n\t$line" }
            }
            log
        }

    val tagsToLog = ObjectSet<String>()
    var filterByTag = true

    internal var formatter: (level: GameLogLevel, tag: String, message: String, throwable: Throwable?) -> String =
        DEFAULT_LOG_FORMATTER
    internal var level = GameLogLevel.OFF

    fun setLogLevel(level: GameLogLevel) {
        this.level = level
    }

    fun getLogLevel() = level

    fun setLogFormatter(
        formatter: (level: GameLogLevel, tag: String, message: String, throwable: Throwable?) -> String
    ) {
        this.formatter = formatter
    }

    override fun log(tag: String, message: String) = print(GameLogLevel.LOG, tag, message)

    override fun log(tag: String, message: String, exception: Throwable) =
        print(GameLogLevel.LOG, tag, message, exception)

    override fun debug(tag: String, message: String) = print(GameLogLevel.DEBUG, tag, message)

    override fun debug(tag: String, message: String, exception: Throwable?) =
        print(GameLogLevel.DEBUG, tag, message, exception)

    override fun error(tag: String, message: String) = print(GameLogLevel.ERROR, tag, message)

    override fun error(tag: String, message: String, exception: Throwable?) =
        print(GameLogLevel.ERROR, tag, message, exception)

    private fun print(level: GameLogLevel, tag: String, message: String, throwable: Throwable? = null) {
        if (level == GameLogLevel.OFF ||
            this.level.ordinal > level.ordinal ||
            (filterByTag && !tagsToLog.contains(tag))
        ) return

        val string = formatter.invoke(level, tag, message, throwable)
        println(string)
    }
}
