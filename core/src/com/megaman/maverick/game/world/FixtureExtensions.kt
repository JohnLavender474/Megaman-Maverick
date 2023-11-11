@file:Suppress("UNCHECKED_CAST")

package com.megaman.maverick.game.world

import com.engine.entities.contracts.IBodyEntity
import com.engine.entities.contracts.IDamageableEntity
import com.engine.entities.contracts.IDamagerEntity
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.contracts.IHealthEntity
import com.megaman.maverick.game.utils.VelocityAlteration

/**
 * Sets the [IBodyEntity] of this fixture. This method MUST be called for every fixture when it is
 * defined.
 *
 * @param entity the entity to set
 * @return this fixture for chaining
 */
fun Fixture.setEntity(entity: IBodyEntity): Fixture {
  properties.put(ConstKeys.ENTITY, entity)
  return this
}

/**
 * Returns the entity of this fixture. Throws exception if no [IBodyEntity] has been set.
 *
 * @return the entity of this fixture
 */
fun Fixture.getEntity() = properties.get(ConstKeys.ENTITY) as IBodyEntity

/**
 * Sets the entity of this fixture to be damaged by the damager that owns the provided fixture. If
 * this fixture's entity is not a [IDamageableEntity] and the entity of the provided fixture is not
 * a [IDamagerEntity], then nothing happens. Otherwise, the provided fixture's entity's damager is
 * added to this fixture's entity's damageable.
 *
 * @param damagerFixture the fixture whose entity's damager will be added to this fixture's entity's
 *   damageable
 * @return true if the entity of this fixture is a [IDamageableEntity] and the entity of the
 *   provided fixture is a [IDamagerEntity], otherwise false
 */
fun Fixture.setDamagedBy(damagerFixture: Fixture): Boolean {
  val damageable = getEntity()
  if (damageable !is IDamageableEntity) return false

  val damager = damagerFixture.getEntity()
  if (damager !is IDamagerEntity) return false

  damageable.addDamager(damager)
  return true
}

/** Depletes the health of the entity that owns this fixture. */
fun Fixture.depleteHealth(): Boolean {
  val entity = getEntity()
  if (entity !is IHealthEntity) return false

  entity.getHealthPoints().setToMin()
  return true
}

/**
 * Returns the body of this fixture. Throws exception if no [IBodyEntity] has been set.
 *
 * @return the body of this fixture
 */
fun Fixture.getBody() = getEntity().body

/**
 * Returns if the body of this fixture has the provided body type. Throws exception if no
 * [IBodyEntity] has been set.
 *
 * @param type the body type to check
 * @return true if the body of this fixture has the provided body type, otherwise false
 */
fun Fixture.bodyHasType(type: BodyType) = getBody().isBodyType(type)

/**
 * Returns if the body of this fixture has the provided body label. Throws exception if no
 * [IBodyEntity] has been set.
 *
 * @param label the body label to check
 * @return true if the body of this fixture has the provided body label, otherwise false
 */
fun Fixture.bodyHasLabel(label: BodyLabel) = getBody().hasBodyLabel(label)

/**
 * Returns if the body of this fixture is sensing the provided body sense. Throws exception if no
 * [IBodyEntity] has been set.
 *
 * @param sense the body sense to check
 * @return true if the body of this fixture is sensing the provided body sense, otherwise false
 */
fun Fixture.bodyIsSensing(sense: BodySense) = getBody().isSensing(sense)

/**
 * Sets the velocity alteration of this fixture.
 *
 * @param alteration the velocity alteration to set
 * @return this fixture for chaining
 */
fun Fixture.setVelocityAlteration(alteration: VelocityAlteration): Fixture {
  properties.put(ConstKeys.VELOCITY_ALTERATION, alteration)
  return this
}

/**
 * Gets the velocity alteration of this fixture or null if none has been set. Throws exception if
 * none has been set.
 *
 * @return the velocity alteration of this fixture or null if none has been set
 */
fun Fixture.getVelocityAlteration() =
    properties.get(ConstKeys.VELOCITY_ALTERATION) as VelocityAlteration

/**
 * Sets the runnable of this fixture.
 *
 * @param runnable the runnable to set
 * @return this fixture for chaining
 */
fun Fixture.setRunnable(runnable: () -> Unit): Fixture {
  properties.put(ConstKeys.RUNNABLE, runnable)
  return this
}

/**
 * Gets the runnable of this fixture or null if none has been set.
 *
 * @return the runnable of this fixture or null
 */
fun Fixture.getRunnable() = properties.get(ConstKeys.RUNNABLE) as (() -> Unit)?
