@file:Suppress("UNCHECKED_CAST")

package com.megaman.maverick.game.screens.levels.map.layers

import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.ObjectSet
import com.engine.common.GameLogger
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.toGameRectangle
import com.engine.screens.levels.tiledmap.builders.ITiledMapLayerBuilder
import com.engine.spawns.ISpawner
import com.engine.spawns.Spawn
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.screens.levels.spawns.SpawnType
import com.megaman.maverick.game.spawns.SpawnerFactory
import com.megaman.maverick.game.utils.toProps

class SpawnersLayerBuilder(private val params: MegaMapLayerBuildersParams) : ITiledMapLayerBuilder {

  companion object {
    const val TAG = "SpawnersLayerBuilder"
  }

  override fun build(layer: MapLayer, returnProps: Properties) {
    GameLogger.debug(TAG, "build(): Building spawners for layer: ${layer.name}")
    val game = params.game

    // define disposables array if necessary and get it
    // these disposables are run when the level is disposed
    if (!returnProps.containsKey(ConstKeys.DISPOSABLES))
        returnProps.put(ConstKeys.DISPOSABLES, Array<Disposable>())
    val disposables = returnProps.get(ConstKeys.DISPOSABLES) as Array<Disposable>

    // define spawners array if necessary and get it
    if (!returnProps.containsKey(ConstKeys.SPAWNERS))
        returnProps.put(ConstKeys.SPAWNERS, Array<ISpawner>())
    val spawners = returnProps.get(ConstKeys.SPAWNERS) as Array<ISpawner>

    // get the entity type from the layer name
    val entityType =
        when (layer.name) {
          ConstKeys.ENEMIES -> EntityType.ENEMY
          ConstKeys.ITEMS -> EntityType.ITEM
          ConstKeys.BLOCKS -> EntityType.BLOCK
          // TODO: add other entity types
          else -> throw IllegalArgumentException("Unknown spawner type: ${layer.name}")
        }
    GameLogger.debug(TAG, "build(): Entity type: $entityType")

    // cycle through each object in the layer to create a spawner for each object
    layer.objects.forEach {
      // objects all MUST be rectangular
      if (it is RectangleMapObject) {
        // convert the MapProperties to a Properties instance
        val spawnProps = it.properties.toProps()
        spawnProps.put(ConstKeys.BOUNDS, it.rectangle)

        // create spawner
        val spawnSupplier = {
          Spawn(EntityFactories.fetch(entityType, it.name ?: "", props())!!, spawnProps)
        }

        // if there is a specified spawn type, then create that type of spawner
        val spawnType = spawnProps.get(ConstKeys.SPAWN_TYPE) as String?
        when (spawnType) {
          // spawn when a room is entered
          SpawnType.SPAWN_ROOM -> {
            // if the spawn type is SPAWN_ROOM, then get the room name and find the room
            // with that name
            val roomName = it.properties.get(SpawnType.SPAWN_ROOM) as String
            val gameRooms = returnProps.get(ConstKeys.GAME_ROOMS) as Array<RectangleMapObject>

            var roomFound = false
            for (room in gameRooms) if (roomName == room.name) {
              spawnProps.put(ConstKeys.ROOM, room)
              val spawner =
                  SpawnerFactory.spawnerForWhenEnteringCamera(
                      game.getGameCamera(), room.rectangle.toGameRectangle(), spawnSupplier)
              spawners.add(spawner)

              GameLogger.debug(TAG, "build(): Adding spawner: $spawner")

              roomFound = true
              break
            }

            check(roomFound) { "Room not found: $roomName" }
          }
          // spawn when the specified event is called
          SpawnType.SPAWN_EVENT -> {
            // collect events from the events prop
            val events = ObjectSet<Any>()
            val eventNames = (spawnProps.get(ConstKeys.EVENTS) as String).split(",")
            eventNames.forEach { eventName ->
              val eventType = EventType.valueOf(eventName)
              events.add(eventType)
            }

            // add spawner to spawners array and as an event listener to the game
            // this spawn should be removed as an event listener when the level is disposed
            val spawner = SpawnerFactory.spawnerForWhenEventCalled(events, spawnSupplier)
            spawners.add(spawner)

            GameLogger.debug(TAG, "build(): Adding spawner: $spawner")

            game.eventsMan.addListener(spawner)
            disposables.add { game.eventsMan.removeListener(spawner) }
          }
          // if spawn type is not specified, then create a spawner using the following logic
          else -> {
            when (it.name) {
              // TODO: add other spawn types
              else -> {
                val spawner =
                    SpawnerFactory.spawnerForWhenEnteringCamera(
                        game.getGameCamera(), it.rectangle.toGameRectangle(), spawnSupplier)
                spawners.add(spawner)

                GameLogger.debug(TAG, "build(): Adding spawner: $spawner")
              }
            }
          }
        }
      }
    }
  }
}

/*
else if (o.getName() != null) {
                            switch (o.getName()) {
                                case ItemFactory.HEART_TANK -> {
                                    if (game.getMegaman().has(MegaHeartTank.get((String) data.get(ConstKeys.VAL)))) {
                                        continue;
                                    }
                                    spawns.add(new SpawnWhenInBounds(engine, gameCam, o.getRectangle(), data,
                                            () -> factories.fetch(entityType, o.getName()), false));
                                }
                                default -> spawns.add(new SpawnWhenInBounds(engine, gameCam, o.getRectangle(), data,
                                        () -> factories.fetch(entityType, o.getName())));
                            }
 */
