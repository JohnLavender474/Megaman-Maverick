package com.mega.game.engine.screens

import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.g2d.Batch
import com.mega.game.engine.common.interfaces.IPropertizable
import com.mega.game.engine.common.interfaces.Resettable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.drawables.IDrawable

interface IScreen : Screen, IDrawable<Batch>, IPropertizable, Resettable {

    fun show(props: Properties) = show()
}
