package com.megaman.maverick.game.entities.contracts

import com.badlogic.gdx.math.Rectangle
import com.engine.common.shapes.GameRectangle
import com.engine.cullables.CullableOnUncontained
import com.engine.entities.contracts.IBodyEntity
import com.engine.entities.contracts.ICullableEntity
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.utils.toGameRectangle

/** An [IBodyEntity] that can be culled by the game camera. */
interface IGameCameraCullableEntity : IBodyEntity, ICullableEntity {

  /** Adds the logic to cull this entity by the game camera. */
  fun addGameCameraCulling() {
    val cullable: CullableOnUncontained<GameRectangle> =
        CullableOnUncontained(
            { (game as MegamanMaverickGame).getGameCamera().toGameRectangle() },
            { it.overlaps(body as Rectangle) })
    getCullablesComponent().cullables.add(cullable)
  }
}
