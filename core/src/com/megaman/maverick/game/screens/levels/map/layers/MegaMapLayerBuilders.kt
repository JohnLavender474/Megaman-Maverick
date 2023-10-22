package com.megaman.maverick.game.screens.levels.map.layers

import com.badlogic.gdx.maps.MapLayers
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.utils.ObjectMap
import com.engine.IGame2D
import com.engine.common.interfaces.Initializable
import com.engine.common.objects.Properties
import com.engine.screens.levels.tiledmap.builders.ITiledMapLayerBuilder
import com.engine.screens.levels.tiledmap.builders.TiledMapLayerBuilders
import com.engine.spawns.SpawnsManager
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame

/**
 * The parameters used to initialize the [MegaMapLayerBuilders] class.
 *
 * @param game the [IGame2D] instance.
 * @param spawnsManager the [SpawnsManager] instance.
 */
data class MegaMapLayerBuildersParams(
    val game: MegamanMaverickGame,
    val spawnsManager: SpawnsManager
)

/**
 * Builds all layers of a [TiledMap] using the [ITiledMapLayerBuilder]s that are added to this.
 *
 * @param params the [MegaMapLayerBuildersParams] to use to initialize this [MegaMapLayerBuilders]
 *   instance.
 */
class MegaMapLayerBuilders(private val params: MegaMapLayerBuildersParams) :
    TiledMapLayerBuilders(), Initializable {

  override val layerBuilders = ObjectMap<String, ITiledMapLayerBuilder>()
  private var initialized = false

  /**
   * Initializes the [MegaMapLayerBuilders] by adding all [ITiledMapLayerBuilder]s to the
   * [layerBuilders]. This method is called automatically by the [build] method if this [init]
   * method has not already been called. It is only called once per [MegaMapLayerBuilders] instance,
   * so a new [MegaMapLayerBuilders] instance should be created for each map that needs to be built.
   */
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
  }

  override fun build(layers: MapLayers, returnProps: Properties) {
    if (!initialized) init()
    super.build(layers, returnProps)
  }
}
