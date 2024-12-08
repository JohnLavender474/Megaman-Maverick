package com.megaman.maverick.game.screens.menus.temp

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.common.objects.GamePair
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.time.TimeMarkedRunnable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.screens.menus.IMenuButton
import com.megaman.maverick.game.ConstFuncs
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.drawables.fonts.MegaFontHandle
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.levels.LevelType
import com.megaman.maverick.game.screens.ScreenEnum
import com.megaman.maverick.game.screens.menus.MegaMenuScreen
import com.megaman.maverick.game.screens.menus.bosses.Mugshot
import com.megaman.maverick.game.screens.menus.bosses.MugshotState
import com.megaman.maverick.game.screens.utils.BlinkingArrow
import com.megaman.maverick.game.screens.utils.ScreenSlide
import com.megaman.maverick.game.utils.extensions.getDefaultCameraPosition

class LevelSelectScreen(game: MegamanMaverickGame) : MegaMenuScreen(game, Position.CENTER.name), Initializable {

    companion object {
        const val TAG = "LevelSelectScreen"

        private const val BACK_BUTTON_KEY = "BACK"
        private const val MEGA_MAN = "MEGA MAN"
        private const val UNKNOWN_MAN = "UNKNOWN MAN"

        private const val PRESS_START = "PRESS START"
        private const val PRESS_START_X = 5.35f
        private const val PRESS_START_Y = 13.35f

        private const val INTRO_DUR = 0.5f
        private val INTRO_BLOCKS_TRANS = Vector3(15f * ConstVals.PPM, 0f, 0f)
        private val INTRO_CAM_POS = getDefaultCameraPosition().add(0f, 0.55f * ConstVals.PPM, 0f)

        private const val OUTRO_DUR = 1f
        private const val OUTRO_BLINKS = 10

        private const val BOSS_NAME_X = 1f
        private const val BOSS_NAME_Y = 1f

        private const val BACK_BUTTON_X = 12.35f
        private const val BACK_BUTTON_Y = 1f
        private const val BACK_ARROW_X = 12f
        private const val BACK_ARROW_Y = 0.75f

        private const val BACKGROUND_BARS_ROWS = 2
        private const val BACKGROUND_BARS_COLUMNS = 5
        private const val BACKGROUND_BAR_X = 3f
        private const val BACKGROUND_BAR_Y = 4f
        private const val BACKGROUND_BAR_Y_OFFSET = 0f // 1.35f
        private const val BACKGROUND_BAR_WIDTH = 5.33f
        private const val BACKGROUND_BAR_HEIGHT = 4f

        private var unknownRegion: TextureRegion? = null
    }

    private val levelDefGrid = ObjectMap<Position, LevelDefinition>()
    private val mugshotGrid = ObjectMap<Position, Mugshot>()

    private val introSlide = ScreenSlide(
        game.getUiCamera(),
        INTRO_BLOCKS_TRANS,
        INTRO_CAM_POS.cpy().sub(INTRO_BLOCKS_TRANS),
        INTRO_CAM_POS,
        INTRO_DUR,
        false
    )

    private val outroTimer = Timer(OUTRO_DUR)
    private var outroBlink = false
    private var outro = false

    private val text = Array<MegaFontHandle>()
    private val blinkingArrows = OrderedMap<String, BlinkingArrow>()

    private val backgroundBlocks = Array<GameSprite>()
    private val backgroundBars = Array<GamePair<GameSprite, IAnimation>>()

    private val whiteBackground = GameSprite()
    private val blackBar = GameSprite()

    private var selectedRmLevelKey: String? = null

    private var initialized = false

