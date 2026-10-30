package com.megaman.maverick.game.screens.levels.camera

class GameCameraState {

    var rotating = false
    // This is set to true when the camera focus on Megaman has (potentially) been snapped away suddenly, and so
    // the camera should be interpolated to Megaman's current position to prevent a jarring camera experience. This
    // is reset back to false once the camera reaches Megaman's true position again.
    var focusSnappedAway = false
}
