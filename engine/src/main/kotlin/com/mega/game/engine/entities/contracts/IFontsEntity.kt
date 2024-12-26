package com.mega.game.engine.entities.contracts

import com.mega.game.engine.common.interfaces.UpdateFunction
import com.mega.game.engine.drawables.fonts.BitmapFontHandle
import com.mega.game.engine.drawables.fonts.FontsComponent
import com.mega.game.engine.entities.IGameEntity

interface IFontsEntity : IGameEntity {

    val fontsComponent: FontsComponent
        get() {
            val key = FontsComponent::class
            return getComponent(key)!!
        }

    fun getFont(key: Any): BitmapFontHandle {
        return this.fontsComponent.fonts.get(key)
    }

    fun addFont(key: Any, font: BitmapFontHandle): BitmapFontHandle {
        return this.fontsComponent.fonts.put(key, font)
    }

    fun removeFont(key: Any): BitmapFontHandle {
        return this.fontsComponent.fonts.remove(key)
    }

    fun putFontUpdateFunction(key: Any, function: UpdateFunction<BitmapFontHandle>) {
        this.fontsComponent.putUpdateFunction(key, function)
    }

    fun removeFontUpdateFunction(key: Any) {
        this.fontsComponent.removeUpdateFunction(key)
    }
}

