package com.megaman.maverick.game.screens.levels.tiled.layers

import com.badlogic.gdx.maps.MapLayers
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.screens.levels.tiledmap.builders.ITiledMapLayerBuilder
import com.mega.game.engine.screens.levels.tiledmap.builders.ITiledMapLayerBuilders
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.spawns.SpawnsManager

data class MegaMapLayerBuildersParams(val game: MegamanMaverickGame, val spawnsManager: SpawnsManager)

class MegaMapLayerBuilders(private val mapLayerParams: MegaMapLayerBuildersParams) :
    ITiledMapLayerBuilders, Initializable {

    companion object {
        const val TAG = "MegaMapLayerBuilders"

        fun isBlocksLayer(name: String?) =
            name != null && (name == ConstKeys.BLOCKS || name.startsWith("${ConstKeys.BLOCKS}_"))
    }

    private val layerBuilders = OrderedMap<String, ITiledMapLayerBuilder>()

    private var initialized = false

    override fun init(vararg params: Any) {
        if (initialized) return
        initialized = true
        layerBuilders.put(ConstKeys.GAME_ROOMS, GameRoomsLayerBuilder())
        layerBuilders.put(ConstKeys.PLAYER, PlayerLayerBuilder())
        layerBuilders.put(ConstKeys.ENEMIES, SpawnersLayerBuilder(mapLayerParams))
        layerBuilders.put(ConstKeys.BLOCKS, SpawnersLayerBuilder(mapLayerParams))
        layerBuilders.put(ConstKeys.ITEMS, SpawnersLayerBuilder(mapLayerParams))
        layerBuilders.put(ConstKeys.TRIGGERS, TriggersLayerBuilder(mapLayerParams))
        layerBuilders.put(ConstKeys.BACKGROUNDS, BackgroundLayerBuilder(mapLayerParams))
        layerBuilders.put(ConstKeys.FOREGROUNDS, ForegroundLayerBuilder(mapLayerParams))
        layerBuilders.put(ConstKeys.HAZARDS, SpawnersLayerBuilder(mapLayerParams))
        layerBuilders.put(ConstKeys.SPECIALS, SpawnersLayerBuilder(mapLayerParams))
        layerBuilders.put(ConstKeys.SENSORS, SensorsLayerBuilder())
        layerBuilders.put(ConstKeys.DECORATIONS, SpawnersLayerBuilder(mapLayerParams))
        layerBuilders.put(ConstKeys.PROJECTILES, SpawnersLayerBuilder(mapLayerParams))
    }

    override fun build(layers: MapLayers, returnProps: Properties) {
        if (!initialized) init()

        val layersMap = layers.associateBy { it.name }
        layerBuilders.forEach { entry ->
            val key = entry.key
            val builder = entry.value

            if (key == ConstKeys.BLOCKS) {
                layers
                    .filter { it.isVisible && isBlocksLayer(it.name) }
                    .forEach { layer ->
                        GameLogger.debug(TAG, "build(): building blocks layer: ${layer.name}")
                        builder.build(layer, returnProps)
                    }
                return@forEach
            }

            val layer = layersMap[key]
            if (layer != null && layer.isVisible) builder.build(layer, returnProps)
        }
    }
}
