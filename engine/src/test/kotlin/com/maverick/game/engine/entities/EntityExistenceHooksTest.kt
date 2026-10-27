package com.maverick.game.engine.entities

import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.entities.EntityExistenceState.DESTROY_QUEUED
import com.maverick.game.engine.entities.EntityExistenceState.RESPAWN_QUEUED
import com.maverick.game.engine.entities.EntityExistenceState.SPAWN_QUEUED
import com.maverick.game.engine.entities.EntityExistenceState.UNSPAWNED
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlin.random.Random

private class HookEntity(engine: GameEngine) : GameEntity(engine) {

    var allowSpawn = true
    var onSpawnAction: () -> Unit = {}
    var onDestroyAction: () -> Unit = {}
    val spawnQueuedStates = ArrayList<EntityExistenceState>()
    val destroyQueuedStates = ArrayList<EntityExistenceState>()

    override fun canSpawn(spawnProps: Properties) = allowSpawn

    override fun onSpawnQueued() {
        spawnQueuedStates.add(existenceState)
    }

    override fun onDestroyQueued() {
        destroyQueuedStates.add(existenceState)
    }

    override fun onSpawn(spawnProps: Properties) = onSpawnAction()

    override fun onDestroy() = onDestroyAction()
}

class EntityExistenceHooksTest : DescribeSpec({

    lateinit var engine: GameEngine
    lateinit var rejected: MutableList<IGameEntity>

    beforeEach {
        rejected = ArrayList()
        engine = GameEngine(onSpawnRejected = { rejected.add(it) })
    }

    fun live() = HookEntity(engine).also {
        it.spawn(Properties())
        engine.update(0f)
        it.spawnQueuedStates.clear()
    }

    describe("onSpawnQueued") {
        it("runs once with SPAWN_QUEUED for a spawn of an unspawned entity") {
            val e = HookEntity(engine)

            e.spawn(Properties())

            e.spawnQueuedStates shouldBe listOf(SPAWN_QUEUED)
        }

        it("runs once with RESPAWN_QUEUED for a respawn") {
            val e = live()
            e.destroy()

            e.spawn(Properties())

            e.spawnQueuedStates shouldBe listOf(RESPAWN_QUEUED)
        }

        it("runs once for a spawn inside the entity's own onDestroy") {
            val e = live()
            e.onDestroyAction = { e.spawn(Properties()) }
            e.destroy()

            engine.update(0f)

            e.spawnQueuedStates.size shouldBe 1
            e.existenceState shouldBe SPAWN_QUEUED
        }

        it("does not run for a spawn refused by canSpawn, which still calls onSpawnRejected") {
            val e = HookEntity(engine).also { it.allowSpawn = false }

            e.spawn(Properties()) shouldBe false

            e.spawnQueuedStates shouldBe emptyList()
            rejected shouldBe listOf(e)
        }

        it("does not run for a spawn of a live entity, a repeated spawn, or a spawn inside its own onSpawn") {
            val e = live()
            e.spawn(Properties()) shouldBe false

            val other = HookEntity(engine)
            other.onSpawnAction = { other.spawn(Properties()) }
            other.spawn(Properties())
            other.spawn(Properties()) shouldBe false
            engine.update(0f)

            e.spawnQueuedStates shouldBe emptyList()
            other.spawnQueuedStates shouldBe listOf(SPAWN_QUEUED)
        }
    }

    describe("onDestroyQueued") {
        it("runs once with DESTROY_QUEUED for a destroy of a live entity") {
            val e = live()

            e.destroy()

            e.destroyQueuedStates shouldBe listOf(DESTROY_QUEUED)
        }

        it("runs once with UNSPAWNED for a destroy of an entity whose spawn is queued") {
            val e = HookEntity(engine)
            e.spawn(Properties())

            e.destroy()

            e.destroyQueuedStates shouldBe listOf(UNSPAWNED)
        }

        it("runs once for a destroy that cancels a respawn") {
            val e = live()
            e.destroy()
            e.spawn(Properties())
            e.destroyQueuedStates.clear()

            e.destroy() shouldBe true

            e.destroyQueuedStates shouldBe listOf(DESTROY_QUEUED)
        }

        it("runs once for a destroy inside the entity's own onSpawn") {
            val e = HookEntity(engine)
            e.onSpawnAction = { e.destroy() }
            e.spawn(Properties())

            engine.update(0f)

            e.destroyQueuedStates shouldBe listOf(UNSPAWNED)
        }

        it("does not run for a refused or repeated destroy") {
            val unspawned = HookEntity(engine)
            unspawned.destroy() shouldBe false

            val e = live()
            e.destroy()
            e.destroy() shouldBe false
            e.onDestroyAction = { e.destroy() }
            engine.update(0f)

            unspawned.destroyQueuedStates shouldBe emptyList()
            e.destroyQueuedStates shouldBe listOf(DESTROY_QUEUED)
        }
    }

    describe("reset and dispose") {
        it("run neither hook") {
            val a = live()
            val b = HookEntity(engine).also { it.spawn(Properties()) }
            b.spawnQueuedStates.clear()

            engine.reset()
            val c = live()
            engine.dispose()

            listOf(a, b, c).forEach {
                it.spawnQueuedStates shouldBe emptyList()
                it.destroyQueuedStates shouldBe emptyList()
            }
        }
    }

    describe("on seeded random call sequences") {
        it("a hook runs once exactly when a call changes 'live after the next update', and never otherwise") {
            var checks = 0

            repeat(2_000) { sequence ->
                val random = Random(sequence.toLong())
                val entities = (0 until random.nextInt(3, 7)).map { HookEntity(engine) }

                val expectedHooks = HashMap<HookEntity, Int>()

                fun hookCount(e: HookEntity) = e.spawnQueuedStates.size + e.destroyQueuedStates.size

                fun spawn(e: HookEntity) {
                    val spawnsBefore = e.spawnQueuedStates.size
                    val destroysBefore = e.destroyQueuedStates.size
                    val accepted = e.spawn(Properties())
                    withClue("sequence $sequence, spawn") {
                        e.spawnQueuedStates.size shouldBe spawnsBefore + if (accepted) 1 else 0
                        e.destroyQueuedStates.size shouldBe destroysBefore
                    }
                    if (accepted) expectedHooks[e] = (expectedHooks[e] ?: 0) + 1
                    checks++
                }

                fun destroy(e: HookEntity) {
                    val liveAfterUpdateBefore = e.existenceState.liveAfterUpdate
                    val spawnsBefore = e.spawnQueuedStates.size
                    val destroysBefore = e.destroyQueuedStates.size
                    val accepted = e.destroy()
                    withClue("sequence $sequence, destroy") {
                        e.destroyQueuedStates.size shouldBe destroysBefore + if (accepted && liveAfterUpdateBefore) 1 else 0
                        e.spawnQueuedStates.size shouldBe spawnsBefore
                    }
                    if (accepted && liveAfterUpdateBefore) expectedHooks[e] = (expectedHooks[e] ?: 0) + 1
                    checks++
                }

                fun nested() {
                    val roll = random.nextInt(100)
                    val target = entities[random.nextInt(entities.size)]
                    when {
                        roll < 15 -> spawn(target)
                        roll < 30 -> destroy(target)
                        roll < 36 -> {
                            spawn(target)
                            destroy(target)
                        }
                    }
                }
                entities.forEach {
                    it.onSpawnAction = ::nested
                    it.onDestroyAction = ::nested
                }

                repeat(random.nextInt(20, 61)) {
                    val roll = random.nextInt(100)
                    val target = entities[random.nextInt(entities.size)]
                    when {
                        roll < 35 -> spawn(target)
                        roll < 60 -> destroy(target)
                        roll < 95 -> engine.update(0f)
                        else -> engine.reset()
                    }

                    entities.forEach { e ->
                        withClue("sequence $sequence: hooks run only inside an accepted call") {
                            hookCount(e) shouldBe (expectedHooks[e] ?: 0)
                        }
                    }
                }

                engine.reset()
            }

            checks shouldBeGreaterThan 10_000
        }
    }
})
