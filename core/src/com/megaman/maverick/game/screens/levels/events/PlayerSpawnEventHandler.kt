package com.megaman.maverick.game.screens.levels.events

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.engine.animations.Animation
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.interfaces.Initializable
import com.engine.common.interfaces.Updatable
import com.engine.common.time.Timer
import com.engine.controller.ControllerSystem
import com.engine.drawables.IDrawable
import com.engine.drawables.fonts.BitmapFontHandle
import com.engine.events.Event
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.events.EventType
import java.util.*

/**
 * This class is responsible for handling the player spawn event.
 *
 * @param game the [MegamanMaverickGame] instance.
 */
class PlayerSpawnEventHandler(private val game: MegamanMaverickGame) :
    Initializable, Updatable, IDrawable<Batch> {

  companion object {
    private const val PRE_BEAM_DUR = 1f
    private const val BEAM_DOWN_DUR = .5f
    private const val BEAM_TRANS_DUR = .2f
    private const val BLINK_READY_DUR = .125f
  }

  val finished: Boolean
    get() =
        preBeamTimer.isFinished() && beamDownTimer.isFinished() && beamTransitionTimer.isFinished()

  private val blinkTimer = Timer(BLINK_READY_DUR).setToEnd()
  private val preBeamTimer = Timer(PRE_BEAM_DUR).setToEnd()
  private val beamDownTimer = Timer(BEAM_DOWN_DUR).setToEnd()
  private val beamTransitionTimer = Timer(BEAM_TRANS_DUR).setToEnd()

  private var beamRegion: TextureRegion? = null
  private var beamSprite: Sprite? = null
  private var beamLandAnimation: Animation? = null

  private var ready: BitmapFontHandle? = null

  private var initialized = false
  private var blinkReady = false

  override fun init() {
    if (!initialized) {
      val atlas = game.assMan.getTextureAtlas(TextureAsset.MEGAMAN_BUSTER.source)

      beamRegion = atlas.findRegion("Beam")
      beamSprite = Sprite(beamRegion)
      beamSprite?.setSize(1.5f * ConstVals.PPM, 1.5f * ConstVals.PPM)

      beamLandAnimation = Animation(atlas.findRegion("BeamLand"), 1, 2, .1f, false)

      ready =
          BitmapFontHandle(
              { ConstKeys.READY.uppercase(Locale.getDefault()) },
              (ConstVals.PPM / 2f).toInt(),
              Vector2(
                  ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f,
                  ConstVals.VIEW_HEIGHT * ConstVals.PPM / 2f),
              fontSource = "Megaman10Font.ttf")
      ready!!.init()

      initialized = true
    }

    blinkReady = false

    blinkTimer.reset()
    preBeamTimer.reset()
    beamDownTimer.reset()
    beamTransitionTimer.reset()
    beamLandAnimation!!.reset()
    beamSprite?.setPosition(-ConstVals.PPM.toFloat(), -ConstVals.PPM.toFloat())

    game.megaman.body.physics.gravityOn = false
    game.getSystems().get(ControllerSystem::class.simpleName).on = false
    game.eventsMan.submitEvent(Event(EventType.PLAYER_SPAWN))
  }

  override fun draw(drawer: Batch) {
    if (blinkReady) {
      drawer.projectionMatrix = game.getUiCamera().combined
      ready!!.draw(drawer)
    }

    if (preBeamTimer.isFinished() &&
        (!beamDownTimer.isFinished() || !beamTransitionTimer.isFinished())) {
      drawer.projectionMatrix = game.getGameCamera().combined
      beamSprite!!.draw(drawer)
    }
  }

  override fun update(delta: Float) {
    if (!preBeamTimer.isFinished()) preBeam(delta)
    else if (!beamDownTimer.isFinished()) beamDown(delta)
    else if (!beamTransitionTimer.isFinished()) beamTrans(delta)

    blinkTimer.update(delta)

    if (blinkTimer.isFinished()) {
      blinkReady = !blinkReady
      blinkTimer.reset()
    }
  }

  private fun preBeam(delta: Float) {
    preBeamTimer.update(delta)
    if (preBeamTimer.isJustFinished()) beamSprite?.setRegion(beamRegion)
  }

  private fun beamDown(delta: Float) {
    beamDownTimer.update(delta)

    val startY: Float = game.megaman.body.y + ConstVals.VIEW_HEIGHT * ConstVals.PPM
    val offsetY: Float = ConstVals.VIEW_HEIGHT * ConstVals.PPM * beamDownTimer.getRatio()

    beamSprite?.setCenterX(game.megaman.body.getCenter().x)
    beamSprite?.setY(startY - offsetY)
  }

  private fun beamTrans(delta: Float) {
    beamTransitionTimer.update(delta)
    beamLandAnimation?.update(delta)
    beamSprite?.setRegion(beamLandAnimation!!.getCurrentRegion())

    if (beamTransitionTimer.isJustFinished()) {
      game.getSystems().get(ControllerSystem::class.simpleName).on = true

      game.megaman.body.physics.gravityOn = true
      game.megaman.ready = true

      game.eventsMan.submitEvent(Event(EventType.PLAYER_READY))
      game.audioMan.playSound(SoundAsset.BEAM_IN_SOUND, false)
    }
  }
}
