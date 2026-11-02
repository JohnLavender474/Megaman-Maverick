package com.megaman.maverick.game.entities.special

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
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.systems.GameSystem
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
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.blocks.FeetRiseSinkBlock
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.state.PlayPhase
import com.megaman.maverick.game.entities.state.PlayPhaseComponent
import com.megaman.maverick.game.entities.state.SandReactionComponent
import com.megaman.maverick.game.entities.state.SandReaction
import com.megaman.maverick.game.players.MegaPlayers
import com.megaman.maverick.game.players.WorldGravity
import com.megaman.maverick.game.state.GameMode
import com.megaman.maverick.game.testServices
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.collisions.MegaCollisionHandler
import com.megaman.maverick.game.world.contacts.MegaContactFilter
import com.megaman.maverick.game.world.contacts.megaContactRules
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

private const val PPM = ConstVals.PPM.toFloat()
private const val STEP = 1f / 150f
private const val SINK_CYCLES = 60
private const val FAR_X = 40f

private class SandWalker(
    services: MegaServices,
    private val reaction: SandReaction? = SandReaction.SINK,
    private val bodyType: BodyType = BodyType.DYNAMIC
) : MegaGameEntity(services), IBodyEntity {

    override fun init(vararg params: Any) {
        super.init()
        val body = Body(bodyType)
        body.setSize(PPM)
        body.physics.gravityOn = true
        body.physics.gravity.y = -8f * PPM
        val feet = Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.5f * PPM, 0.1f * PPM))
        feet.offsetFromBodyAttachment.y = -0.5f * PPM
        body.addFixture(feet)
        addComponent(BodyComponentCreator.create(this, body))
        addComponent(PlayPhaseComponent(PlayPhase.PRE_PLAY))
        reaction?.let { addComponent(SandReactionComponent(it)) }
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        body.setBottomCenterToPoint(spawnProps.get(ConstKeys.POSITION, Vector2::class)!!)
    }

    override fun getType() = EntityType.SPECIAL
}

