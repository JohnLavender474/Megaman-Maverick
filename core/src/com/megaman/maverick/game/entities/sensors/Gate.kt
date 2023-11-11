package com.megaman.maverick.game.entities.sensors

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
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
import com.engine.drawables.sprites.SpriteComponent
import com.engine.drawables.sprites.setPosition
import com.engine.entities.GameEntity
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
import com.megaman.maverick.game.world.FixtureType

/** A gate that opens when the player comes into contact with it. */
class Gate(game: MegamanMaverickGame) :
    GameEntity(game), IBodyEntity, ISpriteEntity, IEventListener, Resettable {

  /** The state of this gate. */
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

  private lateinit var gateState: GateState
  private lateinit var nextRoomKey: String

  private var transitionFinished = false

  override fun init() {
    if (atlas == null) atlas = game.assMan.getTextureAtlas(TextureAsset.GATES.source)

    addComponent(defineBodyComponent())
    addComponent(defineUpdatablesComponent())
    addComponent(defineSpriteComponent())
    addComponent(defineAnimationsComponent())

    runnablesOnDestroy.add { game.eventsMan.removeListener(this) }
  }

  override fun spawn(spawnProps: Properties) {
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
    gateState = GateState.OPENABLE
  }

  private fun defineUpdatablesComponent() =
      UpdatablesComponent(
          this,
          {
            when (gateState) {
              // opening
              GateState.OPENING -> {
                timer.update(it)
                if (timer.isFinished()) {
                  GameLogger.debug(TAG, "Set gate to OPENED")
                  timer.reset()
                  gateState = GateState.OPEN
                  game.eventsMan.submitEvent(Event(EventType.GATE_FINISH_OPENING))
                  game.eventsMan.submitEvent(
                      Event(EventType.NEXT_ROOM_REQ, props(ConstKeys.ROOM to nextRoomKey)))
                }
              }

              // open
              GateState.OPEN -> {
                if (transitionFinished) {
                  GameLogger.debug(TAG, "Set gate to CLOSING")
                  transitionFinished = false
                  gateState = GateState.CLOSING
                  (game as MegamanMaverickGame)
                      .audioMan
                      .playSound(SoundAsset.BOSS_DOOR.source, false)
                  game.eventsMan.submitEvent(Event(EventType.GATE_INIT_CLOSING))
                }
              }

              // closing
              GateState.CLOSING -> {
                timer.update(it)
                if (timer.isFinished()) {
                  GameLogger.debug(TAG, "Set gate to CLOSED")
                  timer.reset()
                  gateState = GateState.CLOSED
                  game.eventsMan.submitEvent(Event(EventType.GATE_FINISH_CLOSING))
                }
              }
              else -> {}
            }
          })

  private fun defineBodyComponent(): BodyComponent {
    val body = Body(BodyType.ABSTRACT)
    body.setSize(2f * ConstVals.PPM, 3f * ConstVals.PPM)

    // gate fixture
    val gateFixture = Fixture(body.copy(), FixtureType.GATE)
    body.addFixture(gateFixture)

    return BodyComponent(this, body)
  }

  private fun defineSpriteComponent(): SpriteComponent {
    val sprite = GameSprite()
    sprite.setSize(4f * ConstVals.PPM, 3f * ConstVals.PPM)

    val spriteComponent = SpriteComponent(this, "gate" to sprite)
    spriteComponent.putUpdateFunction("gate") { _, _sprite ->
      _sprite as GameSprite

      when (gateState) {
        GateState.CLOSING,
        GateState.CLOSED -> _sprite.setPosition(body.getBottomRightPoint(), Position.BOTTOM_RIGHT)
        else -> _sprite.setPosition(body.getBottomLeftPoint(), Position.BOTTOM_LEFT)
      }

      _sprite.hidden = gateState == GateState.OPEN

      _sprite.setFlip(gateState == GateState.CLOSING || gateState == GateState.CLOSED, false)
    }

    return spriteComponent
  }

  private fun defineAnimationsComponent(): AnimationsComponent {
    val keySupplier = {
      when (gateState) {
        GateState.OPENABLE,
        GateState.CLOSED -> "closed"
        GateState.OPENING -> "opening"
        GateState.OPEN -> null
        GateState.CLOSING -> "closing"
      }
    }

    val closed = Animation(atlas!!.findRegion("closed"), 1, 1, 1f, true)
    val opening = Animation(atlas!!.findRegion("opening"), 1, 4, 0.125f, false)
    val closing = Animation(atlas!!.findRegion("closing"), 1, 4, 1f, true)

    val animator =
        Animator(
            keySupplier,
            objectMapOf("closed" to closed, "opening" to opening, "closing" to closing))

    return AnimationsComponent(this, animator)
  }
}
