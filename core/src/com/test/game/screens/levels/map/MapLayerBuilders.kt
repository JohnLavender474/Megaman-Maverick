package com.test.game.screens.levels.map

import com.badlogic.gdx.utils.ObjectMap
import com.engine.IGame2D
import com.engine.screens.levels.tiledmap.ITiledMapLayerBuilder
import com.test.game.ConstKeys
import com.test.game.screens.levels.map.layers.*

class MapLayerBuilders(game: IGame2D) : ObjectMap<String, ITiledMapLayerBuilder>() {

  init {
    put(ConstKeys.PLAYER, PlayerLayerBuilder())
    put(ConstKeys.ENEMIES, EnemiesLayerBuilder(game))
    put(ConstKeys.BLOCKS, BlocksLayerBuilder(game))
    put(ConstKeys.ITEMS, ItemsLayerBuilder(game))
    put(ConstKeys.TRIGGERS, TriggersLayerBuilder(game))
    put(ConstKeys.BACKGROUNDS, BackgroundLayerBuilder(game))
    put(ConstKeys.FOREGROUNDS, ForegroundLayerBuilder(game))
  }
}
