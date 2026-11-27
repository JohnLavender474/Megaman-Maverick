package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.camera.CameraView
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.cullables.CullablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.testServices
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll

private const val PPM = ConstVals.PPM.toFloat()
private const val CULL_WAIT = 2f

class LavaDropCullTest : DescribeSpec({

    lateinit var engine: GameEngine
    lateinit var services: MegaServices
    lateinit var camera: OrthographicCamera

    beforeEach {
        engine = GameEngine()
        camera = OrthographicCamera().apply {
            viewportWidth = 16f * PPM
            viewportHeight = 14f * PPM
            position.set(8f * PPM, 7f * PPM, 0f)
        }
        services = testServices(engine = engine, gameCamera = CameraView(camera))
        val atlas = mockk<TextureAtlas>()
        every { atlas.findRegion(any()) } returns AtlasRegion(mockk<Texture>(relaxed = true), 0, 0, 96, 32)
        every { services.assets.get(any<String>(), TextureAtlas::class.java) } returns atlas
    }

    afterEach {
        engine.reset()
        unmockkAll()
    }

    fun LavaDrop.spawnAtTile(xTiles: Float, yTiles: Float) {
        spawn(props(ConstKeys.POSITION pairTo Vector2(xTiles * PPM, yTiles * PPM))) shouldBe true
        engine.update(0f)
    }

    fun LavaDrop.culled(delta: Float) =
        getComponent(CullablesComponent::class)!!.cullables.values().any { it.shouldBeCulled(delta) }

    describe("a LavaDrop") {
        it("is culled after it fell out of the camera") {
            val drop = LavaDrop(services)
            drop.spawnAtTile(xTiles = 8f, yTiles = 7f)
            drop.setToFall()
            drop.culled(CULL_WAIT) shouldBe false

            camera.position.set(8f * PPM, 40f * PPM, 0f)
            drop.culled(CULL_WAIT) shouldBe true
        }

        it("is not culled while it hangs under its supplier, even off camera") {
            val drop = LavaDrop(services)
            drop.spawnAtTile(xTiles = 8f, yTiles = 7f)

            camera.position.set(8f * PPM, 40f * PPM, 0f)
            drop.culled(CULL_WAIT) shouldBe false
        }

        it("does not keep the camera cull of its last life after it is spawned again") {
            val drop = LavaDrop(services)
            drop.spawnAtTile(xTiles = 8f, yTiles = 7f)
            drop.setToFall()
            drop.destroy()
            engine.update(0f)

            drop.spawnAtTile(xTiles = 8f, yTiles = 7f)
            camera.position.set(8f * PPM, 40f * PPM, 0f)
            drop.culled(CULL_WAIT) shouldBe false
        }
    }
})
