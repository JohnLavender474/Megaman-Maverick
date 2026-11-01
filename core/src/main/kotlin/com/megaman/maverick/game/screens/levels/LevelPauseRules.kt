package com.megaman.maverick.game.screens.levels

import com.badlogic.gdx.utils.Queue
import com.maverick.game.engine.controller.buttons.ButtonStatus
import com.maverick.game.engine.input.InputFrame
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.controllers.SelectButtonAction

fun isPauseRequested(
    paused: Boolean,
    alive: Boolean,
    inPlay: Boolean,
    locked: Boolean,
    playerFrame: InputFrame,
    backStatus: ButtonStatus,
    selectAction: SelectButtonAction
) = !paused && alive && inPlay && !locked &&
    (playerFrame.isJustReleased(MegaControllerButton.START) ||
        backStatus == ButtonStatus.JUST_PRESSED ||
        (selectAction == SelectButtonAction.START && playerFrame.isJustPressed(MegaControllerButton.SELECT)))

// TODO: a lock that a listener submits, for example MegaLevelScreen:753, starts 1 frame later, so a real START release
//  at that moment still pauses, and only Esc leaves that pause; follow-up in item 012 (see 031f CONTRACT.md, intended
//  change 6)
fun queuePause(queue: Queue<() -> Unit>, locked: () -> Boolean, pause: () -> Unit) {
    queue.addLast { if (!locked()) pause() }
}
