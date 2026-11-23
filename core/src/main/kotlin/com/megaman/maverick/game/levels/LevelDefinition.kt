package com.megaman.maverick.game.levels

import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.OrderedSet
import com.maverick.game.engine.common.extensions.objectSetOf
import com.maverick.game.engine.common.extensions.putIfAbsentAndGet
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.assets.IAsset
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.difficulty.DifficultyMode
import com.megaman.maverick.game.entities.items.Life
import com.megaman.maverick.game.entities.items.WeaponEnergyBulb
import com.megaman.maverick.game.entities.megaman.constants.MegaHealthTank
import com.megaman.maverick.game.entities.megaman.constants.MegaHeartTank
import com.megaman.maverick.game.entities.megaman.weapons.MegamanWeapons
import com.megaman.maverick.game.weapons.WeaponDef
import com.megaman.maverick.game.screens.ScreenEnum
import com.megaman.maverick.game.players.PlayerCharacter

enum class LevelDefinition(
    val type: LevelType,
    val tmxMapSource: String,
    val screenOnCompletion: (LevelState) -> ScreenEnum,
    val music: MusicAsset? = null,
    val mugshotAtlas: String? = null,
    val mugshotRegion: String? = null,
    val levelOverride: LevelOverride? = null,
    val characters: Set<PlayerCharacter> = setOf(PlayerCharacter.MEGAMAN)
) : IAsset {
    INTRO_STAGE(
        type = LevelType.INTRO_LEVEL,
        tmxMapSource = "IntroStage_v3.tmx",
        music = MusicAsset.VINNYZ_INTRO_STAGE_MUSIC,
        screenOnCompletion = RobotMasterLevelOnCompletion
    ),
    TIMBER_WOMAN(
        type = LevelType.ROBOT_MASTER_LEVEL,
        mugshotAtlas = "FACES_1",
        mugshotRegion = "Timber Woman",
        tmxMapSource = "TimberWoman_16x14_v4.tmx",
        music = MusicAsset.MEGA_QUEST_2_LEVEL_1_MUSIC,
        screenOnCompletion = RobotMasterLevelOnCompletion
    ),
    MOON_MAN(
        type = LevelType.ROBOT_MASTER_LEVEL,
        mugshotAtlas = "FACES_1",
        mugshotRegion = "Moon Man",
        tmxMapSource = "MoonMan_16x14_v5.tmx",
        music = MusicAsset.MMX5_DARK_DIZZY_MUSIC,
        screenOnCompletion = RobotMasterLevelOnCompletion
    ),
    RODENT_MAN(
        type = LevelType.ROBOT_MASTER_LEVEL,
        mugshotAtlas = "FACES_1",
        mugshotRegion = "Rodent Man",
        tmxMapSource = "RodentMan_16x14.tmx",
        music = MusicAsset.FAMITARD_OC_2_MUSIC,
        screenOnCompletion = RobotMasterLevelOnCompletion
    ),
    DESERT_MAN(
        type = LevelType.ROBOT_MASTER_LEVEL,
        mugshotAtlas = "FACES_1",
        mugshotRegion = "Desert Man",
        tmxMapSource = "DesertMan_16x14_v4.tmx",
        music = MusicAsset.CODY_O_QUINN_BATTLE_MAN_MUSIC_INTRO,
        screenOnCompletion = RobotMasterLevelOnCompletion
    ),
    INFERNO_MAN(
        type = LevelType.ROBOT_MASTER_LEVEL,
        mugshotAtlas = "FACES_1",
        mugshotRegion = "Inferno Man",
        tmxMapSource = "InfernoMan_16x14_v3.tmx",
        music = MusicAsset.INFERNO_MAN_INTRO_MUSIC,
        screenOnCompletion = RobotMasterLevelOnCompletion
    ),
    REACTOR_MAN(
        type = LevelType.ROBOT_MASTER_LEVEL,
        mugshotAtlas = "FACES_1",
        mugshotRegion = "Reactor Man",
        tmxMapSource = "ReactorMan_v3.tmx",
        music = MusicAsset.FAMITARD_OC_1_MUSIC,
        screenOnCompletion = RobotMasterLevelOnCompletion
    ),
    GLACIER_MAN(
        type = LevelType.ROBOT_MASTER_LEVEL,
        mugshotAtlas = "FACES_1",
        mugshotRegion = "Glacier Man",
        tmxMapSource = "GlacierMan_16x14_v2.tmx",
        music = MusicAsset.VINNYZ_GLACIER_MUSIC,
        screenOnCompletion = RobotMasterLevelOnCompletion
    ),
    PRECIOUS_WOMAN(
        type = LevelType.ROBOT_MASTER_LEVEL,
        mugshotAtlas = "FACES_1",
        mugshotRegion = "Precious Woman",
        tmxMapSource = "PreciousWoman_v2.tmx",
        music = MusicAsset.MMX2_CRYSTAL_SNAIL_MUSIC,
        screenOnCompletion = RobotMasterLevelOnCompletion
    ),
    WILY_STAGE_1(
        type = LevelType.WILY_LEVEL,
        tmxMapSource = "WilyStage1_v2.tmx",
        music = MusicAsset.MMX_SIGMA_FORTRESS_1_MUSIC,
        screenOnCompletion = { ScreenEnum.SAVE_GAME_SCREEN }
    ),
    WILY_STAGE_2(
        type = LevelType.WILY_LEVEL,
        tmxMapSource = "WilyStage2_v2.tmx",
        music = MusicAsset.CYBERNETIC_FACTORY_MUSIC,
        screenOnCompletion = { ScreenEnum.SAVE_GAME_SCREEN }
    ),
    WILY_STAGE_3(
        type = LevelType.WILY_LEVEL,
        tmxMapSource = "WilyStage3_v4.tmx",
        music = MusicAsset.MMX6_GATE_STAGE_MUSIC,
        screenOnCompletion = { ScreenEnum.SAVE_GAME_SCREEN }
    ),
    TEST_1(
        type = LevelType.TEST_LEVEL,
        tmxMapSource = "Test1.tmx",
        music = MusicAsset.MM3_SNAKE_MAN_MUSIC,
        screenOnCompletion = { ScreenEnum.SAVE_GAME_SCREEN }
    ),
    TEST_2(
        type = LevelType.TEST_LEVEL,
        tmxMapSource = "QuickRun.tmx",
        music = MusicAsset.MM3_SNAKE_MAN_MUSIC,
        screenOnCompletion = { ScreenEnum.SAVE_GAME_SCREEN }
    ),
    REFACTOR_TEST_029(
        type = LevelType.TEST_LEVEL,
        tmxMapSource = "RefactorTest029.tmx",
        music = MusicAsset.MM3_SNAKE_MAN_MUSIC,
        screenOnCompletion = { ScreenEnum.SAVE_GAME_SCREEN },
        levelOverride = LevelOverride(
            loadout = LoadoutOverride(
                weapons = objectSetOf<WeaponDef<*>>(MegamanWeapons.FRIGID_SHOT, MegamanWeapons.MOON_SCYTHES)
            )
        )
    ),
    REFACTOR_TEST_031(
        type = LevelType.TEST_LEVEL,
        tmxMapSource = "RefactorTest031.tmx",
        music = MusicAsset.MM3_SNAKE_MAN_MUSIC,
        screenOnCompletion = { ScreenEnum.SAVE_GAME_SCREEN },
        levelOverride = LevelOverride(
            loadout = LoadoutOverride(
                weapons = objectSetOf<WeaponDef<*>>(MegamanWeapons.FRIGID_SHOT, MegamanWeapons.MOON_SCYTHES)
            )
        )
    ),
    REFACTOR_TEST_084(
        type = LevelType.TEST_LEVEL,
        tmxMapSource = "RefactorTest084.tmx",
        music = MusicAsset.MM3_SNAKE_MAN_MUSIC,
        screenOnCompletion = { ScreenEnum.SAVE_GAME_SCREEN }
    ),
    REFACTOR_TEST_016(
        type = LevelType.TEST_LEVEL,
        tmxMapSource = "RefactorTest016.tmx",
        music = MusicAsset.MM3_SNAKE_MAN_MUSIC,
        screenOnCompletion = { ScreenEnum.SAVE_GAME_SCREEN }
    ),
    REFACTOR_TEST_051(
        type = LevelType.TEST_LEVEL,
        tmxMapSource = "RefactorTest051.tmx",
        music = MusicAsset.MM3_SNAKE_MAN_MUSIC,
        screenOnCompletion = { ScreenEnum.SAVE_GAME_SCREEN }
    ),
    REFACTOR_TEST_038(
        type = LevelType.TEST_LEVEL,
        tmxMapSource = "RefactorTest038.tmx",
        music = MusicAsset.MM3_SNAKE_MAN_MUSIC,
        screenOnCompletion = { ScreenEnum.SAVE_GAME_SCREEN }
    ),
    REFACTOR_TEST_047(
        type = LevelType.TEST_LEVEL,
        tmxMapSource = "RefactorTest047.tmx",
        music = MusicAsset.MM3_SNAKE_MAN_MUSIC,
        screenOnCompletion = { ScreenEnum.SAVE_GAME_SCREEN }
    ),
    REFACTOR_TEST_127(
        type = LevelType.TEST_LEVEL,
        tmxMapSource = "RefactorTest127.tmx",
        music = MusicAsset.MM3_SNAKE_MAN_MUSIC,
        screenOnCompletion = { ScreenEnum.SAVE_GAME_SCREEN }
    ),
    REFACTOR_TEST_068(
        type = LevelType.TEST_LEVEL,
        tmxMapSource = "RefactorTest068.tmx",
        music = MusicAsset.MM3_SNAKE_MAN_MUSIC,
        screenOnCompletion = { ScreenEnum.SAVE_GAME_SCREEN }
    ),
    REFACTOR_TEST_081(
        type = LevelType.TEST_LEVEL,
        tmxMapSource = "RefactorTest081.tmx",
        music = MusicAsset.MM3_SNAKE_MAN_MUSIC,
        screenOnCompletion = { ScreenEnum.SAVE_GAME_SCREEN },
        characters = setOf(PlayerCharacter.X)
    ),
    REFACTOR_TEST_060(
        type = LevelType.TEST_LEVEL,
        tmxMapSource = "RefactorTest060.tmx",
        music = MusicAsset.MM3_SNAKE_MAN_MUSIC,
        screenOnCompletion = { ScreenEnum.SAVE_GAME_SCREEN },
        characters = setOf(PlayerCharacter.PROTOMAN)
    ),
    REFACTOR_TEST_134(
        type = LevelType.TEST_LEVEL,
        tmxMapSource = "RefactorTest134.tmx",
        music = MusicAsset.MM3_SNAKE_MAN_MUSIC,
        screenOnCompletion = { ScreenEnum.SAVE_GAME_SCREEN }
    ),
    REFACTOR_TEST_137(
        type = LevelType.TEST_LEVEL,
        tmxMapSource = "RefactorTest137.tmx",
        music = MusicAsset.MM3_SNAKE_MAN_MUSIC,
        screenOnCompletion = { ScreenEnum.SAVE_GAME_SCREEN }
    ),
    REFACTOR_TEST_079(
        type = LevelType.TEST_LEVEL,
        tmxMapSource = "RefactorTest079.tmx",
        music = MusicAsset.MM3_SNAKE_MAN_MUSIC,
        screenOnCompletion = { ScreenEnum.SAVE_GAME_SCREEN }
    ),
    REFACTOR_TEST_045(
        type = LevelType.TEST_LEVEL,
        tmxMapSource = "RefactorTest045.tmx",
        music = MusicAsset.MM3_SNAKE_MAN_MUSIC,
        screenOnCompletion = { ScreenEnum.SAVE_GAME_SCREEN }
    ),
    REFACTOR_TEST_077(
        type = LevelType.TEST_LEVEL,
        tmxMapSource = "RefactorTest077.tmx",
        music = MusicAsset.MM3_SNAKE_MAN_MUSIC,
        screenOnCompletion = { ScreenEnum.SAVE_GAME_SCREEN }
    ),
    REFACTOR_TEST_078(
        type = LevelType.TEST_LEVEL,
        tmxMapSource = "RefactorTest078.tmx",
        music = MusicAsset.MM3_SNAKE_MAN_MUSIC,
        screenOnCompletion = { ScreenEnum.SAVE_GAME_SCREEN }
    ),
    REFACTOR_TEST_109(
        type = LevelType.TEST_LEVEL,
        tmxMapSource = "RefactorTest109.tmx",
        music = MusicAsset.MM3_SNAKE_MAN_MUSIC,
        screenOnCompletion = { ScreenEnum.SAVE_GAME_SCREEN }
    ),
    REFACTOR_TEST_178(
        type = LevelType.TEST_LEVEL,
        tmxMapSource = "RefactorTest178.tmx",
        music = MusicAsset.MM3_SNAKE_MAN_MUSIC,
        screenOnCompletion = { ScreenEnum.SAVE_GAME_SCREEN }
    ),
    FIX_TEST_SPAWNER_REARM(
        type = LevelType.TEST_LEVEL,
        tmxMapSource = "FixTestSpawnerRearm.tmx",
        music = MusicAsset.MM3_SNAKE_MAN_MUSIC,
        screenOnCompletion = { ScreenEnum.SAVE_GAME_SCREEN }
    ),
    SMB3_1(
        type = LevelType.MARIO_LEVEL,
        tmxMapSource = "SMB3_1.tmx",
        screenOnCompletion = { ScreenEnum.LEVEL_SCREEN }
    ),
    GAUNTLET_1(
        type = LevelType.EXTRA_LEVEL,
        tmxMapSource = "Gauntlet1_v2.tmx",
        music = MusicAsset.MMX5_VOLT_KRAKEN_MUSIC,
        screenOnCompletion = { ScreenEnum.EXTRAS_SCREEN },
        levelOverride = LevelOverride(
            loadout = LoadoutOverride(
                heartTanks = MegaHeartTank.entries.toSet(),
                healthTanks = MegaHealthTank.entries.associateWith { ConstVals.MAX_HEALTH }
            ),
            difficultyMode = DifficultyMode.HARD,
            canSpawn = { it !is Life && it !is WeaponEnergyBulb }
        ),
    ),
    X6_INTRO(
        type = LevelType.EXTRA_LEVEL,
        tmxMapSource = "X6Intro_16x14_v1.tmx",
        music = MusicAsset.MMX6_INTRO_STAGE_MUSIC,
        screenOnCompletion = { ScreenEnum.EXTRAS_SCREEN },
        characters = setOf(PlayerCharacter.X),
        // TODO: X starts the intro with every heart tank and health tank, a play-test aid for the D-1000 fight; consider removing this loadout override later (item 081)
        levelOverride = LevelOverride(
            loadout = LoadoutOverride(
                heartTanks = MegaHeartTank.entries.toSet(),
                healthTanks = MegaHealthTank.entries.associateWith { ConstVals.MAX_HEALTH }
            )
        )
    );

    // TODO: Gauntlet 2 is retired: its waves need JetMet, and CeilingCrusher has a pooled scratch object fault; restore
    //  JetMet before this entry (docs/issues/2026-11-02-retire-gauntlet-2.md)
    // GAUNTLET_2(
    //     type = LevelType.EXTRA_LEVEL,
    //     tmxMapSource = "Gauntlet2.tmx",
    //     music = MusicAsset.MMX5_VOLT_KRAKEN_MUSIC,
    //     screenOnCompletion = { ScreenEnum.EXTRAS_SCREEN },
    //     levelOverride = LevelOverride(
    //         loadout = LoadoutOverride(
    //             heartTanks = MegaHeartTank.entries.toSet(),
    //             healthTanks = MegaHealthTank.entries.associateWith { ConstVals.MAX_HEALTH }
    //         ),
    //         difficultyMode = DifficultyMode.HARD,
    //         canSpawn = {
    //             it !is Life && it !is WeaponEnergyBulb
    //         }
    //     ),
    // );

    // RUMBLECADE_FACTORY(
    //     type = LevelType.EXTRA_LEVEL,
    //     tmxMapSource = "RumblecadeFactory.tmx",
    //     music = MusicAsset.MMX5_VOLT_KRAKEN_MUSIC,
    //     screenOnCompletion = { ScreenEnum.EXTRAS_SCREEN },
    //     levelOverride = LevelOverride(
    //         loadout = LoadoutOverride(
    //             heartTanks = MegaHeartTank.entries.toSet(),
    //             healthTanks = MegaHealthTank.entries.associateWith { ConstVals.MAX_HEALTH }
    //         ),
    //         difficultyMode = DifficultyMode.HARD,
    //         canSpawn = {
    //             it !is Life && it !is WeaponEnergyBulb
    //         }
    //     ),
    // ),

    companion object {
        const val TMX_SOURCE_PREFIX = "tiled_maps/tmx/"
    }

    override val source: String
        get() = "${TMX_SOURCE_PREFIX}${tmxMapSource}"
    override val assClass: Class<*>
        get() = TiledMap::class.java

    fun getFormattedName() = name.replace("_", " ")
}

object LevelDefMap {

    private val levelTypeToDefs = OrderedMap<LevelType, OrderedSet<LevelDefinition>>()

    init {
        LevelDefinition.entries.forEach { def ->
            val type = def.type
            levelTypeToDefs.putIfAbsentAndGet(type) { OrderedSet() }.add(def)
        }
    }

    fun getDefsOfLevelType(type: LevelType): OrderedSet<LevelDefinition> =
        levelTypeToDefs.get(type) ?: throw IllegalArgumentException("No defs for level type $type")
}
