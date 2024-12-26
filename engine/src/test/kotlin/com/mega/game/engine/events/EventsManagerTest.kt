package com.mega.game.engine.events

import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.objects.Properties
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.*

class EventsManagerTest :
    DescribeSpec({
        describe("EventManager") {
            var eventHandled = false
            lateinit var eventsManager: EventsManager

            beforeEach {
                eventHandled = false
                eventsManager = EventsManager()
            }

            it("should queue an event") {
                // if
                val event = Event("testEvent", Properties())

                // when
                eventsManager.submitEvent(event)

                // then
                eventsManager.events.containsKey(event.key) shouldBe true
            }

            it("should add and remove a listener") {
                // if
                val listener =
                    object : IEventListener {
                        override val eventKeyMask = ObjectSet<Any>()

                        override fun onEvent(event: Event) {
                            eventHandled = true
                        }
                    }

                // when
                eventsManager.addListener(listener)

                // then
                eventsManager.listeners.size shouldBe 1

                // when
                eventsManager.removeListener(listener)

                // then
                eventsManager.listeners.shouldBeEmpty()
            }

            it("should clear all listeners") {
                // if
                val listener1 =
                    object : IEventListener {
                        override val eventKeyMask = ObjectSet<Any>()

                        override fun onEvent(event: Event) {
                            eventHandled = true
                        }
                    }
                val listener2 =
                    object : IEventListener {
                        override val eventKeyMask = ObjectSet<Any>()

                        override fun onEvent(event: Event) {
                            eventHandled = true
                        }
                    }

                // when
                eventsManager.addListener(listener1)
                eventsManager.addListener(listener2)
                eventsManager.clearListeners()

                // then
                eventsManager.listeners.shouldBeEmpty()
            }

            it("should run and handle an event") {
                // if
                val listener =
                    object : IEventListener {
                        override val eventKeyMask = objectSetOf<Any>("testEvent")

                        override fun onEvent(event: Event) {
                            eventHandled = true
                        }
                    }
                val event = Event("testEvent", Properties())

                // when
                eventsManager.addListener(listener)
                eventsManager.submitEvent(event)
                eventsManager.run()

                // then
                eventHandled shouldBe true
                eventsManager.events.shouldBeEmpty()
            }

            it("should handle multiple listeners") {
                // if
                val listener1 =
                    mockk<IEventListener> {
                        every { eventKeyMask } returns objectSetOf("testEvent")
                        every { onEvent(any()) } just Runs
                    }
                val listener2 =
                    mockk<IEventListener> {
                        every { eventKeyMask } returns objectSetOf("testEvent")
                        every { onEvent(any()) } just Runs
                    }

                // when
                eventsManager.addListener(listener1)
                eventsManager.addListener(listener2)
                val event = Event("testEvent", Properties())
                eventsManager.submitEvent(event)
                eventsManager.run()

                // then
                verify(exactly = 1) {
                    listener1.onEvent(any())
                    listener2.onEvent(any())
                }
            }
        }
    })
