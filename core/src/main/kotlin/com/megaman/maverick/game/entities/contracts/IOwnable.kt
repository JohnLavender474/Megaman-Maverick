package com.megaman.maverick.game.entities.contracts

interface IOwnable<T> : IMegaGameEntity {

    var owner: T?
}
