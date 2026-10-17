package com.megaman.maverick.game.screens.levels.tiled.layers

import com.badlogic.gdx.maps.MapLayers
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.indexOfKey
import com.mega.game.engine.common.extensions.orderedMapOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.screens.levels.tiledmap.builders.ITiledMapLayerBuilder
import com.mega.game.engine.screens.levels.tiledmap.builders.ITiledMapLayerBuilders
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.spawns.SpawnsManager

data class MegaMapLayerBuildersParams(val game: MegamanMaverickGame, val spawnsManager: SpawnsManager)

class MegaMapLayerBuilders(private val mapLayerParams: MegaMapLayerBuildersParams) : ITiledMapLayerBuilders {

    companion object {
        const val TAG = "MegaMapLayerBuilders"

        private val MULTI_LAYER_KEYS = setOf(ConstKeys.BLOCKS)

        fun getLayerKey(name: String?): String? {
            if (name == null) return null
            return MULTI_LAYER_KEYS.firstOrNull { name == it || name.startsWith("${it}_") } ?: name
        }
    }

    internal val builderFactories = orderedMapOf(
        ConstKeys.GAME_ROOMS pairTo { GameRoomsLayerBuilder() },
        ConstKeys.PLAYER pairTo { PlayerLayerBuilder() },
        ConstKeys.ENEMIES pairTo { SpawnersLayerBuilder(mapLayerParams) },
        ConstKeys.BLOCKS pairTo { SpawnersLayerBuilder(mapLayerParams) },
        ConstKeys.ITEMS pairTo { SpawnersLayerBuilder(mapLayerParams) },
        ConstKeys.TRIGGERS pairTo { TriggersLayerBuilder(mapLayerParams) },
        ConstKeys.BACKGROUNDS pairTo { BackgroundLayerBuilder(mapLayerParams) },
        ConstKeys.FOREGROUNDS pairTo { ForegroundLayerBuilder(mapLayerParams) },
        ConstKeys.HAZARDS pairTo { SpawnersLayerBuilder(mapLayerParams) },
        ConstKeys.SPECIALS pairTo { SpawnersLayerBuilder(mapLayerParams) },
        ConstKeys.SENSORS pairTo { SensorsLayerBuilder() },
        ConstKeys.DECORATIONS pairTo { SpawnersLayerBuilder(mapLayerParams) },
        ConstKeys.PROJECTILES pairTo { SpawnersLayerBuilder(mapLayerParams) }
    )

    private val builders = ObjectMap<String, ITiledMapLayerBuilder>()

    override fun build(layers: MapLayers, returnProps: Properties) =
        layers
            .filter { it.isVisible }
            .mapNotNull { layer ->
                val key = getLayerKey(layer.name)
                if (key == null || !builderFactories.containsKey(key)) {
                    GameLogger.debug(TAG, "build(): skipping layer with no builder: ${layer.name}")
                    null
                } else key to layer
            }
            .sortedBy { (key, _) -> builderFactories.indexOfKey(key) }
            .forEach { (key, layer) ->
                GameLogger.debug(TAG, "build(): building layer: ${layer.name}")
                getBuilderForLayer(key).build(layer, returnProps)
            }

    private fun getBuilderForLayer(layerKey: String): ITiledMapLayerBuilder {
        var builder = builders[layerKey]
        if (builder == null) {
            builder = builderFactories.get(layerKey).invoke()
            builders.put(layerKey, builder)
        }
        return builder
    }
}
