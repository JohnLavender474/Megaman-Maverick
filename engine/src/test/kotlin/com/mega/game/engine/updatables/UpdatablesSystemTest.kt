package com.mega.game.engine.updatables

import com.mega.game.engine.GameEngine
import com.mega.game.engine.MockGameEntity
import com.mega.game.engine.common.extensions.orderedMapOf
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.objects.pairTo
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import io.mockk.*

class UpdatablesSystemTest : DescribeSpec({

    lateinit var system: UpdatablesSystem
    lateinit var engine: GameEngine

    beforeEach {
        system = UpdatablesSystem()
        engine = GameEngine()
        engine.systems.add(system)
    }

    describe("UpdatablesSystem") {
        it("should call the update method for each updatable in each entity") {
            val entity1 = MockGameEntity()
            val entity2 = MockGameEntity()
            val updatable1 = mockk<Updatable> { every { update(any()) } just Runs }
            val updatable2 = mockk<Updatable> { every { update(any()) } just Runs }
            val updatablesComponent1 = UpdatablesComponent(orderedMapOf("1" pairTo updatable1))
            val updatablesComponent2 = UpdatablesComponent(orderedMapOf("2" pairTo updatable2))
            entity1.addComponent(updatablesComponent1)
            entity2.addComponent(updatablesComponent2)

            engine.spawn(entity1)
            engine.spawn(entity2)

            checkAll(Arb.int(0, 100)) { delta ->
                engine.update(delta.toFloat())
                verify { updatable1.update(delta.toFloat()) }
                verify { updatable2.update(delta.toFloat()) }
            }
        }

        it("should not call the update method when the system is off") {
            system.on = false
            val entity = MockGameEntity()
            val updatable = mockk<Updatable> { every { update(any()) } just Runs }
            val updatablesComponent = UpdatablesComponent(orderedMapOf("1" pairTo updatable))
            entity.addComponent(updatablesComponent)

            engine.spawn(entity)

            checkAll(Arb.int(0, 100)) { delta ->
                system.update(delta.toFloat())
                verify(exactly = 0) { updatable.update(any()) }
            }
        }
    }
})
