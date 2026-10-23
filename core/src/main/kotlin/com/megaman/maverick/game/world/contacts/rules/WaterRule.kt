package com.megaman.maverick.game.world.contacts.rules

import com.maverick.game.engine.world.body.IFixture
import com.maverick.game.engine.world.contacts.IContactRule
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.contracts.IWater
import com.megaman.maverick.game.entities.decorations.Splash
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.AButtonTask
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.world.body.*

object WaterRule : IContactRule {

    override fun onBegin(a: IFixture, b: IFixture, delta: Float) = begin(a, b)

    override fun onContinue(a: IFixture, b: IFixture, delta: Float) = continueContact(a, b)

    override fun onEnd(a: IFixture, b: IFixture, delta: Float) = end(a, b)

    private fun begin(listenerFixture: IFixture, waterFixture: IFixture) {
        val entity = listenerFixture.getEntity()

        val body = listenerFixture.getBody()
        val wasInWater = body.hasAnyContactWater()

        val water = waterFixture.getEntity() as IWater
        body.setBodySense(BodySense.IN_WATER, true)
        body.addContactWater(water)

        listenerFixture.receiver(Receivers.HIT_BY_WATER)?.invoke(water)

        val shouldSplash = water.shouldSplash(listenerFixture) &&
            listenerFixture.getOrDefaultProperty(ConstKeys.SPLASH, true, Boolean::class)

        if (!wasInWater && shouldSplash) Splash.splashOnWaterSurface(
            listenerFixture.getBody().getBounds(),
            waterFixture.getBody().getBounds(),
            water.getSplashType(listenerFixture),
            water.doMakeSplashSound(listenerFixture)
        )

        if (entity is Megaman) {
            if (!entity.body.isSensing(BodySense.FEET_ON_GROUND) &&
                !entity.isBehaviorActive(BehaviorType.WALL_SLIDING)
            ) entity.aButtonTask = AButtonTask.SWIM

            entity.gravityScalar = MegamanValues.WATER_GRAVITY_SCALAR

            if (!wasInWater) {
                if (entity.direction.isVertical()) body.physics.velocity.y = 0f
                else body.physics.velocity.x = 0f
            }
        }
    }

    private fun continueContact(listenerFixture: IFixture, waterFixture: IFixture) {
        val water = waterFixture.getEntity() as IWater

        val body = listenerFixture.getBody()
        body.setBodySense(BodySense.IN_WATER, true)
        body.addContactWater(water)

        val entity = listenerFixture.getEntity()
        if (entity is Megaman && !entity.body.isSensing(BodySense.FEET_ON_GROUND) &&
            !entity.isBehaviorActive(BehaviorType.WALL_SLIDING)
        ) entity.aButtonTask = AButtonTask.SWIM
    }

    private fun end(listenerFixture: IFixture, waterFixture: IFixture) {
        val water = waterFixture.getEntity() as IWater

        val body = listenerFixture.getBody()
        body.removeContactWater(water)
        if (!body.hasAnyContactWater()) body.setBodySense(BodySense.IN_WATER, false)

        if (!body.isSensing(BodySense.IN_WATER)) {
            val shouldSplash = water.shouldSplash(listenerFixture) &&
                listenerFixture.getOrDefaultProperty(ConstKeys.SPLASH, true, Boolean::class)

            if (shouldSplash) Splash.splashOnWaterSurface(
                listenerFixture.getBody().getBounds(),
                waterFixture.getBody().getBounds(),
                water.getSplashType(listenerFixture),
                water.doMakeSplashSound(listenerFixture)
            )

            val listener = listenerFixture.getEntity()
            if (listener is Megaman) {
                listener.aButtonTask = AButtonTask.AIR_DASH
                listener.gravityScalar = 1f
            }
        }
    }
}
