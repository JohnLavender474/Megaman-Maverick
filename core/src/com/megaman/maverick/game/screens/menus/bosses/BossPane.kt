package com.megaman.maverick.game.screens.menus.bosses

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.engine.animations.Animation
import com.engine.common.enums.Position
import com.engine.common.interfaces.Updatable
import com.engine.drawables.IDrawable
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.bosses.BossType
import java.util.function.Supplier

class BossPane(
    game: MegamanMaverickGame, private val bossRegSupplier: Supplier<TextureRegion?>,
    val bossName: String?, x: Int, y: Int
) : Updatable, IDrawable<Batch> {

    companion object {
        const val PANE_BOUNDS_WIDTH = 5.33f
        const val PANE_BOUNDS_HEIGHT = 4f
        const val BOTTOM_OFFSET = 1.35f
        const val SPRITE_HEIGHT = 2f
        const val SPRITE_WIDTH = 2.5f
        const val PANE_HEIGHT = 3f
        const val PANE_WIDTH = 4f
    }

    private val bossSprite = Sprite()
    private val paneSprite = Sprite()

    private val paneBlinkingAnim: Animation
    private val paneHighlightedAnim: Animation
    private val paneUnhighlightedAnim: Animation

    var bossPaneStat = BossPaneStat.UNHIGHLIGHTED

    constructor(game: MegamanMaverickGame, boss: BossType) : this(
        game,
        game.assMan.get<TextureAtlas>(TextureAsset.FACES_1.source, TextureAtlas::class.java).findRegion(boss.name),
        boss.name,
        boss.position
    )

    constructor(game: MegamanMaverickGame, bossRegion: TextureRegion?, bossName: String?, position: Position) : this(
        game,
        bossRegion,
        bossName,
        position.x,
        position.y
    )

    constructor(game: MegamanMaverickGame, bossRegion: TextureRegion?, bossName: String?, x: Int, y: Int) : this(
        game,
        Supplier<TextureRegion?> { bossRegion },
        bossName,
        x,
        y
    )

    constructor(
        game2d: MegamanMaverickGame, bossRegSupplier: Supplier<TextureRegion?>,
        bossName: String?, position: Position
    ) : this(game2d, bossRegSupplier, bossName, position.x, position.y)

    init {
        val centerX =
            (x * PANE_BOUNDS_WIDTH * ConstVals.PPM) + (PANE_BOUNDS_WIDTH * ConstVals.PPM / 2f)
        val centerY = (BOTTOM_OFFSET * ConstVals.PPM + y * PANE_BOUNDS_HEIGHT * ConstVals.PPM) +
                (PANE_BOUNDS_HEIGHT * ConstVals.PPM / 2f)
        bossSprite.setSize(SPRITE_WIDTH * ConstVals.PPM, SPRITE_HEIGHT * ConstVals.PPM)
        bossSprite.setCenter(centerX, centerY)
        paneSprite.setSize(PANE_WIDTH * ConstVals.PPM, PANE_HEIGHT * ConstVals.PPM)
        paneSprite.setCenter(centerX, centerY)
        val decorationAtlas = game.assMan.get(TextureAsset.UI_1.source, TextureAtlas::class.java)
        val paneUnhighlighted: TextureRegion = decorationAtlas.findRegion("Pane")
        this.paneUnhighlightedAnim = Animation(paneUnhighlighted)
        val paneBlinking: TextureRegion = decorationAtlas.findRegion("PaneBlinking")
        this.paneBlinkingAnim = Animation(paneBlinking, 1, 2, .125f, true)
        val paneHighlighted: TextureRegion = decorationAtlas.findRegion("PaneHighlighted")
        this.paneHighlightedAnim = Animation(paneHighlighted)
    }

    override fun update(delta: Float) {
        val timedAnimation = when (bossPaneStat) {
            BossPaneStat.BLINKING -> paneBlinkingAnim
            BossPaneStat.HIGHLIGHTED -> paneHighlightedAnim
            BossPaneStat.UNHIGHLIGHTED -> paneUnhighlightedAnim
        }
        timedAnimation.update(delta)
        paneSprite.setRegion(timedAnimation.getCurrentRegion())
    }

    override fun draw(drawer: Batch) {
        paneSprite.draw(drawer)
        bossSprite.setRegion(bossRegSupplier.get())
        bossSprite.draw(drawer)
    }
}
