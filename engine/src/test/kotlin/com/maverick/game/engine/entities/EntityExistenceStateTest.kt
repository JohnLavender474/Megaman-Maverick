package com.maverick.game.engine.entities

import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.common.GameLogLevel
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.LogReceiver
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.entities.EntityExistenceState.DESTROY_QUEUED
import com.maverick.game.engine.entities.EntityExistenceState.LIVE
import com.maverick.game.engine.entities.EntityExistenceState.SPAWN_QUEUED
import com.maverick.game.engine.entities.EntityExistenceState.UNSPAWNED
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlin.random.Random

private class StateEntity(engine: GameEngine, val name: String, private val log: MutableList<String>) :
    GameEntity(engine) {

    var allowSpawn = true
    var onSpawnAction: () -> Unit = {}
    var onDestroyAction: () -> Unit = {}
    val statesInOnSpawn = ArrayList<EntityExistenceState>()
    val statesInOnDestroy = ArrayList<EntityExistenceState>()

    override fun canSpawn(spawnProps: Properties) = allowSpawn

    override fun onSpawn(spawnProps: Properties) {
        log.add("onSpawn $name")
        statesInOnSpawn.add(existenceState)
        onSpawnAction()
    }

    override fun onDestroy() {
        log.add("onDestroy $name")
        statesInOnDestroy.add(existenceState)
        onDestroyAction()
    }
}

private val IGameEntity.dead get() = existenceState == UNSPAWNED || existenceState == DESTROY_QUEUED

