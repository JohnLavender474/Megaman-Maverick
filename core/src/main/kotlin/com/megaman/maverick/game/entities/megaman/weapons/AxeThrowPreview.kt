package com.megaman.maverick.game.entities.megaman.weapons

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.components.holdingAxe
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.projectiles.AxeTrajectory
import com.megaman.maverick.game.entities.state.inPlay

internal class AxeThrowPreview {

    val points = Array<Vector2>()
    var count = 0
        private set

    private val position = Vector2()
    private val impulse = Vector2()
    private val gravity = Vector2()

    init {
        repeat(MegamanValues.AXE_PREVIEW_MAX_DOTS) { points.add(Vector2()) }
    }

    fun update(megaman: Megaman) {
        if (!shows(megaman)) {
            count = 0
            return
        }

        AxeThrow.launch(megaman, megaman.axeWindup.time, position, impulse)
        AxeTrajectory.gravity(megaman.direction, megaman.gravityScalar, gravity)
        count = AxeTrajectory.dots(
            start = position,
            velocity = impulse,
            gravity = gravity,
            up = megaman.direction,
            fixedStep = megaman.services.settings.performance.fixedStep,
            spacing = MegamanValues.AXE_PREVIEW_DOT_SPACING * ConstVals.PPM,
            maxDots = MegamanValues.AXE_PREVIEW_MAX_DOTS,
            maxTime = MegamanValues.AXE_PREVIEW_MAX_TIME,
            out = points
        )
    }

    private fun shows(megaman: Megaman) = megaman.holdingAxe &&
        megaman.axeWindup.time >= MegamanValues.AXE_PREVIEW_DELAY &&
        megaman.inPlay &&
        !megaman.services.levelState.roomTransition &&
        !megaman.services.gameCamera.rotating &&
        !megaman.damaged
}
