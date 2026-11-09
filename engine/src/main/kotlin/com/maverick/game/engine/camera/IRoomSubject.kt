package com.maverick.game.engine.camera

import com.maverick.game.engine.common.shapes.GameRectangle

interface IRoomSubject {

    fun getSubjectBounds(): GameRectangle
}
