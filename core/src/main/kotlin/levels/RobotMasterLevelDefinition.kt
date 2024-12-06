package com.megaman.maverick.game.levels

import com.badlogic.gdx.utils.OrderedMap

data class RobotMasterLevelMap(val map: OrderedMap<String, RobotMasterLevelDefinition>)

data class RobotMasterLevelDefinition(
    var name: String,
    var atlas: String,
    var region: String,
    var level: String,
    var music: String,
    var screenOnCompletion: String
)
