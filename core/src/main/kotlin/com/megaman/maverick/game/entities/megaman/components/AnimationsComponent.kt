package com.megaman.maverick.game.entities.megaman.components

import com.badlogic.gdx.utils.OrderedMap
import com.maverick.game.engine.animations.IAnimation
import com.maverick.game.engine.entities.input
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegaChargeStatus
import com.megaman.maverick.game.entities.megaman.weapons.currentLook

internal fun Megaman.onChangeAnimationKey(
    currentKey: String?, nextKey: String?, animations: OrderedMap<String, IAnimation>
) {
    // Special case for Axe Throw weapon: need to ensure that if Megaman is "shooting" (throwing),
    // then if the animation key changes, the new animation starts at the same time stamp as what
    // the current animation is at.
    if (currentLook.keepsAnimTimeAcrossKeys &&
        currentKey != null && nextKey != null &&
        currentKey.contains("axe_throw")
    ) {
        val time = animations[currentKey]?.getCurrentTime()
        time?.let { t -> animations[nextKey]?.setCurrentTime(t) }
    }
}

internal fun Megaman.shouldEqualAnimKeysTriggerChange(key: String?): Boolean {
    // When the player is equipped with the Rodent Claws, then the animation should be
    // reset each time the player presses the SHOOT button EXCEPT when the player is
    // standing. This is because every slash sequence besides the STAND animation uses
    // the same animation repeatedly. Without this check, then Mega Man will appear to
    // be frozen still if the player rapid fires the SHOOT button.
    return key != null && currentLook.restartsSameKeyOnPress &&
        weaponsHandler.canFireWeapon(currentWeapon, MegaChargeStatus.NOT_CHARGED) &&
        input.isPressed(MegaControllerButton.B) &&
        key.contains("slash") && !key.contains("stand")
}

internal fun Megaman.postProcessAnimationKey(currentKey: String?, nextKey: String?): String? {
    if (currentKey?.contains("_shoot") == true && nextKey?.contains("needle_spin") == true) {
        shootAnimTimer.setToEnd()
        return currentKey.replace("_shoot", "")
    }

    return nextKey
}
