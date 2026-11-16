package com.megaman.maverick.game.entities.megaman.weapons

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.entities.EntityExistenceState.LIVE
import com.maverick.game.engine.entities.GameEntity
import com.maverick.game.engine.systems.GameSystem
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.updatables.UpdatablesSystem
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.megaman.constants.MegaChargeStatus
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.projectiles.MoonScythe
import com.megaman.maverick.game.testServices
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll

private const val FRAME = 1f / 60f

class MoonScythesThreeShotsTest : DescribeSpec({

    lateinit var engine: GameEngine
    lateinit var services: MegaServices
    lateinit var state: MoonScythesState
    var fireRequested = false
    var lastShot = listOf<MoonScythe>()

    fun fire() {
        state.maxWaitTime.reset()
        val shot = MegamanValues.MOON_SCYTHE_DEG_OFFSETS.toList().map {
            val scythe = MegaEntityFactory.fetch(MoonScythe::class)!!
            scythe.spawn(
                props(
                    ConstKeys.FADE pairTo false,
                    ConstKeys.TRAJECTORY pairTo Vector2(0f, 1f),
                    ConstKeys.POSITION pairTo Vector2()
                )
            )
            state.addSpawned(MegaChargeStatus.NOT_CHARGED, scythe)
            scythe
        }
        state.fadeExtraScythes()
        lastShot = shot
    }

    beforeEach {
        engine = GameEngine(Array<GameSystem>().apply { add(UpdatablesSystem()) })
        services = testServices(engine = engine)
        val atlas = mockk<TextureAtlas>()
        every { atlas.findRegion(any()) } returns AtlasRegion(mockk<Texture>(relaxed = true), 0, 0, 64, 64)
        every { services.assets.get(any<String>(), TextureAtlas::class.java) } returns atlas
        MegaEntityFactory.init(services)
        state = MoonScythesState(cooldown = 0.1f)
        fireRequested = false

        val holder = object : GameEntity(engine) {
            override fun onSpawn(spawnProps: Properties) {}
            override fun onDestroy() {}
        }
        holder.addComponent(UpdatablesComponent({ delta ->
            if (fireRequested) {
                fireRequested = false
                fire()
            }
            state.update(delta)
        }))
        holder.spawn(props())
        engine.update(FRAME)
    }

    afterEach {
        engine.reset()
        MegaEntityFactory.reset()
        unmockkAll()
    }

    fun runFrames(seconds: Float) = repeat((seconds / FRAME).toInt() + 1) { engine.update(FRAME) }

    fun shoot(): List<MoonScythe> {
        withClue("the weapon can fire") { state.canFire(MegaChargeStatus.NOT_CHARGED) shouldBe true }
        fireRequested = true
        engine.update(FRAME)
        engine.update(FRAME)
        lastShot.forEach { withClue("a new scythe is live after one frame") { it.existenceState shouldBe LIVE } }
        return lastShot
    }

    fun describe(scythes: List<MoonScythe>) = scythes.joinToString {
        "#${it.hashCode()} ${it.existenceState} order=${it.spawnOrder} fading=${it.fading}"
    }

    fun tracked(): String {
        val out = Array<MegaGameEntity>()
        state.getAllSpawned(out)
        return describe(out.map { it as MoonScythe })
    }

    describe("three Moon Scythes shots 1 s apart, with trails and pool reuse") {
        it("fades the 4 older scythes on the second and on the third shot") {
            val shot1 = shoot()
            runFrames(1.05f)

            val shot2 = shoot()
            withClue("after shot 2: shot1=[${describe(shot1)}] shot2=[${describe(shot2)}] tracked=[${tracked()}]") {
                shot1.map { it.fading } shouldBe List(4) { true }
                shot2.map { it.fading } shouldBe List(4) { false }
            }
            runFrames(1.05f)

            val beforeShot3 = describe(shot2)
            val trackedBeforeShot3 = tracked()
            val shot3 = shoot()
            withClue(
                "before shot 3: shot2=[$beforeShot3] tracked=[$trackedBeforeShot3]; " +
                    "after shot 3: shot2=[${describe(shot2)}] shot3=[${describe(shot3)}] tracked=[${tracked()}]"
            ) {
                shot2.map { it.fading } shouldBe List(4) { true }
                shot3.map { it.fading } shouldBe List(4) { false }
            }
        }
    }
})
