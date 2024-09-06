package com.megaman.maverick.game.entities.factories

import com.mega.game.engine.world.body.*;
import com.mega.game.engine.world.collisions.*;
import com.mega.game.engine.world.contacts.*;
import com.mega.game.engine.world.pathfinding.*;

import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.interfaces.IClearable
import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.common.objects.Pool
import com.mega.game.engine.factories.IFactory
import com.megaman.maverick.game.entities.contracts.MegaGameEntity

abstract class EntityFactory : IFactory<MegaGameEntity>, Initializable, IClearable {

    protected val pools = ObjectMap<Any, Pool<MegaGameEntity>>()

    override fun clear() = pools.clear()
}