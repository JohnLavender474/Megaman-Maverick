package com.megaman.maverick.game.world.contacts.rules

import com.maverick.game.engine.world.body.IFixture
import com.maverick.game.engine.world.contacts.IContactRule
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.contracts.ICartRider
import com.megaman.maverick.game.entities.contracts.IOwnable
import com.megaman.maverick.game.entities.special.Cart
import com.megaman.maverick.game.world.body.*

object FeetCartRule : IContactRule {

    override fun onBegin(a: IFixture, b: IFixture, delta: Float) = begin(a, b)

    override fun onEnd(a: IFixture, b: IFixture, delta: Float) = end(a, b)

    private fun begin(feetFixture: IFixture, cartFixture: IFixture) {
        val feetEntity = feetFixture.getEntity()
        val cartEntity = cartFixture.getEntity() as IOwnable<*>

        if (feetEntity is ICartRider && cartEntity is Cart) {
            cartEntity.owner = feetEntity
            feetEntity.body.setBodySense(BodySense.TOUCHING_CART, true)
            feetEntity.body.putProperty(ConstKeys.CART, cartEntity)
        }
    }

    private fun end(feetFixture: IFixture, cartFixture: IFixture) {
        val feetEntity = feetFixture.getEntity()
        val cartEntity = cartFixture.getEntity() as IOwnable<*>
        cartEntity.owner = null

        if (feetEntity is ICartRider && cartEntity is Cart) {
            feetEntity.body.setBodySense(BodySense.TOUCHING_CART, false)
            feetEntity.body.removeProperty(ConstKeys.CART)
        }
    }
}
