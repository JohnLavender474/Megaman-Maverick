package com.megaman.maverick.game.entities.utils

import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.maverick.game.engine.world.container.SimpleGridWorldContainer
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.testServices
import com.megaman.maverick.game.world.body.BodyLabel
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.addBodyLabel

class GroundTestOwner(services: MegaServices, x: Float, y: Float) : MegaGameEntity(services), IBodyEntity {

    init {
        val ppm = ConstVals.PPM.toFloat()
        addComponent(BodyComponent(Body(BodyType.DYNAMIC, x * ppm, y * ppm, ppm, ppm)))
    }

    override fun getType() = EntityType.ENEMY
}

class GroundTestWorld {

    private val ppm = ConstVals.PPM.toFloat()
    val container = SimpleGridWorldContainer(ConstVals.PPM)
    val services: MegaServices = testServices(engine = GameEngine()).also { it.levelState.worldContainer = container }

    fun owner(x: Float, y: Float) = GroundTestOwner(services, x, y)

    fun block(x: Float, y: Float, width: Float, height: Float, label: BodyLabel? = null): Body {
        val body = Body(BodyType.STATIC, x * ppm, y * ppm, width * ppm, height * ppm)
        if (label != null) body.addBodyLabel(label)
        container.addBody(body)
        return body
    }

    fun death(x: Float, y: Float, width: Float, height: Float, active: Boolean = true): Fixture {
        val body = Body(BodyType.ABSTRACT, x * ppm, y * ppm, width * ppm, height * ppm)
        val fixture = Fixture(body, FixtureType.DEATH, GameRectangle().setSize(width * ppm, height * ppm), active = active)
        body.addFixture(fixture)
        container.addFixture(fixture)
        return fixture
    }
}
