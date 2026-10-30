package com.megaman.maverick.game.entities.state

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.extensions.objectSetOf
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.entities.GameEntity
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk

private open class KnockedEntity : GameEntity(mockk(relaxed = true)), IBodyEntity {

    init {
        addComponent(BodyComponent(Body(BodyType.DYNAMIC)))
    }

    override fun onSpawn(spawnProps: Properties) {}
    override fun onDestroy() {}
}

private class KnockDamager : KnockedEntity(), IDamagerEntity

private class ExemptDamager : KnockedEntity(), IDamagerEntity

private class NoBodyDamager : GameEntity(mockk(relaxed = true)), IDamagerEntity {
    override fun onSpawn(spawnProps: Properties) {}
    override fun onDestroy() {}
}

class KnockbacksTest : DescribeSpec({

    fun knocked(direction: Direction, damager: KnockedEntity, damagerOffset: Float): Vector2 {
        val entity = KnockedEntity()
        entity.addComponent(KnockbackComponent(8f, 5f, objectSetOf<Any>(ExemptDamager::class)))
        entity.body.direction = direction
        damager.body.setX(damagerOffset)
        damager.body.setY(damagerOffset)
        entity.knockBackFrom(damager as IDamagerEntity)
        return Vector2(entity.body.physics.velocity)
    }

    describe("knockBackFrom") {
        it("pushes away from the damager and off the surface in each direction") {
            knocked(Direction.UP, KnockDamager(), 1f) shouldBe Vector2(-8f, 5f)
            knocked(Direction.UP, KnockDamager(), -1f) shouldBe Vector2(8f, 5f)
            knocked(Direction.DOWN, KnockDamager(), 1f) shouldBe Vector2(-8f, -5f)
            knocked(Direction.DOWN, KnockDamager(), -1f) shouldBe Vector2(8f, -5f)
            knocked(Direction.LEFT, KnockDamager(), 1f) shouldBe Vector2(-5f, -8f)
            knocked(Direction.LEFT, KnockDamager(), -1f) shouldBe Vector2(-5f, 8f)
            knocked(Direction.RIGHT, KnockDamager(), 1f) shouldBe Vector2(5f, -8f)
            knocked(Direction.RIGHT, KnockDamager(), -1f) shouldBe Vector2(5f, 8f)
        }

        it("does nothing for an exempt damager") {
            knocked(Direction.UP, ExemptDamager(), 1f) shouldBe Vector2(0f, 0f)
        }

        it("does nothing for a damager with no body") {
            val entity = KnockedEntity()
            entity.addComponent(KnockbackComponent(8f, 5f, objectSetOf()))

            entity.knockBackFrom(NoBodyDamager())

            entity.body.physics.velocity shouldBe Vector2(0f, 0f)
        }

        it("does nothing with no component") {
            val entity = KnockedEntity()

            entity.knockBackFrom(KnockDamager())

            entity.body.physics.velocity shouldBe Vector2(0f, 0f)
        }
    }
})
