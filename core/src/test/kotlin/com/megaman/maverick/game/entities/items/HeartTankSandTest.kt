package com.megaman.maverick.game.entities.items

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.camera.CameraView
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.systems.GameSystem
import com.maverick.game.engine.world.WorldSystem
import com.maverick.game.engine.world.container.SimpleGridWorldContainer
import com.maverick.game.engine.world.contacts.ContactRuleDispatcher
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.blocks.FeetRiseSinkBlock
import com.megaman.maverick.game.entities.megaman.constants.MegaHeartTank
import com.megaman.maverick.game.entities.special.QuickSand
import com.megaman.maverick.game.players.MegaPlayers
import com.megaman.maverick.game.players.WorldGravity
import com.megaman.maverick.game.state.GameMode
import com.megaman.maverick.game.testServices
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.getBlockFilters
import com.megaman.maverick.game.world.body.isSensing
import com.megaman.maverick.game.world.collisions.MegaCollisionHandler
import com.megaman.maverick.game.world.contacts.MegaContactFilter
import com.megaman.maverick.game.world.contacts.megaContactRules
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

private const val PPM = ConstVals.PPM.toFloat()
private const val STEP = 1f / 150f
private const val SAND_TOP = 0f
private const val SAND_DEPTH = 6f
private const val SAND_LEFT = -4f
private const val SAND_WIDTH = 8f
private const val FLOOR_LEFT = 20f
private const val VIEW_SIZE = 64f
private const val FALL_CYCLES = 100
private const val SINK_CYCLES = 30
private const val EXPECTED_SINK_SPEED = 4.07f

class HeartTankSandTest : DescribeSpec({

    lateinit var engine: GameEngine
    lateinit var services: MegaServices

    fun step(cycles: Int) = repeat(cycles) { engine.update(STEP) }

    beforeEach {
        val players = MegaPlayers()
        val gravity = WorldGravity(players)
        val worldContainer = SimpleGridWorldContainer(ConstVals.PPM)
        val worldSystem = WorldSystem(
            ppm = ConstVals.PPM,
            fixedStep = STEP,
            worldContainerSupplier = { worldContainer },
            contactListener = ContactRuleDispatcher(megaContactRules(players, gravity, GameMode.SINGLE.rules) { _, _ -> false }),
            collisionHandler = MegaCollisionHandler(),
            contactFilter = MegaContactFilter()
        )
        engine = GameEngine(Array<GameSystem>().apply { add(worldSystem) })
        services = testServices(
            engine = engine,
            players = players,
            gravity = gravity,
            gameCamera = CameraView(
                OrthographicCamera().apply {
                    viewportWidth = VIEW_SIZE * PPM
                    viewportHeight = VIEW_SIZE * PPM
                }
            )
        )
        val atlas = mockk<TextureAtlas>()
        every { atlas.findRegion(any()) } returns AtlasRegion(mockk<Texture>(relaxed = true), 0, 0, 64, 64)
        every { services.assets.get(any<String>(), TextureAtlas::class.java) } returns atlas

        MegaEntityFactory.init(services)

        QuickSand(services).spawn(
            props(
                ConstKeys.BOUNDS pairTo GameRectangle(
                    SAND_LEFT * PPM, SAND_TOP - SAND_DEPTH * PPM, SAND_WIDTH * PPM, SAND_DEPTH * PPM
                ),
                ConstKeys.FALL pairTo -0.5f,
                ConstKeys.RISE pairTo 3f,
                ConstKeys.MIN pairTo 2f
            )
        ) shouldBe true

        Block(services).spawn(
            props(ConstKeys.BOUNDS pairTo GameRectangle(FLOOR_LEFT * PPM, SAND_TOP - PPM, SAND_WIDTH * PPM, PPM))
        ) shouldBe true

        step(2)
    }

    afterEach {
        engine.reset()
        MegaEntityFactory.reset()
    }

    fun sinkBlock() = MegaGameEntities.getOfTag(Block.TAG).filterIsInstance<FeetRiseSinkBlock>().first()

    fun tankProps(x: Float) = props(
        ConstKeys.POSITION pairTo Vector2(x * PPM, SAND_TOP + 2f * PPM),
        ConstKeys.DIRECTION pairTo Direction.UP.name,
        ConstKeys.VALUE pairTo MegaHeartTank.C
    )

    fun spawnTank(x: Float = 0f): HeartTank {
        val tank = HeartTank(services)
        tank.spawn(tankProps(x)) shouldBe true
        return tank
    }

    fun measureSinkSpeed(tank: HeartTank): Float {
        val startY = tank.body.getY()
        step(SINK_CYCLES)
        return (startY - tank.body.getY()) / PPM / (SINK_CYCLES * STEP)
    }

    describe("a HeartTank that falls on quicksand") {
        it("sinks below the top of the block") {
            val tank = spawnTank()

            step(FALL_CYCLES)

            tank.body.isSensing(BodySense.FEET_ON_SAND) shouldBe true
            tank.body.getY() shouldBeLessThan sinkBlock().body.getMaxY() - PPM
        }

        it("sinks at the speed limit of the tank, with no sand friction") {
            val tank = spawnTank()

            step(FALL_CYCLES)

            measureSinkSpeed(tank) shouldBe (EXPECTED_SINK_SPEED plusOrMinus 0.1f)
        }

        it("does not move the block") {
            val tank = spawnTank()

            step(FALL_CYCLES)

            tank.body.isSensing(BodySense.FEET_ON_SAND) shouldBe true
            sinkBlock().body.getMaxY() shouldBe SAND_TOP
        }

        it("still sinks on a second spawn of the pooled tank") {
            val tank = spawnTank()
            step(1)
            val filterCount = tank.body.getBlockFilters()!!.size

            tank.destroy()
            step(1)
            tank.spawn(tankProps(0f)) shouldBe true
            step(FALL_CYCLES)

            tank.body.getBlockFilters()!!.size shouldBe filterCount
            tank.body.getY() shouldBeLessThan sinkBlock().body.getMaxY() - PPM
            measureSinkSpeed(tank) shouldBe (EXPECTED_SINK_SPEED plusOrMinus 0.1f)
        }
    }

    describe("controls") {
        it("a HeartTank that falls on a plain block stops on it") {
            val tank = spawnTank(FLOOR_LEFT + 2f)

            step(FALL_CYCLES)

            tank.body.isSensing(BodySense.FEET_ON_GROUND) shouldBe true
            tank.body.getY() shouldBe (SAND_TOP plusOrMinus 0.1f * PPM)
        }

        it("another item that falls on the quicksand block stops on it") {
            val bulb = HealthBulb(services)
            bulb.spawn(
                props(
                    ConstKeys.POSITION pairTo Vector2(0f, SAND_TOP + 2f * PPM),
                    ConstKeys.DIRECTION pairTo Direction.UP.name,
                    ConstKeys.LARGE pairTo false,
                    ConstKeys.TIMED pairTo false
                )
            ) shouldBe true

            step(FALL_CYCLES)
            step(SINK_CYCLES)

            bulb.body.getY() shouldBe (SAND_TOP plusOrMinus 0.1f * PPM)
        }
    }
})
