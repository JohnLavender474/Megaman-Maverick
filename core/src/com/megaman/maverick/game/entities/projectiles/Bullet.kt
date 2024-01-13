package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.engine.common.CAUSE_OF_DEATH_MESSAGE
import com.engine.common.enums.Direction
import com.engine.common.enums.Position
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.getTextureRegion
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.damage.IDamageable
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.entities.GameEntity
import com.engine.entities.IGameEntity
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
import com.megaman.maverick.game.entities.contracts.IDirectionRotatable
import com.megaman.maverick.game.entities.contracts.IProjectileEntity
import com.megaman.maverick.game.entities.contracts.defineProjectileComponents
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import com.megaman.maverick.game.world.getEntity

class Bullet(game: MegamanMaverickGame) : GameEntity(game), IProjectileEntity, IDirectionRotatable {

  companion object {
    private const val CLAMP = 10f
    private var bulletRegion: TextureRegion? = null
  }

  override var owner: IGameEntity? = null
  override lateinit var directionRotation: Direction

  override fun init() {
    defineProjectileComponents().forEach { addComponent(it) }
    addComponent(defineBodyComponent())
    addComponent(defineSpritesCompoent())
  }

  override fun spawn(spawnProps: Properties) {
    super.spawn(spawnProps)

    owner = spawnProps.get(ConstKeys.OWNER) as IGameEntity?

    val spawn = spawnProps.get(ConstKeys.POSITION) as Vector2
    body.setCenter(spawn)

    directionRotation = spawnProps.getOrDefault(ConstKeys.DIRECTION, Direction.UP, Direction::class)

    val trajectory = spawnProps.get(ConstKeys.TRAJECTORY) as Vector2
    body.physics.velocity.set(trajectory.scl(ConstVals.PPM.toFloat()))
  }

  override fun onDamageInflictedTo(damageable: IDamageable) = disintegrate("Damage inflicted")

  override fun hitBlock(blockFixture: Fixture) = disintegrate("Hit block")

  override fun hitShield(shieldFixture: Fixture) {
    owner = shieldFixture.getEntity()

    val trajectory = body.physics.velocity.cpy()
    if (isDirectionRotatedVertically()) trajectory.x *= -1f else trajectory.y *= -1f

    val deflection =
        if (shieldFixture.properties.containsKey(ConstKeys.DIRECTION))
            shieldFixture.properties.get(ConstKeys.DIRECTION) as Direction
        else Direction.UP

    when (deflection) {
      Direction.UP -> trajectory.y = 5f * ConstVals.PPM
      Direction.DOWN -> trajectory.y = -5f * ConstVals.PPM
      Direction.LEFT -> trajectory.x = -5f * ConstVals.PPM
      Direction.RIGHT -> trajectory.x = 5f * ConstVals.PPM
    }

    body.physics.velocity.set(trajectory)
    requestToPlaySound(SoundAsset.DINK_SOUND, false)
  }

  fun disintegrate(causeOfDeathMessage: String = "Disintegration") {
    kill(props(CAUSE_OF_DEATH_MESSAGE to causeOfDeathMessage))
    val disintegration =
        EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.DISINTEGRATION)
    game.gameEngine.spawn(disintegration!!, props(ConstKeys.POSITION to body.getCenter()))
  }

  private fun defineBodyComponent(): BodyComponent {
    val body = Body(BodyType.ABSTRACT)
    body.setSize(.15f * ConstVals.PPM)
    body.physics.velocityClamp.set(CLAMP * ConstVals.PPM, CLAMP * ConstVals.PPM)

    // body fixture
    val bodyFixture = Fixture(GameRectangle().set(body), FixtureType.BODY)
    bodyFixture.putProperty(ConstKeys.GRAVITY_ROTATABLE, false)
    body.addFixture(bodyFixture)

    // projectile fixture
    val projectileFixture =
        Fixture(GameRectangle().setSize(.2f * ConstVals.PPM), FixtureType.PROJECTILE)
    body.addFixture(projectileFixture)

    // damager fixture
    val damagerFixture = Fixture(GameRectangle().setSize(.2f * ConstVals.PPM), FixtureType.DAMAGER)
    body.addFixture(damagerFixture)

    addComponent(
        DrawableShapesComponent(this, debugShapeSuppliers = gdxArrayOf({ body }), debug = true))

    return BodyComponentCreator.create(this, body)
  }

  private fun defineSpritesCompoent(): SpritesComponent {
    if (bulletRegion == null)
        bulletRegion = game.assMan.getTextureRegion(TextureAsset.PROJECTILES_1.source, "Bullet")

    val sprite = GameSprite(bulletRegion!!, DrawingPriority(DrawingSection.PLAYGROUND, 4))
    sprite.setSize(1.25f * ConstVals.PPM)

    val SpritesComponent = SpritesComponent(this, "bullet" to sprite)
    SpritesComponent.putUpdateFunction("bullet") { _, _sprite ->
      (_sprite as GameSprite).setPosition(body.getCenter(), Position.CENTER)

      val rotation = if (directionRotation.isVertical()) 0f else 90f
      _sprite.setOriginCenter()
      _sprite.rotation = rotation
    }

    return SpritesComponent
  }
}
