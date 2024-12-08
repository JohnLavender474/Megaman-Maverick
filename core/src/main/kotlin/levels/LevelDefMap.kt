package com.megaman.maverick.game.levels

import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.OrderedSet
import com.mega.game.engine.common.extensions.putIfAbsentAndGet

class LevelDefMap {

    private val levelDefs = OrderedMap<String, LevelDefinition>()
    private val levelTypeToKeys = OrderedMap<LevelType, OrderedSet<String>>()
    private val keyToLevelType = OrderedMap<String, LevelType>()

    fun putLevelDef(key: String, levelDef: LevelDefinition) {
        levelDefs.put(key, levelDef)

        val type = levelDef.type
        levelTypeToKeys.putIfAbsentAndGet(type, OrderedSet()).add(key)

        keyToLevelType.put(key, type)
    }

    fun getLevelDef(key: String): LevelDefinition =
        levelDefs[key] ?: throw IllegalArgumentException("No level def for key=$key")

    fun getLevelType(key: String): LevelType =
        keyToLevelType[key] ?: throw IllegalArgumentException("No level type for key=$key")

    fun getKeysOfLevelType(type: LevelType): OrderedSet<String> =
        levelTypeToKeys[type] ?: throw IllegalArgumentException("No keys for type=$type")
}
