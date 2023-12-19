package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.CAUSE_OF_DEATH_MESSAGE
import com.engine.common.enums.Position
import com.engine.common.extensions.getTextureRegion
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpriteComponent
import com.engine.drawables.sprites.setPosition
import com.engine.entities.GameEntity
import com.engine.entities.contracts.ISpriteEntity
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.DecorationsFactory
import kotlin.math.ceil

/** A splash decoration. */
class Splash(game: MegamanMaverickGame) : GameEntity(game), ISpriteEntity {

  companion object {
    private const val SPLASH_REGION_KEY = "Water/Splash"
    private const val ALPHA = .5f

    private var splashRegion: TextureRegion? = null

    /** Generates splash decorations based on the overlap between the splasher and water body. */
    fun generate(game: MegamanMaverickGame, splasher: Body, water: Body) {
      val numSplashes = ceil(splasher.width / ConstVals.PPM).toInt()
      for (i in 0 until numSplashes) {
        val spawn =
            Vector2(splasher.x + ConstVals.PPM / 2f + i * ConstVals.PPM, water.y + water.height)
        val s = EntityFactories.fetch(EntityType.DECORATION, DecorationsFactory.SPLASH)
        game.gameEngine.spawn(s!!, props(ConstKeys.POSITION to spawn))
      }
    }
  }

  private lateinit var animation: IAnimation

  override fun init() {
    if (splashRegion == null)
        splashRegion =
            game.assMan.getTextureRegion(TextureAsset.ENVIRONS_1.source, SPLASH_REGION_KEY)

    addComponent(defineSpriteComponent())
    addComponent(defineAnimationsComponent())
    addComponent(defineUpdatablesComponent())
  }

  override fun spawn(spawnProps: Properties) {
    super.spawn(spawnProps)
    val spawn = spawnProps.get(ConstKeys.POSITION) as Vector2
    (firstSprite as GameSprite).setPosition(spawn, Position.BOTTOM_CENTER)
  }

  private fun defineUpdatablesComponent() =
      UpdatablesComponent(
          this,
          {
            if (animation.isFinished()) kill(props(CAUSE_OF_DEATH_MESSAGE to "Animation finished"))
          })

  private fun defineAnimationsComponent(): AnimationsComponent {
    animation = Animation(splashRegion!!, 1, 5, 0.075f, false)
    return AnimationsComponent(this, Animator(animation))
  }

  private fun defineSpriteComponent(): SpriteComponent {
    val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, -1))
    sprite.setRegion(splashRegion!!)
    sprite.setAlpha(ALPHA)

    return SpriteComponent(this, "splash" to sprite)
  }
}
