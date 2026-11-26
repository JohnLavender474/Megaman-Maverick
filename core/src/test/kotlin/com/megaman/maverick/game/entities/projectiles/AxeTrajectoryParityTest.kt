package com.megaman.maverick.game.entities.projectiles

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
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.systems.GameSystem
import com.maverick.game.engine.updatables.UpdatablesSystem
import com.maverick.game.engine.world.WorldSystem
import com.maverick.game.engine.world.container.SimpleGridWorldContainer
import com.maverick.game.engine.world.contacts.ContactRuleDispatcher
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.players.MegaPlayers
import com.megaman.maverick.game.players.WorldGravity
import com.megaman.maverick.game.state.GameMode
import com.megaman.maverick.game.testServices
import com.megaman.maverick.game.world.collisions.MegaCollisionHandler
import com.megaman.maverick.game.world.contacts.MegaContactFilter
import com.megaman.maverick.game.world.contacts.megaContactRules
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import kotlin.math.sqrt

private const val PPM = ConstVals.PPM.toFloat()
private const val STEP = 1f / 150f
private const val VIEW_SIZE = 64f
private const val TEXTURE_SIZE = 1024
private const val FRAME_SIZE = 16
private const val STEPS = 100
private const val SPACING = 0.5f * PPM
private const val MAX_DOTS = 64
private const val TOLERANCE = 0.05f
private const val STEP_TOLERANCE = 0.005f
private const val IMPULSE_X = 8f * PPM
private const val IMPULSE_Y = 20f * PPM

private fun throwImpulse(direction: Direction) = when (direction) {
    Direction.UP -> Vector2(IMPULSE_X, IMPULSE_Y)
    Direction.DOWN -> Vector2(-IMPULSE_X, -IMPULSE_Y)
    Direction.LEFT -> Vector2(-IMPULSE_Y, IMPULSE_X)
    Direction.RIGHT -> Vector2(IMPULSE_Y, -IMPULSE_X)
}

private fun dotsAlongPath(start: Vector2, path: List<Vector2>, spacing: Float): List<Vector2> {
    val dots = ArrayList<Vector2>()
    var previous = start
    var travelled = 0f
    var nextDotAt = spacing
    path.forEach { point ->
        val dx = point.x - previous.x
        val dy = point.y - previous.y
        val segment = sqrt(dx * dx + dy * dy)
        while (segment > 0f && travelled + segment >= nextDotAt) {
            val fraction = (nextDotAt - travelled) / segment
            dots.add(Vector2(previous.x + dx * fraction, previous.y + dy * fraction))
            nextDotAt += spacing
        }
        travelled += segment
        previous = point
    }
    return dots
}

class AxeTrajectoryParityTest : DescribeSpec({

    lateinit var engine: GameEngine
    fun step() = engine.update(STEP)

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
        engine = GameEngine(Array<GameSystem>().apply {
            add(worldSystem)
            add(UpdatablesSystem())
        })
        val services = testServices(
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
        val texture = mockk<Texture>(relaxed = true)
        every { texture.width } returns TEXTURE_SIZE
        every { texture.height } returns TEXTURE_SIZE
        val atlas = mockk<TextureAtlas>()
        every { atlas.findRegion(any()) } returns AtlasRegion(texture, 0, 0, FRAME_SIZE, FRAME_SIZE)
        every { services.assets.get(any<String>(), TextureAtlas::class.java) } returns atlas
        MegaEntityFactory.init(services)
    }

    afterEach {
        engine.reset()
        MegaEntityFactory.reset()
        clearAllMocks(answers = false)
    }

    fun throwAxe(start: Vector2, impulse: Vector2, direction: Direction, gravityScalar: Float): Axe {
        val axe = MegaEntityFactory.fetch(Axe::class)!!
        axe.spawn(
            props(
                ConstKeys.POSITION pairTo start,
                ConstKeys.IMPULSE pairTo impulse,
                ConstKeys.DIRECTION pairTo direction,
                "${ConstKeys.GRAVITY}_${ConstKeys.SCALAR}" pairTo gravityScalar
            )
        ) shouldBe true
        return axe
    }

    fun recordPath(axe: Axe, steps: Int): List<Vector2> = (0 until steps).map {
        step()
        axe.body.getCenter(Vector2())
    }

    Direction.entries.forEach { direction ->
        listOf(1f, 0.25f).forEach { scalar ->
            describe("an axe thrown in a $direction room with the gravity scalar $scalar") {
                val start = Vector2(1f * PPM, 2f * PPM)

                it("has the body gravity of AxeTrajectory.gravity") {
                    val axe = throwAxe(start, throwImpulse(direction), direction, scalar)
                    step()

                    val expected = AxeTrajectory.gravity(direction, scalar, Vector2())
                    axe.body.physics.gravity.x shouldBe expected.x
                    axe.body.physics.gravity.y shouldBe expected.y
                }

                it("moves by one full step in the first update after its spawn") {
                    val impulse = throwImpulse(direction)
                    val axe = throwAxe(start, impulse, direction, scalar)
                    val center = recordPath(axe, steps = 1).first()

                    val gravity = AxeTrajectory.gravity(direction, scalar, Vector2())
                    center.x shouldBe (start.x + (impulse.x + gravity.x * STEP) * STEP plusOrMinus STEP_TOLERANCE)
                    center.y shouldBe (start.y + (impulse.y + gravity.y * STEP) * STEP plusOrMinus STEP_TOLERANCE)
                }

                it("flies through the dots of AxeTrajectory.dots") {
                    val impulse = throwImpulse(direction)
                    val axe = throwAxe(start, impulse, direction, scalar)
                    val path = recordPath(axe, STEPS)
                    val expected = dotsAlongPath(start, path, SPACING)

                    val out = Array<Vector2>().apply { repeat(MAX_DOTS) { add(Vector2()) } }
                    val count = AxeTrajectory.dots(
                        start,
                        impulse,
                        AxeTrajectory.gravity(direction, scalar, Vector2()),
                        direction,
                        STEP,
                        SPACING,
                        maxDots = MAX_DOTS,
                        maxTime = (STEPS - 0.5f) * STEP,
                        out = out
                    )

                    count shouldBe expected.size
                    for (i in 0 until count) withClue("dot $i") {
                        out[i].x shouldBe (expected[i].x plusOrMinus TOLERANCE)
                        out[i].y shouldBe (expected[i].y plusOrMinus TOLERANCE)
                    }
                }
            }
        }
    }
})
