package com.megaman.maverick.game.screens.menus.level

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.toGdxArray
import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.common.objects.table.Table
import com.mega.game.engine.common.objects.table.TableBuilder
import com.mega.game.engine.common.objects.table.TableNode
import com.mega.game.engine.common.time.TimeMarkedRunnable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.IDrawable
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.containsRegion
import com.mega.game.engine.screens.menus.IMenuButton
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.audio.MegaAudioManager
import com.megaman.maverick.game.drawables.fonts.MegaFontHandle
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegaHealthTank
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.megaman.constants.MegamanWeapon
import com.megaman.maverick.game.screens.ScreenEnum
import com.megaman.maverick.game.screens.menus.MegaMenuScreen
import com.megaman.maverick.game.screens.utils.Fade
import com.megaman.maverick.game.screens.utils.Fade.FadeType
import com.megaman.maverick.game.state.GameState
import kotlin.math.min

class LevelPauseScreen(game: MegamanMaverickGame) : MegaMenuScreen(game), Initializable {

    companion object {
        const val TAG = "LevelPauseScreen"

        private val FULL_TABLE = TableBuilder<Any>().row(gdxArrayOf(MegamanWeapon.MEGA_BUSTER, null))
            .row(gdxArrayOf(MegamanWeapon.MOON_SCYTHE, null)).row(gdxArrayOf(MegamanWeapon.FIRE_BALL, null))
            .row(gdxArrayOf(MegamanWeapon.ICE_CUBE, MegamanWeapon.RUSH_JETPACK)).row(gdxArrayOf(null, null))
            .row(gdxArrayOf(MegaHealthTank.A, MegaHealthTank.C, null))
            .row(gdxArrayOf(MegaHealthTank.B, MegaHealthTank.D, ConstKeys.EXIT)).build()

        private const val OPTIONS_PREFIX = "options"
        private const val WEAPONS_PREFIX = "weapons"
        private const val HEALTH_TANKS_PREFIX = "health_tanks"

        private const val SELECTED_SUFFIX = "_selected"

        private const val LIVES_X = 13.15f
        private const val LIVES_Y = 3.75f

        private const val SCREWS_X = 13.15f
        private const val SCREWS_Y = 2.75f

        private const val BACKGROUND_SPRITE_TARGET_POS_Y = 0f

        private const val SLIDE_OFFSET_Y = ConstVals.VIEW_HEIGHT
        private const val SLIDE_DUR = 0.5f

        private const val WEAPON_ROWS = 5
        private const val WEAPON_BITS_COLUMN_1_X = 3.45f
        private const val WEAPON_BITS_ROWS_1_Y = 6.6f
        private const val WEAPON_BITS_ROW_OFFSET = 1.075f
        private const val WEAPON_BITS_COLUMN_OFFSET = 6.675f

        private const val HEALTH_TANK_ROWS = 2
        private const val HEALTH_TANK_BITS_COLUMN_1_X = 2.275f
        private const val HEALTH_TANK_BITS_ROW_1_Y = 2.125f
        private const val HEALTH_TANK_BITS_ROW_OFFSET = 1.375f
        private const val HEALTH_TANK_BITS_COLUMN_OFFSET = 4.625f

        private const val EXIT_DUR = 1f

        private val buttonRegions = OrderedMap<String, TextureRegion>()
    }

    private val megaman: Megaman
        get() = game.megaman
    private val state: GameState
        get() = game.state
    private val audioMan: MegaAudioManager
        get() = game.audioMan

    private lateinit var table: Table<Any>
    private lateinit var node: TableNode<Any>

    private val backgroundSprite = GameSprite()
    private val buttonSprites = OrderedMap<String, GameSprite>()
    private val bitsBars = Array<LevelPauseScreenBitsBar>()
    private val fontHandles = Array<MegaFontHandle>()

    private val slideTimer = Timer(SLIDE_DUR)
    private var closing = false

