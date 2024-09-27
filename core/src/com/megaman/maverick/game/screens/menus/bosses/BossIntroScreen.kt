package com.megaman.maverick.game.screens.menus.bosses

import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.common.objects.GamePair
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.fonts.BitmapFontHandle
import com.mega.game.engine.screens.BaseScreen
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.drawables.sprites.ScrollingStars
import com.megaman.maverick.game.entities.bosses.BossType
import com.megaman.maverick.game.utils.MegaUtilMethods.getDefaultFontSize
import com.megaman.maverick.game.utils.getDefaultCameraPosition
import java.util.*

class BossIntroScreen(private val game: MegamanMaverickGame) : BaseScreen(), Initializable {

    companion object {
        private const val DUR = 7f
        private const val B_DROP = 0.25f
        private const val B_LET_DELAY = 1f
        private const val B_LET_DUR = 0.2f
        private const val STARS_N_BARS = 4
    }

    private val uiCam = game.getUiCamera()
    private val audioMan = game.audioMan

    private val bLettersDelay = Timer(B_LET_DELAY)
    private val bLettersTimer = Timer(B_LET_DUR)
    private val bLettersAnimQ = LinkedList<Runnable>()

    private val bars = Array<Sprite>()

    private lateinit var scrollingStars: ScrollingStars
    private lateinit var durTimer: Timer
    private lateinit var bDropTimer: Timer
    private lateinit var barAnim: Animation
    private lateinit var bText: BitmapFontHandle

    private var b: BossType? = null
    private var currBAnim: GamePair<Sprite, Queue<GamePair<Animation, Timer>?>>? = null

    private var initialized = false

    override fun init() {
        if (initialized) return
        initialized = true

        val barReg: TextureRegion =
            game.assMan.get(TextureAsset.UI_1.source, TextureAtlas::class.java).findRegion("Bar")
        barAnim = Animation(barReg, 1, 4, Array.with(0.3f, 0.15f, 0.15f, 0.15f), true)

        for (i in 0 until STARS_N_BARS) {
            val bar = Sprite()
            bar.setBounds(
                (i * ConstVals.VIEW_WIDTH * ConstVals.PPM / 3f) - 5f,
                ConstVals.VIEW_HEIGHT * ConstVals.PPM / 3f,
                (ConstVals.VIEW_WIDTH * ConstVals.PPM / 3f) + 5f,
                ConstVals.VIEW_HEIGHT * ConstVals.PPM / 3f
            )
            bars.add(bar)
        }

        scrollingStars = ScrollingStars(game, Vector2(-10f * ConstVals.PPM, 0f))

        durTimer = Timer(DUR)
        bDropTimer = Timer(B_DROP)

        bText = BitmapFontHandle(
            { "" }, getDefaultFontSize(), Vector2(
                ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f, ConstVals.VIEW_HEIGHT * ConstVals.PPM / 4f
            ), centerX = true, centerY = true, fontSource = ConstVals.MEGAMAN_MAVERICK_FONT
        )
    }

    fun set(b: BossType) {
        this.b = b

        val s = Sprite()
        val size = b.spriteSize
        s.setSize(size.x * ConstVals.PPM, size.y * ConstVals.PPM)

        currBAnim = GamePair(
            s, b.getIntroAnimsQ(
                game.assMan.get(b.ass.source, TextureAtlas::class.java)
            )
        )

        bLettersAnimQ.clear()

        for (i in 0 until b.name.length) {
            bLettersAnimQ.add(Runnable {
                bText.textSupplier = { b.name.substring(0, i + 1).uppercase() }
                if (Character.isWhitespace(b.name[i])) return@Runnable
                audioMan.playSound(SoundAsset.THUMP_SOUND, false)
            })
        }
    }

    override fun show() {
        if (!initialized) init()
        super.show()

        bText.textSupplier = { "" }

        durTimer.reset()
        bDropTimer.reset()
        bLettersTimer.reset()
        bLettersDelay.reset()

        currBAnim!!.component1().setPosition(
            ((ConstVals.VIEW_WIDTH / 2f) - 1.5f) * ConstVals.PPM, ConstVals.VIEW_HEIGHT * ConstVals.PPM
        )

        for (e in currBAnim!!.component2()) {
            e!!.component1().reset()
            e.component2().reset()
        }

        uiCam.position.set(getDefaultCameraPosition())

        audioMan.playMusic(MusicAsset.MM2_BOSS_INTRO_MUSIC, false)
    }

    override fun render(delta: Float) {
        if (durTimer.isFinished()) {
            game.startLevelScreen(b!!.level)
            return
        }

        val bSprite = currBAnim!!.component1()

        if (!game.paused) {
            durTimer.update(delta)

            scrollingStars.update(delta)
            barAnim.update(delta)
            bars.forEach { it.setRegion(barAnim.getCurrentRegion()) }

            bDropTimer.update(delta)
            if (!bDropTimer.isFinished()) bSprite.y =
                ConstVals.VIEW_HEIGHT * ConstVals.PPM -
                        (((ConstVals.VIEW_HEIGHT * ConstVals.PPM / 2f) + 0.85f * ConstVals.PPM) * bDropTimer.getRatio())
            if (bDropTimer.isJustFinished()) bSprite.y =
                ConstVals.VIEW_HEIGHT * ConstVals.PPM / 2f - 0.85f * ConstVals.PPM

            bLettersDelay.update(delta)
            if (bLettersDelay.isFinished() && bDropTimer.isFinished() && !bLettersAnimQ.isEmpty()) {
                bLettersTimer.update(delta)
                if (bLettersTimer.isFinished()) {
                    bLettersAnimQ.poll().run()
                    bLettersTimer.reset()
                }
            }

            val bAnimQ = currBAnim!!.component2()
            assert(bAnimQ.peek() != null)

            val t = bAnimQ.peek()!!.component2()
            if (bAnimQ.size > 1 && t.isFinished()) {
                bAnimQ.peek()!!.component1().reset()
                bAnimQ.poll()
            }
            t.update(delta)

            val bAnim = bAnimQ.peek()!!.component1()
            bAnim.update(delta)

            bSprite.setRegion(bAnim.getCurrentRegion())
        }

        val batch = game.batch
        batch.projectionMatrix = uiCam.combined
        batch.begin()
        scrollingStars.draw(batch)
        bars.forEach { it.draw(batch) }
        bSprite.draw(batch)
        bText.draw(batch)
        batch.end()
    }


    override fun pause() {
        audioMan.pauseAllSound()
        audioMan.pauseMusic(null)
    }

    override fun resume() {
        audioMan.resumeAllSound()
        audioMan.playMusic(null, true)
    }

    override fun hide() = audioMan.stopMusic(null)
}

