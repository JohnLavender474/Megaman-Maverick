package com.megaman.maverick.game.entities.projectiles

import com.engine.damage.IDamageable
import com.engine.entities.GameEntity
import com.engine.entities.IGameEntity
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.IProjectileEntity

class MegaChargedShot(game: MegamanMaverickGame) : GameEntity(game), IProjectileEntity {

  override var owner: IGameEntity?
    get() = TODO("Not yet implemented")
    set(value) {}

  override fun canDamage(damageable: IDamageable): Boolean {
    TODO("Not yet implemented")
  }

  override fun onDamageInflictedTo(damageable: IDamageable) {
    TODO("Not yet implemented")
  }
}
