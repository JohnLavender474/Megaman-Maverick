package com.megaman.maverick.game.entities.contracts

import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.cullables.CullableOnUncontained
import com.maverick.game.engine.cullables.ICullable
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.entities.contracts.ICullableEntity
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.testServices
import com.megaman.maverick.game.world.body.BodyComponentCreator
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

private class ReuseTestEnemy(services: MegaServices) : AbstractEnemy(services) {

    override fun defineBodyComponent(): BodyComponent = BodyComponentCreator.create(this, Body(BodyType.DYNAMIC))

    override fun defineSpritesComponent() = SpritesComponent()
}

private class ReuseTestShot(services: MegaServices) : AbstractProjectile(services) {

    override fun defineBodyComponent(): BodyComponent = BodyComponentCreator.create(this, Body(BodyType.ABSTRACT))

    override fun defineSpritesComponent() = SpritesComponent()
}

class SpawnReuseTest : DescribeSpec({

    lateinit var engine: GameEngine
    lateinit var services: MegaServices

    beforeEach {
        engine = GameEngine()
        services = testServices(engine = engine)
    }

    afterEach { engine.reset() }

    fun ICullableEntity.spawnWith(vararg entries: Pair<Any, Any?>) {
        (this as MegaGameEntity).spawn(Properties().apply { entries.forEach { (k, v) -> put(k, v) } }) shouldBe true
        engine.update(0f)
    }

    fun MegaGameEntity.destroyNow() {
        destroy()
        engine.update(0f)
    }

    fun levelReset() {
        services.events.submitEvent(Event(EventType.LEVEL_RESET, props()))
        services.events.run()
    }

    fun checkReuse(entity: ICullableEntity, defaultCullTime: Float) {
        entity.spawnWith(ConstKeys.CULL_TIME to 2f)
        val outOfBounds = entity.getCullable(ConstKeys.CULL_OUT_OF_BOUNDS) as CullableOnUncontained<*>
        val onEvents = entity.getCullable(ConstKeys.CULL_EVENTS)!!
        outOfBounds.timeToCull shouldBe 2f

        levelReset()
        onEvents.shouldBeCulled(0f) shouldBe true

        (entity as MegaGameEntity).destroyNow()
        levelReset()
        onEvents.shouldBeCulled(0f) shouldBe false

        entity.spawnWith()
        entity.getCullable(ConstKeys.CULL_OUT_OF_BOUNDS) shouldBeSameInstanceAs outOfBounds
        entity.getCullable(ConstKeys.CULL_EVENTS) shouldBeSameInstanceAs onEvents
        outOfBounds.timeToCull shouldBe defaultCullTime
        onEvents.shouldBeCulled(0f) shouldBe false

        levelReset()
        onEvents.shouldBeCulled(0f) shouldBe true

        entity.destroyNow()
        entity.spawnWith(ConstKeys.CULL_OUT_OF_BOUNDS to false, ConstKeys.CULL_EVENTS to false)
        entity.getCullable(ConstKeys.CULL_OUT_OF_BOUNDS).shouldBeNull()
        entity.getCullable(ConstKeys.CULL_EVENTS).shouldBeNull()
        levelReset()
        (onEvents as ICullable).shouldBeCulled(0f) shouldBe false
    }

    describe("a pooled enemy") {
        it("reuses its cullables on the next spawn, resets them, and listens only while it is spawned") {
            checkReuse(ReuseTestEnemy(services), AbstractEnemy.DEFAULT_CULL_TIME)
        }
    }

    describe("a pooled projectile") {
        it("reuses its cullables on the next spawn, resets them, and listens only while it is spawned") {
            checkReuse(ReuseTestShot(services), PROJECTILE_DEFAULT_CULL_TIME)
        }
    }
})
