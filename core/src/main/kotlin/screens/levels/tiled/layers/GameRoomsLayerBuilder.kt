package com.megaman.maverick.game.screens.levels.tiled.layers

import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.screens.levels.tiledmap.builders.ITiledMapLayerBuilder
import com.megaman.maverick.game.ConstKeys

class GameRoomsLayerBuilder : ITiledMapLayerBuilder {

    companion object {
        const val TAG = "GameRoomsLayerBuilder"
    }

    override fun build(layer: MapLayer, returnProps: Properties) {
        val gameRooms = Array<RectangleMapObject>()
        layer.objects.forEach { if (it is RectangleMapObject) gameRooms.add(it) }

        val printedGameRooms = gameRooms.map { "[${it.name}, ${it.rectangle}]" }
        GameLogger.debug(TAG, "build(): Game rooms: $printedGameRooms")

        returnProps.put(ConstKeys.GAME_ROOMS, gameRooms)
    }
}
