package com.megaman.maverick.game.entities.factories.impl

import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.factories.EntityFactory
import com.megaman.maverick.game.entities.factories.GameEntityPoolCreator
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
        const val ROCKET_BOMB = "RocketBomb"
        const val BUNBY_RED_ROCKET = "BunbyRedRocket"
        const val FIRE_MET_FLAME = "FireMetFlame"
        const val PIPI_EGG = "PipiEgg"
        const val SPIT_FIREBALL = "SpitFireball"
        const val SMALL_MISSILE = "SmallMissile"
        const val ARIGOCK_BALL = "ArigockBall"
        const val CACTUS_MISSILE = "CactusMissile"
        const val SPHINX_BALL = "SphinxBall"
        const val TEARDROP_BLAST = "TeardropBlast"
        const val NEEDLE = "Needle"
        const val FALLING_ICICLE = "FallingIcicle"
        const val SEALION_BALL = "SealionBall"
        const val FIRE_WALL = "FireWall"
        const val FIRE_PELLET = "FirePellet"
        const val MAGMA_ORB = "MagmaOrb"
        const val MAGMA_METEOR = "MagmaMeteor"
        const val MAGMA_GOOP = "MagmaGoop"
        const val MAGMA_WAVE = "MagmaWave"
        const val MAGMA_PELLET = "MagmaPellet"
        const val MOON_SCYTHE = "MoonScythe"
        const val SHARP_STAR = "SharpStar"
        const val NUTT = "Nutt"
        const val GROUND_PEBBLE = "GroundPebble"
        const val CACA_FLAME = "CacaFlame"
        const val SUPER_COOL_ACTION_STAR_WARS_SPACE_LAZER = "SuperCoolActionStarWarsSpaceLazer"
        const val AXE = "axe"
    }

    override fun init() {
        pools.put(BULLET, GameEntityPoolCreator.create { Bullet(game) })
        pools.put(CHARGED_SHOT, GameEntityPoolCreator.create { ChargedShot(game) })
        pools.put(PURPLE_BLAST, GameEntityPoolCreator.create { PurpleBlast(game) })
        pools.put(FIREBALL, GameEntityPoolCreator.create { Fireball(game) })
        pools.put(JOEBALL, GameEntityPoolCreator.create { JoeBall(game) })
        pools.put(PETAL, GameEntityPoolCreator.create { Petal(game) })
        pools.put(SNOWBALL, GameEntityPoolCreator.create { Snowball(game) })
        pools.put(PICKET, GameEntityPoolCreator.create { Picket(game) })
        pools.put(ELECTRIC_BALL, GameEntityPoolCreator.create { ElectricBall(game) })
        pools.put(CAVE_ROCK, GameEntityPoolCreator.create { CaveRock(game) })
        pools.put(SNIPER_JOE_SHIELD, GameEntityPoolCreator.create { SniperJoeShield(game) })
        pools.put(EXPLODING_BALL, GameEntityPoolCreator.create { ExplodingBall(game) })
        pools.put(SPIDER_WEB, GameEntityPoolCreator.create { SpiderWeb(game) })
        pools.put(SIGMA_RAT_ELECTRIC_BALL, GameEntityPoolCreator.create { SigmaRatElectricBall(game) })
        pools.put(BOULDER_PROJECTILE, GameEntityPoolCreator.create { BoulderProjectile(game) })
        pools.put(SNOW_HEAD, GameEntityPoolCreator.create { Snowhead(game) })
        pools.put(UFO_BOMB, GameEntityPoolCreator.create { UFOBomb(game) })
        pools.put(ROLLING_BOT_SHOT, GameEntityPoolCreator.create { RollingBotShot(game) })
        pools.put(TOXIC_GOOP_SHOT, GameEntityPoolCreator.create { ToxicGoopShot(game) })
        pools.put(REACTOR_MONKEY_BALL, GameEntityPoolCreator.create { ReactorMonkeyBall(game) })
        pools.put(TUBE_BEAM, GameEntityPoolCreator.create { TubeBeam(game) })
        pools.put(REACT_MAN_PROJECTILE, GameEntityPoolCreator.create { ReactorManProjectile(game) })
        pools.put(ASTEROID, GameEntityPoolCreator.create { Asteroid(game) })
        pools.put(ROCKET_BOMB, GameEntityPoolCreator.create { RocketBomb(game) })
        pools.put(BUNBY_RED_ROCKET, GameEntityPoolCreator.create { BunbyRedRocket(game) })
        pools.put(FIRE_MET_FLAME, GameEntityPoolCreator.create { FireMetFlame(game) })
        pools.put(PIPI_EGG, GameEntityPoolCreator.create { PipiEgg(game) })
        pools.put(SPIT_FIREBALL, GameEntityPoolCreator.create { SpitFireball(game) })
        pools.put(SMALL_MISSILE, GameEntityPoolCreator.create { SmallGreenMissile(game) })
        pools.put(ARIGOCK_BALL, GameEntityPoolCreator.create { ArigockBall(game) })
        pools.put(CACTUS_MISSILE, GameEntityPoolCreator.create { CactusMissile(game) })
        pools.put(SPHINX_BALL, GameEntityPoolCreator.create { SphinxBall(game) })
        pools.put(TEARDROP_BLAST, GameEntityPoolCreator.create { TeardropBlast(game) })
        pools.put(NEEDLE, GameEntityPoolCreator.create { Needle(game) })
        pools.put(FALLING_ICICLE, GameEntityPoolCreator.create { FallingIcicle(game) })
        pools.put(SEALION_BALL, GameEntityPoolCreator.create { SealionBall(game) })
        pools.put(FIRE_WALL, GameEntityPoolCreator.create { FireWall(game) })
        pools.put(FIRE_PELLET, GameEntityPoolCreator.create { FirePellet(game) })
        pools.put(MAGMA_ORB, GameEntityPoolCreator.create { MagmaOrb(game) })
        pools.put(MAGMA_METEOR, GameEntityPoolCreator.create { MagmaMeteor(game) })
        pools.put(MAGMA_GOOP, GameEntityPoolCreator.create { MagmaGoop(game) })
        pools.put(MAGMA_WAVE, GameEntityPoolCreator.create { MagmaWave(game) })
        pools.put(MAGMA_PELLET, GameEntityPoolCreator.create { MagmaPellet(game) })
        pools.put(MOON_SCYTHE, GameEntityPoolCreator.create { MoonScythe(game) })
        pools.put(SHARP_STAR, GameEntityPoolCreator.create { SharpStar(game) })
        pools.put(NUTT, GameEntityPoolCreator.create { Nutt(game) })
        pools.put(GROUND_PEBBLE, GameEntityPoolCreator.create { GroundPebble(game) })
        pools.put(CacaFlame, GameEntityPoolCreator.create { CacaFlame(game) })
        pools.put(
            SUPER_COOL_ACTION_STAR_WARS_SPACE_LAZER,
            GameEntityPoolCreator.create { SuperCoolActionStarWarsSpaceLazer(game) }
        )
        pools.put(AXE, GameEntityPoolCreator.create { Axe(game) })
    }

    override fun fetch(key: Any?) = pools.get(if (key == "") BULLET else key)?.fetch()
}
