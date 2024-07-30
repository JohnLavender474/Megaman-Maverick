package com.megaman.maverick.game.entities.factories.impl

import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.factories.EntityFactory
import com.megaman.maverick.game.entities.factories.EntityPoolCreator
import com.megaman.maverick.game.entities.projectiles.*

class ProjectilesFactory(private val game: MegamanMaverickGame) : EntityFactory() {

    companion object {
        const val TAG = "ProjectileFactory"
        const val BULLET = "Bullet"
        const val PICKET = "Picket"
        const val JOEBALL = "Joeball"
        const val FIREBALL = "Fireball"
        const val SNOWBALL = "Snowball"
        const val CHARGED_SHOT = "ChargedShot"
        const val PURPLE_BLAST = "PurpleBlast"
        const val PRECIOUS_SHOT = "PreciousShot"
        const val PETAL = "Petal"
        const val ELECTRIC_BALL = "ElectricBall"
        const val CAVE_ROCK = "CaveRock"
        const val SNIPER_JOE_SHIELD = "SniperJoeShield"
        const val EXPLODING_BALL = "ExplodingBall"
        const val SPIDER_WEB = "SpiderWeb"
        const val SIGMA_RAT_ELECTRIC_BALL = "SigmaRatElectricBall"
        const val BOULDER_PROJECTILE = "BoulderProjectile"
        const val SNOW_HEAD = "SnowHead"
        const val UFO_BOMB = "UFOBomb"
        const val ROLLING_BOT_SHOT = "RollingBotShot"
        const val TOXIC_GOOP_SHOT = "ToxicGoopShot"
        const val REACTOR_MONKEY_BALL = "ReactorMonkeyBall"
        const val TUBE_BEAM = "TubeBeam"
        const val REACT_MAN_PROJECTILE = "ReactManProjectile"
        const val ASTEROID = "Asteroid"
    }

    override fun init() {
        pools.put(BULLET, EntityPoolCreator.create { Bullet(game) })
        pools.put(CHARGED_SHOT, EntityPoolCreator.create { ChargedShot(game) })
        pools.put(PURPLE_BLAST, EntityPoolCreator.create { PurpleBlast(game) })
        pools.put(FIREBALL, EntityPoolCreator.create { Fireball(game) })
        pools.put(JOEBALL, EntityPoolCreator.create { JoeBall(game) })
        pools.put(PETAL, EntityPoolCreator.create { Petal(game) })
        pools.put(SNOWBALL, EntityPoolCreator.create { Snowball(game) })
        pools.put(PICKET, EntityPoolCreator.create { Picket(game) })
        pools.put(ELECTRIC_BALL, EntityPoolCreator.create { ElectricBall(game) })
        pools.put(CAVE_ROCK, EntityPoolCreator.create { CaveRock(game) })
        pools.put(SNIPER_JOE_SHIELD, EntityPoolCreator.create { SniperJoeShield(game) })
        pools.put(EXPLODING_BALL, EntityPoolCreator.create { ExplodingBall(game) })
        pools.put(SPIDER_WEB, EntityPoolCreator.create { SpiderWeb(game) })
        pools.put(SIGMA_RAT_ELECTRIC_BALL, EntityPoolCreator.create { SigmaRatElectricBall(game) })
        pools.put(BOULDER_PROJECTILE, EntityPoolCreator.create { BoulderProjectile(game) })
        pools.put(SNOW_HEAD, EntityPoolCreator.create { Snowhead(game) })
        pools.put(UFO_BOMB, EntityPoolCreator.create { UFOBomb(game) })
        pools.put(ROLLING_BOT_SHOT, EntityPoolCreator.create { RollingBotShot(game) })
        pools.put(TOXIC_GOOP_SHOT, EntityPoolCreator.create { ToxicGoopShot(game) })
        pools.put(REACTOR_MONKEY_BALL, EntityPoolCreator.create { ReactorMonkeyBall(game) })
        pools.put(TUBE_BEAM, EntityPoolCreator.create { TubeBeam(game) })
        pools.put(REACT_MAN_PROJECTILE, EntityPoolCreator.create { ReactManProjectile(game) })
        pools.put(ASTEROID, EntityPoolCreator.create { Asteroid(game) })
    }

    override fun fetch(key: Any) = pools.get(if (key == "") BULLET else key)?.fetch()
}
