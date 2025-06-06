package com.megaman.maverick.game.entities.mocks

import com.badlogic.gdx.graphics.g2d.Batch
import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.drawables.IDrawable

interface MockRobotMaster : Initializable, Updatable, IDrawable<Batch>
