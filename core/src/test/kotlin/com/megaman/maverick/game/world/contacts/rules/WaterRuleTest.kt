package com.megaman.maverick.game.world.contacts.rules

import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.contracts.IWater
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.Receivers
import com.megaman.maverick.game.world.body.setReceiver
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class WaterRuleTest : DescribeSpec({

    lateinit var events: MutableList<String>
    lateinit var listenerFixture: Fixture

    fun waterFixture(): Fixture {
        val water = mockk<MegaGameEntity>(relaxed = true, moreInterfaces = arrayOf(IWater::class, IBodyEntity::class))
        val waterBody = Body(BodyType.ABSTRACT)
        every { (water as IBodyEntity).body } returns waterBody
        every { (water as IWater).shouldSplash(any()) } answers {
            events.add("shouldSplash")
            false
        }
        val fixture = Fixture(waterBody, FixtureType.WATER)
        fixture.putProperty(ConstKeys.ENTITY, water)
        return fixture
    }

    beforeEach {
        events = mutableListOf()

        val listenerBody = Body(BodyType.DYNAMIC)
        val listener = mockk<MegaGameEntity>(relaxed = true, moreInterfaces = arrayOf(IBodyEntity::class))
        every { (listener as IBodyEntity).body } returns listenerBody
        listenerFixture = Fixture(listenerBody, FixtureType.WATER_LISTENER)
        listenerFixture.putProperty(ConstKeys.ENTITY, listener)
        listenerFixture.setReceiver(Receivers.WATER_CONTACT) { state, wasInWater ->
            events.add("contact $state wasInWater=$wasInWater")
        }
    }

    describe("WaterRule") {
        it("calls the water contact after the splash decision, with wasInWater") {
            val water1 = waterFixture()
            val water2 = waterFixture()

            WaterRule.onBegin(listenerFixture, water1, 0f)
            WaterRule.onBegin(listenerFixture, water2, 0f)

            events shouldBe listOf(
                "shouldSplash",
                "contact BEGIN wasInWater=false",
                "shouldSplash",
                "contact BEGIN wasInWater=true"
            )
        }

        it("calls the water contact in continue") {
            WaterRule.onContinue(listenerFixture, waterFixture(), 0f)

            events shouldBe listOf("contact CONTINUE wasInWater=false")
        }

        it("calls the water contact on end only when no water is left, after the splash decision") {
            val water1 = waterFixture()
            val water2 = waterFixture()
            WaterRule.onBegin(listenerFixture, water1, 0f)
            WaterRule.onBegin(listenerFixture, water2, 0f)
            events.clear()

            WaterRule.onEnd(listenerFixture, water1, 0f)
            events shouldBe emptyList()

            WaterRule.onEnd(listenerFixture, water2, 0f)
            events shouldBe listOf("shouldSplash", "contact END wasInWater=false")
        }
    }
})
