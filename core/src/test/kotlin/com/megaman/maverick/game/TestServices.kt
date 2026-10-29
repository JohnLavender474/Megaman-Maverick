package com.megaman.maverick.game

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.Queue
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.common.interfaces.Updatable
import com.maverick.game.engine.events.EventsManager
import com.maverick.game.engine.groups.GroupsSystem
import com.megaman.maverick.game.audio.MegaAudioManager
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.levels.LevelState
import com.megaman.maverick.game.players.MegaPlayers
import com.megaman.maverick.game.players.WorldGravity
import com.megaman.maverick.game.screens.levels.camera.GameCameraState
import com.megaman.maverick.game.screens.levels.camera.RotatableCamera
import com.megaman.maverick.game.state.GameClock
import com.megaman.maverick.game.state.GameSettings
import com.megaman.maverick.game.state.GameState
import com.megaman.maverick.game.state.Performance
import io.mockk.mockk

fun testServices(
    engine: GameEngine = mockk(relaxed = true),
    eventsMan: EventsManager = EventsManager(),
    assMan: AssetManager = mockk(relaxed = true),
    audioMan: MegaAudioManager = mockk(relaxed = true),
    state: GameState = GameState(),
    players: MegaPlayers = MegaPlayers(),
    gravity: WorldGravity = WorldGravity(players),
    levelState: LevelState = LevelState().apply { currentLevel = LevelDefinition.TEST_1 },
    clock: GameClock = GameClock(),
    camera: GameCameraState = GameCameraState(),
    settings: GameSettings = GameSettings().apply { performance = Performance.MEDIUM },
    updatables: OrderedMap<String, Updatable> = OrderedMap(),
    runQueue: Queue<() -> Unit> = Queue(),
    groups: GroupsSystem = GroupsSystem(),
    gameCamera: RotatableCamera = RotatableCamera()
) = MegaServices(
    engine,
    eventsMan,
    assMan,
    audioMan,
    state,
    players,
    gravity,
    levelState,
    clock,
    camera,
    settings,
    updatables,
    runQueue,
    groups,
    gameCamera
)
