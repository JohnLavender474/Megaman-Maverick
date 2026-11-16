package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.camera.CameraView
import com.maverick.game.engine.common.enums.Position
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.systems.GameSystem
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.bosses.bigassmaverickrobot.HandSide
import com.megaman.maverick.game.testServices
import com.megaman.maverick.game.world.body.getPositionPoint
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

private const val PPM = ConstVals.PPM.toFloat()

class BigAssMaverickRobotHandTest : DescribeSpec({

    lateinit var engine: GameEngine
    lateinit var services: MegaServices

    beforeEach {
        engine = GameEngine(Array<GameSystem>())
        services = testServices(
            engine = engine,
            gameCamera = CameraView(
                OrthographicCamera().apply {
                    viewportWidth = 16f * PPM
                    viewportHeight = 14f * PPM
                }
            )
        )
        val atlas = mockk<TextureAtlas>()
        every { atlas.findRegion(any()) } returns AtlasRegion(mockk<Texture>(relaxed = true), 0, 0, 64, 64)
        every { services.assets.get(any<String>(), TextureAtlas::class.java) } returns atlas

        MegaEntityFactory.init(services)
    }

    afterEach {
        engine.reset()
        MegaEntityFactory.reset()
    }

    describe("BigAssMaverickRobotHand") {
        it("spawns its block under the hand at the boss, not at the old position of a pooled block") {
            val oldBlock = MegaEntityFactory.fetch(Block::class)!!
            oldBlock.spawn(props(ConstKeys.BOUNDS pairTo GameRectangle(-50f * PPM, 30f * PPM, PPM, PPM))) shouldBe true
            engine.update(0f)
            oldBlock.destroy()
            engine.update(0f)

            val bossCenter = Vector2(10f * PPM, 40f * PPM)
            val bossBody = Body(BodyType.ABSTRACT).also {
                it.setSize(4f * PPM)
                it.setCenter(bossCenter.x, bossCenter.y)
            }
            val boss = BigAssMaverickRobot(services)
            boss.addComponent(BodyComponent(bossBody))
            withClue("boss body") {
                boss.body.getPositionPoint(Position.CENTER, Vector2()).x shouldBe (bossCenter.x plusOrMinus 0.01f)
            }

            val hand = MegaEntityFactory.fetch(BigAssMaverickRobotHand::class)!!
            hand.spawn(
                props(
                    ConstKeys.OWNER pairTo boss,
                    ConstKeys.SIDE pairTo HandSide.LEFT,
                    ConstKeys.ORIGIN pairTo Vector2(0f, 30f * PPM),
                    "${ConstKeys.ARM}_${ConstKeys.ORIGIN}" pairTo Vector2(0f, 30f * PPM)
                )
            ) shouldBe true
            engine.update(0f)
            engine.update(0f)

            val handCenter = hand.body.getPositionPoint(Position.CENTER, Vector2()).cpy()
            val blockTop = oldBlock.body.getPositionPoint(Position.TOP_CENTER, Vector2()).cpy()
            withClue("hand=$handCenter block=$blockTop live=${hand.existenceState} ${oldBlock.existenceState}") {
                handCenter.x shouldBe (bossCenter.x plusOrMinus 0.01f)
                handCenter.y shouldBe (bossCenter.y plusOrMinus 0.01f)
                blockTop.x shouldBe (bossCenter.x plusOrMinus 0.01f)
                blockTop.y shouldBe (bossCenter.y + 0.5f * PPM plusOrMinus 0.01f)
            }
        }
    }
})
