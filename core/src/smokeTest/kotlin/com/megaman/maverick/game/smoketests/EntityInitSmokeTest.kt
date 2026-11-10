package com.megaman.maverick.game.smoketests

import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.state.TagsComponent
import io.kotest.assertions.fail
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import java.io.File
import java.lang.reflect.Modifier
import java.util.zip.ZipFile
import kotlin.reflect.KClass

private const val ENTITIES_PACKAGE = "com.megaman.maverick.game.entities"
private const val KNOWN_FAILURES = "/known-entity-failures.txt"
private const val EXPECTED_TAGS = "/entity-tags.txt"
private val NOT_POOLED = setOf("$ENTITIES_PACKAGE.blocks.PushableBlock\$InnerBlock")

private fun KClass<*>.entityId() = java.name.removePrefix("$ENTITIES_PACKAGE.")

private fun compiledClassNames(): List<String> {
    val root = File(MegaGameEntity::class.java.protectionDomain.codeSource.location.toURI())
    val paths = if (root.isDirectory) {
        root.walkTopDown().filter { it.isFile }.map { it.relativeTo(root).invariantSeparatorsPath }.toList()
    } else {
        ZipFile(root).use { zip -> zip.entries().asSequence().map { it.name }.toList() }
    }
    val prefix = ENTITIES_PACKAGE.replace('.', '/') + "/"
    return paths
        .filter { it.startsWith(prefix) && it.endsWith(".class") }
        .map { it.removeSuffix(".class").replace('/', '.') }
        .sorted()
}

private fun entityClasses(): List<KClass<out MegaGameEntity>> {
    val loader = MegaGameEntity::class.java.classLoader
    return compiledClassNames()
        .filter { it !in NOT_POOLED }
        .map { Class.forName(it, false, loader) }
        .filter { MegaGameEntity::class.java.isAssignableFrom(it) }
        .filter { !it.isInterface && !Modifier.isAbstract(it.modifiers) }
        .map {
            @Suppress("UNCHECKED_CAST")
            it.kotlin as KClass<out MegaGameEntity>
        }
}

private fun knownFailures(): Set<String> {
    val file = requireNotNull(EntityInitSmokeTest::class.java.getResource(KNOWN_FAILURES)) { KNOWN_FAILURES }
    return file.readText().lines().map { it.substringBefore('#').trim() }.filter { it.isNotEmpty() }.toSet()
}

private fun expectedTags(): Map<String, List<String>> {
    val file = requireNotNull(EntityInitSmokeTest::class.java.getResource(EXPECTED_TAGS)) { EXPECTED_TAGS }
    return file.readText().lines().filter { it.isNotBlank() }.associate { line ->
        line.substringBefore(':').trim() to line.substringAfter(':').split(',').map { it.trim() }
    }
}

private fun TagsComponent.toList() = (0 until size).map { get(it) }

class EntityInitSmokeTest : DescribeSpec({

    val harness = HeadlessHarness()
    val classes = entityClasses()
    val knownFailures = knownFailures()
    val expectedTags = expectedTags()

    afterSpec { harness.dispose() }

    fun initProblem(id: String, clazz: KClass<out MegaGameEntity>): AssertionError? {
        harness.regionMisses.clear()
        val entity = MegaEntityFactory.fetch(clazz)!!
        val thrown = runCatching { entity.init() }.exceptionOrNull()
        val misses = harness.regionMisses
        val tags = entity.getComponent(TagsComponent::class)?.toList()
        val expected = expectedTags[id]
        return when {
            thrown != null -> AssertionError("$id: init() threw, missing regions: $misses", thrown)
            misses.isNotEmpty() -> AssertionError("$id: missing regions: $misses")
            tags != expected -> AssertionError("$id: tags $tags, expected $expected (see $EXPECTED_TAGS)")
            else -> null
        }
    }

    describe("the entity classes") {
        it("are found by the scan") {
            classes.size shouldBeGreaterThan 400
        }

        it("include every class on the known-failures list") {
            val ids = classes.map { it.entityId() }.toSet()
            withClue("Listed classes that do not exist") { (knownFailures - ids).shouldBeEmpty() }
        }
    }

    describe("every concrete entity class") {
        classes.forEach { clazz ->
            val id = clazz.entityId()
            it("$id builds and runs init()") {
                val problem = initProblem(id, clazz)
                when {
                    id in knownFailures && problem == null ->
                        fail("$id now passes. Remove it from $KNOWN_FAILURES.")

                    id !in knownFailures && problem != null -> throw problem
                }
            }
        }
    }
})
