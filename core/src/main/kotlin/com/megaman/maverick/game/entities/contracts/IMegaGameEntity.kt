package com.megaman.maverick.game.entities.contracts

import com.maverick.game.engine.common.interfaces.ITaggable
import com.maverick.game.engine.common.interfaces.ITypable
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.utils.extensions.getRotatedBounds
import com.megaman.maverick.game.world.body.getBounds

interface IMegaGameEntity : IGameEntity, ITaggable, ITypable<EntityType>

fun <T> T.overlapsGameCamera() where T : MegaGameEntity, T : IBodyEntity =
    services.gameCamera.getRotatedBounds().overlaps(body.getBounds())
