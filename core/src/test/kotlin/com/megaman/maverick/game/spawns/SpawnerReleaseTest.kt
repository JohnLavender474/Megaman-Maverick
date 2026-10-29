package com.megaman.maverick.game.spawns

import com.megaman.maverick.game.testServices
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.events.Event
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.levels.LevelState
import com.megaman.maverick.game.state.GameState
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.mockk

private class SpawnedEntity(game: MegaServices) : MegaGameEntity(game) {
    var allowSpawn = true
    override fun canSpawn(spawnProps: Properties) = allowSpawn && super.canSpawn(spawnProps)
    override fun getType() = EntityType.OTHER
}

private const val ROOM_EVENT = "room_event"

class SpawnerReleaseTest : DescribeSpec({

    lateinit var engine: GameEngine
    lateinit var game: MegaServices
    lateinit var spawns: Array<Spawn>
    lateinit var spawnsMan: SpawnsManager

    beforeEach {
        engine = GameEngine()
        game = testServices(engine = engine)
        spawns = Array()
        spawnsMan = SpawnsManager(spawns)
    }

    fun spawnPass() {
        spawnsMan.update(0f)
        val iter = spawns.iterator()
        while (iter.hasNext()) {
            val spawn = iter.next()
            if (!engine.spawn(spawn.entity, spawn.properties)) spawn.onRejected?.invoke()
            iter.remove()
        }
    }

    fun frame(duringUpdate: () -> Unit = {}, events: List<Event> = emptyList(), listener: SpawnerForEvent? = null) {
        spawnPass()
        duringUpdate()
        engine.update(0f)
        events.forEach { listener?.onEvent(it) }
    }

    fun eventSpawner(supplier: () -> MegaGameEntity) = SpawnerForEvent(
        predicate = { it.key == ROOM_EVENT },
        spawnSupplier = { Spawn(supplier(), Properties()) }
    )

    describe("an event spawner") {
        it("accepts the events of the frame in which its entity is destroyed after the spawn pass") {
            val made = ArrayList<MegaGameEntity>()
            val spawner = eventSpawner { SpawnedEntity(game).also { made.add(it) } }
            spawnsMan.setSpawners(Array<ISpawner>().apply { add(spawner) })

            frame(events = listOf(Event(ROOM_EVENT)), listener = spawner)
            frame()
            made.size shouldBe 1
            made[0].existenceState.liveNow shouldBe true

            frame(duringUpdate = { made[0].destroy() }, events = listOf(Event(ROOM_EVENT)), listener = spawner)
            frame()

            made.size shouldBe 2
            spawner.spawned shouldBe true
        }

        it("ignores events while its entity lives") {
            val made = ArrayList<MegaGameEntity>()
            val spawner = eventSpawner { SpawnedEntity(game).also { made.add(it) } }
            spawnsMan.setSpawners(Array<ISpawner>().apply { add(spawner) })

            frame(events = listOf(Event(ROOM_EVENT)), listener = spawner)
            frame(events = listOf(Event(ROOM_EVENT)), listener = spawner)
            frame()

            made.size shouldBe 1
        }

        it("does not keep a pooled entity that another caller spawned again before the next spawn pass") {
            val pooled = SpawnedEntity(game)
            val spawner = eventSpawner { pooled }
            spawnsMan.setSpawners(Array<ISpawner>().apply { add(spawner) })

            frame(events = listOf(Event(ROOM_EVENT)), listener = spawner)
            frame()
            spawner.get()!!.entity shouldBeSameInstanceAs pooled

            frame(duringUpdate = { pooled.destroy() })
            pooled.spawn(Properties())
            frame()

            spawner.spawned shouldBe false
        }

        it("is free after reset, and leaves no runnable on its entity") {
            val entity = SpawnedEntity(game)
            val spawner = eventSpawner { entity }
            spawnsMan.setSpawners(Array<ISpawner>().apply { add(spawner) })
            frame(events = listOf(Event(ROOM_EVENT)), listener = spawner)
            frame()

            spawnsMan.reset()

            spawner.spawned shouldBe false
            entity.runnablesOnDestroyQueued.containsKey(ConstKeys.SPAWNER) shouldBe false
        }

        it("leaves no runnable on its entity when it is removed after one spawn") {
            val entity = SpawnedEntity(game)
            val spawner = SpawnerForEvent(
                predicate = { it.key == ROOM_EVENT },
                spawnSupplier = { Spawn(entity, Properties()) },
                respawnable = false
            )
            spawnsMan.setSpawners(Array<ISpawner>().apply { add(spawner) })

            frame(events = listOf(Event(ROOM_EVENT)), listener = spawner)
            frame()

            entity.existenceState.liveNow shouldBe true
            entity.runnablesOnDestroyQueued.containsKey(ConstKeys.SPAWNER) shouldBe false
        }

        it("leaves no runnable on its pooled entity after another owner reused it") {
            val pooled = SpawnedEntity(game)
            val spawner = eventSpawner { pooled }
            spawnsMan.setSpawners(Array<ISpawner>().apply { add(spawner) })
            frame(events = listOf(Event(ROOM_EVENT)), listener = spawner)
            frame()
            frame(duringUpdate = { pooled.destroy() })
            pooled.spawn(Properties())
            frame()

            frame(events = listOf(Event(ROOM_EVENT)), listener = spawner)
            spawner.spawned shouldBe false
            pooled.destroy()

            pooled.runnablesOnDestroyQueued.containsKey(ConstKeys.SPAWNER) shouldBe false
        }

        it("is free again after its spawn is rejected") {
            val entity = SpawnedEntity(game).also { it.allowSpawn = false }
            val spawner = eventSpawner { entity }
            spawnsMan.setSpawners(Array<ISpawner>().apply { add(spawner) })

            frame(events = listOf(Event(ROOM_EVENT)), listener = spawner)
            frame()

            spawner.spawned shouldBe false
        }
    }

    describe("a bounds spawner") {
        fun boundsSpawner(camera: GameRectangle, made: MutableList<MegaGameEntity>) = SpawnerForBoundsEntered(
            spawnSupplier = { Spawn(SpawnedEntity(game).also { made.add(it) }, Properties()) },
            thisBounds = { GameRectangle(100, 0, 10, 10) },
            otherBounds = { camera }
        )

        val onScreen = GameRectangle(50, 0, 100, 100)
        val offScreen = GameRectangle(500, 0, 100, 100)

        it("spawns when its area enters the camera after its entity died off screen") {
            val camera = GameRectangle(onScreen)
            val made = ArrayList<MegaGameEntity>()
            val spawner = boundsSpawner(camera, made)
            spawnsMan.setSpawners(Array<ISpawner>().apply { add(spawner) })

            frame()
            made.size shouldBe 1

            camera.set(offScreen)
            frame()
            frame(duringUpdate = { made[0].destroy() })
            camera.set(onScreen)
            frame()

            made.size shouldBe 2
        }

        it("respawns an entity that died on screen only after its area leaves the camera and enters again") {
            val camera = GameRectangle(onScreen)
            val made = ArrayList<MegaGameEntity>()
            val spawner = boundsSpawner(camera, made)
            spawnsMan.setSpawners(Array<ISpawner>().apply { add(spawner) })

            frame()
            frame(duringUpdate = { made[0].destroy() })
            frame()
            frame()
            made.size shouldBe 1

            camera.set(offScreen)
            frame()
            made.size shouldBe 1

            camera.set(onScreen)
            frame()
            made.size shouldBe 2
        }
    }
})
