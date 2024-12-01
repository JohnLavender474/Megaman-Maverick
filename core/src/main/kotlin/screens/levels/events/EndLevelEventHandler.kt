package com.megaman.maverick.game.screens.levels.events

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.common.interfaces.Resettable
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.IDrawable
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.events.Event
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.world.body.getCenter

class EndLevelEventHandler(private val game: MegamanMaverickGame) : Initializable, Updatable, IDrawable<Batch>,
    Resettable {

    companion object {
        const val TAG = "PlayerSpawnEventHandler"
        private const val START_DELAY_DUR = 3f
        private const val PRE_BEAM_DUR = 7f
        private const val BEAM_UP_DUR = 0.5f
        private const val BEAM_TRANS_DUR = 0.2f
        private const val BEAM_END_DUR = 0.5f
    }

    val finished: Boolean
        get() = startDelayTimer.isFinished() &&
            preBeamTimer.isFinished() &&
            beamUpTimer.isFinished() &&
            beamTransitionTimer.isFinished() &&
            beamEndTimer.isFinished()

    private val megaman = game.megaman

    private val startDelayTimer = Timer(START_DELAY_DUR).setToEnd()
    private val preBeamTimer = Timer(PRE_BEAM_DUR).setToEnd()
    private val beamUpTimer = Timer(BEAM_UP_DUR).setToEnd()
    private val beamTransitionTimer = Timer(BEAM_TRANS_DUR).setToEnd()
    private val beamEndTimer = Timer(BEAM_END_DUR).setToEnd()

    private lateinit var beamRegion: TextureRegion
    private lateinit var beamSprite: GameSprite
    private lateinit var beamTransAnim: Animation

    private var initialized = false

    override fun init() {
        GameLogger.debug(PlayerSpawnEventHandler.TAG, "init()")

        if (!initialized) {
            initialized = true

            val atlas = game.assMan.getTextureAtlas(TextureAsset.MEGAMAN_BUSTER.source)
            beamRegion = atlas.findRegion("Beam")
            beamSprite = GameSprite(beamRegion)
            beamSprite.setSize(1.5f * ConstVals.PPM)

            beamTransAnim = Animation(atlas.findRegion("Spawn"), 1, 2, 0.1f, false).reversed()
        }

        startDelayTimer.reset()

        beamTransAnim.reset()
        beamSprite.hidden = true
        beamSprite.setPosition(-ConstVals.PPM.toFloat(), -ConstVals.PPM.toFloat())

        megaman.canBeDamaged = false
        megaman.setAllBehaviorsAllowed(false)

        game.eventsMan.submitEvent(Event(EventType.TURN_CONTROLLER_OFF))
    }

    override fun draw(drawer: Batch) {
        val drawing = drawer.isDrawing
        if (!drawing) drawer.begin()

        if (startDelayTimer.isFinished() && preBeamTimer.isFinished() &&
            (!beamUpTimer.isFinished() || !beamTransitionTimer.isFinished())
        ) {
            drawer.projectionMatrix = game.getGameCamera().combined
            beamSprite.draw(drawer)
        }

        if (!drawing) drawer.end()
    }

    override fun update(delta: Float) {
        when {
            !startDelayTimer.isFinished() -> {
                startDelayTimer.update(delta)
                if (startDelayTimer.isJustFinished()) {
                    GameLogger.debug(TAG, "start delay timer just finished")
                    preBeamTimer.reset()
                    game.audioMan.playSound(SoundAsset.MM2_VICTORY_SOUND, false)
                }
            }

            !preBeamTimer.isFinished() -> preBeam(delta)
            !beamTransitionTimer.isFinished() -> {
                beamSprite.hidden = false
                beamTrans(delta)
            }

            !beamUpTimer.isFinished() -> {
                beamSprite.setRegion(beamRegion)
                beamUp(delta)
            }

            !beamEndTimer.isFinished() -> beamEnd(delta)
        }
    }

    override fun reset() {
        startDelayTimer.setToEnd()
        preBeamTimer.setToEnd()
        beamUpTimer.setToEnd()
        beamTransitionTimer.setToEnd()
        beamEndTimer.setToEnd()
    }

    private fun preBeam(delta: Float) {
        preBeamTimer.update(delta)
        if (preBeamTimer.isFinished()) {
            GameLogger.debug(TAG, "pre-beam timer just finished")

            megaman.ready = false

            beamSprite.setOriginCenter()
            beamSprite.rotation = megaman.direction.rotation
            beamSprite.setCenter(megaman.body.getCenter())

            beamTransitionTimer.reset()

            game.audioMan.playSound(SoundAsset.BEAM_SOUND, false)
        }
    }

    private fun beamTrans(delta: Float) {
        beamTransitionTimer.update(delta)
        beamTransAnim.update(delta)
        beamSprite.setRegion(beamTransAnim.getCurrentRegion())

        if (beamTransitionTimer.isFinished()) {
            GameLogger.debug(TAG, "beam transition timer just finished")
            beamUpTimer.reset()
        }
    }

    private fun beamUp(delta: Float) {
        beamUpTimer.update(delta)

        beamSprite.setCenter(megaman.body.getCenter())
        when (megaman.direction) {
            Direction.UP -> beamSprite.y += ConstVals.VIEW_HEIGHT * ConstVals.PPM * beamUpTimer.getRatio()
            Direction.DOWN -> beamSprite.y -= ConstVals.VIEW_HEIGHT * ConstVals.PPM * beamUpTimer.getRatio()
            Direction.LEFT -> beamSprite.x -= ConstVals.VIEW_HEIGHT * ConstVals.PPM * beamUpTimer.getRatio()
            Direction.RIGHT -> beamSprite.x += ConstVals.VIEW_HEIGHT * ConstVals.PPM * beamUpTimer.getRatio()
        }

        if (beamUpTimer.isFinished()) {
            GameLogger.debug(TAG, "beam up timer just finished")
            beamSprite.hidden = true
            beamEndTimer.reset()
        }
    }

    private fun beamEnd(delta: Float) {
        beamEndTimer.update(delta)

        if (beamEndTimer.isFinished()) {
            GameLogger.debug(TAG, "beam end timer just finished")

            game.eventsMan.submitEvent(Event(EventType.END_LEVEL))
        }
    }
}
