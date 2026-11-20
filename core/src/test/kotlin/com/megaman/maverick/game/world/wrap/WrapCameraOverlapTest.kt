package com.megaman.maverick.game.world.wrap

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.maverick.game.engine.camera.CameraView
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.levels.LevelState
import com.megaman.maverick.game.testServices
import com.megaman.maverick.game.world.body.defaultDoUpdate
import com.megaman.maverick.game.world.body.overlapsGameCam
import com.megaman.maverick.game.world.body.setEntity
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

private const val ROOM_X = 32f
private const val ROOM_Y = 64f
private const val ROOM_WIDTH = ConstVals.VIEW_WIDTH * ConstVals.PPM
private const val ROOM_HEIGHT = ConstVals.VIEW_HEIGHT * ConstVals.PPM
private const val SIZE = 16f
private const val PAST_RIGHT_X = ROOM_X + ROOM_WIDTH + SIZE
private const val PAST_LEFT_X = ROOM_X - 2f * SIZE
private const val INSIDE_X = ROOM_X + ROOM_WIDTH / 2f
private const val Y = ROOM_Y + ROOM_HEIGHT / 2f

private class PlainEntity(services: MegaServices) : MegaGameEntity(services) {

    override fun getType() = EntityType.ENEMY
}

class WrapCameraOverlapTest : DescribeSpec({

    fun servicesIn(wrap: Boolean?): MegaServices {
        val camera = OrthographicCamera().apply {
            viewportWidth = ROOM_WIDTH
            viewportHeight = ROOM_HEIGHT
            position.set(ROOM_X + ROOM_WIDTH / 2f, ROOM_Y + ROOM_HEIGHT / 2f, 0f)
        }
        val room = RectangleMapObject(ROOM_X, ROOM_Y, ROOM_WIDTH, ROOM_HEIGHT).also {
            it.name = "arena"
            if (wrap != null) it.properties.put(ConstKeys.WRAP, wrap)
        }
        val levelState = LevelState().apply { currentRoomSupplier = { room } }
        return testServices(levelState = levelState, gameCamera = CameraView(camera))
    }

    fun shotAt(services: MegaServices, x: Float) = Body(BodyType.ABSTRACT, x, Y, SIZE, SIZE)
        .also { it.setEntity(Bullet(services)) }

    fun plainAt(services: MegaServices, x: Float) = Body(BodyType.ABSTRACT, x, Y, SIZE, SIZE)
        .also { it.putProperty(ConstKeys.ENTITY, PlainEntity(services)) }

    describe("in a wrap room") {
        val services = servicesIn(wrap = true)

        it("sees a shot fully past the right edge, so a still shot updates") {
            val body = shotAt(services, PAST_RIGHT_X)
            body.overlapsGameCam() shouldBe true
            body.defaultDoUpdate() shouldBe true
        }

        it("sees a shot fully past the left edge") {
            shotAt(services, PAST_LEFT_X).overlapsGameCam() shouldBe true
        }

        it("does not see a shot at its cap past the edge it did not come in through") {
            val body = shotAt(services, PAST_RIGHT_X).apply { wrapBy(-ROOM_WIDTH); setX(PAST_RIGHT_X) }
            body.overlapsGameCam() shouldBe false
        }

        it("does not see an entity that does not wrap past the edge") {
            val body = plainAt(services, PAST_RIGHT_X)
            body.overlapsGameCam() shouldBe false
            body.defaultDoUpdate() shouldBe false
        }

        it("sees a shot and an entity that does not wrap inside the view") {
            shotAt(services, INSIDE_X).overlapsGameCam() shouldBe true
            plainAt(services, INSIDE_X).overlapsGameCam() shouldBe true
        }
    }

    for (wrap in listOf(false, null)) describe("in a room with wrap=$wrap") {
        val services = servicesIn(wrap = wrap)

        it("does not see a shot past either edge, so a still shot does not update") {
            val right = shotAt(services, PAST_RIGHT_X)
            right.overlapsGameCam() shouldBe false
            right.defaultDoUpdate() shouldBe false
            shotAt(services, PAST_LEFT_X).overlapsGameCam() shouldBe false
        }

        it("does not see an entity that does not wrap past the edge") {
            plainAt(services, PAST_RIGHT_X).overlapsGameCam() shouldBe false
        }

        it("sees a shot and an entity that does not wrap inside the view") {
            shotAt(services, INSIDE_X).overlapsGameCam() shouldBe true
            plainAt(services, INSIDE_X).overlapsGameCam() shouldBe true
        }
    }

    describe("with no current room") {
        val services = testServices(
            levelState = LevelState().apply { currentRoomSupplier = { null } },
            gameCamera = CameraView(OrthographicCamera().apply {
                viewportWidth = ROOM_WIDTH
                viewportHeight = ROOM_HEIGHT
                position.set(ROOM_X + ROOM_WIDTH / 2f, ROOM_Y + ROOM_HEIGHT / 2f, 0f)
            })
        )

        it("does not see a shot past the edge") {
            shotAt(services, PAST_RIGHT_X).overlapsGameCam() shouldBe false
        }
    }
})
