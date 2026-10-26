package com.megaman.maverick.game.screens.levels.events

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.extensions.getTextureRegion
import com.maverick.game.engine.common.interfaces.IFinishable
import com.maverick.game.engine.common.interfaces.Initializable
import com.maverick.game.engine.common.interfaces.Resettable
import com.maverick.game.engine.common.interfaces.Updatable
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.drawables.IDrawable
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.setSize
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.locks.lock
import com.maverick.game.engine.locks.unlock
import com.maverick.game.engine.players.PlayerSlot
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.drawables.fonts.MegaFontHandle
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.components.MEGAMAN_SPRITE_SIZE
import com.megaman.maverick.game.entities.megaman.components.getSpritePriority
import com.megaman.maverick.game.entities.state.PlayPhase
import com.megaman.maverick.game.entities.state.setPlayPhase
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.events.PlayerEventKeys
import com.megaman.maverick.game.locks.MegaLockGroup
import com.megaman.maverick.game.locks.MegaLockReason
import com.megaman.maverick.game.players.MegaPlayer
import com.megaman.maverick.game.players.body
import com.megaman.maverick.game.players.facing
import com.megaman.maverick.game.players.megaman
import com.megaman.maverick.game.screens.utils.Fade
import com.megaman.maverick.game.screens.utils.Fade.FadeType
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.world.body.getCenter

