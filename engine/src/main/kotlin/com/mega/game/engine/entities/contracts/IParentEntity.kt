package com.mega.game.engine.entities.contracts

import com.badlogic.gdx.utils.Array
import com.mega.game.engine.entities.IGameEntity

interface IParentEntity {

    var children: Array<IGameEntity>
}
