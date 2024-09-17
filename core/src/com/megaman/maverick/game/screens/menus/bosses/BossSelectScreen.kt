package com.megaman.maverick.game.screens.menus.bosses

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.Position.Companion.get
import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.common.time.TimeMarkedRunnable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.fonts.BitmapFontHandle
import com.mega.game.engine.screens.menus.IMenuButton
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.bosses.BossType
import com.megaman.maverick.game.screens.ScreenEnum
import com.megaman.maverick.game.screens.menus.MegaMenuScreen
import com.megaman.maverick.game.screens.utils.BlinkingArrow
import com.megaman.maverick.game.screens.utils.ScreenSlide
import com.megaman.maverick.game.utils.MegaUtilMethods.getDefaultFontSize
import com.megaman.maverick.game.utils.getDefaultCameraPosition
import java.util.*
import java.util.function.Supplier

class BossSelectScreen(game: MegamanMaverickGame) : MegaMenuScreen(game, MEGA_MAN), Initializable {

    companion object {
        private val INTRO_BLOCKS_TRANS = Vector3(15f * ConstVals.PPM, 0f, 0f)
        private val CAM_POS = getDefaultCameraPosition().add(0f, 0.55f * ConstVals.PPM, 0f)
        private const val MEGA_MAN = "MEGA MAN"
        private const val BACK = "BACK"
    }

    private val bNameSet = ObjectSet<String>()
    private val slide = ScreenSlide(
        game.getUiCamera(),
        INTRO_BLOCKS_TRANS,
        CAM_POS.cpy().sub(INTRO_BLOCKS_TRANS),
        CAM_POS,
        0.5f,
        false
    )
    private val bar1 = Sprite()
    private val bar2 = Sprite()
    private val white = Sprite()
    private val outTimer = Timer(1.05f)
    private val t = Array<BitmapFontHandle>()
    private val bp = Array<BossPane>()
    private val bkgd = Array<Sprite>()
    private val bars = ObjectMap<Sprite, Animation>()
    private val bArrs = ObjectMap<String, BlinkingArrow>()
    private lateinit var bName: BitmapFontHandle
    private var outro = false
    private var blink = false
    private var bSelect: BossType? = null
    private var initialized = false

    override fun init() {
        if (initialized) return

        buttons.put(MEGA_MAN, object : IMenuButton {
            override fun onSelect(delta: Float) = false

            override fun onNavigate(direction: Direction, delta: Float): String? {
                return when (direction) {
                    Direction.UP -> BossType.findByPos(1, 2).name
                    Direction.DOWN -> BossType.findByPos(1, 0).name
                    Direction.LEFT -> BossType.findByPos(0, 1).name
                    Direction.RIGHT -> BossType.findByPos(2, 1).name
                }
            }
        })
        buttons.put(BACK, object : IMenuButton {
            override fun onSelect(delta: Float): Boolean {
                game.setCurrentScreen(ScreenEnum.MAIN_MENU_SCREEN.name)
                return true
            }

            override fun onNavigate(direction: Direction, delta: Float): String? {
                return when (direction) {
                    Direction.UP, Direction.LEFT, Direction.RIGHT -> BossType.findByPos(2, 0).name
                    Direction.DOWN -> BossType.findByPos(2, 2).name
                }
            }
        })
        for (boss in BossType.values()) {
            buttons.put(boss.name, object : IMenuButton {
                override fun onSelect(delta: Float): Boolean {
                    game.audioMan.playSound(SoundAsset.BEAM_OUT_SOUND, false)
                    game.audioMan.stopMusic(null)
                    bSelect = boss
                    outro = true
                    return true
                }

                override fun onNavigate(direction: Direction, delta: Float): String? {
                    var x = boss.position.x
                    var y = boss.position.y
                    when (direction) {
                        Direction.UP -> y += 1
                        Direction.DOWN -> y -= 1
                        Direction.LEFT -> x -= 1
                        Direction.RIGHT -> x += 1
                    }
                    if (y < 0 || y > 2) return BACK
                    if (x < 0) x = 2
                    if (x > 2) x = 0
                    val position = get(x, y)
                    return when (position) {
                        null -> throw IllegalStateException()
                        Position.CENTER -> MEGA_MAN
                        else -> BossType.findByPos(x, y).name
                    }
                }
            })
        }

        for (b in BossType.values()) bNameSet.add(b.name)
        val outTimerRunnable = Array<TimeMarkedRunnable>()
        for (i in 1..10) outTimerRunnable.add(TimeMarkedRunnable(.1f * i) { blink = !blink })
        outTimer.setRunnables(outTimerRunnable)
        val megamanFacesAtlas = game.assMan.get(TextureAsset.FACES_1.source, TextureAtlas::class.java)
        val megamanFaces: MutableMap<Position, TextureRegion> = EnumMap(Position::class.java)
        for (position in Position.values()) {
            val faceRegion: TextureRegion = megamanFacesAtlas.findRegion("Megaman/" + position.name)
            megamanFaces[position] = faceRegion
        }
        val megamanFaceSupplier = Supplier {
            val boss = BossType.findByName(currentButtonKey) ?: return@Supplier megamanFaces[Position.CENTER]
            megamanFaces[boss.position]
        }
        val megamanPane = BossPane(game, megamanFaceSupplier, MEGA_MAN, Position.CENTER)
        bp.add(megamanPane)
        for (boss in BossType.values()) bp.add(BossPane(game, boss))
        t.add(
            BitmapFontHandle(
                "PRESS START",
                getDefaultFontSize(),
                Vector2(5.35f * ConstVals.PPM, 13.85f * ConstVals.PPM),
                centerX = false,
                centerY = false,
                fontSource = "Megaman10Font.ttf"
            )
        )
        t.add(
            BitmapFontHandle(
                BACK, getDefaultFontSize(), Vector2(
                    12.35f * ConstVals.PPM, ConstVals.PPM.toFloat()
                ), false, centerY = false, fontSource = "Megaman10Font.ttf"
            )
        )
        bArrs.put(
            BACK, BlinkingArrow(
                game.assMan, Vector2(
                    12f * ConstVals.PPM, .75f * ConstVals.PPM
                )
            )
        )
        val stageSelectAtlas = game.assMan.get(TextureAsset.UI_1.source, TextureAtlas::class.java)
        val bar: TextureRegion = stageSelectAtlas.findRegion("Bar")
        for (i in 0..5) {
            for (j in 0..2) {
                val sprite = Sprite(bar)
                sprite.setBounds(
                    i * 3f * ConstVals.PPM,
                    (j * 4f * ConstVals.PPM) + 1.35f * ConstVals.PPM,
                    5.33f * ConstVals.PPM,
                    4f * ConstVals.PPM
                )
                val timedAnimation = Animation(bar, 1, 4, Array.with(.3f, .15f, .15f, .15f), true)
                bars.put(sprite, timedAnimation)
            }
        }
        val colorsAtlas = game.assMan.get(TextureAsset.COLORS.source, TextureAtlas::class.java)
        val whiteReg: TextureRegion = colorsAtlas.findRegion("White")
        white.setRegion(whiteReg)
        white.setBounds(0f, 0f, ConstVals.VIEW_WIDTH * ConstVals.PPM, ConstVals.VIEW_HEIGHT * ConstVals.PPM)
        val black: TextureRegion = colorsAtlas.findRegion("Black")
        bar1.setRegion(black)
        bar1.setBounds(
            -ConstVals.PPM.toFloat(),
            -ConstVals.PPM.toFloat(),
            (2f + ConstVals.VIEW_WIDTH) * ConstVals.PPM,
            2f * ConstVals.PPM
        )
        bar2.setRegion(black)
        bar2.setBounds(0f, 0f, 0.25f * ConstVals.PPM, (ConstVals.VIEW_HEIGHT + 1) * ConstVals.PPM)
        val tilesAtlas = game.assMan.get(TextureAsset.PLATFORMS_1.source, TextureAtlas::class.java)
        val blueBlockRegion: TextureRegion = tilesAtlas.findRegion("8bitBlueBlockTransBorder")
        val halfPPM = ConstVals.PPM / 2f
        var i = 0
        while (i < ConstVals.VIEW_WIDTH) {
            var j = 0
            while (j < ConstVals.VIEW_HEIGHT - 1) {
                for (x in 0..1) {
                    for (y in 0..1) {
                        val blueBlock = Sprite(blueBlockRegion)
                        blueBlock.setBounds(
                            i * ConstVals.PPM + (x * halfPPM), j * ConstVals.PPM + (y * halfPPM), halfPPM, halfPPM
                        )
                        bkgd.add(blueBlock)
                    }
                }
                j++
            }
            i++
        }
        bName = BitmapFontHandle(
            { "" }, getDefaultFontSize(), Vector2(
                ConstVals.PPM.toFloat(), ConstVals.PPM.toFloat()
            ), false, centerY = false, fontSource = "Megaman10Font.ttf"
        )
    }

