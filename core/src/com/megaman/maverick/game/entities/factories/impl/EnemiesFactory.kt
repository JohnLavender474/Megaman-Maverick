package com.megaman.maverick.game.entities.factories.impl

import com.badlogic.gdx.utils.ObjectMap
import com.engine.common.GameLogger
import com.engine.common.objects.Pool
import com.engine.entities.IGameEntity
import com.engine.factories.IFactory
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.enemies.*
import com.megaman.maverick.game.entities.factories.EntityPoolCreator

class EnemiesFactory(game: MegamanMaverickGame) : IFactory<IGameEntity> {

    companion object {
        const val TAG = "EnemiesFactory"

        const val MET = "Met"
        const val BAT = "Bat"
        const val RATTON = "Ratton"
        const val MAG_FLY = "MagFly"
        const val FLY_BOY = "FlyBoy"
        const val PENGUIN = "Penguin"
        const val SCREWIE = "Screwie"
        const val PICKET_JOE = "PicketJoe"
        const val SNIPER_JOE = "SniperJoe"
        const val PRECIOUS_JOE = "PreciousJoe"
        const val CARTIN_JOE = "CartinJoe"
        const val DRAGON_FLY = "DragonFly"
        const val MATASABURO = "Matasaburo"
        const val SPRING_HEAD = "SpringHead"
        const val SWINGIN_JOE = "SwinginJoe"
        const val GAPING_FISH = "GapingFish"
        const val FLOATING_CAN = "FloatingCan"
        const val SUCTION_ROLLER = "SuctionRoller"
        const val SHIELD_ATTACKER = "ShieldAttacker"
        const val HANABIRAN = "Hanabiran"
        const val ELECN = "Elecn"
        const val ROBBIT = "Robbit"
        const val CAVE_ROCKER = "CaveRocker"
        const val EYEE = "Eyee"
        const val TOGGLEE = "Togglee"
        const val ADAMSKI = "Adamski"
        const val BIG_JUMPING_JOE = "BigJumpingJoe"
        const val UP_N_DOWN = "Up_N_Down"
        const val SUICIDE_BUMMER = "SuicideBummer"
        const val GACHAPPAN = "Gachappan"
        const val BULB_BLASTER = "BulbBlaster"
        const val IMORM = "Imorm"
        const val WANAAN = "Wanaan"
    }

    private val pools = ObjectMap<Any, Pool<AbstractEnemy>>()

    init {
        pools.put(BAT, EntityPoolCreator.create(3) { Bat(game) })
        pools.put(MET, EntityPoolCreator.create(3) { Met(game) })
        pools.put(FLOATING_CAN, EntityPoolCreator.create(5) { FloatingCan(game) })
        pools.put(DRAGON_FLY, EntityPoolCreator.create(2) { DragonFly(game) })
        pools.put(FLY_BOY, EntityPoolCreator.create(2) { FlyBoy(game) })
        pools.put(GAPING_FISH, EntityPoolCreator.create(3) { GapingFish(game) })
        pools.put(SPRING_HEAD, EntityPoolCreator.create(2) { SpringHead(game) })
        pools.put(SUCTION_ROLLER, EntityPoolCreator.create(2) { SuctionRoller(game) })
        pools.put(MAG_FLY, EntityPoolCreator.create(2) { MagFly(game) })
        pools.put(MATASABURO, EntityPoolCreator.create(2) { Matasaburo(game) })
        pools.put(SWINGIN_JOE, EntityPoolCreator.create(2) { SwinginJoe(game) })
        pools.put(SNIPER_JOE, EntityPoolCreator.create(2) { SniperJoe(game) })
        pools.put(CARTIN_JOE, EntityPoolCreator.create(2) { CartinJoe(game) })
        pools.put(PENGUIN, EntityPoolCreator.create(2) { Penguin(game) })
        pools.put(SHIELD_ATTACKER, EntityPoolCreator.create(2) { ShieldAttacker(game) })
        pools.put(SCREWIE, EntityPoolCreator.create(5) { Screwie(game) })
        pools.put(HANABIRAN, EntityPoolCreator.create(2) { Hanabiran(game) })
        pools.put(RATTON, EntityPoolCreator.create(2) { Ratton(game) })
        pools.put(PICKET_JOE, EntityPoolCreator.create(2) { PicketJoe(game) })
        pools.put(ELECN, EntityPoolCreator.create(2) { Elecn(game) })
        pools.put(ROBBIT, EntityPoolCreator.create(2) { Robbit(game) })
        pools.put(CAVE_ROCKER, EntityPoolCreator.create(2) { CaveRocker(game) })
        pools.put(EYEE, EntityPoolCreator.create(3) { Eyee(game) })
        pools.put(TOGGLEE, EntityPoolCreator.create(2) { Togglee(game) })
        pools.put(ADAMSKI, EntityPoolCreator.create(6) { Adamski(game) })
        pools.put(BIG_JUMPING_JOE, EntityPoolCreator.create(2) { BigJumpingJoe(game) })
        pools.put(UP_N_DOWN, EntityPoolCreator.create(3) { UpNDown(game) })
        pools.put(SUICIDE_BUMMER, EntityPoolCreator.create(3) { SuicideBummer(game) })
        pools.put(GACHAPPAN, EntityPoolCreator.create(2) { Gachappan(game) })
        pools.put(BULB_BLASTER, EntityPoolCreator.create(1) { BulbBlaster(game) })
        pools.put(IMORM, EntityPoolCreator.create(2) { Imorm(game) })
        pools.put(WANAAN, EntityPoolCreator.create(2) { Wanaan(game) })
    }

    override fun fetch(key: Any): AbstractEnemy? {
        GameLogger.debug(TAG, "Spawning Enemy: key = $key")
        return pools.get(key)?.fetch()
    }
}
