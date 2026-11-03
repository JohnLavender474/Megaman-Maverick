package com.megaman.maverick.game.world.collisions

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.camera.CameraView
import com.maverick.game.engine.common.enums.ProcessState
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.systems.GameSystem
import com.maverick.game.engine.world.WorldSystem
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.maverick.game.engine.world.body.IFixture
import com.maverick.game.engine.world.container.SimpleGridWorldContainer
import com.maverick.game.engine.world.contacts.ContactRuleDispatcher
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.blocks.GroundSlope
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.state.PlayPhase
import com.megaman.maverick.game.entities.state.PlayPhaseComponent
import com.megaman.maverick.game.players.MegaPlayers
import com.megaman.maverick.game.players.WorldGravity
import com.megaman.maverick.game.state.GameMode
import com.megaman.maverick.game.testServices
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.Receivers
import com.megaman.maverick.game.world.body.hasAnyFeetBlock
import com.megaman.maverick.game.world.body.setReceiver
import com.megaman.maverick.game.world.contacts.MegaContactFilter
import com.megaman.maverick.game.world.contacts.megaContactRules
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlin.math.max

private const val PPM = ConstVals.PPM.toFloat()
private const val STEP = 1f / 150f

private class Walker(services: MegaServices) : MegaGameEntity(services), IBodyEntity {

    var speedX = 0f
    var sliding = false
    val overGroundSet = ObjectSet<IFixture>()
    val overGround get() = !overGroundSet.isEmpty

    override fun init(vararg params: Any) {
        super.init()
        val body = Body(BodyType.DYNAMIC)
        body.setSize(PPM, 1.5f * PPM)
        body.physics.gravityOn = true
        body.physics.applyFrictionY = true
        body.physics.defaultFrictionOnSelf.set(ConstVals.STANDARD_RESISTANCE_X, ConstVals.STANDARD_RESISTANCE_Y)

        val feet = Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.5f * PPM, 0.25f * PPM))
        feet.offsetFromBodyAttachment.y = -0.75f * PPM
        body.addFixture(feet)

        val gravityRect = GameRectangle().setSize(0.9f * PPM, 0.1f * PPM)
        val gravityFixture = Fixture(body, FixtureType.CONSUMER, gravityRect)
        gravityFixture.offsetFromBodyAttachment.y = -0.75f * PPM
        gravityFixture.setReceiver(Receivers.FILTER) { it.getType() == FixtureType.BLOCK }
        gravityFixture.setReceiver(Receivers.CONSUMER) { state, fixture ->
            if (state == ProcessState.END) overGroundSet.remove(fixture) else overGroundSet.add(fixture)
        }
        body.addFixture(gravityFixture)

        body.preProcess.put(ConstKeys.GRAVITY) {
            if (sliding && !overGround) sliding = false
            body.setSize(PPM, (if (sliding) 0.9f else 1.5f) * PPM)
            feet.offsetFromBodyAttachment.y = -body.getHeight() / 2f
            gravityFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
            body.physics.velocity.x = speedX
            if (sliding) {
                body.physics.velocity.y = 0f
                body.physics.gravity.y = 0f
            } else body.physics.gravity.y = (if (overGround) -0.15f else -75f) * PPM
        }
        addComponent(BodyComponentCreator.create(this, body))
        addComponent(PlayPhaseComponent(PlayPhase.PRE_PLAY))
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        overGroundSet.clear()
        body.setBottomCenterToPoint(spawnProps.get(ConstKeys.POSITION, Vector2::class)!!)
    }

    override fun getType() = EntityType.SPECIAL
}

