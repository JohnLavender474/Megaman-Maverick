package com.megaman.maverick.game.screens.levels.map.layers

import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.engine.common.objects.Properties
import com.engine.common.shapes.toGameRectangle
import com.engine.screens.levels.tiledmap.builders.ITiledMapLayerBuilder
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.SensorsFactory
import com.megaman.maverick.game.utils.toProps

class SensorsLayerBuilder(private val game: MegamanMaverickGame) : ITiledMapLayerBuilder {

    override fun build(layer: MapLayer, returnProps: Properties) {
        layer.objects.forEach { mapObject ->
            if (mapObject is RectangleMapObject) {
                val name = mapObject.name
                val props = mapObject.toProps()
                props.put(ConstKeys.BOUNDS, mapObject.rectangle.toGameRectangle())

                when (name) {
                    "Death" -> {
                        val death = EntityFactories.fetch(EntityType.SENSOR, SensorsFactory.DEATH)!!
                        game.gameEngine.spawn(death, props)
                    }

                    "Gate" -> {
                        val gate = EntityFactories.fetch(EntityType.SENSOR, SensorsFactory.GATE)!!
                        game.gameEngine.spawn(gate, props)
                    }

                    else -> throw IllegalArgumentException("Unknown sensor type: $name")
                }
            }
        }
    }
}
