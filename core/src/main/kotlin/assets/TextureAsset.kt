package com.megaman.maverick.game.assets

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.utils.Array
import com.megaman.maverick.game.assets.TextureAsset.values

const val TEXTURE_ASSET_PREFIX = "sprites/sprite_sheets/"

enum class TextureAsset(src: String) : IAsset {
    COLORS("Colors.txt"),
    ENEMIES_1("Enemies1.txt"),
    ENEMIES_2("Enemies2.txt"),
    SPECIALS_1("Specials1.txt"),
    HAZARDS_1("Hazards1.txt"),
    PROJECTILES_1("Projectiles1.txt"),
    PROJECTILES_2("Projectiles2.txt"),
    MEGAMAN_CHARGED_SHOT("MegamanChargedShot.txt"),
    ITEMS_1("Items1.txt"),
    DECORATIONS_1("Decorations1.txt"),
    ENVIRONS_1("Environs1.txt"),
    EXPLOSIONS_1("Explosions1.txt"),
    BACKGROUNDS_1("Backgrounds1.txt"),
    BACKGROUNDS_2("Backgrounds2.txt"),
    BACKGROUNDS_3("Backgrounds3.txt"),
    BACKGROUNDS_4("Backgrounds4.txt"),
    BACKGROUNDS_5("Backgrounds5.txt"),
    BACKGROUNDS_6("Backgrounds6.txt"),
    UI_1("Ui1.txt"),
    FACES_1("Faces1.txt"),
    MEGAMAN_V2_BUSTER("Megaman_v2_BUSTER.txt"),
    MEGAMAN_BUSTER("Megaman_BUSTER.txt"),
    MEGAMAN_MAVERICK_BUSTER("MegamanMaverick_BUSTER.txt"),
    MEGAMAN_FLAME_TOSS("Megaman_FLAME_TOSS.txt"),
    MEGAMAN_RUSH_JETPACK("Megaman_RUSH_JETPACK.txt"),
    MEGAMAN_TRAIL_SPRITE("MegamanTrailSprite.txt"),
    MEGAMAN_TRAIL_SPRITE_V2("MegamanTrailSprite_v2.txt"),
    GATES("Gates.txt"),
    PLATFORMS_1("Platforms1.txt"),
    BOSSES_1("Bosses1.txt"),
    BOSSES_2("Bosses2.txt"),
    TIMBER_WOMAN("TimberWoman.txt"),
    RODENT_MAN("RodentMan.txt"),
    GUTS_TANK("GutsTank.txt"),
    WINTRY_MAN("WintryMan.txt"),
    DISTRIBUTOR_MAN("DistributorMan.txt"),
    ROASTER_MAN("RoasterMan.txt"),
    MISTER_MAN("MisterMan.txt"),
    BLUNT_MAN("BluntMan.txt"),
    NUKE_MAN("NukeMan.txt"),
    FREEZER_MAN("FreezerMan.txt"),
    PRECIOUS_MAN("PreciousMan.txt"),
    MICROWAVE_MAN("MicrowaveMan.txt"),
    TEST("Test.txt");

    companion object {
        fun valuesAsIAssetArray(): Array<IAsset> {
            val assets = Array<IAsset>()
            values().forEach { assets.add(it) }
            return assets
        }
    }

    override val source = TEXTURE_ASSET_PREFIX + src
    override val assClass = TextureAtlas::class.java
}
