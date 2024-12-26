package com.mega.game.engine.drawables.fonts

import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.interfaces.UpdateFunction
import com.mega.game.engine.common.objects.GamePair
import com.mega.game.engine.components.IGameComponent


class FontsComponent(val fonts: OrderedMap<Any, BitmapFontHandle> = OrderedMap()) : IGameComponent, Updatable {

    internal val updatables = objectMapOf<Any, UpdateFunction<BitmapFontHandle>>()

    constructor(vararg fonts: GamePair<Any, BitmapFontHandle>) : this(OrderedMap<Any, BitmapFontHandle>().apply {
        fonts.forEach {
            put(
                it.first,
                it.second
            )
        }
    })

    fun putUpdateFunction(key: Any, function: UpdateFunction<BitmapFontHandle>) {
        updatables.put(key, function)
    }

    fun removeUpdateFunction(key: Any) {
        updatables.remove(key)
    }

    override fun update(delta: Float) {
        updatables.forEach { e ->
            val key = e.key
            val function = e.value
            fonts[key]?.let { function.update(delta, it) }
        }
    }

    fun add(key: Any, font: BitmapFontHandle) {
        fonts.put(key, font)
    }

    fun remove(key: Any) {
        fonts.remove(key)
    }

    fun get(key: Any): BitmapFontHandle? {
        return fonts[key]
    }
}