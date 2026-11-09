package com.megaman.maverick.game.screens.levels.camera

import com.maverick.game.engine.camera.CameraLimitStage
import com.maverick.game.engine.camera.CameraRoomBounds
import com.maverick.game.engine.camera.IRoomSubject
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.maverick.game.engine.camera.CameraController
import com.maverick.game.engine.camera.CameraFollow
import com.maverick.game.engine.camera.CameraRoomTransition
import com.maverick.game.engine.camera.CameraRotation
import com.maverick.game.engine.camera.CameraShake
import com.maverick.game.engine.camera.CameraView
import com.maverick.game.engine.camera.IFocusable
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.shapes.GameRectangle
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

private class ScrollFocus(var x: Float, var y: Float) : IFocusable, IRoomSubject {
    override fun getSubjectBounds() = GameRectangle().setSize(10f, 10f).setCenter(x, y)
    override fun getFocusPosition() = Vector2(x, y)
}

private class ScrollRig(
    section: CameraScrollSection?,
    x: Float = 300f,
    y: Float = 200f,
    roomList: Array<RectangleMapObject> = Array.with(room("a", 0f, 0f, 3000f, 3000f))
) {
    val camera = OrthographicCamera().also {
        it.viewportWidth = 100f
        it.viewportHeight = 80f
    }
    val rotation = CameraRotation().also { it.snap(Direction.UP) }
    val view = CameraView(camera, rotation)
    val focus = ScrollFocus(x, y)
    val follow = CameraFollow(1f, { 1f })
    val transition = CameraRoomTransition(20f, 0.1f, 0.1f)
    val roomBounds = CameraRoomBounds(follow, transition, Vector2(50f, 50f)).also { it.gameRooms = roomList }
    var held = false
    var stageHeld = false
    var fall = Direction.DOWN
    val shake = CameraShake(1f)
    val controller = CameraController(camera)
    val scroll = CameraScroll(view, follow, roomBounds, controller.settledPosition, { fall }, { held })
    val stage = CameraLimitStage(view, follow, roomBounds, controller.settledPosition, 100f, { stageHeld })

    init {
        if (section != null) {
            scroll.sections = Array.with(section)
            stage.addRule(scroll)
            controller.addMover(roomBounds)
            controller.addMover(transition)
            controller.addMover(follow)
            controller.addMover(scroll)
        } else {
            controller.addMover(roomBounds)
            controller.addMover(transition)
            controller.addMover(follow)
        }
        controller.addMover(stage)
        controller.addMover(CameraRoomClamp(view, stage))
        controller.addMover(CameraRoomScan(roomBounds))
        controller.addEffect(shake)
        follow.setFocus(camera, focus.also { roomBounds.subject = it })
        controller.reset()
        controller.update(0.01f)
    }

    fun run(frames: Int, delta: Float = 0.1f) = repeat(frames) { controller.update(delta) }
}

private fun room(name: String, x: Float, y: Float, width: Float, height: Float) =
    RectangleMapObject(x, y, width, height).also { it.name = name }

private fun right(arrive: Boolean = false, edges: ObjectSet<Direction>? = null) =
    CameraScrollSection(Rectangle(200f, 0f, 500f, 3000f), Direction.RIGHT, 100f, arrive, edges)

