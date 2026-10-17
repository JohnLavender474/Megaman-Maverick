package com.mega.game.engine

import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.entities.GameEntity
import com.mega.game.engine.entities.IGameEntity
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class GameEngineTest :
    DescribeSpec({
        describe("GameEngine.spawn") {

            class FilteredEntity(engine: GameEngine, var allowSpawn: Boolean) : GameEntity(engine) {
                override fun canSpawn(spawnProps: Properties) = allowSpawn

                override fun onSpawn(spawnProps: Properties) {}

                override fun onDestroy() {}
            }

            lateinit var engine: GameEngine
            lateinit var rejected: MutableList<IGameEntity>

            beforeEach {
                rejected = mutableListOf()
                engine = GameEngine(onSpawnRejected = { rejected.add(it) })
            }

            it("should call onSpawnRejected when canSpawn returns false") {
                val entity = FilteredEntity(engine, allowSpawn = false)

                engine.spawn(entity) shouldBe false
                rejected shouldContainExactly listOf(entity)
            }

            it("should not call onSpawnRejected when the entity is queued to spawn") {
                val entity = FilteredEntity(engine, allowSpawn = true)

                engine.spawn(entity) shouldBe true
                rejected.shouldBeEmpty()
            }

            it("should not call onSpawnRejected when the entity is already queued to spawn") {
                val entity = FilteredEntity(engine, allowSpawn = true)

                engine.spawn(entity) shouldBe true
                engine.spawn(entity) shouldBe false
                rejected.shouldBeEmpty()
            }
        }
    })
