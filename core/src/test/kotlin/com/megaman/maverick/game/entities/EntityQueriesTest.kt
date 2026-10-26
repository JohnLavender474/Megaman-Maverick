package com.megaman.maverick.game.entities

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.damage.IDamageable
import com.maverick.game.engine.entities.GameEntity
import com.maverick.game.engine.points.Points
import com.maverick.game.engine.points.PointsComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.contracts.IHealthEntity
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.players.GravityDeviceComponent
import com.megaman.maverick.game.players.PlayerLight
import com.megaman.maverick.game.players.PlayerLightComponent
import com.megaman.maverick.game.players.lightOrNull
import com.megaman.maverick.game.players.onGravityDeviceSwitch
import com.megaman.maverick.game.screens.levels.camera.IFocusable
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.mockk

private class BareEntity : GameEntity(mockk(relaxed = true)) {
    override fun onSpawn(spawnProps: Properties) {}
    override fun onDestroy() {}
}

private class PartsEntity(game: MegamanMaverickGame) : MegaGameEntity(game), IDamageable, IHealthEntity, IFocusable {
    private val bounds = GameRectangle(0f, 0f, 2f, 2f)
    override fun getFocusBounds() = bounds
    override fun getFocusPosition(): Vector2 = bounds.getCenter(Vector2())
    override var invincible = false
    override fun getType() = EntityType.ENEMY
}

class EntityQueriesTest : DescribeSpec({

    describe("core entity queries on an entity without the parts") {
        it("fail at the read for needed parts") {
            val entity = BareEntity()

            shouldThrow<IllegalStateException> { entity.focus }
            shouldThrow<IllegalStateException> { entity.currentHealth }
            shouldThrow<IllegalStateException> { entity.depleteHealth() }
        }

        it("give the empty value for optional parts") {
            val entity = BareEntity()

            entity.invincible shouldBe true
            entity.damaged shouldBe false
            entity.shooting shouldBe false
            entity.lightOrNull shouldBe null
            entity.onGravityDeviceSwitch()
        }
    }

    describe("core entity queries on an entity with the parts") {
        it("read the focus, invincibility, health, light and gravity device") {
            val game = mockk<MegamanMaverickGame>(relaxed = true)
            every { game.engine } returns mockk<GameEngine>(relaxed = true)
            var switches = 0
            val entity = PartsEntity(game)
            entity.addComponent(PointsComponent().also { it.putPoints(ConstKeys.HEALTH, Points(0, 10, 10)) })
            entity.addComponent(PlayerLightComponent { PlayerLight.JETPACK })
            entity.addComponent(GravityDeviceComponent { switches++ })

            entity.focus shouldBeSameInstanceAs entity
            entity.invincible shouldBe false
            entity.translateHealth(-4) shouldBe true
            entity.currentHealth shouldBe 6
            entity.maxHealth shouldBe 10
            entity.healthRatio shouldBe 0.6f
            entity.lightOrNull shouldBe PlayerLight.JETPACK
            entity.onGravityDeviceSwitch()
            switches shouldBe 1

            entity.depleteHealth()

            entity.currentHealth shouldBe 0
        }
    }
})
