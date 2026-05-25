package com.megaman.maverick.game.screens.other

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Queue
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.*
import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.common.objects.GamePair
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.screens.BaseScreen
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.drawables.fonts.MegaFontHandle
import com.megaman.maverick.game.entities.bosses.*
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.utils.extensions.getDefaultCameraPosition
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.utils.interfaces.IShapeDebuggable
import java.util.*

class RobotMasterIntroScreenV2(private val game: MegamanMaverickGame) : BaseScreen(), Initializable, IShapeDebuggable {

    companion object {
        private const val TAG = "RobotMasterIntroScreenV2"

        private const val STAGE_START_REG_KEY = "stage_start"
        private const val STAGE_START_FLASH_REG_KEY = "stage_start_flash"
        private const val EXPLOSION_ORB_REG_KEY = "ExplosionOrb"

        private const val TOTAL_DUR = 7f
        private const val ORB_CONVERGE_DUR = 1f
        private const val FLASH_DUR = 1f
        private const val LETTER_DELAY = 0.2f

        private const val NUM_ORBS = 8
        private const val ORB_SIZE = 3f

        // How many full revolutions each orb completes while spiraling inward
        private const val ORB_REVOLUTIONS = 0.75f

        private const val SCREEN_WIDTH_PX = ConstVals.VIEW_WIDTH * ConstVals.PPM
        private const val SCREEN_HEIGHT_PX = ConstVals.VIEW_HEIGHT * ConstVals.PPM
        private val CENTER_PX = Vector2(SCREEN_WIDTH_PX / 2f, SCREEN_HEIGHT_PX / 2f)

        private val STD_END_POS = Vector2(
            ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f,
            (ConstVals.VIEW_HEIGHT * ConstVals.PPM / 2f) - 1f * ConstVals.PPM
        )

        private const val DEBUG_SHAPES = false
    }

    private enum class Phase { ORBS_CONVERGE, FLASH, NORMAL }

    private data class RobotMasterAnimDef(
        val fullText: String,
        val spriteSize: Vector2,
        val animQueue: Queue<GamePair<IAnimation, Timer?>>,
        val shadowRegion: TextureRegion?,
        val startPosition: Vector2,
        val endPosition: Vector2
    )

    private val uiCam = game.getUiCamera()
    private val audioMan = game.audioMan

    // Background
    private val backgroundSprite = GameSprite()

    // Orbs
    private val orbSprites = Array<GameSprite>()
    private val orbStartPositions = Array<Vector2>()
    private val orbOutPositions = Array<Vector2>()
    private val orbInitialAngles = FloatArray(NUM_ORBS)
    private val orbInitialRadii = FloatArray(NUM_ORBS)
    private lateinit var orbAnimation: IAnimation
    private val orbSize = ORB_SIZE * ConstVals.PPM

    // Flash overlay
    private val flashSprite = GameSprite()
    private lateinit var flashAnimation: IAnimation

    // Shadow boss sprite
    private val shadowBossSprite = GameSprite()

    // Normal boss sprite + anim queue
    private val bossSprite = GameSprite()
    private val bossAnimQ = Queue<GamePair<IAnimation, Timer?>>()

    // Text
    private lateinit var textHandle: MegaFontHandle
    private val lettersQ = LinkedList<Runnable>()
    private val letterDelay = Timer(LETTER_DELAY)

    // Boss end position
    private val endPosition = Vector2()

    // Phase management
    private var phase = Phase.ORBS_CONVERGE
    private val totalTimer = Timer(TOTAL_DUR)
    private val orbTimer = Timer(ORB_CONVERGE_DUR)
    private val flashTimer = Timer(FLASH_DUR)

    private var initialized = false

