package com.megaman.maverick.game.entities.factories.impl

import com.badlogic.gdx.utils.ObjectMap
import com.engine.common.objects.Pool
import com.engine.entities.IGameEntity
import com.engine.factories.IFactory
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.contracts.IProjectileEntity
import com.megaman.maverick.game.entities.factories.EntityPoolCreator
import com.megaman.maverick.game.entities.projectiles.*

class ProjectilesFactory(game: MegamanMaverickGame) : IFactory<IGameEntity> {

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
        const val GACHAPPAN_BALL = "GachappanBall"
        const val SPIDER_WEB = "SpiderWeb"
    }

    private val pools = ObjectMap<Any, Pool<IProjectileEntity>>()

    init {
        pools.put(BULLET, EntityPoolCreator.create(100) { Bullet(game) })
        pools.put(CHARGED_SHOT, EntityPoolCreator.create(5) { ChargedShot(game) })
        pools.put(PURPLE_BLAST, EntityPoolCreator.create(5) { PurpleBlast(game) })
        pools.put(FIREBALL, EntityPoolCreator.create(3) { Fireball(game) })
        pools.put(JOEBALL, EntityPoolCreator.create(3) { JoeBall(game) })
        pools.put(PETAL, EntityPoolCreator.create(4) { Petal(game) })
        pools.put(SNOWBALL, EntityPoolCreator.create(3) { Snowball(game) })
        pools.put(PICKET, EntityPoolCreator.create(3) { Picket(game) })
        pools.put(ELECTRIC_BALL, EntityPoolCreator.create(8) { ElectricBall(game) })
        pools.put(CAVE_ROCK, EntityPoolCreator.create(3) { CaveRock(game) })
        pools.put(SNIPER_JOE_SHIELD, EntityPoolCreator.create(2) { SniperJoeShield(game) })
        pools.put(GACHAPPAN_BALL, EntityPoolCreator.create(3) { GachappanBall(game) })
        pools.put(SPIDER_WEB, EntityPoolCreator.create(1) { SpiderWeb(game) })
    }

    override fun fetch(key: Any) = pools.get(if (key == "") BULLET else key)?.fetch()
}
