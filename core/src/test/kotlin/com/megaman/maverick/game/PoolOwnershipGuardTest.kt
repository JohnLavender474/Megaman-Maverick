package com.megaman.maverick.game

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import java.io.File

private val POOL_SOURCE_ROOTS = listOf("src/main/kotlin", "../lwjgl3/src/main/kotlin")

private val POOLED_DEFAULT_PARAMETER = Regex("""\w+\s*:\s*[\w.<>?]+\s*=\s*GameObjectPools\.fetch\(""")

private fun pooledDefaultParameters(text: String): List<Int> = text.lines().mapIndexedNotNull { index, line ->
    val code = line.substringBefore("//")
    if (POOLED_DEFAULT_PARAMETER.containsMatchIn(code) && !code.trimStart().startsWith("val ") &&
        !code.trimStart().startsWith("var ")
    ) index + 1 else null
}

class PoolOwnershipGuardTest : DescribeSpec({

    describe("the pool ownership guard") {

        it("finds no pool fetch in a default parameter value") {
            val found = POOL_SOURCE_ROOTS.flatMap { root ->
                val dir = File(root)
                withClue(dir.absolutePath) { dir.isDirectory shouldBe true }
                dir.walkTopDown()
                    .filter { it.isFile && it.extension == "kt" }
                    .flatMap { file -> pooledDefaultParameters(file.readText()).map { "${file.path}:$it" } }
                    .toList()
            }
            withClue("take an explicit `out` parameter, or give a function with no `out` that returns a scratch") {
                found.shouldBeEmpty()
            }
        }

        it("detects a pooled default parameter, and skips locals and comments") {
            val bad = listOf(
                "fun Camera.toGameRectangle(out: GameRectangle = GameObjectPools.fetch(GameRectangle::class)) {",
                "        out: Vector2 = GameObjectPools.fetch(Vector2::class)"
            )
            bad.forEach { line -> withClue(line) { pooledDefaultParameters(line) shouldBe listOf(1) } }

            val good = listOf(
                "val out = GameObjectPools.fetch(Vector2::class)",
                "        val center: Vector2 = GameObjectPools.fetch(Vector2::class)",
                "fun IBody.getCenter() = getCenter(GameObjectPools.fetch(Vector2::class))",
                "// out: Vector2 = GameObjectPools.fetch(Vector2::class)"
            )
            good.forEach { line -> withClue(line) { pooledDefaultParameters(line).shouldBeEmpty() } }
        }
    }
})
