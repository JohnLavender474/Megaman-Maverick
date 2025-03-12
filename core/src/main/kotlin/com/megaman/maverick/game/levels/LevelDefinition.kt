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
        tmxMapSource = "IntroStage_v2.tmx",
        music = MusicAsset.MMX2_INTRO_STAGE_MUSIC,
        screenOnCompletion = { ScreenEnum.SAVE_GAME_SCREEN }
    ),
    TIMBER_WOMAN(
        type = LevelType.ROBOT_MASTER_LEVEL,
        mugshotAtlas = "FACES_1",
        mugshotRegion = "Timber Woman",
        tmxMapSource = "TimberWoman_16x14_v2.tmx",
        music = MusicAsset.MMX3_NEON_TIGER_MUSIC,
        screenOnCompletion = { ScreenEnum.SAVE_GAME_SCREEN }
    ),
    MOON_MAN(
        type = LevelType.ROBOT_MASTER_LEVEL,
        mugshotAtlas = "FACES_1",
        mugshotRegion = "Moon Man",
        tmxMapSource = "MoonMan_16x14_v3.tmx",
        music = MusicAsset.MMX5_DARK_DIZZY_MUSIC,
        screenOnCompletion = { ScreenEnum.SAVE_GAME_SCREEN }
    ),
    RODENT_MAN(
        type = LevelType.ROBOT_MASTER_LEVEL,
        mugshotAtlas = "FACES_1",
        mugshotRegion = "Rodent Man",
        tmxMapSource = "RodentMan_16x14.tmx",
        music = MusicAsset.MM7_SLASH_MAN_MUSIC,
        screenOnCompletion = { ScreenEnum.SAVE_GAME_SCREEN }
    ),
    DESERT_MAN(
        type = LevelType.ROBOT_MASTER_LEVEL,
        mugshotAtlas = "FACES_1",
        mugshotRegion = "Desert Man",
        tmxMapSource = "DesertMan_16x14_v3.tmx",
        music = MusicAsset.MMX7_VANISHING_GUNGAROO_MUSIC,
        screenOnCompletion = { ScreenEnum.SAVE_GAME_SCREEN }
    ),
    INFERNO_MAN(
        type = LevelType.ROBOT_MASTER_LEVEL,
        mugshotAtlas = "FACES_1",
        mugshotRegion = "Inferno Man",
        tmxMapSource = "InfernoMan_16x14_v2.tmx",
        music = MusicAsset.INFERNO_MAN_MUSIC_OLD,
        screenOnCompletion = { ScreenEnum.SAVE_GAME_SCREEN }
    ),
    REACTOR_MAN(
        type = LevelType.ROBOT_MASTER_LEVEL,
        mugshotAtlas = "FACES_1",
        mugshotRegion = "Reactor Man",
        tmxMapSource = "ReactorMan_v2.tmx",
        music = MusicAsset.MMX8_BURN_ROOSTER_MUSIC,
        screenOnCompletion = { ScreenEnum.SAVE_GAME_SCREEN }
    ),
    GLACIER_MAN(
        type = LevelType.ROBOT_MASTER_LEVEL,
        mugshotAtlas = "FACES_1",
        mugshotRegion = "Glacier Man",
        tmxMapSource = "GlacierMan_16x14.tmx",
        music = MusicAsset.MMX_CHILL_PENGUIN_MUSIC,
        screenOnCompletion = { ScreenEnum.SAVE_GAME_SCREEN }
    ),
    PRECIOUS_WOMAN(
        type = LevelType.ROBOT_MASTER_LEVEL,
        mugshotAtlas = "FACES_1",
        mugshotRegion = "Precious Woman",
        tmxMapSource = "PreciousWoman.tmx",
        music = MusicAsset.MMX2_CRYSTAL_SNAIL_MUSIC,
        screenOnCompletion = { ScreenEnum.SAVE_GAME_SCREEN }
    ),
    WILY_STAGE_1(
        type = LevelType.WILY_LEVEL,
        tmxMapSource = "WilyStage1_v2.tmx",
        music = MusicAsset.MMX_SIGMA_FORTRESS_1_MUSIC,
        screenOnCompletion = { ScreenEnum.WILY_CASTLE_SCREEN }
    ),
    WILY_STAGE_2(
        type = LevelType.WILY_LEVEL,
        tmxMapSource = "WilyStage2.tmx",
        music = MusicAsset.MM3_SNAKE_MAN_MUSIC,
        screenOnCompletion = { ScreenEnum.WILY_CASTLE_SCREEN }
    ),
    WILY_STAGE_3(
        type = LevelType.WILY_LEVEL,
        tmxMapSource = "WilyStage3_v2.tmx",
        music = MusicAsset.MM3_SNAKE_MAN_MUSIC,
        screenOnCompletion = { ScreenEnum.WILY_CASTLE_SCREEN }
    ),
    TEST_1(
        type = LevelType.TEST_LEVEL,
        tmxMapSource = "Test1.tmx",
        music = MusicAsset.MM3_SNAKE_MAN_MUSIC,
        screenOnCompletion = { ScreenEnum.WILY_CASTLE_SCREEN }
    ),
    TEST_TILESET_SIZE(
        type = LevelType.TEST_LEVEL,
        tmxMapSource = "TilesetSizeTest.tmx",
        music = MusicAsset.MM3_SNAKE_MAN_MUSIC,
        screenOnCompletion = { ScreenEnum.WILY_CASTLE_SCREEN }
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
