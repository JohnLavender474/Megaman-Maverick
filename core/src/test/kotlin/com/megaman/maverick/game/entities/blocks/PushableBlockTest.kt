package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.camera.CameraView
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.systems.GameSystem
import com.maverick.game.engine.updatables.UpdatablesSystem
import com.maverick.game.engine.world.WorldSystem
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.maverick.game.engine.world.container.SimpleGridWorldContainer
import com.maverick.game.engine.world.contacts.ContactRuleDispatcher
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.special.QuickSand
import com.megaman.maverick.game.entities.state.SandReaction
import com.megaman.maverick.game.entities.state.SandReactionComponent
import com.megaman.maverick.game.players.MegaPlayers
import com.megaman.maverick.game.players.WorldGravity
import com.megaman.maverick.game.screens.levels.spawns.SpawnType
import com.megaman.maverick.game.state.GameMode
import com.megaman.maverick.game.state.Performance
import com.megaman.maverick.game.testServices
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.isSensing
import com.megaman.maverick.game.world.collisions.MegaCollisionHandler
import com.megaman.maverick.game.world.contacts.MegaContactFilter
import com.megaman.maverick.game.world.contacts.megaContactRules
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.ints.shouldBeLessThan as intShouldBeLessThan
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlin.math.ceil

private const val PPM = ConstVals.PPM.toFloat()
private const val STEP = 1f / 150f

private class Rider(services: MegaServices) : MegaGameEntity(services), IBodyEntity {

    override fun init(vararg params: Any) {
        super.init()
        val body = Body(BodyType.DYNAMIC)
        body.setSize(PPM)
        body.physics.gravityOn = true
        val feet = Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.5f * PPM, 0.25f * PPM))
        feet.offsetFromBodyAttachment.y = -0.5f * PPM
        body.addFixture(feet)
        body.preProcess.put(ConstKeys.GRAVITY) {
            val grounded = body.isSensing(BodySense.FEET_ON_GROUND)
            body.physics.gravity.y = if (grounded) 0f else -37.5f * PPM
            if (grounded) body.physics.velocity.y = 0f
        }
        addComponent(BodyComponentCreator.create(this, body))
        addComponent(SandReactionComponent(SandReaction.SINK))
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        body.setBottomCenterToPoint(spawnProps.get(ConstKeys.POSITION, Vector2::class)!!)
    }

    override fun getType() = EntityType.SPECIAL
}

