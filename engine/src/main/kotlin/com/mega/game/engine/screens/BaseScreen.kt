package com.mega.game.engine.screens

import com.badlogic.gdx.graphics.g2d.Batch
import com.mega.game.engine.common.objects.Properties

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
