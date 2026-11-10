package com.megaman.maverick.game.entities

import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.testServices
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

private class LookupTestEntity(services: MegaServices) : MegaGameEntity(services) {

    override fun getType() = EntityType.OTHER
}

class MegaGameEntitiesTest : DescribeSpec({

    lateinit var engine: GameEngine
    lateinit var services: MegaServices

    beforeEach {
        engine = GameEngine()
        services = testServices(engine = engine)
    }

    afterEach { engine.reset() }

    describe("MegaGameEntities lookups") {

        it("give the same empty set for each missing key, with no new set per call") {
            val byTag = MegaGameEntities.getOfTag("NoSuchTag")
            byTag.isEmpty shouldBe true
            MegaGameEntities.getOfTag("NoSuchTag") shouldBeSameInstanceAs byTag
            MegaGameEntities.getOfId(990_001) shouldBeSameInstanceAs byTag
            MegaGameEntities.existsAnyOfId(990_001) shouldBe false
        }

        it("do not let a caller add to the shared empty set") {
            val entity = LookupTestEntity(services)
            shouldThrow<UnsupportedOperationException> { MegaGameEntities.getOfTag("NoSuchTag").add(entity) }
        }

        it("give the entities of a tag and of a map object id while they are spawned") {
            val entity = LookupTestEntity(services)
            entity.spawn(props(ConstKeys.ID pairTo 990_002))
            engine.update(0f)

            MegaGameEntities.getOfTag("LookupTestEntity").contains(entity) shouldBe true
            MegaGameEntities.getOfType(EntityType.OTHER).contains(entity) shouldBe true
            MegaGameEntities.getOfId(990_002).contains(entity) shouldBe true

            entity.destroy()
            engine.update(0f)

            MegaGameEntities.getOfTag("LookupTestEntity").contains(entity) shouldBe false
            MegaGameEntities.existsAnyOfId(990_002) shouldBe false
        }
    }
})