class EntityExistenceStateTest : DescribeSpec({

    lateinit var engine: GameEngine
    lateinit var log: MutableList<String>
    lateinit var engineLog: MutableList<String>

    val receiver = object : LogReceiver {
        override fun receive(
            fullMessage: String,
            time: String,
            level: GameLogLevel,
            tag: String,
            message: String,
            throwable: Throwable?
        ) {
            if (tag == GameEngine.TAG) engineLog.add(message)
        }
    }

    fun entity(name: String = "e") = StateEntity(engine, name, log)

    beforeEach {
        engine = GameEngine()
        log = ArrayList()
        engineLog = ArrayList()
        GameLogger.setLogLevel(GameLogLevel.DEBUG, true)
        GameLogger.tagsToLog.add(GameEngine.TAG)
        GameLogger.logReceivers.add(receiver)
    }

    afterEach {
        GameLogger.logReceivers.removeValue(receiver, true)
        GameLogger.tagsToLog.remove(GameEngine.TAG)
        GameLogger.setLogLevel(GameLogLevel.DEBUG, false)
    }

    describe("the normal life of an entity") {
        it("goes UNSPAWNED, SPAWN_QUEUED, LIVE, DESTROY_QUEUED, UNSPAWNED") {
            val e = entity()
            e.existenceState shouldBe UNSPAWNED
            e.spawned shouldBe false

            e.spawn(Properties()) shouldBe true
            e.existenceState shouldBe SPAWN_QUEUED
            e.spawned shouldBe false

            engine.update(0f)
            e.existenceState shouldBe LIVE
            e.spawned shouldBe true

            e.destroy() shouldBe true
            e.existenceState shouldBe DESTROY_QUEUED
            e.spawned shouldBe true

            engine.update(0f)
            e.existenceState shouldBe UNSPAWNED
            e.spawned shouldBe false
            log shouldBe listOf("onSpawn e", "onDestroy e")
            engineLog shouldBe emptyList()
        }

        it("reads the state before the change inside onSpawn and onDestroy") {
            val e = entity()
            e.spawn(Properties())
            engine.update(0f)
            e.destroy()
            engine.update(0f)

            e.statesInOnSpawn shouldBe listOf(SPAWN_QUEUED)
            e.statesInOnDestroy shouldBe listOf(DESTROY_QUEUED)
        }

        it("does not change the state on a refused spawn") {
            val e = entity().also { it.allowSpawn = false }

            e.spawn(Properties()) shouldBe false

            e.existenceState shouldBe UNSPAWNED
        }

        it("does not change the state on a second spawn or destroy call in the same frame") {
            val e = entity()
            e.spawn(Properties())
            e.spawn(Properties()) shouldBe false
            e.existenceState shouldBe SPAWN_QUEUED

            engine.update(0f)
            e.destroy()
            e.destroy() shouldBe false
            e.existenceState shouldBe DESTROY_QUEUED
        }
    }

    describe("a spawn and a destroy of an unspawned entity in one frame") {
        it("goes back to UNSPAWNED, and the update runs onSpawn and then onDestroy") {
            val e = entity()
            e.spawn(Properties())
            e.destroy()
            e.existenceState shouldBe UNSPAWNED

            engine.update(0f)

            e.existenceState shouldBe UNSPAWNED
            log shouldBe listOf("onSpawn e", "onDestroy e")
        }
    }

    describe("the odd paths keep the engine's actions, give the true state and write a log line") {
        it("a spawn of a live entity stays LIVE and runs onSpawn again") {
            val e = entity()
            e.spawn(Properties())
            engine.update(0f)

            e.spawn(Properties()) shouldBe true
            e.existenceState shouldBe LIVE
            engine.update(0f)

            e.existenceState shouldBe LIVE
            log shouldBe listOf("onSpawn e", "onSpawn e")
            engineLog.size shouldBe 1
            engineLog[0].startsWith("spawn(): the entity is live") shouldBe true
        }

        it("a spawn of a live entity while its destroy is queued stays DESTROY_QUEUED and ends UNSPAWNED") {
            val e = entity()
            e.spawn(Properties())
            engine.update(0f)

            e.destroy()
            e.spawn(Properties()) shouldBe true
            e.existenceState shouldBe DESTROY_QUEUED
            e.dead shouldBe true
            engine.update(0f)

            e.existenceState shouldBe UNSPAWNED
            log shouldBe listOf("onSpawn e", "onSpawn e", "onDestroy e")
            engineLog.size shouldBe 1
            engineLog[0].startsWith("spawn(): a destroy is queued") shouldBe true
        }

        it("a spawn of an unspawned entity while its destroy is queued stays UNSPAWNED") {
            val e = entity()
            e.destroy()
            e.spawn(Properties()) shouldBe true
            e.existenceState shouldBe UNSPAWNED
            engine.update(0f)

            e.existenceState shouldBe UNSPAWNED
            log shouldBe listOf("onSpawn e", "onDestroy e")
            engineLog.size shouldBe 2
            engineLog[0].startsWith("destroy(): the entity is not spawned") shouldBe true
            engineLog[1].startsWith("spawn(): a destroy is queued") shouldBe true
        }

        it("a destroy of an unspawned entity stays UNSPAWNED and runs onDestroy again") {
            val e = entity()
            e.spawn(Properties())
            engine.update(0f)
            e.destroy()
            engine.update(0f)

            e.destroy() shouldBe true
            e.existenceState shouldBe UNSPAWNED
            engine.update(0f)

            e.existenceState shouldBe UNSPAWNED
            log shouldBe listOf("onSpawn e", "onDestroy e", "onDestroy e")
            engineLog.size shouldBe 1
            engineLog[0].startsWith("destroy(): the entity is not spawned") shouldBe true
        }
    }

    describe("a spawn and then a destroy in the destroy phase of an update") {
        it("is SPAWN_QUEUED after the update and LIVE after the next one") {
            val parent = entity("parent")
            val child = entity("child")
            parent.onDestroyAction = {
                child.spawn(Properties())
                child.destroy()
            }
            parent.spawn(Properties())
            engine.update(0f)

            parent.destroy()
            engine.update(0f)
            child.existenceState shouldBe SPAWN_QUEUED
            child.dead shouldBe false

            engine.update(0f)
            child.existenceState shouldBe LIVE
            log shouldBe listOf("onSpawn parent", "onDestroy parent", "onDestroy child", "onSpawn child")
        }
    }

    describe("reset and dispose") {
        it("reset sets live and queued entities to UNSPAWNED") {
            val live = entity("live")
            val destroying = entity("destroying")
            val queued = entity("queued")
            live.spawn(Properties())
            destroying.spawn(Properties())
            engine.update(0f)
            destroying.destroy()
            queued.spawn(Properties())

            engine.reset()

            live.existenceState shouldBe UNSPAWNED
            destroying.existenceState shouldBe UNSPAWNED
            queued.existenceState shouldBe UNSPAWNED
            log shouldBe listOf("onSpawn live", "onSpawn destroying", "onDestroy live", "onDestroy destroying")
        }

        it("a reset during an update runs after the update and sets every entity to UNSPAWNED") {
            val resetter = entity("resetter")
            val other = entity("other")
            resetter.onSpawnAction = {
                other.spawn(Properties())
                engine.reset()
            }
            resetter.spawn(Properties())

            engine.update(0f)

            resetter.existenceState shouldBe UNSPAWNED
            other.existenceState shouldBe UNSPAWNED
        }

        it("dispose sets live and queued entities to UNSPAWNED") {
            val live = entity("live")
            val queued = entity("queued")
            live.spawn(Properties())
            engine.update(0f)
            queued.spawn(Properties())

            engine.dispose()

            live.existenceState shouldBe UNSPAWNED
            queued.existenceState shouldBe UNSPAWNED
        }
    }

    describe("the invariant on seeded random call sequences") {
        it("spawned is 'in the engine now', and not dead is 'in the engine after the next update'") {
            var checks = 0

            repeat(2_000) { sequence ->
                val random = Random(sequence.toLong())
                val entities = (0 until random.nextInt(3, 7)).map { entity("e$it") }
                val touched = HashSet<StateEntity>()

                fun spawn(e: StateEntity) {
                    touched.add(e)
                    e.spawn(Properties())
                }

                fun destroy(e: StateEntity) {
                    touched.add(e)
                    e.destroy()
                }

                fun nested() {
                    val roll = random.nextInt(100)
                    val target = entities[random.nextInt(entities.size)]
                    if (roll < 15) spawn(target) else if (roll < 30) destroy(target)
                }
                entities.forEach {
                    it.onSpawnAction = ::nested
                    it.onDestroyAction = ::nested
                }

                repeat(random.nextInt(20, 61)) { step ->
                    val liveAfterNextUpdate = entities.map { !it.dead }
                    touched.clear()

                    val roll = random.nextInt(100)
                    val target = entities[random.nextInt(entities.size)]
                    when {
                        roll < 35 -> spawn(target)
                        roll < 60 -> destroy(target)
                        roll < 95 -> {
                            engine.update(0f)
                            entities.forEachIndexed { index, e ->
                                if (e !in touched) withClue("sequence $sequence, step $step, ${e.name}") {
                                    e.spawned shouldBe liveAfterNextUpdate[index]
                                    checks++
                                }
                            }
                        }

                        else -> engine.reset()
                    }

                    entities.forEach { e ->
                        withClue("sequence $sequence, step $step, ${e.name}") { e.spawned shouldBe engine.contains(e) }
                    }
                }

                engine.reset()
            }

            checks shouldBeGreaterThan 10_000
        }
    }
})
