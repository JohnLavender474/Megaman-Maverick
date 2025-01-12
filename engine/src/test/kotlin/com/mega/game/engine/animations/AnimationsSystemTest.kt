package com.mega.game.engine.animations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mega.game.engine.MockGameEntity
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.entities.GameEntity
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.*

class AnimationsSystemTest :
    DescribeSpec({
        describe("AnimationSystem") {
            lateinit var entity: GameEntity
            lateinit var mockSprite: GameSprite
            lateinit var mockAnimator: IAnimator
            lateinit var animationsComponent: AnimationsComponent
            lateinit var animationsSystem: AnimationsSystem

            beforeEach {
                clearAllMocks()
                entity = MockGameEntity()
                mockSprite = mockk { every { setRegion(any<TextureRegion>()) } just Runs }
                mockAnimator = mockk {
                    every { shouldAnimate(any()) } returns true
                    every { animate(any(), any()) } answers
                        {
                            val sprite = arg<GameSprite>(0)
                            sprite.setRegion(TextureRegion())
                        }
                }
                animationsComponent = spyk(AnimationsComponent())
                animationsComponent.putAnimator(mockSprite, mockAnimator)
                animationsSystem = spyk(AnimationsSystem())
                entity.addComponent(animationsComponent)
                animationsSystem.add(entity)
            }

            it("process") {
                animationsSystem.update(1f)
                verify(exactly = 1) { mockAnimator.animate(mockSprite, 1f) }
                verify(exactly = 1) { mockSprite.setRegion(any<TextureRegion>()) }
            }
        }
    })
