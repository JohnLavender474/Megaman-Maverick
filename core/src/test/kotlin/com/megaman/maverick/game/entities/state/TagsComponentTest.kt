package com.megaman.maverick.game.entities.state

import com.badlogic.gdx.utils.ObjectSet
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.components.MissingComponentException
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.testServices
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

private open class TagParent(services: MegaServices) : MegaGameEntity(services) {

    companion object {
        const val TAG = "TagParent"
    }

    override fun getType() = EntityType.OTHER

    override fun defineTagsComponent(): TagsComponent {
        val component = super.defineTagsComponent()
        component.add(TAG)
        return component
    }
}

private class TagChild(services: MegaServices) : TagParent(services)

private class TagPlain(services: MegaServices) : MegaGameEntity(services) {
    override fun getType() = EntityType.OTHER
}

private fun TagsComponent.toList() = (0 until size).map { get(it) }

class TagsComponentTest : DescribeSpec({

    val services = testServices()

    describe("TagsComponent") {
        it("keeps the tags in order and drops a repeated tag") {
            TagsComponent("A", "B", "A").toList() shouldBe listOf("A", "B")
        }

        it("needs at least 1 tag") {
            shouldThrow<IllegalArgumentException> { TagsComponent() }
        }

        it("adds a tag at the end, once") {
            val tags = TagsComponent("A")
            tags.add("B")
            tags.add("A")
            tags.toList() shouldBe listOf("A", "B")
        }

        it("keeps its tags after a reset") {
            val tags = TagsComponent("A", "B")
            tags.reset()
            tags.toList() shouldBe listOf("A", "B")
        }
    }

    describe("the tags that init() gives") {
        it("is the runtime class name by default") {
            val entity = TagPlain(services).also { it.init() }

            entity.tags.toList() shouldBe listOf("TagPlain")
        }

        it("gives a subclass its own name, then the tag that the base override adds") {
            val child = TagChild(services).also { it.init() }
            val parent = TagParent(services).also { it.init() }

            child.tags.toList() shouldBe listOf("TagChild", "TagParent")
            parent.tags.toList() shouldBe listOf("TagParent")
        }
    }

    describe("the accessors") {
        it("match any tag of the entity") {
            val child = TagChild(services).also { it.init() }

            child.hasTag("TagParent") shouldBe true
            child.hasTag("tagchild") shouldBe false
            child.hasTag("tagchild", ignoreCase = true) shouldBe true
            child.hasAnyTag(ObjectSet<String>().also { it.add("Other"); it.add("TagChild") }) shouldBe true
            child.hasAnyTag(ObjectSet<String>().also { it.add("Other") }) shouldBe false
        }

        it("throw for an entity with no tags component") {
            val entity = TagPlain(services)

            shouldThrow<MissingComponentException> { entity.hasTag("TagPlain") }
        }
    }

    describe("the MegaGameEntities index") {
        it("finds an entity under each of its tags, and removes it from each") {
            val child = TagChild(services).also { it.init() }

            child.onSpawn(Properties())
            MegaGameEntities.getOfTag("TagChild").contains(child) shouldBe true
            MegaGameEntities.getOfTag("TagParent").contains(child) shouldBe true

            child.onDestroy()
            MegaGameEntities.getOfTag("TagChild").contains(child) shouldBe false
            MegaGameEntities.getOfTag("TagParent").contains(child) shouldBe false
        }
    }
})
