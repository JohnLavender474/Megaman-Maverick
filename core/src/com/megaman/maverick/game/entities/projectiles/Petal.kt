package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.audio.AudioComponent
import com.engine.common.CAUSE_OF_DEATH_MESSAGE
import com.engine.common.GameLogger
import com.engine.common.extensions.getTextureRegion
import com.engine.common.extensions.objectMapOf
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.damage.IDamageable
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setSize
import com.engine.entities.GameEntity
import com.engine.entities.IGameEntity
import com.engine.entities.contracts.IAudioEntity
import com.engine.points.PointsComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.IHealthEntity
import com.megaman.maverick.game.entities.contracts.IProjectileEntity
import com.megaman.maverick.game.entities.contracts.defineProjectileComponents
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import kotlin.reflect.KClass

class Petal(game: MegamanMaverickGame) :
    GameEntity(game), IProjectileEntity, IAudioEntity, IHealthEntity, IDamager, IDamageable {

  companion object {
    const val TAG = "Petal"
    private var region: TextureRegion? = null
    private const val SPEED = 10f
    private const val DAMAGE_DURATION = 0.25f
  }

  override val invincible = false

  override var owner: IGameEntity? = null

  private val damageNegotiations =
      objectMapOf<KClass<out IDamager>, Int>(
          Bullet::class to 10,
          Fireball::class to ConstVals.MAX_HEALTH,
          ChargedShot::class to ConstVals.MAX_HEALTH,
          ChargedShotExplosion::class to ConstVals.MAX_HEALTH)

  private val damageTimer = Timer(DAMAGE_DURATION)

  override fun init() {
    if (region == null)
        region = game.assMan.getTextureRegion(TextureAsset.PROJECTILES_1.source, "Petal")
    defineProjectileComponents().forEach { addComponent(it) }
    addComponent(AudioComponent(this))
    addComponent(defineBodyComponent())
    addComponent(defineSpriteComponent())
    addComponent(defineAnimationsComponent())
    addComponent(definePointsComponent())
  }

  override fun spawn(spawnProps: Properties) {
    super.spawn(spawnProps)
    setHealth(ConstVals.MAX_HEALTH)
    GameLogger.debug(TAG, "Health: ${getCurrentHealth()}. Spawn props: $spawnProps.")

    val center = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
    body.setCenter(center)

    val trajectory = spawnProps.get(ConstKeys.TRAJECTORY, Vector2::class)!!
    body.physics.velocity.set(trajectory.scl(ConstVals.PPM.toFloat() * SPEED))

    owner = spawnProps.get(ConstKeys.OWNER, IGameEntity::class)
  }

  override fun onDestroy() {
    super<GameEntity>.onDestroy()
    GameLogger.debug(TAG, "Petal destroyed")
  }

  override fun canBeDamagedBy(damager: IDamager) =
      !invincible && damageNegotiations.containsKey(damager::class)

  override fun takeDamageFrom(damager: IDamager): Boolean {
    val damagerKey = damager::class
    if (!damageNegotiations.containsKey(damagerKey)) return false

    damageTimer.reset()

    val damage = damageNegotiations[damagerKey]
    getHealthPoints().translate(-damage)
    requestToPlaySound(SoundAsset.ENEMY_DAMAGE_SOUND, false)
    return true
  }

  private fun definePointsComponent(): PointsComponent {
    val pointsComponent = PointsComponent(this)
    pointsComponent.putPoints(ConstKeys.HEALTH, ConstVals.MAX_HEALTH)
    pointsComponent.putListener(ConstKeys.HEALTH) {
      if (it.current <= 0) kill(props(CAUSE_OF_DEATH_MESSAGE to "Health depleted"))
    }
    return pointsComponent
  }

  private fun defineBodyComponent(): BodyComponent {
    val body = Body(BodyType.ABSTRACT)
    body.setSize(0.9f * ConstVals.PPM)

    val debugShapes = Array<() -> IDrawableShape?>()

    // body fixture
    val bodyFixture = Fixture(GameRectangle().setSize(0.4f * ConstVals.PPM), FixtureType.BODY)
    body.addFixture(bodyFixture)

    // projectile fixture
    val projectileFixture =
        Fixture(GameRectangle().setSize(0.4f * ConstVals.PPM), FixtureType.PROJECTILE)
    body.addFixture(projectileFixture)
    // debugShapes.add { projectileFixture.shape }

    // damager fixture
    val damagerFixture = Fixture(GameRectangle().setSize(0.4f * ConstVals.PPM), FixtureType.DAMAGER)
    body.addFixture(damagerFixture)
    debugShapes.add { damagerFixture.shape }

    // damageable fixture
    val damageableFixture =
        Fixture(GameRectangle().setSize(0.4f * ConstVals.PPM), FixtureType.DAMAGEABLE)
    body.addFixture(damageableFixture)
    // debugShapes.add { damageableFixture.shape }

    addComponent(DrawableShapesComponent(this, debugShapeSuppliers = debugShapes, debug = true))

    return BodyComponentCreator.create(this, body)
  }

  private fun defineSpriteComponent(): SpritesComponent {
    val sprite = GameSprite()
    sprite.setSize(1.25f * ConstVals.PPM)

    val spritesComponent = SpritesComponent(this, "petal" to sprite)
    spritesComponent.putUpdateFunction("petal") { _, _sprite ->
      _sprite as GameSprite

      val center = body.getCenter()
      _sprite.setCenter(center.x, center.y)
    }

    return spritesComponent
  }

  private fun defineAnimationsComponent(): AnimationsComponent {
    val animation = Animation(region!!, 1, 4, 0.015f, true)
    val animator = Animator(animation)
    return AnimationsComponent(this, animator)
  }
}
