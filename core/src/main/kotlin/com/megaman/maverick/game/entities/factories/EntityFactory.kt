package com.megaman.maverick.game.entities.factories

import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.common.interfaces.IClearable
import com.maverick.game.engine.common.interfaces.Initializable
import com.maverick.game.engine.common.objects.Pool
import com.maverick.game.engine.factories.IFactory
import com.megaman.maverick.game.entities.contracts.MegaGameEntity

abstract class EntityFactory : IFactory<Any?, MegaGameEntity>, Initializable, IClearable {

    protected val pools = ObjectMap<Any, Pool<MegaGameEntity>>()

    override fun clear() = pools.clear()
}
