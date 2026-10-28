package com.megaman.maverick.game.entities.contracts

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import java.io.File

class OnDestroySuperCallTest : DescribeSpec({

    describe("onDestroy overrides in core/src/main") {
        it("all call super.onDestroy(), except the MegaGameEntity base") {
            val root = SOURCE_ROOTS.map { File(it) }.firstOrNull { it.isDirectory }
            (root != null) shouldBe true

            var overrides = 0
            val missing = ArrayList<String>()
            root!!.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { file ->
                val path = file.relativeTo(root).invariantSeparatorsPath
                val source = stripComments(file.readText())
                OVERRIDE.findAll(source).forEach { match ->
                    overrides++
                    if (path == BASE_PATH) return@forEach
                    val body = blockBody(source, match.range.last + 1)
                    if (body == null || !SUPER_CALL.containsMatchIn(body)) missing.add(path)
                }
            }

            overrides shouldBeGreaterThan 100
            withClue("onDestroy() overrides without super.onDestroy() in $root") { missing.shouldBeEmpty() }
        }
    }
}) {

    companion object {
        private val SOURCE_ROOTS = listOf("src/main/kotlin", "core/src/main/kotlin")
        private const val BASE_PATH = "com/megaman/maverick/game/entities/contracts/MegaGameEntity.kt"
        private val OVERRIDE = Regex("""override\s+fun\s+onDestroy\s*\(\s*\)""")
        private val SUPER_CALL = Regex("""super(\s*<[^>]*>)?\s*\.\s*onDestroy\s*\(\s*\)""")
        private val COMMENTS = Regex("""//[^\n]*|/\*[\s\S]*?\*/""")

        private fun stripComments(source: String) = COMMENTS.replace(source, "")

        private fun blockBody(source: String, start: Int): String? {
            var i = start
            while (i < source.length && source[i].isWhitespace()) i++
            if (i >= source.length || source[i] != '{') return null
            var depth = 0
            for (j in i until source.length) {
                when (source[j]) {
                    '{' -> depth++
                    '}' -> if (--depth == 0) return source.substring(i, j + 1)
                }
            }
            return null
        }
    }
}
