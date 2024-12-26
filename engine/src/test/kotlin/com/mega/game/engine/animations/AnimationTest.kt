package com.mega.game.engine.animations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.extensions.round
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk

class AnimationTest :
    DescribeSpec({
        describe("Animation") {
            val rows = 2
            val columns = 2
            val durations = Array<Float>()

            val mockRegionWidth = 4
            val mockRegionHeight = 4
            val mockRegion = mockk<TextureRegion>()

            lateinit var animation: Animation

            beforeEach {
                clearAllMocks()

                durations.clear()
                durations.add(0.25f)
                durations.add(0.5f)
                durations.add(0.25f)
                durations.add(0.5f)

                every { mockRegion.regionWidth } returns mockRegionWidth
                every { mockRegion.regionHeight } returns mockRegionHeight
                every { mockRegion.texture } returns null
                every { mockRegion.split(any(), any()) } answers
                        {
                            Array(rows) {
                                Array(columns) {
                                    mockk {
                                        every { regionWidth } returns mockRegionWidth / columns
                                        every { regionHeight } returns mockRegionHeight / rows
                                    }
                                }
                            }
                        }
                animation = Animation(mockRegion, rows, columns, durations)
            }

            it("should have the correct initial properties") {
                animation.getDuration() shouldBe durations.sum()
                animation.isFinished() shouldBe false
                animation.elapsedTime shouldBe 0f
            }

            describe("update") {
                it("should update correctly - looping") {
                    animation.setLooping(true)

                    // elapsed time = 0.2f
                    animation.update(0.2f)
                    animation.currentIndex shouldBe 0
                    animation.elapsedTime.round(2) shouldBe 0.2f
                    animation.isFinished() shouldBe false

                    // elapsed time = 0.45f
                    animation.update(0.25f)
                    animation.currentIndex shouldBe 1
                    animation.elapsedTime.round(2) shouldBe 0.45f
                    animation.isFinished() shouldBe false

                    // elapsed time = 0.7f
                    animation.update(0.25f)
                    animation.currentIndex shouldBe 1
                    animation.elapsedTime.round(2) shouldBe 0.70f
                    animation.isFinished() shouldBe false

                    // elapsed time = 0.95f
                    animation.update(0.25f)
                    animation.currentIndex shouldBe 2
                    animation.elapsedTime.round(2) shouldBe 0.95f
                    animation.isFinished() shouldBe false

                    // elapsed time = 1.01f
                    animation.update(0.051f)
                    animation.currentIndex shouldBe 3
                    animation.elapsedTime.round(3) shouldBe 1.001f
                    animation.isFinished() shouldBe false

                    // elapsed time = 1.249f
                    animation.update(0.248f)
                    animation.currentIndex shouldBe 3
                    animation.elapsedTime.round(3) shouldBe 1.249f
                    animation.isFinished() shouldBe false

                    // elapsed time = 1.5f
                    animation.update(0.251f)
                    animation.currentIndex shouldBe 0
                    animation.elapsedTime.round(2) shouldBe 0.00f
                    animation.isFinished() shouldBe false
                }

                it("should update correctly - not looping") {
                    animation.setLooping(false)

                    // elapsed time = 0.2f
                    animation.update(0.2f)
                    animation.currentIndex shouldBe 0
                    animation.elapsedTime.round(2) shouldBe 0.2f
                    animation.isFinished() shouldBe false

                    // elapsed time = 0.45f
                    animation.update(0.25f)
                    animation.currentIndex shouldBe 1
                    animation.elapsedTime.round(2) shouldBe 0.45f
                    animation.isFinished() shouldBe false

                    // elapsed time = 0.7f
                    animation.update(0.25f)
                    animation.currentIndex shouldBe 1
                    animation.elapsedTime.round(2) shouldBe 0.70f
                    animation.isFinished() shouldBe false

                    // elapsed time = 0.95f
                    animation.update(0.25f)
                    animation.currentIndex shouldBe 2
                    animation.elapsedTime.round(2) shouldBe 0.95f
                    animation.isFinished() shouldBe false

                    // elapsed time = 1.01f
                    animation.update(0.051f)
                    animation.currentIndex shouldBe 3
                    animation.elapsedTime.round(3) shouldBe 1.001f
                    animation.isFinished() shouldBe false

                    // elapsed time = 1.249f
                    animation.update(0.248f)
                    animation.currentIndex shouldBe 3
                    animation.elapsedTime.round(3) shouldBe 1.249f
                    animation.isFinished() shouldBe false

                    // elapsed time = 1.5f
                    animation.update(0.251f)
                    animation.currentIndex shouldBe 3
                    animation.elapsedTime.round(2) shouldBe 1.5f
                    animation.isFinished() shouldBe true

                    // elapsed time should remain the same
                    animation.update(1f)
                    animation.currentIndex shouldBe 3
                    animation.elapsedTime.round(2) shouldBe 1.5f
                    animation.isFinished() shouldBe true
                }
            }

            it("should reset the animation correctly") {
                animation.update(0.75f)
                animation.reset()
                animation.elapsedTime shouldBe 0f
                animation.isFinished() shouldBe false
            }

            it("should create an animation filled with single duration value") {
                val _animation = Animation(mockRegion, rows, columns, 0.5f)
                _animation.durations.size shouldBe rows * columns
            }

            it("should set the animation to looping mode") {
                animation.setLooping(true)
                animation.update(1.5f)
                animation.currentIndex shouldBe 0
                animation.isFinished() shouldBe false
            }

            describe("setIndex") {
                it("should set the current index correctly within valid bounds") {
                    animation.setIndex(2)
                    animation.currentIndex shouldBe 2
                    animation.isFinished() shouldBe false
                }

                it("should clamp the index to the first frame if index is less than 0") {
                    animation.setIndex(-1)
                    animation.currentIndex shouldBe 0
                }

                it("should clamp the index to the last frame if index exceeds frame count") {
                    animation.setIndex(5)
                    animation.currentIndex shouldBe 3
                }
            }

            describe("setTime") {
                it("should set the elapsed time correctly and update currentIndex accordingly") {
                    animation.setCurrentTime(0.75f)
                    animation.elapsedTime shouldBe 0.75f
                    animation.currentIndex shouldBe 1
                    animation.isFinished() shouldBe false
                }

                it("should roll-over animation time if loop is true and setTime exceeds total duration") {
                    animation.setCurrentTime(2.0f)
                    animation.elapsedTime shouldBe 0.5f
                    animation.currentIndex shouldBe 1
                    animation.isFinished() shouldBe false
                }

                it("should mark the animation as finished if loop is false and setTime exceeds total duration") {
                    animation.setLooping(false)
                    animation.setCurrentTime(2.0f)
                    animation.elapsedTime shouldBe 1.5f
                    animation.currentIndex shouldBe 3
                    animation.isFinished() shouldBe true
                }

                it("should throw exception if setTime is less than 0") {
                    shouldThrow<IllegalArgumentException> { animation.setCurrentTime(-0.5f) }
                }
            }
        }
    })
