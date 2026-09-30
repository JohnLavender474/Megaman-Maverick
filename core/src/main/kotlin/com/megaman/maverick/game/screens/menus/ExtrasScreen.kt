package com.megaman.maverick.game.screens.menus

import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.screens.menus.IMenuButton
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.levels.LevelType
import com.megaman.maverick.game.screens.ScreenEnum
import com.megaman.maverick.game.screens.utils.MenuScrollPane
import com.megaman.maverick.game.utils.extensions.setToDefaultPosition

class ExtrasScreen(game: MegamanMaverickGame) : MegaMenuScreen(game, BACK), Initializable {

    companion object {
        const val TAG = "ExtrasScreen"
        private const val BACK = "BACK"
        private const val BKG_REG_KEY = "menu_screen_bkg_v2"
        private const val TEXT_ROW_START = 10f
        private const val TEXT_X = 3f
        private const val MAX_VISIBLE_ROWS = 10
        private const val WRAP = true
    }

    private val backgroundSprite = GameSprite()
    private lateinit var scrollPane: MenuScrollPane

    private var initialized = false

    override fun init(vararg params: Any) {
        if (initialized) return
        initialized = true

        GameLogger.debug(TAG, "init()")

        val uiAtlas = game.assMan.getTextureAtlas(TextureAsset.UI_1.source)
        val backgroundRegion = uiAtlas.findRegion(BKG_REG_KEY)
        backgroundSprite.setBounds(0f, 0f, ConstVals.VIEW_WIDTH * ConstVals.PPM, ConstVals.VIEW_HEIGHT * ConstVals.PPM)
        backgroundSprite.setRegion(backgroundRegion)

        scrollPane = MenuScrollPane(
            assMan = game.assMan,
            textX = TEXT_X,
            topRow = TEXT_ROW_START,
            maxVisibleRows = MAX_VISIBLE_ROWS,
            wrap = WRAP
        )

        scrollPane.addItem(BACK)
        val levels = OrderedMap<String, LevelDefinition>()
        LevelDefinition.entries.filter { it.type == LevelType.EXTRA_LEVEL }.forEach { level ->
            val key = level.getFormattedName()
            scrollPane.addItem(key)
            levels.put(key, level)
        }

        scrollPane.keys.forEach { key ->
            buttons.put(key, object : IMenuButton {

                override fun onNavigate(direction: Direction, delta: Float) = scrollPane.navigate(key, direction)

                override fun onSelect(delta: Float): Boolean {
                    game.audioMan.playSound(SoundAsset.SELECT_PING_SOUND, false)

                    if (key == BACK) {
                        game.resetPlayerProgress()
                        game.setCurrentScreen(ScreenEnum.MAIN_MENU_SCREEN.name)
                    } else game.startExtraLevel(levels[key])

                    return true
                }
            })
        }
    }

    override fun show() {
        if (!initialized) init()
        GameLogger.debug(TAG, "show()")
        super.show()
        scrollPane.reset()
        game.getUiCamera().setToDefaultPosition()
        game.audioMan.playMusic(MusicAsset.VINNYZ_MAIN_MENU_MUSIC, false)
    }

    override fun render(delta: Float) {
        super.render(delta)

        scrollPane.update(delta, buttonKey)

        val batch = game.batch
        batch.projectionMatrix = game.getUiCamera().combined
        batch.begin()

        backgroundSprite.draw(batch)
        scrollPane.draw(batch, buttonKey)

        batch.end()
    }

    override fun onNavigate(button: IMenuButton, direction: Direction, delta: Float) {
        val oldKey = buttonKey
        super.onNavigate(button, direction, delta)
        // when wrap is off, the selection does not move at the two ends of the list, so no sound plays
        if (buttonKey != oldKey) game.audioMan.playSound(SoundAsset.CURSOR_MOVE_BLOOP_SOUND)
    }
}