class CameraScrollTest : DescribeSpec({

    describe("CameraScroll") {
        it("starts from the camera position, with no jump") {
            val rig = ScrollRig(right())
            rig.run(1, 0f)
            rig.camera.position.x shouldBe 300f
        }

        it("does not start while the focus is outside the section") {
            val rig = ScrollRig(right(), x = 100f)
            rig.run(5)
            rig.camera.position.x shouldBe 100f
        }

        it("moves at the section speed, whatever the focus does") {
            val rig = ScrollRig(right())
            rig.run(1, 0f)
            rig.run(10)
            rig.camera.position.x shouldBe (400f plusOrMinus 0.01f)
            rig.focus.x = 250f
            rig.run(10)
            rig.camera.position.x shouldBe (500f plusOrMinus 0.01f)
        }

        it("moves on each axis in each direction, and the other axis follows") {
            val cases = listOf(
                Direction.LEFT to (-100f to 0f),
                Direction.RIGHT to (100f to 0f),
                Direction.UP to (0f to 100f),
                Direction.DOWN to (0f to -100f)
            )
            cases.forEach { (direction, change) ->
                val rig = ScrollRig(CameraScrollSection(Rectangle(0f, 0f, 3000f, 3000f), direction, 100f), 1500f, 1500f)
                rig.run(1, 0f)
                val startX = rig.camera.position.x
                val startY = rig.camera.position.y
                rig.run(10)
                rig.camera.position.x shouldBe (startX + change.first plusOrMinus 0.01f)
                rig.camera.position.y shouldBe (startY + change.second plusOrMinus 0.01f)
            }
        }

        it("keeps the follow on the other axis") {
            val rig = ScrollRig(right())
            rig.run(1, 0f)
            rig.focus.y = 400f
            rig.run(10)
            rig.camera.position.y shouldBe 400f
        }

        it("does not add a shake to the scroll position") {
            val rig = ScrollRig(right())
            rig.run(1, 0f)
            rig.shake.startShake(5f, 0.01f, 30f, 0f)
            rig.run(10)
            rig.stage.limits.x.min shouldBe (350f plusOrMinus 0.01f)
        }

        it("waits during a hold") {
            val rig = ScrollRig(right())
            rig.run(1, 0f)
            rig.held = true
            rig.run(10)
            rig.camera.position.x shouldBe 300f
        }

        it("writes the pin to the limits during a stage hold") {
            val rig = ScrollRig(right())
            rig.run(1, 0f)
            rig.stageHeld = true
            rig.run(10)
            rig.camera.position.x shouldBe (400f plusOrMinus 0.01f)
            rig.stage.limits.x.min shouldBe (350f plusOrMinus 0.01f)
        }

        it("keeps the pinned axis on the scroll position through a side turn") {
            val rig = ScrollRig(right())
            rig.controller.addEffect(rig.rotation)
            rig.run(1, 0f)
            rig.held = true
            rig.stageHeld = true
            rig.focus.x = 800f
            val x = rig.camera.position.x
            rig.rotation.start(Direction.LEFT, 1f)
            repeat(12) {
                rig.controller.update(0.1f)
                rig.camera.position.x shouldBe (x plusOrMinus 0.01f)
            }
        }

        it("stops in mode exit when the trailing edge reaches the far edge, and keeps the trailing limit") {
            val rig = ScrollRig(right())
            rig.run(1, 0f)
            rig.focus.x = 1000f
            rig.run(60)
            rig.stage.limits.x.min shouldBe 700f
            rig.camera.position.x shouldBe 1000f
            rig.stage.edgeLimit(Direction.LEFT) shouldBe 700f
            rig.stage.edgeLimit(Direction.RIGHT).shouldBeNull()
        }

        it("stops in mode arrive when the leading edge reaches the far edge") {
            val rig = ScrollRig(right(arrive = true))
            rig.run(1, 0f)
            rig.focus.x = 1000f
            rig.run(60)
            rig.stage.limits.x.min shouldBe 600f
        }

        it("does not start a done section again") {
            val rig = ScrollRig(right())
            rig.run(1, 0f)
            rig.focus.x = 1000f
            rig.run(60)
            rig.focus.x = 720f
            rig.run(5)
            rig.focus.x = 690f
            rig.run(20)
            rig.camera.position.x shouldBe 750f
        }

        it("has edge blocks on both edges, and a section can override them") {
            val rig = ScrollRig(right())
            rig.run(2)
            rig.stage.edgeLimit(Direction.RIGHT) shouldBe rig.stage.limits.x.max
            rig.stage.edgeLimit(Direction.LEFT) shouldBe rig.stage.limits.x.min
            val edges = ObjectSet<Direction>().also { it.add(Direction.RIGHT) }
            val custom = ScrollRig(right(edges = edges))
            custom.run(2)
            custom.stage.edgeLimit(Direction.LEFT).shouldBeNull()
            custom.stage.edgeLimit(Direction.RIGHT) shouldBe custom.stage.limits.x.max
        }

        it("waits at a room edge, turns the leading block off, and keeps the trailing block") {
            val rig = ScrollRig(
                right(), roomList = Array.with(room("a", 0f, 0f, 500f, 3000f))
            )
            rig.run(1, 0f)
            rig.run(30)
            rig.camera.position.x shouldBe 450f
            rig.stage.edgeLimit(Direction.RIGHT).shouldBeNull()
            rig.stage.edgeLimit(Direction.LEFT) shouldBe 400f
        }

        it("continues from the camera after a pan if the section holds the focus") {
            val rig = ScrollRig(
                CameraScrollSection(Rectangle(200f, 0f, 800f, 3000f), Direction.RIGHT, 100f),
                roomList = Array.with(room("a", 0f, 0f, 500f, 3000f), room("b", 500f, 0f, 1500f, 3000f))
            )
            rig.run(1, 0f)
            rig.run(20)
            rig.camera.position.x shouldBe 450f
            rig.focus.x = 497f
            rig.run(1, 0.01f)
            rig.transition.transitioning shouldBe true
            while (rig.transition.transitioning) rig.controller.update(0.01f)
            rig.focus.x = 520f
            rig.run(1, 0f)
            val x = rig.camera.position.x
            rig.run(10)
            rig.camera.position.x shouldBe (x + 100f plusOrMinus 0.01f)
        }

        it("kills at a trailing edge in the direction of the fall, in place of a block") {
            val section = CameraScrollSection(Rectangle(0f, 0f, 3000f, 3000f), Direction.UP, 100f)
            val rig = ScrollRig(section, 1500f, 1500f)
            rig.run(5)
            rig.stage.edgeLimit(Direction.DOWN).shouldBeNull()
            rig.stage.killEdge(Direction.DOWN) shouldBe rig.stage.limits.y.min
            val rotated = ScrollRig(section, 1500f, 1500f).also { it.fall = Direction.LEFT }
            rotated.run(5)
            rotated.stage.edgeLimit(Direction.DOWN) shouldBe rotated.stage.limits.y.min
        }

        it("has no block on a leading edge in the direction of the fall, and kills past it") {
            val section = CameraScrollSection(Rectangle(0f, 0f, 3000f, 3000f), Direction.DOWN, 100f)
            val rig = ScrollRig(section, 1500f, 1500f)
            rig.run(5)
            rig.stage.edgeLimit(Direction.DOWN).shouldBeNull()
            rig.stage.killEdge(Direction.DOWN) shouldBe rig.stage.limits.y.min
            rig.stage.edgeLimit(Direction.UP) shouldBe rig.stage.limits.y.max
        }

        it("does not kill past a leading fall edge at a room edge") {
            val section = CameraScrollSection(Rectangle(0f, 0f, 3000f, 3000f), Direction.DOWN, 100f)
            val rig = ScrollRig(section, 1500f, 100f)
            rig.run(30)
            rig.stage.limits.y.min shouldBe 0f
            rig.stage.killEdge(Direction.DOWN).shouldBeNull()
        }

        it("gives a block and no kill to a fall edge that edge_blocks lists") {
            val edges = ObjectSet<Direction>().also { it.add(Direction.DOWN) }
            val section = CameraScrollSection(Rectangle(0f, 0f, 3000f, 3000f), Direction.DOWN, 100f, edgeBlocks = edges)
            val rig = ScrollRig(section, 1500f, 1500f)
            rig.run(5)
            rig.stage.edgeLimit(Direction.DOWN) shouldBe rig.stage.limits.y.min
            rig.stage.killEdge(Direction.DOWN).shouldBeNull()
            rig.stage.edgeLimit(Direction.UP).shouldBeNull()
            rig.stage.killEdge(Direction.UP).shouldBeNull()
        }

        it("keeps a fall kill on a fall edge that edge_blocks leaves out") {
            val section = CameraScrollSection(Rectangle(0f, 0f, 3000f, 3000f), Direction.UP, 100f, edgeBlocks = ObjectSet())
            val rig = ScrollRig(section, 1500f, 1500f)
            rig.run(5)
            rig.stage.killEdge(Direction.DOWN) shouldBe rig.stage.limits.y.min
        }

        it("matches the case table for each scroll direction and fall direction") {
            Direction.entries.forEach { scroll ->
                Direction.entries.forEach { fall ->
                    val section = CameraScrollSection(Rectangle(0f, 0f, 3000f, 3000f), scroll, 100f)
                    val rig = ScrollRig(section, 1500f, 1500f).also { it.fall = fall }
                    rig.run(5)
                    val leading = rig.stage.edgeLimit(scroll)
                    val trailing = rig.stage.edgeLimit(scroll.getOpposite())
                    when (fall) {
                        scroll -> {
                            leading.shouldBeNull()
                            (rig.stage.killEdge(scroll) != null) shouldBe true
                            (trailing != null) shouldBe true
                        }
                        scroll.getOpposite() -> {
                            (leading != null) shouldBe true
                            trailing.shouldBeNull()
                            (rig.stage.killEdge(scroll.getOpposite()) != null) shouldBe true
                        }
                        else -> {
                            (leading != null) shouldBe true
                            (trailing != null) shouldBe true
                            rig.stage.killEdge(scroll).shouldBeNull()
                        }
                    }
                }
            }
        }

        it("kills past the trailing edge while it runs, and not after the stop") {
            val rig = ScrollRig(right())
            rig.run(2)
            rig.stage.killEdge(Direction.LEFT) shouldBe rig.stage.limits.x.min
            rig.focus.x = 1000f
            rig.run(60)
            rig.stage.killEdge(Direction.LEFT).shouldBeNull()
        }

        it("gives the section as the edge extent, also after the stop") {
            val section = CameraScrollSection(Rectangle(200f, 100f, 500f, 400f), Direction.RIGHT, 100f)
            val rig = ScrollRig(section)
            rig.run(2)
            rig.stage.edgeExtent(Direction.RIGHT) shouldBe section.bounds
            rig.stage.edgeExtent(Direction.LEFT) shouldBe section.bounds
            rig.focus.x = 690f
            rig.run(60)
            rig.stage.edgeExtent(Direction.LEFT) shouldBe section.bounds
        }

        it("gives its crush surfaces only while it runs") {
            val section = right()
            section.crushSurfaces.add(GameRectangle().setSize(10f))
            val rig = ScrollRig(section)
            rig.run(2)
            rig.stage.crushSurfaces.size shouldBe 1
            rig.focus.x = 1000f
            rig.run(60)
            rig.stage.crushSurfaces.size shouldBe 0
        }

        it("clears on a reset, and counts a section as done if the camera is past its stop") {
            val rig = ScrollRig(right())
            rig.run(1, 0f)
            rig.focus.x = 1000f
            rig.run(60)
            rig.focus.x = 300f
            rig.controller.reset()
            rig.run(2)
            rig.stage.edgeLimit(Direction.LEFT) shouldBe rig.stage.limits.x.min
            rig.camera.position.x shouldBe (300f plusOrMinus 20f)

            val past = ScrollRig(right(), x = 1200f)
            past.run(3)
            past.stage.limits.x.min shouldBe 700f
            past.stage.edgeLimit(Direction.LEFT) shouldBe 700f
        }

        it("gives the same result with no section as with no scroll part") {
            val without = ScrollRig(null)
            val with = ScrollRig(null).also { it.stage.addRule(it.scroll) }
            for (x in floatArrayOf(50f, 300f, 1200f, 90f)) {
                without.focus.x = x
                with.focus.x = x
                repeat(30) {
                    without.controller.update(0.02f)
                    with.controller.update(0.02f)
                    with.camera.position.x shouldBe without.camera.position.x
                    with.stage.limits.x.max shouldBe without.stage.limits.x.max
                }
            }
        }
    }

    describe("CameraScroll room scope") {
        fun up(x: Float) = CameraScrollSection(Rectangle(x, 0f, 500f, 400f), Direction.UP, 100f)

        it("does not count a section in another room as done after a respawn") {
            val rig = ScrollRig(
                up(2000f), 300f, 900f,
                Array.with(room("a", 0f, 0f, 1000f, 1000f))
            )
            rig.run(3)
            rig.stage.killEdge(Direction.DOWN).shouldBeNull()
            rig.stage.limits.y.min shouldBe 0f
        }

        it("does not apply a kept edge in a room that the section does not overlap") {
            val rig = ScrollRig(
                CameraScrollSection(Rectangle(200f, 0f, 300f, 1000f), Direction.RIGHT, 100f),
                300f, 500f,
                Array.with(room("a", 0f, 0f, 1000f, 1000f), room("b", 1000f, 0f, 1000f, 1000f))
            )
            rig.run(1, 0f)
            rig.focus.x = 900f
            rig.run(60)
            rig.stage.edgeLimit(Direction.LEFT) shouldBe 500f
            rig.focus.x = 997f
            rig.run(1, 0.01f)
            while (rig.transition.transitioning) rig.controller.update(0.01f)
            rig.focus.x = 1100f
            rig.run(3, 0.01f)
            rig.stage.edgeLimit(Direction.LEFT).shouldBeNull()
        }

        it("gives no edge at the end of a pan, while the limits are from the last room") {
            val rig = ScrollRig(
                CameraScrollSection(Rectangle(0f, 0f, 1000f, 2000f), Direction.UP, 100f),
                500f, 1500f,
                Array.with(room("a", 0f, 0f, 1000f, 1000f), room("b", 0f, 1000f, 1000f, 1000f))
            )
            rig.run(2, 0f)
            rig.stage.killEdge(Direction.DOWN) shouldBe rig.stage.limits.y.min
            rig.focus.y = 1003f
            rig.held = true
            rig.run(1, 0.01f)
            rig.transition.transitioning shouldBe true
            while (rig.transition.transitioning) rig.controller.update(0.01f)
            rig.stage.limits.y.min shouldBe (1460f plusOrMinus 100f)
            rig.stage.killEdge(Direction.DOWN).shouldBeNull()
        }
    }

    describe("CameraScroll chains") {
        it("starts next when a section stops, with no position test, and only one way") {
            val up = CameraScrollSection(Rectangle(600f, 0f, 500f, 3000f), Direction.UP, 100f)
            val first = CameraScrollSection(Rectangle(200f, 0f, 400f, 3000f), Direction.RIGHT, 100f, arrive = true, next = up)
            val rig = ScrollRig(first)
            rig.scroll.sections = Array.with(first, up)
            rig.run(1, 0f)
            rig.run(35)
            rig.stage.limits.x.min shouldBe 500f
            val y = rig.camera.position.y
            rig.run(10)
            rig.camera.position.y shouldBe (y + 100f plusOrMinus 0.01f)
            rig.stage.edgeLimit(Direction.LEFT) shouldBe 500f
            rig.stage.killEdge(Direction.DOWN) shouldBe rig.stage.limits.y.min
        }

        it("starts an overlapping section with no next after the first stops, in Tiled order") {
            val first = CameraScrollSection(Rectangle(200f, 0f, 400f, 3000f), Direction.RIGHT, 100f, arrive = true)
            val second = CameraScrollSection(Rectangle(500f, 0f, 500f, 3000f), Direction.RIGHT, 50f)
            val third = CameraScrollSection(Rectangle(500f, 0f, 500f, 3000f), Direction.RIGHT, 10f)
            val rig = ScrollRig(first)
            rig.scroll.sections = Array.with(first, second, third)
            rig.run(1, 0f)
            rig.run(25)
            val x = rig.camera.position.x
            rig.run(10)
            rig.camera.position.x shouldBe (x + 50f plusOrMinus 0.01f)
        }
    }

    describe("isPastCameraEdge") {
        it("needs the whole body past the edge") {
            val bounds = GameRectangle().setSize(10f, 10f).setCenter(100f, 100f)
            isPastCameraEdge(bounds, Direction.DOWN, 106f) shouldBe true
            isPastCameraEdge(bounds, Direction.DOWN, 100f) shouldBe false
            isPastCameraEdge(bounds, Direction.UP, 94f) shouldBe true
            isPastCameraEdge(bounds, Direction.LEFT, 106f) shouldBe true
            isPastCameraEdge(bounds, Direction.RIGHT, 94f) shouldBe true
        }
    }
})
