package com.megaman.maverick.game.spawns

import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.screens.levels.tiled.layers.SpawnersLayerBuilder
import com.megaman.maverick.game.testServices
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private class RearmEntity(services: MegaServices) : MegaGameEntity(services) {
    override fun getType() = EntityType.OTHER
}

class SpawnerRearmMarginTest : DescribeSpec({

    val inView = GameRectangle(50, 0, 100, 100)
    val justOut = GameRectangle(115, 0, 100, 100)
    val farOut = GameRectangle(200, 0, 100, 100)
    val margin = 50f

    lateinit var engine: GameEngine
    lateinit var services: MegaServices
    lateinit var spawnsMan: SpawnsManager
    lateinit var camera: GameRectangle
    lateinit var made: ArrayList<MegaGameEntity>
    var transition = false

    beforeEach {
        engine = GameEngine()
        services = testServices(engine = engine)
        spawnsMan = SpawnsManager { entity, props -> engine.spawn(entity, props) }
        camera = GameRectangle(inView)
        made = ArrayList()
        transition = false
    }

    fun spawner(rearmMargin: Float, respawnable: Boolean = true) = SpawnerFactory.spawnerForWhenInCamera(
        cameraBounds = camera,
        spawnShape = GameRectangle(100, 0, 10, 10),
        spawnSupplier = { Spawn(RearmEntity(services).also { made.add(it) }, Properties()) },
        respawnable = respawnable,
        shouldTest = { !transition },
        rearmMargin = rearmMargin
    ).also { spawnsMan.setSpawners(Array<ISpawner>().apply { add(it) }) }

    fun frame(duringUpdate: () -> Unit = {}) {
        spawnsMan.update(0f)
        duringUpdate()
        engine.update(0f)
    }

    fun killInView() {
        frame()
        made.size shouldBe 1
        frame(duringUpdate = { made.last().destroy() })
        frame()
    }

    fun moveTo(view: GameRectangle) {
        camera.set(view)
        frame()
    }

    describe("a bounds spawner with no margin") {
        it("respawns after its area leaves the view by a small step and comes back") {
            spawner(0f)
            killInView()

            moveTo(justOut)
            moveTo(inView)

            made.size shouldBe 2
        }

        it("does not re-arm at a level reset while its area stays in view") {
            spawner(0f)
            killInView()

            spawnsMan.rearm()
            frame()

            made.size shouldBe 1
        }
    }

    describe("a bounds spawner with a margin") {
        it("does not respawn after a step out of the view that is smaller than the margin") {
            spawner(margin)
            killInView()

            moveTo(justOut)
            moveTo(inView)

            made.size shouldBe 1
        }

        it("respawns after its area goes past the margin and comes back") {
            spawner(margin)
            killInView()

            moveTo(farOut)
            made.size shouldBe 1
            moveTo(inView)

            made.size shouldBe 2
        }

        it("respawns at the next entry when its entity dies past the margin") {
            spawner(margin)
            frame()
            camera.set(farOut)
            frame(duringUpdate = { made[0].destroy() })
            frame()

            moveTo(inView)

            made.size shouldBe 2
        }

        it("re-arms when the view passes the margin while spawns are not tested") {
            spawner(margin)
            killInView()

            transition = true
            moveTo(farOut)
            moveTo(inView)
            made.size shouldBe 1
            transition = false
            frame()

            made.size shouldBe 2
        }

        it("re-arms at a level reset and spawns at once while its area is in view") {
            spawner(margin)
            frame()
            frame(duringUpdate = {
                made[0].destroy()
                spawnsMan.rearm()
            })
            frame()

            made.size shouldBe 2
        }

        it("spawns once when it is not respawnable") {
            spawner(margin, respawnable = false)
            killInView()

            moveTo(farOut)
            moveTo(inView)
            spawnsMan.rearm()
            frame()

            made.size shouldBe 1
        }
    }

    describe("the rearm_margin map property") {
        it("is 0 when the object does not set it") {
            SpawnersLayerBuilder.rearmMargin(Properties(), "Met") shouldBe 0f
        }

        it("gives the margin in world units") {
            SpawnersLayerBuilder.rearmMargin(props(ConstKeys.REARM_MARGIN pairTo 6f), "Met") shouldBe
                6f * ConstVals.PPM
        }

        it("throws for a negative value, with the object name and the key in the message") {
            val e = shouldThrow<IllegalArgumentException> {
                SpawnersLayerBuilder.rearmMargin(props(ConstKeys.REARM_MARGIN pairTo -1f, ConstKeys.ID pairTo 7), "Met")
            }
            e.message!! shouldContain "Met"
            e.message!! shouldContain "id=7"
            e.message!! shouldContain ConstKeys.REARM_MARGIN
        }
    }
})
