package com.megaman.maverick.game.screens.menus.temp

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
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
import com.mega.game.engine.screens.menus.IMenuButton
import com.megaman.maverick.game.ConstFuncs
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.drawables.fonts.MegaFontHandle
import com.megaman.maverick.game.entities.bosses.BossType
import com.megaman.maverick.game.screens.ScreenEnum
import com.megaman.maverick.game.screens.menus.MegaMenuScreen
import com.megaman.maverick.game.screens.utils.BlinkingArrow
import java.util.*
import java.util.function.Supplier

class BossSelectScreen(game: MegamanMaverickGame) : MegaMenuScreen(game, MEGA_MAN), Initializable {

    companion object {
        private const val MEGA_MAN = "MEGA MAN"
        private const val BACK = "BACK"
    }

    private val bNameSet = ObjectSet<String>()
    private val bar1 = Sprite()
    private val bar2 = Sprite()
    private val white = Sprite()
    private val outTimer = Timer(1.05f)
    private val t = Array<MegaFontHandle>()
    private val bp = Array<Mugshot>()
    private val bkgd = Array<Sprite>()
    private val bars = ObjectMap<Sprite, Animation>()
    private val bArrs = ObjectMap<String, BlinkingArrow>()
    private lateinit var bName: MegaFontHandle
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
                    Direction.UP -> BossType.findByPos(1, 2)!!.bossName
                    Direction.DOWN -> BossType.findByPos(1, 0)!!.bossName
                    Direction.LEFT -> BossType.findByPos(0, 1)!!.bossName
                    Direction.RIGHT -> BossType.findByPos(2, 1)!!.bossName
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
                    Direction.UP, Direction.LEFT, Direction.RIGHT -> BossType.findByPos(2, 0)!!.bossName
                    Direction.DOWN -> BossType.findByPos(2, 2)!!.bossName
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
                        else -> BossType.findByPos(x, y)!!.bossName
                    }
                }
            })
        }

        for (b in BossType.values()) bNameSet.add(b.name)
        val outTimerRunnable = Array<TimeMarkedRunnable>()
        for (i in 1..10) outTimerRunnable.add(TimeMarkedRunnable(.1f * i) { blink = !blink })
        outTimer.addRunnables(outTimerRunnable)
        val megamanFacesAtlas = game.assMan.get(TextureAsset.FACES_1.source, TextureAtlas::class.java)
        val megamanFaces: MutableMap<Position, TextureRegion> = EnumMap(Position::class.java)
        for (position in Position.values()) {
            val faceRegion: TextureRegion = megamanFacesAtlas.findRegion("Megaman/" + position.name)
            megamanFaces[position] = faceRegion
        }
        val megamanFaceSupplier = Supplier {
            val boss = BossType.findByName(currentButtonKey!!) ?: return@Supplier megamanFaces[Position.CENTER]
            megamanFaces[boss.position]
        }
        val megamanPane = Mugshot(game, megamanFaceSupplier, MEGA_MAN, Position.CENTER)
        bp.add(megamanPane)
        for (boss in BossType.values()) bp.add(Mugshot(game, boss))
        t.add(
            MegaFontHandle(
                text = "PRESS START",
                positionX = 5.35f * ConstVals.PPM,
                positionY = 13.85f * ConstVals.PPM,
                centerX = false,
                centerY = false
            )
        )
        t.add(
            MegaFontHandle(
                text = BACK,
                positionX = 12.35f * ConstVals.PPM,
                positionY = ConstVals.PPM.toFloat(),
                centerX = false, centerY = false
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
        val bar: TextureRegion = stageSelectAtlas.findRegion("bar")
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
        val blueBlockRegion: TextureRegion = tilesAtlas.findRegion("8bitBlueBlockNoBorder")
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
        bName = MegaFontHandle(
            text = "",
            positionX = ConstVals.PPM.toFloat(),
            positionY = ConstVals.PPM.toFloat(),
            centerX = false, centerY = false,
        )
    }

    override fun show() {
        if (!initialized) {
            init()
            initialized = true
        }
        super.show()
        // slide.init()
        game.getUiCamera().position.set(ConstFuncs.getGameCamInitPos())
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
            // slide.update(delta)
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
                b.state =
                    if (b.name == currentButtonKey) (if (selectionMade) MugshotState.HIGHLIGHTED else MugshotState.BLINKING)
                    else MugshotState.NONE
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
            bName.setTextSupplier { currentButtonKey!!.uppercase(Locale.getDefault()) }
            bName.draw(batch)
        }
        batch.end()
    }
}

