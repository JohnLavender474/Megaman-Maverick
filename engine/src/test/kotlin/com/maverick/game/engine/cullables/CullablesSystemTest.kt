package com.maverick.game.engine.cullables

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.maverick.game.engine.MockGameEntity
import com.maverick.game.engine.diagnostics.RuntimeDiagnostics
import com.maverick.game.engine.entities.GameEntity
import com.maverick.game.engine.entities.IGameEntity
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify

class CullablesSystemTest : DescribeSpec({

    lateinit var culler: GameEntityCuller
    lateinit var cullablesSystem: CullablesSystem
    lateinit var culledEntities: ObjectSet<IGameEntity>


    beforeEach {
        culledEntities = ObjectSet()
        culler = spyk(object : GameEntityCuller {
            override fun cull(entity: IGameEntity) {
                culledEntities.add(entity)
            }
        })
        cullablesSystem = CullablesSystem(culler)
    }

    describe("CullablesSystem") {
        it("should cull entities with cullables marked for culling") {
            val entities = Array<GameEntity>()
            for (i in 0..10) {
                val shouldCull = i % 2 == 0
                val entity = spyk(MockGameEntity())
                val cullablesComponent = CullablesComponent()
                cullablesComponent.cullables.put("key", object : ICullable {
                    override fun shouldBeCulled(delta: Float) = shouldCull
                })
                entity.addComponent(cullablesComponent)
                cullablesSystem.add(entity)
                entities.add(entity)
            }

            cullablesSystem.update(1f)

            for (i in 0..10) {
                val times = if (i % 2 == 0) 1 else 0
                val entity = entities[i]
                val cullable = entity.getComponent(CullablesComponent::class)?.cullables?.get("key")
                cullable shouldNotBe null
                verify(exactly = times) { culler.cull(entity) }
                val contains = culledEntities.contains(entity)
                contains shouldBe (times != 0)
            }
        }

        it("should go on with the next entities when a member loses its cullables component during the update") {
            val diagnostics = mockk<RuntimeDiagnostics>(relaxed = true)
            val a = MockGameEntity()
            val b = MockGameEntity()
            val c = MockGameEntity()
            val removingCuller = spyk(object : GameEntityCuller {
                override fun cull(entity: IGameEntity) {
                    if (entity == a) b.removeComponent(CullablesComponent::class)
                }
            })
            val system = CullablesSystem(removingCuller, diagnostics)
            for (entity in listOf(a, b, c)) {
                val component = CullablesComponent()
                component.put("key", object : ICullable {
                    override fun shouldBeCulled(delta: Float) = true
                })
                entity.addComponent(component)
                system.add(entity)
            }

            system.update(1f)

            verify(exactly = 1) { removingCuller.cull(a) }
            verify(exactly = 0) { removingCuller.cull(b) }
            verify(exactly = 1) { removingCuller.cull(c) }
            verify(exactly = 1) { diagnostics.endEntry() }
        }

        it("should not cull entities with no cullable component") {
            val entities = Array<GameEntity>()

            for (i in 0..10) {
                val entity = MockGameEntity()
                cullablesSystem.add(entity)
                entities.add(entity)
            }

            cullablesSystem.update(1f)

            entities.forEach { entity ->
                verify(exactly = 0) { culler.cull(entity) }
                entity.getComponent(CullablesComponent::class) shouldBe null
                culledEntities.isEmpty shouldBe true
            }
        }
    }
})
