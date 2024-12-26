package com.mega.game.engine.screens

import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.g2d.Batch
import com.mega.game.engine.common.interfaces.IPropertizable
import com.mega.game.engine.common.interfaces.Resettable
import com.mega.game.engine.drawables.IDrawable

/**
 * Interface for the base screen. This implementation extends LibGDX's [Screen] interface.
 * It also extends the following:
 * - [IDrawable]: Ideally, drawables should be rendered separate from the [Screen.render] method.
 * - [IPropertizable]: To contain optional properties of the screen.
 * - [Resettable]: To reset the state of the screen.
 */
interface IScreen : Screen, IDrawable<Batch>, IPropertizable, Resettable
