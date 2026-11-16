package com.megaman.maverick.game

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import java.io.File

private val SOURCE_ROOTS = listOf("src/main/kotlin", "../engine/src/main/kotlin", "../lwjgl3/src/main/kotlin")

private const val RANDOM_PACKAGE = "com/maverick/game/engine/common/random/"

private val DIRECT_RANDOM_CALLS = listOf(
    Regex("""\bMathUtils\.random"""),
    Regex("""import com\.badlogic\.gdx\.math\.MathUtils\.(random|\*)"""),
    Regex("""import com\.badlogic\.gdx\.math\.MathUtils as\b"""),
    Regex("""\b(Random|RandomXS128|SecureRandom|SplittableRandom)\("""),
    Regex("""\bCollections\.shuffle\b"""),
    Regex("""import (java\.util|kotlin\.random)\.Random\b"""),
    Regex("""\bRandom\.(Default|next)"""),
    Regex("""\bThreadLocalRandom\b"""),
    Regex("""\bMath\.random\b"""),
    Regex("""\.random\(\)"""),
    Regex("""\.shuffle\(\)"""),
    Regex("""\.shuffled\("""),
    Regex("""\bUtilMethods\.random\b""")
)

private fun directRandomCalls(text: String): List<Int> = text.lines().mapIndexedNotNull { index, line ->
    val code = line.substringBefore("//")
    if (DIRECT_RANDOM_CALLS.any { it.containsMatchIn(code) }) index + 1 else null
}

class RandomSourceGuardTest : DescribeSpec({

    describe("the random source guard") {

        it("finds no direct random call outside the engine random package") {
            val found = SOURCE_ROOTS.flatMap { root ->
                val dir = File(root)
                withClue(dir.absolutePath) { dir.isDirectory shouldBe true }
                dir.walkTopDown()
                    .filter { it.isFile && it.extension == "kt" }
                    .filterNot { it.invariantSeparatorsPath.contains(RANDOM_PACKAGE) }
                    .flatMap { file -> directRandomCalls(file.readText()).map { "${file.path}:$it" } }
                    .toList()
            }
            withClue("use services.random (GameRandom) in place of these calls") { found.shouldBeEmpty() }
        }

        it("detects each kind of direct random call, and skips comments and the injected source") {
            val bad = listOf(
                "val x = MathUtils.random(0f, 1f)",
                "import com.badlogic.gdx.math.MathUtils.*",
                "val r = Random(42L)",
                "val r = RandomXS128(1L)",
                "val r = SecureRandom()",
                "import com.badlogic.gdx.math.MathUtils as MU",
                "Collections.shuffle(list)",
                "import java.util.Random",
                "import kotlin.random.Random",
                "val i = Random.nextInt(3)",
                "val e = Position.entries.random()",
                "array.shuffle()",
                "(0..3).shuffled()",
                "val d = Math.random()",
                "UtilMethods.random.nextInt(2)"
            )
            bad.forEach { line -> withClue(line) { directRandomCalls(line) shouldBe listOf(1) } }

            val good = listOf(
                "val x = services.random.nextFloat(0f, 1f)",
                "services.random.shuffle(array)",
                "val r = SeededRandom(source)",
                "// MathUtils.random(0f, 1f)",
                "val e = services.random.pick(Position.entries)"
            )
            good.forEach { line -> withClue(line) { directRandomCalls(line).shouldBeEmpty() } }
        }
    }
})
