package com.megaman.maverick.game.levels

import com.badlogic.gdx.utils.OrderedMap

data class LevelMap(
    val bossLevels: OrderedMap<String, RobotMasterLevelDefinition>,
    val wilyLevels: OrderedMap<String, WilyLevelDefinition>
)
