package com.mega.game.engine.world.contacts

import com.mega.game.engine.common.objects.GamePair
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.world.WorldSystem
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.mega.game.engine.world.body.IFixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class ContactDescribeSpec :
    DescribeSpec({
        describe("Contact data class") {
            val body = Body(BodyType.ABSTRACT)
            val fixture1 = Fixture(body, "type1", GameRectangle())
            val fixture2 = Fixture(body, "type2", GameRectangle())
            val contact = Contact(fixture1, fixture2)

            val out = GamePair<IFixture, IFixture>(WorldSystem.DummyFixture(), WorldSystem.DummyFixture())

            it("should have the correct initial properties") {
                contact.fixture1 shouldBe fixture1
                contact.fixture2 shouldBe fixture2
            }

            it("should get fixtures in order correctly") {
                val fixtureType1 = "type1"
                val fixtureType2 = "type2"

                var fixturesInOrder = contact.getFixturesInOrder(fixtureType1, fixtureType2, out)
                fixturesInOrder shouldBe GamePair(fixture1, fixture2)

                fixturesInOrder = contact.getFixturesInOrder(fixtureType2, fixtureType1, out)
                fixturesInOrder shouldBe GamePair(fixture2, fixture1)
            }

            it("should check if fixtures are of types correctly") {
                val fixtureType1 = "type1"
                val fixtureType2 = "type2"
                val badFixtureType = "type3"

                val test: (String, String, Boolean) -> Unit = { f1, f2, expected ->
                    contact.fixturesMatch(f1, f2) shouldBe expected
                }

                test(fixtureType1, fixtureType2, true)
                test(fixtureType2, fixtureType1, true)
                test(fixtureType1, fixtureType1, false)
                test(fixtureType2, fixtureType2, false)
                test(fixtureType1, badFixtureType, false)
                test(fixtureType2, badFixtureType, false)
                test(badFixtureType, badFixtureType, false)
            }

            it("should check equality correctly") {
                val sameContact = Contact(fixture1, fixture2)
                val swappedContact = Contact(fixture2, fixture1)
                val differentFixture = Fixture(body, "type3", GameRectangle())
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
