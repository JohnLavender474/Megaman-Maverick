package com.megaman.maverick.game.levels

import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.screens.levels.tiledmap.TiledMapLoadResult
import com.maverick.game.engine.world.container.IWorldContainer
import com.megaman.maverick.game.entities.decorations.FloatingPoints.FloatingPointsType

class LevelState {

    var currentLevel: LevelDefinition? = null
    var roomsSupplier: (() -> Array<RectangleMapObject>?)? = null
    var currentRoomSupplier: (() -> RectangleMapObject?)? = null
    var worldContainer: IWorldContainer? = null
    var tiledMapLoadResult: TiledMapLoadResult? = null
    var roomTransition = false
    var megamanBeam = false
    var megamanBeamCenter: Vector2? = null
    var coins = 0
    var levelPoints = 0
    var timeSinceLastFloatingPoints = 0f
    var lastFloatingPointsType: FloatingPointsType? = null
}
