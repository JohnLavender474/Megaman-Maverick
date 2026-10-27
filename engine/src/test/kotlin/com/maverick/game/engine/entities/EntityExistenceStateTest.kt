package com.maverick.game.engine.entities

import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.common.GameLogLevel
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.LogReceiver
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.entities.EntityExistenceState.DESTROY_QUEUED
import com.maverick.game.engine.entities.EntityExistenceState.LIVE
import com.maverick.game.engine.entities.EntityExistenceState.RESPAWN_QUEUED
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

    describe("the existence questions") {
        it("give liveNow and liveAfterUpdate for each state") {
            EntityExistenceState.entries.associateWith { it.liveNow to it.liveAfterUpdate } shouldBe mapOf(
                UNSPAWNED to (false to false),
                SPAWN_QUEUED to (false to true),
                LIVE to (true to true),
                DESTROY_QUEUED to (true to false),
                RESPAWN_QUEUED to (true to true)
            )
        }
    }

    describe("hasClaimOn") {
        it("is false for a never spawned entity and true while the spawn is queued and the entity is live") {
            val e = entity()
            engine.hasClaimOn(e) shouldBe false

            e.spawn(Properties())
            engine.hasClaimOn(e) shouldBe true

            engine.update(0f)
            engine.hasClaimOn(e) shouldBe true
        }

        it("is true while the destroy is queued and false after the destroy") {
            val e = entity()
            e.spawn(Properties())
            engine.update(0f)

            e.destroy()
            engine.hasClaimOn(e) shouldBe true

            engine.update(0f)
            engine.hasClaimOn(e) shouldBe false
        }

        it("is true while a respawn is queued") {
            val e = entity()
            e.spawn(Properties())
            engine.update(0f)
            e.destroy()
            e.spawn(Properties())

            e.existenceState shouldBe RESPAWN_QUEUED
            engine.hasClaimOn(e) shouldBe true
        }

        it("is true inside the entity's own onSpawn and onDestroy") {
            val e = entity()
            val inOnSpawn = ArrayList<Boolean>()
            val inOnDestroy = ArrayList<Boolean>()
            e.onSpawnAction = { inOnSpawn.add(engine.hasClaimOn(e)) }
            e.onDestroyAction = { inOnDestroy.add(engine.hasClaimOn(e)) }

            e.spawn(Properties())
            engine.update(0f)
            e.destroy()
            engine.update(0f)

            inOnSpawn shouldBe listOf(true)
            inOnDestroy shouldBe listOf(true)
        }

        it("is true for a spawn and then a destroy before the update, until the destroy runs") {
            val e = entity()
            e.spawn(Properties())
            e.destroy()

            e.existenceState shouldBe UNSPAWNED
            engine.hasClaimOn(e) shouldBe true

            engine.update(0f)
            engine.hasClaimOn(e) shouldBe false
            log shouldBe listOf("onSpawn e", "onDestroy e")
        }

        it("is false after reset for live and queued entities") {
            val live = entity("live")
            val queued = entity("queued")
            live.spawn(Properties())
            engine.update(0f)
            queued.spawn(Properties())

            engine.reset()

            engine.hasClaimOn(live) shouldBe false
            engine.hasClaimOn(queued) shouldBe false
        }
    }

    describe("the normal life of an entity") {
        it("goes UNSPAWNED, SPAWN_QUEUED, LIVE, DESTROY_QUEUED, UNSPAWNED") {
            val e = entity()
            e.existenceState shouldBe UNSPAWNED
            e.existenceState.liveNow shouldBe false

            e.spawn(Properties()) shouldBe true
            e.existenceState shouldBe SPAWN_QUEUED
            e.existenceState.liveNow shouldBe false

            engine.update(0f)
            e.existenceState shouldBe LIVE
            e.existenceState.liveNow shouldBe true

            e.destroy() shouldBe true
            e.existenceState shouldBe DESTROY_QUEUED
            e.existenceState.liveNow shouldBe true

            engine.update(0f)
            e.existenceState shouldBe UNSPAWNED
            e.existenceState.liveNow shouldBe false
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

    describe("rule 1: a spawn of a live entity is refused") {
        it("returns false, keeps the entity LIVE, runs no onSpawn and writes a log line") {
            val e = entity()
            e.spawn(Properties())
            engine.update(0f)

            e.spawn(Properties()) shouldBe false
            e.existenceState shouldBe LIVE
            engine.update(0f)

            e.existenceState shouldBe LIVE
            log shouldBe listOf("onSpawn e")
            engineLog.size shouldBe 1
            engineLog[0].startsWith("spawn(): refused") shouldBe true
        }

        it("refuses a spawn of the entity inside its own onSpawn") {
            val e = entity()
            var result: Boolean? = null
            e.onSpawnAction = { result = e.spawn(Properties()) }
            e.spawn(Properties())

            engine.update(0f)
            engine.update(0f)

            result shouldBe false
            e.existenceState shouldBe LIVE
            log shouldBe listOf("onSpawn e")
        }
    }

    describe("rule 2: a spawn while a destroy is queued is a respawn") {
        it("is RESPAWN_QUEUED, live now and live after the update, and the update runs onDestroy then onSpawn") {
            val e = entity()
            e.spawn(Properties())
            engine.update(0f)

            e.destroy()
            e.spawn(Properties()) shouldBe true
            e.existenceState shouldBe RESPAWN_QUEUED
            e.existenceState.liveNow shouldBe true
            e.existenceState.liveAfterUpdate shouldBe true
            e.spawn(Properties()) shouldBe false
            engine.update(0f)

            e.existenceState shouldBe LIVE
            log shouldBe listOf("onSpawn e", "onDestroy e", "onSpawn e")
            e.statesInOnDestroy shouldBe listOf(RESPAWN_QUEUED)
            engineLog.size shouldBe 1
            engineLog[0].startsWith("spawn(): a destroy is queued, so the entity respawns") shouldBe true
        }

        it("runs the respawn after the other spawns of the update") {
            val e = entity("e")
            val other = entity("other")
            e.spawn(Properties())
            engine.update(0f)

            e.destroy()
            e.spawn(Properties())
            other.spawn(Properties())
            engine.update(0f)

            log shouldBe listOf("onSpawn e", "onSpawn other", "onDestroy e", "onSpawn e")
        }

        it("a destroy after the respawn cancels it: the last call wins") {
            val e = entity()
            e.spawn(Properties())
            engine.update(0f)

            e.destroy()
            e.spawn(Properties())
            e.destroy() shouldBe true
            e.existenceState shouldBe DESTROY_QUEUED
            engine.update(0f)

            e.existenceState shouldBe UNSPAWNED
            log shouldBe listOf("onSpawn e", "onDestroy e")
        }

        it("a destroy inside the onSpawn of a respawn waits for the next update") {
            val e = entity()
            e.spawn(Properties())
            engine.update(0f)
            e.destroy()
            e.spawn(Properties())
            e.onSpawnAction = {
                e.onSpawnAction = {}
                e.destroy()
            }

            engine.update(0f)
            e.existenceState shouldBe DESTROY_QUEUED
            engine.update(0f)

            e.existenceState shouldBe UNSPAWNED
            log shouldBe listOf("onSpawn e", "onDestroy e", "onSpawn e", "onDestroy e")
        }
    }

    describe("rule 3: a destroy of an unspawned entity is refused") {
        it("returns false, runs no onDestroy and writes a log line") {
            val e = entity()
            e.spawn(Properties())
            engine.update(0f)
            e.destroy()
            engine.update(0f)

            e.destroy() shouldBe false
            e.existenceState shouldBe UNSPAWNED
            engine.update(0f)

            log shouldBe listOf("onSpawn e", "onDestroy e")
            engineLog.size shouldBe 1
            engineLog[0].startsWith("destroy(): refused, the entity is not spawned") shouldBe true
        }

        it("refuses a destroy of a never spawned entity, so a later spawn is a normal spawn") {
            val e = entity()

            e.destroy() shouldBe false
            e.spawn(Properties()) shouldBe true
            e.existenceState shouldBe SPAWN_QUEUED
            engine.update(0f)

            e.existenceState shouldBe LIVE
            log shouldBe listOf("onSpawn e")
        }

        it("refuses a destroy of the entity inside its own onDestroy") {
            val e = entity()
            var result: Boolean? = null
            e.onDestroyAction = { result = e.destroy() }
            e.spawn(Properties())
            engine.update(0f)
            e.destroy()

            engine.update(0f)
            engine.update(0f)

            result shouldBe false
            e.existenceState shouldBe UNSPAWNED
            log shouldBe listOf("onSpawn e", "onDestroy e")
        }

        it("a child and its parent destroyed in one frame: the onDestroy of the child runs once") {
            val parent = entity("parent")
            val child = entity("child")
            parent.onDestroyAction = { child.destroy() }
            parent.spawn(Properties())
            child.spawn(Properties())
            engine.update(0f)

            child.destroy()
            parent.destroy()
            engine.update(0f)

            log shouldBe listOf("onSpawn parent", "onSpawn child", "onDestroy child", "onDestroy parent")
        }
    }

    describe("rule 4: a destroy of an entity whose spawn is still queued waits for the spawn") {
        it("a spawn and then a destroy in the destroy phase: onSpawn then onDestroy at the next update") {
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
            child.existenceState shouldBe UNSPAWNED
            child.existenceState.liveAfterUpdate shouldBe false

            engine.update(0f)
            child.existenceState shouldBe UNSPAWNED
            log shouldBe listOf("onSpawn parent", "onDestroy parent", "onSpawn child", "onDestroy child")
        }

        it("a destroy inside onSpawn is still the normal path") {
            val e = entity()
            e.onSpawnAction = { e.destroy() shouldBe true }
            e.spawn(Properties())

            engine.update(0f)

            e.existenceState shouldBe UNSPAWNED
            log shouldBe listOf("onSpawn e", "onDestroy e")
            engineLog shouldBe emptyList()
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
        it("onSpawn and onDestroy alternate, the last accepted call wins, and liveNow and liveAfterUpdate tell the truth") {
            var checks = 0

            repeat(2_000) { sequence ->
                val random = Random(sequence.toLong())
                val entities = (0 until random.nextInt(3, 7)).map { entity("e$it") }
                val touched = HashSet<StateEntity>()
                val lastAcceptedCall = HashMap<StateEntity, Boolean>()

                fun spawn(e: StateEntity) {
                    touched.add(e)
                    if (e.spawn(Properties())) lastAcceptedCall[e] = true
                }

                fun destroy(e: StateEntity) {
                    touched.add(e)
                    if (e.destroy()) lastAcceptedCall[e] = false
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
                log.clear()

                repeat(random.nextInt(20, 61)) { step ->
                    val liveAfterNextUpdate = entities.map { it.existenceState.liveAfterUpdate }
                    val liveBefore = entities.map { it.existenceState.liveNow }
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
                                    e.existenceState.liveNow shouldBe liveAfterNextUpdate[index]
                                    e.existenceState.liveNow shouldBe (lastAcceptedCall.remove(e) ?: liveBefore[index])
                                    checks++
                                }
                            }
                        }

                        else -> {
                            engine.reset()
                            lastAcceptedCall.clear()
                        }
                    }

                    entities.forEach { e ->
                        withClue("sequence $sequence, step $step, ${e.name}") { e.existenceState.liveNow shouldBe engine.contains(e) }
                    }
                }

                engine.reset()

                entities.forEach { e ->
                    val events = log.filter { it == "onSpawn ${e.name}" || it == "onDestroy ${e.name}" }
                    events.forEachIndexed { index, event ->
                        val expected = if (index % 2 == 0) "onSpawn ${e.name}" else "onDestroy ${e.name}"
                        withClue("sequence $sequence, ${e.name}, event $index") { event shouldBe expected }
                    }
                }
                log.clear()
            }

            checks shouldBeGreaterThan 10_000
        }
    }
})
