package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.common.enums.Facing
import com.engine.common.enums.Position
import com.engine.common.extensions.getTextureRegion
import com.engine.common.extensions.objectMapOf
import com.engine.common.interfaces.IFaceable
import com.engine.common.interfaces.Updatable
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.damage.IDamager
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.IProjectileEntity
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.utils.VelocityAlteration
import com.megaman.maverick.game.utils.getMegamanMaverickGame
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import com.megaman.maverick.game.world.getEntity
import com.megaman.maverick.game.world.setVelocityAlteration
import kotlin.reflect.KClass

class Matasaburo(game: MegamanMaverickGame) : AbstractEnemy(game), IFaceable {

  companion object {
    const val TAG = "Matasaburo"
    private const val BLOW_FORCE = 15f
    private var matasaburoReg: TextureRegion? = null
  }

  override var facing = Facing.RIGHT

  override val damageNegotiations =
      objectMapOf<KClass<out IDamager>, Int>(
          Bullet::class to 10,
          Fireball::class to ConstVals.MAX_HEALTH,
          ChargedShot::class to 10,
          ChargedShotExplosion::class to 5)

  override fun init() {
    super.init()
    if (matasaburoReg == null)
        matasaburoReg = game.assMan.getTextureRegion(TextureAsset.ENEMIES_1.source, "Matasaburo")
    addComponent(defineAnimationsComponent())
  }

  override fun spawn(spawnProps: Properties) {
    super.spawn(spawnProps)
    val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
    body.setBottomCenterToPoint(spawn)
  }

  override fun defineBodyComponent(): BodyComponent {
    val body = Body(BodyType.DYNAMIC)
    body.setSize(ConstVals.PPM.toFloat())

    // blow fixture
    val blowFixture =
        Fixture(
            GameRectangle().setSize(10f * ConstVals.PPM, 1.15f * ConstVals.PPM), FixtureType.FORCE)
    blowFixture.setVelocityAlteration { fixture, _ ->
      val entity = fixture.getEntity()
      if (entity is AbstractEnemy) return@setVelocityAlteration VelocityAlteration.addNone()
      if (entity is IProjectileEntity) entity.owner = null
      val force = BLOW_FORCE * ConstVals.PPM * facing.value
      return@setVelocityAlteration VelocityAlteration.add(force, 0f)
    }
    body.addFixture(blowFixture)

    // damager fixture
    val damagerFixture =
        Fixture(GameRectangle().setSize(0.85f * ConstVals.PPM), FixtureType.DAMAGER)
    body.addFixture(damagerFixture)

    // damageable fixture
    val damageableFixture =
        Fixture(GameRectangle().setSize(ConstVals.PPM.toFloat()), FixtureType.DAMAGEABLE)
    body.addFixture(damageableFixture)

    // pre-process
    body.preProcess = Updatable {
      val offsetX = 5f * ConstVals.PPM * facing.value
      blowFixture.offsetFromBodyCenter.x = offsetX
    }

    return BodyComponentCreator.create(this, body)
  }

  override fun defineSpritesComponent(): SpritesComponent {
    val sprite = GameSprite()
    sprite.setSize(1.5f * ConstVals.PPM)
    val SpritesComponent = SpritesComponent(this, "matasaburo" to sprite)
    SpritesComponent.putUpdateFunction("matasaburo") { _, _sprite ->
      _sprite as GameSprite
      val position = body.getBottomCenterPoint()
      _sprite.setPosition(position, Position.BOTTOM_CENTER)
      _sprite.setFlip(facing == Facing.LEFT, false)
    }
    return SpritesComponent
  }

  override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
    super.defineUpdatablesComponent(updatablesComponent)
    updatablesComponent.add {
      facing = if (getMegamanMaverickGame().megaman.body.x > body.x) Facing.RIGHT else Facing.LEFT
    }
  }

  private fun defineAnimationsComponent(): AnimationsComponent {
    val animation = Animation(matasaburoReg!!, 1, 6, 0.1f, true)
    val animator = Animator(animation)
    return AnimationsComponent(this, animator)
  }
}
