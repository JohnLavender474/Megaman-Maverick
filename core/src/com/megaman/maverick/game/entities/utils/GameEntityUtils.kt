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
import com.engine.entities.contracts.ISpritesEntity
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaGameEntity
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.utils.toGameRectangle
import com.megaman.maverick.game.utils.toProps

fun getGameCameraCullingLogic(entity: IBodyEntity, timeToCull: Float = 1f) = CullableOnUncontained(
    { (entity as MegaGameEntity).game.getGameCamera().toGameRectangle() },
    { it.overlaps(entity.body as Rectangle) },
    timeToCull
)

fun getGameCameraCullingLogic(camera: Camera, bounds: () -> Rectangle, timeToCull: Float = 1f) =
    CullableOnUncontained({ camera.toGameRectangle() }, { it.overlaps(bounds()) }, timeToCull)

fun getObjectProps(props: Properties): Array<RectangleMapObject> {
    val objectProps = Array<RectangleMapObject>()
    props.forEach { _, value -> if (value is RectangleMapObject) objectProps.add(value) }
    return objectProps
}

fun convertObjectPropsToEntitySuppliers(props: Properties): Array<Pair<() -> IGameEntity, Properties>> {
    val childEntitySuppliers = Array<Pair<() -> IGameEntity, Properties>>()

    props.forEach { key, value ->
        if (value is RectangleMapObject) {
            val childProps = value.toProps()
            if (childProps.containsKey(ConstKeys.ENTITY_TYPE)) {
                val entityTypeString = childProps.get(ConstKeys.ENTITY_TYPE) as String
                val entityType = EntityType.valueOf(entityTypeString.uppercase())

                val childEntitySupplier: () -> IGameEntity = {
                    EntityFactories.fetch(entityType, value.name)!!
                }

                GameLogger.debug(
                    "convertObjectPropsToEntities()", "entityType=$entityType,name=${value.name}"
                )

                childProps.put(ConstKeys.CHILD_KEY, key)
                childProps.put(ConstKeys.BOUNDS, value.rectangle.toGameRectangle())

                childEntitySuppliers.add(childEntitySupplier to childProps)
            }
        }
    }

    return childEntitySuppliers
}

fun standardOnPortalHopperStart(entity: IGameEntity) {
    GameLogger.debug("standardOnPortalHopperStart()", "entity=$entity")
    if (entity is IBodyEntity) {
        val body = entity.body
        body.physics.velocity.setZero()
        body.physics.collisionOn = false
        body.physics.gravityOn = false
    }
    if (entity is ISpritesEntity) entity.sprites.forEach { it.value.hidden = true }
}

fun setStandardOnPortalHopperStartProp(entity: IGameEntity) {
    entity.putProperty(ConstKeys.ON_PORTAL_HOPPER_START, { standardOnPortalHopperStart(entity) })
}

fun setStandardOnPortalHopperContinueProp(entity: IGameEntity) {
    entity.putProperty(ConstKeys.ON_PORTAL_HOPPER_CONTINUE, { standardOnPortalHopperStart(entity) })
}

fun standardOnPortalHopperEnd(entity: IGameEntity) {
    GameLogger.debug("standardOnPortalHopperEnd()", "entity=$entity")
    if (entity is IBodyEntity) {
        val body = entity.body
        body.physics.velocity.setZero()
        body.physics.collisionOn = true
        body.physics.gravityOn = true
    }
    if (entity is ISpritesEntity) entity.sprites.forEach { it.value.hidden = false }
}

fun setStandardOnPortalHopperEndProp(entity: IGameEntity) {
    entity.putProperty(ConstKeys.ON_PORTAL_HOPPER_END, { standardOnPortalHopperEnd(entity) })
}
