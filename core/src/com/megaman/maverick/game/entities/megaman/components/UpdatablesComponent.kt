package com.megaman.maverick.game.entities.megaman.components

import com.engine.drawables.sprites.GameSprite
import com.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.extensions.stopCharging

internal fun Megaman.defineUpdatablesComponent() =
    UpdatablesComponent(
        this,
        { delta ->
          // update weapons
          if (!weaponHandler.isChargeable(currentWeapon)) stopCharging()
          weaponHandler.update(delta)

          // if under damage, reset the charge timer and update the damage flash timer
          if (isUnderDamage()) {
            chargingTimer.reset()
            damageFlashTimer.update(delta)
            if (damageFlashTimer.isFinished()) {
              damageFlashTimer.reset()
              damageFlash = !damageFlash
            }
          } else {
            damageFlashTimer.reset()
            damageFlash = false
          }

          // flash Megaman if the damage flasher is on
          if (damageFlash) (firstSprite as GameSprite).setAlpha(.5f)
          else (firstSprite as GameSprite).setAlpha(1f)

          // update the timers
          shootAnimTimer.update(delta)
          wallJumpTimer.update(delta)
        })
