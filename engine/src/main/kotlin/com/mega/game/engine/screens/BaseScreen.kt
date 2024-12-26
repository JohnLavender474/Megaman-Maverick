package com.mega.game.engine.screens

import com.badlogic.gdx.graphics.g2d.Batch
import com.mega.game.engine.common.objects.Properties

/**
 * A base implementation of [IScreen] with all methods given no-op default implementations. As stated in the [IScreen]
 * documentation, it is ideal for textures to be drawn in the [draw] method separate from the [render] method, though
 * this is optional given that the [draw] method has a no-op default implementation.
 */
abstract class BaseScreen(override val properties: Properties = Properties()) : IScreen {

    override fun show() {}

    override fun render(delta: Float) {}

    override fun draw(drawer: Batch) {}

    override fun resize(width: Int, height: Int) {}

    override fun pause() {}

    override fun resume() {}

    override fun reset() {}

    override fun hide() {}

    override fun dispose() {}
}
