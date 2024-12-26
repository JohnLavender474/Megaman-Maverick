package com.mega.game.engine.drawables.sprites

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.*

class GameSpriteTest :
    DescribeSpec({
        lateinit var mockBatch: Batch
        lateinit var gameSprite: GameSprite

        beforeEach {
            clearAllMocks()
            mockBatch = mockk { every { draw(any<Texture>(), any(), any(), any()) } just Runs }
            gameSprite = spyk(GameSprite())
        }

        describe("GameSprite") {
            it("should draw when not hidden and has a texture") {
                every { gameSprite.texture } returns
                        mockk {
                            every { width } returns 1
                            every { height } returns 1
                        }
                gameSprite.hidden = false

                gameSprite.draw(mockBatch)

                verify(exactly = 1) { mockBatch.draw(any<Texture>(), any(), any(), any()) }
            }

            it("should not draw when hidden") {
                every { gameSprite.texture } returns
                        mockk {
                            every { width } returns 1
                            every { height } returns 1
                        }
                gameSprite.hidden = true

                gameSprite.draw(mockBatch)

                verify(exactly = 0) { mockBatch.draw(any<Texture>(), any(), any(), any()) }
            }

            it("should not draw when it has no texture") {
                every { gameSprite.texture } returns null
                gameSprite.hidden = false

                gameSprite.draw(mockBatch)

                verify(exactly = 0) { mockBatch.draw(any<Texture>(), any(), any(), any()) }
            }

            it("should draw when not hidden and has a texture") {
                every { gameSprite.texture } returns
                        mockk {
                            every { width } returns 1
                            every { height } returns 1
                        }
                gameSprite.hidden = false
                gameSprite.draw(mockBatch)

                verify(exactly = 1) { mockBatch.draw(any<Texture>(), any(), any(), any()) }
            }
        }
    })
