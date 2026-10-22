package com.maverick.game.engine.common.extensions

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.maverick.game.engine.common.interfaces.IRectangle

fun ShapeRenderer.rect(bounds: IRectangle) = rect(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight())
