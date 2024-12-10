package com.megaman.maverick.game.screens.menus.bosses

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.time.TimeMarkedRunnable
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
import com.megaman.maverick.game.levels.LevelDefMap
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.levels.LevelType
import com.megaman.maverick.game.screens.ScreenEnum
import com.megaman.maverick.game.screens.menus.MegaMenuScreen
import com.megaman.maverick.game.screens.menus.bosses.MugshotPane.MugshotPaneState
import com.megaman.maverick.game.screens.utils.BlinkingArrow

class LevelSelectScreen(game: MegamanMaverickGame) : MegaMenuScreen(game, Position.CENTER.name), Initializable {

    companion object {
        const val TAG = "LevelSelectScreen"

        private const val DEBUG_TEXT_BOUNDS = false

        private const val PRESS_START = "PRESS START"
        private const val MEGA_MAN = "MEGA MAN"

        private const val SIDE_BAR_COLOR_KEY = ConstKeys.BLACK

        private const val BOSS_NAME_TEXT_X = 2f
        private const val BOSS_NAME_TEXT_Y = 1f

        private const val BACK_BUTTON_KEY = "BACK"
        private const val BACK_BUTTON_X = ConstVals.VIEW_WIDTH - 3f
        private const val BACK_BUTTON_Y = 1f

        private const val BACK_ARROW_X = ConstVals.VIEW_WIDTH - 4.5f
        private const val BACK_ARROW_Y = 0.55f

        private const val BOTTOM_BLACK_BAR_WIDTH = ConstVals.VIEW_WIDTH
        private const val BOTTOM_BLACK_BAR_HEIGHT = 1f
        private const val SIDE_BLACK_BAR_WIDTH = 2f
        private const val SIDE_BLACK_BAR_HEIGHT = ConstVals.VIEW_HEIGHT

        // the offsets of the panes as a collective from the left x and bottom y
        private const val PANES_X_OFFSET = 2f
        private const val PANES_Y_OFFSET = 1f

        private const val PANE_WIDTH = 3f // 5f
        private const val PANE_HEIGHT = 3f
        private const val PANE_X_PADDING = 1.5f
        private const val PANE_Y_PADDING = 0.5f // 1f

        private const val BARS_ROWS = 3
        private const val BARS_COLUMNS = 6
        private const val BARS_OFFSET_Y = 0.5f

        private const val BAR_WIDTH = 5.33f
        private const val BAR_HEIGHT = 4f
        private const val BAR_PADDING_X = 3f
        private const val BAR_PADDING_Y = 3.5f

        private const val MOON_MAN_WIDTH = 4f
        private const val MOON_MAN_HEIGHT = 4f
        private const val MOON_MAN_OFFSET_Y = -0.5f

        private const val OUTRO_DUR = 1f
        private const val OUTRO_BLINKS = 10

        private val regions = ObjectMap<String, TextureRegion>()
        private var unknownRegion: TextureRegion? = null
    }

    private val levelDefGrid = ObjectMap<Position, LevelDefinition>()
    private val mugshotGrid = ObjectMap<Position, MugshotPane>()

    private val outroTimer = Timer(OUTRO_DUR)
    private var outroBlink = false
    private var outro = false

    private val background = GameSprite()
    private val text = Array<MegaFontHandle>()
    private val blocksBackground = Array<GameSprite>()
    private val barsBackground = ObjectMap<GameSprite, Animation>()
    private val blinkingArrows = OrderedMap<String, BlinkingArrow>()
    private val sideBars = Array<GameSprite>()

    private var selectedLevelDef: LevelDefinition? = null
    private var initialized = false

    private val out = Vector2()

