package com.mega.game.engine.cullables

import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.events.Event
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class CullableOnEventTest :
    DescribeSpec({
        describe("CullableOnEvent") {
            it("should be culled on specified event") {
                val eventKey = "testEvent"
                val eventProperties = props("property1" pairTo 42, "property2" pairTo "test")
                val event = Event(eventKey, eventProperties)

                // Create a CullableOnEvent that should be culled on the specified event
                val cullableOnEvent =
                    CullableOnEvent(cullOnEvent = { _event -> _event.key == eventKey })

                cullableOnEvent.shouldBeCulled(1f) shouldBe false
                cullableOnEvent.onEvent(event)
                cullableOnEvent.shouldBeCulled(1f) shouldBe true

                // Reset the CullableOnEvent and test again
                cullableOnEvent.reset()

                cullableOnEvent.shouldBeCulled(1f) shouldBe false
                cullableOnEvent.onEvent(event)
                cullableOnEvent.shouldBeCulled(1f) shouldBe true
            }

            it("should not be culled on different event") {
                val eventKey = "testEvent"
                val eventProperties = props("property1" pairTo 42, "property2" pairTo "test")
                val event = Event(eventKey, eventProperties)

                // Create a CullableOnEvent that should not be culled on a different event
                val cullableOnEvent =
                    CullableOnEvent(cullOnEvent = { _event -> _event.key == "differentEvent" })

                cullableOnEvent.shouldBeCulled(1f) shouldBe false
                cullableOnEvent.onEvent(event)
                cullableOnEvent.shouldBeCulled(1f) shouldBe false
            }

            it("should reset cullable state") {
                val eventKey = "testEvent"
                val eventProperties = props("property1" pairTo 42, "property2" pairTo "test")
                val event = Event(eventKey, eventProperties)

                val cullableOnEvent =
                    CullableOnEvent(cullOnEvent = { _event -> _event.key == eventKey })

                cullableOnEvent.onEvent(event)
                cullableOnEvent.shouldBeCulled(1f) shouldBe true

                cullableOnEvent.reset()
                cullableOnEvent.shouldBeCulled(1f) shouldBe false
            }
        }
    })
