package com.megaman.maverick.game.entities.factories.impl

import com.mega.game.engine.world.body.*;
import com.mega.game.engine.world.collisions.*;
import com.mega.game.engine.world.contacts.*;
import com.mega.game.engine.world.pathfinding.*;

import com.mega.game.engine.common.GameLogger
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.explosions.*
import com.megaman.maverick.game.entities.factories.EntityFactory
import com.megaman.maverick.game.entities.factories.GameEntityPoolCreator

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
        const val ASTEROID_EXPLOSION = "AsteroidExplosion"
        const val GREEN_EXPLOSION = "GreenExplosion"
    }

    override fun init() {
        pools.put(EXPLOSION, GameEntityPoolCreator.create { Explosion(game) })
        pools.put(SNOWBALL_EXPLOSION, GameEntityPoolCreator.create { SnowballExplosion(game) })
        pools.put(DISINTEGRATION, GameEntityPoolCreator.create { Disintegration(game) })
        pools.put(CHARGED_SHOT_EXPLOSION, GameEntityPoolCreator.create { ChargedShotExplosion(game) })
        pools.put(EXPLOSION_ORB, GameEntityPoolCreator.create { ExplosionOrb(game) })
        pools.put(CAVE_ROCK_EXPLOSION, GameEntityPoolCreator.create { CaveRockExplosion(game) })
        pools.put(
            SIGMA_RAT_ELECTRIC_BALL_EXPLOSION,
            GameEntityPoolCreator.create { SigmaRatElectricBallExplosion(game) })
        pools.put(ICE_SHARD, GameEntityPoolCreator.create { IceShard(game) })
        pools.put(TOXIC_GOOP_SPLASH, GameEntityPoolCreator.create { ToxicGoopSplash(game) })
        pools.put(SMOKE_PUFF, GameEntityPoolCreator.create { SmokePuff(game) })
        pools.put(ASTEROID_EXPLOSION, GameEntityPoolCreator.create { AsteroidExplosion(game) })
        pools.put(GREEN_EXPLOSION, GameEntityPoolCreator.create { GreenExplosion(game) })
    }

    override fun fetch(key: Any): MegaGameEntity? {
        GameLogger.debug(TAG, "Spawning Explosion: key = $key")
        return pools.get(if (key == "") EXPLOSION else key)?.fetch()
    }
}