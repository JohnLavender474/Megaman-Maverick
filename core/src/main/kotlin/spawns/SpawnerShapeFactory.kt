package com.megaman.maverick.game.spawns

import com.badlogic.gdx.maps.MapObject
import com.mega.game.engine.common.shapes.IGameShape2D
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.utils.getShape

object SpawnerShapeFactory {

    fun getSpawnShape(entityType: EntityType, mapObject: MapObject): IGameShape2D {
        /*
        if (entityType == EntityType.DECORATION && mapObject.name == Lantern.TAG) {
            val bounds = GameCircle()
                .setRadius(Lantern.RADIUS * ConstVals.PPM.toFloat())
                .setCenter(mapObject.getShape().getCenter())
            return bounds
        }
         */
        return mapObject.getShape()
    }
}
