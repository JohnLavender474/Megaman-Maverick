package com.megaman.maverick.game.entities.factories.impl

import com.mega.game.engine.common.GameLogger
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.enemies.*
import com.megaman.maverick.game.entities.factories.EntityFactory
import com.megaman.maverick.game.entities.factories.GameEntityPoolCreator

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
        const val ANGRY_FLAME_BALL = "AngryFlameBall"
        const val POPOHELI = "Popoheli"
        const val POPUP_CANON = "PopupCanon"
        const val JET_MET = "JetMet"
        const val BUNBY_TANK = "BunbyTank"
        const val FIRE_MET = "FireMet"
        const val FIRE_MET_SPAWNER = "FireMetSpawner"
        const val FLAME_HEAD_THROWER = "FlameHeadThrower"
        const val PIPI = "Pipi"
        const val COPIPI = "Copipi"
        const val BOMB_POTTON = "BombPotton"
        const val ARIGOCK = "Arigock"
        const val BOMB_CHUTE = "BombChute"
        const val TOTEM_POLEM = "TotemPolem"
        const val CARRI_CARRY = "CarriCarry"
        const val CACTUS_LAUNCHER = "CactusLauncher"
        const val COLTON_JOE = "ColtonJoe"
        const val TURN_BLASTER = "TurnBlaster"
        const val JETPACK_ICE_BLASTER = "JetpackIceBlaster"
        const val SPIKE_BOT = "SpikeBot"
        const val TROPISH = "Tropish"
        const val SEALION = "Sealion"
        const val YELLOW_TIGGER_SQUIRT = "YellowTiggerSquirt"
        const val UNDERWATER_PENGUIN_BOT = "UnderwaterPenguinBot"
        const val UF_OH_NO_BOT = "UFOhNoBot"
        const val TANK_BOT = "TankBot"
        const val WALRUS_BOT = "WalrusBot"
        const val DARSPIDER = "Darspider"
        const val BIG_FISH_NEO = "BigFishNeo"
        const val FIRE_DISPENSENATOR = "FireDispensenator"
        const val DEMON_MET = "DemonMet"
        const val RAT_ROBOT = "RatRobot"
    }

    override fun init() {
        pools.put(TEST_ENEMY, GameEntityPoolCreator.create { TestEnemy(game) })
        pools.put(BAT, GameEntityPoolCreator.create { Bat(game) })
        pools.put(MET, GameEntityPoolCreator.create { Met(game) })
        pools.put(FLOATING_CAN, GameEntityPoolCreator.create { FloatingCan(game) })
        pools.put(FLOATING_CAN_HOLE, GameEntityPoolCreator.create { FloatingCanHole(game) })
        pools.put(DRAGON_FLY, GameEntityPoolCreator.create { DragonFly(game) })
        pools.put(FLY_BOY, GameEntityPoolCreator.create { FlyBoy(game) })
        pools.put(GAPING_FISH, GameEntityPoolCreator.create { GapingFish(game) })
        pools.put(SPRING_HEAD, GameEntityPoolCreator.create { SpringHead(game) })
        pools.put(SUCTION_ROLLER, GameEntityPoolCreator.create { SuctionRoller(game) })
        pools.put(MAG_FLY, GameEntityPoolCreator.create { MagFly(game) })
        pools.put(MATASABURO, GameEntityPoolCreator.create { Matasaburo(game) })
        pools.put(SWINGIN_JOE, GameEntityPoolCreator.create { SwinginJoe(game) })
        pools.put(SNIPER_JOE, GameEntityPoolCreator.create { SniperJoe(game) })
        pools.put(CARTIN_JOE, GameEntityPoolCreator.create { CartinJoe(game) })
        pools.put(PENGUIN, GameEntityPoolCreator.create { Penguin(game) })
        pools.put(SHIELD_ATTACKER, GameEntityPoolCreator.create { ShieldAttacker(game) })
        pools.put(SCREWIE, GameEntityPoolCreator.create { Screwie(game) })
        pools.put(HANABIRAN, GameEntityPoolCreator.create { Hanabiran(game) })
        pools.put(RATTON, GameEntityPoolCreator.create { Ratton(game) })
        pools.put(PICKET_JOE, GameEntityPoolCreator.create { PicketJoe(game) })
        pools.put(ELECN, GameEntityPoolCreator.create { Elecn(game) })
        pools.put(ROBBIT, GameEntityPoolCreator.create { Robbit(game) })
        pools.put(CAVE_ROCKER, GameEntityPoolCreator.create { CaveRocker(game) })
        pools.put(EYEE, GameEntityPoolCreator.create { Eyee(game) })
        pools.put(ADAMSKI, GameEntityPoolCreator.create { Adamski(game) })
        pools.put(BIG_JUMPING_JOE, GameEntityPoolCreator.create { BigJumpingJoe(game) })
        pools.put(UP_N_DOWN, GameEntityPoolCreator.create { UpNDown(game) })
        pools.put(SUICIDE_BUMMER, GameEntityPoolCreator.create { SuicideBummer(game) })
        pools.put(GACHAPPAN, GameEntityPoolCreator.create { Gachappan(game) })
        pools.put(BULB_BLASTER, GameEntityPoolCreator.create { BulbBlaster(game) })
        pools.put(IMORM, GameEntityPoolCreator.create { Imorm(game) })
        pools.put(WANAAN, GameEntityPoolCreator.create { Wanaan(game) })
        pools.put(PEAT, GameEntityPoolCreator.create { Peat(game) })
        pools.put(BABY_SPIDER, GameEntityPoolCreator.create { BabySpider(game) })
        pools.put(HELI_MET, GameEntityPoolCreator.create { HeliMet(game) })
        pools.put(SHOT_MAN, GameEntityPoolCreator.create { Shotman(game) })
        pools.put(PETIT_DEVIL, GameEntityPoolCreator.create { PetitDevil(game) })
        pools.put(PETIT_DEVIL_CHILD, GameEntityPoolCreator.create { PetitDevilChild(game) })
        pools.put(SNOWHEAD_THROWER, GameEntityPoolCreator.create { SnowheadThrower(game) })
        pools.put(SPIKY, GameEntityPoolCreator.create { Spiky(game) })
        pools.put(BABY_PENGUIN, GameEntityPoolCreator.create { BabyPenguin(game) })
        pools.put(UFO_BOMB_BOT, GameEntityPoolCreator.create { OLD_UFOBombBot(game) })
        pools.put(ROLLING_BOT, GameEntityPoolCreator.create { RollingBot(game) })
        pools.put(TOXIC_BARREL_BOT, GameEntityPoolCreator.create { ToxicBarrelBot(game) })
        pools.put(ANGRY_FLAME_BALL, GameEntityPoolCreator.create { AngryFlameBall(game) })
        pools.put(POPOHELI, GameEntityPoolCreator.create { Popoheli(game) })
        pools.put(POPUP_CANON, GameEntityPoolCreator.create { PopupCanon(game) })
        pools.put(JET_MET, GameEntityPoolCreator.create { JetMet(game) })
        pools.put(BUNBY_TANK, GameEntityPoolCreator.create { BunbyTank(game) })
        pools.put(FIRE_MET, GameEntityPoolCreator.create { FireMet(game) })
        pools.put(FIRE_MET_SPAWNER, GameEntityPoolCreator.create { FireMetSpawner(game) })
        pools.put(FLAME_HEAD_THROWER, GameEntityPoolCreator.create { FlameHeadThrower(game) })
        pools.put(PIPI, GameEntityPoolCreator.create { Pipi(game) })
        pools.put(COPIPI, GameEntityPoolCreator.create { Copipi(game) })
        pools.put(BOMB_POTTON, GameEntityPoolCreator.create { BombPotton(game) })
        pools.put(ARIGOCK, GameEntityPoolCreator.create { Arigock(game) })
        pools.put(BOMB_CHUTE, GameEntityPoolCreator.create { BombChute(game) })
        pools.put(TOTEM_POLEM, GameEntityPoolCreator.create { TotemPolem(game) })
        pools.put(CARRI_CARRY, GameEntityPoolCreator.create { CarriCarry(game) })
        pools.put(CACTUS_LAUNCHER, GameEntityPoolCreator.create { CactusLauncher(game) })
        pools.put(COLTON_JOE, GameEntityPoolCreator.create { ColtonJoe(game) })
        pools.put(TURN_BLASTER, GameEntityPoolCreator.create { TurnBlaster(game) })
        pools.put(JETPACK_ICE_BLASTER, GameEntityPoolCreator.create { JetpackIceBlaster(game) })
        pools.put(SPIKE_BOT, GameEntityPoolCreator.create { SpikeBot(game) })
        pools.put(TROPISH, GameEntityPoolCreator.create { Tropish(game) })
        pools.put(SEALION, GameEntityPoolCreator.create { Sealion(game) })
        pools.put(YELLOW_TIGGER_SQUIRT, GameEntityPoolCreator.create { YellowTiggerSquirt(game) })
        pools.put(UNDERWATER_PENGUIN_BOT, GameEntityPoolCreator.create { UnderwaterPenguinBot(game) })
        pools.put(UF_OH_NO_BOT, GameEntityPoolCreator.create { UFOhNoBot(game) })
        pools.put(TANK_BOT, GameEntityPoolCreator.create { TankBot(game) })
        pools.put(WALRUS_BOT, GameEntityPoolCreator.create { WalrusBot(game) })
        pools.put(DARSPIDER, GameEntityPoolCreator.create { Darspider(game) })
        pools.put(BIG_FISH_NEO, GameEntityPoolCreator.create { BigFishNeo(game) })
        pools.put(FIRE_DISPENSENATOR, GameEntityPoolCreator.create { FireDispensenator(game) })
        pools.put(DEMON_MET, GameEntityPoolCreator.create { DemonMet(game) })
        pools.put(RAT_ROBOT, GameEntityPoolCreator.create { RatRobot(game) })
    }

    override fun fetch(key: Any): MegaGameEntity? {
        GameLogger.debug(TAG, "Spawning Enemy: key = $key")
        return pools.get(key)?.fetch()
    }
}
