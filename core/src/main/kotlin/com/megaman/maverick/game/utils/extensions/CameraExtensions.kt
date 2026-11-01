package com.megaman.maverick.game.utils.extensions

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.math.collision.BoundingBox
import com.maverick.game.engine.camera.CameraController
import com.maverick.game.engine.camera.CameraFollow
import com.maverick.game.engine.camera.ICameraView
import com.maverick.game.engine.common.extensions.overlaps
import com.maverick.game.engine.common.interfaces.IRectangle
import com.maverick.game.engine.common.shapes.GameRectangle
import com.megaman.maverick.game.utils.GameObjectPools

fun Camera.overlaps(bounds: IRectangle) = overlaps(bounds, GameObjectPools.fetch(BoundingBox::class))

fun ICameraView.overlaps(bounds: IRectangle) = overlaps(bounds, GameObjectPools.fetch(BoundingBox::class))

fun ICameraView.getRotatedBounds() = getRotatedBounds(GameObjectPools.fetch(GameRectangle::class))

fun ICameraView.toGameRectangle() = toGameRectangle(GameObjectPools.fetch(GameRectangle::class))

fun CameraController.startSmoothFollow() {
    getPart(CameraFollow::class)?.startSmoothFollow()
}

fun CameraController.stopSmoothFollow() {
    getPart(CameraFollow::class)?.stopSmoothFollow()
}