    override fun init() {
        if (initialized) {
            GameLogger.debug(TAG, "init(): already initialized, nothing to do")
            return
        }

        if (regions.isEmpty) {
            val uiAtlas = game.assMan.getTextureAtlas(TextureAsset.UI_1.source)
            gdxArrayOf("blue", "black", "white", "bar").forEach { regions.put(it, uiAtlas.findRegion(it)) }
            gdxArrayOf("left", "left_80x48", "right", "right_80x48").forEach {
                regions.put(it, uiAtlas.findRegion("${MugshotPane.TAG}/$it"))
            }

            val tilesAtlas = game.assMan.getTextureAtlas(TextureAsset.PLATFORMS_1.source)
            regions.put("block", tilesAtlas.findRegion("8bitBlueBlockTransBorder"))
        }

        val blackRegion = regions[SIDE_BAR_COLOR_KEY]

        val bottomBlackBar = GameSprite(blackRegion)
        bottomBlackBar.setBounds(
            0f,
            0f,
            BOTTOM_BLACK_BAR_WIDTH * ConstVals.PPM,
            BOTTOM_BLACK_BAR_HEIGHT * ConstVals.PPM
        )
        sideBars.add(bottomBlackBar)

        val leftBar = GameSprite(blackRegion)
        leftBar.setBounds(0f, 0f, SIDE_BLACK_BAR_WIDTH * ConstVals.PPM, SIDE_BLACK_BAR_HEIGHT * ConstVals.PPM)
        sideBars.add(leftBar)

        val rightBar = GameSprite(blackRegion)
        rightBar.setSize(SIDE_BLACK_BAR_WIDTH * ConstVals.PPM, SIDE_BLACK_BAR_HEIGHT * ConstVals.PPM)
        rightBar.setPosition((ConstVals.VIEW_WIDTH - SIDE_BLACK_BAR_WIDTH) * ConstVals.PPM, 0f)
        sideBars.add(rightBar)

        val pressStart = MegaFontHandle(
            text = PRESS_START,
            positionX = ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f,
            positionY = ConstVals.VIEW_HEIGHT * ConstVals.PPM,
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
                val key = currentButtonKey
                when (key) {
                    BACK_BUTTON_KEY, null -> PRESS_START
                    Position.CENTER.name -> MEGA_MAN
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
                game.setCurrentScreen(ScreenEnum.MAIN_MENU_SCREEN.name)
                return true
            }

            override fun onNavigate(direction: Direction, delta: Float) = when (direction) {
                Direction.UP, Direction.LEFT, Direction.RIGHT -> Position.get(2, 0).name
                Direction.DOWN -> Position.get(2, 2).name
            }
        })

        val levelDefIter = LevelDefMap.getDefsOfLevelType(LevelType.ROBOT_MASTER_LEVEL).iterator()
        Position.entries.forEach { pos ->
            when (pos) {
                Position.CENTER -> putMegamanMugshot()
                else -> {
                    if (!levelDefIter.hasNext()) {
                        putDefaultMugshot(pos)
                        return@forEach
                    }
                    val levelDef = levelDefIter.next()
                    levelDefGrid.put(pos, levelDef)
                    putBossMugshot(pos, levelDef)
                }
            }
        }

        val outroRunnables = Array<TimeMarkedRunnable>()
        for (i in 1..OUTRO_BLINKS) {
            val outroRunnableTime = i * (OUTRO_DUR / OUTRO_BLINKS)
            val outroRunnable = TimeMarkedRunnable(outroRunnableTime) { outroBlink = !outroBlink }
            outroRunnables.add(outroRunnable)
        }
        outroTimer.setRunnables(outroRunnables)

        background.setBounds(
            0f,
            0f,
            ConstVals.VIEW_WIDTH * ConstVals.PPM,
            ConstVals.VIEW_HEIGHT * ConstVals.PPM
        )

