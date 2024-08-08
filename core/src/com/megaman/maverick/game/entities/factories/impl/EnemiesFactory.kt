package com.megaman.maverick.game.entities.factories.impl

import com.engine.common.GameLogger
import com.engine.entities.IGameEntity
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.enemies.*
import com.megaman.maverick.game.entities.factories.EntityFactory
import com.megaman.maverick.game.entities.factories.EntityPoolCreator

class EnemiesFactory(private val game: MegamanMaverickGame) : EntityFactory() {

    companion object {
        const val TAG = "EnemiesFactory"
        const val TEST_ENEMY = "TestEnemy"
        const val MET = "Met"
        const val BAT = "Bat"
        const val RATTON = "Ratton"
        const val MAG_FLY = "MagFly"
        const val FLY_BOY = "FlyBoy"
        const val PENGUIN = "Penguin"
        const val SCREWIE = "Screwie"
        const val PICKET_JOE = "PicketJoe"
        const val SNIPER_JOE = "SniperJoe"
        const val CARTIN_JOE = "CartinJoe"
        const val DRAGON_FLY = "DragonFly"
        const val MATASABURO = "Matasaburo"
        const val SPRING_HEAD = "SpringHead"
        const val SWINGIN_JOE = "SwinginJoe"
        const val GAPING_FISH = "GapingFish"
        const val FLOATING_CAN = "FloatingCan"
        const val FLOATING_CAN_HOLE = "FloatingCanHole"
        const val SUCTION_ROLLER = "SuctionRoller"
        const val SHIELD_ATTACKER = "ShieldAttacker"
        const val HANABIRAN = "Hanabiran"
        const val ELECN = "Elecn"
        const val ROBBIT = "Robbit"
        const val CAVE_ROCKER = "CaveRocker"
        const val EYEE = "Eyee"
        const val ADAMSKI = "Adamski"
        const val BIG_JUMPING_JOE = "BigJumpingJoe"
        const val UP_N_DOWN = "Up_N_Down"
        const val SUICIDE_BUMMER = "SuicideBummer"
        const val GACHAPPAN = "Gachappan"
        const val BULB_BLASTER = "BulbBlaster"
        const val IMORM = "Imorm"
        const val WANAAN = "Wanaan"
        const val PEAT = "Peat"
        const val BABY_SPIDER = "BabySpider"
        const val HELI_MET = "HeliMet"
        const val SHOT_MAN = "Shotman"
        const val PETIT_DEVIL = "PetitDevil"
        const val PETIT_DEVIL_CHILD = "PetitDevilChild"
        const val SNOWHEAD_THROWER = "SnowheadThrower"
        const val SPIKY = "Spiky"
        const val BABY_PENGUIN = "BabyPenguin"
        const val UFO_BOMB_BOT = "UFOBombBot"
        const val ROLLING_BOT = "RollingBot"
        const val TOXIC_BARREL_BOT = "ToxicBarrelBot"
        const val BOUNCING_ANGRY_FLAME_BALL = "BouncingAngryFlameBall"
        const val POPOHELI = "Popoheli"
        const val POPUP_CANON = "PopupCanon"
        const val JET_MET = "JetMet"
        const val BUNBY_TANK = "BunbyTank"
    }

