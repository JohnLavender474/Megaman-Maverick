package com.megaman.maverick.game.screens.levels.map.layers

import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.toGameRectangle
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
import com.megaman.maverick.game.utils.convertToProps

class SpawnersLayerBuilder(private val params: MegaMapLayerBuildersParams) : ITiledMapLayerBuilder {

    companion object {
        const val TAG = "SpawnersLayerBuilder"
    }

    override fun build(layer: MapLayer, returnProps: Properties) {
        GameLogger.debug(TAG, "build(): Building spawners for layer: ${layer.name}")
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
        GameLogger.debug(TAG, "build(): Entity type: $entityType")

        layer.objects.forEach {
            val spawnProps = it.convertToProps()

            val spawnType = spawnProps.get(ConstKeys.SPAWN_TYPE) as String?
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
                    val roomName = it.properties.get(SpawnType.SPAWN_ROOM) as String
                    val gameRooms = returnProps.get(ConstKeys.GAME_ROOMS) as Array<RectangleMapObject>

                    var roomFound = false
                    for (room in gameRooms) if (roomName == room.name) {
                        spawnProps.put(ConstKeys.ROOM, room)
                        val spawner = SpawnerFactory.spawnerForWhenInCamera(
                            game.getGameCamera(), room.rectangle.toGameRectangle(), spawnSupplier, respawnable
                        )
                        spawners.add(spawner)

                        GameLogger.debug(TAG, "build(): Adding spawner $spawner for game rectangle object ${it.name}")

                        roomFound = true
                        break
                    }

                    check(roomFound) { "Room not found: $roomName" }
                }

                SpawnType.SPAWN_EVENT -> {
                    val events = ObjectSet<Any>()
                    val eventNames = (spawnProps.get(ConstKeys.EVENTS) as String).split(",")
                    eventNames.forEach { eventName ->
                        val eventType = EventType.valueOf(eventName)
                        events.add(eventType)
                    }

                    val spawner = SpawnerFactory.spawnerForWhenEventCalled(events, spawnSupplier, respawnable)
                    spawners.add(spawner)

                    GameLogger.debug(TAG, "build(): Adding spawner $spawner for game rectangle object ${it.name}")

                    game.eventsMan.addListener(spawner)
                    disposables.add { game.eventsMan.removeListener(spawner) }
                }

                else -> {
                    val spawner = SpawnerFactory.spawnerForWhenInCamera(
                        game.getGameCamera(),
                        SpawnerShapeFactory.getSpawnShape(entityType, it),
                        spawnSupplier,
                        respawnable
                    )
                    spawners.add(spawner)

                    GameLogger.debug(TAG, "build(): Adding spawner $spawner for game rectangle object ${it.name}")
                }
            }
        }
    }
}
