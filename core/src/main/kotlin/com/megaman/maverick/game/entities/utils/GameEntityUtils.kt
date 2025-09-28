package com.megaman.maverick.game.entities.utils

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.objects.GamePair
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.cullables.CullableOnEvent
import com.mega.game.engine.cullables.CullableOnUncontained
import com.mega.game.engine.entities.GameEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.events.Event
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.screens.levels.camera.RotatableCamera
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.utils.extensions.toProps
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter

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
    CullableOnUncontained({ camera.getRotatedBounds() }, { bounds().overlaps(it) }, timeToCull)

fun convertObjectPropsToEntitySuppliers(props: Properties): Array<GamePair<() -> MegaGameEntity, Properties>> {
    val childEntitySuppliers = Array<GamePair<() -> MegaGameEntity, Properties>>()

    props.forEach { key, value ->
        if (value is RectangleMapObject) {
            val childProps = value.toProps()
            if (childProps.containsKey(ConstKeys.ENTITY_TYPE)) {
                val entityTypeString = childProps.get(ConstKeys.ENTITY_TYPE, String::class)!!
                val entityType = EntityType.valueOf(entityTypeString.uppercase())

                val childEntitySupplier: () -> MegaGameEntity = { EntityFactories.fetch(entityType, value.name)!! }

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
        body.forEachFixture { fixture -> fixture.setActive(false) }
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
        body.forEachFixture { fixture -> fixture.setActive(true) }
    }
    if (entity is ISpritesEntity) entity.sprites.forEach { it.value.hidden = false }
}

fun setStandardOnTeleportEndProp(entity: GameEntity) {
    entity.putProperty(ConstKeys.ON_TELEPORT_END, { standardOnTeleportEnd(entity) })
}

fun IBodyEntity.moveTowards(target: Vector2, speed: Float, lerp: Boolean = false, lerpScalar: Float = 1f) {
    val velocity = GameObjectPools.fetch(Vector2::class)
        .set(target)
        .sub(body.getCenter())
        .nor()
        .scl(speed)

    if (lerp) body.physics.velocity.lerp(velocity, lerpScalar * Gdx.graphics.deltaTime)
    else body.physics.velocity.set(velocity)
}

fun delayNextPossibleSpawn(game: MegamanMaverickGame, tag: String, mapObjectId: Int, delay: Float) {
    val key = "$tag/$mapObjectId"
    val timer = Timer(delay).setRunOnFinished {
        game.runQueue.addLast {
            game.updatables.remove(key)
        }
    }
    game.updatables.put(key, timer)
}

fun isNextPossibleSpawnDelayed(game: MegamanMaverickGame, tag: String, mapObjectId: Int): Boolean {
    val key = "$tag/$mapObjectId"
    val timer = game.updatables.get(key) as Timer?
    return timer != null && !timer.isFinished()
}

fun onMaxSpawnedByTag(tag: String, max: Int, onMax: (ObjectSet<MegaGameEntity>) -> Unit): Boolean {
    val setOfAll = MegaGameEntities.getOfTag(tag)
    if (setOfAll.size <= max) return false
    onMax.invoke(setOfAll)
    return true
}