    override fun init(vararg params: Any) {
        if (initialized) {
            GameLogger.error(TAG, "init(): already initialized")
            return
        }

        GameLogger.debug(TAG, "init()")
        initialized = true

        // Background sprite: fills the full screen
        val bgRegion = game.assMan.getTextureRegion(TextureAsset.UI_1.source, STAGE_START_REG_KEY)
        backgroundSprite.setRegion(bgRegion)
        backgroundSprite.setBounds(
            0f,
            0f,
            SCREEN_WIDTH_PX,
            SCREEN_HEIGHT_PX
        )

        // Flash sprite: same full-screen bounds; 2 rows × 1 col animation
        val flashRegion = game.assMan.getTextureRegion(TextureAsset.UI_1.source, STAGE_START_FLASH_REG_KEY)
        flashAnimation = Animation(flashRegion, 2, 1, 0.05f, true)
        flashSprite.setBounds(
            0f,
            0f,
            SCREEN_WIDTH_PX,
            SCREEN_HEIGHT_PX
        )

        // Orb animation: 1 row × 2 cols, matches ExplosionOrb entity
        val orbRegion = game.assMan.getTextureRegion(TextureAsset.EXPLOSIONS_1.source, EXPLOSION_ORB_REG_KEY)
        orbAnimation = Animation(orbRegion, 1, 2, 0.075f, true)

        // Orb start positions (UI pixel space; center = 256, 224 for 16×14 @ PPM=32)
        // Half orb size offset so they start just off-screen
        val halfOrb = orbSize / 2f
        val startPositionData = arrayOf(
            Vector2(-halfOrb, -halfOrb),               // 0: bottom-left corner
            Vector2(SCREEN_WIDTH_PX + halfOrb, -halfOrb),         // 1: bottom-right corner
            Vector2(-halfOrb, SCREEN_HEIGHT_PX + halfOrb),         // 2: top-left corner
            Vector2(SCREEN_WIDTH_PX + halfOrb, SCREEN_HEIGHT_PX + halfOrb), // 3: top-right corner
            Vector2(CENTER_PX.x, -halfOrb),            // 4: bottom center
            Vector2(CENTER_PX.x, SCREEN_HEIGHT_PX + halfOrb),     // 5: top center
            Vector2(-halfOrb, CENTER_PX.y),             // 6: left center
            Vector2(SCREEN_WIDTH_PX + halfOrb, CENTER_PX.y)       // 7: right center
        )

        for (i in 0 until NUM_ORBS) {
            val orb = GameSprite()
            orb.setSize(orbSize)
            orbSprites.add(orb)

            val startPos = startPositionData[i].cpy()
            orbStartPositions.add(startPos)
            orbOutPositions.add(Vector2())

            orbInitialAngles[i] = MathUtils.atan2(startPos.y - CENTER_PX.y, startPos.x - CENTER_PX.x)
            orbInitialRadii[i] = startPos.dst(CENTER_PX)
        }

        textHandle = MegaFontHandle(
            positionX = ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f,
            positionY = ConstVals.VIEW_HEIGHT * ConstVals.PPM / 4f
        )
    }

    override fun show() {
        if (!initialized) init()

        GameLogger.debug(TAG, "show()")

        val level = game.getCurrentLevel()
        val (fullText, spriteSize, animQueue, shadowRegion, _, endPos) = buildRobotMasterAnimDef(level)

        // Populate boss anim queue
        animQueue.forEach { bossAnimQ.addLast(it) }

        // Boss sprite — placed at final position immediately
        bossSprite.setSize(spriteSize)
        bossSprite.setPosition(endPos, Position.BOTTOM_CENTER)
        endPosition.set(endPos)

        // Shadow boss sprite
        shadowBossSprite.setSize(spriteSize)
        shadowBossSprite.setPosition(endPos, Position.BOTTOM_CENTER)
        shadowBossSprite.hidden = (shadowRegion == null)
        if (shadowRegion != null) shadowBossSprite.setRegion(shadowRegion)

        // Text
        textHandle.setText("")
        for ((i, element) in fullText.withIndex()) {
            lettersQ.addLast {
                val char = element
                if (Character.isWhitespace(char)) return@addLast
                val text = fullText.subSequence(0, i + 1).toString()
                textHandle.setText(text)
            }
        }

        // Phase setup
        phase = Phase.ORBS_CONVERGE
        totalTimer.reset()
        orbTimer.reset()
        flashTimer.reset()
        letterDelay.reset()

        val camPos = getDefaultCameraPosition()
        uiCam.position.set(camPos)

        audioMan.playMusic(MusicAsset.MM2_BOSS_INTRO_MUSIC, false)
    }

