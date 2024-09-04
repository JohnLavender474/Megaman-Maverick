package com.megaman.maverick.game.utils

import com.mega.game.engine.common.objects.Properties

object StringToPropertiesParser {

    fun parse(string: String): Properties {
        val properties = Properties()

        var leastIndex = 0
        while (leastIndex < string.length) {
            val propBeginIndex = string.indexOf("[", leastIndex)
            if (propBeginIndex == -1) break

            val propEndIndex = string.indexOf("]", propBeginIndex)
            if (propEndIndex == -1)
                throw IllegalArgumentException(
                    "Missing closing bracket ']'. Property begins at index $propBeginIndex."
                )

            val keyValueString = string.substring(propBeginIndex + 1, propEndIndex)
            val keyValueSplitIndex = keyValueString.indexOf("=")

            if (keyValueSplitIndex == -1)
                throw IllegalArgumentException("Missing '=' in property: $keyValueString")

            val key = keyValueString.substring(0, keyValueSplitIndex).trim()
            val value = keyValueString.substring(keyValueSplitIndex + 1).trim()

            if (key.isEmpty()) throw IllegalArgumentException("Empty key in property: $keyValueString")

            properties.put(key, value)
            leastIndex = propEndIndex + 1
        }

        return properties
    }
}
