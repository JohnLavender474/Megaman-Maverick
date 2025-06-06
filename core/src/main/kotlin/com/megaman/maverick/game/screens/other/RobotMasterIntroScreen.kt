package com.megaman.maverick.game.screens.other

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Queue
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.*
import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.common.objects.GamePair
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.screens.BaseScreen
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.drawables.backgrounds.ScrollingStars
import com.megaman.maverick.game.drawables.fonts.MegaFontHandle
import com.megaman.maverick.game.entities.bosses.*
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.utils.extensions.getDefaultCameraPosition
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.utils.interfaces.IShapeDebuggable
import java.util.*

class RobotMasterIntroScreen(private val game: MegamanMaverickGame) : BaseScreen(), Initializable, IShapeDebuggable {

    companion object {
        private const val TAG = "RobotMasterIntroScreen"

        private const val BAR_REG_KEY = "Bar"

        private const val TOTAL_DUR = 7f
        private const val DROP_DUR = 0.25f
        private const val LETTER_DELAY = 0.2f

        private val STD_START_POS = Vector2(
            ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f,
            ConstVals.VIEW_HEIGHT * ConstVals.PPM
        )
        private val STD_END_POS = Vector2(
            ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f,
            (ConstVals.VIEW_HEIGHT * ConstVals.PPM / 2f) - 1f * ConstVals.PPM
        )

        private const val NUM_STARS_N_BARS = 4

        private const val DEBUG_SHAPES = false
    }

    private data class RobotMasterAnimDef(
        val fullText: String,
        val spriteSize: Vector2,
        val animQueue: Queue<GamePair<IAnimation, Timer?>>,
        val startPosition: Vector2,
        val endPosition: Vector2
    )

    private val uiCam = game.getUiCamera()
    private val audioMan = game.audioMan

    private val bossSprite = GameSprite()
    private val bossAnimQ = Queue<GamePair<IAnimation, Timer?>>()

    private val startPosition = Vector2()
    private val endPosition = Vector2()
    private val outPosition = Vector2()

    private lateinit var textHandle: MegaFontHandle

    private val bars = Array<GameSprite>()
    private lateinit var barAnim: IAnimation

    private lateinit var scrollingStars: ScrollingStars

    private val totalTimer = Timer(TOTAL_DUR)
    private val dropTimer = Timer(DROP_DUR)

    private val letterDelay = Timer(LETTER_DELAY)
    private val lettersQ = LinkedList<Runnable>()

    private var initialized = false

    override fun init() {
        if (initialized) {
            GameLogger.error(TAG, "init(): already initialized")
            return
        }

        GameLogger.debug(TAG, "init()")

        initialized = true

        val barReg = game.assMan.getTextureRegion(TextureAsset.UI_1.source, BAR_REG_KEY)
        barAnim = Animation(barReg, 1, 4, Array.with(0.3f, 0.15f, 0.15f, 0.15f), true)

        for (i in 0 until NUM_STARS_N_BARS) {
            val bar = GameSprite()
            bar.setBounds(
                (i * ConstVals.VIEW_WIDTH * ConstVals.PPM / 3f) - 5f,
                ConstVals.VIEW_HEIGHT * ConstVals.PPM / 3f,
                (ConstVals.VIEW_WIDTH * ConstVals.PPM / 3f) + 5f,
                ConstVals.VIEW_HEIGHT * ConstVals.PPM / 3f
            )
            bars.add(bar)
        }

        scrollingStars = ScrollingStars(
            game,
            Vector2(
                -ScrollingStars.COLS * ScrollingStars.WIDTH * ConstVals.PPM / 2f,
                -ScrollingStars.ROWS * ScrollingStars.HEIGHT * ConstVals.PPM / 2f
            )
        )
        val pos = getDefaultCameraPosition()
        scrollingStars.set(pos.x, pos.y)

        textHandle = MegaFontHandle(
            positionX = ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f,
            positionY = ConstVals.VIEW_HEIGHT * ConstVals.PPM / 4f
        )
    }

    override fun show() {
        if (!initialized) init()

        GameLogger.debug(TAG, "show()")

        val level = game.getCurrentLevel()
        val (fullText, spriteSize, animQueue, startPosition, endPosition) = buildRobotMasterAnimDef(level)

        animQueue.forEach { bossAnimQ.addLast(it) }

        bossSprite.setSize(spriteSize)
        bossSprite.setPosition(startPosition, Position.BOTTOM_CENTER)

        this.startPosition.set(startPosition)
        this.endPosition.set(endPosition)

        textHandle.setText("")
        for (i in 0 until fullText.length) {
            lettersQ.addLast {
                val char = fullText[i]

                if (Character.isWhitespace(char)) return@addLast

                val text = fullText.subSequence(0, i + 1).toString()
                textHandle.setText(text)

                audioMan.playSound(SoundAsset.THUMP_SOUND, false)
            }
        }

        dropTimer.reset()
        totalTimer.reset()
        letterDelay.reset()

        uiCam.position.set(getDefaultCameraPosition())

        audioMan.playMusic(MusicAsset.MM2_BOSS_INTRO_MUSIC, false)
    }

