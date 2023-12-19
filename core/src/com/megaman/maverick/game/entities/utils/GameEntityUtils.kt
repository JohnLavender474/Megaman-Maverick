package com.megaman.maverick.game.entities.utils

import com.badlogic.gdx.math.Rectangle
import com.engine.cullables.CullableOnUncontained
import com.engine.entities.contracts.IBodyEntity
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.utils.toGameRectangle

/** Get the logic to cull this entity by the game camera. */
fun getGameCameraCullingLogic(entity: IBodyEntity, timeToCull: Float = 1f) =
    CullableOnUncontained(
        { (entity.game as MegamanMaverickGame).getGameCamera().toGameRectangle() },
        { it.overlaps(entity.body as Rectangle) },
        timeToCull)
