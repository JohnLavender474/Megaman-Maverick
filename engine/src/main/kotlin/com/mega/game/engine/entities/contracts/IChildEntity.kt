package com.mega.game.engine.entities.contracts

import com.mega.game.engine.entities.IGameEntity

interface IChildEntity {

    var parent: IGameEntity?
}
