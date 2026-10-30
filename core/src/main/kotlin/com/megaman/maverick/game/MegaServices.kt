package com.megaman.maverick.game

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.Queue
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.common.interfaces.Updatable
import com.maverick.game.engine.events.EventsManager
import com.maverick.game.engine.groups.GroupsSystem
import com.megaman.maverick.game.audio.MegaAudioManager
import com.megaman.maverick.game.levels.LevelState
import com.megaman.maverick.game.players.MegaPlayers
import com.megaman.maverick.game.players.WorldGravity
import com.megaman.maverick.game.screens.levels.camera.GameCameraState
import com.megaman.maverick.game.screens.levels.camera.RotatableCamera
import com.megaman.maverick.game.state.GameClock
import com.megaman.maverick.game.state.GameSettings
import com.megaman.maverick.game.state.GameState

class MegaServices(
    val engine: GameEngine,
    val events: EventsManager,
    val assets: AssetManager,
    val audio: MegaAudioManager,
    val gameState: GameState,
    val players: MegaPlayers,
    val gravity: WorldGravity,
    val levelState: LevelState,
    val clock: GameClock,
    val camera: GameCameraState,
    val settings: GameSettings,
    val updatables: OrderedMap<String, Updatable>,
    val runQueue: Queue<() -> Unit>,
    val groups: GroupsSystem,
    private val gameCamera: RotatableCamera
) {

    fun getGameCamera() = gameCamera
}
