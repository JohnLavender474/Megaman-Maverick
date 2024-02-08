package com.megaman.maverick.game.entities.factories.impl

import com.badlogic.gdx.utils.ObjectMap
import com.engine.common.objects.Pool
import com.engine.entities.IGameEntity
import com.engine.factories.IFactory
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.factories.EntityPoolCreator
import com.megaman.maverick.game.entities.special.*

class SpecialsFactory(private val game: MegamanMaverickGame) : IFactory<IGameEntity> {

    companion object {
        const val WATER = "Water"
        const val LADDER = "Ladder"
        const val GRAVITY_CHANGE = "GravityChange"
        const val SPRING_BOUNCER = "SpringBouncer"
        const val DISAPPEARING_BLOCKS = "DisappearingBlocks"
        const val CART = "Cart"
        const val ROTATION_ANCHOR = "RotationAnchor"
        const val PORTAL_HOPPER = "PortalHopper"
    }

    private val pools = ObjectMap<Any, Pool<IGameEntity>>()

    init {
        pools.put(WATER, EntityPoolCreator.create(3) { Water(game) })
        pools.put(SPRING_BOUNCER, EntityPoolCreator.create(2) { SpringBouncer(game) })
        pools.put(LADDER, EntityPoolCreator.create(10) { Ladder(game) })
        pools.put(GRAVITY_CHANGE, EntityPoolCreator.create(10) { GravityChange(game) })
        pools.put(DISAPPEARING_BLOCKS, EntityPoolCreator.create(2) { DisappearingBlocks(game) })
        pools.put(CART, EntityPoolCreator.create(2) { Cart(game) })
        pools.put(ROTATION_ANCHOR, EntityPoolCreator.create(2) { RotationAnchor(game) })
        pools.put(PORTAL_HOPPER, EntityPoolCreator.create(5) { PortalHopper(game) })
    }

    override fun fetch(key: Any) = pools.get(key)?.fetch()
}
