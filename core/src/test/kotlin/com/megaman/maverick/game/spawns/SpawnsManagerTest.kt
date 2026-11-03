package com.megaman.maverick.game.spawns

import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.common.objects.Properties
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk

private class StubSpawner(
    private val spawn: Spawn,
    override var respawnable: Boolean = true
) : ISpawner {

    var fire = true
    var tests = 0
    var rejections = 0
    var resets = 0

    override fun get() = spawn

    override fun test(delta: Float): Boolean {
        tests++
        return fire
    }

    override fun onSpawnRejected() {
        rejections++
    }

    override fun reset() {
        resets++
    }
}

class SpawnsManagerTest : DescribeSpec({

    lateinit var given: ArrayList<MegaGameEntity>
    var accept: (MegaGameEntity) -> Boolean = { true }
    lateinit var spawnsMan: SpawnsManager

    fun stub(respawnable: Boolean = true) =
        StubSpawner(Spawn(mockk<MegaGameEntity>(), Properties()), respawnable)

    beforeEach {
        given = ArrayList()
        accept = { true }
        spawnsMan = SpawnsManager { entity, _ ->
            given.add(entity)
            accept(entity)
        }
    }

    describe("update") {

        it("gives each fired spawn to the spawn function in spawner order, in the same call") {
            val a = stub()
            val b = stub().also { it.fire = false }
            val c = stub()
            spawnsMan.setSpawners(Array<ISpawner>().apply { add(a); add(b); add(c) })

            spawnsMan.update(0f)

            given shouldBe listOf(a.get().entity, c.get().entity)
        }

        it("calls onSpawnRejected only on the spawner whose spawn is refused") {
            val a = stub()
            val b = stub()
            accept = { it !== b.get().entity }
            spawnsMan.setSpawners(Array<ISpawner>().apply { add(a); add(b) })

            spawnsMan.update(0f)

            a.rejections shouldBe 0
            b.rejections shouldBe 1
        }

        it("removes a non-respawnable spawner after its spawn and does not test it again") {
            val once = stub(respawnable = false)
            val again = stub()
            spawnsMan.setSpawners(Array<ISpawner>().apply { add(once); add(again) })

            spawnsMan.update(0f)
            spawnsMan.update(0f)

            once.tests shouldBe 1
            once.resets shouldBe 1
            again.tests shouldBe 2
            given.size shouldBe 3
        }
    }
})
