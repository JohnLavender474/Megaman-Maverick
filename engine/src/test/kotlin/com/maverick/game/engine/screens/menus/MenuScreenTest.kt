package com.maverick.game.engine.screens.menus

import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.extensions.objectMapOf
import com.maverick.game.engine.common.objects.pairTo
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.spyk
import io.mockk.verify

class MenuScreenTest : DescribeSpec({

    describe("MenuScreen") {

        lateinit var standardMenuScreen: StandardMenuScreen

        var direction: Direction?
        var selectionRequested: Boolean
        var onAnyMovement = false
        var backRequested = false
        var interactionAllowed = true
        val calls = ArrayList<String>()

        val firstButtonKey = "firstButtonKey"
        val secondButtonKey = "secondButtonKey"
        val buttons =
            objectMapOf(
                firstButtonKey pairTo
                    spyk(
                        object : IMenuButton {
                            override fun onSelect(delta: Float) = true

                            override fun onNavigate(direction: Direction, delta: Float) =
                                secondButtonKey
                        }),
                secondButtonKey pairTo
                    spyk(
                        object : IMenuButton {
                            override fun onSelect(delta: Float) = false

                            override fun onNavigate(direction: Direction, delta: Float) =
                                firstButtonKey
                        })
            )

        beforeEach {
            clearAllMocks()

            direction = null
            selectionRequested = false
            onAnyMovement = false
            backRequested = false
            interactionAllowed = true
            calls.clear()

            standardMenuScreen = spyk(
                object : StandardMenuScreen(buttons, firstButtonKey) {

                    init {
                        setCurrentButtonKey(firstButtonKey)
                    }

                    override fun onAnyMovement(direction: Direction) {
                        onAnyMovement = true
                        calls.add("navigate")
                    }

                    override fun isInteractionAllowed() = super.isInteractionAllowed() && interactionAllowed

                    override fun backRequested() = backRequested

                    override fun onBack(delta: Float) {
                        calls.add("back $delta")
                    }

                    override fun getNavigationDirection() = direction

                    override fun selectionRequested() = selectionRequested

                    override fun resize(width: Int, height: Int) {}

                    override fun pause() {}

                    override fun resume() {}

                    override fun hide() {}

                    override fun dispose() {}
                })
        }

        it("should show the menu screen correctly") {
            // when
            standardMenuScreen.show()

            // then
            standardMenuScreen.selectionMade shouldBe false
            standardMenuScreen.getCurrentButtonKey() shouldBe firstButtonKey
        }

        describe("renderLevelMap") {
            it("should renderLevelMap the menu screen correctly") {
                // when
                standardMenuScreen.render(0.0f)

                // then
                standardMenuScreen.selectionMade shouldBe false
                standardMenuScreen.getCurrentButtonKey() shouldBe firstButtonKey
            }

            it("should call button 1 navigation") {
                // if
                direction = Direction.UP

                // when
                standardMenuScreen.render(0.0f)

                // then
                standardMenuScreen.selectionMade shouldBe false
                standardMenuScreen.getCurrentButtonKey() shouldBe secondButtonKey
                verify(exactly = 1) { buttons[firstButtonKey]?.onNavigate(Direction.UP, 0.0f) }
                verify(exactly = 0) { buttons[secondButtonKey]?.onNavigate(Direction.DOWN, 0.0f) }
                verify(exactly = 0) { buttons[secondButtonKey]?.onSelect(0.0f) }
                onAnyMovement shouldBe true
            }

            it("should call button 2 navigation") {
                // given
                direction = Direction.DOWN
                standardMenuScreen.setCurrentButtonKey(secondButtonKey)

                // when
                standardMenuScreen.render(0.0f)

                // then
                standardMenuScreen.selectionMade shouldBe false
                standardMenuScreen.getCurrentButtonKey() shouldBe firstButtonKey
                verify(exactly = 1) { buttons[secondButtonKey]?.onNavigate(Direction.DOWN, 0.0f) }
                verify(exactly = 0) { buttons[firstButtonKey]?.onNavigate(Direction.UP, 0.0f) }
                verify(exactly = 0) { buttons[secondButtonKey]?.onSelect(0.0f) }
                onAnyMovement shouldBe true
            }

            it("should make selection") {
                // if
                selectionRequested = true

                // when
                standardMenuScreen.render(0.0f)

                // then
                standardMenuScreen.selectionMade shouldBe true
                standardMenuScreen.getCurrentButtonKey() shouldBe firstButtonKey
                verify(exactly = 0) { buttons[firstButtonKey]?.onNavigate(any(), 0.0f) }
                verify(exactly = 1) { buttons[firstButtonKey]?.onSelect(0.0f) }
                verify(exactly = 0) { buttons[secondButtonKey]?.onSelect(0.0f) }
                onAnyMovement shouldBe false
            }
        }

        describe("back action") {
            it("should run onBack once, with the delta of the frame, and nothing else") {
                backRequested = true

                standardMenuScreen.render(0.5f)

                calls shouldBe listOf("back 0.5")
                standardMenuScreen.selectionMade shouldBe false
                standardMenuScreen.getCurrentButtonKey() shouldBe firstButtonKey
                verify(exactly = 0) { buttons[firstButtonKey]?.onSelect(any()) }
                verify(exactly = 0) { buttons[secondButtonKey]?.onSelect(any()) }
            }

            it("should not run onBack with no back request") {
                standardMenuScreen.render(0.5f)

                calls shouldBe emptyList()
            }

            it("should not run onBack when a selection is made") {
                selectionRequested = true
                standardMenuScreen.render(0.5f)
                standardMenuScreen.selectionMade shouldBe true

                selectionRequested = false
                backRequested = true
                standardMenuScreen.render(0.5f)

                calls shouldBe emptyList()
            }

            it("should not run onBack when interaction is not allowed") {
                interactionAllowed = false
                backRequested = true

                standardMenuScreen.render(0.5f)

                standardMenuScreen.selectionMade shouldBe false
                calls shouldBe emptyList()
            }

            it("should not run onBack when the current key is null") {
                standardMenuScreen.setCurrentButtonKey(null)
                backRequested = true

                standardMenuScreen.render(0.5f)

                calls shouldBe emptyList()
            }

            it("should not run onBack when the current key has no button") {
                standardMenuScreen.setCurrentButtonKey("noButton")
                backRequested = true

                standardMenuScreen.render(0.5f)

                calls shouldBe emptyList()
            }

            it("should run the confirm and not onBack in one frame when onSelect gives true") {
                selectionRequested = true
                backRequested = true

                standardMenuScreen.render(0.5f)

                verify(exactly = 1) { buttons[firstButtonKey]?.onSelect(0.5f) }
                standardMenuScreen.selectionMade shouldBe true
                calls shouldBe emptyList()
            }

            it("should run the confirm and not onBack in one frame when onSelect gives false") {
                standardMenuScreen.setCurrentButtonKey(secondButtonKey)
                selectionRequested = true
                backRequested = true

                standardMenuScreen.render(0.5f)

                verify(exactly = 1) { buttons[secondButtonKey]?.onSelect(0.5f) }
                standardMenuScreen.selectionMade shouldBe false
                calls shouldBe emptyList()
            }

            it("should run a navigation first, then onBack, in one frame") {
                direction = Direction.UP
                backRequested = true

                standardMenuScreen.render(0.5f)

                calls shouldBe listOf("navigate", "back 0.5")
                standardMenuScreen.getCurrentButtonKey() shouldBe secondButtonKey
            }

            it("should have no back request in a subclass with no override, and act as before") {
                class NoBackScreen : StandardMenuScreen(buttons, firstButtonKey) {

                    init {
                        setCurrentButtonKey(firstButtonKey)
                    }

                    fun isBackRequested() = backRequested()

                    override fun getNavigationDirection() = Direction.UP

                    override fun selectionRequested() = true
                }

                val screen = NoBackScreen()

                screen.isBackRequested() shouldBe false

                screen.render(0.5f)

                verify(exactly = 1) { buttons[firstButtonKey]?.onNavigate(Direction.UP, 0.5f) }
                verify(exactly = 1) { buttons[firstButtonKey]?.onSelect(0.5f) }
                screen.getCurrentButtonKey() shouldBe secondButtonKey
                screen.selectionMade shouldBe true
            }
        }
    }
})
