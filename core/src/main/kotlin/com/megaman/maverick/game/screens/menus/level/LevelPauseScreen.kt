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
import com.megaman.maverick.game.screens.menus.MegaMenuScreen
import com.megaman.maverick.game.state.GameState
import kotlin.math.min

class LevelPauseScreen(game: MegamanMaverickGame) : MegaMenuScreen(game), Initializable {

    companion object {
        const val TAG = "LevelPauseScreen"

        private val WEAPONS_TABLE = TableBuilder<MegamanWeapon>()
            .row(gdxArrayOf(MegamanWeapon.MEGA_BUSTER))
            .row(gdxArrayOf(MegamanWeapon.MOON_SCYTHE))
            .row(gdxArrayOf(MegamanWeapon.FIRE_BALL))
            .row(gdxArrayOf(MegamanWeapon.RUSH_JETPACK))
            .build()

        private val HEALTH_TANKS_TABLE = TableBuilder<MegaHealthTank>()
            .row(gdxArrayOf(MegaHealthTank.A, MegaHealthTank.C))
            .row(gdxArrayOf(MegaHealthTank.B, MegaHealthTank.D))
            .build()

        private const val WEAPONS_PREFIX = "weapons"
        private const val HEALTH_TANKS_PREFIX = "health_tanks"
        private const val SELECTED_SUFFIX = "_selected"

        private const val LIVES_X = 13.5f
        private const val LIVES_Y = 2f

        private const val SCREWS_X = 13.5f
        private const val SCREWS_Y = 0.875f

        private const val BACKGROUND_SPRITE_TARGET_POS_Y = 0f

        private const val SLIDE_OFFSET_Y = ConstVals.VIEW_HEIGHT
        private const val SLIDE_DUR = 0.5f

        private const val WEAPON_BITS_COLUMN_1_X = 3.5f
        private const val WEAPON_BITS_ROWS_1_Y = 7.75f
        private const val WEAPON_BITS_ROW_OFFSET = 1f
        private const val WEAPON_BITS_COLUMN_OFFSET = 7f

        private const val HEALTH_TANK_BITS_COLUMN_1_X = 2.25f
        private const val HEALTH_TANK_BITS_ROW_1_Y = 0.25f
        private const val HEALTH_TANK_BITS_ROW_OFFSET = 1.375f
        private const val HEALTH_TANK_BITS_COLUMN_OFFSET = 4.625f

        private val buttonRegions = OrderedMap<String, TextureRegion>()
    }

    private val megaman: Megaman
        get() = game.megaman
    val state: GameState
        get() = game.state
    val audioMan: MegaAudioManager
        get() = game.audioMan

    private lateinit var table: Table<Any>
    private lateinit var node: TableNode<Any>

    private val backgroundSprite = GameSprite()
    private val buttonSprites = OrderedMap<String, GameSprite>()
    private val bitsBars = Array<LevelPauseScreenBitsBar>()
    private val fontHandles = Array<MegaFontHandle>()

    private val slideTimer = Timer(SLIDE_DUR)
    private var exiting = false

    private val fillHealthTimer = Timer()

    private var initialized = false

