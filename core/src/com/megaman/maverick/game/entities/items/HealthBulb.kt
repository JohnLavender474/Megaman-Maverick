package com.megaman.maverick.game.entities.items

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.CAUSE_OF_DEATH_MESSAGE
import com.engine.common.GameLogger
import com.engine.common.enums.Position
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.extensions.objectSetOf
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.TimeMarkedRunnable
import com.engine.common.time.Timer
import com.engine.cullables.CullableOnEvent
import com.engine.cullables.CullablesComponent
import com.engine.drawables.shapes.DrawableShapeComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpriteComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.entities.GameEntity
import com.engine.entities.contracts.IBodyEntity
import com.engine.entities.contracts.ISpriteEntity
import com.engine.events.Event
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.ItemEntity
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType

class HealthBulb(game: MegamanMaverickGame) :
    GameEntity(game), ItemEntity, ISpriteEntity, IBodyEntity {

  companion object {
    const val TAG = "HealthBulb"

    const val SMALL_HEALTH = 3
    const val LARGE_HEALTH = 6

    private var textureAtlas: TextureAtlas? = null

    private const val TIME_TO_BLINK = 2f
    private const val BLINK_DUR = .01f
    private const val CULL_DUR = 3.5f
  }

  private val blinkTimer = Timer(BLINK_DUR)
  private val cullTimer = Timer(CULL_DUR)

  private lateinit var itemFixture: Fixture

  private var large = false
  private var timeCull = false
  private var blink = false
  private var warning = false

  override fun init() {
    if (textureAtlas == null)
        textureAtlas = game.assMan.getTextureAtlas(TextureAsset.ITEMS_1.source)

    cullTimer.setRunnables(
        gdxArrayOf(
            TimeMarkedRunnable(TIME_TO_BLINK) { warning = true },
        ))

    addComponent(defineBodyComponent())
    addComponent(defineSpriteComponent())
    addComponent(defineAnimationsComponent())
    addComponent(defineCullablesComponent())
    addComponent(defineUpdatablesComponent())
  }

  override fun spawn(spawnProps: Properties) {
    super.spawn(spawnProps)

    val spawn =
        if (spawnProps.containsKey(ConstKeys.BOUNDS))
            spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        else spawnProps.get(ConstKeys.POSITION, Vector2::class)!!

    large = spawnProps.get(ConstKeys.LARGE) as Boolean

    timeCull =
        !spawnProps.containsKey(ConstKeys.TIMED) ||
            spawnProps.get(ConstKeys.TIMED, Boolean::class)!!

    warning = false
    blink = false

    blinkTimer.setToEnd()
    cullTimer.reset()

    body.setSize((if (large) 0.5f else 0.25f) * ConstVals.PPM)
    body.setCenter(spawn)
    (itemFixture.shape as GameRectangle).set(body)
  }

  override fun contactWithPlayer(megaman: Megaman) {
    GameLogger.debug(TAG, "contactWithPlayer")
    kill(props(CAUSE_OF_DEATH_MESSAGE to "Megaman touched the health bulb!"))
    game.eventsMan.submitEvent(
        Event(
            EventType.ADD_PLAYER_HEALTH,
            props(ConstKeys.VALUE to (if (large) LARGE_HEALTH else SMALL_HEALTH))))
  }

  private fun defineBodyComponent(): BodyComponent {
    val body = Body(BodyType.ABSTRACT)
    body.physics.gravityOn = false

    val shapes = Array<() -> IDrawableShape>()

    // item fixture
    itemFixture = Fixture(GameRectangle(), FixtureType.ITEM)
    body.addFixture(itemFixture)

    itemFixture.shape.color = Color.PURPLE
    shapes.add { itemFixture.shape }

    addComponent(DrawableShapeComponent(this, debugShapeSuppliers = shapes, debug = true))

    return BodyComponentCreator.create(this, body)
  }

  private fun defineSpriteComponent(): SpriteComponent {
    val sprite = GameSprite()
    sprite.setSize(1.5f * ConstVals.PPM)

    val spriteComponent = SpriteComponent(this, "healthBulb" to sprite)
    spriteComponent.putUpdateFunction("healthBulb") { _, _sprite ->
      _sprite as GameSprite
      _sprite.setPosition(body.getBottomCenterPoint(), Position.BOTTOM_CENTER)
      _sprite.hidden = blink
    }

    return spriteComponent
  }

  private fun defineAnimationsComponent(): AnimationsComponent {
    val keySupplier: () -> String = { if (large) "large" else "small" }
    val animations =
        objectMapOf<String, IAnimation>(
            "large" to Animation(textureAtlas!!.findRegion("HealthBulb"), 1, 2, 0.15f, true),
            "small" to Animation(textureAtlas!!.findRegion("SmallHealthBulb")))
    val animator = Animator(keySupplier, animations)
    return AnimationsComponent(this, animator)
  }

  private fun defineCullablesComponent(): CullablesComponent {
    val eventsToCullOn = objectSetOf<Any>(EventType.GAME_OVER)
    val cullOnEvent = CullableOnEvent({ eventsToCullOn.contains(it.key) }, eventsToCullOn)
    return CullablesComponent(this, cullOnEvent)
  }

  private fun defineUpdatablesComponent() =
      UpdatablesComponent(
          this,
          {
            if (!timeCull) return@UpdatablesComponent

            if (warning) {
              blinkTimer.update(it)
              if (blinkTimer.isJustFinished()) {
                blinkTimer.reset()
                blink = !blink
              }
            }

            cullTimer.update(it)
            if (cullTimer.isFinished()) kill(props(CAUSE_OF_DEATH_MESSAGE to "Time's up!"))
          })
}
