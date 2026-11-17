package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.motion.MotionComponent
import com.maverick.game.engine.motion.Pendulum
import com.maverick.game.engine.motion.RotatingLine
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.screens.levels.spawns.SpawnType
import com.megaman.maverick.game.testServices
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll

private const val PPM = ConstVals.PPM.toFloat()

class SawTest : DescribeSpec({

    lateinit var engine: GameEngine
    lateinit var services: MegaServices

    beforeEach {
        engine = GameEngine()
        services = testServices(engine = engine)
        val atlas = mockk<TextureAtlas>()
        every { atlas.findRegion(any()) } returns AtlasRegion(mockk<Texture>(relaxed = true), 0, 0, 64, 64)
        every { services.assets.get(any<String>(), TextureAtlas::class.java) } returns atlas
    }

    afterEach {
        engine.reset()
        unmockkAll()
    }

    fun Saw.spawnAs(type: String, xTiles: Float, vararg extra: Pair<Any, Any?>) {
        val spawnProps = props(
            ConstKeys.BOUNDS pairTo GameRectangle(xTiles * PPM, 10f * PPM, PPM, PPM),
            ConstKeys.TYPE pairTo type,
            SpawnType.SPAWN_ROOM pairTo "room"
        )
        extra.forEach { (key, value) -> spawnProps.put(key, value) }
        spawn(spawnProps) shouldBe true
        engine.update(0f)
    }

    fun Saw.respawnAs(type: String, xTiles: Float, vararg extra: Pair<Any, Any?>) {
        destroy()
        engine.update(0f)
        spawnAs(type, xTiles, *extra)
    }

    fun Saw.definitions() = getComponent(MotionComponent::class)!!.definitions

    fun Saw.debugShapes() = getComponent(DrawableShapesComponent::class)!!.debugShapeSuppliers.map { it() }

    fun Saw.ringsHidden(): List<Boolean> {
        val component = getComponent(SpritesComponent::class)!!
        component.preProcess(0f)
        return (0..6).map { component.sprites["ring_$it"]!!.hidden }
    }

    describe("a pooled Saw") {
        it("reuses its motions, its motion definitions and its debug shapes when the spawn type changes") {
            val saw = Saw(services)

            saw.spawnAs(Saw.PENDULUM_TYPE, xTiles = 5f, ConstKeys.LENGTH to 2f)
            val pendulumDefinition = saw.definitions()[ConstKeys.PENDULUM]!!
            val pendulum = pendulumDefinition.motion as Pendulum
            val anchor = pendulum.anchor
            anchor.x shouldBe 5.5f * PPM
            pendulum.length shouldBe 2f * PPM
            val shapes = saw.debugShapes()
            shapes.forEach { it.shouldNotBeNull() }
            saw.ringsHidden() shouldContainExactly List(7) { false }

            saw.respawnAs(Saw.ROTATION_TYPE, xTiles = 8f, ConstKeys.SPEED to 4f)
            saw.definitions()[ConstKeys.PENDULUM].shouldBeNull()
            val rotationDefinition = saw.definitions()[ConstKeys.ROTATION]!!
            val rotation = rotationDefinition.motion as RotatingLine
            rotation.getOrigin(Vector2()) shouldBe Vector2(8.5f * PPM, 10.5f * PPM)
            rotation.speed shouldBe 4f * PPM

            saw.respawnAs(Saw.TRAJECTORY_TYPE, xTiles = 2f, ConstKeys.TRAJECTORY to "1,0,1")
            val trajectoryDefinition = saw.definitions()[ConstKeys.TRAJECTORY]!!
            saw.debugShapes().forEach { it.shouldBeNull() }
            saw.ringsHidden() shouldContainExactly List(7) { true }

            saw.respawnAs(Saw.TRAJECTORY_TYPE, xTiles = 3f, ConstKeys.TRAJECTORY to "0,1,1")
            saw.definitions()[ConstKeys.TRAJECTORY] shouldBeSameInstanceAs trajectoryDefinition

            saw.respawnAs(Saw.PENDULUM_TYPE, xTiles = 20f)
            saw.definitions()[ConstKeys.PENDULUM] shouldBeSameInstanceAs pendulumDefinition
            pendulum.anchor shouldBeSameInstanceAs anchor
            anchor.x shouldBe 20.5f * PPM
            pendulum.length shouldBe 3f * PPM
            pendulum.angle shouldBe pendulum.defaultAngle
            saw.debugShapes().zip(shapes).forEach { (now, before) -> now shouldBeSameInstanceAs before }

            saw.respawnAs(Saw.ROTATION_TYPE, xTiles = 1f)
            saw.definitions()[ConstKeys.ROTATION] shouldBeSameInstanceAs rotationDefinition
            rotation.speed shouldBe 2f * PPM
        }
    }
})
