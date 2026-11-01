package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.common.enums.ProcessState
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.world.body.Fixture
import com.maverick.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.players.nonPlayer
import com.megaman.maverick.game.players.registeredPlayer
import com.megaman.maverick.game.testServices
import com.megaman.maverick.game.utils.VelocityAlteration
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.Receivers
import com.megaman.maverick.game.world.body.receiver
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class UnderwaterFanTest : DescribeSpec({

    fun blowFixture(): IFixture {
        val services = testServices()
        val atlas = mockk<TextureAtlas>()
        every { atlas.findRegion(any()) } returns AtlasRegion(mockk<Texture>(relaxed = true), 0, 0, 64, 64)
        every { services.assets.get(any<String>(), TextureAtlas::class.java) } returns atlas

        val fan = UnderwaterFan(services)
        fan.init()
        return fan.body.getFixtures(Array(), FixtureType.FORCE).first()
    }

    fun alteration(entity: MegaGameEntity): VelocityAlteration {
        val fixture = Fixture(entity.body, FixtureType.BODY)
        fixture.putProperty(ConstKeys.ENTITY, entity)
        return blowFixture().receiver(Receivers.VELOCITY_ALTERATION)!!.invoke(fixture, 1f, ProcessState.CONTINUE)
    }

    describe("UnderwaterFan") {
        it("blows a player") {
            alteration(registeredPlayer()).forceY shouldBeGreaterThan 0f
        }

        it("does not blow another body") {
            alteration(nonPlayer()) shouldBe VelocityAlteration.addNone()
        }
    }
})