        val barRegion = regions["bar"]
        for (barCol in 0 until BARS_COLUMNS) for (barRow in 0 until BARS_ROWS) {
            val sprite = GameSprite(barRegion)
            sprite.setBounds(
                barCol * BAR_PADDING_X * ConstVals.PPM,
                (barRow * BAR_PADDING_Y * ConstVals.PPM).plus(BARS_OFFSET_Y * ConstVals.PPM),
                BAR_WIDTH * ConstVals.PPM,
                BAR_HEIGHT * ConstVals.PPM
            )
            val timedAnimation = Animation(barRegion, 1, 4, Array.with(0.3f, 0.15f, 0.15f, 0.15f), true)
            barsBackground.put(sprite, timedAnimation)
        }

        val blockRegion = regions["block"]
        val halfPPM = ConstVals.PPM / 2f
        (0 until ConstVals.VIEW_WIDTH.toInt()).forEach { blockX ->
            (0 until ConstVals.VIEW_HEIGHT.toInt() - 1).forEach { blockY ->
                (0..1).forEach { offsetX ->
                    (0..1).forEach { offsetY ->
                        val blueBlock = GameSprite(blockRegion)
                        blueBlock.setBounds(
                            (blockX * ConstVals.PPM) + (offsetX * halfPPM),
                            (blockY * ConstVals.PPM) + (offsetY * halfPPM),
                            halfPPM,
                            halfPPM
                        )
                        blocksBackground.add(blueBlock)
                    }
                }
            }
        }
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

    private fun getMugshotPosition(position: Position, out: Vector2): Vector2 {
        val posX = (position.x * PANE_WIDTH * ConstVals.PPM)
            .plus(PANE_X_PADDING * position.x * ConstVals.PPM)
            .plus(PANES_X_OFFSET * ConstVals.PPM)
        val posY = (position.y * PANE_HEIGHT * ConstVals.PPM)
            .plus(PANE_Y_PADDING * position.y * ConstVals.PPM)
            .plus(PANES_Y_OFFSET * ConstVals.PPM)
        return out.set(posX, posY)
    }

    private fun putDefaultMugshot(position: Position) {
        if (unknownRegion == null) unknownRegion = game.assMan.getTextureRegion(TextureAsset.FACES_1.source, "Unknown")

        val pos = getMugshotPosition(position, out)
        val mugshot =
            MugshotPane(
                game,
                pos.x,
                pos.y,
                PANE_WIDTH * ConstVals.PPM,
                PANE_HEIGHT * ConstVals.PPM,
                { unknownRegion!! },
                null
            )
        mugshotGrid.put(position, mugshot)

        buttons.put(position.name, object : IMenuButton {

            override fun onSelect(delta: Float): Boolean {
                game.audioMan.playSound(SoundAsset.ERROR_SOUND, false)
                return false
            }

            override fun onNavigate(direction: Direction, delta: Float) = navigate(position, direction)
        })
    }

    private fun putMegamanMugshot() {
        val atlas = game.assMan.getTextureAtlas(TextureAsset.FACES_1.source)

        val faces = ObjectMap<Position, TextureRegion>()
        Position.entries.forEach { pos ->
            val region = atlas.findRegion("Megaman/${pos.name}")
            faces.put(pos, region)
        }

        val faceSupplier: () -> TextureRegion = {
            val position = Position.entries.find { it.name == currentButtonKey } ?: Position.CENTER
            faces[position]
        }

        val pos = getMugshotPosition(Position.CENTER, out)
        val mugshot =
            MugshotPane(game, pos.x, pos.y, PANE_WIDTH * ConstVals.PPM, PANE_HEIGHT * ConstVals.PPM, faceSupplier, null)
        mugshotGrid.put(Position.CENTER, mugshot)

        buttons.put(Position.CENTER.name, object : IMenuButton {

            override fun onSelect(delta: Float) = false

            override fun onNavigate(direction: Direction, delta: Float) = Position.CENTER.move(direction).name
        })
    }

