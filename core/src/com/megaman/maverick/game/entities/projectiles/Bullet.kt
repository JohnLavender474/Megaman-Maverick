package com.megaman.maverick.game.entities.projectiles

import com.engine.IGame2D
import com.engine.entities.GameEntity
import com.engine.entities.IGameEntity
import com.megaman.maverick.game.entities.IProjectileEntity

class Bullet(game: IGame2D) : GameEntity(game), IProjectileEntity {

  override var owner: IGameEntity?
    get() = TODO("Not yet implemented")
    set(value) {}
}
