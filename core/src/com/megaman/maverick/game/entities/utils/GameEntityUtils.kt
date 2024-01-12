package com.megaman.maverick.game.entities.utils

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Array
import com.engine.common.GameLogger
import com.engine.common.objects.Properties
import com.engine.common.shapes.toGameRectangle
import com.engine.cullables.CullableOnUncontained
import com.engine.entities.IGameEntity
import com.engine.entities.contracts.IBodyEntity
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.utils.toGameRectangle
import com.megaman.maverick.game.utils.toProps

fun getGameCameraCullingLogic(entity: IBodyEntity, timeToCull: Float = 1f) =
    CullableOnUncontained(
        { (entity.game as MegamanMaverickGame).getGameCamera().toGameRectangle() },
        { it.overlaps(entity.body as Rectangle) },
        timeToCull)

fun getGameCameraCullingLogic(camera: Camera, bounds: () -> Rectangle, timeToCull: Float = 1f) =
    CullableOnUncontained({ camera.toGameRectangle() }, { it.overlaps(bounds()) }, timeToCull)

fun convertObjectPropsToEntities(props: Properties): Array<Pair<IGameEntity, Properties>> {
  val childEntities = Array<Pair<IGameEntity, Properties>>()

  props.forEach { _, value ->
    if (value is RectangleMapObject) {
      val childProps = value.properties.toProps()
      childProps.put(ConstKeys.BOUNDS, value.rectangle.toGameRectangle())

      val entityType = EntityType.valueOf(childProps.get(ConstKeys.TYPE) as String)
      GameLogger.debug(
          "convertObjectPropsToEntities()", "entityType=$entityType,name=${value.name}")
      val childEntity = EntityFactories.fetch(entityType, value.name)!!

      childEntities.add(childEntity to childProps)
    }
  }

  return childEntities
}
