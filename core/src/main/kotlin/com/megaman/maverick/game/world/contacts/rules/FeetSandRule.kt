package com.megaman.maverick.game.world.contacts.rules

import com.maverick.game.engine.common.enums.Position
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.world.body.IFixture
import com.maverick.game.engine.world.contacts.IContactRule
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.decorations.Splash.SplashType
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.DecorationsFactory
import com.megaman.maverick.game.utils.extensions.getBoundingRectangle
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*
import com.megaman.maverick.game.world.contacts.ContactValues.SAND_FRICTION

class FeetSandRule(private val splashesOn: () -> Boolean) : IContactRule {

    override fun onBegin(a: IFixture, b: IFixture, delta: Float) = begin(a)

    override fun onContinue(a: IFixture, b: IFixture, delta: Float) = continueContact(a)

    override fun onEnd(a: IFixture, b: IFixture, delta: Float) = end(a)

    private fun begin(feetFixture: IFixture) {
        val body = feetFixture.getBody()
        body.setBodySense(BodySense.FEET_ON_SAND, true)

        val takeFriction =
            body.getOrDefaultProperty("${ConstKeys.TAKE_FRICTION}_${ConstKeys.SAND}", true, Boolean::class)
        if (takeFriction)
            body.physics.frictionOnSelf.set(SAND_FRICTION, SAND_FRICTION).scl(ConstVals.PPM.toFloat())

        if (splashesOn.invoke()) {
            val splash = EntityFactories.fetch(EntityType.DECORATION, DecorationsFactory.SPLASH)!!
            val position = feetFixture.getShape().getBoundingRectangle().getPositionPoint(Position.BOTTOM_CENTER)
            splash.spawn(
                props(
                    ConstKeys.POSITION pairTo position,
                    ConstKeys.TYPE pairTo SplashType.SAND,
                    ConstKeys.SOUND pairTo true
                )
            )
        }
    }

    private fun continueContact(feetFixture: IFixture) {
        val body = feetFixture.getBody()
        body.setBodySense(BodySense.FEET_ON_SAND, true)

        val takeFriction =
            body.getOrDefaultProperty("${ConstKeys.TAKE_FRICTION}_${ConstKeys.SAND}", true, Boolean::class)
        if (takeFriction)
            body.physics.frictionOnSelf.set(SAND_FRICTION, SAND_FRICTION).scl(ConstVals.PPM.toFloat())
    }

    private fun end(feetFixture: IFixture) {
        val body = feetFixture.getBody()
        body.setBodySense(BodySense.FEET_ON_SAND, false)
    }
}
