package com.megaman.maverick.game.world.wrap

import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.world.WorldWrap
import com.maverick.game.engine.world.body.IBody
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.levels.LevelState

fun RectangleMapObject.isWrapRoom(): Boolean = properties.get(ConstKeys.WRAP, false, Boolean::class.java)

// TODO: the region is the new room at once when a transition enters a wrap room, so a player whose center is still
//  past its edge wraps during the pan (item 111)
fun LevelState.wrapRegion(out: GameRectangle): GameRectangle? {
    val room = currentRoom
    if (room == null || !room.isWrapRoom()) return null
    return out.set(room.rectangle)
}

fun seamShiftOf(entity: IGameEntity, body: IBody, bounds: GameRectangle, region: GameRectangle?): Float {
    if (region == null || !WrapPolicy.wraps(entity)) return 0f
    return WorldWrap.seamShift(
        minX = bounds.getX(),
        maxX = bounds.getMaxX(),
        region = region,
        wrapCount = body.wrapCount,
        lastWrapDx = body.lastWrapDx,
        maxWraps = WrapPolicy.maxWraps(entity)
    )
}

fun checkWrapRoomWidths(rooms: Array<RectangleMapObject>) {
    val width = ConstVals.VIEW_WIDTH * ConstVals.PPM
    for (room in rooms) {
        if (room.isWrapRoom() && room.rectangle.width != width) throw InvalidWrapRoomWidthException(room)
    }
}
