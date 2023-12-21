package com.megaman.maverick.game.entities

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.utils.Array
import com.engine.audio.AudioComponent
import com.engine.common.extensions.objectSetOf
import com.engine.common.extensions.overlaps
import com.engine.components.IGameComponent
import com.engine.cullables.CullableOnEvent
import com.engine.cullables.CullableOnUncontained
import com.engine.cullables.CullablesComponent
import com.engine.damage.IDamageable
import com.engine.damage.IDamager
import com.engine.entities.IGameEntity
import com.engine.entities.contracts.IAudioEntity
import com.engine.entities.contracts.IBodyEntity
import com.engine.entities.contracts.ISpriteEntity
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.events.EventType

/**
 * A projectile entity that can be fired by different entities. It extends [IGameEntity] and
 * implements [IBodyEntity], [ISpriteEntity], [IAudioEntity], and [IGameEntity].
 */
interface IProjectileEntity : IDamager, IBodyEntity, ISpriteEntity, IAudioEntity, IGameEntity {

  /** The owner of the projectile (the entity that fired it). */
  var owner: IGameEntity?

  override fun canDamage(damageable: IDamageable) =
      damageable != owner && damageable !is IProjectileEntity

  override fun onDamageInflictedTo(damageable: IDamageable) {
    // do nothing
  }

  /**
   * Called when the projectile hits a body fixture.
   *
   * @param bodyFixture The body fixture that was hit.
   */
  fun hitBody(bodyFixture: Fixture) {}

  /**
   * Called when the projectile hits a block fixture.
   *
   * @param blockFixture The block fixture that was hit.
   */
  fun hitBlock(blockFixture: Fixture) {}

  /**
   * Called when the projectile hits a shield fixture.
   *
   * @param shieldFixture The shield fixture that was hit.
   */
  fun hitShield(shieldFixture: Fixture) {}

  /**
   * Called when the projectile hits a water fixture.
   *
   * @param waterFixture The water fixture that was hit.
   */
  fun hitWater(waterFixture: Fixture) {}
}

/** Defines some common components for the [IProjectileEntity]. */
internal fun IProjectileEntity.defineProjectileComponents(): Array<IGameComponent> {
  val components = Array<IGameComponent>()

  // add audio component
  components.add(AudioComponent(this))

  // cull on events: player spawn, begin room transition, and gate init opening
  val cullEvents =
      objectSetOf<Any>(
          EventType.PLAYER_SPAWN, EventType.BEGIN_ROOM_TRANS, EventType.GATE_INIT_OPENING)
  val cullOnEvent = CullableOnEvent({ cullEvents.contains(it.key) }, cullEvents)

  // cull on out of game camera
  val cullOnOutOfGameCam =
      CullableOnUncontained<Camera>(
          containerSupplier = { game.viewports.get(ConstKeys.GAME).camera },
          containable = { it.overlaps(body) })
  components.add(CullablesComponent(this, cullOnEvent, cullOnOutOfGameCam))

  return components
}