    private fun putBossMugshot(position: Position, levelDef: LevelDefinition) {
        val mugshotRegion = game.assMan.getTextureRegion(
            TextureAsset.valueOf(levelDef.mugshotAtlas.uppercase()).source,
            levelDef.mugshotRegion
        )
        val faceSupplier: () -> TextureRegion = {
            when {
                game.state.levelsDefeated.contains(levelDef) -> regions["black"]
                else -> mugshotRegion
            }
        }
        val text = game.assMan.getTextureRegion(TextureAsset.UI_1.source, "${MugshotPane.TAG}/Text/${levelDef.name}")

        val pos = getMugshotPosition(position, out)
        val mugshot = when (levelDef) {
            LevelDefinition.MOON_MAN -> {
                MugshotPane(
                    game,
                    pos.x,
                    pos.y,
                    PANE_WIDTH * ConstVals.PPM,
                    PANE_HEIGHT * ConstVals.PPM,
                    faceSupplier,
                    { text },
                    faceWidth = MOON_MAN_WIDTH * ConstVals.PPM,
                    faceHeight = MOON_MAN_HEIGHT * ConstVals.PPM,
                    faceOffsetY = MOON_MAN_OFFSET_Y * ConstVals.PPM,
                    underPane = false
                )
            }

            else -> MugshotPane(
                game,
                pos.x,
                pos.y,
                PANE_WIDTH * ConstVals.PPM,
                PANE_HEIGHT * ConstVals.PPM,
                faceSupplier,
                { text }
            )
        }
        mugshotGrid.put(position, mugshot)

        buttons.put(position.name, object : IMenuButton {

            override fun onSelect(delta: Float): Boolean {
                game.audioMan.playSound(SoundAsset.BEAM_OUT_SOUND, false)
                game.audioMan.stopMusic(null)

                selectedLevelDef = levelDef
                outro = true

                return true
            }

            override fun onNavigate(direction: Direction, delta: Float) = navigate(position, direction)
        })
    }

    override fun show() {
        if (!initialized) {
            init()
            initialized = true
        }

        super.show()

        outro = false
        outroTimer.reset()

        game.getUiCamera().position.set(ConstFuncs.getUiCamInitPos())
        game.audioMan.playMusic(MusicAsset.MM3_SNAKE_MAN_MUSIC, true)
    }

    override fun onAnyMovement(direction: Direction) =
        game.audioMan.playSound(SoundAsset.CURSOR_MOVE_BLOOP_SOUND, false)

    override fun render(delta: Float) {
        super.render(delta)

        if (!game.paused) {
            if (outro) outroTimer.update(delta)
            if (outroTimer.isJustFinished()) {
                /*
                TODO:
                val bIntroScreen = game.screens.get(ScreenEnum.BOSS_INTRO_SCREEN.name) as BossIntroScreen
                bIntroScreen.set(bSelect!!)
                game.setCurrentScreen(ScreenEnum.BOSS_INTRO_SCREEN.name)
                */
                game.startLevelScreen(selectedLevelDef!!)
            }
            if (outroTimer.isFinished()) return

            mugshotGrid.forEach {
                val key = it.key
                val mugshot = it.value

                mugshot.update(delta)

                mugshot.state = when (currentButtonKey) {
                    key.name -> if (selectionMade) MugshotPaneState.HIGHLIGHTED else MugshotPaneState.BLINKING
                    else -> MugshotPaneState.NONE
                }
            }

            blinkingArrows.get(currentButtonKey)?.update(delta)

            barsBackground.values().forEach { it.update(delta) }
        }

        val batch = game.batch

        batch.projectionMatrix = game.getUiCamera().combined
        batch.begin()

        background.setRegion(regions[if (outro && outroBlink) ConstKeys.WHITE else ConstKeys.BLACK])
        background.draw(batch)

        blocksBackground.forEach { it.draw(batch) }
        barsBackground.forEach {
            val sprite = it.key
            val animation = it.value
            val region = animation.getCurrentRegion()
            sprite.setRegion(region)
            sprite.draw(batch)
        }
        mugshotGrid.forEach { it.value.draw(batch) }
        sideBars.forEach { it.draw(batch) }

        text.forEach { it.draw(batch) }
        blinkingArrows.get(currentButtonKey)?.draw(batch)

        batch.end()

        if (DEBUG_TEXT_BOUNDS) {
            val shapeRenderer = game.shapeRenderer
            shapeRenderer.begin()
            shapeRenderer.projectionMatrix = game.getUiCamera().combined
            text.forEach { it.draw(shapeRenderer) }
            shapeRenderer.end()
        }
    }
}

