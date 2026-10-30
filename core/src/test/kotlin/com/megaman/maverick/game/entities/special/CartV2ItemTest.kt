package com.megaman.maverick.game.entities.special

import com.megaman.maverick.game.testServices
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.systems.GameSystem
import com.maverick.game.engine.world.WorldSystem
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.container.SimpleGridWorldContainer
import com.maverick.game.engine.world.contacts.ContactRuleDispatcher
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.items.HealthBulb
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.levels.LevelState
import com.megaman.maverick.game.state.GameState
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.collisions.MegaCollisionHandler
import com.megaman.maverick.game.world.contacts.MegaContactFilter
import com.megaman.maverick.game.world.contacts.megaContactRules
import com.megaman.maverick.game.players.MegaPlayers
import com.megaman.maverick.game.players.WorldGravity
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

private const val PPM = ConstVals.PPM.toFloat()
private const val STEP = 1f / 150f
private const val CART_SPEED = 4f * PPM

class CartV2ItemTest : DescribeSpec({

    lateinit var engine: GameEngine
    lateinit var services: MegaServices
    lateinit var cart: CartV2

    class Crate(services: MegaServices) : MegaGameEntity(services), IBodyEntity {

        override fun init(vararg params: Any) {
            super.init()
            val body = Body(BodyType.DYNAMIC)
            body.setSize(0.5f * PPM, 0.5f * PPM)
            addComponent(BodyComponentCreator.create(this, body))
        }

        override fun onSpawn(spawnProps: com.maverick.game.engine.common.objects.Properties) {
            super.onSpawn(spawnProps)
            body.setBottomCenterToPoint(spawnProps.get(ConstKeys.POSITION, Vector2::class)!!)
        }

        override fun getType() = EntityType.SPECIAL

        override fun getTag() = "Crate"
    }

    fun step(frames: Int, cartSpeed: Float) = repeat(frames) {
        cart.body.physics.velocity.x = cartSpeed
        engine.update(STEP)
    }

    beforeEach {
        val players = MegaPlayers()
        val gravity = WorldGravity(players)
        val worldContainer = SimpleGridWorldContainer(ConstVals.PPM)
        val worldSystem = WorldSystem(
            ppm = ConstVals.PPM,
            fixedStep = STEP,
            worldContainerSupplier = { worldContainer },
            contactListener = ContactRuleDispatcher(megaContactRules(players, gravity) { _, _ -> false }),
            collisionHandler = MegaCollisionHandler(),
            contactFilter = MegaContactFilter()
        )
        engine = GameEngine(Array<GameSystem>().apply { add(worldSystem) })
        services = testServices(engine = engine, players = players, gravity = gravity)
        val atlas = mockk<TextureAtlas>()
        every { atlas.findRegion(any()) } returns AtlasRegion(mockk<Texture>(relaxed = true), 0, 0, 64, 64)
        every { services.assets.get(any<String>(), TextureAtlas::class.java) } returns atlas

        Block(services).spawn(
            props(ConstKeys.BOUNDS pairTo GameRectangle(-10f * PPM, -PPM, 40f * PPM, PPM))
        ) shouldBe true

        cart = CartV2(services)
        cart.spawn(
            props(
                ConstKeys.BOUNDS pairTo GameRectangle(2f * PPM, 0f, 2f * PPM, PPM),
                ConstKeys.FACING pairTo Facing.RIGHT.name
            )
        ) shouldBe true

        engine.update(STEP)
        step(30, 0f)
    }

    afterEach { engine.reset() }

    fun spawnBulb(x: Float, y: Float): HealthBulb {
        val bulb = HealthBulb(services)
        bulb.spawn(
            props(
                ConstKeys.POSITION pairTo Vector2(x, y),
                ConstKeys.DIRECTION pairTo Direction.UP.name,
                ConstKeys.LARGE pairTo false,
                ConstKeys.TIMED pairTo false
            )
        ) shouldBe true
        step(30, 0f)
        return bulb
    }

    describe("a moving CartV2") {
        it("passes over an item that lies on the ground in front of it and does not move the item") {
            val bulb = spawnBulb(8f * PPM, 0f)
            val startX = bulb.body.getX()
            val startY = bulb.body.getY()

            step(300, CART_SPEED)

            cart.body.getX() shouldBeGreaterThan 8f * PPM
            bulb.body.getX() shouldBe startX
            bulb.body.getY() shouldBe (startY plusOrMinus 0.01f)
        }

        it("carries an item that rests on top of it") {
            val bulb = spawnBulb(3f * PPM, PPM)
            val startX = bulb.body.getX()
            val cartStartX = cart.body.getX()

            step(75, CART_SPEED)

            val cartMoved = cart.body.getX() - cartStartX
            cartMoved shouldBeGreaterThan 0.5f * PPM
            (bulb.body.getX() - startX) shouldBe (cartMoved plusOrMinus 0.05f * PPM)
            bulb.body.getY() shouldBe (PPM plusOrMinus 0.1f * PPM)
        }

        it("still pushes another dynamic body that is in front of it") {
            val crate = Crate(services)
            crate.spawn(props(ConstKeys.POSITION pairTo Vector2(6f * PPM, 0f))) shouldBe true
            step(1, 0f)
            val startX = crate.body.getX()

            step(150, CART_SPEED)

            crate.body.getX() shouldBeGreaterThan startX + PPM
            crate.body.getX() shouldBe (cart.body.getMaxX() plusOrMinus 0.01f * PPM)
        }
    }
})
