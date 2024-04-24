package com.megaman.maverick.game.screens.levels

import com.megaman.maverick.game.assets.MusicAsset

enum class Level(tmxSourceFile: String, val musicAss: MusicAsset) {
    TEST1("Test1.tmx", MusicAsset.MMX2_X_HUNTER_MUSIC),
    TEST2("Test2.tmx", MusicAsset.MMX5_VOLT_KRAKEN_MUSIC),
    TEST3("Test3.tmx", MusicAsset.XENOBLADE_GAUR_PLAINS_MUSIC),
    TEST4("Test4.tmx", MusicAsset.MMX_SIGMA_1ST_MUSIC),
    TEST5("Test5.tmx", MusicAsset.MMZ_NEO_ARCADIA_MUSIC),
    TEST6("Test6.tmx", MusicAsset.MMX3_INTRO_STAGE_MUSIC),
    TEST7("Test7.tmx", MusicAsset.MMX_SIGMA_2_STAGE),
    MAGNET_MAN("MagnetMan.tmx", MusicAsset.MM3_MAGNET_MAN),
    TIMBER_WOMAN("TimberWoman.tmx", MusicAsset.XENOBLADE_GAUR_PLAINS_MUSIC),
    DISTRIBUTOR_MAN("Test1.tmx", MusicAsset.XENOBLADE_GAUR_PLAINS_MUSIC),
    ROASTER_MAN("Test1.tmx", MusicAsset.XENOBLADE_GAUR_PLAINS_MUSIC),
    MISTER_MAN("Test1.tmx", MusicAsset.XENOBLADE_GAUR_PLAINS_MUSIC),
    BLUNT_MAN("Test1.tmx", MusicAsset.XENOBLADE_GAUR_PLAINS_MUSIC),
    NUKE_MAN("Test1.tmx", MusicAsset.XENOBLADE_GAUR_PLAINS_MUSIC),
    RODENT_MAN("CrewMan.tmx", MusicAsset.MM7_JUNK_MAN_MUSIC),
    FREEZER_MAN("FreezerMan.tmx", MusicAsset.MMX_CHILL_PENGUIN_MUSIC),
    MICROWAVE_MAN("Test1.tmx", MusicAsset.XENOBLADE_GAUR_PLAINS_MUSIC),
    GALAXY_MAN("GalaxyMan.tmx", MusicAsset.MM9_GALAXY_MAN_MUSIC);

    val tmxSourceFile = "tiled_maps/tmx/$tmxSourceFile"
}
