package com.mega.game.engine.common.utils

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.math.collision.BoundingBox

object BoundingBoxUtils {

    fun isInCamera(boundingBox: BoundingBox, camera: Camera) = camera.frustum.boundsInFrustum(boundingBox)
}