class GroundSlopeWalkTest : DescribeSpec({

    lateinit var engine: GameEngine
    lateinit var services: MegaServices
    lateinit var uphill: GroundSlope

    beforeEach {
        val players = MegaPlayers(2)
        val gravity = WorldGravity(players)
        val container = SimpleGridWorldContainer(ConstVals.PPM)
        val world = WorldSystem(
            ppm = ConstVals.PPM,
            fixedStep = STEP,
            worldContainerSupplier = { container },
            contactListener = ContactRuleDispatcher(
                megaContactRules(players, gravity, GameMode.COOP.rules) { _, _ -> false }
            ),
            collisionHandler = MegaCollisionHandler(),
            contactFilter = MegaContactFilter()
        )
        engine = GameEngine(Array<GameSystem>().apply { add(world) })
        val camera = OrthographicCamera().apply {
            viewportWidth = 200f * PPM
            viewportHeight = 200f * PPM
        }
        services = testServices(engine = engine, players = players, gravity = gravity, gameCamera = CameraView(camera))
        MegaEntityFactory.init(services)

        fun rect(x: Float, y: Float, w: Float, h: Float) = GameRectangle(x * PPM, y * PPM, w * PPM, h * PPM)
        Block(services).spawn(props(ConstKeys.BOUNDS pairTo rect(0f, -3f, 36f, 3f)))
        Block(services).spawn(props(ConstKeys.BOUNDS pairTo rect(17f, 0f, 6f, 2f)))
        uphill = GroundSlope(services)
        uphill.spawn(props(ConstKeys.BOUNDS pairTo rect(9f, 0f, 8f, 2f), ConstKeys.SIDE pairTo ConstKeys.RIGHT))
        GroundSlope(services).spawn(
            props(ConstKeys.BOUNDS pairTo rect(23f, 0f, 8f, 2f), ConstKeys.SIDE pairTo ConstKeys.LEFT)
        )
    }

    afterEach {
        engine.reset()
        MegaEntityFactory.reset()
    }

    fun spawnWalker(x: Float, y: Float = 0f): Walker {
        val walker = Walker(services)
        walker.spawn(props(ConstKeys.POSITION pairTo Vector2(x * PPM, y * PPM)))
        repeat(20) { engine.update(STEP) }
        return walker
    }

    fun Walker.centerX() = body.getX() + body.getWidth() / 2f

    fun Walker.heightOverSlope() = (body.getY() - uphill.surfaceAt(centerX())) / PPM

    fun walk(fromX: Float, toX: Float, slide: Boolean = false, speed: Float = 6f): List<String> {
        val walker = spawnWalker(fromX)
        walker.speedX = (if (toX > fromX) speed else -speed) * PPM
        walker.sliding = slide
        val events = ArrayList<String>()
        var wasOnFeet = true
        while (if (toX > fromX) walker.centerX() < toX * PPM else walker.centerX() > toX * PPM) {
            engine.update(STEP)
            val onFeet = walker.body.hasAnyFeetBlock()
            val x = walker.centerX() / PPM
            val y = walker.body.getY() / PPM
            if (slide && !walker.sliding) {
                events.add("slide ended at x=$x y=$y")
                walker.sliding = true
            }
            if (!walker.overGround) events.add("not over ground at x=$x y=$y")
            if (wasOnFeet && !onFeet) events.add("feet contact lost at x=$x y=$y")
            wasOnFeet = onFeet
        }
        return events
    }

    describe("a Megaman-sized walker in the shallow room layout") {
        it("stays over ground walking right") { walk(4f, 33f).shouldBeEmpty() }
        it("stays over ground walking left") { walk(33f, 4f).shouldBeEmpty() }
        it("stays over ground sliding right") { walk(4f, 33f, slide = true, speed = 10f).shouldBeEmpty() }
        it("stays over ground sliding left") { walk(33f, 4f, slide = true, speed = 10f).shouldBeEmpty() }

        it("leaves the slope on a jump") {
            val walker = spawnWalker(13f, 1f)
            walker.heightOverSlope() shouldBe (0f plusOrMinus 0.01f)

            walker.body.physics.velocity.y = 25f * PPM
            repeat(15) { engine.update(STEP) }

            walker.heightOverSlope() shouldBeGreaterThan 1f
        }

        it("leaves the slope on a knockback") {
            val walker = spawnWalker(13f, 1f)

            walker.body.physics.velocity.set(-8f * PPM, 5f * PPM)
            var highest = 0f
            repeat(15) {
                engine.update(STEP)
                highest = max(highest, walker.heightOverSlope())
            }

            highest shouldBeGreaterThan 0.15f
        }

        it("does not pull down a body with gravity off") {
            val walker = spawnWalker(13f, 1f)
            walker.body.physics.gravityOn = false
            walker.body.physics.velocity.setZero()
            walker.body.translate(0f, 2f)
            val y = walker.body.getY()

            repeat(30) { engine.update(STEP) }

            walker.body.getY() shouldBe y
        }
    }
})
