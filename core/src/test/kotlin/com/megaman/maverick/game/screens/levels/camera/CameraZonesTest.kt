package com.megaman.maverick.game.screens.levels.camera

import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.camera.CameraFollow
import com.maverick.game.engine.camera.CameraLimits
import com.maverick.game.engine.camera.IFocusable
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.shapes.GameRectangle
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.screens.levels.tiled.layers.CameraSectionsLayerBuilder
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

private class ZoneFocus(var x: Float, var y: Float) : IFocusable {
    override fun getFocusBounds() = GameRectangle().setSize(10f, 10f).setCenter(x, y)
    override fun getFocusPosition() = Vector2(x, y)
}

private fun CameraZones.limitsAt(x: Float, y: Float, focus: ZoneFocus): CameraLimits {
    focus.x = x
    focus.y = y
    val limits = CameraLimits()
    limits.x.set(0f, 1000f)
    limits.y.set(0f, 1000f)
    apply(limits)
    return limits
}

private fun rig(vararg zones: CameraZone): Pair<CameraZones, ZoneFocus> {
    val focus = ZoneFocus(0f, 0f)
    val follow = CameraFollow(1f, { 1f })
    follow.setFocus(com.badlogic.gdx.graphics.OrthographicCamera(), focus)
    return CameraZones(follow).also { it.zones = Array.with(*zones) } to focus
}

class CameraZonesTest : DescribeSpec({

    describe("CameraZones") {
        it("leaves the limits alone when no zone holds the focus") {
            val (zones, focus) = rig(CameraZone(Rectangle(100f, 100f, 50f, 50f), maxX = 300f))
            zones.limitsAt(10f, 10f, focus).x.max shouldBe 1000f
        }

        it("sets only the edges that a zone names") {
            val (zones, focus) = rig(CameraZone(Rectangle(0f, 0f, 500f, 500f), maxX = 300f))
            val limits = zones.limitsAt(10f, 10f, focus)
            limits.x.min shouldBe 0f
            limits.x.max shouldBe 300f
            limits.y.max shouldBe 1000f
        }

        it("folds in order, so that a later zone wins per edge") {
            val (zones, focus) = rig(
                CameraZone(Rectangle(0f, 0f, 100f, 100f), minX = 10f, maxX = 300f),
                CameraZone(Rectangle(50f, 0f, 100f, 100f), maxX = 200f)
            )
            val limits = zones.limitsAt(60f, 10f, focus)
            limits.x.min shouldBe 10f
            limits.x.max shouldBe 200f
        }

        it("gives the same limits at a position on any path") {
            val (zones, focus) = rig(
                CameraZone(Rectangle(0f, 0f, 100f, 100f), maxX = 300f),
                CameraZone(Rectangle(50f, 0f, 100f, 100f), maxX = 200f)
            )
            zones.limitsAt(10f, 10f, focus)
            val fromLeft = zones.limitsAt(60f, 10f, focus).x.max
            zones.limitsAt(140f, 10f, focus)
            val fromRight = zones.limitsAt(60f, 10f, focus).x.max
            fromLeft shouldBe fromRight
        }

        it("locks an axis with a range of one view size") {
            val (zones, focus) = rig(CameraZone(Rectangle(0f, 0f, 100f, 100f), minY = 360f, maxY = 440f))
            val limits = zones.limitsAt(10f, 10f, focus)
            limits.y.clamp(0f, 80f) shouldBe 400f
            limits.y.clamp(1000f, 80f) shouldBe 400f
        }

        it("holds the low edge when a rotated view is larger than a locked range") {
            val (zones, focus) = rig(CameraZone(Rectangle(0f, 0f, 100f, 100f), minY = 360f, maxY = 440f))
            val limits = zones.limitsAt(10f, 10f, focus)
            limits.y.clamp(1000f, 100f) shouldBe 410f
        }

        it("lets a later zone beat a range per edge") {
            val (zones, focus) = rig(
                CameraZone(Rectangle(0f, 0f, 100f, 100f), minY = 100f, maxY = 900f),
                CameraZone(Rectangle(0f, 0f, 100f, 100f), minY = 360f, maxY = 440f),
                CameraZone(Rectangle(0f, 0f, 100f, 100f), maxY = 500f)
            )
            val limits = zones.limitsAt(10f, 10f, focus)
            limits.y.min shouldBe 360f
            limits.y.max shouldBe 500f
        }

        it("takes the ease speed of the last zone that sets one") {
            val (zones, focus) = rig(
                CameraZone(Rectangle(0f, 0f, 100f, 100f), easeSpeed = 10f),
                CameraZone(Rectangle(0f, 0f, 100f, 100f), easeSpeed = 20f),
                CameraZone(Rectangle(0f, 0f, 100f, 100f), maxX = 300f)
            )
            zones.limitsAt(10f, 10f, focus).easeSpeed shouldBe 20f
        }
    }

    describe("CameraSectionsLayerBuilder") {
        it("reads zones in order, reads the edge that each property names, and skips other objects") {
            val layer = MapLayer()
            val limit = RectangleMapObject(100f, 200f, 30f, 40f)
            layer.objects.add(limit)
            layer.objects.add(RectangleMapObject(0f, 0f, 32f, 32f).also {
                it.properties.put(ConstKeys.TYPE, "zone")
                it.properties.put("min_x", limit)
                it.properties.put("max_y", limit)
            })
            layer.objects.add(RectangleMapObject(0f, 0f, 32f, 32f).also {
                it.properties.put(ConstKeys.TYPE, "scroll")
            })
            layer.objects.add(RectangleMapObject(32f, 0f, 32f, 32f).also {
                it.properties.put(ConstKeys.TYPE, "zone")
                it.properties.put("max_x", limit)
                it.properties.put("min_y", limit)
            })
            val result = Properties()
            CameraSectionsLayerBuilder().build(layer, result)
            @Suppress("UNCHECKED_CAST")
            val zones = result.get(ConstKeys.CAMERA_SECTIONS) as Array<CameraZone>
            zones.size shouldBe 2
            zones[0].minX shouldBe 100f
            zones[0].maxY shouldBe 240f
            zones[0].maxX shouldBe null
            zones[0].minY shouldBe null
            zones[1].maxX shouldBe 130f
            zones[1].minY shouldBe 200f
        }

        it("reads the ease speed in tiles") {
            val layer = MapLayer()
            layer.objects.add(RectangleMapObject(0f, 0f, 32f, 32f).also {
                it.properties.put(ConstKeys.TYPE, "zone")
                it.properties.put(ConstKeys.SPEED, 2f)
            })
            val result = Properties()
            CameraSectionsLayerBuilder().build(layer, result)
            @Suppress("UNCHECKED_CAST")
            val zones = result.get(ConstKeys.CAMERA_SECTIONS) as Array<CameraZone>
            zones[0].easeSpeed shouldBe 2f * ConstVals.PPM
        }
    }
})
