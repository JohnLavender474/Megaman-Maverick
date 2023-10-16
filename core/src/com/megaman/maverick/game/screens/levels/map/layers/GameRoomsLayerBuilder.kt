package com.megaman.maverick.game.screens.levels.map.layers

import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.utils.Array
import com.engine.common.objects.Properties
import com.engine.screens.levels.tiledmap.builders.ITiledMapLayerBuilder
import com.megaman.maverick.game.ConstKeys

/** This class is responsible for building the game rooms layer. */
class GameRoomsLayerBuilder : ITiledMapLayerBuilder {

  /**
   * Builds the game rooms layer.
   *
   * @param layer the [MapLayer] to build.
   * @param returnProps the [Properties] to use as a return container.
   */
  override fun build(layer: MapLayer, returnProps: Properties) {
    val gameRooms = Array<RectangleMapObject>()
    layer.objects.forEach { if (it is RectangleMapObject) gameRooms.add(it) }
    returnProps.put(ConstKeys.GAME_ROOMS, gameRooms)
  }
}
