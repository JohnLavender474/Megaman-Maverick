package com.megaman.maverick.game.screens.menus.level

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.IDrawable
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.screens.menus.IMenuButton
import com.megaman.maverick.game.ConstFuncs
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.drawables.fonts.MegaFontHandle
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.levels.LevelDefMap
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.levels.LevelType
import com.megaman.maverick.game.screens.ScreenEnum
import com.megaman.maverick.game.screens.menus.MegaMenuScreen
import com.megaman.maverick.game.screens.utils.BlinkingArrow
import com.megaman.maverick.game.utils.interfaces.IShapeDebuggable

class LevelSelectScreen(game: MegamanMaverickGame) : MegaMenuScreen(game, Position.CENTER.name), Initializable,
    IShapeDebuggable {

    companion object {
        const val TAG = "LevelSelectScreen"

        private const val DEBUG_TEXT_BOUNDS = false

        private const val PRESS_START = "PRESS START"

        private const val DR_WILY = "DR. WILY"
        private const val MEGA_MAN = "MEGA MAN"

        private const val BOSS_NAME_TEXT_X = 2f
        private const val BOSS_NAME_TEXT_Y = 1f

        private const val BACK_BUTTON_KEY = "BACK"
        private const val BACK_BUTTON_X = ConstVals.VIEW_WIDTH - 3f
        private const val BACK_BUTTON_Y = 1f

        private const val BACK_ARROW_X = ConstVals.VIEW_WIDTH - 4.5f
        private const val BACK_ARROW_Y = 0.55f

        private const val SELECTOR_BLINK_DUR = 0.1f

        private val MUGSHOT_X_COORDS = gdxArrayOf(40f, 112f, 184f)
        private val MUGSHOT_Y_COORDS = gdxArrayOf(45f, 109f, 173f)
        private const val MUGSHOT_WIDTH = 31f
        private const val MUGSHOT_HEIGHT = 31f

        private const val MOON_PIECES_REGION_KEY = "extras/MoonPieces"
        private const val MOON_PIECES_WIDTH = MUGSHOT_WIDTH * 1.5f
        private const val MOON_PIECES_HEIGHT = MUGSHOT_HEIGHT * 1.5f

        private const val OUTRO_DUR = 1f

        private val regions = ObjectMap<String, TextureRegion>()
    }

    private val levelDefGrid = ObjectMap<Position, LevelDefinition>()
    private val mugshotGrid = ObjectMap<Position, Mugshot>()

    private val outroTimer = Timer(OUTRO_DUR)
    private var outro = false

    private val background = GameSprite()
    private val backgroundAnims = ObjectMap<String, Animation>()
    private val currentBkgAnim: Animation
        get() = if (selectionMade) backgroundAnims[ConstKeys.SELECTED] else backgroundAnims[ConstKeys.STATIC]

    private val selector = GameSprite()
    private val selectorRegions = ObjectMap<String, TextureRegion>()
    private val selectorBlinkTimer = Timer(SELECTOR_BLINK_DUR)
    private val shouldDrawSelector: Boolean
        get() = buttonKey != null && buttonKey != BACK_BUTTON_KEY && !selectionMade

    private val text = Array<MegaFontHandle>()
    private val blinkingArrows = OrderedMap<String, BlinkingArrow>()
    private val foregroundSprites = OrderedMap<GameSprite, Updatable?>()

    private var selectedLevelDef: LevelDefinition? = null
    private var initialized = false

    private val out = Vector2()

    override fun init() {
        if (initialized) {
            GameLogger.debug(TAG, "init(): already initialized, nothing to do")
            return
        }

        val blackRegion = game.assMan.getTextureRegion(TextureAsset.COLORS.source, ConstKeys.BLACK)
        regions.put(ConstKeys.BLACK, blackRegion)

        val levelSelectScreenAtlas = game.assMan.getTextureAtlas(TextureAsset.LEVEL_SELECT_SCREEN.source)

        val staticBkgRegion =
            levelSelectScreenAtlas.findRegion("${ConstKeys.BACKGROUND}_${ConstKeys.STATIC}")
        val staticAnim = Animation(staticBkgRegion)
        backgroundAnims.put(ConstKeys.STATIC, staticAnim)

        val selectedBackgroundRegion =
            levelSelectScreenAtlas.findRegion("${ConstKeys.BACKGROUND}_${ConstKeys.SELECTED}")
        val selectedAnim = Animation(selectedBackgroundRegion, 2, 1, 0.1f, true)
        backgroundAnims.put(ConstKeys.SELECTED, selectedAnim)

        Position.entries.forEach { position ->
            val key = position.name

            val selectorRegion = levelSelectScreenAtlas.findRegion("${ConstKeys.SELECTOR}/${key.lowercase()}")
            selectorRegions.put(key, selectorRegion)
        }

        val pressStart = MegaFontHandle(
            text = PRESS_START,
            positionX = ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f,
            positionY = (ConstVals.VIEW_HEIGHT + 0.3f) * ConstVals.PPM,
            attachment = Position.TOP_CENTER
        )
        text.add(pressStart)

        val back = MegaFontHandle(
            text = BACK_BUTTON_KEY,
            positionX = BACK_BUTTON_X * ConstVals.PPM,
            positionY = BACK_BUTTON_Y * ConstVals.PPM
        )
        text.add(back)

        val bossName = MegaFontHandle(
            textSupplier = {
                val key = buttonKey
                when (key) {
                    BACK_BUTTON_KEY, null -> PRESS_START
                    Position.CENTER.name -> if (game.state.allRobotMasterLevelsDefeated()) DR_WILY else MEGA_MAN
                    else -> {
                        val position = Position.valueOf(key)
                        levelDefGrid[position].getFormattedName()
                    }
                }
            },
            positionX = BOSS_NAME_TEXT_X * ConstVals.PPM,
            positionY = BOSS_NAME_TEXT_Y * ConstVals.PPM,
            centerX = false
        )
        text.add(bossName)

        val backArrow = BlinkingArrow(game.assMan, Vector2(BACK_ARROW_X * ConstVals.PPM, BACK_ARROW_Y * ConstVals.PPM))
        blinkingArrows.put(BACK_BUTTON_KEY, backArrow)

        buttons.put(BACK_BUTTON_KEY, object : IMenuButton {
            override fun onSelect(delta: Float): Boolean {
                game.setCurrentScreen(ScreenEnum.SAVE_GAME_SCREEN.name)
                return true
            }

            override fun onNavigate(direction: Direction, delta: Float) = when (direction) {
                Direction.UP, Direction.LEFT, Direction.RIGHT -> Position.get(2, 0).name
                Direction.DOWN -> Position.get(2, 2).name
            }
        })

        val levelDefIter = LevelDefMap.getDefsOfLevelType(LevelType.ROBOT_MASTER_LEVEL).iterator()
        val posIter = Position.entries.iterator()
        while (posIter.hasNext()) {
            if (!levelDefIter.hasNext()) throw IllegalStateException("Not enough levels defined to fill eight mugshots")

            val pos = posIter.next()
            when (pos) {
                Position.CENTER -> putCenterMugshot()
                else -> {
                    val levelDef = levelDefIter.next()
                    levelDefGrid.put(pos, levelDef)
                    putBossMugshot(pos, levelDef)
                }
            }
        }

        background.setBounds(
            0f,
            0f,
            ConstVals.VIEW_WIDTH * ConstVals.PPM,
            ConstVals.VIEW_HEIGHT * ConstVals.PPM
        )

        selector.setBounds(
            0f,
            0f,
            ConstVals.VIEW_WIDTH * ConstVals.PPM,
            ConstVals.VIEW_HEIGHT * ConstVals.PPM
        )
    }

    private fun getMugshotPosition(position: Position, out: Vector2): Vector2 {
        val x = MUGSHOT_X_COORDS[position.x] * 2f
        val y = MUGSHOT_Y_COORDS[position.y] * 2f
        return out.set(x, y)
    }

    private fun putCenterMugshot() {
        val atlas = game.assMan.getTextureAtlas(TextureAsset.FACES_1.source)

        val megamanFaces = ObjectMap<Position, TextureRegion>()
        Position.entries.forEach { pos ->
            val region = atlas.findRegion("${Megaman.TAG}/${pos.name.lowercase()}")
            megamanFaces.put(pos, region)
        }

        val wilyFace = atlas.findRegion("Wily")

        val faceSupplier: () -> TextureRegion = faceSupplier@{
            if (game.state.allRobotMasterLevelsDefeated()) return@faceSupplier wilyFace

            val position = Position.entries.find { it.name == buttonKey } ?: Position.CENTER
            val megamanFace = megamanFaces[position]
            return@faceSupplier megamanFace
        }

        val pos = getMugshotPosition(Position.CENTER, out)
        val mugshot = Mugshot(
            game = game,
            positionX = pos.x,
            positionY = pos.y,
            faceWidth = MUGSHOT_WIDTH * 2f,
            faceHeight = MUGSHOT_HEIGHT * 2f,
            faceSupplier = faceSupplier
        )
        mugshotGrid.put(Position.CENTER, mugshot)

        buttons.put(Position.CENTER.name, object : IMenuButton {

            override fun onSelect(delta: Float): Boolean {
                if (game.state.allRobotMasterLevelsDefeated()) {
                    val wilyStage = game.state.getNextWilyStage()

                    if (wilyStage == null) {
                        GameLogger.error(TAG, "Next Wily stage is null")
                        return false
                    }

                    select(wilyStage)

                    return true
                }

                return false
            }

            override fun onNavigate(direction: Direction, delta: Float) = Position.CENTER.move(direction).name
        })
    }

    private fun navigate(position: Position, direction: Direction): String {
        var x = position.x
        var y = position.y

        when (direction) {
            Direction.UP -> y += 1
            Direction.DOWN -> y -= 1
            Direction.LEFT -> x -= 1
            Direction.RIGHT -> x += 1
        }
        if (y < 0 || y > 2) return BACK_BUTTON_KEY

        if (x < 0) x = 2
        if (x > 2) x = 0

        return Position.get(x, y).name
    }

    private fun select(levelDef: LevelDefinition) {
        game.audioMan.playSound(SoundAsset.BEAM_OUT_SOUND, false)
        game.audioMan.stopMusic(null)

        selectedLevelDef = levelDef

        outro = true
    }

    private fun putBossMugshot(position: Position, levelDef: LevelDefinition) {
        val mugshotRegion = game.assMan.getTextureRegion(
            TextureAsset.valueOf(levelDef.mugshotAtlas!!.uppercase()).source,
            levelDef.mugshotRegion!!
        )
        val faceSupplier: () -> TextureRegion = {
            when {
                game.state.isLevelDefeated(levelDef) -> regions[ConstKeys.BLACK]
                else -> mugshotRegion
            }
        }

        val pos = getMugshotPosition(position, out)
        val mugshot = Mugshot(
            game = game,
            positionX = pos.x,
            positionY = pos.y,
            faceWidth = MUGSHOT_WIDTH * 2f,
            faceHeight = MUGSHOT_HEIGHT * 2f,
            faceSupplier = faceSupplier
        )
        mugshotGrid.put(position, mugshot)

        buttons.put(position.name, object : IMenuButton {

            override fun onSelect(delta: Float): Boolean {
                select(levelDef)
                return true
            }

            override fun onNavigate(direction: Direction, delta: Float) = navigate(position, direction)
        })

        if (levelDef == LevelDefinition.MOON_MAN) {
            GameLogger.debug(TAG, "putBossMugshot(): put moon pieces for Moon Man")

            val region = game.assMan.getTextureRegion(TextureAsset.FACES_1.source, MOON_PIECES_REGION_KEY)

            val moonPieces = GameSprite(region)
            foregroundSprites.put(moonPieces) {
                val x = mugshot.faceSprite.x
                val y = mugshot.faceSprite.y
                val width = MOON_PIECES_WIDTH * 2f
                val height = MOON_PIECES_HEIGHT * 2f

                moonPieces.setPosition(x, y)
                moonPieces.setSize(width, height)

                moonPieces.hidden = game.state.isLevelDefeated(levelDef)
            }
        }
    }

    override fun show() {
        if (!initialized) {
            init()
            initialized = true
        }

        super.show()

        outro = false
        outroTimer.reset()

        selectorBlinkTimer.reset()

        game.getUiCamera().position.set(ConstFuncs.getUiCamInitPos())

        val musicAsset = when {
            game.state.allRobotMasterLevelsDefeated() -> MusicAsset.MMX5_STAGE_SELECT_2_MUSIC
            else -> MusicAsset.MMX5_STAGE_SELECT_1_MUSIC
        }
        game.audioMan.playMusic(musicAsset, true)
    }

    override fun onAnyMovement(direction: Direction) =
        game.audioMan.playSound(SoundAsset.CURSOR_MOVE_BLOOP_SOUND, false)

    override fun render(delta: Float) {
        super.render(delta)

        if (!game.paused) {
            if (outro) outroTimer.update(delta)
            if (outroTimer.isJustFinished()) {
                game.setCurrentLevel(selectedLevelDef!!)
                game.setCurrentScreen(ScreenEnum.ROBOT_MASTER_INTRO_SCREEN.name)
            }
            if (outroTimer.isFinished()) return

            currentBkgAnim.update(delta)
            background.setRegion(currentBkgAnim.getCurrentRegion())

            if (shouldDrawSelector) {
                val selectorRegion = selectorRegions[buttonKey]
                selector.setRegion(selectorRegion)

                selectorBlinkTimer.update(delta)
                if (selectorBlinkTimer.isFinished()) {
                    selector.hidden = !selector.hidden
                    selectorBlinkTimer.reset()
                }
            }

            blinkingArrows.get(buttonKey)?.update(delta)
            foregroundSprites.values().forEach { it?.update(delta) }
        }
    }

    override fun draw(drawer: Batch) {
        game.viewports.get(ConstKeys.UI).apply()
        drawer.projectionMatrix = game.getUiCamera().combined
        drawer.begin()

        background.draw(drawer)
        mugshotGrid.forEach { it.value.draw(drawer) }
        text.forEach { it.draw(drawer) }
        blinkingArrows.get(buttonKey)?.draw(drawer)
        foregroundSprites.keys().forEach { it.draw(drawer) }
        if (shouldDrawSelector) selector.draw(drawer)

        drawer.end()
    }

    override fun draw(renderer: ShapeRenderer) {
        if (DEBUG_TEXT_BOUNDS) {
            renderer.projectionMatrix = game.getUiCamera().combined
            text.forEach { it.draw(renderer) }
        }
    }
}

