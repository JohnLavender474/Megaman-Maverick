package com.mega.game.engine

import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.entities.GameEntity
import io.mockk.mockk

class MockGameEntity(
    engine: GameEngine,
    var init: (MockGameEntity) -> Unit = {},
    var onSpawn: (MockGameEntity, Properties) -> Unit = { _, _ -> },
    var onDestroy: (MockGameEntity) -> Unit = {}
) : GameEntity(engine) {

    constructor(
        init: (MockGameEntity) -> Unit = {},
        onSpawn: (MockGameEntity, Properties) -> Unit = { _, _ -> },
        onDestroy: (MockGameEntity) -> Unit = {}
    ) : this(mockk(), init, onSpawn, onDestroy)

    override var initialized = false

    override fun init() = init.invoke(this)

    override fun onSpawn(spawnProps: Properties) = onSpawn.invoke(this, spawnProps)

    override fun onDestroy() = onDestroy.invoke(this)
}