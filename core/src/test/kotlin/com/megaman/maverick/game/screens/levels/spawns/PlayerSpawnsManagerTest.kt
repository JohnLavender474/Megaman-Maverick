package com.megaman.maverick.game.screens.levels.spawns

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.common.shapes.GameRectangle
import com.megaman.maverick.game.ConstKeys
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class PlayerSpawnsManagerTest : DescribeSpec({

    fun spawn(name: String, x: Float, y: Float = 0f, size: Float = 32f): RectangleMapObject {
        val spawn = RectangleMapObject(x, y, size, size)
        spawn.name = name
        return spawn
    }

    fun camera(centerX: Float, centerY: Float = 0f, width: Float = 512f, height: Float = 448f): OrthographicCamera {
        val camera = OrthographicCamera()
        camera.viewportWidth = width
        camera.viewportHeight = height
        camera.position.set(centerX, centerY, 0f)
        return camera
    }

    fun spawns(vararg spawns: RectangleMapObject): Array<RectangleMapObject> {
        val array = Array<RectangleMapObject>()
        spawns.forEach { array.add(it) }
        return array
    }

    val farAway = 100_000f

    describe("set") {
        it("makes the spawn whose name sorts first the current spawn") {
            val manager = PlayerSpawnsManager(camera(farAway))
            manager.set(spawns(spawn("2", 1000f), spawn("0", 0f), spawn("1", 500f)))

            manager.currentSpawnProps!!.get(ConstKeys.NAME) shouldBe "0"
        }

        it("sorts the names as strings, so '10' comes before '2'") {
            val manager = PlayerSpawnsManager(camera(farAway))
            manager.set(spawns(spawn("2", 0f), spawn("10", 1000f)))

            manager.currentSpawnProps!!.get(ConstKeys.NAME) shouldBe "10"
        }
    }

    describe("currentSpawnProps") {
        it("is null before set") {
            PlayerSpawnsManager(camera(farAway)).currentSpawnProps shouldBe null
        }

        it("holds the name, the bounds and the object properties of the current spawn") {
            val start = spawn("0", 64f, 96f)
            start.properties.put(ConstKeys.FACING, "left")
            val manager = PlayerSpawnsManager(camera(farAway))
            manager.set(spawns(start))

            val props = manager.currentSpawnProps!!
            props.get(ConstKeys.NAME) shouldBe "0"
            props.get(ConstKeys.FACING) shouldBe "left"
            val bounds = props.get(ConstKeys.BOUNDS, GameRectangle::class)!!
            bounds.getX() shouldBe 64f
            bounds.getY() shouldBe 96f
        }
    }

    describe("run") {
        it("changes the current spawn when the camera overlaps a remaining spawn") {
            val camera = camera(farAway)
            var changed: Pair<String?, String?>? = null
            val manager = PlayerSpawnsManager(camera) { new, old -> changed = new?.name to old?.name }
            manager.set(spawns(spawn("0", 0f), spawn("1", 2000f)))

            camera.position.set(2000f, 0f, 0f)
            manager.run()

            manager.currentSpawnProps!!.get(ConstKeys.NAME) shouldBe "1"
            changed shouldBe ("1" to "0")
        }

        it("does nothing when the camera overlaps no remaining spawn") {
            var calls = 0
            val manager = PlayerSpawnsManager(camera(farAway)) { _, _ -> calls++ }
            manager.set(spawns(spawn("0", 0f), spawn("1", 2000f)))

            manager.run()

            manager.currentSpawnProps!!.get(ConstKeys.NAME) shouldBe "0"
            calls shouldBe 0
        }

        it("activates each spawn only once") {
            val camera = camera(farAway)
            val manager = PlayerSpawnsManager(camera)
            manager.set(spawns(spawn("0", 0f), spawn("1", 2000f), spawn("2", 4000f)))

            camera.position.set(2000f, 0f, 0f)
            manager.run()
            camera.position.set(4000f, 0f, 0f)
            manager.run()
            camera.position.set(2000f, 0f, 0f)
            manager.run()

            manager.currentSpawnProps!!.get(ConstKeys.NAME) shouldBe "2"
        }

        it("never activates the first spawn again after set") {
            val camera = camera(farAway)
            val manager = PlayerSpawnsManager(camera)
            manager.set(spawns(spawn("0", 0f), spawn("1", 2000f)))

            camera.position.set(2000f, 0f, 0f)
            manager.run()
            camera.position.set(0f, 0f, 0f)
            manager.run()

            manager.currentSpawnProps!!.get(ConstKeys.NAME) shouldBe "1"
        }
    }

    describe("startAt") {
        it("makes the given spawn the current spawn") {
            val start = spawn("2", 4000f)
            val manager = PlayerSpawnsManager(camera(farAway))
            manager.set(spawns(spawn("0", 0f), spawn("1", 2000f), start))

            manager.startAt(start)

            manager.currentSpawnProps!!.get(ConstKeys.NAME) shouldBe "2"
        }

        it("keeps spawns that sort before the start spawn from activating") {
            val start = spawn("2", 4000f)
            val camera = camera(farAway)
            val manager = PlayerSpawnsManager(camera)
            manager.set(spawns(spawn("0", 0f), spawn("1", 2000f), start))
            manager.startAt(start)

            camera.position.set(0f, 0f, 0f)
            manager.run()
            camera.position.set(2000f, 0f, 0f)
            manager.run()

            manager.currentSpawnProps!!.get(ConstKeys.NAME) shouldBe "2"
        }

        it("lets spawns that sort after the start spawn activate") {
            val start = spawn("1", 2000f)
            val camera = camera(farAway)
            val manager = PlayerSpawnsManager(camera)
            manager.set(spawns(spawn("0", 0f), start, spawn("2", 4000f)))
            manager.startAt(start)

            camera.position.set(4000f, 0f, 0f)
            manager.run()

            manager.currentSpawnProps!!.get(ConstKeys.NAME) shouldBe "2"
        }
    }

    describe("remove") {
        it("keeps a removed spawn from activating") {
            val camera = camera(farAway)
            val manager = PlayerSpawnsManager(camera)
            manager.set(spawns(spawn("0", 0f), spawn("4", 2000f), spawn("4", 4000f)))

            manager.remove("4")
            camera.position.set(2000f, 0f, 0f)
            manager.run()

            manager.currentSpawnProps!!.get(ConstKeys.NAME) shouldBe "0"
        }
    }

    describe("reset") {
        it("clears the current spawn and the remaining spawns") {
            val camera = camera(farAway)
            val manager = PlayerSpawnsManager(camera)
            manager.set(spawns(spawn("0", 0f), spawn("1", 2000f)))

            manager.reset()
            camera.position.set(2000f, 0f, 0f)
            manager.run()

            manager.currentSpawnProps shouldBe null
        }
    }
})
