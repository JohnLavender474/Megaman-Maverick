package com.megaman.maverick.game.screens.levels.events

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.common.interfaces.Resettable
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.IDrawable
import com.mega.game.engine.drawables.fonts.BitmapFontHandle
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.events.Event
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.components.MEGAMAN_SPRITE_SIZE
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.getCenter
import java.util.*

class PlayerSpawnEventHandler(private val game: MegamanMaverickGame) : Initializable, Updatable, IDrawable<Batch>,
    Resettable {

    companion object {
        const val TAG = "PlayerSpawnEventHandler"
        private const val PRE_BEAM_DUR = 1f
        private const val BEAM_DOWN_DUR = 0.5f
        private const val BEAM_TRANS_DUR = 0.35f
        private const val BLINK_READY_DUR = 0.125f
    }

    val finished: Boolean
        get() = preBeamTimer.isFinished() && beamDownTimer.isFinished() && beamTransitionTimer.isFinished()
    val beamCenter: Vector2
        get() = beamSprite.boundingRectangle.getCenter()

    private val megaman = game.megaman

    private val blinkTimer = Timer(BLINK_READY_DUR).setToEnd()
    private val preBeamTimer = Timer(PRE_BEAM_DUR).setToEnd()
    private val beamDownTimer = Timer(BEAM_DOWN_DUR).setToEnd()
    private val beamTransitionTimer = Timer(BEAM_TRANS_DUR).setToEnd()

    private lateinit var beamRegion: TextureRegion
    private lateinit var beamSprite: Sprite
    private lateinit var beamLandAnimation: Animation
    private lateinit var ready: BitmapFontHandle

    private var initialized = false
    private var blinkReady = false

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (!initialized) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.MEGAMAN_V2_BUSTER.source)

            beamRegion = atlas.findRegion(ConstKeys.BEAM)
            beamSprite = Sprite(beamRegion)
            beamSprite.setSize(MEGAMAN_SPRITE_SIZE * ConstVals.PPM)

            beamLandAnimation = Animation(atlas.findRegion(ConstKeys.SPAWN), 1, 7, 0.05f, false)

            ready =
                BitmapFontHandle(
                    { ConstKeys.READY.uppercase(Locale.getDefault()) },
                    (ConstVals.PPM / 2f).toInt(),
                    Vector2(
                        ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f,
                        ConstVals.VIEW_HEIGHT * ConstVals.PPM / 2f
                    ),
                    fontSource = "Megaman10Font.ttf"
                )
            ready.init()

            initialized = true
        }

        blinkReady = false

        blinkTimer.reset()
        preBeamTimer.reset()
        beamDownTimer.reset()
        beamTransitionTimer.reset()
        beamLandAnimation.reset()
        beamSprite.setPosition(-ConstVals.PPM.toFloat(), -ConstVals.PPM.toFloat())

        megaman.ready = false
        megaman.canBeDamaged = false
        megaman.body.physics.gravityOn = false
        megaman.setAllBehaviorsAllowed(false)

        GameLogger.debug(TAG, "init(): submit PLAYER_SPAWN event")
        game.eventsMan.submitEvent(Event(EventType.PLAYER_SPAWN))
        game.eventsMan.submitEvent(Event(EventType.TURN_CONTROLLER_OFF))

        game.putProperty("${Megaman.TAG}_${ConstKeys.BEAM}", true)
    }

    override fun draw(drawer: Batch) {
        val drawing = drawer.isDrawing
        if (!drawing) drawer.begin()

        if (blinkReady) {
            drawer.projectionMatrix = game.getUiCamera().combined
            ready.draw(drawer)
        }

        if (preBeamTimer.isFinished() &&
            (!beamDownTimer.isFinished() || !beamTransitionTimer.isFinished())
        ) {
            drawer.projectionMatrix = game.getGameCamera().combined
            beamSprite.draw(drawer)
        }

        if (!drawing) drawer.end()
    }

    override fun update(delta: Float) {
        game.putProperty("${Megaman.TAG}_${ConstKeys.BEAM}_${ConstKeys.CENTER}", beamCenter)

        if (!preBeamTimer.isFinished()) preBeam(delta)
        else if (!beamDownTimer.isFinished()) beamDown(delta)
        else if (!beamTransitionTimer.isFinished()) beamTrans(delta)

        blinkTimer.update(delta)
        if (blinkTimer.isFinished()) {
            blinkReady = !blinkReady
            blinkTimer.reset()
        }
    }

    override fun reset() {
        preBeamTimer.setToEnd()
        beamDownTimer.setToEnd()
        beamTransitionTimer.setToEnd()
    }

    private fun preBeam(delta: Float) {
        preBeamTimer.update(delta)
        if (preBeamTimer.isJustFinished()) {
            GameLogger.debug(TAG, "preBeam(): pre-beam timer just finished")
            beamSprite.setRegion(beamRegion)
        }
    }

    private fun beamDown(delta: Float) {
        beamDownTimer.update(delta)

        val startY: Float = game.megaman.body.getY() + (ConstVals.VIEW_HEIGHT * ConstVals.PPM)
        val offsetY: Float = (ConstVals.VIEW_HEIGHT * ConstVals.PPM) * beamDownTimer.getRatio()

        beamSprite.setCenterX(game.megaman.body.getCenter().x)
        beamSprite.y = startY - offsetY
    }

    private fun beamTrans(delta: Float) {
        beamTransitionTimer.update(delta)
        beamLandAnimation.update(delta)
        beamSprite.setRegion(beamLandAnimation.getCurrentRegion())
        beamSprite.setFlip(megaman.isFacing(Facing.LEFT), false)

        if (beamTransitionTimer.isJustFinished()) {
            GameLogger.debug(TAG, "beamTrans(): beam transition timer just finished")

            megaman.body.physics.gravityOn = true
            megaman.canBeDamaged = true
            megaman.ready = true
            megaman.setAllBehaviorsAllowed(true)

            game.eventsMan.submitEvent(Event(EventType.PLAYER_READY))
            game.eventsMan.submitEvent(Event(EventType.TURN_CONTROLLER_ON))
            game.audioMan.playSound(SoundAsset.BEAM_SOUND, false)

            game.putProperty("${Megaman.TAG}_${ConstKeys.BEAM}", false)
        }
    }
}