    override fun show() {
        if (!initialized) {
            init()
            initialized = true
        }
        super.show()
        slide.init()
        outro = false
        outTimer.reset()
        game.audioMan.playMusic(MusicAsset.MM3_SNAKE_MAN_MUSIC, true)
    }

    override fun onAnyMovement(direction: Direction) =
        game.audioMan.playSound(SoundAsset.CURSOR_MOVE_BLOOP_SOUND, false)

    override fun render(delta: Float) {
        super.render(delta)
        val batch: Batch = game.batch
        if (!game.paused) {
            slide.update(delta)
            if (outro) outTimer.update(delta)
            if (outTimer.isFinished()) {
                /*
                TODO:
                val bIntroScreen = game.screens.get(ScreenEnum.BOSS_INTRO_SCREEN.name) as BossIntroScreen
                bIntroScreen.set(bSelect!!)
                game.setCurrentScreen(ScreenEnum.BOSS_INTRO_SCREEN.name)
                 */
                game.startLevelScreen(bSelect!!.level)
                return
            }
            for (e in bars) {
                e.value.update(delta)
                e.key.setRegion(e.value.getCurrentRegion())
            }
            for (b in bp) {
                b.bossPaneStat =
                    if (b.bossName == currentButtonKey) (if (selectionMade) BossPaneStat.HIGHLIGHTED else BossPaneStat.BLINKING)
                    else BossPaneStat.UNHIGHLIGHTED
                b.update(delta)
            }
            if (bArrs.containsKey(currentButtonKey)) bArrs.get(currentButtonKey).update(delta)
        }
        batch.projectionMatrix = game.getUiCamera().combined
        batch.begin()
        if (outro && blink) white.draw(batch)
        for (b in bkgd) b.draw(batch)
        for (e in bars) e.key.draw(batch)
        for (b in bp) b.draw(batch)
        bar1.draw(batch)
        bar2.draw(batch)
        if (bArrs.containsKey(currentButtonKey)) bArrs.get(currentButtonKey).draw(batch)
        for (text in t) text.draw(batch)
        if (MEGA_MAN == currentButtonKey || bNameSet.contains(currentButtonKey)) {
            bName.textSupplier = { currentButtonKey!!.uppercase(Locale.getDefault()) }
            bName.draw(batch)
        }
        batch.end()
    }
}

