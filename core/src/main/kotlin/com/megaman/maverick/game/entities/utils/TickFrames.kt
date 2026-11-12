package com.megaman.maverick.game.entities.utils

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.animations.IAnimation
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.drawables.sprites.GameSprite

// TODO: TickFrames times the frames in 60 Hz ticks; change it to the engine Animation with durations in seconds
//  (item 140)
class TickFrameDef(
    val width: Int,
    val height: Int,
    val originX: Int,
    val originY: Int,
    val count: Int,
    val order: IntArray,
    val ticks: IntArray,
    val loopTo: Int?
)

object TickFrames {

    const val FPS = 60f
    const val SCALE = 2f
    const val LOOP_SUFFIX = "_loop"

    fun seconds(frames: Int) = frames / FPS

    fun animations(
        atlas: TextureAtlas,
        tag: String,
        defs: OrderedMap<String, TickFrameDef>,
        out: ObjectMap<String, IAnimation> = ObjectMap()
    ): ObjectMap<String, IAnimation> {
        defs.forEach { entry -> animation(atlas, "$tag/${entry.key}", entry.key, entry.value, out) }
        return out
    }

    fun animation(
        atlas: TextureAtlas,
        regionKey: String,
        key: String,
        def: TickFrameDef,
        out: ObjectMap<String, IAnimation>
    ) {
        val region = atlas.findRegion(regionKey) ?: throw IllegalStateException("Missing region $regionKey")
        val frames = (0 until def.count).map { TextureRegion(region, 0, it * def.height, def.width, def.height) }
        val entries = def.order.indices.map { frames[def.order[it]] pairTo seconds(def.ticks[it]) }
        val loopTo = def.loopTo
        when {
            loopTo == null -> out.put(key, Animation.of(entries, false))
            loopTo == 0 -> out.put(key, Animation.of(entries, true))
            else -> {
                out.put(key, Animation.of(entries.subList(0, loopTo), false))
                out.put(key + LOOP_SUFFIX, Animation.of(entries.subList(loopTo, entries.size), true))
            }
        }
    }

    fun hasLoop(defs: OrderedMap<String, TickFrameDef>, key: String) = (defs[key]?.loopTo ?: 0) > 0

    fun baseKey(key: String) = key.removeSuffix(LOOP_SUFFIX)

    fun artFrame(def: TickFrameDef, key: String, index: Int) =
        def.order[(if (key.endsWith(LOOP_SUFFIX)) def.loopTo!! else 0) + index]

    fun maxBoxes(table: OrderedMap<String, kotlin.Array<IntArray>>) =
        table.values().maxOf { frames -> frames.maxOf { it.size / 4 } }

    fun placeBox(
        out: GameRectangle,
        def: TickFrameDef,
        boxes: IntArray,
        start: Int,
        originX: Float,
        originY: Float,
        facingRight: Boolean
    ): GameRectangle {
        val x = boxes[start]
        val y = boxes[start + 1]
        val width = boxes[start + 2]
        val height = boxes[start + 3]
        val left = if (facingRight) originX + (def.originX - x - width) * SCALE else originX + (x - def.originX) * SCALE
        val bottom = originY + (def.originY - y - height) * SCALE
        return out.set(left, bottom, width * SCALE, height * SCALE)
    }

    fun place(sprite: GameSprite, def: TickFrameDef, originX: Float, originY: Float, facingRight: Boolean) {
        val width = def.width * SCALE
        val height = def.height * SCALE
        sprite.setSize(width, height)
        val left = if (facingRight) originX - (def.width - def.originX) * SCALE else originX - def.originX * SCALE
        val bottom = originY - (def.height - def.originY) * SCALE
        sprite.setPosition(left, bottom)
        sprite.setFlip(facingRight, false)
    }
}
