package com.megaman.maverick.game.world.body

import com.maverick.game.engine.world.body.IFixtureType

enum class FixtureType : IFixtureType {
    BLOCK,
    PLAYER,
    BODY,
    FEET,
    HEAD,
    SIDE,
    ITEM,
    ICE,
    SAND,
    SNOW,
    GATE,
    CART,
    LASER,
    WATER,
    FORCE,
    LADDER,
    SHIELD,
    EXPLOSION,
    BOUNCER,
    CONSUMER,
    CUSTOM,
    DEATH,
    DAMAGER,
    DAMAGEABLE,
    PROJECTILE,
    WATER_LISTENER,
    GRAVITY_CHANGE,
    TELEPORTER,
    TELEPORTER_LISTENER;

    override val index get() = ordinal
}
