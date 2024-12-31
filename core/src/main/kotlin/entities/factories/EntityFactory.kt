package com.megaman.maverick.game.entities.factories

import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.interfaces.IClearable
import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.common.objects.Pool
import com.mega.game.engine.factories.IFactory
import com.megaman.maverick.game.entities.contracts.MegaGameEntity

abstract class EntityFactory : IFactory<Any?, MegaGameEntity>, Initializable, IClearable {

    protected val pools = ObjectMap<Any, Pool<MegaGameEntity>>()

    override fun clear() = pools.clear()
}
