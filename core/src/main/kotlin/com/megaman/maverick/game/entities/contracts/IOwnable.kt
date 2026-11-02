package com.megaman.maverick.game.entities.contracts

import com.maverick.game.engine.entities.IGameEntity

interface IOwnable<T : IGameEntity> : IMegaGameEntity {

    var owner: T?
}
