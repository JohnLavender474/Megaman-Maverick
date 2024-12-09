package com.megaman.maverick.game.screens.menus.bosses

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
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
import com.megaman.maverick.game.screens.menus.MegaMenuScreen
import com.megaman.maverick.game.screens.menus.bosses.MugshotPane.MugshotPaneState
import com.megaman.maverick.game.screens.utils.BlinkingArrow

class LevelSelectScreen(game: MegamanMaverickGame) : MegaMenuScreen(game, Position.CENTER.name), Initializable {

    companion object {
        const val TAG = "LevelSelectScreen"

        /*
        private const val BACK_BUTTON_KEY = "BACK"
        private const val BACK_BUTTON_X = ConstVals.VIEW_WIDTH - 2f
        private const val BACK_BUTTON_Y = -0.1f
        private const val BACK_ARROW_X = ConstVals.VIEW_WIDTH - 2.35f
        private const val BACK_ARROW_Y = -0.3f
         */

        private const val PRESS_START = "PRESS START"
        private const val PRESS_START_X = ConstVals.VIEW_WIDTH / 2f
        private const val PRESS_START_Y = (ConstVals.VIEW_HEIGHT / 2f) - 1.125f

        private const val PANE_X_OFFSET = 0.125f
        private const val PANE_WIDTH = 5f
        private const val PANE_HEIGHT = 4f

        private const val MOON_MAN_WIDTH = 4f
        private const val MOON_MAN_HEIGHT = 4f
        private const val MOON_MAN_OFFSET_Y = 0.5f

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

    private val text = Array<MegaFontHandle>()
    private val blinkingArrows = OrderedMap<String, BlinkingArrow>()
    private val background = GameSprite()
    private val sides = Array<GameSprite>()

    private var selectedLevelDef: LevelDefinition? = null

    private var initialized = false

    override fun init() {
        if (initialized) {
            GameLogger.debug(TAG, "init(): already initialized, nothing to do")
            return
        }

        if (regions.isEmpty) {
            val uiAtlas = game.assMan.getTextureAtlas(TextureAsset.UI_1.source)
            gdxArrayOf("blue", "black", "white").forEach { regions.put(it, uiAtlas.findRegion(it)) }
            gdxArrayOf("left", "right").forEach { regions.put(it, uiAtlas.findRegion("MugshotPane/$it")) }
        }

        /*
        val pressStart = MegaFontHandle(
            PRESS_START,
            positionX = PRESS_START_X * ConstVals.PPM,
            positionY = PRESS_START_Y * ConstVals.PPM,
            centerX = true,
            centerY = false
        )
        text.add(pressStart)
         */

        /*
        val back = MegaFontHandle(
            BACK_BUTTON_KEY,
            positionX = BACK_BUTTON_X * ConstVals.PPM,
            positionY = BACK_BUTTON_Y * ConstVals.PPM,
            centerX = false,
            centerY = false
        )
        text.add(back)

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
         */

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
        for (y in 0 until 3) {
            val left = GameSprite(regions["left"])
            left.setBounds(0f, y * PANE_HEIGHT * ConstVals.PPM, PANE_WIDTH * ConstVals.PPM, PANE_HEIGHT * ConstVals.PPM)
            sides.add(left)

            val right = GameSprite(regions["right"])
            right.setBounds(
                (ConstVals.VIEW_WIDTH - PANE_WIDTH) * ConstVals.PPM,
                y * PANE_HEIGHT * ConstVals.PPM,
                PANE_WIDTH * ConstVals.PPM,
                PANE_HEIGHT * ConstVals.PPM
            )
            sides.add(right)
        }
    }

    private fun navigate(position: Position, direction: Direction): String {
        return position.move(direction).name

        /*
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
         */
    }

    private fun putDefaultMugshot(position: Position) {
        if (unknownRegion == null) unknownRegion = game.assMan.getTextureRegion(TextureAsset.FACES_1.source, "Unknown")

        val posX = (position.x + PANE_X_OFFSET) * PANE_WIDTH * ConstVals.PPM
        val posY = position.y * PANE_HEIGHT * ConstVals.PPM
        val mugshot =
            MugshotPane(
                game,
                posX,
                posY,
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

        val posX = (Position.CENTER.x + PANE_X_OFFSET) * PANE_WIDTH * ConstVals.PPM
        val posY = Position.CENTER.y * PANE_HEIGHT * ConstVals.PPM
        val mugshot =
            MugshotPane(game, posX, posY, PANE_WIDTH * ConstVals.PPM, PANE_HEIGHT * ConstVals.PPM, faceSupplier, null)
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
        val text = game.assMan.getTextureRegion(TextureAsset.UI_1.source, "MugshotPane/Text/${levelDef.name}")
        val posX = (position.x + PANE_X_OFFSET) * PANE_WIDTH * ConstVals.PPM
        val posY = position.y * PANE_HEIGHT * ConstVals.PPM

        val mugshot = when (levelDef) {
            LevelDefinition.MOON_MAN -> {
                MugshotPane(
                    game,
                    posX,
                    posY,
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
                posX,
                posY,
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
        }

        val batch = game.batch

        batch.projectionMatrix = game.getUiCamera().combined
        batch.begin()

        background.setRegion(regions[if (outro && outroBlink) ConstKeys.WHITE else ConstKeys.BLUE])
        background.draw(batch)

        mugshotGrid.forEach { it.value.draw(batch) }
        sides.forEach { it.draw(batch) }
        text.forEach { it.draw(batch) }
        blinkingArrows.get(currentButtonKey)?.draw(batch)

        batch.end()
    }

    override fun reset() {
        super.reset()
        levelDefGrid.clear()
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
    private val underPane: Boolean = false
) : Initializable, Updatable, IDrawable<Batch> {

    companion object {
        const val TAG = "MugshotPane"
        private const val DEFAULT_FACE_WIDTH = 2.125f
        private const val DEFAULT_FACE_HEIGHT = 2.125f
        private const val DEFAULT_FACE_OFFSET_Y = 1.35f
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
            regions.put("mugshot_none", uiAtlas.findRegion("MugshotPane/none"))
            regions.put("mugshot_blinking", uiAtlas.findRegion("MugshotPane/blinking"))
            regions.put("mugshot_highlighted", uiAtlas.findRegion("MugshotPane/highlighted"))
        }

        paneSprite.setSize(paneWidth, paneHeight)
        paneSprite.setPosition(positionX, positionY)

        faceSprite.setSize(faceWidth, faceHeight)
        faceSprite.setCenterX(positionX + (paneWidth / 2f))
        faceSprite.y = positionY + faceOffsetY

        textSprite.setSize(paneWidth, paneHeight)
        textSprite.setPosition(positionX, positionY)

        paneAnimations.put(MugshotPaneState.NONE, Animation(regions["mugshot_none"]))
        paneAnimations.put(MugshotPaneState.BLINKING, Animation(regions["mugshot_blinking"], 2, 1, 0.125f, true))
        paneAnimations.put(MugshotPaneState.HIGHLIGHTED, Animation(regions["mugshot_highlighted"]))
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

        val text = textSupplier?.invoke()
        if (text != null) {
            textSprite.setRegion(text)
            textSprite.draw(drawer)
        }
    }
}
