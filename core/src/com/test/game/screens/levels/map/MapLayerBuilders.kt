package com.test.game.screens.levels.map

import com.badlogic.gdx.utils.ObjectMap
import com.engine.IGame2D
import com.engine.screens.levels.tiledmap.ITiledMapLayerBuilder
import com.test.game.screens.levels.map.layers.*

class MapLayerBuilders(game: IGame2D) : ObjectMap<String, ITiledMapLayerBuilder>() {

  init {
    put("player", PlayerLayerBuilder(game))
    put("enemies", EnemiesLayerBuilder(game))
    put("items", ItemsLayerBuilder(game))
    put("triggers", TriggersLayerBuilder(game))
    put("background", BackgroundLayerBuilder(game))
    put("foreground", ForegroundLayerBuilder(game))
  }
}
