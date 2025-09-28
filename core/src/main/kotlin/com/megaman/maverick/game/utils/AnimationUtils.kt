package com.megaman.maverick.game.utils

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.IAnimation
import com.megaman.maverick.game.animations.AnimationDef

object AnimationUtils {

    fun loadRegions(
        tag: String,
        atlas: TextureAtlas,
        keys: Iterable<String>,
        regions: ObjectMap<String, TextureRegion>
    ) = keys.forEach { key ->
        val region = atlas.findRegion("$tag/$key") ?: throw IllegalStateException("Region is null: $key")
        regions.put(key, region)
    }

    fun loadAnimationDefs(
        animDefs: ObjectMap<String, AnimationDef>,
        animations: ObjectMap<String, IAnimation>,
        regions: ObjectMap<String, TextureRegion>
    ) = animDefs.forEach { entry ->
        val key = entry.key
        val region = regions[key] ?: throw IllegalStateException("Region is null: $key")
        val (rows, columns, durations, loop, reverse) = entry.value
        animations.put(key, Animation(region, rows, columns, durations, loop, reverse))
    }

    fun loadAnimationDef(
        animDef: AnimationDef,
        animations: ObjectMap<String, IAnimation>,
        regions: ObjectMap<String, TextureRegion>,
        keys: Iterable<String>
    ) = keys.forEach { key ->
        val region = regions[key] ?: throw IllegalStateException("Region is null: $key")
        val (rows, columns, durations, loop) = animDef
        animations.put(key, Animation(region, rows, columns, durations, loop))
    }
}
