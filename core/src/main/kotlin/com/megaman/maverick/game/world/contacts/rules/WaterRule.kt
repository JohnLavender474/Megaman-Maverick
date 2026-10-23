package com.megaman.maverick.game.world.contacts.rules

import com.maverick.game.engine.common.enums.ProcessState
import com.maverick.game.engine.world.body.IFixture
import com.maverick.game.engine.world.contacts.IContactRule
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.contracts.IWater
import com.megaman.maverick.game.entities.decorations.Splash
import com.megaman.maverick.game.world.body.*

object WaterRule : IContactRule {

    override fun onBegin(a: IFixture, b: IFixture, delta: Float) = begin(a, b)

    override fun onContinue(a: IFixture, b: IFixture, delta: Float) = continueContact(a, b)

    override fun onEnd(a: IFixture, b: IFixture, delta: Float) = end(a, b)

    private fun begin(listenerFixture: IFixture, waterFixture: IFixture) {
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

        listenerFixture.receiver(Receivers.WATER_CONTACT)?.invoke(ProcessState.BEGIN, wasInWater)
    }

    private fun continueContact(listenerFixture: IFixture, waterFixture: IFixture) {
        val water = waterFixture.getEntity() as IWater

        val body = listenerFixture.getBody()
        body.setBodySense(BodySense.IN_WATER, true)
        body.addContactWater(water)

        listenerFixture.receiver(Receivers.WATER_CONTACT)?.invoke(ProcessState.CONTINUE, false)
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

            listenerFixture.receiver(Receivers.WATER_CONTACT)?.invoke(ProcessState.END, false)
        }
    }
}
