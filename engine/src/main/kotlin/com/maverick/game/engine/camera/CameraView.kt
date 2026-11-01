package com.maverick.game.engine.camera

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.collision.BoundingBox
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.interfaces.IRectangle
import com.maverick.game.engine.common.shapes.GameRectangle

class CameraView(private val camera: Camera, private val rotation: CameraRotation? = null) : ICameraView {

    override val direction: Direction
        get() = rotation?.direction ?: Direction.UP

    override val rotating: Boolean
        get() = rotation?.rotating == true

    val viewWidth: Float
        get() = if (direction.isVertical()) camera.viewportWidth else camera.viewportHeight

    val viewHeight: Float
        get() = if (direction.isVertical()) camera.viewportHeight else camera.viewportWidth

    val blendedViewWidth: Float
        get() = MathUtils.lerp(camera.viewportWidth, camera.viewportHeight, rotation?.sideways ?: 0f)

    val blendedViewHeight: Float
        get() = MathUtils.lerp(camera.viewportHeight, camera.viewportWidth, rotation?.sideways ?: 0f)

    override fun getRotatedBounds(out: GameRectangle): GameRectangle {
        toGameRectangle(out)
        out.rotate(direction.rotation, camera.position.x, camera.position.y)
        return out
    }

    override fun toGameRectangle(out: GameRectangle): GameRectangle {
        out.setSize(camera.viewportWidth, camera.viewportHeight)
        out.setCenter(camera.position.x, camera.position.y)
        return out
    }

    override fun overlaps(bounds: IRectangle, out: BoundingBox) = camera.frustum.boundsInFrustum(bounds.toBoundingBox(out))
}
