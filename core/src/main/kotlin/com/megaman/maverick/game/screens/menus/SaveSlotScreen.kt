package com.megaman.maverick.game.screens.menus

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.OrderedMap
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.interfaces.Initializable
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.screens.menus.IMenuButton
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.drawables.fonts.MegaFontHandle
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.screens.ScreenEnum
import com.megaman.maverick.game.screens.utils.BlinkingArrow
import com.megaman.maverick.game.state.saves.SaveSlotContent
import com.megaman.maverick.game.state.saves.SaveSlots
import com.megaman.maverick.game.utils.extensions.setToDefaultPosition

enum class SaveSlotScreenMode(val title: String, val backScreen: ScreenEnum) {
    SAVE("SAVE GAME", ScreenEnum.SAVE_GAME_SCREEN),
    LOAD("LOAD GAME", ScreenEnum.MAIN_MENU_SCREEN)
}

class SaveSlotScreen(game: MegamanMaverickGame, private val mode: SaveSlotScreenMode) :
    MegaMenuScreen(game, BACK), Initializable {

    companion object {
        const val TAG = "SaveSlotScreen"
        private const val BACK = "BACK"
        private const val YES = "YES"
        private const val NO = "NO"
        private const val BKG_REG_KEY = "menu_screen_bkg_v2"
        private const val TITLE_ROW = 11f
        private const val TEXT_ROW_START = 9f
        private const val TEXT_X = 2.5f
        private const val ARROW_OFFSET_X = -0.5f
    }

    private val keys = Array<String>()
    private val rowTexts = Array<String>()
    private val fontHandles = OrderedMap<String, MegaFontHandle>()
    private val arrows = OrderedMap<String, BlinkingArrow>()
    private val backgroundSprite = GameSprite()
    private lateinit var titleHandle: MegaFontHandle

    private lateinit var promptHandle: MegaFontHandle
    private var pendingSaveSlot: Int? = null

    private var initialized = false

    override fun init(vararg params: Any) {
        if (initialized) return
        initialized = true

        GameLogger.debug(TAG, "init(): mode=$mode")

        val uiAtlas = game.assets.getTextureAtlas(TextureAsset.UI_1.source)
        backgroundSprite.setBounds(0f, 0f, ConstVals.VIEW_WIDTH * ConstVals.PPM, ConstVals.VIEW_HEIGHT * ConstVals.PPM)
        backgroundSprite.setRegion(uiAtlas.findRegion(BKG_REG_KEY))

        titleHandle = MegaFontHandle(
            text = mode.title,
            positionX = ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f,
            positionY = TITLE_ROW * ConstVals.PPM,
            centerX = true,
            centerY = false
        )

        for (slot in 1..SaveSlots.SLOT_COUNT) {
            keys.add(slotKey(slot))
            rowTexts.add(slotKey(slot))
        }
        keys.add(BACK)
        rowTexts.add(BACK)

        var row = TEXT_ROW_START
        for (index in 0 until keys.size) {
            val key = keys[index]
            val fontHandle = MegaFontHandle(
                textSupplier = { rowTexts[index] },
                positionX = TEXT_X * ConstVals.PPM,
                positionY = row * ConstVals.PPM,
                centerX = false,
                centerY = false
            )
            fontHandles.put(key, fontHandle)

            val arrow = BlinkingArrow(game.assets)
            arrow.centerX = (TEXT_X + ARROW_OFFSET_X) * ConstVals.PPM
            arrow.centerY = (row - ConstVals.ARROW_CENTER_ROW_DECREMENT) * ConstVals.PPM
            arrows.put(key, arrow)

            row -= ConstVals.TEXT_ROW_DECREMENT * ConstVals.PPM
        }

        promptHandle = MegaFontHandle(
            textSupplier = { "SAVE TO SLOT ${pendingSaveSlot ?: ""}?" },
            positionX = TEXT_X * ConstVals.PPM,
            positionY = TEXT_ROW_START * ConstVals.PPM,
            centerX = false,
            centerY = false
        )

        var confirmRow = TEXT_ROW_START - 2f * ConstVals.TEXT_ROW_DECREMENT * ConstVals.PPM
        for (key in arrayOf(YES, NO)) {
            fontHandles.put(
                key,
                MegaFontHandle(
                    text = key,
                    positionX = TEXT_X * ConstVals.PPM,
                    positionY = confirmRow * ConstVals.PPM,
                    centerX = false,
                    centerY = false
                )
            )

            val arrow = BlinkingArrow(game.assets)
            arrow.centerX = (TEXT_X + ARROW_OFFSET_X) * ConstVals.PPM
            arrow.centerY = (confirmRow - ConstVals.ARROW_CENTER_ROW_DECREMENT) * ConstVals.PPM
            arrows.put(key, arrow)

            confirmRow -= ConstVals.TEXT_ROW_DECREMENT * ConstVals.PPM
        }

        buttons.put(YES, object : IMenuButton {

            override fun onNavigate(direction: Direction, delta: Float) = navigateConfirm(YES, direction)

            override fun onSelect(delta: Float): Boolean {
                val slot = pendingSaveSlot!!
                pendingSaveSlot = null
                save(slot)
                setCurrentButtonKey(slotKey(slot))
                return false
            }
        })

        buttons.put(NO, object : IMenuButton {

            override fun onNavigate(direction: Direction, delta: Float) = navigateConfirm(NO, direction)

            override fun onSelect(delta: Float): Boolean {
                cancelConfirm()
                return false
            }
        })

        for (slot in 1..SaveSlots.SLOT_COUNT) buttons.put(slotKey(slot), object : IMenuButton {

            override fun onNavigate(direction: Direction, delta: Float) = navigate(slotKey(slot), direction)

            override fun onSelect(delta: Float) = when (mode) {
                SaveSlotScreenMode.SAVE -> askToSave(slot)
                SaveSlotScreenMode.LOAD -> load(slot)
            }
        })

        buttons.put(BACK, object : IMenuButton {

            override fun onNavigate(direction: Direction, delta: Float) = navigate(BACK, direction)

            override fun onSelect(delta: Float): Boolean {
                game.audio.playSound(SoundAsset.SELECT_PING_SOUND, false)
                game.setCurrentScreen(mode.backScreen.name)
                return true
            }
        })
    }

    private fun slotKey(slot: Int) = "SLOT $slot"

    private fun askToSave(slot: Int): Boolean {
        game.audio.playSound(SoundAsset.SELECT_PING_SOUND, false)
        pendingSaveSlot = slot
        setCurrentButtonKey(NO)
        return false
    }

    private fun cancelConfirm() {
        val slot = pendingSaveSlot!!
        pendingSaveSlot = null
        setCurrentButtonKey(slotKey(slot))
    }

    private fun navigateConfirm(key: String, direction: Direction): String? = when (direction) {
        Direction.UP, Direction.DOWN -> if (key == YES) NO else YES
        else -> getCurrentButtonKey()
    }

    private fun save(slot: Int): Boolean {
        if (game.saveState(slot)) {
            game.audio.playSound(SoundAsset.SELECT_PING_SOUND, false)
            refreshRow(slot)
        } else game.audio.playSound(SoundAsset.ERROR_SOUND, false)
        return false
    }

    private fun load(slot: Int): Boolean {
        if (!game.loadSavedState(slot)) {
            game.audio.playSound(SoundAsset.ERROR_SOUND, false)
            return false
        }

        game.audio.playSound(SoundAsset.SELECT_PING_SOUND, false)
        if (game.gameState.isLevelCompleted(LevelDefinition.INTRO_STAGE))
            game.setCurrentScreen(ScreenEnum.LEVEL_SELECT_SCREEN.name)
        else {
            game.levelState.currentLevel = LevelDefinition.INTRO_STAGE
            game.startLevel()
        }
        return true
    }

    private fun refreshRow(slot: Int) {
        val label = slotKey(slot)
        rowTexts[slot - 1] = when (val content = game.saveSlots.read(slot)) {
            SaveSlotContent.Empty -> "$label  EMPTY"
            is SaveSlotContent.Used ->
                "$label  ${content.save.difficulty.name}  ${content.save.levelsCompleted.size} LEVELS"
            is SaveSlotContent.NewerVersion -> "$label  NEWER VERSION"
            is SaveSlotContent.Unreadable -> {
                GameLogger.error(TAG, "refreshRow(): slot $slot is unreadable: ${content.reason}")
                "$label  UNREADABLE"
            }
        }
    }

    private fun navigate(key: String, direction: Direction): String? {
        val index = keys.indexOf(key, false)
        return when (direction) {
            Direction.UP -> keys[(index - 1 + keys.size) % keys.size]
            Direction.DOWN -> keys[(index + 1) % keys.size]
            else -> getCurrentButtonKey()
        }
    }

    override fun show() {
        if (!initialized) init()
        GameLogger.debug(TAG, "show(): mode=$mode, activeSaveSlot=${game.activeSaveSlot}")

        pendingSaveSlot = null
        for (slot in 1..SaveSlots.SLOT_COUNT) refreshRow(slot)
        firstButtonKey = slotKey(game.activeSaveSlot ?: 1)

        super.show()
        game.getUiCamera().setToDefaultPosition()
    }

    override fun render(delta: Float) {
        super.render(delta)

        val arrow = arrows[buttonKey]
        arrow.update(delta)

        val batch = game.batch
        batch.projectionMatrix = game.getUiCamera().combined
        batch.begin()

        backgroundSprite.draw(batch)
        titleHandle.draw(batch)
        if (pendingSaveSlot != null) {
            promptHandle.draw(batch)
            fontHandles[YES].draw(batch)
            fontHandles[NO].draw(batch)
        } else keys.forEach { fontHandles[it].draw(batch) }
        arrow.draw(batch)

        batch.end()
    }

    override fun onBack(delta: Float) {
        if (pendingSaveSlot != null) {
            game.audio.playSound(SoundAsset.SELECT_PING_SOUND, false)
            cancelConfirm()
            return
        }

        selectionMade = buttons[BACK].onSelect(delta)
        if (selectionMade) onAnySelection()
    }

    override fun onAnyMovement(direction: Direction) {
        game.audio.playSound(SoundAsset.CURSOR_MOVE_BLOOP_SOUND)
    }
}
