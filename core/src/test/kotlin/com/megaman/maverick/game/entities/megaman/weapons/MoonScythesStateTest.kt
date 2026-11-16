package com.megaman.maverick.game.entities.megaman.weapons

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.megaman.constants.MegaChargeStatus
import com.megaman.maverick.game.entities.projectiles.MoonScythe
import com.megaman.maverick.game.testServices
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll

class MoonScythesStateTest : DescribeSpec({

    lateinit var engine: GameEngine
    lateinit var services: MegaServices
    lateinit var state: MoonScythesState

    beforeEach {
        engine = GameEngine()
        services = testServices(engine = engine)
        val atlas = mockk<TextureAtlas>()
        every { atlas.findRegion(any()) } returns AtlasRegion(mockk<Texture>(relaxed = true), 0, 0, 64, 64)
        every { services.assets.get(any<String>(), TextureAtlas::class.java) } returns atlas
        state = MoonScythesState(cooldown = 0.1f)
    }

    afterEach {
        engine.reset()
        unmockkAll()
    }

    fun shoot(scythes: List<MoonScythe>): List<MoonScythe> {
        scythes.forEach { scythe ->
            scythe.spawn(
                props(
                    ConstKeys.FADE pairTo false,
                    ConstKeys.POSITION pairTo Vector2(),
                    ConstKeys.TRAJECTORY pairTo Vector2()
                )
            ) shouldBe true
            state.addSpawned(MegaChargeStatus.NOT_CHARGED, scythe)
        }
        return scythes
    }

    fun shoot(count: Int = 4) = shoot(List(count) { MoonScythe(services) })

    fun List<MoonScythe>.fadingFlags() = map { it.fading }

    describe("the fade of extra moon scythes") {
        it("fades the older shot and keeps the new shot when it runs while the new spawns are queued") {
            val older = shoot()
            engine.update(0f)

            val newer = shoot()
            state.fadeExtraScythes()
            engine.update(0f)

            older.fadingFlags() shouldBe List(4) { true }
            newer.fadingFlags() shouldBe List(4) { false }
        }

        it("counts once a scythe that the pool gives back to the next shot") {
            val scythes = shoot()
            engine.update(0f)
            state.fadeExtraScythes()
            scythes.forEach { it.destroy() }
            engine.update(0f)

            shoot(scythes)
            engine.update(0f)
            state.fadeExtraScythes()

            scythes.fadingFlags() shouldBe List(4) { false }
        }

        it("does not count a scythe that already fades") {
            val older = shoot()
            engine.update(0f)
            val newer = shoot()
            engine.update(0f)
            newer[3].fading = true

            state.fadeExtraScythes()

            older.fadingFlags() shouldBe listOf(true, true, true, false)
            newer.fadingFlags() shouldBe listOf(false, false, false, true)
        }

        it("fades only the oldest scythe when a shot makes 5") {
            val older = shoot(1)
            engine.update(0f)
            state.fadeExtraScythes()

            val newer = shoot()
            state.fadeExtraScythes()
            engine.update(0f)

            older.fadingFlags() shouldBe listOf(true)
            newer.fadingFlags() shouldBe List(4) { false }
        }
    }
})
