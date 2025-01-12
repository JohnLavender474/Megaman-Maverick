package com.megaman.maverick.game.entities.utils

import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods.getOverlapPushDirection
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.objects.GamePair
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.cullables.CullableOnEvent
import com.mega.game.engine.cullables.CullableOnUncontained
import com.mega.game.engine.entities.GameEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.events.Event
import com.mega.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.screens.levels.camera.RotatableCamera
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.utils.extensions.toProps
import com.megaman.maverick.game.world.body.getBody
import com.megaman.maverick.game.world.body.getBounds
import kotlin.math.abs

fun performStandardShieldReflection(projectileFixture: IFixture, shieldFixture: IFixture) {
    val body = projectileFixture.getBody()
    var direction = getOverlapPushDirection(body.getBounds(), shieldFixture.getShape())
    direction?.let {
        when (it) {
            Direction.UP -> body.physics.velocity.y = abs(body.physics.velocity.y)
            Direction.DOWN -> body.physics.velocity.y = -abs(body.physics.velocity.y)
            Direction.LEFT -> {
                body.physics.velocity.x = -abs(body.physics.velocity.x)
            }

            Direction.RIGHT -> body.physics.velocity.x = abs(body.physics.velocity.x)
        }
    }
}

fun getStandardEventCullingLogic(
    entity: ICullableEntity,
    cullEvents: ObjectSet<Any> = objectSetOf(
        EventType.BEGIN_ROOM_TRANS, EventType.GATE_INIT_OPENING, EventType.PLAYER_SPAWN
    ),
    vararg additionalPredicates: (Event) -> Boolean
): CullableOnEvent {
    if (entity !is MegaGameEntity) throw IllegalArgumentException("Must be a MegaGameEntity: $entity")
    val cullableOnEvents = CullableOnEvent(
        { event -> cullEvents.contains(event.key) && additionalPredicates.all { it.invoke(event) } },
        cullEvents
    )
    val eventsMan = entity.game.eventsMan
    entity.runnablesOnSpawn.put(ConstKeys.CULL_EVENTS) { eventsMan.addListener(cullableOnEvents) }
    entity.runnablesOnDestroy.put(ConstKeys.CULL_EVENTS) { eventsMan.removeListener(cullableOnEvents) }
    return cullableOnEvents
}

fun getGameCameraCullingLogic(entity: IBodyEntity, timeToCull: Float = 1f) =
    getGameCameraCullingLogic((entity as MegaGameEntity).getGameCamera(), { entity.body.getBounds() }, timeToCull)

fun getGameCameraCullingLogic(camera: RotatableCamera, bounds: () -> GameRectangle, timeToCull: Float = 1f) =
    CullableOnUncontained<GameRectangle>({ camera.getRotatedBounds() }, { bounds().overlaps(it) }, timeToCull)

fun getObjectProps(props: Properties, out: Array<RectangleMapObject>): Array<RectangleMapObject> {
    props.forEach { _, value -> if (value is RectangleMapObject) out.add(value) }
    return out
}

fun convertObjectPropsToEntitySuppliers(props: Properties): Array<GamePair<() -> GameEntity, Properties>> {
    val childEntitySuppliers = Array<GamePair<() -> GameEntity, Properties>>()

    props.forEach { key, value ->
        if (value is RectangleMapObject) {
            val childProps = value.toProps()
            if (childProps.containsKey(ConstKeys.ENTITY_TYPE)) {
                val entityTypeString = childProps.get(ConstKeys.ENTITY_TYPE, String::class)!!
                val entityType = EntityType.valueOf(entityTypeString.uppercase())

                val childEntitySupplier: () -> GameEntity = { EntityFactories.fetch(entityType, value.name)!! }

                GameLogger.debug(
                    "convertObjectPropsToEntities()", "entityType=$entityType,name=${value.name}"
                )

                childProps.put(ConstKeys.CHILD_KEY, key)
                childProps.put(ConstKeys.BOUNDS, value.rectangle.toGameRectangle(false))

                childEntitySuppliers.add(childEntitySupplier pairTo childProps)
            }
        }
    }

    return childEntitySuppliers
}

fun standardOnTeleportStart(entity: GameEntity) {
    GameLogger.debug("standardOnTeleportStart()", "entity=$entity")
    if (entity is IBodyEntity) {
        val body = entity.body
        body.physics.velocity.setZero()
        body.physics.collisionOn = false
        body.physics.gravityOn = false
    }
    if (entity is ISpritesEntity) entity.sprites.forEach { it.value.hidden = true }
}

fun setStandardOnTeleportStartProp(entity: GameEntity) {
    entity.putProperty(ConstKeys.ON_TELEPORT_START, { standardOnTeleportStart(entity) })
}

fun setStandardOnTeleportContinueProp(entity: GameEntity) {
    entity.putProperty(ConstKeys.ON_TELEPORT_CONTINUE, { standardOnTeleportStart(entity) })
}

fun standardOnTeleportEnd(entity: GameEntity) {
    GameLogger.debug("standardOnTeleportEnd()", "entity=$entity")
    if (entity is IBodyEntity) {
        val body = entity.body
        body.physics.velocity.setZero()
        body.physics.collisionOn = true
        body.physics.gravityOn = true
    }
    if (entity is ISpritesEntity) entity.sprites.forEach { it.value.hidden = false }
}

fun setStandardOnTeleportEndProp(entity: GameEntity) {
    entity.putProperty(ConstKeys.ON_TELEPORT_END, { standardOnTeleportEnd(entity) })
}
