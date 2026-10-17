package com.megaman.maverick.game.levels

import com.megaman.maverick.game.difficulty.DifficultyMode
import com.megaman.maverick.game.entities.contracts.MegaGameEntity

/**
 * The player state and the spawn rules that a level replaces when it starts. When applied, the player's current
 * progress is reset first, so a null value keeps the value that the reset sets.
 *
 * @param loadout the items that Megaman has; the default is no items other than the Mega Buster
 * @param difficultyMode the difficulty mode for the level; null keeps the mode after the reset, which is
 * [DifficultyMode.NORMAL]
 * @param lives the number of lives that Megaman starts the level with; null keeps the lives after the reset
 * @param canSpawn a filter for each entity that tries to spawn while this level is the current level; if it returns
 * false, the entity does not spawn. Null means that every entity can spawn. Unlike the other values, this filter is
 * used for every level type, not only for extra levels.
 */
data class LevelOverride(
    val loadout: LoadoutOverride = LoadoutOverride(),
    val difficultyMode: DifficultyMode? = null,
    val lives: Int? = null,
    val canSpawn: ((entity: MegaGameEntity) -> Boolean)? = null
)
