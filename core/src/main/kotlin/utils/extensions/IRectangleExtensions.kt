package com.megaman.maverick.game.utils.extensions

import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.interfaces.IRectangle
import com.megaman.maverick.game.utils.ObjectPools

class IRectangleExtensions {

    fun IRectangle.getPosition() = getPosition(ObjectPools.get(Vector2::class))

    fun IRectangle.getCenter() = getCenter(ObjectPools.get(Vector2::class))
}
