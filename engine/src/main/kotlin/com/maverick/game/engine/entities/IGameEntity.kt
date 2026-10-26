package com.maverick.game.engine.entities

import com.badlogic.gdx.utils.OrderedMap
import com.maverick.game.engine.common.interfaces.IPropertizable
import com.maverick.game.engine.common.interfaces.Initializable
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.components.IComponentBucket
import com.maverick.game.engine.components.IGameComponent
import kotlin.reflect.KClass

interface IGameEntity : IComponentBucket, IPropertizable, Initializable {

    val components: OrderedMap<KClass<out IGameComponent>, IGameComponent>
    var initialized: Boolean
    val existenceState: EntityExistenceState

    val spawned: Boolean
        get() = existenceState == EntityExistenceState.LIVE || existenceState == EntityExistenceState.DESTROY_QUEUED

    fun canSpawn(spawnProps: Properties): Boolean

    fun onSpawn(spawnProps: Properties)

    fun onDestroy()
}