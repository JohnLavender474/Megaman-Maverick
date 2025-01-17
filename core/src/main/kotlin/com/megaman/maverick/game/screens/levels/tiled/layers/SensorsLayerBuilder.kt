package com.megaman.maverick.game.screens.levels.tiled.layers

import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.screens.levels.tiledmap.builders.ITiledMapLayerBuilder
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.SensorsFactory
import com.megaman.maverick.game.entities.sensors.Death
import com.megaman.maverick.game.entities.sensors.Gate
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.utils.extensions.toProps

class SensorsLayerBuilder : ITiledMapLayerBuilder {

    override fun build(layer: MapLayer, returnProps: Properties) {
        layer.objects.forEach { mapObject ->
            if (mapObject is RectangleMapObject) {
                val name = mapObject.name
                val props = mapObject.toProps()
                props.put(ConstKeys.BOUNDS, mapObject.rectangle.toGameRectangle())

                when (name) {
                    Death.TAG -> {
                        val death = EntityFactories.fetch(EntityType.SENSOR, SensorsFactory.DEATH)!!
                        death.spawn(props)
                    }

                    Gate.TAG -> {
                        val gate = EntityFactories.fetch(EntityType.SENSOR, SensorsFactory.GATE)!!
                        gate.spawn(props)
                    }

                    else -> throw IllegalArgumentException("Unknown sensor type: $name")
                }
            }
        }
    }
}
