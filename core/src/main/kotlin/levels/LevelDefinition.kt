package com.megaman.maverick.game.levels

import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.OrderedSet
import com.mega.game.engine.common.extensions.putIfAbsentAndGet

enum class LevelDefinition(
    val type: LevelType,
    val mugshotAtlas: String,
    val mugshotRegion: String,
    val tmxMapSource: String,
    val music: String,
    val screenOnCompletion: String
) {
    TIMBER_WOMAN(
        type = LevelType.ROBOT_MASTER_LEVEL,
        mugshotAtlas = "FACES_1",
        mugshotRegion = "Timber Woman",
        tmxMapSource = "TimberWoman.tmx",
        music = "MM3_SNAKE_MAN_MUSIC",
        screenOnCompletion = "SAVE_GAME_SCREEN"
    ),
    MOON_MAN(
        type = LevelType.ROBOT_MASTER_LEVEL,
        mugshotAtlas = "FACES_1",
        mugshotRegion = "Moon Man",
        tmxMapSource = "MoonMan_16x12.tmx",
        music = "MMX5_DARK_DIZZY_MUSIC",
        screenOnCompletion = "SAVE_GAME_SCREEN"
    ),
    RODENT_MAN(
        type = LevelType.ROBOT_MASTER_LEVEL,
        mugshotAtlas = "FACES_1",
        mugshotRegion = "Rodent Man",
        tmxMapSource = "RodentMan_v2.tmx",
        music = "MM7_SLASH_MAN_MUSIC",
        screenOnCompletion = "SAVE_GAME_SCREEN"
    ),
    DESERT_MAN(
        type = LevelType.ROBOT_MASTER_LEVEL,
        mugshotAtlas = "FACES_1",
        mugshotRegion = "Desert Man",
        tmxMapSource = "DesertMan_16x12.tmx",
        music = "MMX7_VANISHING_GUNGAROO_MUSIC",
        screenOnCompletion = "SAVE_GAME_SCREEN"
    ),
    INFERNO_MAN(
        type = LevelType.ROBOT_MASTER_LEVEL,
        mugshotAtlas = "FACES_1",
        mugshotRegion = "Inferno Man",
        tmxMapSource = "InfernoMan_16x12.tmx",
        music = "MMX6_BLAZE_HEATNIX_MUSIC",
        screenOnCompletion = "SAVE_GAME_SCREEN"
    ),
    REACTOR_MAN(
        type = LevelType.ROBOT_MASTER_LEVEL,
        mugshotAtlas = "FACES_1",
        mugshotRegion = "Reactor Man",
        tmxMapSource = "ReactorMan_16x12.tmx",
        music = "MMX8_BURN_ROOSTER_MUSIC",
        screenOnCompletion = "SAVE_GAME_SCREEN"
    ),
    GLACIER_MAN(
        type = LevelType.ROBOT_MASTER_LEVEL,
        mugshotAtlas = "FACES_1",
        mugshotRegion = "Glacier Man",
        tmxMapSource = "GlacierMan_16x12_v2.tmx",
        music = "MMX_CHILL_PENGUIN_MUSIC",
        screenOnCompletion = "SAVE_GAME_SCREEN"
    ),
    PRECIOUS_MAN(
        type = LevelType.ROBOT_MASTER_LEVEL,
        mugshotAtlas = "FACES_1",
        mugshotRegion = "Precious Man",
        tmxMapSource = "PreciousMan_16x12.tmx",
        music = "MM3_SNAKE_MAN_MUSIC",
        screenOnCompletion = "SAVE_GAME_SCREEN"
    ),
    WILY_STAGE_1(
        type = LevelType.WILY_LEVEL,
        mugshotAtlas = "FACES_1",
        mugshotRegion = "Rodent Man",
        tmxMapSource = "WilyStage1.tmx",
        music = "MM3_SNAKE_MAN_MUSIC",
        screenOnCompletion = "WILY_CASTLE_SCREEN"
    ),
    WILY_STAGE_2(
        type = LevelType.WILY_LEVEL,
        mugshotAtlas = "FACES_1",
        mugshotRegion = "Rodent Man",
        tmxMapSource = "WilyStage2.tmx",
        music = "MM3_SNAKE_MAN_MUSIC",
        screenOnCompletion = "WILY_CASTLE_SCREEN"
    ),
    WILY_STAGE_3(
        type = LevelType.WILY_LEVEL,
        mugshotAtlas = "FACES_1",
        mugshotRegion = "Rodent Man",
        tmxMapSource = "WilyStage3_v2.tmx",
        music = "MM3_SNAKE_MAN_MUSIC",
        screenOnCompletion = "WILY_CASTLE_SCREEN"
    );

    fun getFormattedName() = name.replace("_", " ")
}

object LevelDefMap {

    private val levelTypeToDefs = OrderedMap<LevelType, OrderedSet<LevelDefinition>>()

    init {
        LevelDefinition.entries.forEach { def ->
            val type = def.type
            levelTypeToDefs.putIfAbsentAndGet(type, OrderedSet()).add(def)
        }
    }

    fun getDefsOfLevelType(type: LevelType): OrderedSet<LevelDefinition> =
        levelTypeToDefs.get(type) ?: throw IllegalArgumentException("No defs for level type $type")
}
