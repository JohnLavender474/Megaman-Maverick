package com.megaman.maverick.game.entities.utils

import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.entities.GameEntity
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.screens.levels.tiledmap.TiledMapLoadResult
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.levels.LevelState
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk

private class BoundedEntity : GameEntity(mockk(relaxed = true)), IBodyEntity {

    init {
        addComponent(BodyComponent(Body(BodyType.DYNAMIC, width = 32f, height = 48f)))
    }

    override fun onSpawn(spawnProps: Properties) {}
    override fun onDestroy() {}
}

class LevelBoundsTest : DescribeSpec({

    lateinit var entity: BoundedEntity
    lateinit var levelState: LevelState

    beforeEach {
        entity = BoundedEntity()
        levelState = LevelState()

        val map = TiledMap()
        map.properties.put(ConstKeys.WIDTH, 10)
        map.properties.put(ConstKeys.HEIGHT, 8)
        levelState.tiledMapLoadResult = TiledMapLoadResult(map, ObjectMap(), 0, 0, 0, 0)
    }

    fun isOutside() = entity.isOutsideLevel(levelState, 1.5f, 2f)

    describe("the level bounds") {
        it("has the entity inside the map") {
            entity.body.setPosition(64f, 64f)

            isOutside() shouldBe false
        }

        it("keeps the left limit inside and goes past it") {
            entity.body.setPosition(-48f, 64f)
            isOutside() shouldBe false

            entity.body.setPosition(-48.5f, 64f)
            isOutside() shouldBe true
        }

        it("keeps the bottom limit inside and goes past it") {
            entity.body.setPosition(64f, -64f)
            isOutside() shouldBe false

            entity.body.setPosition(64f, -64.5f)
            isOutside() shouldBe true
        }

        it("keeps the right limit inside and goes past it") {
            entity.body.setPosition(368f - 32f, 64f)
            isOutside() shouldBe false

            entity.body.setPosition(368.5f - 32f, 64f)
            isOutside() shouldBe true
        }

        it("keeps the top limit inside and goes past it") {
            entity.body.setPosition(64f, 320f - 48f)
            isOutside() shouldBe false

            entity.body.setPosition(64f, 320.5f - 48f)
            isOutside() shouldBe true
        }

        it("does not read the map for a left or bottom result") {
            levelState.tiledMapLoadResult = null

            entity.body.setPosition(-100f, 64f)
            isOutside() shouldBe true

            entity.body.setPosition(64f, -100f)
            isOutside() shouldBe true
        }
    }
})
