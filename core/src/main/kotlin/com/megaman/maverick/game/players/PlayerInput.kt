package com.megaman.maverick.game.players

import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.components.require
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.entities.input
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.entities.state.RunComponent

fun Facing.runButton() = if (this == Facing.LEFT) MegaControllerButton.LEFT else MegaControllerButton.RIGHT

fun IGameEntity.isRunSidePressed(side: Facing) = input.isPressed(side.runButton()) &&
    (!input.isPressed(side.opposite().runButton()) || require(RunComponent::class).lastPressedSide == side)
