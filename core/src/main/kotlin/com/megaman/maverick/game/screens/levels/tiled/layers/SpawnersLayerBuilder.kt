package com.megaman.maverick.game.screens.levels.tiled.layers

import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.screens.levels.tiledmap.builders.ITiledMapLayerBuilder
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.screens.levels.spawns.SpawnType
import com.megaman.maverick.game.spawns.ISpawner
import com.megaman.maverick.game.spawns.Spawn
import com.megaman.maverick.game.spawns.SpawnerFactory
import com.megaman.maverick.game.spawns.SpawnerShapeFactory
import com.megaman.maverick.game.utils.extensions.convertToProps
import kotlin.reflect.KClass

class SpawnersLayerBuilder(private val params: MegaMapLayerBuildersParams) : ITiledMapLayerBuilder {

    companion object {
        const val TAG = "SpawnersLayerBuilder"
    }

    override fun build(layer: MapLayer, returnProps: Properties) {
        val game = params.game

        if (!returnProps.containsKey(ConstKeys.DISPOSABLES)) returnProps.put(ConstKeys.DISPOSABLES, Array<Disposable>())
        val disposables = returnProps.get(ConstKeys.DISPOSABLES) as Array<Disposable>

        if (!returnProps.containsKey(ConstKeys.SPAWNERS)) returnProps.put(ConstKeys.SPAWNERS, Array<ISpawner>())
        val spawners = returnProps.get(ConstKeys.SPAWNERS) as Array<ISpawner>

        val entityType = when (layer.name) {
            ConstKeys.DECORATIONS -> EntityType.DECORATION
            ConstKeys.ENEMIES -> EntityType.ENEMY
            ConstKeys.ITEMS -> EntityType.ITEM
            ConstKeys.BLOCKS -> EntityType.BLOCK
            ConstKeys.SPECIALS -> EntityType.SPECIAL
            ConstKeys.HAZARDS -> EntityType.HAZARD
            ConstKeys.PROJECTILES -> EntityType.PROJECTILE
            else -> throw IllegalArgumentException("Unknown spawner type: ${layer.name}")
        }
        val shouldTestSpawnerPredicate: (Float) -> Boolean = when (entityType) {
            EntityType.BLOCK,
            EntityType.HAZARD,
            EntityType.DECORATION -> {
                { true }
            }

            else -> {
                { !game.isProperty(ConstKeys.ROOM_TRANSITION, true) }
            }
        }

        GameLogger.debug(TAG, "build(): layerName=${layer.name}, entityType=$entityType")

        layer.objects.forEach {
            val spawnProps = it.convertToProps()

            var name = it.name
            if (name == null) name = when (entityType) {
                EntityType.BLOCK -> Block.TAG
                else -> throw IllegalStateException("Name cannot be blank in layer ${layer.name}: spawnProps=$spawnProps")
            }

            val clazz: KClass<out MegaGameEntity>
            try {
                clazz = Class.forName(entityType.getFullyQualifiedName(name)).kotlin as KClass<out MegaGameEntity>
            } catch (e: Exception) {
                GameLogger.error(
                    TAG, "Failed to create spawner for entity: name=${name}, layer.name=${layer.name}", e
                )

                return@forEach
            }

            val spawnType = spawnProps.get(ConstKeys.SPAWN_TYPE, String::class)
            if (spawnType == SpawnType.SPAWN_NOW) {
                val entity = MegaEntityFactory.fetch(clazz) ?: throw IllegalStateException(
                    "Entity of type $entityType not found: $name"
                )

                entity.spawn(spawnProps)

                return@forEach
            }

            val spawnSupplier = {
                val entity = MegaEntityFactory.fetch(clazz) ?: throw IllegalStateException(
                    "Entity of type $entityType not found: $name"
                )
                Spawn(entity, spawnProps)
            }

            val respawnable = spawnProps.getOrDefault(ConstKeys.RESPAWNABLE, true, Boolean::class)

            when (spawnType) {
                SpawnType.SPAWN_ROOM -> {
                    val roomName = it.properties.get(SpawnType.SPAWN_ROOM, String::class.java)!!

                    GameLogger.debug(TAG, "build(): adding SPAWN_ROOM spawner: entity=${name}, room=$roomName")

                    val spawner = SpawnerFactory.spawnerForOnEvent(
                        predicate = { event ->
                            val currentRoom = game.getCurrentRoom()?.name
                            val shouldSpawn = currentRoom == roomName
                            GameLogger.debug(
                                TAG,
                                "build(): " +
                                    "entity=$name, " +
                                    "shouldSpawn=$shouldSpawn, " +
                                    "entityRoom=$roomName, " +
                                    "megamanRoom=$currentRoom"
                            )
                            shouldSpawn
                        },
                        eventKeyMask = objectSetOf<Any>(
                            EventType.PLAYER_READY,
                            EventType.BEGIN_ROOM_TRANS,
                            EventType.SET_TO_ROOM_NO_TRANS
                        ),
                        respawnable = respawnable,
                        spawnSupplier = spawnSupplier,
                        shouldTest = shouldTestSpawnerPredicate
                    )
                    spawners.add(spawner)

                    game.eventsMan.addListener(spawner)
                    disposables.add { game.eventsMan.removeListener(spawner) }
                }

                SpawnType.SPAWN_EVENT -> {
                    val events = ObjectSet<Any>()

                    val eventNames = spawnProps.get(ConstKeys.EVENTS, String::class)!!.split(",")
                    eventNames.forEach { eventName ->
                        val eventType = EventType.valueOf(eventName.uppercase())
                        events.add(eventType)
                    }

                    val spawner = SpawnerFactory.spawnerForWhenEventCalled(
                        events = events,
                        respawnable = respawnable,
                        spawnSupplier = spawnSupplier,
                        shouldTest = shouldTestSpawnerPredicate
                    )
                    spawners.add(spawner)

                    GameLogger.debug(TAG, "build(): adding SPAWN_EVENT spawne: entity=$name")

                    game.eventsMan.addListener(spawner)
                    disposables.add { game.eventsMan.removeListener(spawner) }
                }

                else -> {
                    val spawner = SpawnerFactory.spawnerForWhenInCamera(
                        camera = game.getGameCamera(),
                        spawnShape = SpawnerShapeFactory.getSpawnShape(entityType, it),
                        respawnable = respawnable,
                        spawnSupplier = spawnSupplier,
                        shouldTest = shouldTestSpawnerPredicate
                    )
                    spawners.add(spawner)

                    GameLogger.debug(TAG, "build(): adding GAME_CAM_BOUNDS spawner: entity=${it.name}")
                }
            }
        }
    }
}
