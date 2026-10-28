package com.maverick.game.engine.entities.contracts

import com.maverick.game.engine.entities.IGameEntity

interface IChildEntity : IGameEntity {

    var parent: IGameEntity?
}
