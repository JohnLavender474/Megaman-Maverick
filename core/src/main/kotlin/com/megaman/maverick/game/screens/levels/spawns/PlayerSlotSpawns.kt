package com.megaman.maverick.game.screens.levels.spawns

import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.players.PlayerSlot
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals

fun slotSpawnProps(slot: PlayerSlot, spawnProps: Properties): Properties {
    val props = Properties()
    props.putAll(spawnProps)

    val target = spawnProps.get("${ConstKeys.PLAYER}_${slot.index + 1}") as RectangleMapObject?
    val bounds = when {
        target != null -> GameRectangle(target.rectangle)
        else -> GameRectangle(spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!).also {
            val name = spawnProps.getOrDefault(ConstKeys.DIRECTION, ConstKeys.UP, String::class)
            val offset = slot.index * ConstVals.PPM.toFloat()
            when (Direction.valueOf(name.uppercase()).getRotatedClockwise()) {
                Direction.RIGHT -> it.translate(offset, 0f)
                Direction.LEFT -> it.translate(-offset, 0f)
                Direction.UP -> it.translate(0f, offset)
                Direction.DOWN -> it.translate(0f, -offset)
            }
        }
    }
    props.put(ConstKeys.BOUNDS, bounds)
    return props
}
