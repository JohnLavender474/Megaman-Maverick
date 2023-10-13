package com.test.game.screens.levels

import com.engine.IGame2D
import com.engine.common.objects.Properties
import com.engine.events.Event
import com.engine.screens.levels.tiledmap.TiledMapLevelScreen
import com.engine.spawns.SpawnsManager
import com.test.game.ConstKeys
import com.test.game.screens.levels.map.MapLayerBuilders
import com.test.game.screens.levels.spawns.PlayerSpawnsManager

class MegaLevelScreen(private val game: IGame2D, private val props: Properties) :
    TiledMapLevelScreen(game.batch, props.get(ConstKeys.TMX_SRC, String::class)!!) {

  // TODO: implement level variables

  private val spawnsMan = SpawnsManager()
  private val playerSpawnsMan = PlayerSpawnsManager(game.viewports.get(ConstKeys.GAME).camera)
  private val levelStateHandler = LevelStateHandler(game)
  /*
   private final PlayerStatsHandler playerStatsHandler;
   private final PlayerSpawnEventHandler playerSpawnEventHandler;
   private final PlayerDeathEventHandler playerDeathEventHandler;
  */

  override fun show() {
    super.show()
    game.eventsMan.addListener(this)
    props.get(ConstKeys.LEVEL_MUSIC, String::class)?.let { game.audioMan.playMusic(it, true) }
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
