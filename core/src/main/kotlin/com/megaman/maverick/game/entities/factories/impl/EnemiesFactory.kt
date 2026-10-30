package com.megaman.maverick.game.entities.factories.impl

import com.maverick.game.engine.common.GameLogger
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.enemies.*
import com.megaman.maverick.game.entities.factories.EntityFactory
import com.megaman.maverick.game.entities.factories.GameEntityPoolCreator

class EnemiesFactory(private val services: MegaServices) : EntityFactory() {

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
        const val NUTT_GLIDER = "NuttGlider"
        const val DEATH_BAT = "DeathBat"
        const val ASTRO_ASS_ASSAULTER = "AstroAssAssaulter"
        const val STAGED_MOON_LANDING_FLAG = "StagedMoonLandingFlag"
        const val CANNON_HOPPER = "CannonHopper"
    }

    override fun init(vararg params: Any) {
        pools.put(TEST_ENEMY, GameEntityPoolCreator.create { TestEnemy(services) })
        pools.put(BAT, GameEntityPoolCreator.create { Bat(services) })
        pools.put(MET, GameEntityPoolCreator.create { Met(services) })
        pools.put(FLOATING_CAN, GameEntityPoolCreator.create { FloatingCan(services) })
        pools.put(FLOATING_CAN_HOLE, GameEntityPoolCreator.create { FloatingCanHole(services) })
        pools.put(DRAGON_FLY, GameEntityPoolCreator.create { DragonFly(services) })
        pools.put(FLY_BOY, GameEntityPoolCreator.create { FlyBoy(services) })
        pools.put(GAPING_FISH, GameEntityPoolCreator.create { GapingFish(services) })
        pools.put(SPRING_HEAD, GameEntityPoolCreator.create { SpringHead(services) })
        pools.put(SUCTION_ROLLER, GameEntityPoolCreator.create { SuctionRoller(services) })
        pools.put(MAG_FLY, GameEntityPoolCreator.create { MagFly(services) })
        pools.put(MATASABURO, GameEntityPoolCreator.create { Matasaburo(services) })
        pools.put(SWINGIN_JOE, GameEntityPoolCreator.create { SwinginJoe(services) })
        pools.put(SNIPER_JOE, GameEntityPoolCreator.create { SniperJoe_OLD(services) })
        pools.put(CARTIN_JOE, GameEntityPoolCreator.create { CartinJoe(services) })
        pools.put(PENGUIN, GameEntityPoolCreator.create { Penguin(services) })
        pools.put(SHIELD_ATTACKER, GameEntityPoolCreator.create { ShieldAttacker(services) })
        pools.put(SCREWIE, GameEntityPoolCreator.create { Screwie(services) })
        pools.put(HANABIRAN, GameEntityPoolCreator.create { Hanabiran(services) })
        pools.put(RATTON, GameEntityPoolCreator.create { Ratton(services) })
        pools.put(PICKET_JOE, GameEntityPoolCreator.create { PicketJoe(services) })
        pools.put(ELECN, GameEntityPoolCreator.create { Elecn(services) })
        pools.put(ROBBIT, GameEntityPoolCreator.create { Robbit(services) })
        pools.put(CAVE_ROCKER, GameEntityPoolCreator.create { CaveRocker(services) })
        pools.put(EYEE, GameEntityPoolCreator.create { Eyee(services) })
        pools.put(ADAMSKI, GameEntityPoolCreator.create { Adamski(services) })
        pools.put(BIG_JUMPING_JOE, GameEntityPoolCreator.create { BigJumpingJoe(services) })
        pools.put(UP_N_DOWN, GameEntityPoolCreator.create { UpNDown(services) })
        pools.put(SUICIDE_BUMMER, GameEntityPoolCreator.create { SuicideBummer(services) })
        pools.put(GACHAPPAN, GameEntityPoolCreator.create { Gachappan(services) })
        pools.put(BULB_BLASTER, GameEntityPoolCreator.create { BulbBlaster(services) })
        pools.put(IMORM, GameEntityPoolCreator.create { Imorm(services) })
        pools.put(WANAAN, GameEntityPoolCreator.create { Wanaan(services) })
        pools.put(PEAT, GameEntityPoolCreator.create { Peat(services) })
        pools.put(BABY_SPIDER, GameEntityPoolCreator.create { BabySpider(services) })
        pools.put(HELI_MET, GameEntityPoolCreator.create { HeliMet(services) })
        pools.put(SHOT_MAN, GameEntityPoolCreator.create { Shotman(services) })
        pools.put(PETIT_DEVIL, GameEntityPoolCreator.create { PetitDevil(services) })
        pools.put(PETIT_DEVIL_CHILD, GameEntityPoolCreator.create { PetitDevilChild(services) })
        pools.put(SNOWHEAD_THROWER, GameEntityPoolCreator.create { SnowheadThrower(services) })
        pools.put(SPIKY, GameEntityPoolCreator.create { Spiky(services) })
        pools.put(BABY_PENGUIN, GameEntityPoolCreator.create { BabyPenguin(services) })
        pools.put(ROLLING_BOT, GameEntityPoolCreator.create { RollingBot(services) })
        pools.put(TOXIC_BARREL_BOT, GameEntityPoolCreator.create { ToxicBarrelBot(services) })
        pools.put(ANGRY_FLAME_BALL, GameEntityPoolCreator.create { AngryFlameBall(services) })
        pools.put(POPOHELI, GameEntityPoolCreator.create { Popoheli(services) })
        pools.put(POPUP_CANON, GameEntityPoolCreator.create { PopupCanon(services) })
        pools.put(JET_MET, GameEntityPoolCreator.create { JetMet(services) })
        pools.put(BUNBY_TANK, GameEntityPoolCreator.create { BunbyTank(services) })
        pools.put(FIRE_MET, GameEntityPoolCreator.create { FireMet(services) })
        pools.put(FIRE_MET_SPAWNER, GameEntityPoolCreator.create { FireMetSpawner(services) })
        pools.put(FLAME_HEAD_THROWER, GameEntityPoolCreator.create { FlameHeadThrower(services) })
        pools.put(PIPI, GameEntityPoolCreator.create { Pipi(services) })
        pools.put(COPIPI, GameEntityPoolCreator.create { Copipi(services) })
        pools.put(BOMB_POTTON, GameEntityPoolCreator.create { BombPotton(services) })
        pools.put(ARIGOCK, GameEntityPoolCreator.create { Arigock(services) })
        pools.put(BOMB_CHUTE, GameEntityPoolCreator.create { BombChute(services) })
        pools.put(TOTEM_POLEM, GameEntityPoolCreator.create { TotemPolem(services) })
        pools.put(CARRI_CARRY, GameEntityPoolCreator.create { CarriCarry(services) })
        pools.put(CACTUS_LAUNCHER, GameEntityPoolCreator.create { CactusLauncher(services) })
        pools.put(COLTON_JOE, GameEntityPoolCreator.create { ColtonJoe(services) })
        pools.put(TURN_BLASTER, GameEntityPoolCreator.create { TurnBlaster(services) })
        pools.put(JETPACK_ICE_BLASTER, GameEntityPoolCreator.create { JetpackBlaster(services) })
        pools.put(SPIKE_BOT, GameEntityPoolCreator.create { SpikeBot_OLD(services) })
        pools.put(TROPISH, GameEntityPoolCreator.create { Tropish(services) })
        pools.put(SEALION, GameEntityPoolCreator.create { Sealion(services) })
        pools.put(YELLOW_TIGGER_SQUIRT, GameEntityPoolCreator.create { YellowTiggerSquirt(services) })
        pools.put(UNDERWATER_PENGUIN_BOT, GameEntityPoolCreator.create { UnderwaterPenguinBot(services) })
        pools.put(UF_OH_NO_BOT, GameEntityPoolCreator.create { UFOhNoBot(services) })
        pools.put(TANK_BOT, GameEntityPoolCreator.create { TankBot(services) })
        pools.put(WALRUS_BOT, GameEntityPoolCreator.create { WalrusBot(services) })
        pools.put(DARSPIDER, GameEntityPoolCreator.create { Darspider(services) })
        pools.put(BIG_FISH_NEO, GameEntityPoolCreator.create { BigFishNeo(services) })
        pools.put(FIRE_DISPENSENATOR, GameEntityPoolCreator.create { FireDispensenator(services) })
        pools.put(DEMON_MET, GameEntityPoolCreator.create { DemonMet(services) })
        pools.put(RAT_ROBOT, GameEntityPoolCreator.create { RatRobot(services) })
        pools.put(NUTT_GLIDER, GameEntityPoolCreator.create { NuttGlider(services) })
        pools.put(DEATH_BAT, GameEntityPoolCreator.create { DeathBat(services) })
        pools.put(ASTRO_ASS_ASSAULTER, GameEntityPoolCreator.create { AstroAssAssaulter(services) })
        pools.put(STAGED_MOON_LANDING_FLAG, GameEntityPoolCreator.create { StagedMoonLandingFlag(services) })
        pools.put(CANNON_HOPPER, GameEntityPoolCreator.create { CanonHopper(services) })
    }

    override fun fetch(key: Any?): MegaGameEntity? {
        GameLogger.debug(TAG, "Spawning enemy: key = $key")
        return pools.get(key)?.fetch()
    }
}
