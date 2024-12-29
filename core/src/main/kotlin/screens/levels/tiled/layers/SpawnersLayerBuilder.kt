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
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.screens.levels.spawns.SpawnType
import com.megaman.maverick.game.spawns.ISpawner
import com.megaman.maverick.game.spawns.Spawn
import com.megaman.maverick.game.spawns.SpawnerFactory
import com.megaman.maverick.game.spawns.SpawnerShapeFactory
import com.megaman.maverick.game.utils.extensions.convertToProps

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

        GameLogger.debug(TAG, "build(): layerName=${layer.name}, entityType=$entityType")

        layer.objects.forEach {
            val spawnProps = it.convertToProps()

            val spawnType = spawnProps.get(ConstKeys.SPAWN_TYPE, String::class)
            if (spawnType == SpawnType.SPAWN_NOW) {
                if (it.name == null) throw IllegalStateException("Entity name not found for spawn now")

                val entity = EntityFactories.fetch(entityType, it.name) ?: throw IllegalStateException(
                    "Entity of type $entityType not found: ${it.name}"
                )

                entity.spawn(spawnProps)

                return@forEach
            }

            val spawnSupplier = {
                val entity = EntityFactories.fetch(entityType, it.name ?: "")
                    ?: throw IllegalStateException("Entity of type $entityType not found: ${it.name}")

                Spawn(entity, spawnProps)
            }

            val respawnable = spawnProps.getOrDefault(ConstKeys.RESPAWNABLE, true, Boolean::class)

            when (spawnType) {
                SpawnType.SPAWN_ROOM -> {
                    val roomName = it.properties.get(SpawnType.SPAWN_ROOM, String::class.java)!!

                    GameLogger.debug(TAG, "build(): adding SPAWN_ROOM spawner: entity=${it.name}, room=$roomName")

                    val spawner = SpawnerFactory.spawnerForOnEvent(
                        predicate = { event ->
                            val currentRoom = game.getCurrentRoom()?.name
                            val shouldSpawn = currentRoom == roomName
                            GameLogger.debug(
                                TAG,
                                "build(): " +
                                    "entity=${it.name}, " +
                                    "shouldSpawn=$shouldSpawn, " +
                                    "entityRoom=$roomName, " +
                                    "megamanRoom=$currentRoom"
                            )
                            shouldSpawn
                        },
                        eventKeyMask = objectSetOf<Any>(
                            EventType.PLAYER_READY,
                            EventType.SET_TO_ROOM_NO_TRANS,
                            EventType.END_ROOM_TRANS
                        ),
                        spawnSupplier = spawnSupplier,
                        respawnable = respawnable
                    )
                    spawners.add(spawner)

                    game.eventsMan.addListener(spawner)
                    disposables.add { game.eventsMan.removeListener(spawner) }
                }

                SpawnType.SPAWN_EVENT -> {
                    val events = ObjectSet<Any>()

                    val eventNames = (spawnProps.get(ConstKeys.EVENTS) as String).split(",")
                    eventNames.forEach { eventName ->
                        val eventType = EventType.valueOf(eventName.uppercase())
                        events.add(eventType)
                    }

                    val spawner = SpawnerFactory.spawnerForWhenEventCalled(events, spawnSupplier, respawnable)
                    spawners.add(spawner)

                    GameLogger.debug(TAG, "build(): adding SPAWN_EVENT spawne: entity=${it.name}")

                    game.eventsMan.addListener(spawner)
                    disposables.add { game.eventsMan.removeListener(spawner) }
                }

                else -> {
                    val spawnRoomTrans =
                        spawnProps.getOrDefault("${ConstKeys.SPAWN}_${ConstKeys.ROOM_TRANSITION}", false, Boolean::class)

                    val spawner = SpawnerFactory.spawnerForWhenInCamera(
                        game.getGameCamera(),
                        SpawnerShapeFactory.getSpawnShape(entityType, it),
                        spawnSupplier,
                        respawnable
                    )
                    spawners.add(spawner)

                    GameLogger.debug(TAG, "build(): adding GAME_CAM_BOUNDS spawner: entity=${it.name}")
                }
            }
        }
    }
}
