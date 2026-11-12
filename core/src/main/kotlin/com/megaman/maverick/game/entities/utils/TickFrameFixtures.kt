package com.megaman.maverick.game.entities.utils

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.drawables.shapes.IDrawableShape
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.world.body.FixtureType

class TickFrameFixtures(private val body: Body, private val type: FixtureType, count: Int) {

    val fixtures = Array<Fixture>(count)

    private val shapes = Array<GameRectangle>(count)
    private val box = GameRectangle()
    private val center = Vector2()

    init {
        repeat(count) {
            val shape = GameRectangle()
            val fixture = Fixture(body, type, shape)
            shapes.add(shape)
            fixtures.add(fixture)
            body.addFixture(fixture)
        }
    }

    fun place(
        def: TickFrameDef,
        boxes: IntArray,
        originX: Float,
        originY: Float,
        facingRight: Boolean,
        active: Boolean
    ) {
        val count = boxes.size / 4
        if (count > fixtures.size) throw IllegalStateException(
            "A frame has $count $type boxes, but the entity made only ${fixtures.size}: make the count from the table"
        )
        body.getCenter(center)
        for (i in 0 until fixtures.size) {
            val fixture = fixtures[i]
            if (i >= count) {
                fixture.setActive(false)
                continue
            }
            TickFrames.placeBox(box, def, boxes, i * 4, originX, originY, facingRight)
            shapes[i].setSize(box.getWidth(), box.getHeight())
            fixture.offsetFromBodyAttachment.set(
                box.getX() + box.getWidth() / 2f - center.x,
                box.getY() + box.getHeight() / 2f - center.y
            )
            fixture.setActive(active)
        }
    }

    fun addDebugShapes(out: Array<() -> IDrawableShape?>) {
        for (i in 0 until fixtures.size) {
            val fixture = fixtures[i]
            out.add { if (fixture.isActive()) fixture else null }
        }
    }
}
