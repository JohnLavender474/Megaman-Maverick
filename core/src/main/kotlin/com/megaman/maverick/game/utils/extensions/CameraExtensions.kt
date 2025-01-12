package com.megaman.maverick.game.utils.extensions

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.math.collision.BoundingBox
import com.mega.game.engine.common.extensions.overlaps
import com.mega.game.engine.common.interfaces.IRectangle
import com.megaman.maverick.game.utils.GameObjectPools

fun Camera.overlaps(bounds: IRectangle) = overlaps(bounds, GameObjectPools.fetch(BoundingBox::class))