    override fun init() {
        pools.put(TEST_ENEMY, EntityPoolCreator.create { TestEnemy(game) })
        pools.put(BAT, EntityPoolCreator.create { Bat(game) })
        pools.put(MET, EntityPoolCreator.create { Met(game) })
        pools.put(FLOATING_CAN, EntityPoolCreator.create { FloatingCan(game) })
        pools.put(FLOATING_CAN_HOLE, EntityPoolCreator.create { FloatingCanHole(game) })
        pools.put(DRAGON_FLY, EntityPoolCreator.create { DragonFly(game) })
        pools.put(FLY_BOY, EntityPoolCreator.create { FlyBoy(game) })
        pools.put(GAPING_FISH, EntityPoolCreator.create { GapingFish(game) })
        pools.put(SPRING_HEAD, EntityPoolCreator.create { SpringHead(game) })
        pools.put(SUCTION_ROLLER, EntityPoolCreator.create { SuctionRoller(game) })
        pools.put(MAG_FLY, EntityPoolCreator.create { MagFly(game) })
        pools.put(MATASABURO, EntityPoolCreator.create { Matasaburo(game) })
        pools.put(SWINGIN_JOE, EntityPoolCreator.create { SwinginJoe(game) })
        pools.put(SNIPER_JOE, EntityPoolCreator.create { SniperJoe(game) })
        pools.put(CARTIN_JOE, EntityPoolCreator.create { CartinJoe(game) })
        pools.put(PENGUIN, EntityPoolCreator.create { Penguin(game) })
        pools.put(SHIELD_ATTACKER, EntityPoolCreator.create { ShieldAttacker(game) })
        pools.put(SCREWIE, EntityPoolCreator.create { Screwie(game) })
        pools.put(HANABIRAN, EntityPoolCreator.create { Hanabiran(game) })
        pools.put(RATTON, EntityPoolCreator.create { Ratton(game) })
        pools.put(PICKET_JOE, EntityPoolCreator.create { PicketJoe(game) })
        pools.put(ELECN, EntityPoolCreator.create { Elecn(game) })
        pools.put(ROBBIT, EntityPoolCreator.create { Robbit(game) })
        pools.put(CAVE_ROCKER, EntityPoolCreator.create { CaveRocker(game) })
        pools.put(EYEE, EntityPoolCreator.create { Eyee(game) })
        pools.put(ADAMSKI, EntityPoolCreator.create { Adamski(game) })
        pools.put(BIG_JUMPING_JOE, EntityPoolCreator.create { BigJumpingJoe(game) })
        pools.put(UP_N_DOWN, EntityPoolCreator.create { UpNDown(game) })
        pools.put(SUICIDE_BUMMER, EntityPoolCreator.create { SuicideBummer(game) })
        pools.put(GACHAPPAN, EntityPoolCreator.create { Gachappan(game) })
        pools.put(BULB_BLASTER, EntityPoolCreator.create { BulbBlaster(game) })
        pools.put(IMORM, EntityPoolCreator.create { Imorm(game) })
        pools.put(WANAAN, EntityPoolCreator.create { Wanaan(game) })
        pools.put(PEAT, EntityPoolCreator.create { Peat(game) })
        pools.put(BABY_SPIDER, EntityPoolCreator.create { BabySpider(game) })
        pools.put(HELI_MET, EntityPoolCreator.create { HeliMet(game) })
        pools.put(SHOT_MAN, EntityPoolCreator.create { Shotman(game) })
        pools.put(PETIT_DEVIL, EntityPoolCreator.create { PetitDevil(game) })
        pools.put(PETIT_DEVIL_CHILD, EntityPoolCreator.create { PetitDevilChild(game) })
        pools.put(SNOWHEAD_THROWER, EntityPoolCreator.create { SnowheadThrower(game) })
        pools.put(SPIKY, EntityPoolCreator.create { Spiky(game) })
        pools.put(BABY_PENGUIN, EntityPoolCreator.create { BabyPenguin(game) })
        pools.put(UFO_BOMB_BOT, EntityPoolCreator.create { UFOBombBot(game) })
        pools.put(ROLLING_BOT, EntityPoolCreator.create { RollingBot(game) })
        pools.put(TOXIC_BARREL_BOT, EntityPoolCreator.create { ToxicBarrelBot(game) })
        pools.put(BOUNCING_ANGRY_FLAME_BALL, EntityPoolCreator.create { BouncingAngryFlameBall(game) })
        pools.put(POPOHELI, EntityPoolCreator.create { Popoheli(game) })
        pools.put(POPUP_CANON, EntityPoolCreator.create { PopupCanon(game) })
        pools.put(JET_MET, EntityPoolCreator.create { JetMet(game) })
        pools.put(BUNBY_TANK, EntityPoolCreator.create { BunbyTank(game) })
    }

    override fun fetch(key: Any): IGameEntity? {
        GameLogger.debug(TAG, "Spawning Enemy: key = $key")
        return pools.get(key)?.fetch()
    }
}