internal class MugshotPane(
    private val game: MegamanMaverickGame,
    private val positionX: Float,
    private val positionY: Float,
    private val paneWidth: Float,
    private val paneHeight: Float,
    private val faceSupplier: () -> TextureRegion,
    private val textSupplier: (() -> TextureRegion)?,
    private val faceWidth: Float = DEFAULT_FACE_WIDTH * ConstVals.PPM,
    private val faceHeight: Float = DEFAULT_FACE_HEIGHT * ConstVals.PPM,
    private val faceOffsetY: Float = DEFAULT_FACE_OFFSET_Y * ConstVals.PPM,
    private val underPane: Boolean = false,
    private val renderText: Boolean = false
) : Initializable, Updatable, IDrawable<Batch> {

    companion object {
        const val TAG = "MugshotPane"

        private const val DEFAULT_FACE_WIDTH = 2.125f
        private const val DEFAULT_FACE_HEIGHT = 2.125f
        private const val DEFAULT_FACE_OFFSET_Y = 0.5f // 1.35f

        private const val KEY_SUFFIX = "_48x48"
        private const val NONE_REGION_KEY = "none$KEY_SUFFIX"
        private const val BLINKING_REGION_KEY = "blinking$KEY_SUFFIX"
        private const val HIGHLIGHTED_REGION_KEY = "highlighted$KEY_SUFFIX"

        private val regions = ObjectMap<String, TextureRegion>()
    }

    internal enum class MugshotPaneState { NONE, BLINKING, HIGHLIGHTED }

    var state = MugshotPaneState.NONE

    private val faceSprite = GameSprite()
    private val paneSprite = GameSprite()
    private val textSprite = GameSprite()
    private val paneAnimations = ObjectMap<MugshotPaneState, IAnimation>()
    private var initialized = false

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

        paneSprite.setSize(paneWidth, paneHeight)
        paneSprite.setPosition(positionX, positionY)

        faceSprite.setSize(faceWidth, faceHeight)
        faceSprite.setCenterX(positionX + (paneWidth / 2f))
        faceSprite.y = positionY + faceOffsetY

        textSprite.setSize(paneWidth, paneHeight)
        textSprite.setPosition(positionX, positionY)

        paneAnimations.put(MugshotPaneState.NONE, Animation(regions[NONE_REGION_KEY]))
        paneAnimations.put(MugshotPaneState.BLINKING, Animation(regions[BLINKING_REGION_KEY], 2, 1, 0.125f, true))
        paneAnimations.put(MugshotPaneState.HIGHLIGHTED, Animation(regions[HIGHLIGHTED_REGION_KEY]))
    }

    override fun update(delta: Float) {
        if (!initialized) {
            init()
            initialized = true
        }

        val paneAnimation = paneAnimations[state]
        paneAnimation.update(delta)
        paneSprite.setRegion(paneAnimation.getCurrentRegion())
    }

    private fun drawFace(drawer: Batch) {
        val face = faceSupplier.invoke()
        faceSprite.setRegion(face)
        faceSprite.draw(drawer)
    }

    override fun draw(drawer: Batch) {
        if (underPane) drawFace(drawer)

        paneSprite.draw(drawer)

        if (!underPane) drawFace(drawer)

        if (renderText) {
            val text = textSupplier?.invoke()
            if (text != null) {
                textSprite.setRegion(text)
                textSprite.draw(drawer)
            }
        }
    }
}
