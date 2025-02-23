package com.megaman.maverick.game.screens.other

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.Queue
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.putAll
import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.screens.BaseScreen
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.drawables.fonts.MegaFontHandle
import com.megaman.maverick.game.screens.ScreenEnum
import com.megaman.maverick.game.screens.utils.Fade

class LogoScreen(private val game: MegamanMaverickGame) : BaseScreen(), Initializable {

    companion object {
        private const val LOGO_REGION_KEY = "logo"
        private const val LOGO_WIDTH = 6.25f
        private const val LOGO_HEIGHT = 2f

        private const val INIT_DUR = 0.5f
        private const val LOGO_FADE_IN_DUR = 2.5f
        private const val SHOW_DUR = 2f
        private const val FADE_OUT_DUR = 1f

        private const val TOP_LINE = "A GAME BY"
        private const val BOTTOM_LINE_1 = "MEGAMAN IS A CAPCOM TRADEMARK."
        private const val BOTTOM_LINE_2 = "THIS IS A FAN-MADE GAME"
        private const val BOTTOM_LINE_3 = "NOT ENDORSED BY CAPCOM."
    }

    private enum class LogoScreenState { INIT, FADE_IN_LOGO, SHOW_LOGO, FADE_OUT }

    private val stateQueue = Queue<LogoScreenState>()
    private val currentState: LogoScreenState
        get() = stateQueue.first()

    private val timers = OrderedMap<LogoScreenState, Timer>()

    private val text = Array<MegaFontHandle>()
    private val oldLavyLogo = GameSprite()
    private lateinit var fadeOut: Fade

    private var initialized = false

    override fun init() {
        timers.putAll(
            LogoScreenState.INIT pairTo Timer(INIT_DUR),
            LogoScreenState.FADE_IN_LOGO pairTo Timer(LOGO_FADE_IN_DUR),
            LogoScreenState.SHOW_LOGO pairTo Timer(SHOW_DUR),
            LogoScreenState.FADE_OUT pairTo Timer(FADE_OUT_DUR)
        )

        val fanGameByText = MegaFontHandle(
            TOP_LINE,
            positionX = ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f,
            positionY = (ConstVals.VIEW_HEIGHT - 4f) * ConstVals.PPM
        )
        text.add(fanGameByText)

        val trademarkedText = MegaFontHandle(
            BOTTOM_LINE_1,
            positionX = ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f,
            positionY = 4f * ConstVals.PPM
        )
        text.add(trademarkedText)

        val nonCommercialText = MegaFontHandle(
            BOTTOM_LINE_2,
            positionX = ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f,
            positionY = 3f * ConstVals.PPM
        )
        text.add(nonCommercialText)

        val byFansText = MegaFontHandle(
            BOTTOM_LINE_3,
            positionX = ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f,
            positionY = 2f * ConstVals.PPM
        )
        text.add(byFansText)

        val logo = game.assMan.getTextureRegion(TextureAsset.UI_1.source, LOGO_REGION_KEY)
        oldLavyLogo.setRegion(logo)
        oldLavyLogo.setSize(LOGO_WIDTH * ConstVals.PPM, LOGO_HEIGHT * ConstVals.PPM)
        oldLavyLogo.setCenter(ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f, ConstVals.VIEW_HEIGHT * ConstVals.PPM / 2f)

        fadeOut = Fade(Fade.FadeType.FADE_OUT, FADE_OUT_DUR)

        val black = game.assMan.getTextureRegion(TextureAsset.COLORS.source, ConstKeys.BLACK)
        fadeOut.setRegion(black)
        fadeOut.setPosition(0f, 0f)
        fadeOut.setWidth(ConstVals.VIEW_WIDTH * ConstVals.PPM)
        fadeOut.setHeight(ConstVals.VIEW_HEIGHT * ConstVals.PPM)

        initialized = true
    }

    override fun show() {
        if (!initialized) init()

        oldLavyLogo.setAlpha(0f)

        fadeOut.reset()
        timers.values().forEach { it.reset() }

        stateQueue.clear()
        LogoScreenState.entries.forEach { stateQueue.addLast(it) }
    }

    override fun render(delta: Float) {
        if (game.controllerPoller.isJustReleased(MegaControllerButton.START)) {
            game.setCurrentScreen(ScreenEnum.MAIN_MENU_SCREEN.name)
            return
        }

        val timer = timers[currentState]
        timer.update(delta)

        val batch = game.batch
        batch.projectionMatrix = game.getUiCamera().combined
        batch.begin()

        when (currentState) {
            LogoScreenState.FADE_IN_LOGO -> {
                val alpha = timer.getRatio()
                oldLavyLogo.setAlpha(alpha)
                oldLavyLogo.draw(batch)
            }

            LogoScreenState.SHOW_LOGO -> {
                text.forEach { it.draw(batch) }
                oldLavyLogo.draw(batch)
            }

            LogoScreenState.FADE_OUT -> {
                text.forEach { it.draw(batch) }
                oldLavyLogo.draw(batch)

                fadeOut.update(delta)
                fadeOut.draw(batch)
            }

            else -> {}
        }

        batch.end()

        if (timer.isFinished()) {
            timer.reset()

            stateQueue.removeFirst()

            when {
                // TODO: set to intro cutscenes screen instead of main menu
                stateQueue.isEmpty -> game.setCurrentScreen(ScreenEnum.MAIN_MENU_SCREEN.name)

                currentState == LogoScreenState.FADE_IN_LOGO ->
                    game.audioMan.playMusic(MusicAsset.MM6_CAPCOM_LOGO_MUSIC, false)
            }
        }
    }
}
