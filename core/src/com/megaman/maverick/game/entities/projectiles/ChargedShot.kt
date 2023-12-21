package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.common.CAUSE_OF_DEATH_MESSAGE
import com.engine.common.enums.Direction
import com.engine.common.enums.Facing
import com.engine.common.enums.Position
import com.engine.common.extensions.getTextureRegion
import com.engine.common.extensions.objectMapOf
import com.engine.common.interfaces.IFaceable
import com.engine.common.interfaces.swapFacing
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.damage.IDamageable
import com.engine.drawables.shapes.DrawableShapeComponent
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
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.IProjectileEntity
import com.megaman.maverick.game.entities.defineProjectileComponents
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import com.megaman.maverick.game.world.getEntity

/**
 * ChargedShot class represents a projectile, which can be fully or half charged, in the game. It
 * extends [GameEntity] and implements [IProjectileEntity] and [IFaceable].
 *
 * @param game The game instance to which this charged shot belongs.
 */
class ChargedShot(game: MegamanMaverickGame) : GameEntity(game), IProjectileEntity, IFaceable {

  /** A companion object that contains shared properties and textures for ChargedShot instances. */
  companion object {
    private var fullyChargedRegion: TextureRegion? = null
    private var halfChargedRegion: TextureRegion? = null
  }

  /** The owner of the charged shot (the entity that fired it). */
  override var owner: IGameEntity? = null

  /** The facing direction of the charged shot (either [Facing.RIGHT] or [Facing.LEFT]). */
  override var facing = Facing.RIGHT

  /** The trajectory of the charged shot. */
  private val trajectory = Vector2()

  /** Whether the charged shot is fully charged or not. */
  var fullyCharged = false
    private set

  /** Initializes the ChargedShot by defining its components and animations. */
  override fun init() {
    if (fullyChargedRegion == null)
        fullyChargedRegion =
            game.assMan.getTextureRegion(TextureAsset.MEGAMAN_CHARGED_SHOT.source, "Shoot")

    if (halfChargedRegion == null)
        halfChargedRegion =
            game.assMan.getTextureRegion(TextureAsset.PROJECTILES_1.source, "HalfChargedShot")

    defineProjectileComponents().forEach { addComponent(it) }
    addComponent(defineBodyComponent())
    addComponent(defineSpriteComponent())
    addComponent(defineAnimationsComponent())
    addComponent(defineUpdatablesComponent())
  }

  /**
   * Spawns the charged shot with specified properties.
   *
   * @param spawnProps Properties for spawning the charged shot, including position and charge
   *   level.
   */
  override fun spawn(spawnProps: Properties) {
    super.spawn(spawnProps)

    owner = spawnProps.get(ConstKeys.OWNER) as IGameEntity?

    fullyCharged = spawnProps.get(ConstKeys.BOOLEAN) as Boolean

    var bodyDimension = .75f * ConstVals.PPM
    var spriteDimension = ConstVals.PPM.toFloat()

    if (fullyCharged) spriteDimension *= 1.5f else bodyDimension /= 2f
    (firstSprite as GameSprite).setSize(spriteDimension)

    body.setSize(bodyDimension)
    body.fixtures.forEach { (it.second.shape as GameRectangle).setSize(bodyDimension) }

    val _trajectory = spawnProps.get(ConstKeys.TRAJECTORY) as Vector2
    trajectory.set(_trajectory.scl(ConstVals.PPM.toFloat()))

    facing = if (trajectory.x > 0f) Facing.RIGHT else Facing.LEFT

    val spawn = spawnProps.get(ConstKeys.POSITION) as Vector2
    body.setCenter(spawn)
  }

  /** Handles damage inflicted to the charged shot by exploding and marking it as dead. */
  override fun onDamageInflictedTo(damageable: IDamageable) = explodeAndDie()

  /**
   * Handles a collision with a block fixture by exploding and marking the charged shot as dead.
   *
   * @param blockFixture The fixture representing the block collided with.
   */
  override fun hitBlock(blockFixture: Fixture) = explodeAndDie()

  /**
   * Handles a collision with a shield fixture by deflecting the charged shot.
   *
   * @param shieldFixture The fixture representing the shield collided with.
   */
  override fun hitShield(shieldFixture: Fixture) {
    owner = shieldFixture.getEntity()
    swapFacing()
    trajectory.x *= -1f

    val deflection =
        if (shieldFixture.properties.containsKey(ConstKeys.DIRECTION))
            shieldFixture.properties.get(ConstKeys.DIRECTION) as Direction
        else Direction.UP

    when (deflection) {
      Direction.UP -> trajectory.y = 5f * ConstVals.PPM
      Direction.DOWN -> trajectory.y = -5f * ConstVals.PPM
      else -> trajectory.y = 0f
    }
    requestToPlaySound(SoundAsset.DINK_SOUND, false)
  }

  /** Explodes the charged shot and marks it as dead, spawning an explosion entity. */
  private fun explodeAndDie() {
    kill(props(CAUSE_OF_DEATH_MESSAGE to "Explode and die"))
    val e = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.CHARGED_SHOT_EXPLOSION)
    val props =
        props(
            ConstKeys.POSITION to body.getCenter(),
            ConstKeys.OWNER to owner,
            ConstKeys.DIRECTION to facing,
            ConstKeys.BOOLEAN to fullyCharged,
        )
    game.gameEngine.spawn(e!!, props)
  }

  /**
   * Defines the updatables component which sets the trajectory every frame.
   *
   * @return The configured updatables component
   */
  private fun defineUpdatablesComponent() =
      UpdatablesComponent(this, { body.physics.velocity.set(trajectory) })

  /**
   * Defines the body component of the charged shot, including fixtures.
   *
   * @return The configured body component.
   */
  private fun defineBodyComponent(): BodyComponent {
    val body = Body(BodyType.ABSTRACT)

    // Projectile fixture
    val projectileFixture = Fixture(GameRectangle(), FixtureType.PROJECTILE)
    body.addFixture(projectileFixture)

    // Damager fixture
    val damagerFixture = Fixture(GameRectangle(), FixtureType.DAMAGER)
    body.addFixture(damagerFixture)

    // add drawable shape component for debugging
    addComponent(DrawableShapeComponent(this, { body }))

    return BodyComponentCreator.create(this, body)
  }

  /**
   * Defines the animations component for the charged shot, including animations for fully charged
   * and half charged states.
   *
   * @return The configured animations component.
   */
  private fun defineAnimationsComponent(): AnimationsComponent {
    val chargedAnimation = Animation(fullyChargedRegion!!, 1, 2, 0.05f, true)
    val halfChargedAnimation = Animation(halfChargedRegion!!, 1, 2, 0.05f, true)
    val animator =
        Animator(
            { if (fullyCharged) "charged" else "half" },
            objectMapOf("charged" to chargedAnimation, "half" to halfChargedAnimation))
    return AnimationsComponent(this, animator)
  }

  /**
   * Defines the sprite component of the charged shot, which includes the shot's graphical
   * representation.
   *
   * @return The configured sprite component.
   */
  private fun defineSpriteComponent(): SpriteComponent {
    val sprite = GameSprite()
    val spriteComponent = SpriteComponent(this, "shot" to sprite)
    spriteComponent.putUpdateFunction("shot") { _, _sprite ->
      _sprite.setFlip(facing == Facing.LEFT, false)
      (_sprite as GameSprite).setPosition(body.getCenter(), Position.CENTER)
    }
    return spriteComponent
  }
}
