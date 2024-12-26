package com.mega.game.engine.entities

import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.common.interfaces.IPropertizable
import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.components.IComponentBucket
import com.mega.game.engine.components.IGameComponent
import kotlin.reflect.KClass

interface IGameEntity : IComponentBucket, IPropertizable, Initializable {

    val components: OrderedMap<KClass<out IGameComponent>, IGameComponent>
    var initialized: Boolean
    var spawned: Boolean

    fun canSpawn(spawnProps: Properties): Boolean

    fun onSpawn(spawnProps: Properties)

    fun onDestroy()
}