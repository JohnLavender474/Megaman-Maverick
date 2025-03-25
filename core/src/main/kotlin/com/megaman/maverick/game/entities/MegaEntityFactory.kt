package com.megaman.maverick.game.entities

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.common.extensions.orderedMapOf
import com.mega.game.engine.common.extensions.putIfAbsentAndGet
import com.mega.game.engine.common.interfaces.ArgsInitializable
import com.mega.game.engine.common.interfaces.Resettable
import com.mega.game.engine.common.objects.Pool
import com.mega.game.engine.common.objects.pairTo
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.factories.GameEntityPoolCreator
import com.megaman.maverick.game.entities.projectiles.Bullet
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.cast
import kotlin.reflect.full.primaryConstructor

object MegaEntityFactory : ArgsInitializable<MegamanMaverickGame>, Resettable {

    private val constructors = ObjectMap<KClass<out MegaGameEntity>, KFunction<MegaGameEntity>>()
    private val pools = OrderedMap<KClass<out MegaGameEntity>, Pool<MegaGameEntity>>()

    private lateinit var game: MegamanMaverickGame
    private var initialized = false

    private val ENTITIES_TO_PRELOAD = orderedMapOf<KClass<out MegaGameEntity>, Int>(
        Bullet::class pairTo 25, Block::class pairTo 25
    )

    override fun init(game: MegamanMaverickGame) {
        this.game = game
        initialized = true
        ENTITIES_TO_PRELOAD.forEach { entry ->
            val key = entry.key
            val amount = entry.value
            (0 until amount).forEach {
                val entity = fetch(key)!!
                entity.init()
                free(entity)
            }
        }
    }

    fun <K : MegaGameEntity> fetch(key: String, castClass: KClass<K>): K? {
        val clazz = Class.forName(key).kotlin as KClass<MegaGameEntity>
        return castClass.cast(fetch(clazz))
    }

    fun <K : MegaGameEntity> fetch(key: KClass<K>): K? {
        if (!initialized) throw IllegalStateException("Mega entity factory not initialized")
        val pool = putPoolIfAbsentAndGet(key)
        return key.cast(pool.fetch())
    }

    fun free(entity: MegaGameEntity) {
        val key = entity::class
        val pool = putPoolIfAbsentAndGet(key)
        pool.free(entity)
    }

    private fun <K: MegaGameEntity> putPoolIfAbsentAndGet(key: KClass<K>): Pool<MegaGameEntity> {
        if (!pools.containsKey(key)) {
            val constructor = constructors.putIfAbsentAndGet(key) { key.primaryConstructor!! }
            val pool = GameEntityPoolCreator.create create@{
                return@create constructor.call(game)
            }
            pools.put(key, pool)
        }
        return pools[key]
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
