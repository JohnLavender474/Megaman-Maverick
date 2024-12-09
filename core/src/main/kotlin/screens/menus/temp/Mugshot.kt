package com.megaman.maverick.game.screens.menus.temp

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.drawables.IDrawable
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.bosses.BossType
import java.util.function.Supplier

class Mugshot(
    private val game: MegamanMaverickGame,
    private val mugshotRegionSupplier: Supplier<TextureRegion?>,
    val name: String?,
    private val x: Int,
    private val y: Int,
    private val mugshotWidth: Float = DEFAULT_MUGSHOT_WIDTH * ConstVals.PPM,
    private val mugshotHeight: Float = DEFAULT_MUGSHOT_HEIGHT * ConstVals.PPM,
    private val paneWidth: Float = DEFAULT_PANE_WIDTH * ConstVals.PPM,
    private val paneHeight: Float = DEFAULT_PANE_HEIGHT * ConstVals.PPM
) : Initializable, Updatable, IDrawable<Batch> {

    companion object {
        const val DEFAULT_PANE_WIDTH = 5.33f
        const val DEFAULT_PANE_HEIGHT = 4f
        const val DEFAULT_MUGSHOT_HEIGHT = 2f
        const val DEFAULT_MUGSHOT_WIDTH = 2.5f
        const val BOTTOM_OFFSET = 0f
        const val PANE_HEIGHT = 3f
        const val PANE_WIDTH = 4f
    }

    var state = MugshotState.NONE

    private lateinit var bossSprite: Sprite
    private lateinit var paneSprite: Sprite
    private lateinit var paneBlinkingAnim: Animation
    private lateinit var paneHighlightedAnim: Animation
    private lateinit var paneUnhighlightedAnim: Animation

    constructor(game: MegamanMaverickGame, boss: BossType) : this(
        game,
        game.assMan.get<TextureAtlas>(TextureAsset.FACES_1.source, TextureAtlas::class.java).findRegion(boss.name),
        boss.name,
        boss.position
    )

    constructor(game: MegamanMaverickGame, bossRegion: TextureRegion?, bossName: String?, position: Position) : this(
        game, bossRegion, bossName, position.x, position.y
    )

    constructor(game: MegamanMaverickGame, bossRegion: TextureRegion?, bossName: String?, x: Int, y: Int) : this(
        game, Supplier<TextureRegion?> { bossRegion }, bossName, x, y
    )

    constructor(
        game2d: MegamanMaverickGame, bossRegSupplier: Supplier<TextureRegion?>,
        bossName: String?, position: Position
    ) : this(game2d, bossRegSupplier, bossName, position.x, position.y)

    private var initialized = false

    override fun init() {
        if (initialized) return
        initialized = true

        val width = when (name) {
            "MOON MAN" -> 5f
            else -> DEFAULT_MUGSHOT_WIDTH
        }
        val height = when (name) {
            "MOON MAN" -> 3.75f
            else -> DEFAULT_MUGSHOT_HEIGHT
        }

        bossSprite = Sprite()
        bossSprite.setSize(width * ConstVals.PPM, height * ConstVals.PPM)
        val centerX =
            (x * DEFAULT_PANE_WIDTH * ConstVals.PPM) + (DEFAULT_PANE_WIDTH * ConstVals.PPM / 2f)
        val centerY = (BOTTOM_OFFSET * ConstVals.PPM + y * DEFAULT_PANE_HEIGHT * ConstVals.PPM) +
                (DEFAULT_PANE_HEIGHT * ConstVals.PPM / 2f)
        bossSprite.setCenter(centerX, centerY)

        paneSprite = Sprite()
        paneSprite.setSize(PANE_WIDTH * ConstVals.PPM, PANE_HEIGHT * ConstVals.PPM)
        paneSprite.setCenter(centerX, centerY)

        val decorationAtlas = game.assMan.get(TextureAsset.UI_1.source, TextureAtlas::class.java)
        val paneUnhighlighted: TextureRegion = decorationAtlas.findRegion("Pane")
        this.paneUnhighlightedAnim = Animation(paneUnhighlighted)

        val paneBlinking: TextureRegion = decorationAtlas.findRegion("PaneBlinking")
        this.paneBlinkingAnim = Animation(paneBlinking, 1, 2, 0.125f, true)

        val paneHighlighted: TextureRegion = decorationAtlas.findRegion("PaneHighlighted")
        this.paneHighlightedAnim = Animation(paneHighlighted)
    }

    override fun update(delta: Float) {
        if (!initialized) init()

        val timedAnimation = when (state) {
            MugshotState.BLINKING -> paneBlinkingAnim
            MugshotState.HIGHLIGHTED -> paneHighlightedAnim
            MugshotState.NONE -> paneUnhighlightedAnim
        }
        timedAnimation.update(delta)

        paneSprite.setRegion(timedAnimation.getCurrentRegion())
    }

    override fun draw(drawer: Batch) {
        paneSprite.draw(drawer)
        val bossRegion = mugshotRegionSupplier.get()
        if (bossRegion != null) {
            bossSprite.setRegion(bossRegion)
            bossSprite.draw(drawer)
        }
    }
}
