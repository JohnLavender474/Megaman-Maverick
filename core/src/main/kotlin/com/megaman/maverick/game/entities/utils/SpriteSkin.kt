package com.megaman.maverick.game.entities.utils

import com.badlogic.gdx.utils.ObjectSet
import com.maverick.game.engine.common.objects.Properties
import com.megaman.maverick.game.ConstKeys

enum class SpriteSkin {
    DEFAULT,
    WILY,
    LAB;

    fun regionName(tag: String, key: String) = when (this) {
        DEFAULT -> "$tag/$key"
        else -> "$tag/${name.lowercase()}/$key"
    }

    companion object {

        fun of(tag: String, spawnProps: Properties, skins: ObjectSet<SpriteSkin>): SpriteSkin {
            val value = spawnProps.getOrDefault(ConstKeys.COLOR, DEFAULT.name, String::class)
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) && skins.contains(it) }
                ?: throw IllegalArgumentException(
                    "Bad sprite skin of $tag in property '${ConstKeys.COLOR}': '$value'. Use one of " +
                        "${entries.filter { skins.contains(it) }.joinToString { it.name.lowercase() }} " +
                        "or leave the property out."
                )
        }
    }
}
