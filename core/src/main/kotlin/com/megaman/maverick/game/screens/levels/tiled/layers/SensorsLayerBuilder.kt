package com.megaman.maverick.game.screens.levels.tiled.layers

import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.screens.levels.tiledmap.builders.ITiledMapLayerBuilder
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.screens.levels.tiled.layers.SpawnersLayerBuilder.Companion.TAG
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.utils.extensions.toProps
import kotlin.reflect.KClass

class SensorsLayerBuilder : ITiledMapLayerBuilder {

    override fun build(layer: MapLayer, returnProps: Properties) {
        layer.objects.forEach { mapObject ->
            if (mapObject is RectangleMapObject) {
                val name = mapObject.name

                val props = mapObject.toProps()
                props.put(ConstKeys.BOUNDS, mapObject.rectangle.toGameRectangle())

                val clazz: KClass<out MegaGameEntity>

                try {
                    clazz = Class.forName(
                        EntityType.SENSOR.getFullyQualifiedName(name)
                    ).kotlin as KClass<out MegaGameEntity>
                } catch (e: Exception) {
                    GameLogger.error(
                        TAG, "Failed to create spawner for entity: name=${name}, layer.name=${layer.name}", e
                    )
                    return@forEach
                }

                val entity = MegaEntityFactory.fetch(clazz)!!
                entity.spawn(props)
            }
        }
    }
}
