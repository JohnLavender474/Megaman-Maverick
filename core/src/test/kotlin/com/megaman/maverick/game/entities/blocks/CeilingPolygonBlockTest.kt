package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.math.Polygon
import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.ProcessState
import com.maverick.game.engine.common.extensions.objectSetOf
import com.maverick.game.engine.common.interfaces.IDirectional
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GamePolygon
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.state.MapObjectComponent
import com.megaman.maverick.game.testServices
import com.megaman.maverick.game.world.body.BodyLabel
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.FixtureLabel
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.Receivers
import com.megaman.maverick.game.world.body.hasBodyLabel
import com.megaman.maverick.game.world.body.hasFixtureLabel
import com.megaman.maverick.game.world.body.isSensing
import com.megaman.maverick.game.world.body.setBodySense
import com.megaman.maverick.game.world.body.setReceiver
import com.megaman.maverick.game.world.collisions.InvalidCeilingLabelException
import com.megaman.maverick.game.world.collisions.MegaCollisionHandler
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class CeilingPolygonBlockTest : DescribeSpec({

    val handler = MegaCollisionHandler()
    lateinit var block: CeilingPolygonBlock
    lateinit var dynamicBody: Body
    lateinit var headFixture: Fixture
    lateinit var entity: MegaGameEntity
    val headStates = ArrayList<ProcessState>()

    fun tiledPolygon(x: Float, y: Float, vararg points: Float): GamePolygon {
        val polygon = Polygon(points)
        polygon.setPosition(x, y)
        return GamePolygon(polygon)
    }

    fun spawn(polygon: GamePolygon, vararg extra: Pair<String, Any>) {
        val spawnProps = props(ConstKeys.POLYGON pairTo polygon, ConstKeys.ID pairTo 7)
        extra.forEach { spawnProps.put(it.first, it.second) }
        block.onSpawn(spawnProps)
    }

    fun vRow() = tiledPolygon(100f, 200f, 0f, 32f, 32f, 0f, 64f, 32f)

    fun step(serial: Int) {
        dynamicBody.beginStep(serial)
        block.body.beginStep(serial)
    }

    fun endStep() = block.body.postProcess()

    beforeEach {
        block = CeilingPolygonBlock(testServices(engine = mockk<GameEngine>(relaxed = true)))
        block.init()

        entity = mockk<MegaGameEntity>(relaxed = true, moreInterfaces = arrayOf(IBodyEntity::class))
        every { entity.getComponent(MapObjectComponent::class) } returns null
        every { entity.existenceState.liveNow } returns true
        dynamicBody = Body(BodyType.DYNAMIC, 116f, 150f, 32f, 48f)
        every { (entity as IBodyEntity).body } returns dynamicBody
        dynamicBody.putProperty(ConstKeys.ENTITY, entity)

        headFixture = Fixture(dynamicBody, FixtureType.HEAD, GameRectangle().setSize(8f, 3.2f))
        headFixture.offsetFromBodyAttachment.y = 24f
        headFixture.putProperty(ConstKeys.ENTITY, entity)
        dynamicBody.addFixture(headFixture)

        headStates.clear()
        headFixture.setReceiver(Receivers.HIT_BY_BLOCK, ProcessState.BEGIN) { _, _ -> headStates.add(ProcessState.BEGIN) }
        headFixture.setReceiver(Receivers.HIT_BY_BLOCK, ProcessState.CONTINUE) { _, _ ->
            headStates.add(ProcessState.CONTINUE)
        }
        headFixture.setReceiver(Receivers.HIT_BY_BLOCK, ProcessState.END) { _, _ -> headStates.add(ProcessState.END) }
    }

    afterEach { MegaGameEntities.remove(block) }

    describe("spawn") {
        it("takes the polygon bounds as its body and labels it a ceiling") {
            spawn(vRow())

            block.body.getX() shouldBe 100f
            block.body.getY() shouldBe 200f
            block.body.getWidth() shouldBe 64f
            block.body.getHeight() shouldBe 32f
            block.body.hasBodyLabel(BodyLabel.CEILING) shouldBe true
        }

        it("gives the block fixture no side contact") {
            spawn(vRow())

            block.blockFixture.hasFixtureLabel(FixtureLabel.NO_SIDE_TOUCHIE) shouldBe true
        }

        it("puts the block fixture on the polygon") {
            spawn(vRow())

            val shape = block.blockFixture.getShape() as GamePolygon
            shape.contains(Vector2(132f, 210f)) shouldBe true
            shape.contains(Vector2(104f, 204f)) shouldBe false
        }

        it("fails fast on a map object that is not a polygon") {
            shouldThrow<InvalidCeilingPolygonException> {
                block.onSpawn(props(ConstKeys.BOUNDS pairTo GameRectangle(0f, 0f, 32f, 32f)))
            }
        }

        it("fails fast on a rotated polygon") {
            shouldThrow<InvalidCeilingPolygonException> { spawn(vRow(), ConstKeys.ROTATION to 90f) }
        }

        it("fails fast on an edge that is not flat or at 45 degrees") {
            shouldThrow<InvalidCeilingPolygonException> { spawn(tiledPolygon(0f, 0f, 0f, 32f, 32f, 0f, 96f, 32f)) }
        }

        it("fails fast on a vertical edge") {
            shouldThrow<InvalidCeilingPolygonException> {
                spawn(tiledPolygon(0f, 0f, 0f, 0f, 0f, 32f, 32f, 64f, 64f, 64f, 64f, 0f))
            }
        }

        it("fails fast when the gravity is not up") {
            val services = testServices(engine = mockk<GameEngine>(relaxed = true))
            services.gravity.set(Direction.LEFT)
            val sideways = CeilingPolygonBlock(services)
            sideways.init()

            try {
                shouldThrow<CeilingPolygonGravityException> {
                    sideways.onSpawn(props(ConstKeys.POLYGON pairTo vRow(), ConstKeys.ID pairTo 7))
                }
            } finally {
                MegaGameEntities.remove(sideways)
            }
        }

        it("fails fast on a polygon that is not convex") {
            shouldThrow<InvalidCeilingPolygonException> {
                spawn(tiledPolygon(0f, 0f, 0f, 32f, 32f, 0f, 64f, 32f, 96f, 0f, 128f, 32f))
            }
        }
    }

    describe("push-out") {
        beforeEach {
            spawn(vRow())
            dynamicBody.beginStep(0)
        }

        it("pushes a rising body down to the face and stops the rise") {
            dynamicBody.setY(170f)
            dynamicBody.physics.velocity.y = 10f

            handler.handleCollision(block.body, dynamicBody) shouldBe true

            dynamicBody.getMaxY() shouldBe 200f
            dynamicBody.physics.velocity.y shouldBe 0f
        }

        it("stops a body beside the tip on the face, above the tip line") {
            dynamicBody.setX(136f)
            dynamicBody.setWidth(20f)
            dynamicBody.setMaxY(230f)

            handler.handleCollision(block.body, dynamicBody) shouldBe true

            dynamicBody.getMaxY() shouldBe 204f
        }

        it("does not move a body in the empty triangle beside the tip") {
            dynamicBody.setPosition(84f, 160f)
            dynamicBody.setSize(20f, 50f)

            handler.handleCollision(block.body, dynamicBody) shouldBe true

            dynamicBody.getMaxY() shouldBe 210f
            dynamicBody.getX() shouldBe 84f
        }

        it("does not move a body above the ceiling") {
            dynamicBody.setY(225f)

            handler.handleCollision(block.body, dynamicBody) shouldBe true

            dynamicBody.getY() shouldBe 225f
        }

        it("pushes a grounded body sideways out of the face, with no side sense") {
            dynamicBody.setPosition(90f, 160f)
            dynamicBody.setSize(32f, 60f)
            dynamicBody.setBodySense(BodySense.FEET_ON_GROUND, true)

            handler.handleCollision(block.body, dynamicBody) shouldBe true

            dynamicBody.getMaxX() shouldBe 112f
            dynamicBody.getY() shouldBe 160f
            dynamicBody.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT) shouldBe false
        }

        it("does not push down a body that slides down beside the face, and never pulls it in") {
            dynamicBody.setPosition(68f, 200f)
            dynamicBody.beginStep(0)
            dynamicBody.setY(190f)
            dynamicBody.physics.velocity.y = -5f

            handler.handleCollision(block.body, dynamicBody) shouldBe true

            dynamicBody.getY() shouldBe 190f
            dynamicBody.getX() shouldBe 68f
            dynamicBody.physics.velocity.y shouldBe -5f
            dynamicBody.isSensing(BodySense.HEAD_TOUCHING_BLOCK) shouldBe false
        }

        it("pushes a body that comes down into the face sideways out of it") {
            dynamicBody.setPosition(70f, 200f)
            dynamicBody.beginStep(0)
            dynamicBody.setY(190f)

            handler.handleCollision(block.body, dynamicBody) shouldBe true

            dynamicBody.getMaxX() shouldBe 100f
            dynamicBody.getY() shouldBe 190f
        }

        it("gives no response to a body whose gravity is not up, and never the rectangle push") {
            val turned = mockk<MegaGameEntity>(relaxed = true, moreInterfaces = arrayOf(IDirectional::class))
            every { (turned as IDirectional).direction } returns Direction.DOWN
            dynamicBody.putProperty(ConstKeys.ENTITY, turned)
            dynamicBody.setY(170f)

            handler.handleCollision(block.body, dynamicBody) shouldBe true

            dynamicBody.getY() shouldBe 170f
            dynamicBody.getX() shouldBe 116f
        }

        it("fails fast on a ceiling label on another block") {
            val body = Body(BodyType.STATIC, 0f, 0f, 32f, 32f)
            val other = mockk<Block>(relaxed = true)
            every { other.getComponent(MapObjectComponent::class) } returns null
            body.putProperty(ConstKeys.ENTITY, other)
            body.putProperty(ConstKeys.BODY_LABELS, objectSetOf(BodyLabel.CEILING))

            shouldThrow<InvalidCeilingLabelException> { handler.handleCollision(body, dynamicBody) }
        }
    }

    describe("head contact") {
        beforeEach { spawn(vRow()) }

        it("begins with the sense, the receiver and no rise") {
            step(1)
            dynamicBody.setY(170f)
            dynamicBody.physics.velocity.y = 10f

            handler.handleCollision(block.body, dynamicBody)

            dynamicBody.isSensing(BodySense.HEAD_TOUCHING_BLOCK) shouldBe true
            dynamicBody.physics.velocity.y shouldBe 0f
            headStates shouldBe listOf(ProcessState.BEGIN)
        }

        it("reports once per step") {
            step(1)
            dynamicBody.setY(170f)
            handler.handleCollision(block.body, dynamicBody)
            handler.handleCollision(block.body, dynamicBody)
            endStep()

            headStates shouldBe listOf(ProcessState.BEGIN)
        }

        it("continues while the body stays flush under the tip with no push") {
            step(1)
            dynamicBody.setY(170f)
            handler.handleCollision(block.body, dynamicBody)
            endStep()

            step(2)
            endStep()

            dynamicBody.isSensing(BodySense.HEAD_TOUCHING_BLOCK) shouldBe true
            headStates shouldBe listOf(ProcessState.BEGIN, ProcessState.CONTINUE)
        }

        it("ends when the head leaves the face") {
            step(1)
            dynamicBody.setY(170f)
            handler.handleCollision(block.body, dynamicBody)
            endStep()

            step(2)
            dynamicBody.setY(140f)
            endStep()

            dynamicBody.isSensing(BodySense.HEAD_TOUCHING_BLOCK) shouldBe false
            headStates shouldBe listOf(ProcessState.BEGIN, ProcessState.END)
        }

        it("does not touch the head of a body far below") {
            step(1)
            dynamicBody.setY(100f)
            handler.handleCollision(block.body, dynamicBody)
            endStep()

            dynamicBody.isSensing(BodySense.HEAD_TOUCHING_BLOCK) shouldBe false
            headStates shouldBe emptyList()
        }

        it("drops the contact of a dead entity with no call") {
            step(1)
            dynamicBody.setY(170f)
            handler.handleCollision(block.body, dynamicBody)
            endStep()
            every { entity.existenceState.liveNow } returns false

            step(2)
            dynamicBody.setY(100f)
            endStep()

            headStates shouldBe listOf(ProcessState.BEGIN)
        }

        it("ends the contact when the block is destroyed") {
            step(1)
            dynamicBody.setY(170f)
            handler.handleCollision(block.body, dynamicBody)

            block.onDestroy()

            dynamicBody.isSensing(BodySense.HEAD_TOUCHING_BLOCK) shouldBe false
            headStates shouldBe listOf(ProcessState.BEGIN, ProcessState.END)
        }
    }
})
