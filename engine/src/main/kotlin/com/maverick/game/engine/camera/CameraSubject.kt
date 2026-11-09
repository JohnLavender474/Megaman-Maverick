package com.maverick.game.engine.camera

import com.badlogic.gdx.graphics.Camera

class CameraSubject(
    private val camera: Camera,
    private val follow: CameraFollow,
    private val roomBounds: CameraRoomBounds,
    private val drawOffset: CameraDrawOffset
) {

    fun set(target: IFocusable, room: IRoomSubject, draw: IDrawOffsetSource) {
        follow.setFocus(camera, target)
        roomBounds.subject = room
        drawOffset.source = draw
    }
}