class QuickSandFloorsTest : DescribeSpec({

    lateinit var engine: GameEngine
    lateinit var services: MegaServices
    lateinit var players: MegaPlayers
    lateinit var camera: OrthographicCamera

    fun step(cycles: Int) = repeat(cycles) { engine.update(STEP) }

    beforeEach {
        players = MegaPlayers(2)
        val gravity = WorldGravity(players)
        val container = SimpleGridWorldContainer(ConstVals.PPM)
        val worldSystem = WorldSystem(
            ppm = ConstVals.PPM,
            fixedStep = STEP,
            worldContainerSupplier = { container },
            contactListener = ContactRuleDispatcher(
                megaContactRules(players, gravity, GameMode.COOP.rules) { _, _ -> false }
            ),
            collisionHandler = MegaCollisionHandler(),
            contactFilter = MegaContactFilter()
        )
        engine = GameEngine(Array<GameSystem>().apply { add(worldSystem) })
        camera = OrthographicCamera().apply {
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

    fun walker(
        slot: Int?,
        x: Float,
        reaction: SandReaction? = SandReaction.SINK,
        bodyType: BodyType = BodyType.DYNAMIC
    ): SandWalker {
        val walker = SandWalker(services, reaction, bodyType)
        if (slot != null) players.register(PlayerSlot(slot), walker)
        walker.spawn(props(ConstKeys.POSITION pairTo Vector2(x * PPM, 0.1f * PPM))) shouldBe true
        return walker
    }

    fun sand(min: Float = 2f): QuickSand {
        val sand = QuickSand(services)
        val spawnProps = props(
            ConstKeys.BOUNDS pairTo GameRectangle(-6f * PPM, -PPM, 12f * PPM, PPM),
            ConstKeys.FALL pairTo -0.5f,
            ConstKeys.RISE pairTo 3f,
            ConstKeys.MIN pairTo min
        )
        sand.spawn(spawnProps) shouldBe true
        return sand
    }

    fun blocks() = MegaGameEntities.getOfTag(Block.TAG).filter { it is FeetRiseSinkBlock }

    fun sharedFloors() = blocks().take(1)

    fun privateFloors() = blocks().drop(1).filterIsInstance<FeetRiseSinkBlock>()

    fun floorOf(entity: SandWalker) = privateFloors()
        .minByOrNull { kotlin.math.abs(it.body.getMaxY() - entity.body.getY()) }!!

    describe("the floors") {
        it("spawns one shared floor and no private floor with no sinkable feet in the area") {
            walker(0, FAR_X)
            sand()
            step(3)

            sharedFloors().size shouldBe 1
            privateFloors().size shouldBe 0
        }

        it("spawns a private floor for a single player, once, and destroys it when the feet leave") {
            val player = walker(0, FAR_X)
            sand()
            step(3)

            player.body.setPosition(5f * PPM, 0.1f * PPM)
            step(3)
            privateFloors().size shouldBe 1

            step(10)
            privateFloors().size shouldBe 1

            player.body.setX(FAR_X * PPM)
            step(3)
            privateFloors().size shouldBe 0
        }

        it("keeps a private floor and its sinking when the sand leaves the camera") {
            walker(0, 5f)
            sand()
            step(SINK_CYCLES)
            val floor = privateFloors().first()
            val depth = floor.body.getMaxY()

            camera.position.x = 1000f * PPM
            step(SINK_CYCLES)

            privateFloors().size shouldBe 1
            floor.body.getMaxY() shouldBeLessThan depth
        }

        it("runs no sensor for sand that is off camera with no private floor") {
            camera.position.x = 1000f * PPM
            walker(0, 5f)
            sand()
            step(3)

            privateFloors().size shouldBe 0
        }

        it("destroys every floor when the QuickSand is destroyed") {
            walker(0, 5f)
            val sand = sand()
            step(3)
            blocks().size shouldBe 2

            sand.destroy()
            step(2)

            blocks().size shouldBe 0
        }
    }

    describe("a sinkable entity") {
        it("sinks alone with 1 player, also below the shared floor") {
            val player = walker(0, 5f)
            sand()
            step(SINK_CYCLES)

            player.body.getY() shouldBeLessThan -0.1f * PPM
            (sharedFloors().first() as Block).body.getMaxY() shouldBe (0f plusOrMinus 0.01f)
        }

        it("makes its floor rise when it leaves the floor") {
            val player = walker(0, 5f)
            sand()
            step(SINK_CYCLES)
            val floor = privateFloors().first()
            val sunk = floor.body.getMaxY()

            player.body.setY(0.5f * PPM)
            step(10)

            floor.body.getMaxY() shouldBeGreaterThan sunk
        }

        it("sinks only by its own feet with 2 players") {
            val first = walker(0, -5f)
            val second = walker(1, FAR_X)
            sand()
            step(SINK_CYCLES)
            val firstDepth = first.body.getY()

            second.body.setPosition(5f * PPM, 0.1f * PPM)
            step(5)

            firstDepth shouldBeLessThan -0.1f * PPM
            second.body.getY() shouldBeGreaterThan -0.05f * PPM
            privateFloors().size shouldBe 2
        }

        it("rises alone while the other player sinks") {
            val first = walker(0, -5f)
            val second = walker(1, 5f)
            sand()
            step(SINK_CYCLES)
            privateFloors().size shouldBe 2
            val firstFloor = floorOf(first)
            val secondFloor = privateFloors().first { it != firstFloor }
            val secondSunk = secondFloor.body.getMaxY()
            val firstSunk = firstFloor.body.getMaxY()

            second.body.setY(0.5f * PPM)
            step(10)

            secondFloor.body.getMaxY() shouldBeGreaterThan secondSunk
            firstFloor.body.getMaxY() shouldBeLessThan firstSunk
        }
    }

    describe("an entity that cannot sink") {
        it("rests on the shared floor while a player sinks") {
            walker(0, 5f)
            val other = walker(null, -5f, reaction = null)
            sand()
            step(SINK_CYCLES)

            privateFloors().size shouldBe 1
            other.body.getY() shouldBe (0f plusOrMinus 0.5f)
        }

        it("rests on the shared floor with SURFACE, as DarkSerket does") {
            val other = walker(null, -5f, reaction = SandReaction.SURFACE)
            sand()
            step(SINK_CYCLES)

            privateFloors().size shouldBe 0
            other.body.getY() shouldBe (0f plusOrMinus 0.5f)
        }

        it("gets no private floor with an ABSTRACT body, because a floor cannot hold it") {
            walker(null, -5f, bodyType = BodyType.ABSTRACT)
            sand()
            step(SINK_CYCLES)

            privateFloors().size shouldBe 0
        }
    }

    describe("an entity with SUBMERGE") {
        it("sinks past a shallow floor until it is under the sand, and then it is destroyed with its private floor") {
            val walker = walker(null, -5f, reaction = SandReaction.SUBMERGE)
            sand(min = 0.5f)
            step(250)

            walker.body.getY() shouldBeLessThan -0.6f * PPM
            walker.existenceState.liveNow shouldBe true

            step(250)

            walker.existenceState.liveNow shouldBe false
            privateFloors().size shouldBe 0
        }
    }

    describe("a private floor under an entity that stands on another block") {
        it("does not sink while the feet touch it, and it sinks when the entity stands on it") {
            val ledgeBounds = GameRectangle(-6f * PPM, -PPM, 2f * PPM, PPM)
            Block(services).spawn(props(ConstKeys.BOUNDS pairTo ledgeBounds)) shouldBe true
            val player = walker(0, -5f)
            sand()
            step(10)

            privateFloors().size shouldBe 1
            val floor = privateFloors().first()
            var lowest = floor.body.getMaxY()
            repeat(SINK_CYCLES) {
                step(1)
                lowest = minOf(lowest, floor.body.getMaxY())
            }
            lowest shouldBe (0f plusOrMinus 0.01f)

            player.body.setX(3f * PPM)
            step(SINK_CYCLES)

            floor.body.getMaxY() shouldBeLessThan -0.1f * PPM
        }
    }

    describe("an entity with SINK that is not a player") {
        it("gets a private floor and sinks on it") {
            val enemy = walker(null, -5f)
            sand()
            step(SINK_CYCLES)

            privateFloors().size shouldBe 1
            enemy.body.getY() shouldBeLessThan -0.1f * PPM
        }
    }
})
