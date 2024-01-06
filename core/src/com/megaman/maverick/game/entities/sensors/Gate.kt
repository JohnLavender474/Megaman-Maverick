package com.megaman.maverick.game.entities.sensors

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.audio.AudioComponent
import com.engine.common.GameLogger
import com.engine.common.enums.Position
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.extensions.objectSetOf
import com.engine.common.interfaces.Resettable
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.entities.GameEntity
import com.engine.entities.contracts.IAudioEntity
import com.engine.entities.contracts.IBodyEntity
import com.engine.entities.contracts.ISpriteEntity
import com.engine.events.Event
import com.engine.events.IEventListener
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.getMegamanMaverickGame
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType

class Gate(game: MegamanMaverickGame) :
    GameEntity(game), IBodyEntity, IAudioEntity, ISpriteEntity, IEventListener, Resettable {

  enum class GateState {
    OPENABLE,
    OPENING,
    OPEN,
    CLOSING,
    CLOSED
  }

  companion object {
    const val TAG = "Gate"

    private var atlas: TextureAtlas? = null
    private const val DURATION = .5f
  }

  override val eventKeyMask = objectSetOf<Any>(EventType.PLAYER_SPAWN, EventType.END_ROOM_TRANS)

  private val timer = Timer(DURATION)

  lateinit var state: GateState
    private set

  private lateinit var nextRoomKey: String

  private var transitionFinished = false

  override fun init() {
    if (atlas == null) atlas = game.assMan.getTextureAtlas(TextureAsset.GATES.source)

    addComponent(defineBodyComponent())
    addComponent(defineUpdatablesComponent())
    addComponent(defineSpritesCompoent())
    addComponent(defineAnimationsComponent())
    addComponent(AudioComponent(this))

    runnablesOnDestroy.add { game.eventsMan.removeListener(this) }
  }

  override fun spawn(spawnProps: Properties) {
    super.spawn(spawnProps)

    reset()
    game.eventsMan.addListener(this)

    val bounds = spawnProps.get(ConstKeys.BOUNDS) as GameRectangle
    body.setCenter(bounds.getCenter())

    nextRoomKey = spawnProps.get(ConstKeys.ROOM) as String
  }

  override fun onEvent(event: Event) {
    when (event.key) {
      EventType.PLAYER_SPAWN -> reset()
      EventType.END_ROOM_TRANS -> {
        val room = event.getProperty(ConstKeys.ROOM) as RectangleMapObject
        if (nextRoomKey == room.name) transitionFinished = true
      }
    }
  }

  override fun reset() {
    timer.reset()
    transitionFinished = false
    state = GateState.OPENABLE
  }

  fun trigger() {
    state = GateState.OPENING
    getMegamanMaverickGame().audioMan.playSound(SoundAsset.BOSS_DOOR, false)
    game.eventsMan.submitEvent(Event(EventType.GATE_INIT_OPENING))
  }

  private fun defineUpdatablesComponent() =
      UpdatablesComponent(
          this,
          {
            if (state == GateState.OPENING) {
              timer.update(it)
              if (timer.isFinished()) {
                GameLogger.debug(TAG, "Set gate to OPENED")
                timer.reset()
                state = GateState.OPEN
                game.eventsMan.submitEvent(Event(EventType.GATE_FINISH_OPENING))
                game.eventsMan.submitEvent(
                    Event(EventType.NEXT_ROOM_REQ, props(ConstKeys.ROOM to nextRoomKey)))
              }
            }

            if (state == GateState.OPEN) {
              if (transitionFinished) {
                GameLogger.debug(TAG, "Set gate to CLOSING")
                transitionFinished = false
                state = GateState.CLOSING
                requestToPlaySound(SoundAsset.BOSS_DOOR, false)
                game.eventsMan.submitEvent(Event(EventType.GATE_INIT_CLOSING))
              }
            }

            if (state == GateState.CLOSING) {
              timer.update(it)
              if (timer.isFinished()) {
                GameLogger.debug(TAG, "Set gate to CLOSED")
                timer.reset()
                state = GateState.CLOSED
                game.eventsMan.submitEvent(Event(EventType.GATE_FINISH_CLOSING))
              }
            }
          })

  private fun defineBodyComponent(): BodyComponent {
    val body = Body(BodyType.ABSTRACT)
    body.setSize(2f * ConstVals.PPM, 3f * ConstVals.PPM)

    // gate fixture
    val gateFixture = Fixture(GameRectangle().set(body), FixtureType.GATE)
    body.addFixture(gateFixture)

    return BodyComponentCreator.create(this, body)
  }

  private fun defineSpritesCompoent(): SpritesComponent {
    val sprite = GameSprite()
    sprite.setSize(4f * ConstVals.PPM, 3f * ConstVals.PPM)

    val SpritesComponent = SpritesComponent(this, "gate" to sprite)
    SpritesComponent.putUpdateFunction("gate") { _, _sprite ->
      _sprite as GameSprite

      when (state) {
        GateState.CLOSING,
        GateState.CLOSED -> _sprite.setPosition(body.getBottomRightPoint(), Position.BOTTOM_RIGHT)
        else -> _sprite.setPosition(body.getBottomLeftPoint(), Position.BOTTOM_LEFT)
      }

      _sprite.hidden = state == GateState.OPEN
      _sprite.setFlip(state == GateState.CLOSING || state == GateState.CLOSED, false)
    }

    return SpritesComponent
  }

  private fun defineAnimationsComponent(): AnimationsComponent {
    val keySupplier = {
      when (state) {
        GateState.OPENABLE,
        GateState.CLOSED -> "closed"
        GateState.OPENING -> "opening"
        GateState.OPEN -> null
        GateState.CLOSING -> "closing"
      }
    }

    val closed = Animation(atlas!!.findRegion("closed"), 1, 1, 1f, true)
    val opening = Animation(atlas!!.findRegion("opening"), 1, 4, 0.125f, false)
    val closing = Animation(opening, reverse = true)

    val animator =
        Animator(
            keySupplier,
            objectMapOf("closed" to closed, "opening" to opening, "closing" to closing))

    return AnimationsComponent(this, animator)
  }
}