    override fun init() {
        if (initialized) {
            GameLogger.debug(TAG, "init(): already initialized, nothing to do")
            return
        }

        buttons.put(BACK_BUTTON_KEY, object : IMenuButton {

            override fun onSelect(delta: Float): Boolean {
                game.setCurrentScreen(ScreenEnum.MAIN_MENU_SCREEN.name)
                return true
            }

            override fun onNavigate(direction: Direction, delta: Float) = when (direction) {
                Direction.UP, Direction.LEFT, Direction.RIGHT -> Position.BOTTOM_CENTER.name
                Direction.DOWN -> Position.TOP_CENTER.name
            }
        })

        val rmLevelKeyIter = game.levelDefs.getKeysOfLevelType(LevelType.RM).iterator()
        Position.entries.forEach { pos ->
            when (pos) {
                Position.CENTER -> putMegamanMugshot()
                else -> {
                    if (!rmLevelKeyIter.hasNext) {
                        putDefaultMugshot(pos)
                        return@forEach
                    }
                    val rmLevelKey = rmLevelKeyIter.next()
                    putBossMugshot(pos, rmLevelKey)
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

        val backButtonText = MegaFontHandle(
            BACK_BUTTON_KEY,
            positionX = BACK_BUTTON_X * ConstVals.PPM,
            positionY = BACK_BUTTON_Y * ConstVals.PPM,
            centerX = false,
            centerY = false
        )
        text.add(backButtonText)

        val pressStartText = MegaFontHandle(
            PRESS_START,
            positionX = PRESS_START_X,
            positionY = PRESS_START_Y,
            centerX = false,
            centerY = false
        )
        text.add(pressStartText)

        val bossNameSupplier: () -> String = {
            val key = currentButtonKey
            when (key) {
                BACK_BUTTON_KEY, null -> ""
                else -> {
                    val position = Position.valueOf(key)
                    val bossName = mugshotGrid[position].name!!
                    bossName
                }
            }
        }
        val bossNameText = MegaFontHandle(
            textSupplier = bossNameSupplier,
            positionX = BOSS_NAME_X * ConstVals.PPM,
            positionY = BOSS_NAME_Y * ConstVals.PPM,
            centerX = false,
            centerY = false
        )
        text.add(bossNameText)

        val backButtonArrow =
            BlinkingArrow(game.assMan, Vector2(BACK_ARROW_X * ConstVals.PPM, BACK_ARROW_Y * ConstVals.PPM))
        blinkingArrows.put(BACK_BUTTON_KEY, backButtonArrow)

        val barRegion = game.assMan.getTextureRegion(TextureAsset.UI_1.source, "Bar")
        for (x in 0..BACKGROUND_BARS_COLUMNS) for (y in 0..BACKGROUND_BARS_ROWS) {
            val barSprite = GameSprite(barRegion)
            barSprite.setBounds(
                x * BACKGROUND_BAR_X * ConstVals.PPM,
                ((y * BACKGROUND_BAR_Y) + BACKGROUND_BAR_Y_OFFSET) * ConstVals.PPM,
                BACKGROUND_BAR_WIDTH * ConstVals.PPM,
                BACKGROUND_BAR_HEIGHT * ConstVals.PPM
            )
            val barAnimation = Animation(barRegion, 1, 4, Array.with(0.3f, 0.15f, 0.15f, 0.15f), true)
            backgroundBars.add(barSprite pairTo barAnimation)
        }

        val colorsAtlas = game.assMan.getTextureAtlas(TextureAsset.COLORS.source)

        val whiteReg = colorsAtlas.findRegion("White")
        whiteBackground.setRegion(whiteReg)
        whiteBackground.setBounds(0f, 0f, ConstVals.VIEW_WIDTH * ConstVals.PPM, ConstVals.VIEW_HEIGHT * ConstVals.PPM)

        val blackReg = colorsAtlas.findRegion("Black")
        blackBar.setRegion(blackReg)
        blackBar.setBounds(
            -ConstVals.PPM.toFloat(),
            -ConstVals.PPM.toFloat(),
            (2f + ConstVals.VIEW_WIDTH) * ConstVals.PPM,
            2f * ConstVals.PPM
        )

        val blueBlockReg = game.assMan.getTextureRegion(TextureAsset.PLATFORMS_1.source, "8bitBlueBlockTransBorder")
        val halfPPM = ConstVals.PPM / 2f
        var i = 0
        while (i < ConstVals.VIEW_WIDTH) {
            var j = 0
            while (j < ConstVals.VIEW_HEIGHT) {
                for (x in 0..1) for (y in 0..1) {
                    val blueBlock = GameSprite(blueBlockReg)
                    blueBlock.setBounds(
                        i * ConstVals.PPM + (x * halfPPM), j * ConstVals.PPM + (y * halfPPM), halfPPM, halfPPM
                    )
                    backgroundBlocks.add(blueBlock)
                }
                j++
            }
            i++
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

    private fun putDefaultMugshot(position: Position) {
        if (unknownRegion == null) unknownRegion = game.assMan.getTextureRegion(TextureAsset.FACES_1.source, "Unknown")
        mugshotGrid.put(position, Mugshot(game, unknownRegion!!, UNKNOWN_MAN, position))

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

        val faceRegionSupplier: () -> TextureRegion = {
            val position = Position.entries.find { it.name == currentButtonKey } ?: Position.CENTER
            faces[position]
        }

        mugshotGrid.put(Position.CENTER, Mugshot(game, faceRegionSupplier, MEGA_MAN, Position.CENTER))

        buttons.put(Position.CENTER.name, object : IMenuButton {

            override fun onSelect(delta: Float) = false

            override fun onNavigate(direction: Direction, delta: Float) = navigate(Position.CENTER, direction)
        })
    }

    private fun putBossMugshot(position: Position, rmLevelKey: String) {
        val bossName = rmLevelKey.uppercase().replace("_", " ")
        val rmLevelDef = game.levelDefs.getLevelDef(rmLevelKey)
        val mugshotRegion = rmLevelDef.mugshotRegion
        mugshotGrid.put(position, Mugshot(game, mugshotRegion, bossName, position))

        buttons.put(position.name, object : IMenuButton {

            override fun onSelect(delta: Float): Boolean {
                game.audioMan.playSound(SoundAsset.BEAM_OUT_SOUND, false)
                game.audioMan.stopMusic(null)

                selectedRmLevelKey = rmLevelKey
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

        // introSlide.init()
        game.getUiCamera().position.set(ConstFuncs.getCamInitPos())

        outro = false
        outroTimer.reset()

        game.audioMan.playMusic(MusicAsset.MM3_SNAKE_MAN_MUSIC, true)
    }

    override fun onAnyMovement(direction: Direction) =
        game.audioMan.playSound(SoundAsset.CURSOR_MOVE_BLOOP_SOUND, false)

    override fun render(delta: Float) {
        super.render(delta)

        if (!game.paused) {
            introSlide.update(delta)

            if (outro) outroTimer.update(delta)
            if (outroTimer.isFinished()) {
                /*
                TODO:
                val bIntroScreen = game.screens.get(ScreenEnum.BOSS_INTRO_SCREEN.name) as BossIntroScreen
                bIntroScreen.set(bSelect!!)
                game.setCurrentScreen(ScreenEnum.BOSS_INTRO_SCREEN.name)
                */
                game.startLevelScreen(selectedRmLevelKey!!)
                return
            }

            backgroundBars.forEach { (sprite, animation) ->
                animation.update(delta)
                sprite.setRegion(animation.getCurrentRegion())
            }

            mugshotGrid.forEach {
                val key = it.key
                val mugshot = it.value

                mugshot.update(delta)

                mugshot.state = when (currentButtonKey) {
                    key.name -> if (selectionMade) MugshotState.HIGHLIGHTED else MugshotState.BLINKING
                    else -> MugshotState.NONE
                }
            }

            blinkingArrows.get(currentButtonKey)?.update(delta)
        }

        val batch = game.batch

        batch.projectionMatrix = game.getUiCamera().combined
        batch.begin()

        if (outro && outroBlink) whiteBackground.draw(batch)

        backgroundBlocks.forEach { it.draw(batch) }
        backgroundBars.forEach { (sprite, _) -> sprite.draw(batch)  }
        mugshotGrid.forEach { it.value.draw(batch) }
        text.forEach { it.draw(batch) }

        batch.end()
    }

    override fun reset() {
        super.reset()
        levelDefGrid.clear()
    }
}
