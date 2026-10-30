package com.megaman.maverick.game.entities.factories.impl

import com.maverick.game.engine.common.GameLogger
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.explosions.*
import com.megaman.maverick.game.entities.factories.EntityFactory
import com.megaman.maverick.game.entities.factories.GameEntityPoolCreator

class ExplosionsFactory(private val services: MegaServices) : EntityFactory() {

    companion object {
        const val TAG = "ExplosionFactory"
        const val EXPLOSION = "Explosion"
        const val EXPLOSION_ORB = "ExplosionOrb"
        const val DISINTEGRATION = "Disintegration"
        const val SNOWBALL_EXPLOSION = "SnowballExplosion"
        const val CHARGED_SHOT_EXPLOSION = "ChargedShotExplosion"
        const val CAVE_ROCK_EXPLOSION = "CaveRockExplosion"
        const val SIGMA_RAT_ELECTRIC_BALL_EXPLOSION = "SigmaRatElectricBallExplosion"
        const val ICE_SHARD = "IceShard"
        const val TOXIC_GOOP_SPLASH = "ToxicGoopSplash"
        const val SMOKE_PUFF = "SmokePuff"
        const val ASTEROID_EXPLOSION = "AsteroidExplosion"
        const val GREEN_EXPLOSION = "GreenExplosion"
        const val EXPLOSION_FIELD = "ExplosionField"
        const val MAGMA_EXPLOSION = "MagmaExplosion"
        const val MAGMA_GOOP_EXPLOSION = "MagmaGoopExplosion"
    }

    override fun init(vararg params: Any) {
        pools.put(EXPLOSION, GameEntityPoolCreator.create { Explosion(services) })
        pools.put(SNOWBALL_EXPLOSION, GameEntityPoolCreator.create { SnowballExplosion(services) })
        pools.put(DISINTEGRATION, GameEntityPoolCreator.create { Disintegration(services) })
        pools.put(CHARGED_SHOT_EXPLOSION, GameEntityPoolCreator.create { ChargedShotExplosion(services) })
        pools.put(EXPLOSION_ORB, GameEntityPoolCreator.create { ExplosionOrb(services) })
        pools.put(CAVE_ROCK_EXPLOSION, GameEntityPoolCreator.create { CaveRockExplosion(services) })
        pools.put(
            SIGMA_RAT_ELECTRIC_BALL_EXPLOSION,
            GameEntityPoolCreator.create { SigmaRatElectricBallExplosion(services) })
        pools.put(ICE_SHARD, GameEntityPoolCreator.create { IceShard(services) })
        pools.put(TOXIC_GOOP_SPLASH, GameEntityPoolCreator.create { ToxicGoopSplash(services) })
        pools.put(SMOKE_PUFF, GameEntityPoolCreator.create { SmokePuff(services) })
        pools.put(ASTEROID_EXPLOSION, GameEntityPoolCreator.create { AsteroidExplosion(services) })
        pools.put(GREEN_EXPLOSION, GameEntityPoolCreator.create { SpreadExplosion(services) })
        pools.put(EXPLOSION_FIELD, GameEntityPoolCreator.create { ExplosionField(services) })
        pools.put(MAGMA_EXPLOSION, GameEntityPoolCreator.create { MagmaExplosion(services) })
        pools.put(MAGMA_GOOP_EXPLOSION, GameEntityPoolCreator.create { MagmaGoopExplosion(services) })
    }

    override fun fetch(key: Any?): MegaGameEntity? {
        GameLogger.debug(TAG, "Spawning Explosion: key = $key")
        return pools.get(if (key == "") EXPLOSION else key)?.fetch()
    }
}