class PlayerSpawnEventHandler(private val game: MegamanMaverickGame) : Initializable, Updatable, Resettable,
    IFinishable, IDrawable<Batch> {

    companion object {
        const val TAG = "PlayerSpawnEventHandler"
        private const val PRE_BEAM_DUR = 1f
        private const val BEAM_DOWN_DUR = 0.5f
        private const val BEAM_TRANS_DUR = 0.35f
        private const val BLINK_READY_DUR = 0.125f
        private const val FADE_IN_DUR = 0.25f
    }

    // TODO: the beam-in acts on the lead player in slot 0; with more players, each player needs its own (item 047)
    private val player: MegaPlayer
        get() = game.players.get(PlayerSlot(0))!!

    private fun playerSlot() = player.slot

    private val blinkTimer = Timer(BLINK_READY_DUR).setToEnd()
    private val preBeamTimer = Timer(PRE_BEAM_DUR).setToEnd()
    private val beamDownTimer = Timer(BEAM_DOWN_DUR).setToEnd()
    private val beamTransitionTimer = Timer(BEAM_TRANS_DUR).setToEnd()

    private val beamCenter = Vector2()

    private lateinit var beamRegion: TextureRegion
    private lateinit var beamSprite: GameSprite
    private lateinit var beamLandAnimation: Animation

    private lateinit var ready: MegaFontHandle

    private val fadein = Fade(FadeType.FADE_IN, FADE_IN_DUR)

    private var initialized = false
    private var showReadyText = false

    private var finished = true

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")

        if (!initialized) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.MEGAMAN_BUSTER.source)

            beamRegion = atlas.findRegion(ConstKeys.BEAM)
            val priority = DrawingPriority()
            beamSprite = GameSprite(beamRegion, player.megaman.getSpritePriority(priority))
            beamSprite.setSize(MEGAMAN_SPRITE_SIZE * ConstVals.PPM)

            beamLandAnimation = Animation(atlas.findRegion(ConstKeys.SPAWN), 1, 7, 0.05f, false)

            ready = MegaFontHandle(
                { ConstKeys.READY.uppercase() },
                positionX = ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f,
                positionY = ConstVals.VIEW_HEIGHT * ConstVals.PPM / 2f,
                priority = DrawingPriority(DrawingSection.FOREGROUND, 15)
            )

            val black = game.assMan.getTextureRegion(TextureAsset.COLORS.source, ConstKeys.BLACK)
            fadein.setRegion(black)

            initialized = true
        }

        showReadyText = false

        blinkTimer.reset()
        preBeamTimer.reset()
        beamDownTimer.reset()
        beamTransitionTimer.reset()
        beamLandAnimation.reset()

        beamSprite.setPosition(-100f * ConstVals.PPM, -100f * ConstVals.PPM)

        player.entity.setPlayPhase(PlayPhase.PRE_PLAY)
        player.entity.lock(MegaLockGroup.DAMAGE, MegaLockReason.SPAWN)
        player.body.physics.gravityOn = false
        player.entity.lock(MegaLockGroup.BEHAVIORS, MegaLockReason.SPAWN)

        game.eventsMan.submitEvent(Event(EventType.PLAYER_SPAWN_INIT, props(PlayerEventKeys.SLOT pairTo playerSlot())))
        game.eventsMan.submitEvent(Event(EventType.TURN_CONTROLLER_OFF))

        game.putProperty("${Megaman.TAG}_${ConstKeys.BEAM}", true)

        fadein.init()

        finished = false
    }

    override fun update(delta: Float) {
        beamCenter.set(beamSprite.boundingRectangle.getCenter())
        game.putProperty("${Megaman.TAG}_${ConstKeys.BEAM}_${ConstKeys.CENTER}", beamCenter)

        when {
            !fadein.isFinished() -> {
                val bounds = game.getUiCamera().toGameRectangle()
                fadein.setPosition(bounds.getX(), bounds.getY())
                fadein.setSize(bounds.getWidth(), bounds.getHeight())
                fadein.update(delta)
            }
            !preBeamTimer.isFinished() -> preBeam(delta)
            !beamDownTimer.isFinished() -> beamDown(delta)
            !beamTransitionTimer.isFinished() -> beamTrans(delta)
        }

        blinkTimer.update(delta)
        if (blinkTimer.isFinished()) {
            showReadyText = !showReadyText
            blinkTimer.reset()
        }

        if (preBeamTimer.isFinished() && (!beamDownTimer.isFinished() || !beamTransitionTimer.isFinished()))
            game.addDrawable(beamSprite)
    }

    override fun isFinished() = finished

    override fun reset() {
        GameLogger.debug(TAG, "reset()")

        preBeamTimer.setToEnd()
        beamDownTimer.setToEnd()
        beamTransitionTimer.setToEnd()
        fadein.setToEnd()

        finished = true
    }

    private fun preBeam(delta: Float) {
        preBeamTimer.update(delta)
        if (preBeamTimer.isJustFinished()) {
            GameLogger.debug(TAG, "preBeam(): just finished")
            beamSprite.setRegion(beamRegion)
        }
    }

    private fun beamDown(delta: Float) {
        beamDownTimer.update(delta)

        val startY = player.body.getY() + (ConstVals.VIEW_HEIGHT * ConstVals.PPM)
        val offsetY = (ConstVals.VIEW_HEIGHT * ConstVals.PPM) * beamDownTimer.getRatio()

        beamSprite.setCenterX(player.body.getCenter().x)
        beamSprite.y = startY - offsetY
    }

    private fun beamTrans(delta: Float) {
        beamTransitionTimer.update(delta)
        beamLandAnimation.update(delta)

        beamSprite.setRegion(beamLandAnimation.getCurrentRegion())
        beamSprite.setFlip(player.facing == Facing.LEFT, false)

        if (beamTransitionTimer.isJustFinished()) {
            GameLogger.debug(TAG, "beamTrans(): just finished")

            player.body.physics.gravityOn = true
            player.entity.unlock(MegaLockGroup.DAMAGE, MegaLockReason.SPAWN)
            player.entity.setPlayPhase(PlayPhase.IN_PLAY)
            player.entity.unlock(MegaLockGroup.BEHAVIORS, MegaLockReason.SPAWN)

            game.eventsMan.submitEvent(Event(EventType.TURN_CONTROLLER_ON))
            game.eventsMan.submitEvent(Event(EventType.PLAYER_READY, props(PlayerEventKeys.SLOT pairTo playerSlot())))

            game.audioMan.playSound(SoundAsset.BEAM_SOUND)

            game.putProperty("${Megaman.TAG}_${ConstKeys.BEAM}", false)

            showReadyText = false

            finished = true
        }
    }

    override fun draw(drawer: Batch) = when {
        fadein.isFinished() && showReadyText -> ready.draw(drawer)
        else -> fadein.draw(drawer)
    }
}