    private val fillHealthTimer = Timer()

    private val fadeout = Fade(FadeType.FADE_OUT, EXIT_DUR)
    private var exiting = false

    private var initialized = false

    override fun init() {
        if (initialized) return
        initialized = true

        val blackRegion = game.assMan.getTextureRegion(TextureAsset.COLORS.source, ConstKeys.BLACK)
        fadeout.setRegion(blackRegion)
        fadeout.setPosition(0f, 0f)
        fadeout.setWidth(ConstVals.VIEW_WIDTH * ConstVals.PPM)
        fadeout.setHeight(ConstVals.VIEW_HEIGHT * ConstVals.PPM)

        val levelPauseScreenAtlas = game.assMan.getTextureAtlas(TextureAsset.LEVEL_PAUSE_SCREEN_V2.source)

        val backgroundRegion = levelPauseScreenAtlas.findRegion(ConstKeys.BACKGROUND)
        backgroundSprite.setBounds(0f, 0f, ConstVals.VIEW_WIDTH * ConstVals.PPM, ConstVals.VIEW_HEIGHT * ConstVals.PPM)
        backgroundSprite.setRegion(backgroundRegion)

        buttonRegions.put(ConstKeys.EXIT, levelPauseScreenAtlas.findRegion("$OPTIONS_PREFIX/${ConstKeys.EXIT}"))
        buttonRegions.put(
            "${ConstKeys.EXIT}$SELECTED_SUFFIX",
            levelPauseScreenAtlas.findRegion("$OPTIONS_PREFIX/${ConstKeys.EXIT}$SELECTED_SUFFIX")
        )

        MegamanWeapon.entries.forEach { weapon ->
            val key = weapon.toString().lowercase()

            if (levelPauseScreenAtlas.containsRegion("$WEAPONS_PREFIX/$key")) {
                buttonRegions.put(key, levelPauseScreenAtlas.findRegion("$WEAPONS_PREFIX/$key"))
                buttonRegions.put(
                    "$key$SELECTED_SUFFIX", levelPauseScreenAtlas.findRegion("$WEAPONS_PREFIX/$key$SELECTED_SUFFIX")
                )
            }
        }

        MegaHealthTank.entries.forEach { healthTank ->
            val key = healthTank.toString().lowercase()

            if (levelPauseScreenAtlas.containsRegion("$HEALTH_TANKS_PREFIX/$key")) {
                buttonRegions.put(key, levelPauseScreenAtlas.findRegion("$HEALTH_TANKS_PREFIX/$key"))
                buttonRegions.put(
                    "$key$SELECTED_SUFFIX",
                    levelPauseScreenAtlas.findRegion("$HEALTH_TANKS_PREFIX/$key$SELECTED_SUFFIX")
                )
            }
        }

        fontHandles.addAll(
            MegaFontHandle(
                textSupplier = { "0${megaman.lives.current}" },
                positionX = LIVES_X * ConstVals.PPM,
                positionY = LIVES_Y * ConstVals.PPM,
                centerX = false,
                centerY = false
            ), MegaFontHandle(
                textSupplier = { state.getCurrency().toString().padStart(3, '0') },
                positionX = SCREWS_X * ConstVals.PPM,
                positionY = SCREWS_Y * ConstVals.PPM,
                centerX = false,
                centerY = false
            )
        )

        GameLogger.debug(TAG, "init(): buttonRegions=$buttonRegions")
    }