    override fun render(delta: Float) {
        if (totalTimer.isFinished()) {
            game.startLevel()
            return
        }

        if (!game.paused) {
            totalTimer.update(delta)

            scrollingStars.update(delta)

            barAnim.update(delta)
            bars.forEach { bar -> bar.setRegion(barAnim.getCurrentRegion()) }

            dropTimer.update(delta)
            if (dropTimer.isFinished()) {
                bossSprite.setPosition(endPosition, Position.BOTTOM_CENTER)

                if (!lettersQ.isEmpty()) {
                    letterDelay.update(delta)

                    if (letterDelay.isFinished()) {
                        lettersQ.pollFirst().run()
                        letterDelay.reset()
                    }
                }
            } else {
                val position = UtilMethods.interpolate(startPosition, endPosition, dropTimer.getRatio(), outPosition)
                bossSprite.setPosition(position, Position.BOTTOM_CENTER)
            }

            val (animation, timer) = bossAnimQ.first()

            animation.update(delta)
            bossSprite.setRegion(animation.getCurrentRegion())

            timer?.update(delta)
            if (timer?.isFinished() == true && bossAnimQ.size > 1) bossAnimQ.removeFirst()
        }
    }

    override fun draw(drawer: Batch) {
        val batch = game.batch
        batch.begin()

        // scrolling stars uses its own internal viewport, so render it before applying the UI viewport
        scrollingStars.draw(batch)

        game.viewports[ConstKeys.UI].apply()
        batch.projectionMatrix = uiCam.combined
        bars.forEach { it.draw(batch) }
        bossSprite.draw(batch)
        textHandle.draw(batch)

        batch.end()
    }

    override fun draw(renderer: ShapeRenderer) {
        if (DEBUG_SHAPES) {
            renderer.projectionMatrix = uiCam.combined
            renderer.rect(bossSprite.boundingRectangle.toGameRectangle())
            textHandle.draw(renderer)
        }
    }

    private fun buildRobotMasterAnimDef(level: LevelDefinition): RobotMasterAnimDef = when (level) {
        LevelDefinition.TIMBER_WOMAN -> {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_2.source)

            val animQueue = Queue<GamePair<IAnimation, Timer?>>()
            animQueue.addLast(
                Animation(atlas.findRegion("${TimberWoman.TAG}/jump_spin"), 4, 2, 0.025f, true) pairTo Timer(DROP_DUR)
            )
            animQueue.addLast(
                Animation(atlas.findRegion("${TimberWoman.TAG}/init"), 7, 1, 0.1f, false) pairTo Timer(0.7f)
            )
            animQueue.addLast(
                Animation(atlas.findRegion("${TimberWoman.TAG}/stand_pound"), 3, 2, 0.1f, true) pairTo Timer(2.4f)
            )
            animQueue.addLast(
                Animation(atlas.findRegion("${TimberWoman.TAG}/init"), 7, 1, 0.1f, false) pairTo Timer(0.7f)
            )
            animQueue.addLast(
                Animation(
                    atlas.findRegion("${TimberWoman.TAG}/stand_still"),
                    2,
                    1,
                    gdxArrayOf(1f, 0.15f),
                    true
                ) pairTo null
            )

            RobotMasterAnimDef(
                "TIMBER WOMAN",
                Vector2().set(TimberWoman.SPRITE_SIZE * ConstVals.PPM),
                animQueue,
                STD_START_POS,
                STD_END_POS
            )
        }
        LevelDefinition.MOON_MAN -> {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_2.source)

            val animQueue = Queue<GamePair<IAnimation, Timer?>>()
            animQueue.addLast(
                Animation(atlas.findRegion("${MoonMan.TAG}/jump")) pairTo Timer(DROP_DUR)
            )
            animQueue.addLast(
                Animation(atlas.findRegion("${MoonMan.TAG}/jump_land"), 2, 1, 0.1f, false) pairTo Timer(0.2f)
            )
            animQueue.addLast(
                Animation(atlas.findRegion("${MoonMan.TAG}/shoot", 0), 2, 2, 0.1f, false) pairTo Timer(0.4f)
            )
            animQueue.addLast(
                Animation(atlas.findRegion("${MoonMan.TAG}/shoot", 1), 3, 1, 0.1f, false) pairTo Timer(0.3f)
            )
            animQueue.addLast(
                Animation(atlas.findRegion("${MoonMan.TAG}/shoot", 2), 3, 1, 0.1f, false) pairTo Timer(0.3f)
            )
            animQueue.addLast(
                Animation(atlas.findRegion("${MoonMan.TAG}/shoot", 3), 2, 1, 0.1f, false) pairTo Timer(0.3f)
            )
            animQueue.addLast(
                Animation(atlas.findRegion("${MoonMan.TAG}/throw")) pairTo Timer(0.2f)
            )
            animQueue.addLast(
                Animation(atlas.findRegion("${MoonMan.TAG}/stand"), 2, 1, gdxArrayOf(1f, 0.15f), true) pairTo null
            )

