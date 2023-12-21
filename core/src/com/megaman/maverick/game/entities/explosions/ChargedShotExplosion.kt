package com.megaman.maverick.game.entities.explosions

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.common.CAUSE_OF_DEATH_MESSAGE
import com.engine.common.enums.Facing
import com.engine.common.enums.Position
import com.engine.common.extensions.getTextureRegion
import com.engine.common.extensions.objectMapOf
import com.engine.common.interfaces.IFaceable
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpriteComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.entities.GameEntity
import com.engine.entities.IGameEntity
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
import com.megaman.maverick.game.entities.IProjectileEntity
import com.megaman.maverick.game.entities.defineProjectileComponents
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType

/**
 * ChargedShotExplosion class represents an explosion caused by a charged shot in the game. It
 * extends [GameEntity] and implements [IProjectileEntity] and [IFaceable].
 *
 * @param game The game instance to which this explosion belongs.
 */
class ChargedShotExplosion(game: MegamanMaverickGame) :
    GameEntity(game), IProjectileEntity, IFaceable {

  /**
   * A companion object that contains shared properties and textures for ChargedShotExplosion
   * instances.
   */
  companion object {
    private const val FULLY_CHARGED_DURATION = .6f
    private const val HALF_CHARGED_DURATION = .3f
    private const val SOUND_INTERVAL = .15f

    private var fullyChargedRegion: TextureRegion? = null
    private var halfChargedRegion: TextureRegion? = null
  }

  /** The owner of the charged shot that caused this explosion. */
  override var owner: IGameEntity? = null

  /** The facing direction of the explosion (either [Facing.RIGHT] or [Facing.LEFT]). */
  override var facing = Facing.RIGHT

  /** Whether the explosion was caused by a fully charged shot or not. */
  var fullyCharged = false
    private set

  /** Timer for the duration of the explosion. */
  private var durationTimer = Timer(FULLY_CHARGED_DURATION)

  /** Timer for controlling the sound effect interval during the explosion. */
  private val soundTimer = Timer(SOUND_INTERVAL)

  /** Initializes the ChargedShotExplosion by defining its components and animations. */
  override fun init() {
    if (fullyChargedRegion == null)
        fullyChargedRegion =
            game.assMan.getTextureRegion(TextureAsset.MEGAMAN_CHARGED_SHOT.source, "Collide")

    if (halfChargedRegion == null)
        halfChargedRegion =
            game.assMan.getTextureRegion(TextureAsset.EXPLOSIONS_1.source, "HalfChargedShot")

    defineProjectileComponents().forEach { addComponent(it) }
    addComponent(defineBodyComponent())
    addComponent(defineSpriteComponent())
    addComponent(defineAnimationsComponent())
    addComponent(defineUpdatablesComponent())
  }

  /**
   * Spawns the explosion with specified properties, including the owner, direction, and charge
   * level.
   *
   * @param spawnProps Properties for spawning the explosion, including owner, direction, and charge
   *   level.
   */
  override fun spawn(spawnProps: Properties) {
    super.spawn(spawnProps)
    soundTimer.reset()

    owner = spawnProps.get(ConstKeys.OWNER) as IGameEntity
    facing = spawnProps.get(ConstKeys.DIRECTION) as Facing
    fullyCharged = spawnProps.get(ConstKeys.BOOLEAN) as Boolean

    durationTimer = Timer(if (fullyCharged) FULLY_CHARGED_DURATION else HALF_CHARGED_DURATION)

    val spawn = spawnProps.get(ConstKeys.POSITION) as Vector2
    body.setCenter(spawn)

    val spriteDimension = (if (fullyCharged) 1.75f else 1.25f) * ConstVals.PPM
    (firstSprite as GameSprite).setSize(spriteDimension)
  }

  /**
   * Defines the updatables component for the explosion, including the duration and sound timers.
   *
   * @return The configured updatables component.
   */
  private fun defineUpdatablesComponent() =
      UpdatablesComponent(
          this,
          {
            durationTimer.update(it)
            if (durationTimer.isFinished()) {
              kill(props(CAUSE_OF_DEATH_MESSAGE to "Duration timer finished"))
            }

            soundTimer.update(it)
            if (soundTimer.isFinished()) {
              requestToPlaySound(SoundAsset.ENEMY_DAMAGE_SOUND, false)
              soundTimer.reset()
            }
          })

  /**
   * Defines the body component of the explosion, including a damager fixture.
   *
   * @return The configured body component.
   */
  private fun defineBodyComponent(): BodyComponent {
    val body = Body(BodyType.ABSTRACT)
    body.setSize(ConstVals.PPM.toFloat(), ConstVals.PPM.toFloat())

    // Damager fixture
    val damagerFixture =
        Fixture(GameRectangle().setSize(ConstVals.PPM.toFloat()), FixtureType.DAMAGER)
    body.addFixture(damagerFixture)

    return BodyComponentCreator.create(this, body)
  }

  /**
   * Defines the sprite component of the explosion, which includes the graphical representation of
   * the explosion.
   *
   * @return The configured sprite component.
   */
  private fun defineSpriteComponent(): SpriteComponent {
    val sprite = GameSprite()
    val spriteComponent = SpriteComponent(this, "explosion" to sprite)
    spriteComponent.putUpdateFunction("explosion") { _, _sprite ->
      (_sprite as GameSprite).setPosition(body.getCenter(), Position.CENTER)
      _sprite.setFlip(facing == Facing.LEFT, false)
    }
    return spriteComponent
  }

  /**
   * Defines the animations component for the explosion, including animations for fully charged and
   * half charged states.
   *
   * @return The configured animations component.
   */
  private fun defineAnimationsComponent(): AnimationsComponent {
    val chargedAnimation = Animation(fullyChargedRegion!!, 1, 3, .05f, true)
    val halfChargedAnimation = Animation(halfChargedRegion!!, 1, 3, .05f, true)
    val animator =
        Animator(
            { if (fullyCharged) "charged" else "halfCharged" },
            objectMapOf("charged" to chargedAnimation, "halfCharged" to halfChargedAnimation))
    return AnimationsComponent(this, animator)
  }
}