internal class Mugshot(
    private val game: MegamanMaverickGame,
    private val positionX: Float,
    private val positionY: Float,
    private val faceWidth: Float,
    private val faceHeight: Float,
    private val faceSupplier: () -> TextureRegion
) : Initializable, IDrawable<Batch> {

    companion object {
        const val TAG = "MugshotPane"

        private const val KEY_SUFFIX = "_50x50" // 48x48
        private const val NONE_REGION_KEY = "none$KEY_SUFFIX"
        private const val BLINKING_REGION_KEY = "blinking$KEY_SUFFIX"
        private const val HIGHLIGHTED_REGION_KEY = "highlighted$KEY_SUFFIX"

        private val regions = ObjectMap<String, TextureRegion>()
    }

    val faceSprite = GameSprite()
    var initialized = false

    override fun init() {
        if (initialized) {
            GameLogger.debug(TAG, "init(): already initialized, nothing to do")
            return
        }

        if (regions.isEmpty) {
            val uiAtlas = game.assMan.getTextureAtlas(TextureAsset.UI_1.source)
            gdxArrayOf(NONE_REGION_KEY, BLINKING_REGION_KEY, HIGHLIGHTED_REGION_KEY).forEach {
                regions.put(it, uiAtlas.findRegion("${TAG}/$it"))
            }
        }


        faceSprite.setSize(faceWidth, faceHeight)
        faceSprite.x = positionX
        faceSprite.y = positionY
    }

    private fun drawFace(drawer: Batch) {
        val face = faceSupplier.invoke()
        faceSprite.setRegion(face)
        faceSprite.draw(drawer)
    }

    override fun draw(drawer: Batch) {
        if (!initialized) {
            init()
            initialized = true
        }

        drawFace(drawer)
    }
}
