package com.megaman.maverick.game

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.io.File

private const val ENTITY_SOURCES = "src/main/kotlin/com/megaman/maverick/game/entities"
private const val BASELINE = "src/test/resources/spawn-allocation-baseline.txt"

private val SPAWN_FUNCTION = Regex("""\bfun\s+(onSpawn|onDestroy|onEvent)\s*\(""")
private val STRING_LITERAL = Regex(""""(?:\\.|[^"\\])*"""")
private val ALLOCATION = Regex(
    """(?<![\w.])(Vector2|Vector3|GameRectangle|GameCircle|GameLine|GamePolygon|GameSprite|DrawingPriority|Timer|""" +
        """TimeMarkedRunnable|Pendulum|RotatingLine|SineWave|Trajectory|MotionDefinition|ArcMotion|Animation|""" +
        """Animator|Regex|CullableOnEvent|CullableOnUncontained)\s*(<[^()]*>)?\(|\.cpy\(\)"""
)

internal fun spawnAllocations(text: String): Int {
    var count = 0
    var depth = 0
    var pending = false
    var bodyDepth = -1
    for (raw in text.lines()) {
        val code = STRING_LITERAL.replace(raw, "\"\"").substringBefore("//")
        if (bodyDepth < 0 && SPAWN_FUNCTION.containsMatchIn(code)) pending = true
        var inBody = bodyDepth >= 0
        for (ch in code) {
            if (ch == '{') {
                depth++
                if (pending && bodyDepth < 0) {
                    bodyDepth = depth
                    pending = false
                    inBody = true
                }
            } else if (ch == '}') {
                if (depth == bodyDepth) bodyDepth = -1
                depth--
            }
        }
        if (inBody) count += ALLOCATION.findAll(code).count()
    }
    return count
}

private fun readBaseline(file: File): Map<String, Int> = file.readLines()
    .map { it.trim() }
    .filter { it.isNotEmpty() && !it.startsWith("#") }
    .associate { line -> line.substringBefore(' ') to line.substringAfter(' ').trim().toInt() }

// TODO: move this text scan to a detekt rule when the detekt setup of item 161 lands (item 045)
class SpawnAllocationGuardTest : DescribeSpec({

    describe("the spawn allocation guard") {

        it("finds no constructor call in onSpawn, onDestroy or onEvent of an entity beyond the baseline") {
            val root = File(ENTITY_SOURCES)
            withClue(root.absolutePath) { root.isDirectory shouldBe true }
            val found = root.walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .associate { it.relativeTo(root).invariantSeparatorsPath to spawnAllocations(it.readText()) }
                .filterValues { it > 0 }
                .toSortedMap()
            val baseline = readBaseline(File(BASELINE)).toSortedMap()
            withClue(
                "a pooled entity makes its objects once (docs/CODE_STYLE.md, \"Allocation and pooled objects\"); " +
                    "if you removed allocations, lower or remove the line in $BASELINE. Found:\n" +
                    found.entries.joinToString("\n") { "${it.key} ${it.value}" }
            ) {
                found shouldBe baseline
            }
        }

        it("counts constructor calls and copies only inside spawn, destroy and event bodies") {
            val text = """
                class A {
                    private val home = Vector2()
                    override fun onSpawn(spawnProps: Properties) {
                        val a = Vector2(1f, 2f)
                        val t = Timer(1f) // Timer(2f)
                        log("GameRectangle()")
                        val c = home.cpy()
                        val b = body.getCenter()
                    }
                    override fun init() { val x = GameSprite() }
                    override fun onDestroy() {
                        if (x) { val y = Array<GameLine>() }
                        val s = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 0))
                    }
                    override fun onEvent(event: Event) {
                        val r = MyVector2(1f)
                    }
                }
            """.trimIndent()
            spawnAllocations(text) shouldBe 5
        }
    }
})
