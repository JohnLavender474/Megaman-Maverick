package com.maverick.game.engine.entities.contracts

import com.badlogic.gdx.utils.Array

interface IParentEntity<T> {

    var children: Array<T>
}
