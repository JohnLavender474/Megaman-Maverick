package com.test.game.screens.levels.map

import com.badlogic.gdx.utils.ObjectMap
import com.engine.IGame2D
import com.engine.screens.levels.tiledmap.ITiledMapLayerBuilder
import com.engine.spawns.SpawnsManager
import com.test.game.ConstKeys
import com.test.game.screens.levels.map.layers.*
import com.test.game.screens.levels.spawns.PlayerSpawnsManager

data class MapLayerBuildersParams(val game: IGame2D, val spawnsManager: SpawnsManager)

class MapLayerBuilders(params: MapLayerBuildersParams) :
    ObjectMap<String, ITiledMapLayerBuilder>() {

  init {
    put(ConstKeys.PLAYER, PlayerLayerBuilder(params))
    put(ConstKeys.ENEMIES, EnemiesLayerBuilder(params))
    put(ConstKeys.BLOCKS, BlocksLayerBuilder(params))
    put(ConstKeys.ITEMS, ItemsLayerBuilder(params))
    put(ConstKeys.TRIGGERS, TriggersLayerBuilder(params))
    put(ConstKeys.BACKGROUNDS, BackgroundLayerBuilder(params))
    put(ConstKeys.FOREGROUNDS, ForegroundLayerBuilder(params))
  }
}