    override fun render(delta: Float) {
        if (totalTimer.isFinished()) {
            game.startLevel()
            return
        }

        if (game.paused) return

        totalTimer.update(delta)

        when (phase) {
            Phase.ORBS_CONVERGE -> {
                orbTimer.update(delta)
                orbAnimation.update(delta)

                val ratio = orbTimer.getRatio()
                val currentRegion = orbAnimation.getCurrentRegion()
                for (i in 0 until NUM_ORBS) {
                    // Spiral inward: radius shrinks to zero, angle advances by ORB_REVOLUTIONS turns
                    val currentRadius = orbInitialRadii[i] * (1f - ratio)
                    val currentAngle = orbInitialAngles[i] + ORB_REVOLUTIONS * ratio * MathUtils.PI2
                    val outPos = orbOutPositions[i]
                    outPos.set(
                        CENTER_PX.x + MathUtils.cos(currentAngle) * currentRadius,
                        CENTER_PX.y + MathUtils.sin(currentAngle) * currentRadius
                    )
                    orbSprites[i].setCenter(outPos)
                    orbSprites[i].setRegion(currentRegion)
                }

                if (orbTimer.isFinished()) {
                    phase = Phase.FLASH
                    flashTimer.reset()
                }
            }

            Phase.FLASH -> {
                flashTimer.update(delta)
                flashAnimation.update(delta)
                flashSprite.setRegion(flashAnimation.getCurrentRegion())

                if (flashTimer.isFinished()) {
                    phase = Phase.NORMAL
                    letterDelay.reset()
                }
            }

            Phase.NORMAL -> {
                // Boss animation queue
                val (animation, timer) = bossAnimQ.first()
                animation.update(delta)
                bossSprite.setRegion(animation.getCurrentRegion())
                timer?.update(delta)
                if (timer?.isFinished() == true && bossAnimQ.size > 1) bossAnimQ.removeFirst()

                // Text reveal
                if (!lettersQ.isEmpty()) {
                    letterDelay.update(delta)
                    if (letterDelay.isFinished()) {
                        lettersQ.pollFirst().run()
                        letterDelay.reset()
                    }
                }
            }
        }
    }

    override fun draw(drawer: Batch) {
        val batch = game.batch
        batch.begin()

        game.viewports[ConstKeys.UI].apply()
        batch.projectionMatrix = uiCam.combined

        // Background always drawn first
        backgroundSprite.draw(batch)

        when (phase) {
            Phase.ORBS_CONVERGE -> {
                orbSprites.forEach { it.draw(batch) }
            }

            Phase.FLASH -> {
                flashSprite.draw(batch)
                shadowBossSprite.draw(batch)
            }

            Phase.NORMAL -> {
                bossSprite.draw(batch)
                textHandle.draw(batch)
            }
        }

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
                Animation(atlas.findRegion("${TimberWoman.TAG}/jump_spin"), 4, 2, 0.025f, true) pairTo Timer(ORB_CONVERGE_DUR)
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

            val shadowRegion = atlas.findRegion("${TimberWoman.TAG}/jump_spin_shadow")

            RobotMasterAnimDef(
                "TIMBER WOMAN",
                Vector2().set(TimberWoman.SPRITE_WIDTH * ConstVals.PPM),
                animQueue,
                shadowRegion,
                STD_END_POS,
                STD_END_POS
            )
        }

        LevelDefinition.MOON_MAN -> {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_2.source)

