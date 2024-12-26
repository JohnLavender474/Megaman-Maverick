package com.mega.game.engine.drawables.fonts

import com.mega.game.engine.common.objects.ImmutableCollection
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.systems.GameSystem
import java.util.function.Consumer


class FontsSystem(protected val fontsCollector: (BitmapFontHandle) -> Unit) :
    GameSystem(FontsComponent::class) {


    constructor(fontsCollector: Consumer<BitmapFontHandle>) : this(fontsCollector::accept)


    override fun process(on: Boolean, entities: ImmutableCollection<IGameEntity>, delta: Float) {
        if (!on) return

        entities.forEach { entity ->
            val fontsComponent = entity.getComponent(FontsComponent::class)
            fontsComponent?.update(delta)
            fontsComponent?.fonts?.values()?.forEach { font -> fontsCollector.invoke(font) }
        }
    }

}