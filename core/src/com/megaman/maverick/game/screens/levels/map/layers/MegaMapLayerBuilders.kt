package com.megaman.maverick.game.screens.levels.map.layers

import com.badlogic.gdx.maps.MapLayers
import com.badlogic.gdx.utils.ObjectMap
import com.engine.common.interfaces.Initializable
import com.engine.common.objects.Properties
import com.engine.screens.levels.tiledmap.builders.ITiledMapLayerBuilder
import com.engine.screens.levels.tiledmap.builders.TiledMapLayerBuilders
import com.engine.spawns.SpawnsManager
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame

data class MegaMapLayerBuildersParams(
    val game: MegamanMaverickGame,
    val spawnsManager: SpawnsManager
)

class MegaMapLayerBuilders(private val params: MegaMapLayerBuildersParams) :
    TiledMapLayerBuilders(), Initializable {

  override val layerBuilders = ObjectMap<String, ITiledMapLayerBuilder>()
  private var initialized = false

  override fun init() {
    if (initialized) return
    initialized = true
    layerBuilders.put(ConstKeys.PLAYER, PlayerLayerBuilder())
    layerBuilders.put(ConstKeys.ENEMIES, SpawnersLayerBuilder(params))
    layerBuilders.put(ConstKeys.BLOCKS, SpawnersLayerBuilder(params))
    layerBuilders.put(ConstKeys.ITEMS, SpawnersLayerBuilder(params))
    layerBuilders.put(ConstKeys.TRIGGERS, TriggersLayerBuilder(params))
    layerBuilders.put(ConstKeys.BACKGROUNDS, BackgroundLayerBuilder(params))
    layerBuilders.put(ConstKeys.FOREGROUNDS, ForegroundLayerBuilder(params))
    layerBuilders.put(ConstKeys.GAME_ROOMS, GameRoomsLayerBuilder())
    layerBuilders.put(ConstKeys.HAZARDS, SpawnersLayerBuilder(params))
    layerBuilders.put(ConstKeys.SPECIALS, SpawnersLayerBuilder(params))
    layerBuilders.put(ConstKeys.SENSORS, SensorsLayerBuilder(params.game))
  }

  override fun build(layers: MapLayers, returnProps: Properties) {
    if (!initialized) init()
    super.build(layers, returnProps)
  }
}
