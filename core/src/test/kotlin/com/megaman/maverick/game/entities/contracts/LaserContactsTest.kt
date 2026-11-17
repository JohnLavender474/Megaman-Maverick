package com.megaman.maverick.game.entities.contracts

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.world.body.IFixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.mockk

class LaserContactsTest : DescribeSpec({

    describe("LaserContacts") {
        it("gives the contacts nearest to the origin first, with copies of the points") {
            val origin = Vector2(0f, 0f)
            val contacts = LaserContacts { origin }
            val fixture = mockk<IFixture>()
            val far = Vector2(5f, 0f)

            contacts.add(far, fixture)
            contacts.add(Vector2(2f, 0f), null)
            far.set(1f, 0f)

            contacts.peek().first shouldBe Vector2(2f, 0f)
            val first = contacts.poll()
            first.second shouldBe null
            val second = contacts.poll()
            second.first shouldBe Vector2(5f, 0f)
            second.second shouldBeSameInstanceAs fixture
            contacts.isEmpty shouldBe true
        }

        it("reuses its pairs after clear") {
            val contacts = LaserContacts { Vector2.Zero }
            contacts.add(Vector2(1f, 0f), null)
            contacts.add(Vector2(3f, 0f), null)
            val near = contacts.peek()

            contacts.clear()
            contacts.isEmpty shouldBe true
            contacts.add(Vector2(4f, 0f), null)

            contacts.peek() shouldBeSameInstanceAs near
            near.first shouldBe Vector2(4f, 0f)
        }
    }
})