class PushableBlockTest : DescribeSpec({

    lateinit var engine: GameEngine
    lateinit var services: MegaServices
    lateinit var world: WorldSystem
    var cycleStep = STEP

    fun step(cycles: Int) = repeat(cycles) { engine.update(cycleStep) }

    fun useTier(tier: Performance) {
        world.fixedStep = tier.fixedStep
        cycleStep = tier.fixedStep
    }

    beforeEach {
        cycleStep = STEP
        val players = MegaPlayers(2)
        val gravity = WorldGravity(players)
        val container = SimpleGridWorldContainer(ConstVals.PPM)
        world = WorldSystem(
            ppm = ConstVals.PPM,
            fixedStep = STEP,
            worldContainerSupplier = { container },
            contactListener = ContactRuleDispatcher(
                megaContactRules(players, gravity, GameMode.COOP.rules) { _, _ -> false }
            ),
            collisionHandler = MegaCollisionHandler(),
            contactFilter = MegaContactFilter()
        )
        engine = GameEngine(Array<GameSystem>().apply {
            add(world)
            add(UpdatablesSystem())
        })
        val camera = OrthographicCamera().apply {
            viewportWidth = 200f * PPM
            viewportHeight = 200f * PPM
        }
        services = testServices(engine = engine, players = players, gravity = gravity, gameCamera = CameraView(camera))
        val atlas = mockk<TextureAtlas>()
        every { atlas.findRegion(any()) } returns AtlasRegion(mockk<Texture>(relaxed = true), 0, 0, 64, 64)
        every { services.assets.get(any<String>(), TextureAtlas::class.java) } returns atlas
        MegaEntityFactory.init(services)
    }

    afterEach {
        engine.reset()
        MegaEntityFactory.reset()
    }

    fun ledge() {
        Block(services).spawn(props(ConstKeys.BOUNDS pairTo GameRectangle(-10f * PPM, -PPM, 10f * PPM, PPM))) shouldBe true
    }

    fun wall() {
        Block(services).spawn(props(ConstKeys.BOUNDS pairTo GameRectangle(2f * PPM, -PPM, 2f * PPM, 3f * PPM))) shouldBe true
    }

    fun block(x: Float): PushableBlock {
        val block = PushableBlock(services)
        val spawnProps = props(
            ConstKeys.BOUNDS pairTo GameRectangle(x * PPM, 0f, 2f * PPM, 2f * PPM),
            SpawnType.SPAWN_ROOM pairTo "room"
        )
        block.spawn(spawnProps) shouldBe true
        return block
    }

    describe("a pushable block") {
        Performance.entries.forEach { tier ->
            listOf(0.5f, 8f, 16f).forEach { s ->
                listOf(0, 900).forEach { rest ->
                    it("leaves a support of $s px within 0.15 s at $tier after $rest steps of rest") {
                        useTier(tier)
                        ledge()
                        wall()
                        val block = block(-2f)
                        step(3 + rest)

                        block.body.setX(-s)
                        step(ceil(0.15f / cycleStep).toInt())

                        block.body.getX() shouldBeGreaterThan -0.01f
                        block.body.getY() shouldBeLessThan -1f
                    }
                }
            }

            listOf(16.5f, 24f, 32f).forEach { s ->
                it("stays on a support of $s px at $tier") {
                    useTier(tier)
                    ledge()
                    wall()
                    val block = block(-2f)
                    step(3)

                    block.body.setX(-s)
                    step(900)

                    block.body.getX() shouldBe -s
                    block.body.getY() shouldBeGreaterThan -1f
                    block.body.isSensing(BodySense.FEET_ON_GROUND) shouldBe true
                }
            }
        }

        it("slides off when it lands on a corner") {
            ledge()
            val block = block(-2f)
            step(1)
            block.body.setY(2f * PPM)
            block.body.setX(-16f)
            step(150)

            block.body.getX() shouldBeGreaterThan -0.01f
            block.body.getY() shouldBeLessThan -1f
        }

        it("crosses a seam between 2 blocks at the same height") {
            ledge()
            Block(services).spawn(props(ConstKeys.BOUNDS pairTo GameRectangle(0f, -PPM, 10f * PPM, PPM))) shouldBe true
            val block = block(-2f)
            step(3)

            while (block.body.getX() < 0f) {
                block.body.physics.velocity.x = 4f * PPM
                step(1)
                block.body.isSensing(BodySense.FEET_ON_GROUND) shouldBe true
                kotlin.math.abs(block.body.getY()) shouldBeLessThan 1f
            }
        }

        listOf(-8f, -56f).forEach { x ->
            it("does not creep when it rests over a seam at x = $x") {
                ledge()
                Block(services).spawn(props(ConstKeys.BOUNDS pairTo GameRectangle(0f, -PPM, 10f * PPM, PPM))) shouldBe true
                val block = block(-2f)
                step(3)

                block.body.setX(x)
                step(900)

                block.body.getX() shouldBe x
            }
        }

        it("stays at a wall that it is pushed into") {
            ledge()
            Block(services).spawn(props(ConstKeys.BOUNDS pairTo GameRectangle(-2f * PPM, 0f, PPM, 3f * PPM))) shouldBe true
            val block = block(-4f)
            step(3)

            repeat(120) {
                block.body.physics.velocity.x = 4f * PPM
                step(1)
            }

            kotlin.math.abs(block.body.getMaxX() + 2f * PPM) shouldBeLessThan 1f
            kotlin.math.abs(block.body.getY()) shouldBeLessThan 1f
            block.body.isSensing(BodySense.FEET_ON_GROUND) shouldBe true
        }

        it("keeps its height under a ceiling corner") {
            ledge()
            Block(services).spawn(props(ConstKeys.BOUNDS pairTo GameRectangle(-3f * PPM, 2f * PPM, 3f * PPM, PPM))) shouldBe true
            val block = block(-6f)
            step(3)

            while (block.body.getMaxX() <= -2f * PPM) {
                block.body.physics.velocity.x = 2f * PPM
                step(1)
                kotlin.math.abs(block.body.getY()) shouldBeLessThan 1f
            }
        }

        it("carries a rider when it slides off") {
            ledge()
            val block = block(-2f)
            step(3)
            block.body.setX(-20f)
            step(3)
            val rider = Rider(services)
            rider.spawn(props(ConstKeys.POSITION pairTo Vector2(block.body.getX() + PPM, block.body.getMaxY()))) shouldBe true
            step(3)

            block.body.translate(12f, 0f)
            rider.body.translate(12f, 0f)
            repeat(60) {
                step(1)
                rider.body.getY() shouldBeGreaterThan block.body.getMaxY() - PPM
            }
            block.body.getY() shouldBeLessThan -1f
        }

        it("falls at the same speed after a short and a long rest") {
            ledge()
            val long = block(-2f)
            step(1050)
            val short = block(-6f)
            step(150)

            long.body.setX(1f * PPM)
            short.body.setX(4f * PPM)
            step(10)

            long.body.physics.velocity.y shouldBe (short.body.physics.velocity.y plusOrMinus 0.01f)
        }

        Performance.entries.forEach { tier ->
            it("drops onto the plate in a hole and rests on it with no jitter at $tier") {
                useTier(tier)
                ledge()
                wall()
                Block(services).spawn(props(ConstKeys.BOUNDS pairTo GameRectangle(0f, -3f * PPM, 2f * PPM, PPM))) shouldBe true
                val plate = FeetRiseSinkBlock(services)
                plate.spawn(
                    props(
                        ConstKeys.BOUNDS pairTo GameRectangle(-0.5f * PPM, -1.5f * PPM, 3f * PPM, 0.5f * PPM),
                        ConstKeys.FALL pairTo 3f,
                        ConstKeys.RISE pairTo 3f,
                        ConstKeys.MIN pairTo 0.25f
                    )
                ) shouldBe true
                val block = block(-2f)
                step(3)

                block.body.setX(-1f)
                step(ceil(1f / cycleStep).toInt())

                val restY = block.body.getY()
                repeat(ceil(1f / cycleStep).toInt()) {
                    step(1)
                    block.body.isSensing(BodySense.FEET_ON_GROUND) shouldBe true
                    kotlin.math.abs(block.body.getY() - restY) shouldBeLessThan 0.01f
                }
                kotlin.math.abs(plate.body.getMaxY() + 1.25f * PPM) shouldBeLessThan 0.1f
                kotlin.math.abs(block.body.getY() - plate.body.getMaxY()) shouldBeLessThan 0.1f
            }
        }

        it("stays on a sinking plate and holds it down") {
            val plate = FeetRiseSinkBlock(services)
            plate.spawn(
                props(
                    ConstKeys.BOUNDS pairTo GameRectangle(-2f * PPM, -0.5f * PPM, 3f * PPM, 0.5f * PPM),
                    ConstKeys.FALL pairTo 3f,
                    ConstKeys.RISE pairTo 3f,
                    ConstKeys.MIN pairTo 0.25f
                )
            ) shouldBe true
            val block = block(-1.5f)
            step(300)

            block.body.isSensing(BodySense.FEET_ON_GROUND) shouldBe true
            plate.body.getMaxY() shouldBeLessThan -0.2f * PPM
            kotlin.math.abs(block.body.getY() - plate.body.getMaxY()) shouldBeLessThan 1f
        }

        listOf(0.5f, 8f, 16f).forEach { s ->
            it("leaves a support of $s px next to sand") {
                ledge()
                val sand = QuickSand(services)
                sand.spawn(
                    props(
                        ConstKeys.BOUNDS pairTo GameRectangle(0f, -PPM, 6f * PPM, PPM),
                        ConstKeys.FALL pairTo -0.5f,
                        ConstKeys.RISE pairTo 3f,
                        ConstKeys.MIN pairTo 2f
                    )
                ) shouldBe true
                val block = block(-2f)
                step(3)

                block.body.setX(-s)
                step(60)

                block.body.getX() shouldBeGreaterThan -0.01f
                block.body.getY() shouldBeLessThan -0.1f * PPM
            }
        }

        it("starts to sink when it is pushed from a ledge onto sand") {
            ledge()
            val sand = QuickSand(services)
            sand.spawn(
                props(
                    ConstKeys.BOUNDS pairTo GameRectangle(0f, -PPM, 6f * PPM, PPM),
                    ConstKeys.FALL pairTo -0.5f,
                    ConstKeys.RISE pairTo 3f,
                    ConstKeys.MIN pairTo 2f
                )
            ) shouldBe true
            val block = block(-2f)
            step(3)

            while (block.body.getX() < 0f) {
                block.body.physics.velocity.x = 4f * PPM
                step(1)
            }
            step(60)

            block.body.getY() shouldBeLessThan -0.1f * PPM
        }

        it("falls at full gravity and stops its x velocity after a short rest on a ledge") {
            ledge()
            val block = block(-2f)
            step(3)
            block.body.isSensing(BodySense.FEET_ON_GROUND) shouldBe true

            while (block.body.getX() < 0f) {
                block.body.physics.velocity.x = 4f * PPM
                step(1)
            }
            val startX = block.body.getX()
            step(15)

            kotlin.math.abs(block.body.getX() - startX) shouldBeLessThan 0.1f * PPM
            block.body.physics.velocity.x shouldBe 0f
            block.body.isSensing(BodySense.FEET_ON_GROUND) shouldBe false
            block.body.physics.velocity.y shouldBeLessThan -2f * PPM
        }

        it("starts to sink at once when it reaches sand slowly") {
            val sand = QuickSand(services)
            sand.spawn(
                props(
                    ConstKeys.BOUNDS pairTo GameRectangle(-6f * PPM, -PPM, 12f * PPM, PPM),
                    ConstKeys.FALL pairTo -0.5f,
                    ConstKeys.RISE pairTo 3f,
                    ConstKeys.MIN pairTo 2f
                )
            ) shouldBe true
            val block = block(-1f)
            step(30)

            block.body.getY() shouldBeLessThan -0.05f * PPM
            block.body.getY() shouldBeGreaterThan -2f * PPM
        }

        it("carries an entity that stands on it while it sinks") {
            val sand = QuickSand(services)
            sand.spawn(
                props(
                    ConstKeys.BOUNDS pairTo GameRectangle(-6f * PPM, -PPM, 12f * PPM, PPM),
                    ConstKeys.FALL pairTo -0.5f,
                    ConstKeys.RISE pairTo 3f,
                    ConstKeys.MIN pairTo 2f
                )
            ) shouldBe true
            val block = block(-1f)
            step(3)
            val rider = Rider(services)
            rider.spawn(props(ConstKeys.POSITION pairTo Vector2(0f, block.body.getMaxY()))) shouldBe true

            repeat(60) {
                step(1)
                kotlin.math.abs(rider.body.getY() - block.body.getMaxY()) shouldBeLessThan 1f
            }

            block.body.getY() shouldBeLessThan -0.1f * PPM
        }

        it("drops into a hole of its own width after a rest, with a sliver left on the ledge") {
            ledge()
            Block(services).spawn(props(ConstKeys.BOUNDS pairTo GameRectangle(2f * PPM, -PPM, 2f * PPM, 3f * PPM))) shouldBe true
            val block = block(-2f)
            step(900)

            block.body.setX(-0.5f)
            step(30)

            block.body.getY() shouldBeLessThan -0.5f * PPM
        }

        it("stays until its top is half a tile under the sand") {
            val sand = QuickSand(services)
            sand.spawn(
                props(
                    ConstKeys.BOUNDS pairTo GameRectangle(-6f * PPM, -PPM, 12f * PPM, PPM),
                    ConstKeys.FALL pairTo -0.5f,
                    ConstKeys.RISE pairTo 3f,
                    ConstKeys.MIN pairTo 2f
                )
            ) shouldBe true
            val block = block(-1f)
            step(1)

            var lastTop = block.body.getMaxY()
            var cycles = 0
            while (block.existenceState.liveNow && cycles < 1200) {
                lastTop = block.body.getMaxY()
                step(1)
                cycles++
            }

            block.existenceState.liveNow shouldBe false
            lastTop shouldBeLessThan -15.5f
        }

        it("sinks with an entity on it, and the entity does not stop at the sand") {
            val sand = QuickSand(services)
            sand.spawn(
                props(
                    ConstKeys.BOUNDS pairTo GameRectangle(-6f * PPM, -PPM, 12f * PPM, PPM),
                    ConstKeys.FALL pairTo -0.5f,
                    ConstKeys.RISE pairTo 3f,
                    ConstKeys.MIN pairTo 2f
                )
            ) shouldBe true
            val block = block(-1f)
            step(3)
            val rider = Rider(services)
            rider.spawn(props(ConstKeys.POSITION pairTo Vector2(0f, block.body.getMaxY()))) shouldBe true
            step(2)

            var longestStop = 0
            var stop = 0
            repeat(800) {
                val y = rider.body.getY()
                step(1)
                stop = if (rider.body.getY() == y) stop + 1 else 0
                longestStop = maxOf(longestStop, stop)
            }

            longestStop intShouldBeLessThan 3
            rider.body.getY() shouldBeLessThan -0.5f * PPM
        }
    }
})
