package com.mega.game.engine.animations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.drawables.sprites.GameSprite
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*

class AnimatorTest :
    DescribeSpec({
        describe("Animator") {
            lateinit var region: TextureRegion
            val finished = false
            val duration = 1f
            var looping = true

            class MockAnimation(val id: String) : IAnimation {

                override fun getCurrentRegion() = region

                override fun isFinished() = finished

                override fun getDuration() = duration

                override fun setFrameDuration(frameDuration: Float) {
                    // TODO: do nothing
                }

                override fun setFrameDuration(index: Int, frameDuration: Float) {
                    // TODO: do nothing
                }

                override fun isLooping() = looping

                override fun setLooping(loop: Boolean) {
                    looping = loop
                }

                override fun reversed(): IAnimation {
                    return this
                }

                override fun slice(start: Int, end: Int): IAnimation {
                    return this
                }

                override fun setIndex(index: Int) {
                    // TODO: do nothing
                }

                override fun getIndex() = 0

                override fun setCurrentTime(time: Float) {
                    // TODO: do nothing
                }

                override fun getCurrentTime() = 0f

                override fun copy(): IAnimation {
                    return this
                }

                override fun update(delta: Float) {}

                override fun reset() {}
            }

            var key = "idle"
            val keySupplier: (String?) -> String? = { key }
            val animations = ObjectMap<String, IAnimation>()

            lateinit var mockSprite: GameSprite
            lateinit var animator: Animator

            beforeEach {
                clearAllMocks()

                animations.clear()
                animations.put("idle", spyk(MockAnimation("idle")))
                animations.put("walk", spyk(MockAnimation("walk")))

                key = "idle"
                region = mockk()
                mockSprite = mockk()
                every { mockSprite.setRegion(any<TextureRegion>()) } just Runs
                animator = Animator(keySupplier, animations)
            }

            it("should correctly initialize with provided key supplier and animations") {
                animator.keySupplier shouldBe keySupplier
                animator.animations shouldBe animations
                animator.currentAnimation shouldBe null
                animator.currentKey shouldBe null
            }

            describe("update") {
                it("should correctly update - test 1") {
                    animator.animate(mockSprite, 0.1f)

                    (animator.currentAnimation as MockAnimation).id shouldBe "idle"
                    animator.currentAnimation shouldBe animations["idle"]

                    verify(exactly = 1) { animations["idle"]?.update(0.1f) }
                    verify(exactly = 1) { mockSprite.setRegion(region) }
                }

                it("should correctly update - test 2") {
                    key = "walk"
                    animator.animate(mockSprite, 0.1f)

                    val currentAnimation = animator.currentAnimation as MockAnimation?

                    currentAnimation?.id shouldBe "walk"
                    currentAnimation shouldBe animations["walk"]
                    verify(exactly = 1) { currentAnimation?.update(0.1f) }
                    verify(exactly = 1) { mockSprite.setRegion(region) }
                }
            }

            it("should reset current animation when key changes") {
                animator.animate(mockSprite, 0.1f)
                val priorAnimation = animator.currentAnimation as MockAnimation?
                key = "walk"
                animator.animate(mockSprite, 0.1f)
                val currentAnimation = animator.currentAnimation as MockAnimation?

                currentAnimation shouldNotBe priorAnimation
                priorAnimation?.id shouldBe "idle"
                currentAnimation?.id shouldBe "walk"
                verify(exactly = 1) { priorAnimation?.update(any()) }
                verify(exactly = 1) { currentAnimation?.update(any()) }
                verify(exactly = 2) { mockSprite.setRegion(region) }
            }
        }
    })
