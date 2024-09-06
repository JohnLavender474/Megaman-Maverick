package com.megaman.maverick.game.drawables.fonts

import com.mega.game.engine.world.body.*;
import com.mega.game.engine.world.collisions.*;
import com.mega.game.engine.world.contacts.*;
import com.mega.game.engine.world.pathfinding.*;

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.drawables.fonts.BitmapFontHandle
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sorting.IComparableDrawable
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.utils.MegaUtilMethods.getDefaultFontSize

class MegaFontHandle(
    textSupplier: () -> String,
    fontSize: Int = getDefaultFontSize(),
    positionX: Float = (ConstVals.VIEW_WIDTH - 3) * ConstVals.PPM,
    positionY: Float = (ConstVals.VIEW_HEIGHT - 1) * ConstVals.PPM,
    centerX: Boolean = true,
    centerY: Boolean = true,
    hidden: Boolean = false,
    fontSource: String = ConstVals.MEGAMAN_MAVERICK_FONT,
    override val priority: DrawingPriority = DrawingPriority(DrawingSection.FOREGROUND, 10)
) : IComparableDrawable<Batch> {

    val font = BitmapFontHandle(
        textSupplier,
        fontSize,
        Vector2(positionX, positionY),
        centerX,
        centerY,
        hidden,
        fontSource,
        priority
    )

    override fun draw(drawer: Batch) = font.draw(drawer)
}