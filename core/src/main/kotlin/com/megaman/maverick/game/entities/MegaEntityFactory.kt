package com.megaman.maverick.game.entities

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.common.extensions.putIfAbsentAndGet
import com.mega.game.engine.common.interfaces.ArgsInitializable
import com.mega.game.engine.common.interfaces.Resettable
import com.mega.game.engine.common.objects.Pool
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.factories.GameEntityPoolCreator
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.cast
import kotlin.reflect.full.primaryConstructor

object MegaEntityFactory : ArgsInitializable<MegamanMaverickGame>, Resettable {

    private val constructors = ObjectMap<KClass<out MegaGameEntity>, KFunction<MegaGameEntity>>()
    private val pools = OrderedMap<KClass<out MegaGameEntity>, Pool<MegaGameEntity>>()

    private lateinit var game: MegamanMaverickGame
    private var initialized = false

    override fun init(game: MegamanMaverickGame) {
        this.game = game
        initialized = true
    }

    fun <K : MegaGameEntity> fetch(key: String, castClass: KClass<K>): K? {
        val clazz = Class.forName(key).kotlin as KClass<MegaGameEntity>
        return castClass.cast(fetch(clazz))
    }

    fun <K : MegaGameEntity> fetch(key: KClass<K>): K? {
        if (!initialized) throw IllegalStateException("Entity factory not initialized")

        if (!pools.containsKey(key)) {
            val constructor = constructors.putIfAbsentAndGet(key) { key.primaryConstructor!! }

            val pool = GameEntityPoolCreator.create create@{
                return@create constructor.call(game)
            }

            pools.put(key, pool)
        }

        return key.cast(pools[key]?.fetch())
    }

    fun <K : MegaGameEntity> fetch(amount: Int, out: Array<K>, key: KClass<K>): Array<K> {
        (0 until amount).forEach {
            val entity = fetch(key)!!
            out.add(entity)
        }
        return out
    }

    override fun reset() = pools.clear()
}