    override fun show() {
        if (!initialized) init()

        super.show()

        closing = false
        slideTimer.reset()

        exiting = false
        fadeout.reset()

        resetFillHealthTimer()

        val builder = TableBuilder<Any>()
        for (i in 0 until FULL_TABLE.rowCount()) {
            val row = Array<Any>()

            val columnCount = FULL_TABLE.columnCount(i)

            for (j in 0 until columnCount) {
                val element = FULL_TABLE.get(i, j).element

                when (element) {
                    is MegamanWeapon -> {
                        if (megaman.hasWeapon(element)) {
                            row.add(element)

                            val button = createWeaponButton(element)
                            buttons.put(element.toString().lowercase(), button)

                            val bitsBarX = WEAPON_BITS_COLUMN_1_X + j * WEAPON_BITS_COLUMN_OFFSET
                            val bitsBarY = WEAPON_BITS_ROWS_1_Y + (WEAPON_ROWS - i - 1) * WEAPON_BITS_ROW_OFFSET
                            val bitsSupplier: () -> Int = when (element) {
                                MegamanWeapon.MEGA_BUSTER -> {
                                    { megaman.getCurrentHealth() }
                                }

                                else -> {
                                    { megaman.weaponsHandler.getAmmo(element) }
                                }
                            }
                            val bitsBar = LevelPauseScreenBitsBar(
                                game.assMan, bitsBarX * ConstVals.PPM, bitsBarY * ConstVals.PPM, bitsSupplier
                            )
                            bitsBars.add(bitsBar)
                        }
                    }

                    is MegaHealthTank -> {
                        if (megaman.hasHealthTank(element)) {
                            row.add(element)

                            val button = createHealthTankButton(element)
                            buttons.put(element.toString().lowercase(), button)

                            val bitsBarX = HEALTH_TANK_BITS_COLUMN_1_X + j * HEALTH_TANK_BITS_COLUMN_OFFSET
                            val bitsBarY =
                                HEALTH_TANK_BITS_ROW_1_Y + (WEAPON_ROWS + HEALTH_TANK_ROWS - i - 1) * HEALTH_TANK_BITS_ROW_OFFSET
                            val bitsSupplier: () -> Int = { game.state.getHealthTankValue(element) }
                            val bitsBar = LevelPauseScreenBitsBar(
                                game.assMan, bitsBarX * ConstVals.PPM, bitsBarY * ConstVals.PPM, bitsSupplier
                            )
                            bitsBars.add(bitsBar)
                        }
                    }

                    ConstKeys.EXIT -> {
                        row.add(ConstKeys.EXIT)

                        val button = object : IMenuButton {

                            override fun onSelect(delta: Float): Boolean {
                                GameLogger.debug(TAG, "exit button: onSelect()")
                                game.audioMan.fadeOutMusic(EXIT_DUR)
                                game.audioMan.playSound(SoundAsset.SELECT_PING_SOUND, false)
                                exiting = true
                                return false
                            }

                            override fun onNavigate(direction: Direction, delta: Float): String? {
                                GameLogger.debug(TAG, "exit button: onNavigate(): direction=$direction")
                                navigate(direction)
                                return null
                            }
                        }

                        buttons.put(ConstKeys.EXIT, button)
                    }
                }
            }

            if (!row.isEmpty) builder.row(row)
        }

        table = builder.build()

        try {
            node = table.get(0, 0)
        } catch (e: Exception) {
            throw IllegalStateException("Failed to initialize node: builder=$builder, table=$table", e)
        }

        val exitButtonSprite = GameSprite(buttonRegions[ConstKeys.EXIT])
        exitButtonSprite.setBounds(0f, 0f, ConstVals.VIEW_WIDTH * ConstVals.PPM, ConstVals.VIEW_HEIGHT * ConstVals.PPM)
        buttonSprites.put(ConstKeys.EXIT, exitButtonSprite)

        MegamanWeapon.entries.forEach { weapon ->
            if (!megaman.hasWeapon(weapon)) return@forEach

            val key = weapon.toString().lowercase()
            try {
                val buttonSprite = GameSprite(buttonRegions[key])
                buttonSprite.setBounds(
                    0f, 0f, ConstVals.VIEW_WIDTH * ConstVals.PPM, ConstVals.VIEW_HEIGHT * ConstVals.PPM
                )
                buttonSprites.put(key, buttonSprite)
            } catch (e: Exception) {
                throw IllegalStateException("Failed to create button sprite for key=$key", e)
            }
        }

        MegaHealthTank.entries.forEach { healthTank ->
            if (!megaman.hasHealthTank(healthTank)) return@forEach

            val key = healthTank.toString().lowercase()
            try {
                val buttonSprite = GameSprite(buttonRegions[key])
                buttonSprite.setBounds(
                    0f, 0f, ConstVals.VIEW_WIDTH * ConstVals.PPM, ConstVals.VIEW_HEIGHT * ConstVals.PPM
                )
                buttonSprites.put(key, buttonSprite)
            } catch (e: Exception) {
                throw IllegalStateException("Failed to create button sprite for key=$key", e)
            }
        }

        GameLogger.debug(
            TAG,
            "show(): currentButtonKey=${getCurrentButtonKey()}, " + "node=$node, " + "table=$table, " + "buttons=${
                buttons.keys().toGdxArray()
            }, " + "buttonSprites=${buttonSprites.keys().toGdxArray()}"
        )
    }

    override fun isInteractionAllowed() =
        super.isInteractionAllowed() && slideTimer.isFinished() && fillHealthTimer.isFinished() && !closing && !exiting

    override fun render(delta: Float) {
        super.render(delta)

        if (exiting) {
            fadeout.update(delta)

            if (fadeout.isJustFinished()) game.runQueue.addLast {
                game.setCurrentScreen(ScreenEnum.SAVE_GAME_SCREEN.name)
            }

            return
        }

        fillHealthTimer.update(delta)
        if (fillHealthTimer.isJustFinished()) resetFillHealthTimer()

        if (!slideTimer.isFinished()) {
            slideTimer.update(delta)

            val start: Float
            val target: Float
            if (closing) {
                start = BACKGROUND_SPRITE_TARGET_POS_Y * ConstVals.PPM
                target = (BACKGROUND_SPRITE_TARGET_POS_Y - SLIDE_OFFSET_Y) * ConstVals.PPM
            } else {
                start = (BACKGROUND_SPRITE_TARGET_POS_Y - SLIDE_OFFSET_Y) * ConstVals.PPM
                target = BACKGROUND_SPRITE_TARGET_POS_Y * ConstVals.PPM
            }

            backgroundSprite.y = UtilMethods.interpolate(start, target, slideTimer.getRatio())

            if (closing && slideTimer.isFinished()) game.runQueue.addLast { game.resume() }
        }

        buttonSprites.forEach { entry ->
            val key = entry.key
            val buttonSprite = entry.value

            val buttonRegion = when {
                key == getCurrentButtonKey() -> {
                    val selectedKey = "$key$SELECTED_SUFFIX"
                    buttonRegions[selectedKey]
                }

                else -> buttonRegions[key]
            }
            buttonSprite.setRegion(buttonRegion)
        }
    }

    override fun draw(drawer: Batch) {
        game.viewports.get(ConstKeys.UI).apply()
        drawer.projectionMatrix = game.getUiCamera().combined

        val drawing = drawer.isDrawing

        if (!drawing) drawer.begin()

        backgroundSprite.draw(drawer)

        if (slideTimer.isFinished() && !closing) {
            buttonSprites.values().forEach { it.draw(drawer) }
            fontHandles.forEach { it.draw(drawer) }
            bitsBars.forEach { it.draw(drawer) }
        }

        if (exiting) fadeout.draw(drawer)

        if (!drawing) drawer.end()
    }

    override fun getCurrentButtonKey() = node.element.toString().lowercase()

    override fun setCurrentButtonKey(key: String?) =
        GameLogger.debug(TAG, "setCurrentButtonKey(): ignore setting button key: $key")

    override fun onAnySelection() {
        GameLogger.debug(TAG, "onAnySelection()")

        super.onAnySelection()

        if (!exiting) {
            closing = true
            slideTimer.reset()
        }
    }

    override fun reset() {
        GameLogger.debug(TAG, "reset()")

        super.reset()

        buttons.clear()
        buttonSprites.clear()

        bitsBars.clear()
    }

    private fun resetFillHealthTimer() = fillHealthTimer.clearRunnables().resetDuration(0f).setToEnd(false)

    private fun createWeaponButton(weapon: MegamanWeapon) = object : IMenuButton {

        override fun onSelect(delta: Float): Boolean {
            GameLogger.debug(TAG, "createWeaponButton(): onSelect(): weapon=$weapon")
            megaman.currentWeapon = weapon
            return true
        }

        override fun onNavigate(direction: Direction, delta: Float): String? {
            GameLogger.debug(TAG, "createWeaponButton(): onNavigate(): weapon=$weapon, direction=$direction")
            navigate(direction)
            return null
        }
    }

    private fun createHealthTankButton(healthTank: MegaHealthTank) = object : IMenuButton {

        override fun onSelect(delta: Float): Boolean {
            GameLogger.debug(TAG, "createHealthTankButton(): onSelect(): healthTank=$healthTank")

            if (megaman.hasMaxHealth() || state.getHealthTankValue(healthTank) <= 0) {
                audioMan.playSound(SoundAsset.ERROR_SOUND, false)
                return false
            }

            val healthToUse = min(
                megaman.getMaxHealth() - megaman.getCurrentHealth(), state.getHealthTankValue(healthTank)
            )

            val duration = healthToUse * ConstVals.DUR_PER_BIT
            fillHealthTimer.resetDuration(duration)

            for (i in 0 until healthToUse) {
                val time = i * ConstVals.DUR_PER_BIT

                fillHealthTimer.addRunnable(TimeMarkedRunnable(time) {
                    megaman.translateHealth(1)
                    state.removeHealthFromHealthTank(healthTank, 1)
                    game.audioMan.playSound(SoundAsset.ENERGY_FILL_SOUND, false)
                })
            }

            return false
        }

        override fun onNavigate(direction: Direction, delta: Float): String? {
            GameLogger.debug(
                TAG, "createHealthTankButton(): onNavigate(): healthTank=$healthTank, direction=$direction"
            )
            navigate(direction)
            return null
        }
    }

    private fun navigate(direction: Direction) {
        val oldNode = node
        try {
            node = when (direction) {
                Direction.UP -> node.nextRow()
                Direction.DOWN -> node.previousRow()
                Direction.LEFT -> node.previousColumn()
                Direction.RIGHT -> node.nextColumn()
            }
        } catch (e: Exception) {
            throw IllegalStateException(
                "Failed to navigate to next node: direction=$direction, currentNode=$node, $table=$table", e
            )
        }
        GameLogger.debug(TAG, "navigate(): direction=$direction, oldNode=$oldNode, node=$node")
    }
}

private class LevelPauseScreenBitsBar(
    private val assMan: AssetManager, private val x: Float, private val y: Float, private val countSupplier: () -> Int
) : Initializable, IDrawable<Batch> {

    private val bitSprites = Array<GameSprite>()
    private var initialized = false

    override fun init() {
        if (initialized) return
        initialized = true

        for (i in 0 until MegamanValues.MAX_WEAPON_AMMO) {
            val bit = GameSprite(
                assMan.getTextureRegion(
                    TextureAsset.BITS.source, "${ConstKeys.STANDARD}_${ConstKeys.ROTATED}"
                )
            )

            bit.setSize(ConstVals.STAT_BIT_HEIGHT * ConstVals.PPM, ConstVals.STAT_BIT_WIDTH * ConstVals.PPM)
            bit.setPosition(x + i * ConstVals.STAT_BIT_HEIGHT * ConstVals.PPM, y)

            bitSprites.add(bit)
        }
    }

    override fun draw(drawer: Batch) {
        if (!initialized) init()

        val bitCount = countSupplier()
        for (i in 0 until bitCount) bitSprites.get(i).draw(drawer)
    }
}