    override fun init() {
        if (initialized) return
        initialized = true

        val atlas = game.assMan.getTextureAtlas(TextureAsset.LEVEL_PAUSE_SCREEN.source)

        val backgroundRegion = atlas.findRegion(ConstKeys.BACKGROUND)
        backgroundSprite.setBounds(0f, 0f, ConstVals.VIEW_WIDTH * ConstVals.PPM, ConstVals.VIEW_HEIGHT * ConstVals.PPM)
        backgroundSprite.setRegion(backgroundRegion)

        MegamanWeapon.entries.forEach { weapon ->
            val key = weapon.toString().lowercase()

            if (atlas.containsRegion("$WEAPONS_PREFIX/$key")) {
                buttonRegions.put(key, atlas.findRegion("$WEAPONS_PREFIX/$key"))
                buttonRegions.put("$key$SELECTED_SUFFIX", atlas.findRegion("$WEAPONS_PREFIX/$key$SELECTED_SUFFIX"))
            }
        }

        MegaHealthTank.entries.forEach { healthTank ->
            val key = healthTank.toString().lowercase()

            if (atlas.containsRegion("$HEALTH_TANKS_PREFIX/$key")) {
                buttonRegions.put(key, atlas.findRegion("$HEALTH_TANKS_PREFIX/$key"))
                buttonRegions.put("$key$SELECTED_SUFFIX", atlas.findRegion("$HEALTH_TANKS_PREFIX/$key$SELECTED_SUFFIX"))
            }
        }

        fontHandles.addAll(
            MegaFontHandle(
                textSupplier = { "0${megaman.lives.current}" },
                positionX = LIVES_X * ConstVals.PPM,
                positionY = LIVES_Y * ConstVals.PPM,
                centerX = false,
                centerY = false
            ),
            MegaFontHandle(
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

        exiting = false
        slideTimer.reset()

        resetFillHealthTimer()

        val builder = TableBuilder<Any>()
        for (i in 0 until WEAPONS_TABLE.rowCount()) {
            val row = Array<Any>()

            val columnCount = WEAPONS_TABLE.columnCount(i)
            for (j in 0 until columnCount) {
                val weapon = WEAPONS_TABLE.get(i, j).element

                if (megaman.hasWeapon(weapon)) {
                    row.add(weapon)

                    val button = createWeaponButton(weapon)
                    buttons.put(weapon.toString().lowercase(), button)

                    val bitsSupplier: () -> Int = when (weapon) {
                        MegamanWeapon.MEGA_BUSTER -> {
                            { megaman.getCurrentHealth() }
                        }

                        else -> {
                            { megaman.weaponsHandler.getAmmo(weapon) }
                        }
                    }

                    val bitsBarX = (WEAPON_BITS_COLUMN_1_X + j * WEAPON_BITS_COLUMN_OFFSET) * ConstVals.PPM
                    val bitsBarY =
                        (WEAPON_BITS_ROWS_1_Y + (WEAPONS_TABLE.rowCount() - i - 1) * WEAPON_BITS_ROW_OFFSET) * ConstVals.PPM

                    val bitsBar = LevelPauseScreenBitsBar(game.assMan, bitsBarX, bitsBarY, bitsSupplier)

                    bitsBars.add(bitsBar)
                }
            }

            if (!row.isEmpty) builder.row(row)
        }
        for (i in 0 until HEALTH_TANKS_TABLE.rowCount()) {
            val row = Array<Any>()

            for (j in 0 until HEALTH_TANKS_TABLE.columnCount(i)) {
                val healthTank = HEALTH_TANKS_TABLE.get(i, j).element

                if (megaman.hasHealthTank(healthTank)) {
                    row.add(healthTank)

                    val button = createHealthTankButton(healthTank)
                    buttons.put(healthTank.toString().lowercase(), button)

                    val bitsSupplier: () -> Int = { game.state.getHealthTankValue(healthTank) }

                    val bitsBarX =
                        (HEALTH_TANK_BITS_COLUMN_1_X + j * HEALTH_TANK_BITS_COLUMN_OFFSET) * ConstVals.PPM
                    val bitsBarY =
                        (HEALTH_TANK_BITS_ROW_1_Y + (HEALTH_TANKS_TABLE.rowCount() - i - 1) * HEALTH_TANK_BITS_ROW_OFFSET) * ConstVals.PPM

                    val bitsBar = LevelPauseScreenBitsBar(game.assMan, bitsBarX, bitsBarY, bitsSupplier)

                    bitsBars.add(bitsBar)
                }
            }

            if (!row.isEmpty) builder.row(row)
        }

        table = builder.build()

        try {
            node = table.get(0, 0)
        } catch (e: Exception) {
            throw IllegalStateException(
                "Failed to initialize node: " +
                    "builder=$builder, " +
                    "table=$table, " +
                    "WEAPONS_TABLE=$WEAPONS_TABLE, " +
                    "HEALTH_TANKS_TABLE=$HEALTH_TANKS_TABLE", e
            )
        }

        MegamanWeapon.entries.forEach { weapon ->
            if (!megaman.hasWeapon(weapon)) return@forEach

            val key = weapon.toString().lowercase()
            try {
                val buttonSprite = GameSprite(buttonRegions[key])
                buttonSprite.setBounds(
                    0f,
                    0f,
                    ConstVals.VIEW_WIDTH * ConstVals.PPM,
                    ConstVals.VIEW_HEIGHT * ConstVals.PPM
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
                    0f,
                    0f,
                    ConstVals.VIEW_WIDTH * ConstVals.PPM,
                    ConstVals.VIEW_HEIGHT * ConstVals.PPM
                )
                buttonSprites.put(key, buttonSprite)
            } catch (e: Exception) {
                throw IllegalStateException("Failed to create button sprite for key=$key", e)
            }
        }

        GameLogger.debug(
            TAG,
            "show(): currentButtonKey=${getCurrentButtonKey()}, " +
                "node=$node, " +
                "table=$table, " +
                "buttons=${buttons.keys().toGdxArray()}, " +
                "buttonSprites=${buttonSprites.keys().toGdxArray()}"
        )
    }

    private fun resetFillHealthTimer() = fillHealthTimer.clearRunnables().resetDuration(0f).setToEnd(false)

    override fun isInteractionAllowed() =
        super.isInteractionAllowed() && slideTimer.isFinished() && fillHealthTimer.isFinished() && !exiting

    override fun render(delta: Float) {
        super.render(delta)

        fillHealthTimer.update(delta)
        if (fillHealthTimer.isJustFinished()) resetFillHealthTimer()

        if (!slideTimer.isFinished()) {
            slideTimer.update(delta)

            val start: Float
            val target: Float
            if (exiting) {
                start = BACKGROUND_SPRITE_TARGET_POS_Y * ConstVals.PPM
                target = (BACKGROUND_SPRITE_TARGET_POS_Y - SLIDE_OFFSET_Y) * ConstVals.PPM
            } else {
                start = (BACKGROUND_SPRITE_TARGET_POS_Y - SLIDE_OFFSET_Y) * ConstVals.PPM
                target = BACKGROUND_SPRITE_TARGET_POS_Y * ConstVals.PPM
            }

            backgroundSprite.y = UtilMethods.interpolate(start, target, slideTimer.getRatio())

            if (exiting && slideTimer.isJustFinished()) game.runQueue.addLast { game.resume() }
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

        if (slideTimer.isFinished() && !exiting) {
            buttonSprites.values().forEach { it.draw(drawer) }
            fontHandles.forEach { it.draw(drawer) }
            bitsBars.forEach { it.draw(drawer) }
        }

        if (!drawing) drawer.end()
    }

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
                megaman.getMaxHealth() - megaman.getCurrentHealth(),
                state.getHealthTankValue(healthTank)
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
                TAG,
                "createHealthTankButton(): onNavigate(): healthTank=$healthTank, direction=$direction"
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

    override fun getCurrentButtonKey() = node.element.toString().lowercase()

    override fun setCurrentButtonKey(key: String?) =
        GameLogger.debug(TAG, "setCurrentButtonKey(): ignore setting button key: $key")

    override fun onAnySelection() {
        GameLogger.debug(TAG, "onAnySelection()")
        super.onAnySelection()
        exiting = true
        slideTimer.reset()
    }

    override fun reset() {
        GameLogger.debug(TAG, "reset()")
        super.reset()
        buttons.clear()
        buttonSprites.clear()
        bitsBars.clear()
    }
}

private class LevelPauseScreenBitsBar(
    private val assMan: AssetManager,
    private val x: Float,
    private val y: Float,
    private val countSupplier: () -> Int
) : Initializable, IDrawable<Batch> {

    private val bitSprites = Array<GameSprite>()
    private var initialized = false

    override fun init() {
        if (initialized) return
        initialized = true

        for (i in 0 until MegamanValues.MAX_WEAPON_AMMO) {
            val bit = GameSprite(assMan.getTextureRegion(TextureAsset.UI_1.source, "BitRotated"))

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

