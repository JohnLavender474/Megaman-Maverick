package com.maverick.game.engine.drawables.sprites

import com.badlogic.gdx.graphics.g2d.TextureAtlas

fun TextureAtlas.containsRegion(regionName: String): Boolean {
    return findRegion(regionName) != null
}