package com.maverick.game.engine.entities

import com.badlogic.gdx.utils.OrderedMap
import com.maverick.game.engine.common.interfaces.IPropertizable
import com.maverick.game.engine.common.interfaces.Initializable
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.components.IComponentBucket
import com.maverick.game.engine.components.IGameComponent
import com.maverick.game.engine.entities.contracts.IDestroyable
import kotlin.reflect.KClass

interface IGameEntity : IComponentBucket, IPropertizable, Initializable, IDestroyable {

    val components: OrderedMap<KClass<out IGameComponent>, IGameComponent>
    var existenceState: EntityExistenceState
    var initialized: Boolean

    fun canSpawn(spawnProps: Properties): Boolean

    fun onSpawnQueued() {}

    fun onSpawn(spawnProps: Properties)

    fun onDestroyQueued() {}

    fun onDestroy()
}
