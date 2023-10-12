package com.test.game.screens.levels

import com.engine.IGame2D
import com.engine.common.objects.Properties
import com.engine.events.Event
import com.engine.screens.levels.tiledmap.TiledMapLevelScreen
import com.test.game.screens.levels.map.MapLayerBuilders

class MegaLevelScreen(private val game: IGame2D, private val props: Properties) :
    TiledMapLevelScreen(game.batch, props.get("tmx_src", String::class)!!) {

  // TODO: implement level variables
  /*
    private val spawnsMan = SpawnsManager()
   private final PlayerSpawnsManager playerSpawnsMan;
   private final LevelStateHandler stateHandler;
   private final PlayerStatsHandler playerStatsHandler;
   private final PlayerSpawnEventHandler playerSpawnEventHandler;
   private final PlayerDeathEventHandler playerDeathEventHandler;
  */

  override fun show() {
    super.show()
    game.eventsMan.addListener(this)
    props.get("level_music", String::class)?.let { game.audioMan.playMusic(it, true) }
  }

  override fun getLayerBuilders() = MapLayerBuilders(game)

  override fun buildLevel(result: Properties) {
    TODO("Not yet implemented")
  }

  override fun render(delta: Float) {
    TODO("Not yet implemented")
  }

  override fun hide() {
    TODO("Not yet implemented")
  }

  override fun onEvent(event: Event) {
    TODO("Not yet implemented")
  }

  override fun pause() {
    TODO("Not yet implemented")
  }

  override fun resize(width: Int, height: Int) {
    TODO("Not yet implemented")
  }

  override fun resume() {
    TODO("Not yet implemented")
  }
}