            RobotMasterAnimDef(
                "MOON MAN",
                Vector2().set(MoonMan.SPRITE_SIZE * ConstVals.PPM),
                animQueue,
                STD_START_POS,
                STD_END_POS
            )
        }
        LevelDefinition.RODENT_MAN -> {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_3.source)

            val animQueue = Queue<GamePair<IAnimation, Timer?>>()
            animQueue.addLast(
                Animation(atlas.findRegion("${RodentMan.TAG}/jump_down_look_down")) pairTo Timer(DROP_DUR)
            )
            animQueue.addLast(
                Animation(atlas.findRegion("${RodentMan.TAG}/shielded"), 2, 1, 0.1f, true) pairTo Timer(0.5f)
            )
            animQueue.addLast(
                Animation(atlas.findRegion("${RodentMan.TAG}/jump_slash"), 2, 1, 0.1f, false) pairTo Timer(0.2f)
            )
            animQueue.addLast(
                Animation(atlas.findRegion("${RodentMan.TAG}/run"), 1, 5, 0.1f, true) pairTo Timer(1f)
            )
            animQueue.addLast(
                Animation(atlas.findRegion("${RodentMan.TAG}/stand"), 2, 1, gdxArrayOf(1f, 0.15f), true) pairTo null
            )

            RobotMasterAnimDef(
                "RODENT MAN",
                Vector2().set(RodentMan.SPRITE_WIDTH, RodentMan.SPRITE_HEIGHT).scl(ConstVals.PPM.toFloat()),
                animQueue,
                STD_START_POS,
                STD_END_POS
            )
        }
        LevelDefinition.DESERT_MAN -> {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_2.source)

            val animQueue = Queue<GamePair<IAnimation, Timer?>>()
            animQueue.addLast(
                Animation(atlas.findRegion("${DesertMan.TAG}/jump")) pairTo Timer(DROP_DUR)
            )
            animQueue.addLast(
                Animation(atlas.findRegion("${DesertMan.TAG}/dance"), 2, 1, 0.1f, true) pairTo Timer(0.5f)
            )
            animQueue.addLast(
                Animation(atlas.findRegion("${DesertMan.TAG}/dance_flash"), 2, 2, 0.1f, true) pairTo Timer(0.5f)
            )
            animQueue.addLast(
                Animation(atlas.findRegion("${DesertMan.TAG}/tornado"), 2, 1, 0.1f, true) pairTo Timer(0.5f)
            )
            animQueue.addLast(
                Animation(atlas.findRegion("${DesertMan.TAG}/tornado_punch"), 2, 2, 0.1f, true) pairTo Timer(1f)
            )
            animQueue.addLast(
                Animation(atlas.findRegion("${DesertMan.TAG}/stand"), 2, 1, gdxArrayOf(1f, 0.15f), true) pairTo null
            )

            RobotMasterAnimDef(
                "DESERT MAN",
                Vector2().set(DesertMan.SPRITE_SIZE * ConstVals.PPM),
                animQueue,
                STD_START_POS,
                STD_END_POS
            )
        }
        LevelDefinition.INFERNO_MAN -> {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_2.source)

            val animQueue = Queue<GamePair<IAnimation, Timer?>>()
            animQueue.addLast(
                Animation(atlas.findRegion("${InfernoMan.TAG}/jump_down")) pairTo Timer(DROP_DUR)
            )
            animQueue.addLast(
                Animation(atlas.findRegion("${InfernoMan.TAG}/init"), 3, 3, 0.1f, false) pairTo Timer(1.5f)
            )
            animQueue.addLast(
                Animation(atlas.findRegion("${InfernoMan.TAG}/stand_shoot_mega"), 3, 2, 0.1f, true) pairTo Timer(1.5f)
            )
            animQueue.addLast(
                Animation(atlas.findRegion("${InfernoMan.TAG}/stand"), 2, 1, gdxArrayOf(1f, 0.15f), true) pairTo null
            )

            RobotMasterAnimDef(
                "INFERNO MAN",
                Vector2().set(InfernoMan.SPRITE_SIZE * ConstVals.PPM),
                animQueue,
                STD_START_POS,
                STD_END_POS
            )
        }
        LevelDefinition.REACTOR_MAN -> {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_1.source)

            val animQueue = Queue<GamePair<IAnimation, Timer?>>()
            animQueue.addLast(
                Animation(atlas.findRegion("${ReactorMan.TAG}/jump")) pairTo Timer(DROP_DUR)
            )
            animQueue.addLast(
                Animation(atlas.findRegion("${ReactorMan.TAG}/jump_throw"), 3, 2, 0.1f, false) pairTo Timer(0.6f)
            )
            animQueue.addLast(
                Animation(atlas.findRegion("${ReactorMan.TAG}/stand_throw_two"), 3, 1, 0.1f, false) pairTo Timer(0.3f)
            )
            animQueue.addLast(
                Animation(atlas.findRegion("${ReactorMan.TAG}/giga_stand"), 2, 1, 0.1f, true) pairTo Timer(1f)
            )
            animQueue.addLast(
                Animation(
                    atlas.findRegion("${ReactorMan.TAG}/stand"),
                    1,
                    3,
                    gdxArrayOf(0.5f, 0.15f, 0.15f),
                    true
                ) pairTo null
            )

            RobotMasterAnimDef(
                "REACTOR MAN",
                Vector2().set(ReactorMan.SPRITE_SIZE * ConstVals.PPM),
                animQueue,
                STD_START_POS,
                STD_END_POS
            )
        }
        LevelDefinition.GLACIER_MAN -> {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_1.source)

            val animQueue = Queue<GamePair<IAnimation, Timer?>>()
            animQueue.addLast(
                Animation(atlas.findRegion("${GlacierMan.TAG}/fall"), 4, 2, 0.1f, true) pairTo Timer(DROP_DUR)
            )
            animQueue.addLast(
                Animation(atlas.findRegion("${GlacierMan.TAG}/brake"), 3, 1, 0.1f, true) pairTo Timer(0.6f)
            )
            animQueue.addLast(
                Animation(atlas.findRegion("${GlacierMan.TAG}/stop"), 3, 3, 0.1f, false) pairTo Timer(1f)
            )
            animQueue.addLast(
                Animation(atlas.findRegion("${GlacierMan.TAG}/ice_blast_attack"), 2, 1, 0.1f, true) pairTo Timer(0.5f)
            )
            animQueue.addLast(
                Animation(atlas.findRegion("${GlacierMan.TAG}/stand")) pairTo null
            )

            RobotMasterAnimDef(
                "GLACIER MAN",
                Vector2().set(GlacierMan.SPRITE_WIDTH, GlacierMan.SPRITE_HEIGHT).scl(ConstVals.PPM.toFloat()),
                animQueue,
                STD_START_POS,
                STD_END_POS
            )
        }
        LevelDefinition.PRECIOUS_WOMAN -> {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_3.source)

            val animQueue = Queue<GamePair<IAnimation, Timer?>>()
            animQueue.addLast(
                Animation(atlas.findRegion("${PreciousWoman.TAG}/jump")) pairTo Timer(DROP_DUR)
            )
            animQueue.addLast(
                Animation(atlas.findRegion("${PreciousWoman.TAG}/stand_throw"), 5, 1, 0.1f, true) pairTo Timer(1f)
            )
            animQueue.addLast(
                Animation(atlas.findRegion("${PreciousWoman.TAG}/stand_laugh1"), 2, 1, 0.1f, true) pairTo Timer(0.5f)
            )
            animQueue.addLast(
                Animation(atlas.findRegion("${PreciousWoman.TAG}/stand_laugh2"), 2, 1, 0.1f, true) pairTo Timer(0.5f)
            )
            animQueue.addLast(
                Animation(atlas.findRegion("${PreciousWoman.TAG}/wink"), 3, 3, 0.1f, false) pairTo Timer(1f)
            )
            animQueue.addLast(
                Animation(atlas.findRegion("${PreciousWoman.TAG}/stand"), 2, 1, gdxArrayOf(1f, 0.15f), true) pairTo null
            )

            RobotMasterAnimDef(
                "PRECIOUS WOMAN",
                Vector2().set(PreciousWoman.SPRITE_SIZE * ConstVals.PPM),
                animQueue,
                STD_START_POS,
                STD_END_POS
            )
        }
        else -> throw IllegalStateException("No boss anim def for level: $level")
    }

    override fun pause() {
        GameLogger.debug(TAG, "pause()")
        audioMan.pauseAllSound()
        audioMan.pauseMusic(null)
    }

    override fun resume() {
        GameLogger.debug(TAG, "resume()")
        audioMan.resumeAllSound()
        audioMan.playMusic(null, true)
    }

    override fun reset() {
        GameLogger.debug(TAG, "reset()")
        bossAnimQ.clear()
        lettersQ.clear()
    }

    override fun dispose() {
        GameLogger.debug(TAG, "dispose()")
        audioMan.stopMusic()
    }
}
