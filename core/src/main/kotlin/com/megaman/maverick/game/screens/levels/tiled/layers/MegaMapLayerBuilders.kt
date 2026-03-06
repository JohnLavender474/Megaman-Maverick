package com.megaman.maverick.game.screens.levels.tiled.layers

import com.badlogic.gdx.maps.MapLayers
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.screens.levels.tiledmap.builders.ITiledMapLayerBuilder
import com.mega.game.engine.screens.levels.tiledmap.builders.TiledMapLayerBuilders
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.spawns.SpawnsManager

data class MegaMapLayerBuildersParams(val game: MegamanMaverickGame, val spawnsManager: SpawnsManager)

class MegaMapLayerBuilders(private val mapLayerParams: MegaMapLayerBuildersParams) : TiledMapLayerBuilders(), Initializable {

    override val layerBuilders = OrderedMap<String, ITiledMapLayerBuilder>()

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
        super.build(layers, returnProps)
    }
}
