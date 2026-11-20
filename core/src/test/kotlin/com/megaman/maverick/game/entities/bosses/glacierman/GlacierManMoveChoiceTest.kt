package com.megaman.maverick.game.entities.bosses.glacierman

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.shapes.GameRectangle
import com.megaman.maverick.game.entities.projectiles.Snowhead
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import java.io.File
import kotlin.math.abs

private const val TOLERANCE = 2f * STEP
private const val AIR_TIME = 2f * 15f / 22.5f

private val PHASE_1_ROWS = listOf(
    TuningRow("normal", hard = false, phase2 = false),
    TuningRow("hard", hard = true, phase2 = false)
)
private val ALL_ROWS = PHASE_1_ROWS + listOf(
    TuningRow("normal phase 2", hard = false, phase2 = true),
    TuningRow("hard phase 2", hard = true, phase2 = true)
)

private fun farTotal(row: GlacierManTuning) = row.iceBlastWeight * row.farIceBlastScale + row.duckWeight +
    row.sledWeight + row.jumpWeight + row.icicleRainWeight + row.sledRamWeight

class GlacierManMoveChoiceTest : DescribeSpec({

    lateinit var rig: GlacierManRig

    afterEach { rig.dispose() }

    fun start(row: TuningRow = PHASE_1_ROWS[0]) {
        rig = GlacierManRig(hard = row.hard)
        rig.startFight(phase2 = row.phase2)
    }

    fun nearX() = (rig.bossBody.getX() - 1f * T) / T + 0.5f

    fun inStand() = rig.baseKey == "stand"

    describe("the weighted pick from STAND") {
        PHASE_1_ROWS.forEach { row ->
            val tuning = row.tuning
            val total = farTotal(tuning)
            val ice = tuning.iceBlastWeight * tuning.farIceBlastScale
            listOf(
                Triple("ice blast", ice / 2f, "throw_flash"),
                Triple("duck", ice + tuning.duckWeight / 2f, "duck"),
                Triple("sled", ice + tuning.duckWeight + tuning.sledWeight / 2f, "brake_flash"),
                Triple("jump", total - tuning.jumpWeight / 2f, "fall_flash")
            ).forEach { (name, roll, key) ->
                it("${row.name}: a roll in the $name share of the weights from the row starts $name ($key)") {
                    start(row)
                    rig.leaveStand(roll / total) shouldBe key
                    rig.random.pickCalls().size shouldBe 1
                    rig.lastPickTotal() shouldBe (total plusOrMinus 0.001f)
                }
            }
        }
    }

    describe("the weights") {
        val row = GlacierManTuning.NORMAL

        it("far and on the ground: ice blast x farIceBlastScale, duck, sled and jump") {
            start()
            rig.leaveStand(0.5f)
            rig.lastPickTotal() shouldBe (farTotal(row) plusOrMinus 0.001f)
        }

        it("no duck when the top of the player is 1.5 tiles or more over his feet, and jump x aboveJumpScale") {
            start()
            rig.place(6f, topOverFloor = 1.5f)
            rig.leaveStand(0.5f)
            rig.lastPickTotal() shouldBe (row.iceBlastWeight * row.farIceBlastScale + row.sledWeight +
                row.jumpWeight * row.aboveJumpScale plusOrMinus 0.001f)
        }

        it("the player at 1.49 tiles is not above") {
            start()
            rig.place(6f, topOverFloor = 1.49f)
            rig.leaveStand(0.5f)
            rig.lastPickTotal() shouldBe (farTotal(row) plusOrMinus 0.001f)
        }

        it("no sled within 2.5 tiles in x, and jump x nearJumpScale; ice blast not scaled") {
            start()
            rig.playerBody.setX(rig.bossBody.getX() - 2.5f * T)
            rig.leaveStand(0.5f)
            rig.lastPickTotal() shouldBe (row.iceBlastWeight + row.duckWeight + row.jumpWeight * row.nearJumpScale
                plusOrMinus 0.001f)
        }

        it("2.6 tiles in x is far") {
            start()
            rig.playerBody.setX(rig.bossBody.getX() - 2.6f * T)
            rig.leaveStand(0.5f)
            rig.lastPickTotal() shouldBe (farTotal(row) plusOrMinus 0.001f)
        }

        it("near and above: jump x nearJumpScale x aboveJumpScale, no duck, no sled") {
            start()
            rig.place(nearX(), topOverFloor = 2f)
            rig.leaveStand(0.5f)
            rig.lastPickTotal() shouldBe (row.iceBlastWeight +
                row.jumpWeight * row.nearJumpScale * row.aboveJumpScale plusOrMinus 0.001f)
        }

        it("no ice blast in the air") {
            start()
            rig.pick(0.5f)
            rig.ticksUntil(3f, each = { rig.holdInAir(3f) }) { !inStand() } shouldBe true
            rig.lastPickTotal() shouldBe (row.duckWeight + row.sledWeight + row.jumpWeight plusOrMinus 0.001f)
        }

        it("no ice blast when a throw spawn point is in the left wall (facing left)") {
            start()
            rig.placeBossLeft(LEFT_INNER + 0.1f * T)
            rig.placePlayerCenter(LEFT_INNER + 0.1f * T, FLOOR + 0.5f * T)
            rig.leaveStand(0.5f)
            rig.boss.facing shouldBe Facing.LEFT
            rig.lastPickTotal() shouldBe (row.duckWeight + row.jumpWeight * row.nearJumpScale plusOrMinus 0.001f)
        }

        it("no ice blast when a throw spawn point is in the right wall (facing right)") {
            start()
            rig.placeBossLeft(RIGHT_INNER - 0.1f * T - rig.bossBody.getWidth())
            rig.placePlayerCenter(RIGHT_INNER - 0.1f * T, FLOOR + 0.5f * T)
            rig.leaveStand(0.5f)
            rig.boss.facing shouldBe Facing.RIGHT
            rig.lastPickTotal() shouldBe (row.duckWeight + row.jumpWeight * row.nearJumpScale plusOrMinus 0.001f)
        }

        it("ice blast near the wall when no spawn point is in it") {
            start()
            rig.placeBossLeft(LEFT_INNER + 2f * T)
            rig.placePlayerCenter(LEFT_INNER + 2f * T, FLOOR + 0.5f * T)
            rig.leaveStand(0.5f)
            rig.lastPickTotal() shouldBe (row.iceBlastWeight + row.duckWeight + row.jumpWeight * row.nearJumpScale
                plusOrMinus 0.001f)
        }
    }

    describe("the repeat rule") {
        val row = GlacierManTuning.NORMAL

        it("the last move gets x lastMoveScale, and a move never runs 3 times in a row (duck)") {
            start()
            rig.place(nearX())
            val near = row.iceBlastWeight + row.duckWeight + row.jumpWeight * row.nearJumpScale
            rig.leaveStand((row.iceBlastWeight + row.duckWeight / 2f) / near) shouldBe "duck"
            rig.untilStand()
            val second = row.iceBlastWeight + row.duckWeight * row.lastMoveScale + row.jumpWeight * row.nearJumpScale
            rig.leaveStand((row.iceBlastWeight + row.duckWeight * row.lastMoveScale / 2f) / second) shouldBe "duck"
            rig.lastPickTotal() shouldBe (second plusOrMinus 0.001f)
            rig.untilStand()
            rig.leaveStand(0.5f)
            rig.lastPickTotal() shouldBe (row.iceBlastWeight + row.jumpWeight * row.nearJumpScale plusOrMinus 0.001f)
        }

        it("a chained move counts: sled, chained jump, picked jump, then no jump") {
            start()
            rig.leaveStand((row.iceBlastWeight * row.farIceBlastScale + row.duckWeight + row.sledWeight / 2f) /
                farTotal(row)) shouldBe "brake_flash"
            rig.untilKey("sled")
            rig.place(6f, topOverFloor = 2f)
            rig.untilKey("jump")
            rig.place(6f)
            rig.untilStand()
            rig.placeBoss(BOSS_START_X)
            val afterChain = farTotal(row) - row.jumpWeight + row.jumpWeight * row.lastMoveScale
            rig.leaveStand(0.999f) shouldBe "fall_flash"
            rig.lastPickTotal() shouldBe (afterChain plusOrMinus 0.001f)
            rig.untilStand()
            rig.placeBoss(BOSS_START_X)
            rig.leaveStand(0.5f)
            rig.lastPickTotal() shouldBe (farTotal(row) - row.jumpWeight plusOrMinus 0.001f)
        }
    }

    describe("every weight at 0") {
        it("after 2 jumps, with the player near and above and a spawn point in the wall, the move is duck") {
            start()
            rig.leaveStand(0.999f) shouldBe "fall_flash"
            rig.untilStand()
            rig.placeBoss(BOSS_START_X)
            rig.leaveStand(0.999f) shouldBe "fall_flash"
            rig.untilStand()
            rig.placeBossLeft(LEFT_INNER + 0.1f * T)
            rig.placePlayerCenter(LEFT_INNER + 0.1f * T, FLOOR + 1.5f * T)
            val picks = rig.random.pickCalls().size
            withClue("STAND ends") { rig.ticksUntil(3f) { !inStand() } shouldBe true }
            rig.animKey shouldBe "duck"
            rig.random.pickCalls().size shouldBe picks
        }
    }

    describe("the sled chain at a wall") {
        fun sledIntoLeftWall(bossX: Float, playerX: Float) {
            rig.placeBoss(bossX)
            rig.place(playerX)
            val row = GlacierManTuning.NORMAL
            rig.leaveStand((row.iceBlastWeight * row.farIceBlastScale + row.duckWeight + row.sledWeight / 2f) /
                farTotal(row)) shouldBe "brake_flash"
            rig.untilKey("sled")
        }

        fun jumpOverStart(): Float {
            withClue("the tell ends") { rig.ticksUntil(2f) { rig.animKey == "fall_flash" } shouldBe true }
            rig.tick()
            val tell = rig.secondsWhile(2f) { rig.animKey == "fall_flash" } + STEP
            rig.animKey shouldBe "jump"
            return tell
        }

        it("a passing sledJumpChainChance roll: fall_flash facing the player, a jump aimed 2 tiles past the player, " +
            "one aimed snowball at the landing, then brake") {
            start()
            val row = GlacierManTuning.NORMAL
            sledIntoLeftWall(8.5f, 4f)
            rig.rolls(FAIL, row.sledJumpChainChance - 0.001f)
            withClue("the sled ends") { rig.ticksUntil(2f) { rig.baseKey != "sled" } shouldBe true }
            rig.bossBody.getX() shouldBe (LEFT_INNER plusOrMinus 0.01f)
            rig.animKey shouldBe "fall_flash"
            rig.tick()
            rig.boss.facing shouldBe Facing.RIGHT
            rig.secondsWhile(2f) { rig.animKey == "fall_flash" } + STEP shouldBe (row.jumpTellDur plusOrMinus TOLERANCE)
            rig.animKey shouldBe "jump"
            val playerX = rig.playerBody.getCenter(Vector2()).x
            rig.bossBody.physics.velocity.x shouldBe
                ((playerX + 2f * T - rig.bossCenter().x) / AIR_TIME plusOrMinus 0.01f)
            rig.bossBody.physics.velocity.y shouldBe (15f * T plusOrMinus 0.01f)
            val shots = rig.spawnsOf<Snowhead>().size
            withClue("the jump lands") { rig.ticksUntil(3f) { rig.baseKey == "brake" } shouldBe true }
            (rig.bossCenter().x > playerX) shouldBe true
            rig.boss.facing shouldBe Facing.LEFT
            val landing = rig.spawnsOf<Snowhead>().drop(shots)
            landing.size shouldBe 1
            val shot = landing.single()
            val target = rig.playerBody.getCenter(Vector2())
            val dx = target.x - shot.position.x
            val time = (abs(dx) / (10f * T)).coerceIn(0.4f, 1f)
            shot.trajectory.x shouldBe (dx / time plusOrMinus 0.01f)
            shot.trajectory.y shouldBe ((target.y - shot.position.y) / time - 0.5f * Snowhead.GRAVITY * T * time
                plusOrMinus 0.01f)
        }

        it("the jump target is clamped to the arena") {
            rig = GlacierManRig()
            rig.walls[1] = GameRectangle(10f * T, FLOOR, 2f * T, 20f * T)
            rig.startFight(props = rig.spawnProps(bossX = 8f))
            val row = GlacierManTuning.NORMAL
            sledIntoLeftWall(9f, 5f)
            rig.rolls(FAIL, row.sledJumpChainChance - 0.001f)
            rig.untilKey("fall_flash", 2f)
            rig.place(8.5f)
            jumpOverStart()
            val landX = 10f * T - rig.bossBody.getWidth() / 2f
            rig.bossBody.physics.velocity.x shouldBe ((landX - rig.bossCenter().x) / AIR_TIME plusOrMinus 0.01f)
        }

        it("a roll at sledJumpChainChance fails: brake") {
            start()
            sledIntoLeftWall(8.5f, 4f)
            rig.rolls(FAIL, GlacierManTuning.NORMAL.sledJumpChainChance)
            withClue("the sled ends") { rig.ticksUntil(2f) { rig.baseKey != "sled" } shouldBe true }
            rig.baseKey shouldBe "brake"
        }

        it("with the player above at the end of the sled, he jumps at once with no tell") {
            start()
            sledIntoLeftWall(8.5f, 4f)
            rig.place(4f, topOverFloor = 2f)
            rig.rolls(FAIL, PASS)
            withClue("the sled ends") { rig.ticksUntil(2f) { rig.baseKey != "sled" } shouldBe true }
            rig.animKey shouldBe "jump"
            (rig.bossBody.physics.velocity.y > 0f) shouldBe true
        }

        it("with the player above at the end of a sled that stops on its timer, he jumps at once with no tell") {
            start()
            val row = GlacierManTuning.NORMAL
            rig.leaveStand((row.iceBlastWeight * row.farIceBlastScale + row.duckWeight + row.sledWeight / 2f) /
                farTotal(row)) shouldBe "brake_flash"
            rig.untilKey("sled")
            rig.place(6f, topOverFloor = 2f)
            withClue("the sled ends") { rig.ticksUntil(2f) { rig.baseKey != "sled" } shouldBe true }
            (rig.bossBody.getX() > LEFT_INNER + T) shouldBe true
            rig.animKey shouldBe "jump"
        }
    }

    describe("the duck chain") {
        val row = GlacierManTuning.NORMAL

        fun duck(far: Boolean) {
            start()
            if (far) {
                rig.leaveStand((row.iceBlastWeight * row.farIceBlastScale + row.duckWeight / 2f) / farTotal(row)) shouldBe
                    "duck"
            } else {
                rig.place(nearX())
                val near = row.iceBlastWeight + row.duckWeight + row.jumpWeight * row.nearJumpScale
                rig.leaveStand((row.iceBlastWeight + row.duckWeight / 2f) / near) shouldBe "duck"
            }
            val shots = rig.spawnsOf<Snowhead>().size
            rig.rolls(FAIL, FAIL)
            withClue("the duck shot") { rig.ticksUntil(1f) { rig.spawnsOf<Snowhead>().size > shots } shouldBe true }
        }

        fun afterDuck(): String? {
            withClue("the duck ends") { rig.ticksUntil(2f) { rig.baseKey != "duck" } shouldBe true }
            return rig.baseKey
        }

        it("a passing duckSledChainChance roll: sled with the tell") {
            duck(far = false)
            rig.rolls(row.duckSledChainChance - 0.001f)
            afterDuck() shouldBe "brake_flash"
            rig.untilKey("sled")
        }

        it("a failed roll with the player far: sled with the tell") {
            duck(far = true)
            rig.rolls(FAIL)
            afterDuck() shouldBe "brake_flash"
        }

        it("a failed roll with the player near and above: jump with the tell") {
            duck(far = false)
            rig.place(nearX(), topOverFloor = 2f)
            rig.rolls(FAIL)
            afterDuck() shouldBe "fall_flash"
            rig.untilKey("jump", 1f)
        }

        it("a roll at duckSledChainChance with the player near and not above: stand") {
            duck(far = false)
            rig.rolls(row.duckSledChainChance)
            afterDuck() shouldBe "stand"
        }
    }

    describe("the tells") {
        ALL_ROWS.forEach { row ->
            val tuning = row.tuning
            val total = farTotal(tuning)
            val ice = tuning.iceBlastWeight * tuning.farIceBlastScale
            listOf(
                Triple("sled", (ice + tuning.duckWeight + tuning.sledWeight / 2f) / total, "brake_flash" to tuning.sledTellDur),
                Triple("jump", (total - tuning.jumpWeight / 2f) / total, "fall_flash" to tuning.jumpTellDur),
                Triple("ice blast", ice / 2f / total, "throw_flash" to tuning.iceBlastTellDur)
            ).forEach { (name, fraction, tell) ->
                it("${row.name}: $name shows ${tell.first} for ${tell.second} s, facing the player, with no move") {
                    start(row)
                    rig.leaveStand(fraction) shouldBe tell.first
                    rig.lastPickTotal() shouldBe (total plusOrMinus 0.001f)
                    val x = rig.bossBody.getX()
                    var moved = false
                    var ticks = 0
                    val seconds = rig.secondsWhile(2f, each = {
                        ticks++
                        if (ticks == 3) rig.placePlayerCenter(rig.bossCenter().x + 3f * T, FLOOR + 0.5f * T)
                        if (rig.bossBody.physics.velocity.x != 0f || rig.bossBody.getX() != x) moved = true
                    }) {
                        if (ticks == 6) rig.boss.facing shouldBe Facing.RIGHT
                        rig.animKey == tell.first
                    }
                    seconds shouldBe (tell.second plusOrMinus TOLERANCE)
                    moved shouldBe false
                }
            }

            it("${row.name}: duck has no tell") {
                start(row)
                val ice = tuning.iceBlastWeight * tuning.farIceBlastScale
                rig.leaveStand((ice + tuning.duckWeight / 2f) / total) shouldBe "duck"
            }
        }
    }

    describe("the random times") {
        ALL_ROWS.forEach { row ->
            val tuning = row.tuning

            it("${row.name}: stand time and its one snowball time come from the row ranges") {
                start(row)
                val begin = rig.time
                val shots = rig.spawnsOf<Snowhead>().size
                val stand = rig.random.floatCalls(tuning.standDurMin, tuning.standDurMax).last().value
                val shot = rig.random.floatCalls(tuning.standShotMin, tuning.standShotMax).last().value
                (stand in tuning.standDurMin..tuning.standDurMax) shouldBe true
                (shot in tuning.standShotMin..tuning.standShotMax) shouldBe true
                rig.secondsWhile(3f) { inStand() } shouldBe (stand plusOrMinus TOLERANCE)
                val fired = rig.spawnsOf<Snowhead>().drop(shots)
                fired.size shouldBe 1
                (fired.single().time - begin) shouldBe (shot plusOrMinus TOLERANCE)
            }

            it("${row.name}: duck lasts duckDur with one shot in the duck shot range") {
                start(row)
                val total = farTotal(tuning)
                rig.leaveStand((tuning.iceBlastWeight * tuning.farIceBlastScale + tuning.duckWeight / 2f) / total) shouldBe
                    "duck"
                val begin = rig.time
                val shots = rig.spawnsOf<Snowhead>().size
                val shot = rig.random.floatCalls(tuning.duckShotMin, tuning.duckShotMax).last().value
                (shot in tuning.duckShotMin..tuning.duckShotMax) shouldBe true
                rig.random.rolls.clear()
                rig.secondsWhile(3f) { rig.baseKey == "duck" } shouldBe (tuning.duckDur plusOrMinus TOLERANCE)
                val fired = rig.spawnsOf<Snowhead>().drop(shots)
                fired.size shouldBe 1
                (fired.single().time - begin) shouldBe (shot plusOrMinus TOLERANCE)
            }

            it("${row.name}: sled shots at 0.5 s and 1 s, each +- sledShotJitter") {
                start(row)
                val total = farTotal(tuning)
                rig.leaveStand((tuning.iceBlastWeight * tuning.farIceBlastScale + tuning.duckWeight +
                    tuning.sledWeight / 2f) / total) shouldBe "brake_flash"
                val jitters = rig.random.floatCalls(-tuning.sledShotJitter, tuning.sledShotJitter)
                rig.untilKey("sled")
                val begin = rig.time
                val shots = rig.spawnsOf<Snowhead>().size
                val calls = rig.random.floatCalls(-tuning.sledShotJitter, tuning.sledShotJitter).drop(jitters.size)
                calls.size shouldBe 2
                rig.secondsWhile(3f) { rig.baseKey == "sled" }
                val fired = rig.spawnsOf<Snowhead>().drop(shots)
                fired.size shouldBe 2
                (fired[0].time - begin) shouldBe (0.5f + calls[0].value plusOrMinus TOLERANCE)
                (fired[1].time - begin) shouldBe (1f + calls[1].value plusOrMinus TOLERANCE)
            }
        }
    }

    describe("the allocation rule (O5-R1)") {
        it("canIceBlast, shoot and cellPoint make no math object or collection per call") {
            val source = File("src/main/kotlin/com/megaman/maverick/game/entities/bosses/GlacierMan.kt").readText()
            listOf("canIceBlast", "shoot", "cellPoint").forEach { name ->
                val body = functionBody(source, name)
                withClue(name) {
                    body.isNotEmpty() shouldBe true
                    ALLOCATION.findAll(body).map { it.value }.toList() shouldBe emptyList()
                }
            }
        }

        it("the scan finds an allocation") {
            val text = "fun shoot() {\n    val a = Vector2(1f, 2f)\n    val b = gdxArrayOf(1)\n}\n"
            ALLOCATION.findAll(functionBody(text, "shoot")).count() shouldBe 2
        }
    }
})

private val ALLOCATION = Regex(
    """(?<![\w.])(Vector2|GameRectangle|GameCircle|GameLine|Array|ObjectMap|ObjectSet|gdxArrayOf|objectMapOf|""" +
        """objectSetOf|arrayOf|listOf|mutableListOf|FloatArray|IntArray)\s*(<[^()]*>)?\(|\.cpy\(\)"""
)

private fun functionBody(source: String, name: String): String {
    val start = Regex("""\bfun\s+$name\s*\(""").find(source) ?: return ""
    val open = source.indexOf('{', start.range.last)
    var depth = 0
    for (i in open until source.length) {
        when (source[i]) {
            '{' -> depth++
            '}' -> {
                depth--
                if (depth == 0) return source.substring(open, i + 1)
            }
        }
    }
    return ""
}
