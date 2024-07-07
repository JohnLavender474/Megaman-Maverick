package com.megaman.maverick.game.entities.factories.impl

import com.engine.common.GameLogger
import com.engine.entities.IGameEntity
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.explosions.*
import com.megaman.maverick.game.entities.factories.EntityFactory
import com.megaman.maverick.game.entities.factories.EntityPoolCreator

class ExplosionsFactory(private val game: MegamanMaverickGame) : EntityFactory() {

    companion object {
        const val TAG = "ExplosionFactory"

        const val EXPLOSION = "Explosion"
        const val EXPLOSION_ORB = "ExplosionOrb"
        const val DISINTEGRATION = "Disintegration"
        const val SNOWBALL_EXPLOSION = "SnowballExplosion"
        const val PRECIOUS_EXPLOSION = "PreciousExplosion"
        const val CHARGED_SHOT_EXPLOSION = "ChargedShotExplosion"
        const val CAVE_ROCK_EXPLOSION = "CaveRockExplosion"
        const val SIGMA_RAT_ELECTRIC_BALL_EXPLOSION = "SigmaRatElectricBallExplosion"
        const val ICE_SHARD = "IceShard"
        const val TOXIC_GOOP_SPLASH = "ToxicGoopSplash"
        const val SMOKE_PUFF = "SmokePuff"
    }

    override fun init() {
        pools.put(EXPLOSION, EntityPoolCreator.create { Explosion(game) })
        pools.put(SNOWBALL_EXPLOSION, EntityPoolCreator.create { SnowballExplosion(game) })
        pools.put(DISINTEGRATION, EntityPoolCreator.create { Disintegration(game) })
        pools.put(CHARGED_SHOT_EXPLOSION, EntityPoolCreator.create { ChargedShotExplosion(game) })
        pools.put(EXPLOSION_ORB, EntityPoolCreator.create { ExplosionOrb(game) })
        pools.put(CAVE_ROCK_EXPLOSION, EntityPoolCreator.create { CaveRockExplosion(game) })
        pools.put(
            SIGMA_RAT_ELECTRIC_BALL_EXPLOSION,
            EntityPoolCreator.create { SigmaRatElectricBallExplosion(game) })
        pools.put(ICE_SHARD, EntityPoolCreator.create { IceShard(game) })
        pools.put(TOXIC_GOOP_SPLASH, EntityPoolCreator.create { ToxicGoopSplash(game) })
        pools.put(SMOKE_PUFF, EntityPoolCreator.create { SmokePuff(game) })
    }

    override fun fetch(key: Any): IGameEntity? {
        GameLogger.debug(TAG, "Spawning Explosion: key = $key")
        return pools.get(if (key == "") EXPLOSION else key)?.fetch()
    }
}
