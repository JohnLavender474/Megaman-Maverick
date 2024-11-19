package com.megaman.maverick.game.entities.megaman.components


import com.mega.game.engine.points.PointsComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues

internal fun Megaman.definePointsComponent(): PointsComponent {
    val pointsComponent = PointsComponent()
    pointsComponent.putPoints(
        ConstKeys.HEALTH,
        max = MegamanValues.START_HEALTH,
        current = MegamanValues.START_HEALTH,
        min = ConstVals.MIN_HEALTH
    )
    pointsComponent.putListener(ConstKeys.HEALTH) {
        if (it.current <= ConstVals.MIN_HEALTH) destroy()
    }
    return pointsComponent
}