            val animQueue = Queue<GamePair<IAnimation, Timer?>>()
            animQueue.addLast(
                Animation(atlas.findRegion("${MoonMan.TAG}/jump")) pairTo Timer(ORB_CONVERGE_DUR)
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

            val shadowRegion = atlas.findRegion("${MoonMan.TAG}/jump_shadow")

            RobotMasterAnimDef(
                "LUNAR MAN",
                Vector2().set(MoonMan.SPRITE_SIZE * ConstVals.PPM),
                animQueue,
                shadowRegion,
                STD_END_POS,
                STD_END_POS
            )
        }

        LevelDefinition.RODENT_MAN -> {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_3.source)

            val animQueue = Queue<GamePair<IAnimation, Timer?>>()
            animQueue.addLast(
                Animation(
                    atlas.findRegion("${RodentMan.TAG}/jump_down_look_down")
                ) pairTo Timer(ORB_CONVERGE_DUR)
            )
            animQueue.addLast(
                Animation(
                    atlas.findRegion("${RodentMan.TAG}/stand"), 2, 1, gdxArrayOf(1f, 0.15f), true
                ) pairTo Timer(0.5f)
            )
            animQueue.addLast(
                Animation(
                    atlas.findRegion("${RodentMan.TAG}/stand_slash_combo"), 4, 3, 0.05f, false
                ) pairTo Timer(0.75f)
            )
            animQueue.addLast(
                Animation(
                    atlas.findRegion("${RodentMan.TAG}/stand_slash_combo"), 4, 3, 0.05f, false
                ) pairTo Timer(0.75f)
            )
            animQueue.addLast(
                Animation(
                    atlas.findRegion("${RodentMan.TAG}/stand"), 2, 1, gdxArrayOf(1f, 0.15f), true
                ) pairTo null
            )

            val shadowRegion = atlas.findRegion("${RodentMan.TAG}/jump_down_look_down_shadow")

