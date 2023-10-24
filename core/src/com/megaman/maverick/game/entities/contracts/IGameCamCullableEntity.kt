package com.megaman.maverick.game.entities.contracts

import com.badlogic.gdx.math.Rectangle
import com.engine.cullables.CullableOnUncontained
import com.engine.entities.contracts.IBodyEntity
import com.engine.entities.contracts.ICullableEntity
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.utils.toGameRectangle

/** An [IBodyEntity] that can be culled by the game camera. */
interface IGameCameraCullableEntity : IBodyEntity, ICullableEntity {

  /** Get the logic to cull this entity by the game camera. */
  fun getGameCameraCulling() =
      CullableOnUncontained(
          { (game as MegamanMaverickGame).getGameCamera().toGameRectangle() },
          { it.overlaps(body as Rectangle) })
}
