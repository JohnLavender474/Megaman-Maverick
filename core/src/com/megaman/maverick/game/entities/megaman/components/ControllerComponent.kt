package com.megaman.maverick.game.entities.megaman.components

import com.engine.controller.ControllerComponent
import com.engine.controller.buttons.ButtonActuator
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.world.BodySense
import com.megaman.maverick.game.world.isSensing

internal fun Megaman.defineControllerComponent(): ControllerComponent {
  // left
  val left =
      ButtonActuator(
          onPressContinued = { poller, delta ->
            if (poller.isButtonPressed(ConstKeys.RIGHT)) return@ButtonActuator

            running = true
            val threshold =
                (if (body.isSensing(BodySense.IN_WATER)) MegamanValues.WATER_RUN_SPEED
                else MegamanValues.RUN_SPEED) * ConstVals.PPM

            if (body.physics.velocity.x > -threshold) {
              val impulseX =
                  if (body.isSensing(BodySense.FEET_ON_ICE)) MegamanValues.ICE_RUN_IMPULSE
                  else MegamanValues.RUN_IMPULSE
              body.physics.velocity.x -= impulseX * delta * ConstVals.PPM
            }
          },
          onJustReleased = { poller ->
            if (!poller.isButtonPressed(ConstKeys.RIGHT)) running = false
          })

  // right
  val right =
      ButtonActuator(
          onPressContinued = { poller, delta ->
            if (poller.isButtonPressed(ConstKeys.LEFT)) return@ButtonActuator

            running = true
            val threshold =
                (if (body.isSensing(BodySense.IN_WATER)) MegamanValues.WATER_RUN_SPEED
                else MegamanValues.RUN_SPEED) * ConstVals.PPM

            if (body.physics.velocity.x < threshold) {
              val impulseX =
                  if (body.isSensing(BodySense.FEET_ON_ICE)) MegamanValues.ICE_RUN_IMPULSE
                  else MegamanValues.RUN_IMPULSE
              body.physics.velocity.x += impulseX * delta * ConstVals.PPM
            }
          },
          onJustReleased = { poller ->
            if (!poller.isButtonPressed(ConstKeys.LEFT)) running = false
          })

  // attack
  val attack =
      ButtonActuator(
          onPressContinued = { poller, delta ->
            // TODO: implement this
            /*
            if (isUnderDamage()) {
                stopCharging()
                return@ButtonActuator
            }
            if (!charging && !weaponHandler.canFireWeapon(currWeapon, MegaChargeStatus.HALF_CHARGED)) {
                stopCharging()
                return@ButtonActuator
            }
            if (halfCharging && !weaponHandler.canFireWeapon(currWeapon, MegaChargeStatus.FULLY_CHARGED)) {
                return@ButtonActuator
            }
            chargingTimer.update(delta)
             */
          },
          onJustReleased = {
            // TODO: implement this
            /*
            if (isUnderDamage() || !canFireCurrWeapon() || !shoot())
                requestToPlaySound(SoundAsset.ERROR_SOUND.source, false)

            stopCharging()
               */
          })

  // swap weapon
  val select =
      ButtonActuator(
          onJustPressed = {
            // TODO: implement this
            /*
            var x: Int = currWeapon.ordinal() + 1
            if (x >= MegamanWeapon.values().length) {
                x = 0
            }
            currWeapon = MegamanWeapon.values().get(x)
             */
          })

  return ControllerComponent(
      this,
      ConstKeys.LEFT to left,
      ConstKeys.RIGHT to right,
      ConstKeys.A to attack,
      ConstKeys.SELECT to select)
}