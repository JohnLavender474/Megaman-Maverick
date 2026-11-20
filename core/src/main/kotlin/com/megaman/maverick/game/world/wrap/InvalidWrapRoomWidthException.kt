package com.megaman.maverick.game.world.wrap

import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals

class InvalidWrapRoomWidthException(val room: RectangleMapObject) : RuntimeException(
    "Room '${room.name}' has ${ConstKeys.WRAP}=true, but it is ${room.rectangle.width / ConstVals.PPM} tiles wide. " +
        "A wrap room must be exactly ${ConstVals.VIEW_WIDTH} tiles (${ConstVals.VIEW_WIDTH * ConstVals.PPM} px) wide; " +
        "in Tiled, resize the ${ConstKeys.GAME_ROOMS} object or remove its ${ConstKeys.WRAP} property."
)
