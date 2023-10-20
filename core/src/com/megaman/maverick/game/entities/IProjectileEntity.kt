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
import com.engine.entities.IGameEntity
import com.engine.entities.contracts.IBodyEntity
import com.engine.entities.contracts.IDamagerEntity
import com.engine.entities.contracts.ISpriteEntity
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.events.EventType

interface IProjectileEntity : IBodyEntity, ISpriteEntity, IDamagerEntity, IGameEntity {

  var owner: IGameEntity?

  fun hitBody(bodyFixture: Fixture) {}

  fun hitBlock(blockFixture: Fixture) {}

  fun hitShield(shieldFixture: Fixture) {}

  fun hitWater(waterFixture: Fixture) {}
}

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
