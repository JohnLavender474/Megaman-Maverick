package com.megaman.maverick.game.levels

import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.OrderedSet
import com.mega.game.engine.common.extensions.putIfAbsentAndGet
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.IAsset
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.screens.ScreenEnum

enum class LevelDefinition(
    val type: LevelType,
    val music: MusicAsset,
    val tmxMapSource: String,
    val screenOnCompletion: (MegamanMaverickGame) -> ScreenEnum,
    val mugshotAtlas: String? = null,
    val mugshotRegion: String? = null
) : IAsset {
    INTRO_STAGE(
        type = LevelType.INTRO_LEVEL,
        tmxMapSource = "IntroStage_v3.tmx",
        music = MusicAsset.VINNYZ_INTRO_STAGE_MUSIC,
        screenOnCompletion = { game -> RobotMasterLevelOnCompletion.invoke(game) }
    ),
    TIMBER_WOMAN(
        type = LevelType.ROBOT_MASTER_LEVEL,
        mugshotAtlas = "FACES_1",
        mugshotRegion = "Timber Woman",
        tmxMapSource = "TimberWoman_16x14_v4.tmx",
        music = MusicAsset.MEGA_QUEST_2_LEVEL_1_MUSIC,
        screenOnCompletion = { game -> RobotMasterLevelOnCompletion.invoke(game) }
    ),
    MOON_MAN(
        type = LevelType.ROBOT_MASTER_LEVEL,
        mugshotAtlas = "FACES_1",
        mugshotRegion = "Moon Man",
        tmxMapSource = "MoonMan_16x14_v5.tmx",
        music = MusicAsset.MMX5_DARK_DIZZY_MUSIC,
        screenOnCompletion = { game -> RobotMasterLevelOnCompletion.invoke(game) }
    ),
    RODENT_MAN(
        type = LevelType.ROBOT_MASTER_LEVEL,
        mugshotAtlas = "FACES_1",
        mugshotRegion = "Rodent Man",
        tmxMapSource = "RodentMan_16x14.tmx",
        music = MusicAsset.FAMITARD_OC_2_MUSIC,
        screenOnCompletion = { game -> RobotMasterLevelOnCompletion.invoke(game) }
    ),
    DESERT_MAN(
        type = LevelType.ROBOT_MASTER_LEVEL,
        mugshotAtlas = "FACES_1",
        mugshotRegion = "Desert Man",
        tmxMapSource = "DesertMan_16x14_v4.tmx",
        music = MusicAsset.CODY_O_QUINN_BATTLE_MAN_MUSIC_INTRO,
        screenOnCompletion = { game -> RobotMasterLevelOnCompletion.invoke(game) }
    ),
    INFERNO_MAN(
        type = LevelType.ROBOT_MASTER_LEVEL,
        mugshotAtlas = "FACES_1",
        mugshotRegion = "Inferno Man",
        tmxMapSource = "InfernoMan_16x14_v2.tmx",
        music = MusicAsset.INFERNO_MAN_INTRO_MUSIC,
        screenOnCompletion = { game -> RobotMasterLevelOnCompletion.invoke(game) }
    ),
    REACTOR_MAN(
        type = LevelType.ROBOT_MASTER_LEVEL,
        mugshotAtlas = "FACES_1",
        mugshotRegion = "Reactor Man",
        tmxMapSource = "ReactorMan_v3.tmx",
        music = MusicAsset.FAMITARD_OC_1_MUSIC,
        screenOnCompletion = { game -> RobotMasterLevelOnCompletion.invoke(game) }
    ),
    GLACIER_MAN(
        type = LevelType.ROBOT_MASTER_LEVEL,
        mugshotAtlas = "FACES_1",
        mugshotRegion = "Glacier Man",
        tmxMapSource = "GlacierMan_16x14_v2.tmx",
        music = MusicAsset.VINNYZ_GLACIER_MUSIC,
        screenOnCompletion = { game -> RobotMasterLevelOnCompletion.invoke(game) }
    ),
    PRECIOUS_WOMAN(
        type = LevelType.ROBOT_MASTER_LEVEL,
        mugshotAtlas = "FACES_1",
        mugshotRegion = "Precious Woman",
        tmxMapSource = "PreciousWoman_v2.tmx",
        music = MusicAsset.MMX2_CRYSTAL_SNAIL_MUSIC,
        screenOnCompletion = { game -> RobotMasterLevelOnCompletion.invoke(game) }
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
        tmxMapSource = "Test8.tmx",
        music = MusicAsset.MMX6_GATE_STAGE_MUSIC,
        screenOnCompletion = { ScreenEnum.SAVE_GAME_SCREEN }
    ),
    TEST(
        type = LevelType.TEST_LEVEL,
        tmxMapSource = "Test8.tmx",
        music = MusicAsset.MM3_SNAKE_MAN_MUSIC,
        screenOnCompletion = { ScreenEnum.SAVE_GAME_SCREEN }
    );

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
