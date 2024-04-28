package com.megaman.maverick.game.entities.factories

import com.badlogic.gdx.utils.ObjectMap
import com.engine.common.interfaces.IClearable
import com.engine.common.interfaces.Initializable
import com.engine.common.objects.Pool
import com.engine.entities.IGameEntity
import com.engine.factories.IFactory

abstract class EntityFactory : IFactory<IGameEntity>, Initializable, IClearable {

    protected val pools = ObjectMap<Any, Pool<IGameEntity>>()

    override fun clear() = pools.clear()

}