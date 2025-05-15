package com.megaman.maverick.game.screens.other

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.Queue
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.screens.BaseScreen
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.drawables.fonts.MegaFontHandle
import com.megaman.maverick.game.levels.LevelDefMap
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.levels.LevelType

class RobotMasterIntroScreen(private val game: MegamanMaverickGame) : BaseScreen(), Initializable {

    private data class IntroAnimationEntry(
        val x: Float,
        val y: Float,
        val pos: Position,
        val width: Float,
        val height: Float,
        val duration: Float,
        val animation: IAnimation
    )

    private val entries = OrderedMap<LevelDefinition, Queue<IntroAnimationEntry>>()
    private val queue = Queue<IntroAnimationEntry>()
    private lateinit var text: MegaFontHandle
    private val chars = Array<Character>()

    private var initialized = false

    override fun init() {
        if (initialized) return
        initialized = true

        val bosses1Atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_1.source)
        val bosses2Atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_2.source)
        val bosses3Atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_3.source)

        LevelDefMap.getDefsOfLevelType(LevelType.ROBOT_MASTER_LEVEL).forEach { level ->
            val queue = Queue<IntroAnimationEntry>()

            when (level) {
                LevelDefinition.TIMBER_WOMAN -> TODO()
                LevelDefinition.MOON_MAN -> TODO()
                LevelDefinition.RODENT_MAN -> TODO()
                LevelDefinition.DESERT_MAN -> TODO()
                LevelDefinition.INFERNO_MAN -> TODO()
                LevelDefinition.REACTOR_MAN -> TODO()
                LevelDefinition.GLACIER_MAN -> TODO()
                LevelDefinition.PRECIOUS_WOMAN -> TODO()
                else -> {}
            }

            entries.put(level, queue)
        }
    }
}
