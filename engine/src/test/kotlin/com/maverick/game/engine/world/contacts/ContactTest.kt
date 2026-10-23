package com.maverick.game.engine.world.contacts

import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.maverick.game.engine.world.body.TestFixtureType
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class ContactDescribeSpec :
    DescribeSpec({
        describe("Contact data class") {
            val body = Body(BodyType.ABSTRACT)
            val fixture1 = Fixture(body, TestFixtureType.TYPE1, GameRectangle())
            val fixture2 = Fixture(body, TestFixtureType.TYPE2, GameRectangle())
            val contact = Contact(fixture1, fixture2)

            it("should have the correct initial properties") {
                contact.fixture1 shouldBe fixture1
                contact.fixture2 shouldBe fixture2
            }

            it("should check equality correctly") {
                val sameContact = Contact(fixture1, fixture2)
                val swappedContact = Contact(fixture2, fixture1)
                val differentFixture = Fixture(body, TestFixtureType.TYPE3, GameRectangle())
                val differentContact = Contact(fixture1, differentFixture)

                (contact == sameContact) shouldBe true
                (contact == swappedContact) shouldBe true
                (contact == differentContact) shouldBe false
            }

            it("should have the correct hash code") {
                val expectedHashCode = 49 + 7 * fixture1.hashCode() + 7 * fixture2.hashCode()
                contact.hashCode() shouldBe expectedHashCode
            }
        }
    })
