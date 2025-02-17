package com.megaman.maverick.game.screens.menus.level

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.toGdxArray
import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.common.objects.table.Table
import com.mega.game.engine.common.objects.table.TableBuilder
import com.mega.game.engine.common.objects.table.TableNode
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.containsRegion
import com.mega.game.engine.screens.menus.IMenuButton
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.drawables.fonts.MegaFontHandle
import com.megaman.maverick.game.entities.megaman.constants.MegaHealthTank
import com.megaman.maverick.game.entities.megaman.constants.MegamanWeapon
import com.megaman.maverick.game.screens.menus.MegaMenuScreen

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
            .row(gdxArrayOf(MegaHealthTank.A, MegaHealthTank.B))
            .row(gdxArrayOf(MegaHealthTank.C, MegaHealthTank.D))
            .build()

        private const val WEAPONS_PREFIX = "weapons"
        private const val HEALTH_TANKS_PREFIX = "health_tanks"
        private const val SELECTED_SUFFIX = "_selected"

        private const val LIVES_X = 13.5f
        private const val LIVES_Y = 2.75f

        private const val SCREWS_X = 13.5f
        private const val SCREWS_Y = 1.75f

        private const val BACKGROUND_SPRITE_TARGET_POS_Y = 0f

        private const val SLIDE_OFFSET_Y = ConstVals.VIEW_HEIGHT
        private const val SLIDE_DUR = 0.5f

        private val buttonRegions = OrderedMap<String, TextureRegion>()
    }

    private lateinit var table: Table<Any>
    private lateinit var node: TableNode<Any>

    private val backgroundSprite = GameSprite()
    private val buttonSprites = OrderedMap<String, GameSprite>()

    private val fontHandles = Array<MegaFontHandle>()

    private var initialized = false

    private var exiting = false

    private val slideTimer = Timer(SLIDE_DUR)

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
                textSupplier = { "0${game.megaman.lives.current}" },
                positionX = LIVES_X * ConstVals.PPM,
                positionY = LIVES_Y * ConstVals.PPM,
                centerX = false,
                centerY = false
            ),
            MegaFontHandle(
                textSupplier = { game.state.getCurrency().toString().padStart(3, '0') },
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

        val builder = TableBuilder<Any>()
        for (i in 0 until WEAPONS_TABLE.rowCount()) {
            val row = Array<Any>()

            for (j in 0 until WEAPONS_TABLE.columnCount(i)) {
                val element = WEAPONS_TABLE.get(i, j).element

                if (game.megaman.hasWeapon(element)) {
                    row.add(element)

                    val button = createWeaponButton(element)
                    buttons.put(element.toString().lowercase(), button)
                }
            }

            if (!row.isEmpty) builder.row(row)
        }
        for (i in 0 until HEALTH_TANKS_TABLE.rowCount()) {
            val row = Array<Any>()

            for (j in 0 until HEALTH_TANKS_TABLE.columnCount(i)) {
                val element = HEALTH_TANKS_TABLE.get(i, j).element

                if (game.megaman.hasHealthTank(element)) {
                    row.add(element)

                    val button = createHealthTankButton(element)
                    buttons.put(element.toString().lowercase(), button)
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
            if (!game.megaman.hasWeapon(weapon)) return@forEach

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
            if (!game.megaman.hasHealthTank(healthTank)) return@forEach

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

    override fun isInteractionAllowed() = super.isInteractionAllowed() && slideTimer.isFinished() && !exiting

    override fun render(delta: Float) {
        super.render(delta)

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
        }

        if (!drawing) drawer.end()
    }

    private fun createWeaponButton(weapon: MegamanWeapon) = object : IMenuButton {

        override fun onSelect(delta: Float): Boolean {
            GameLogger.debug(TAG, "createWeaponButton(): onSelect(): weapon=$weapon")
            game.megaman.currentWeapon = weapon
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
            // TODO: use health tank
            return true
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
    }
}
