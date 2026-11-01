package com.megaman.maverick.game.entities.megaman.components

import com.maverick.game.engine.common.enums.Direction
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.state.FocusComponent
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.world.body.getBounds

internal fun Megaman.defineFocusComponent() = FocusComponent(
    bounds = { body.getBounds() },
    position = {
        val position = DirectionPositionMapper.getInvertedPosition(direction)
        val focus = body.getBounds().getPositionPoint(position)
        when (direction) {
            Direction.UP -> focus.y += MEGAMAN_BODY_HEIGHT * ConstVals.PPM / 2f
            Direction.DOWN -> focus.y -= MEGAMAN_BODY_HEIGHT * ConstVals.PPM / 2f
            Direction.LEFT -> focus.x -= MEGAMAN_BODY_HEIGHT * ConstVals.PPM / 2f
            Direction.RIGHT -> focus.x += MEGAMAN_BODY_HEIGHT * ConstVals.PPM / 2f
        }
        focus
    }
)
