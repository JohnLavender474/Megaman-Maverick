package com.maverick.game.engine

import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.entities.GameEntity
import com.maverick.game.engine.entities.IGameEntity
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class GameEngineTest :
    DescribeSpec({
        describe("GameEngine.spawn") {

            class FilteredEntity(engine: GameEngine, var allowSpawn: Boolean) : GameEntity(engine) {
                var inits = 0
                var initializedAtCanSpawn: Boolean? = null

                override fun init(vararg params: Any) {
                    inits++
                }

                override fun canSpawn(spawnProps: Properties): Boolean {
                    initializedAtCanSpawn = initialized
                    return allowSpawn
                }

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

            it("runs init() before canSpawn on the first spawn request") {
                val entity = FilteredEntity(engine, allowSpawn = true)

                engine.spawn(entity)

                entity.initializedAtCanSpawn shouldBe true
                entity.inits shouldBe 1
            }

            it("runs init() once for a rejected entity, and not again at its next spawn") {
                val entity = FilteredEntity(engine, allowSpawn = false)

                engine.spawn(entity) shouldBe false
                entity.inits shouldBe 1
                entity.allowSpawn = true
                engine.spawn(entity) shouldBe true
                engine.update(0f)

                entity.inits shouldBe 1
            }

            it("should not call onSpawnRejected when the entity is already queued to spawn") {
                val entity = FilteredEntity(engine, allowSpawn = true)

                engine.spawn(entity) shouldBe true
                engine.spawn(entity) shouldBe false
                rejected.shouldBeEmpty()
            }
        }
    })
