package com.megaman.maverick.game.screens.levels.respawn

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.drawables.sorting.IComparableDrawable
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.setSize
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.entities.facing
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.megaman.components.MEGAMAN_SPRITE_SIZE
import com.megaman.maverick.game.entities.megaman.components.getMegamanSpritePriority
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.getCenter

class BeamDown(private val assets: AssetManager, private val addDrawable: (IComparableDrawable<Batch>) -> Unit) {

    companion object {
        private const val BEAM_DOWN_DUR = 0.5f
        private const val BEAM_TRANS_DUR = 0.35f
    }

    private val beamDownTimer = Timer(BEAM_DOWN_DUR).setToEnd()
    private val beamTransitionTimer = Timer(BEAM_TRANS_DUR).setToEnd()

    private lateinit var beamRegion: TextureRegion
    private lateinit var beamSprite: GameSprite
    private lateinit var beamLandAnimation: Animation
    private var loaded = false

    private var player: MegaGameEntity? = null

    val center = Vector2(-100f * ConstVals.PPM, -100f * ConstVals.PPM)

    val finished: Boolean
        get() = beamDownTimer.isFinished() && beamTransitionTimer.isFinished()

    fun start(player: MegaGameEntity) {
        load()
        this.player = player
        beamDownTimer.reset()
        beamTransitionTimer.reset()
        beamLandAnimation.reset()
        beamSprite.setRegion(beamRegion)
        beamSprite.setPosition(-100f * ConstVals.PPM, -100f * ConstVals.PPM)
    }

    fun stop() {
        beamDownTimer.setToEnd()
        beamTransitionTimer.setToEnd()
        player = null
        center.set(-100f * ConstVals.PPM, -100f * ConstVals.PPM)
    }

    fun update(delta: Float) {
        val player = this.player ?: return
        when {
            !beamDownTimer.isFinished() -> beamDown(player, delta)
            !beamTransitionTimer.isFinished() -> beamTrans(player, delta)
        }
        center.set(beamSprite.boundingRectangle.getCenter())
        if (!finished) addDrawable(beamSprite) else this.player = null
    }

    private fun beamDown(player: MegaGameEntity, delta: Float) {
        beamDownTimer.update(delta)

        val startY = player.body.getY() + (ConstVals.VIEW_HEIGHT * ConstVals.PPM)
        val offsetY = (ConstVals.VIEW_HEIGHT * ConstVals.PPM) * beamDownTimer.getRatio()

        beamSprite.setCenterX(player.body.getCenter().x)
        beamSprite.y = startY - offsetY
    }

    private fun beamTrans(player: MegaGameEntity, delta: Float) {
        beamTransitionTimer.update(delta)
        beamLandAnimation.update(delta)

        beamSprite.setRegion(beamLandAnimation.getCurrentRegion())
        beamSprite.setFlip(player.facing == Facing.LEFT, false)
        beamSprite.setCenterX(player.body.getCenter().x)
        beamSprite.y = player.body.getY()
    }

    private fun load() {
        if (loaded) return
        val atlas = assets.getTextureAtlas(TextureAsset.MEGAMAN_BUSTER.source)
        beamRegion = atlas.findRegion(ConstKeys.BEAM)
        beamSprite = GameSprite(beamRegion, getMegamanSpritePriority(DrawingPriority()))
        beamSprite.setSize(MEGAMAN_SPRITE_SIZE * ConstVals.PPM)
        beamLandAnimation = Animation(atlas.findRegion(ConstKeys.SPAWN), 1, 7, 0.05f, false)
        loaded = true
    }
}
