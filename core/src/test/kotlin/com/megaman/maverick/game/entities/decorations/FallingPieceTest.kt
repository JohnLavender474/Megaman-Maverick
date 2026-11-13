package com.megaman.maverick.game.entities.decorations

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
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.systems.GameSystem
import com.maverick.game.engine.updatables.UpdatablesSystem
import com.maverick.game.engine.world.WorldSystem
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.container.SimpleGridWorldContainer
import com.maverick.game.engine.world.contacts.ContactRuleDispatcher
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.players.MegaPlayers
import com.megaman.maverick.game.players.WorldGravity
import com.megaman.maverick.game.state.GameMode
import com.megaman.maverick.game.testServices
import com.megaman.maverick.game.world.collisions.MegaCollisionHandler
import com.megaman.maverick.game.world.contacts.MegaContactFilter
import com.megaman.maverick.game.world.contacts.megaContactRules
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.mockk
import kotlin.math.abs

private const val PPM = ConstVals.PPM.toFloat()
private const val DT = 1f / 60f
private const val DEBRIS_KEY = "FallingPieceTest/debris"
private const val REGION_WIDTH = 10
private const val REGION_HEIGHT = 6

class FallingPieceTest : DescribeSpec({

    lateinit var engine: GameEngine
    lateinit var services: MegaServices

    beforeEach {
        val players = MegaPlayers(2)
        val gravity = WorldGravity(players)
        val container = SimpleGridWorldContainer(ConstVals.PPM)
        val world = WorldSystem(
            ppm = ConstVals.PPM,
            fixedStep = 1f / 150f,
            worldContainerSupplier = { container },
            contactListener = ContactRuleDispatcher(megaContactRules(players, gravity, GameMode.SINGLE.rules) { _, _ -> false }),
            collisionHandler = MegaCollisionHandler(),
            contactFilter = MegaContactFilter()
        )
        engine = GameEngine(Array<GameSystem>().apply {
            add(world)
            add(UpdatablesSystem())
        })
        val camera = OrthographicCamera().apply {
            viewportWidth = 400f * PPM
            viewportHeight = 400f * PPM
        }
        services = testServices(engine = engine, players = players, gravity = gravity, gameCamera = CameraView(camera))
        val texture = mockk<Texture>(relaxed = true)
        every { texture.width } returns 1024
        every { texture.height } returns 1024
        val atlas = mockk<TextureAtlas>()
        every { atlas.findRegion(any()) } answers {
            if (firstArg<String>().startsWith("Missing")) null else AtlasRegion(texture, 0, 0, REGION_WIDTH, REGION_HEIGHT)
        }
        every { services.assets.get(any<String>(), TextureAtlas::class.java) } returns atlas
        MegaEntityFactory.init(services)
        FallingPiece.load(services, arrayOf(DEBRIS_KEY), TextureAsset.ENEMIES_2)
    }

    afterEach {
        engine.reset()
        MegaEntityFactory.reset()
        val left = Array<MegaGameEntity>()
        MegaGameEntities.getOfType(EntityType.DECORATION).forEach { left.add(it) }
        left.forEach { MegaGameEntities.remove(it) }
    }

    fun FallingPiece.body(): Body = getComponent(BodyComponent::class)!!.body

    fun FallingPiece.sprite() = getComponent(SpritesComponent::class)!!.sprites.values().first() as GameSprite

    fun step(piece: FallingPiece, delta: Float = DT) {
        engine.update(delta)
        piece.getComponent(SpritesComponent::class)!!.preProcess(delta)
    }

    fun spawnWith(spawnProps: Properties): FallingPiece {
        val piece = MegaEntityFactory.fetch(FallingPiece::class)!!
        piece.spawn(spawnProps)
        step(piece, 0f)
        return piece
    }

    fun spawn(def: FallingPieceDef, key: String, position: Vector2 = Vector2(5f * PPM, 5f * PPM), velocity: Vector2 = Vector2(2f * PPM, 6f * PPM)) =
        spawnWith(
            props(
                ConstKeys.DEF pairTo def,
                ConstKeys.KEY pairTo key,
                ConstKeys.POSITION pairTo position,
                ConstKeys.TRAJECTORY pairTo velocity
            )
        )

    fun keyOf(def: FallingPieceDef) = when (def) {
        FallingPieceDef.DEBRIS -> DEBRIS_KEY
        FallingPieceDef.BLOCK -> BlockPieceColor.GOLD.regionKey
        FallingPieceDef.WOOD_CRATE -> FallingPiece.WOOD_CRATE_REGIONS[3]
    }

    fun expectedSize(def: FallingPieceDef) = when (def) {
        FallingPieceDef.DEBRIS -> REGION_WIDTH * 2f to REGION_HEIGHT * 2f
        FallingPieceDef.BLOCK -> PPM to PPM
        FallingPieceDef.WOOD_CRATE -> 6f to 6f
    }

    fun expectStartOf(piece: FallingPiece, def: FallingPieceDef) {
        val body = piece.body()
        body.physics.gravity.y shouldBe def.gravity * PPM
        body.physics.velocityClamp.y shouldBe (def.fallClamp?.times(PPM) ?: Float.MAX_VALUE)
        body.physics.applyFrictionX shouldBe def.frictionX
        body.physics.velocity shouldBe Vector2(2f * PPM, 6f * PPM)
        val center = body.getCenter(Vector2())
        (abs(center.x - 5f * PPM) <= def.jitter * PPM) shouldBe true
        (abs(center.y - 5f * PPM) <= def.jitter * PPM) shouldBe true
        piece.sprite().width to piece.sprite().height shouldBe expectedSize(def)
        piece.sprite().rotation shouldBe 0f
        piece.sprite().hidden shouldBe false
        piece.sprite().color.a shouldBe 1f
        piece.key shouldBe keyOf(def)
    }

    describe("a falling piece") {
        FallingPieceDef.entries.forEach { def ->
            it("takes the body and sprite values of $def at spawn") {
                expectStartOf(spawn(def, keyOf(def)), def)
            }

            it("is gone after 2 s and not before, as $def") {
                val piece = spawn(def, keyOf(def))
                repeat(118) { step(piece) }
                piece.existenceState.liveAfterUpdate shouldBe true
                repeat(5) { step(piece) }
                piece.existenceState.liveNow shouldBe false
            }
        }

        it("places block and wood crate pieces at the given point, and jitters debris by up to 0.2 tile") {
            spawn(FallingPieceDef.BLOCK, keyOf(FallingPieceDef.BLOCK)).body().getCenter(Vector2()) shouldBe Vector2(5f * PPM, 5f * PPM)
            spawn(FallingPieceDef.WOOD_CRATE, keyOf(FallingPieceDef.WOOD_CRATE)).body().getCenter(Vector2()) shouldBe Vector2(5f * PPM, 5f * PPM)
            val moved = (0 until 20).count {
                spawn(FallingPieceDef.DEBRIS, DEBRIS_KEY).body().getCenter(Vector2()) != Vector2(5f * PPM, 5f * PPM)
            }
            (moved > 0) shouldBe true
        }

        listOf(FallingPieceDef.BLOCK to 45f, FallingPieceDef.WOOD_CRATE to 90f).forEach { (def, turn) ->
            it("spins $def by $turn degrees each ${def.spin!!.delay} s from 0 degrees") {
                val piece = spawn(def, keyOf(def))
                var last = piece.sprite().rotation
                last shouldBe 0f
                var turns = 0
                repeat(60) {
                    step(piece)
                    val now = piece.sprite().rotation
                    if (now != last) {
                        (now - last) shouldBe turn
                        turns++
                    }
                    last = now
                }
                val most = (1f / def.spin!!.delay).toInt()
                withClue("turns in 1 s") { (turns in most * 3 / 4..most) shouldBe true }
            }
        }

        it("blinks debris on odd 0.033 s steps and never turns it") {
            val piece = spawn(FallingPieceDef.DEBRIS, DEBRIS_KEY)
            var elapsed = 0f
            var shown = 0
            var hidden = 0
            repeat(100) {
                step(piece)
                elapsed += DT
                piece.sprite().hidden shouldBe ((elapsed / 0.033f).toInt() % 2 == 1)
                piece.sprite().rotation shouldBe 0f
                if (piece.sprite().hidden) hidden++ else shown++
            }
            (hidden > 0 && shown > 0) shouldBe true
        }

        it("never hides block and wood crate pieces") {
            listOf(FallingPieceDef.BLOCK, FallingPieceDef.WOOD_CRATE).forEach { def ->
                val piece = spawn(def, keyOf(def))
                repeat(100) {
                    step(piece)
                    piece.sprite().hidden shouldBe false
                }
            }
        }

        it("keeps nothing from its last life when the pool reuses it as another kind") {
            val block = spawn(FallingPieceDef.BLOCK, keyOf(FallingPieceDef.BLOCK))
            repeat(123) { step(block) }
            (block.sprite().rotation > 0f) shouldBe true
            block.existenceState.liveNow shouldBe false

            val wood = spawn(FallingPieceDef.WOOD_CRATE, keyOf(FallingPieceDef.WOOD_CRATE))
            wood shouldBeSameInstanceAs block
            expectStartOf(wood, FallingPieceDef.WOOD_CRATE)
            repeat(123) { step(wood) }
            (wood.sprite().rotation > 0f) shouldBe true

            val debris = spawn(FallingPieceDef.DEBRIS, DEBRIS_KEY)
            debris shouldBeSameInstanceAs block
            expectStartOf(debris, FallingPieceDef.DEBRIS)
            repeat(30) {
                step(debris)
                debris.sprite().rotation shouldBe 0f
            }
            repeat(100) { step(debris) }

            val again = spawn(FallingPieceDef.BLOCK, keyOf(FallingPieceDef.BLOCK))
            again shouldBeSameInstanceAs block
            expectStartOf(again, FallingPieceDef.BLOCK)
        }

        it("takes one value from a spawn property over its def, and the rest from the def") {
            val piece = spawnWith(
                props(
                    ConstKeys.DEF pairTo FallingPieceDef.DEBRIS,
                    ConstKeys.KEY pairTo DEBRIS_KEY,
                    ConstKeys.POSITION pairTo Vector2(5f * PPM, 5f * PPM),
                    ConstKeys.TRAJECTORY pairTo Vector2(2f * PPM, 6f * PPM),
                    ConstKeys.GRAVITY pairTo -10f
                )
            )
            piece.body().physics.gravity.y shouldBe -10f * PPM
            piece.body().physics.velocityClamp.y shouldBe 12f * PPM
            piece.body().physics.applyFrictionX shouldBe false
            piece.sprite().width shouldBe REGION_WIDTH * 2f
        }

        it("takes a null spawn property as no value over its def") {
            val piece = spawnWith(
                props(
                    ConstKeys.DEF pairTo FallingPieceDef.DEBRIS,
                    ConstKeys.KEY pairTo DEBRIS_KEY,
                    ConstKeys.POSITION pairTo Vector2(5f * PPM, 5f * PPM),
                    ConstKeys.TRAJECTORY pairTo Vector2(2f * PPM, 6f * PPM),
                    ConstKeys.CLAMP pairTo null,
                    ConstKeys.BLINK pairTo null
                )
            )
            piece.body().physics.velocityClamp.y shouldBe Float.MAX_VALUE
            repeat(30) {
                step(piece)
                piece.sprite().hidden shouldBe false
            }
        }

        fun allValues() = props(
            ConstKeys.KEY pairTo DEBRIS_KEY,
            ConstKeys.POSITION pairTo Vector2(5f * PPM, 5f * PPM),
            ConstKeys.TRAJECTORY pairTo Vector2(2f * PPM, 6f * PPM),
            ConstKeys.GRAVITY pairTo -20f,
            ConstKeys.CLAMP pairTo 5f,
            ConstKeys.FRICTION_X pairTo false,
            ConstKeys.JITTER pairTo 0f,
            ConstKeys.SIZE pairTo FallingPieceSize.Tiles(0.5f),
            ConstKeys.SPIN pairTo FallingPieceSpin(step = 30f, delay = 0.2f),
            ConstKeys.BLINK pairTo null
        )

        it("takes every value from spawn properties when it has no def") {
            val piece = spawnWith(allValues())
            piece.body().physics.gravity.y shouldBe -20f * PPM
            piece.body().physics.velocityClamp.y shouldBe 5f * PPM
            piece.body().physics.applyFrictionX shouldBe false
            piece.body().getCenter(Vector2()) shouldBe Vector2(5f * PPM, 5f * PPM)
            piece.sprite().width shouldBe 0.5f * PPM
            var turns = 0
            var last = 0f
            repeat(60) {
                step(piece)
                piece.sprite().hidden shouldBe false
                if (piece.sprite().rotation != last) {
                    (piece.sprite().rotation - last) shouldBe 30f
                    turns++
                }
                last = piece.sprite().rotation
            }
            (turns in 4..5) shouldBe true
            piece.body().physics.velocity.y shouldBe (-5f * PPM plusOrMinus 0.001f)
        }

        listOf(
            ConstKeys.GRAVITY, ConstKeys.CLAMP, ConstKeys.FRICTION_X, ConstKeys.JITTER, ConstKeys.SIZE, ConstKeys.SPIN,
            ConstKeys.BLINK
        ).forEach { key ->
            it("fails at spawn when it has no def and no '$key' property") {
                val spawnProps = allValues()
                spawnProps.remove(key)
                MegaEntityFactory.fetch(FallingPiece::class)!!.spawn(spawnProps)
                val error = shouldThrow<IllegalArgumentException> { engine.update(0f) }
                (key in error.message!!) shouldBe true
            }
        }

        it("fails at spawn with a spawn property of the wrong type, and names the key") {
            val spawnProps = allValues()
            spawnProps.put(ConstKeys.GRAVITY, -20)
            MegaEntityFactory.fetch(FallingPiece::class)!!.spawn(spawnProps)
            val error = shouldThrow<IllegalArgumentException> { engine.update(0f) }
            (ConstKeys.GRAVITY in error.message!!) shouldBe true
        }

        it("fails at spawn with a region key that no one loaded") {
            MegaEntityFactory.fetch(FallingPiece::class)!!.spawn(
                props(
                    ConstKeys.DEF pairTo FallingPieceDef.DEBRIS,
                    ConstKeys.KEY pairTo "FallingPieceTest/never_loaded",
                    ConstKeys.POSITION pairTo Vector2(),
                    ConstKeys.TRAJECTORY pairTo Vector2()
                )
            )
            shouldThrow<IllegalStateException> { engine.update(0f) }
        }

        it("fails at load with a region that the atlas does not have") {
            shouldThrow<IllegalStateException> {
                FallingPiece.load(services, arrayOf("Missing/region"), TextureAsset.ENEMIES_2)
            }
        }

        it("throws X6 debris with a speed from the X6 tables for each key") {
            val xs = setOf(-5.6f, -3.75f, 2.8f, 4.7f, -6.6f, -4.7f, 3.75f, 5.6f).map { it * PPM }
            val ys = setOf(6.6f, 8.4f, 11.25f, 5.6f, 7.5f, 9.4f, 10.3f, 4.7f).map { it * PPM }
            val before = mutableSetOf<MegaGameEntity>()
            MegaGameEntities.getOfType(EntityType.DECORATION).forEach { before.add(it) }
            FallingPiece.throwDebris(arrayOf(DEBRIS_KEY, DEBRIS_KEY, DEBRIS_KEY), Vector2(5f * PPM, 5f * PPM))
            engine.update(0f)
            val thrown = mutableListOf<FallingPiece>()
            MegaGameEntities.getOfType(EntityType.DECORATION).forEach { if (it !in before) thrown.add(it as FallingPiece) }
            thrown.size shouldBe 3
            thrown.forEach { piece ->
                (piece.body().physics.velocity.x in xs) shouldBe true
                (piece.body().physics.velocity.y in ys) shouldBe true
                piece.body().physics.gravity.y shouldBe FallingPieceDef.DEBRIS.gravity * PPM
            }
        }
    }
})
