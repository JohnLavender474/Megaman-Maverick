package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.state.GameState
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

private class LaserIgnoreTestEntity(game: MegamanMaverickGame) : MegaGameEntity(game) {
    override fun getType() = EntityType.OTHER
}

class LaserIgnoreTest : DescribeSpec({

    lateinit var engine: GameEngine
    lateinit var game: MegamanMaverickGame

    beforeEach {
        engine = GameEngine()
        game = mockk(relaxed = true)
        every { game.engine } returns engine
        every { game.state } returns GameState()
        every { game.getCurrentLevel() } returns LevelDefinition.TEST_1
        val atlas = mockk<TextureAtlas>()
        every { atlas.findRegion(any()) } returns AtlasRegion(mockk<Texture>(relaxed = true), 0, 0, 64, 64)
        every { game.assMan.get(any<String>(), TextureAtlas::class.java) } returns atlas
        MegaEntityFactory.init(game)
    }

    afterEach {
        engine.reset()
        MegaEntityFactory.reset()
    }

    fun entity(props: Properties = Properties()): LaserIgnoreTestEntity {
        val entity = LaserIgnoreTestEntity(game)
        entity.spawn(props)
        engine.update(0f)
        return entity
    }

    fun ignoreObject(id: Int) = RectangleMapObject().also { it.properties.put(ConstKeys.ID, id) }

    fun laser(owner: MegaGameEntity, vararg ignoreIds: Int): Laser {
        val laser = Laser(game)
        val laserProps = props(ConstKeys.OWNER pairTo owner, ConstKeys.RADIUS pairTo 1f)
        ignoreIds.forEachIndexed { i, id -> laserProps.put("${ConstKeys.IGNORE}_$i", ignoreObject(id)) }
        laser.spawn(laserProps)
        engine.update(0f)
        return laser
    }

    fun reflectingLaser(parent: Laser, reflector: MegaGameEntity): Laser {
        val laser = Laser(game)
        laser.spawn(props(ConstKeys.PARENT pairTo parent, ConstKeys.ACTIVE pairTo false, ConstKeys.RADIUS pairTo 1f))
        engine.update(0f)
        laser.setReflector(reflector, parent)
        return laser
    }

    describe("Laser.isLaserIgnoring") {

        it("ignores a runtime owner by reference, and not other runtime entities") {
            val owner = entity()
            val other = entity()

            val laser = laser(owner)

            laser.isLaserIgnoring(owner) shouldBe true
            laser.isLaserIgnoring(other) shouldBe false
        }

        it("ignores a map owner and every entity with the owner's map id") {
            val owner = entity(props(ConstKeys.ID pairTo 7301))
            val sharesId = entity(props(ConstKeys.ID pairTo 7301))
            val other = entity(props(ConstKeys.ID pairTo 7302))

            val laser = laser(owner)

            laser.isLaserIgnoring(owner) shouldBe true
            laser.isLaserIgnoring(sharesId) shouldBe true
            laser.isLaserIgnoring(other) shouldBe false
        }

        it("ignores the entities of its Tiled ignore objects by map id") {
            val owner = entity()
            val ignored = entity(props(ConstKeys.ID pairTo 7303))
            val other = entity(props(ConstKeys.ID pairTo 7304))

            val laser = laser(owner, 7303)

            laser.isLaserIgnoring(ignored) shouldBe true
            laser.isLaserIgnoring(other) shouldBe false
        }

        it("ignores in a reflecting laser the reflector, its map id, and what the parent ignores") {
            val owner = entity()
            val tiled = entity(props(ConstKeys.ID pairTo 7305))
            val reflector = entity(props(ConstKeys.ID pairTo 7306))
            val sharesReflectorId = entity(props(ConstKeys.ID pairTo 7306))
            val runtimeReflector = entity()
            val other = entity()
            val parent = laser(owner, 7305)

            val reflecting = reflectingLaser(parent, reflector)

            reflecting.isLaserIgnoring(reflector) shouldBe true
            reflecting.isLaserIgnoring(sharesReflectorId) shouldBe true
            reflecting.isLaserIgnoring(owner) shouldBe true
            reflecting.isLaserIgnoring(tiled) shouldBe true
            reflecting.isLaserIgnoring(other) shouldBe false

            reflecting.setReflector(runtimeReflector, parent)

            reflecting.isLaserIgnoring(runtimeReflector) shouldBe true
            reflecting.isLaserIgnoring(reflector) shouldBe false
            reflecting.isLaserIgnoring(sharesReflectorId) shouldBe false
            reflecting.isLaserIgnoring(owner) shouldBe true
        }
    }
})
