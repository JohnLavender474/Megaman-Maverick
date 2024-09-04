package com.megaman.maverick.game.screens.levels.map.layers

import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.toGameRectangle
import com.mega.game.engine.screens.levels.tiledmap.builders.ITiledMapLayerBuilder
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.SensorsFactory
import com.megaman.maverick.game.utils.toProps

class SensorsLayerBuilder : ITiledMapLayerBuilder {

    override fun build(layer: MapLayer, returnProps: Properties) {
        layer.objects.forEach { mapObject ->
            if (mapObject is RectangleMapObject) {
                val name = mapObject.name
                val props = mapObject.toProps()
                props.put(ConstKeys.BOUNDS, mapObject.rectangle.toGameRectangle())

                when (name) {
                    "Death" -> {
                        val death = EntityFactories.fetch(EntityType.SENSOR, SensorsFactory.DEATH)!!
                        death.spawn(props)
                    }

                    "Gate" -> {
                        val gate = EntityFactories.fetch(EntityType.SENSOR, SensorsFactory.GATE)!!
                        gate.spawn(props)
                    }

                    else -> throw IllegalArgumentException("Unknown sensor type: $name")
                }
            }
        }
    }
}
