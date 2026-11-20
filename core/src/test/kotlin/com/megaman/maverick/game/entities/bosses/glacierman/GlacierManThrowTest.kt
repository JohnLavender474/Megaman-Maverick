package com.megaman.maverick.game.entities.bosses.glacierman

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.shapes.GameRectangle
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.bosses.bigassmaverickrobot.OrbAim
import com.megaman.maverick.game.entities.hazards.SmallIceCube
import com.megaman.maverick.game.entities.projectiles.Snowhead
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlin.math.abs

private const val TOLERANCE = 2f * STEP

private val ROWS = listOf(
    TuningRow("normal", hard = false, phase2 = false),
    TuningRow("normal phase 2", hard = false, phase2 = true),
    TuningRow("hard", hard = true, phase2 = false),
    TuningRow("hard phase 2", hard = true, phase2 = true)
)

private fun frames(key: String?) = when (key) {
    "chunk" -> 5
    "aim", "aim_up" -> 4
    else -> throw IllegalStateException("no throw key: $key")
}

class GlacierManThrowTest : DescribeSpec({

    lateinit var rig: GlacierManRig

    afterEach { rig.dispose() }

    fun orderBy(vararg rank: OrbAim): (com.badlogic.gdx.utils.Array<Any?>) -> Unit = { array ->
        val sorted = array.toList().sortedBy { rank.indexOf(it as OrbAim) }
        array.clear()
        sorted.forEach { array.add(it) }
    }

    fun startVolley(row: TuningRow = ROWS[0], setup: () -> Unit = {}) {
        rig = GlacierManRig(hard = row.hard)
        rig.startFight(phase2 = row.phase2)
        setup()
        rig.leaveStand(0.01f) shouldBe "throw_flash"
    }

    fun finishVolley(each: () -> Unit = {}) {
        withClue("the volley ends in STAND") {
            rig.ticksUntil(10f, each = each) { rig.baseKey == "stand" } shouldBe true
        }
    }

    fun cubes() = rig.spawnsOf<SmallIceCube>()

    fun volleyAims() = rig.random.calls.last { it.kind == GlacierManRandom.Kind.SHUFFLE }.items.map { it as OrbAim }

    fun arena() = GameRectangle(LEFT_INNER, rig.bossBody.getY(), RIGHT_INNER - LEFT_INNER, ARENA_HEIGHT)

    describe("the volley size") {
        ROWS.forEach { row ->
            it("${row.name}: a volley has iceBlastCount = ${row.tuning.iceBlastCount} cubes") {
                startVolley(row)
                finishVolley()
                cubes().size shouldBe row.tuning.iceBlastCount
                volleyAims().size shouldBe row.tuning.iceBlastCount
            }
        }

        it("6 cubes on normal and 12 on hard") {
            GlacierManTuning.NORMAL.iceBlastCount shouldBe 6
            GlacierManTuning.NORMAL_PHASE_2.iceBlastCount shouldBe 6
            GlacierManTuning.HARD.iceBlastCount shouldBe 12
            GlacierManTuning.HARD_PHASE_2.iceBlastCount shouldBe 12
            rig = GlacierManRig()
        }
    }

    describe("the volley aims") {
        it("a 6-cube volley has 2 LEAD, 2 CURRENT, 1 NEAR and 1 WIDE, shuffled with services.random") {
            startVolley()
            finishVolley()
            val aims = volleyAims()
            aims.count { it == OrbAim.LEAD } shouldBe 2
            aims.count { it == OrbAim.CURRENT } shouldBe 2
            aims.count { it == OrbAim.NEAR } shouldBe 1
            aims.count { it == OrbAim.WIDE } shouldBe 1
            rig.random.calls.count { it.kind == GlacierManRandom.Kind.SHUFFLE } shouldBe 1
        }

        it("a 12-cube volley has 3 of each") {
            startVolley(ROWS[2])
            finishVolley()
            val aims = volleyAims()
            OrbAim.entries.forEach { aim -> aims.count { it == aim } shouldBe 3 }
        }

        it("each cube flies to the point of its aim, in the shuffled order, straight at 10 tiles/s") {
            startVolley {
                rig.place(10f)
                rig.playerBody.physics.velocity.x = 4f * T
            }
            rig.random.shuffler = null
            val shuffles = rig.random.calls.count { it.kind == GlacierManRandom.Kind.SHUFFLE }
            shuffles shouldBe 0
            rig.random.shuffler = orderBy(OrbAim.WIDE, OrbAim.NEAR, OrbAim.CURRENT, OrbAim.LEAD)
            finishVolley()
            val aims = volleyAims()
            aims shouldBe listOf(OrbAim.WIDE, OrbAim.NEAR, OrbAim.CURRENT, OrbAim.CURRENT, OrbAim.LEAD, OrbAim.LEAD)
            val row = GlacierManTuning.NORMAL
            val records = cubes()
            records.size shouldBe 6
            var marker = rig.random.calls.indexOfLast { it.kind == GlacierManRandom.Kind.SHUFFLE } + 1
            records.forEachIndexed { k, record ->
                withClue("cube $k (${aims[k]})") {
                    record.bossKey shouldBe "aim"
                    val segment = (marker until record.callIndex).map { rig.random.calls[it] }
                    segment.first().let { it.min shouldBe 0f; it.max shouldBe 1f }
                    val replay = segment.drop(1).map { it.value }.iterator()
                    val target = OrbAim.point(
                        aims[k], rig.playerBody.getCenter(Vector2()), rig.playerBody.physics.velocity,
                        row.cubeLeadTime, arena(), { _, _ -> replay.next() }, Vector2()
                    )
                    replay.hasNext() shouldBe false
                    val expected = Vector2(target).sub(record.position).nor().scl(10f * T)
                    record.trajectory.x shouldBe (expected.x plusOrMinus 0.01f)
                    record.trajectory.y shouldBe (expected.y plusOrMinus 0.01f)
                    record.props.get(ConstKeys.GRAVITY_ON, Boolean::class) shouldBe false
                    record.props.get(ConstKeys.CLAMP, Boolean::class) shouldBe true
                    record.props.get(ConstKeys.OWNER) shouldBe rig.boss
                }
                marker = record.callIndex
            }
        }
    }

    describe("the throw strip") {
        it("aim_up for every throw when the player is above, with no chunk roll") {
            startVolley { rig.place(10f, topOverFloor = 2f) }
            val before = rig.random.calls.size
            finishVolley()
            cubes().map { it.bossKey }.toSet() shouldBe setOf("aim_up")
            val units = (before until rig.random.calls.size).map { rig.random.calls[it] }
                .count { it.kind == GlacierManRandom.Kind.FLOAT && it.min == 0f && it.max == 1f }
            units shouldBe volleyAims().count { it == OrbAim.WIDE }
        }

        it("else chunk when the roll is under chunkChance, else aim") {
            val chance = GlacierManTuning.NORMAL.chunkChance
            startVolley { rig.place(10f) }
            rig.random.shuffler = orderBy(OrbAim.LEAD, OrbAim.CURRENT, OrbAim.NEAR, OrbAim.WIDE)
            rig.rolls(chance - 0.001f, chance, 0f, FAIL, chance - 0.001f, chance, FAIL)
            finishVolley()
            cubes().map { it.bossKey } shouldBe listOf("chunk", "aim", "chunk", "aim", "chunk", "aim")
            rig.random.rolls.isEmpty shouldBe true
        }

        it("he faces the player at the start of each throw, and the spawn point mirrors with the facing") {
            startVolley { rig.place(14f) }
            var seen = 0
            finishVolley(each = {
                val count = cubes().size
                if (count > seen) {
                    seen = count
                    val right = count % 2 == 1
                    rig.place(if (right) BOSS_START_X + 4f else BOSS_START_X - 4f)
                }
            })
            val records = cubes()
            records.size shouldBe 6
            records.forEachIndexed { k, record ->
                withClue("cube $k") { record.bossFacing shouldBe if (k % 2 == 0) Facing.LEFT else Facing.RIGHT }
            }
            val center = rig.bossCenter()
            records.forEach { it.bossKey shouldBe "aim" }
            val leftOffset = records[0].position.x - center.x
            (leftOffset < 0f) shouldBe true
            records.forEach { record ->
                val offset = record.position.x - center.x
                offset shouldBe ((if (record.bossFacing == Facing.LEFT) leftOffset else -leftOffset) plusOrMinus 0.01f)
                record.position.y shouldBe (records[0].position.y plusOrMinus 0.01f)
            }
        }
    }

    describe("the cube spawn point") {
        listOf(
            Triple("chunk", 14f, 28f),
            Triple("aim", 13f, 20f),
            Triple("aim_up", 13f, 32f)
        ).forEach { (key, cellX, cellY) ->
            listOf(Facing.LEFT, Facing.RIGHT).forEach { facing ->
                it("$key facing ${facing.name.lowercase()}: cell point ($cellX, $cellY) of the 64 x 48 cell " +
                    "(x = 64 - px facing right), scaled to the 4 x 3 tile sprite placed bottom-center on the body") {
                    val playerX = if (facing == Facing.LEFT) BOSS_START_X - 10f else BOSS_START_X + 4f
                    startVolley { rig.place(playerX, topOverFloor = if (key == "aim_up") 2f else 1.25f) }
                    rig.random.unitDefault = { if (key == "chunk") PASS else FAIL }
                    withClue("the first cube") { rig.ticksUntil(2f) { cubes().isNotEmpty() } shouldBe true }
                    val record = cubes().first()
                    record.bossKey shouldBe key
                    record.bossFacing shouldBe facing
                    val body = rig.bossBody
                    val x = if (facing == Facing.LEFT) cellX else 64f - cellX
                    val spriteLeft = body.getX() + body.getWidth() / 2f - 2f * T
                    record.position.x shouldBe (spriteLeft + x * 4f * T / 64f plusOrMinus 0.01f)
                    record.position.y shouldBe (body.getY() + cellY * 3f * T / 48f plusOrMinus 0.01f)
                }
            }
        }
    }

    describe("the throw timing") {
        listOf(
            Triple(ROWS[0], FAIL, "aim"),
            Triple(ROWS[2], PASS, "chunk"),
            Triple(ROWS[3], FAIL, "aim")
        ).forEach { (row, roll, key) ->
            it("${row.name}, $key: the cube spawns at 2 x throwFrameDur, a throw lasts frames x throwFrameDur + " +
                "throwRest, and the volley ends in STAND") {
                val tuning = row.tuning
                startVolley(row) { rig.place(10f) }
                rig.random.unitDefault = { roll }
                withClue("the tell ends") { rig.ticksUntil(2f) { rig.animKey != "throw_flash" } shouldBe true }
                val start = rig.time
                finishVolley()
                val end = rig.time
                val records = cubes()
                records.size shouldBe tuning.iceBlastCount
                records.forEach { it.bossKey shouldBe key }
                val throwDur = frames(key) * tuning.throwFrameDur + tuning.throwRest
                (records[0].time + STEP - start) shouldBe (2f * tuning.throwFrameDur plusOrMinus TOLERANCE)
                for (k in 1 until records.size) withClue("throw $k") {
                    (records[k].time - records[k - 1].time) shouldBe (throwDur plusOrMinus 1.5f * STEP)
                }
                (end - records.last().time - STEP) shouldBe
                    (throwDur - 2f * tuning.throwFrameDur plusOrMinus TOLERANCE)
            }
        }
    }

    describe("the cube flight") {
        listOf(
            "unclamped flight time" to 13f,
            "flight time clamped to 1.3 s" to 4f,
            "flight time clamped to 0.6 s" to 17f
        ).forEach { (name, playerX) ->
            it("chunk, $name: a lob with gravity -22.5 tiles/s2 that reaches the aim point, no speed clamp") {
                startVolley { rig.place(playerX) }
                rig.random.shuffler = orderBy(OrbAim.CURRENT, OrbAim.LEAD, OrbAim.NEAR, OrbAim.WIDE)
                rig.rolls(PASS)
                withClue("the first cube") { rig.ticksUntil(2f) { cubes().isNotEmpty() } shouldBe true }
                val record = cubes().first()
                record.bossKey shouldBe "chunk"
                record.props.get(ConstKeys.GRAVITY, Float::class) shouldBe -22.5f
                record.props.get(ConstKeys.GRAVITY_ON, Boolean::class) shouldBe true
                record.props.get(ConstKeys.CLAMP, Boolean::class) shouldBe false
                val target = rig.playerBody.getCenter(Vector2())
                val dx = target.x - record.position.x
                val time = (abs(dx) / (7f * T)).coerceIn(0.6f, 1.3f)
                when (playerX) {
                    13f -> (abs(dx) / (7f * T) in 0.6f..1.3f) shouldBe true
                    4f -> (abs(dx) / (7f * T) > 1.3f) shouldBe true
                    else -> (abs(dx) / (7f * T) < 0.6f) shouldBe true
                }
                (dx / record.trajectory.x) shouldBe (time plusOrMinus 0.001f)
                val y = record.position.y + record.trajectory.y * time + 0.5f * -22.5f * T * time * time
                y shouldBe (target.y plusOrMinus 0.05f)
            }
        }

        it("aim: a straight throw at 10 tiles/s to the aim point, no gravity") {
            startVolley { rig.place(10f) }
            rig.random.shuffler = orderBy(OrbAim.CURRENT, OrbAim.LEAD, OrbAim.NEAR, OrbAim.WIDE)
            rig.rolls(FAIL)
            withClue("the first cube") { rig.ticksUntil(2f) { cubes().isNotEmpty() } shouldBe true }
            val record = cubes().first()
            record.bossKey shouldBe "aim"
            record.props.get(ConstKeys.GRAVITY_ON, Boolean::class) shouldBe false
            record.props.get(ConstKeys.CLAMP, Boolean::class) shouldBe true
            record.trajectory.len() shouldBe (10f * T plusOrMinus 0.01f)
            val toPlayer = rig.playerBody.getCenter(Vector2()).sub(record.position).nor()
            val direction = Vector2(record.trajectory).nor()
            direction.x shouldBe (toPlayer.x plusOrMinus 0.0001f)
            direction.y shouldBe (toPlayer.y plusOrMinus 0.0001f)
        }
    }

    describe("the snowball aim") {
        val row = GlacierManTuning.NORMAL

        fun nextSnowball(each: () -> Unit = {}): SpawnRecord {
            val before = rig.spawnsOf<Snowhead>().size
            withClue("a snowball") {
                rig.ticksUntil(3f, each = each) { rig.spawnsOf<Snowhead>().size > before } shouldBe true
            }
            return rig.spawnsOf<Snowhead>()[before]
        }

        fun shouldArcToPlayer(record: SpawnRecord) {
            val target = rig.playerBody.getCenter(Vector2())
            val dx = target.x - record.position.x
            val time = (abs(dx) / (10f * T)).coerceIn(0.4f, 1f)
            record.trajectory.x shouldBe (dx / time plusOrMinus 0.01f)
            record.trajectory.y shouldBe ((target.y - record.position.y) / time - 0.5f * Snowhead.GRAVITY * T * time
                plusOrMinus 0.01f)
        }

        fun sled() {
            rig = GlacierManRig()
            rig.startFight()
            val total = row.iceBlastWeight * row.farIceBlastScale + row.duckWeight + row.sledWeight + row.jumpWeight
            rig.leaveStand((row.iceBlastWeight * row.farIceBlastScale + row.duckWeight + row.sledWeight / 2f) / total) shouldBe
                "brake_flash"
            rig.untilKey("sled")
        }

        listOf("near" to 18f, "mid" to 14f, "far" to 4f).forEach { (name, playerX) ->
            it("on the ground, a passing snowballAimChance roll with the player in front ($name): an arc to his center") {
                rig = GlacierManRig()
                rig.startFight()
                rig.place(playerX)
                rig.rolls(row.snowballAimChance - 0.001f)
                shouldArcToPlayer(nextSnowball())
            }
        }

        it("a roll at snowballAimChance: the fixed master velocity, straight") {
            rig = GlacierManRig()
            rig.startFight()
            rig.rolls(row.snowballAimChance, FAIL)
            val record = nextSnowball()
            record.trajectory shouldBe Vector2(-15f * T, 6f * T)
        }

        it("a roll at snowballAimChance: the fixed master velocity, up") {
            rig = GlacierManRig()
            rig.startFight()
            rig.rolls(row.snowballAimChance, 0f)
            val record = nextSnowball()
            record.trajectory shouldBe Vector2(-9f * T, 15f * T)
        }

        it("a passing roll in the sled with the player in front: an arc to his center") {
            sled()
            rig.rolls(PASS)
            val record = nextSnowball()
            record.bossFacing shouldBe Facing.LEFT
            shouldArcToPlayer(record)
        }

        it("a passing roll in the sled with the player behind: the fixed master velocity") {
            sled()
            rig.place(24f)
            rig.rolls(PASS)
            val record = nextSnowball()
            record.bossFacing shouldBe Facing.LEFT
            record.trajectory shouldBe Vector2(-15f * T, 6f * T)
        }

        it("a snowball in the air drops with no speed") {
            rig = GlacierManRig()
            rig.startFight()
            rig.random.unitDefault = { PASS }
            val record = nextSnowball(each = { rig.holdInAir(3f) })
            record.trajectory shouldBe Vector2(0f, 0f)
        }
    }

    describe("the arena") {
        it("aim points stay between the inner wall edges and at most 14 tiles over his feet") {
            startVolley { rig.placePlayerCenter(30f * T, FLOOR + 20f * T) }
            rig.random.shuffler = orderBy(OrbAim.CURRENT, OrbAim.LEAD, OrbAim.NEAR, OrbAim.WIDE)
            withClue("the first cube") { rig.ticksUntil(2f) { cubes().isNotEmpty() } shouldBe true }
            val record = cubes().first()
            record.bossKey shouldBe "aim_up"
            record.bossFacing shouldBe Facing.RIGHT
            val expected = Vector2(RIGHT_INNER, rig.bossBody.getY() + ARENA_HEIGHT).sub(record.position).nor()
            val direction = Vector2(record.trajectory).nor()
            direction.x shouldBe (expected.x plusOrMinus 0.0001f)
            direction.y shouldBe (expected.y plusOrMinus 0.0001f)
        }

        fun withWalls(vararg walls: GameRectangle): Properties {
            rig.walls.clear()
            walls.forEach { rig.walls.add(it) }
            return rig.spawnProps()
        }

        it("a spawn with no wall on the right throws an IllegalStateException") {
            rig = GlacierManRig()
            val props = withWalls(GameRectangle(0f, FLOOR, LEFT_INNER, 20f * T))
            shouldThrow<IllegalStateException> { rig.spawnBoss(props) }
        }

        it("a spawn with both walls on the left throws an IllegalStateException") {
            rig = GlacierManRig()
            val props = withWalls(
                GameRectangle(0f, FLOOR, LEFT_INNER, 20f * T),
                GameRectangle(4f * T, FLOOR, T, 20f * T)
            )
            shouldThrow<IllegalStateException> { rig.spawnBoss(props) }
        }

        it("a spawn with a wall on each side does not throw") {
            rig = GlacierManRig()
            rig.spawnBoss()
        }
    }
})