            RobotMasterAnimDef(
                "RODENT MAN",
                Vector2().set(RodentMan.SPRITE_WIDTH, RodentMan.SPRITE_HEIGHT).scl(ConstVals.PPM.toFloat()),
                animQueue,
                shadowRegion,
                STD_END_POS,
                STD_END_POS
            )
        }

        LevelDefinition.DESERT_MAN -> {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_2.source)

            val animQueue = Queue<GamePair<IAnimation, Timer?>>()
            animQueue.addLast(
                Animation(atlas.findRegion("${DesertMan.TAG}/jump")) pairTo Timer(ORB_CONVERGE_DUR)
            )
            animQueue.addLast(
                Animation(atlas.findRegion("${DesertMan.TAG}/tornado"), 2, 1, 0.1f, true) pairTo Timer(0.5f)
            )
            animQueue.addLast(
                Animation(atlas.findRegion("${DesertMan.TAG}/tornado_punch"), 2, 2, 0.1f, true) pairTo Timer(0.5f)
            )
            animQueue.addLast(
                Animation(atlas.findRegion("${DesertMan.TAG}/jump")) pairTo Timer(0.1f)
            )
            animQueue.addLast(
                Animation(atlas.findRegion("${DesertMan.TAG}/stand"), 2, 1, gdxArrayOf(1f, 0.15f), true) pairTo null
            )

            val shadowRegion = atlas.findRegion("${DesertMan.TAG}/jump_shadow")

            RobotMasterAnimDef(
                "DESERT MAN",
                Vector2().set(DesertMan.SPRITE_SIZE * ConstVals.PPM),
                animQueue,
                shadowRegion,
                STD_END_POS,
                STD_END_POS
            )
        }

        LevelDefinition.INFERNO_MAN -> {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_2.source)

            val animQueue = Queue<GamePair<IAnimation, Timer?>>()
            animQueue.addLast(
                Animation(atlas.findRegion("${InfernoMan.TAG}/jump_down")) pairTo Timer(ORB_CONVERGE_DUR)
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

            val shadowRegion = atlas.findRegion("${InfernoMan.TAG}/jump_down_shadow")

            RobotMasterAnimDef(
                "INFERNO MAN",
                Vector2().set(InfernoMan.SPRITE_SIZE * ConstVals.PPM),
                animQueue,
                shadowRegion,
                STD_END_POS,
                STD_END_POS
            )
        }

        LevelDefinition.REACTOR_MAN -> {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_1.source)

            val animQueue = Queue<GamePair<IAnimation, Timer?>>()
            animQueue.addLast(
                Animation(atlas.findRegion("${ReactorMan.TAG}/jump")) pairTo Timer(ORB_CONVERGE_DUR)
            )
            animQueue.addLast(
                Animation(atlas.findRegion("${ReactorMan.TAG}/giga_stand"), 2, 1, 0.1f, true) pairTo Timer(1f)
            )
            animQueue.addLast(
                Animation(atlas.findRegion("${ReactorMan.TAG}/stand_throw_two"), 3, 1, 0.1f, false) pairTo Timer(0.5f)
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

            val shadowRegion = atlas.findRegion("${ReactorMan.TAG}/jump_shadow")

            RobotMasterAnimDef(
                "REACTOR MAN",
                Vector2().set(ReactorMan.SPRITE_SIZE * ConstVals.PPM),
                animQueue,
                shadowRegion,
                STD_END_POS,
                STD_END_POS
            )
        }

        LevelDefinition.GLACIER_MAN -> {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_1.source)

            val animQueue = Queue<GamePair<IAnimation, Timer?>>()
            animQueue.addLast(
                Animation(atlas.findRegion("${GlacierMan.TAG}/fall"), 4, 2, 0.1f, true) pairTo Timer(ORB_CONVERGE_DUR)
            )
            animQueue.addLast(
                Animation(atlas.findRegion("${GlacierMan.TAG}/brake"), 3, 1, 0.1f, true) pairTo Timer(0.6f)
            )
            animQueue.addLast(
                Animation(atlas.findRegion("${GlacierMan.TAG}/stop"), 3, 3, 0.1f, false) pairTo Timer(1f)
            )
            animQueue.addLast(
                Animation(atlas.findRegion("${GlacierMan.TAG}/stand")) pairTo null
            )

            val shadowRegion = atlas.findRegion("${GlacierMan.TAG}/fall_shadow")

            RobotMasterAnimDef(
                "GLACIER MAN",
                Vector2().set(GlacierMan.SPRITE_WIDTH, GlacierMan.SPRITE_HEIGHT).scl(ConstVals.PPM.toFloat()),
                animQueue,
                shadowRegion,
                STD_END_POS,
                STD_END_POS
            )
        }

        LevelDefinition.PRECIOUS_WOMAN -> {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_3.source)

            val animQueue = Queue<GamePair<IAnimation, Timer?>>()
            animQueue.addLast(
                Animation(atlas.findRegion("${PreciousWoman.TAG}/jump")) pairTo Timer(ORB_CONVERGE_DUR)
            )
            animQueue.addLast(
                Animation(
                    atlas.findRegion("${PreciousWoman.TAG}/stand"),
                    2,
                    1,
                    gdxArrayOf(1f, 0.15f),
                    true
                ) pairTo Timer(0.5f)
            )
            animQueue.addLast(
                Animation(atlas.findRegion("${PreciousWoman.TAG}/wink"), 3, 3, 0.1f, false) pairTo null
            )

            val shadowRegion = atlas.findRegion("${PreciousWoman.TAG}/jump_shadow")

            RobotMasterAnimDef(
                "PRECIOUS WOMAN",
                Vector2().set(PreciousWoman.SPRITE_SIZE * ConstVals.PPM),
                animQueue,
                shadowRegion,
                STD_END_POS,
                STD_END_POS
            )
        }

        else -> throw IllegalStateException("$TAG: no anim def for level: $level")
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
        phase = Phase.ORBS_CONVERGE
        orbTimer.reset()
        flashTimer.reset()
    }

    override fun dispose() {
        GameLogger.debug(TAG, "dispose()")
        audioMan.stopMusic()
    }
}
