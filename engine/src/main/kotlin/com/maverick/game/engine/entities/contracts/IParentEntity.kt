package com.maverick.game.engine.entities.contracts

import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.entities.IGameEntity

interface IParentEntity<T> : IGameEntity {

    var children: Array<T>
}
