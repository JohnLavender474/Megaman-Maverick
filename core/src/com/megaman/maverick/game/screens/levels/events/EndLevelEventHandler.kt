package com.megaman.maverick.game.screens.levels.events

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.engine.animations.Animation
import com.engine.common.GameLogger
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.interfaces.Initializable
import com.engine.common.interfaces.Updatable
import com.engine.common.time.Timer
import com.engine.drawables.IDrawable
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.setCenter
import com.engine.drawables.sprites.setSize
import com.engine.events.Event
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.screens.ScreenEnum

class EndLevelEventHandler(private val game: MegamanMaverickGame) : Initializable, Updatable, IDrawable<Batch> {

    companion object {
        const val TAG = "PlayerSpawnEventHandler"
        private const val PRE_BEAM_DUR = 7f
        private const val BEAM_UP_DUR = 0.5f
        private const val BEAM_TRANS_DUR = 0.2f
        private const val BEAM_END_DUR = 0.5f
    }

    val finished: Boolean
        get() = preBeamTimer.isFinished() &&
                beamUpTimer.isFinished() &&
                beamTransitionTimer.isFinished() &&
                beamEndTimer.isFinished()

    private val megaman = game.megaman

    private val preBeamTimer = Timer(PRE_BEAM_DUR).setToEnd()
    private val beamUpTimer = Timer(BEAM_UP_DUR).setToEnd()
    private val beamTransitionTimer = Timer(BEAM_TRANS_DUR).setToEnd()
    private val beamEndTimer = Timer(BEAM_END_DUR).setToEnd()

    private lateinit var beamRegion: TextureRegion
    private lateinit var beamSprite: GameSprite
    private lateinit var beamTransAnim: Animation

    private var initialized = false

    private fun endLevelSuccessfully() {
        game.eventsMan.submitEvent(Event(EventType.TURN_CONTROLLER_ON))
        game.setCurrentScreen(ScreenEnum.SAVE_GAME_SCREEN.name)
    }

    override fun init() {
        GameLogger.debug(PlayerSpawnEventHandler.TAG, "Initializing...")
        if (!initialized) {
            initialized = true
            GameLogger.debug(PlayerSpawnEventHandler.TAG, "First time initializing...")
            val atlas = game.assMan.getTextureAtlas(TextureAsset.MEGAMAN_BUSTER.source)
            beamRegion = atlas.findRegion("Beam")
            beamSprite = GameSprite(beamRegion)
            beamSprite.setSize(1.5f * ConstVals.PPM)
            beamTransAnim = Animation(atlas.findRegion("BeamLand"), 1, 2, 0.1f, false).reversed()
        }

        preBeamTimer.reset()

        beamTransAnim.reset()
        beamSprite.hidden = true
        beamSprite.setPosition(-ConstVals.PPM.toFloat(), -ConstVals.PPM.toFloat())

        megaman.canBeDamaged = false
        megaman.setAllBehaviorsAllowed(false)

        game.eventsMan.submitEvent(Event(EventType.TURN_CONTROLLER_OFF))
        game.audioMan.playSound(SoundAsset.MM1_VICTORY_SOUND, false)
    }

    override fun draw(drawer: Batch) {
        val drawing = drawer.isDrawing
        if (!drawing) drawer.begin()
        if (preBeamTimer.isFinished() && (!beamUpTimer.isFinished() || !beamTransitionTimer.isFinished())) {
            drawer.projectionMatrix = game.getGameCamera().combined
            beamSprite.draw(drawer)
        }
        if (!drawing) drawer.end()
    }

    override fun update(delta: Float) {
        if (!preBeamTimer.isFinished()) preBeam(delta)
        else if (!beamTransitionTimer.isFinished()) {
            beamSprite.hidden = false
            beamTrans(delta)
        } else if (!beamUpTimer.isFinished()) {
            beamSprite.setRegion(beamRegion)
            beamUp(delta)
        } else if (!beamEndTimer.isFinished()) beamEnd(delta)
    }

    private fun preBeam(delta: Float) {
        preBeamTimer.update(delta)
        if (preBeamTimer.isFinished()) {
            megaman.ready = false
            beamSprite.setCenter(megaman.body.getCenter())
            beamTransitionTimer.reset()
            game.audioMan.playSound(SoundAsset.BEAM_SOUND, false)
        }
    }

    private fun beamTrans(delta: Float) {
        beamTransitionTimer.update(delta)
        beamTransAnim.update(delta)
        beamSprite.setRegion(beamTransAnim.getCurrentRegion())
        if (beamTransitionTimer.isFinished()) beamUpTimer.reset()
    }

    private fun beamUp(delta: Float) {
        beamUpTimer.update(delta)
        val offsetY = (ConstVals.VIEW_HEIGHT * ConstVals.PPM) * beamUpTimer.getRatio()
        val center = megaman.body.getCenter()
        beamSprite.setCenter(center.x, center.y + offsetY)
        if (beamUpTimer.isFinished()) {
            beamSprite.hidden = true
            beamEndTimer.reset()
        }
    }

    private fun beamEnd(delta: Float) {
        beamEndTimer.update(delta)
        if (beamEndTimer.isFinished()) game.eventsMan.submitEvent(Event(EventType.END_LEVEL)) // endLevelSuccessfully()
    }
}
