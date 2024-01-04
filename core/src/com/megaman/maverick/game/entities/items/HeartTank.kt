package com.megaman.maverick.game.entities.items

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.common.CAUSE_OF_DEATH_MESSAGE
import com.engine.common.enums.Position
import com.engine.common.extensions.getTextureRegion
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.entities.GameEntity
import com.engine.entities.contracts.IBodyEntity
import com.engine.entities.contracts.ISpriteEntity
import com.engine.events.Event
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.IUpsideDownable
import com.megaman.maverick.game.entities.contracts.ItemEntity
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegaHeartTank
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.getMegamanMaverickGame
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType

class HeartTank(game: MegamanMaverickGame) :
    GameEntity(game), ItemEntity, IBodyEntity, ISpriteEntity, IUpsideDownable {

  companion object {
    const val TAG = "HeartTank"
    private var textureRegion: TextureRegion? = null
  }

  override var upsideDown: Boolean = false

  lateinit var heartTank: MegaHeartTank
    private set

  override fun init() {
    if (textureRegion == null)
        textureRegion = game.assMan.getTextureRegion(TextureAsset.ITEMS_1.source, "HeartTank")
    addComponent(defineBodyComponent())
    addComponent(defineSpritesCompoent())
    addComponent(defineAnimationsComponent())
  }

  override fun spawn(spawnProps: Properties) {
    super.spawn(spawnProps)

    heartTank = MegaHeartTank.get(spawnProps.get(ConstKeys.VALUE, String::class)!!)
    upsideDown = spawnProps.getOrDefault(ConstKeys.UPSIDE_DOWN, false) as Boolean

    val megaman = getMegamanMaverickGame().megaman
    if (megaman.has(heartTank))
        kill(props(CAUSE_OF_DEATH_MESSAGE to "Already have this heart tank."))

    val spawn =
        if (spawnProps.containsKey(ConstKeys.BOUNDS))
            spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
        else spawnProps.get(ConstKeys.POSITION, Vector2::class)!!

    body.setBottomCenterToPoint(spawn)
  }

  override fun contactWithPlayer(megaman: Megaman) {
    kill(props(CAUSE_OF_DEATH_MESSAGE to "Contact with Megaman."))
    game.eventsMan.submitEvent(Event(EventType.ADD_HEART_TANK, props(ConstKeys.VALUE to heartTank)))
  }

  private fun defineBodyComponent(): BodyComponent {
    val body = Body(BodyType.ABSTRACT)
    body.setSize(ConstVals.PPM.toFloat())

    // item fixture
    val itemFixture = Fixture(GameRectangle().setSize(ConstVals.PPM.toFloat()), FixtureType.ITEM)
    body.addFixture(itemFixture)

    return BodyComponentCreator.create(this, body)
  }

  private fun defineSpritesCompoent(): SpritesComponent {
    val sprite = GameSprite()
    sprite.setSize(1.5f * ConstVals.PPM)
    val SpritesComponent = SpritesComponent(this, "heart" to sprite)
    SpritesComponent.putUpdateFunction("heart") { _, _sprite ->
      _sprite as GameSprite

      val position = if (upsideDown) Position.TOP_CENTER else Position.BOTTOM_CENTER
      val bodyPosition = body.getPositionPoint(position)
      _sprite.setPosition(bodyPosition, position)

      _sprite.setFlip(false, upsideDown)
    }
    return SpritesComponent
  }

  private fun defineAnimationsComponent(): AnimationsComponent {
    val animation = Animation(textureRegion!!, 1, 2, 0.15f, true)
    val animator = Animator(animation)
    return AnimationsComponent(this, animator)
  }
}
