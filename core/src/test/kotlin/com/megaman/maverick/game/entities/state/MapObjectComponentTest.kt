package com.megaman.maverick.game.entities.state

import com.megaman.maverick.game.testServices
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.levels.LevelState
import com.megaman.maverick.game.state.GameState
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

private class MapObjectTestEntity(services: MegaServices) : MegaGameEntity(services) {

    var idInOnDestroy: Int? = null

    override fun getType() = EntityType.OTHER

    override fun onDestroy() {
        super.onDestroy()
        idInOnDestroy = mapObjectId
    }
}

class MapObjectComponentTest : DescribeSpec({

    lateinit var engine: GameEngine
    lateinit var services: MegaServices

    beforeEach {
        engine = GameEngine()
        services = testServices(engine = engine)
    }

    afterEach { engine.reset() }

    fun spawn(entity: MapObjectTestEntity, props: Properties): MapObjectTestEntity {
        entity.spawn(props)
        engine.update(0f)
        return entity
    }

    fun destroy(entity: MapObjectTestEntity) {
        entity.destroy()
        engine.update(0f)
    }

    describe("MapObjectComponent") {

        it("is added with the id when the spawn has ConstKeys.ID") {
            val entity = spawn(MapObjectTestEntity(services), props(ConstKeys.ID pairTo 7101))

            entity.getComponent(MapObjectComponent::class)!!.id shouldBe 7101
            entity.mapObjectId shouldBe 7101
        }

        it("is not added when the spawn has no ConstKeys.ID") {
            val entity = spawn(MapObjectTestEntity(services), Properties())

            entity.hasComponent(MapObjectComponent::class) shouldBe false
            entity.mapObjectId shouldBe null
        }

        it("is removed when a pooled entity spawns again with no ConstKeys.ID") {
            val entity = spawn(MapObjectTestEntity(services), props(ConstKeys.ID pairTo 7102))
            destroy(entity)

            spawn(entity, Properties())

            entity.hasComponent(MapObjectComponent::class) shouldBe false
            MegaGameEntities.getOfId(7102).contains(entity) shouldBe false
        }

        it("gets the new id when a pooled entity spawns again with another ConstKeys.ID") {
            val entity = spawn(MapObjectTestEntity(services), props(ConstKeys.ID pairTo 7103))
            destroy(entity)

            spawn(entity, props(ConstKeys.ID pairTo 7104))

            entity.mapObjectId shouldBe 7104
            MegaGameEntities.getOfId(7103).contains(entity) shouldBe false
            MegaGameEntities.getOfId(7104).toList() shouldBe listOf(entity)
        }

        it("keeps the id readable in onDestroy, after the component reset") {
            val entity = spawn(MapObjectTestEntity(services), props(ConstKeys.ID pairTo 7105))

            destroy(entity)

            entity.idInOnDestroy shouldBe 7105
        }
    }

    describe("MegaGameEntities.getOfId") {

        it("gives the entities of a map id in spawn order, and drops them at destroy") {
            val first = spawn(MapObjectTestEntity(services), props(ConstKeys.ID pairTo 7201))
            val second = spawn(MapObjectTestEntity(services), props(ConstKeys.ID pairTo 7201))

            MegaGameEntities.getOfId(7201).toList() shouldBe listOf(first, second)
            MegaGameEntities.existsAnyOfId(7201) shouldBe true

            destroy(first)
            MegaGameEntities.getOfId(7201).toList() shouldBe listOf(second)

            destroy(second)
            MegaGameEntities.getOfId(7201).isEmpty shouldBe true
            MegaGameEntities.existsAnyOfId(7201) shouldBe false
        }

        it("does not index an entity that spawns with no map id") {
            val runtime = spawn(MapObjectTestEntity(services), Properties())

            MegaGameEntities.getOfTag(runtime.getTag()).contains(runtime) shouldBe true
            listOf(-1, 0, 1).forEach { MegaGameEntities.getOfId(it).contains(runtime) shouldBe false }
        }
    }
})
