package com.megaman.maverick.game.entities

import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.testServices
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import java.io.File
import java.lang.reflect.Modifier
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

class EntityConstructorsTest : DescribeSpec({

    val notPooled = setOf("com.megaman.maverick.game.entities.blocks.PushableBlock\$InnerBlock")

    fun entityClasses(): List<KClass<out MegaGameEntity>> {
        val root = File(MegaGameEntity::class.java.protectionDomain.codeSource.location.toURI())
        val dir = File(root, "com/megaman/maverick/game/entities")
        val loader = MegaGameEntity::class.java.classLoader
        return dir.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".class") }
            .map { it.relativeTo(root).path.removeSuffix(".class").replace(File.separatorChar, '.') }
            .sorted()
            .map { Class.forName(it, false, loader) }
            .filter { MegaGameEntity::class.java.isAssignableFrom(it) }
            .filter { !it.isInterface && !Modifier.isAbstract(it.modifiers) }
            .filter { it.name !in notPooled }
            .map {
                @Suppress("UNCHECKED_CAST")
                it.kotlin as KClass<out MegaGameEntity>
            }
            .toList()
    }

    describe("every concrete entity class") {
        it("has a primary constructor with one required MegaServices parameter, and builds with a holder") {
            val classes = entityClasses()
            classes.size shouldBeGreaterThan 400
            val services = testServices()
            val failures = mutableListOf<String>()
            for (clazz in classes) {
                val constructor = clazz.primaryConstructor
                if (constructor == null) {
                    failures.add("${clazz.qualifiedName}: no primary constructor")
                    continue
                }
                val required = constructor.parameters.filter { !it.isOptional }
                if (required.size != 1 || required[0].type.classifier != MegaServices::class) {
                    failures.add("${clazz.qualifiedName}: parameters ${constructor.parameters.map { it.type }}")
                    continue
                }
                try {
                    constructor.isAccessible = true
                    constructor.callBy(mapOf(required[0] to services))
                } catch (e: Throwable) {
                    failures.add("${clazz.qualifiedName}: ${e.cause ?: e}")
                }
            }
            withClue(failures.joinToString("\n")) { failures.shouldBeEmpty() }
        }
    }
})
