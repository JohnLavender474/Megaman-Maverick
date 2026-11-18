package com.megaman.maverick.game.entities.utils

import com.maverick.game.engine.common.objects.Properties
import com.megaman.maverick.game.ConstKeys

enum class SpriteSkin {
    DEFAULT,
    WILY;

    fun regionName(tag: String, key: String) = when (this) {
        DEFAULT -> "$tag/$key"
        else -> "$tag/${name.lowercase()}/$key"
    }

    companion object {

        fun of(spawnProps: Properties): SpriteSkin {
            val value = spawnProps.getOrDefault(ConstKeys.COLOR, DEFAULT.name, String::class)
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException(
                    "Bad sprite skin in property '${ConstKeys.COLOR}': '$value'. " +
                        "Use one of ${entries.joinToString { it.name.lowercase() }} or leave the property out."
                )
        }
    }
}
