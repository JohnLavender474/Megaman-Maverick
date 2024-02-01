package com.megaman.maverick.game.entities.megaman.components

import com.engine.common.CAUSE_OF_DEATH_MESSAGE
import com.engine.common.objects.props
import com.engine.points.PointsComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues

internal fun Megaman.definePointsComponent(): PointsComponent {
    val pointsComponent = PointsComponent(this)
    pointsComponent.putPoints(ConstKeys.HEALTH, MegamanValues.START_HEALTH)
    pointsComponent.putListener(ConstKeys.HEALTH) {
        if (it.current <= 0) kill(props(CAUSE_OF_DEATH_MESSAGE to "Health depleted"))
    }
    return pointsComponent
}
