package com.megaman.maverick.game.entities.bosses.glacierman

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.entities.body
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.explosions.IceShard
import com.megaman.maverick.game.entities.projectiles.FallingIcicle
import com.megaman.maverick.game.entities.projectiles.FallingIcicle.FallingIcicleState
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.events.EventType
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlin.math.roundToInt

private const val TOLERANCE = 2f * STEP
private const val NARROW_RIGHT_INNER = 9f * T
private const val NARROW_BOSS_X = 8f

private val PHASE_1_ROWS = listOf(
    TuningRow("normal", hard = false, phase2 = false),
    TuningRow("hard", hard = true, phase2 = false)
)
private val PHASE_2_ROWS = listOf(
    TuningRow("normal phase 2", hard = false, phase2 = true),
    TuningRow("hard phase 2", hard = true, phase2 = true)
)

private fun farTotal(row: GlacierManTuning) = row.iceBlastWeight * row.farIceBlastScale + row.duckWeight +
    row.sledWeight + row.jumpWeight + row.icicleRainWeight + row.sledRamWeight

private fun rainFraction(row: GlacierManTuning, rainScale: Float = 1f, total: Float = farTotal(row)) =
    (row.iceBlastWeight * row.farIceBlastScale + row.duckWeight + row.sledWeight +
        row.icicleRainWeight * rainScale / 2f) / total

private fun waveDur(row: GlacierManTuning) = 0.2f + (row.icicleCount - 1) * row.icicleStagger + row.icicleWaveRest

class GlacierManIcicleRainTest : DescribeSpec({

    lateinit var rig: GlacierManRig

    afterEach { rig.dispose() }

    fun start(row: TuningRow = PHASE_2_ROWS[0], narrow: Boolean = false) {
        rig = GlacierManRig(hard = row.hard)
        if (narrow) {
            rig.walls[1] = GameRectangle(NARROW_RIGHT_INNER, FLOOR, 2f * T, 20f * T)
            rig.startFight(phase2 = row.phase2, props = rig.spawnProps(bossX = NARROW_BOSS_X))
        } else rig.startFight(phase2 = row.phase2)
    }

    fun pound(row: GlacierManTuning = GlacierManTuning.NORMAL_PHASE_2, rainScale: Float = 1f,
              total: Float = farTotal(row)): Float {
        rig.leaveStand(rainFraction(row, rainScale, total)) shouldBe "pound"
        return rig.time
    }

    fun icicles() = rig.spawnsOf<FallingIcicle>()

    fun columnOf(record: SpawnRecord): Int {
        val offset = (record.position.x - LEFT_INNER) / T - 0.5f
        offset shouldBe (offset.roundToInt().toFloat() plusOrMinus 0.001f)
        return offset.roundToInt()
    }

    fun columns(from: Int = 0) = icicles().drop(from).map { columnOf(it) }

    fun waitIcicles(count: Int, max: Float = 3f) =
        withClue("$count icicles") { rig.ticksUntil(max) { icicles().size >= count } shouldBe true }

    fun standCalls(row: GlacierManTuning) = rig.random.floatCalls(row.standDurMin, row.standDurMax).size

    fun untilRealStand(row: GlacierManTuning, max: Float = 6f) {
        val before = standCalls(row)
        withClue("STAND starts") { rig.ticksUntil(max) { standCalls(row) > before } shouldBe true }
    }

    fun gapPicks() = rig.random.calls.filter { it.kind == GlacierManRandom.Kind.INT }

    describe("the pick") {
        PHASE_1_ROWS.forEach { row ->
            it("${row.name}: icicleRainWeight is 0, so a roll at the top of the weights is jump") {
                row.tuning.icicleRainWeight shouldBe 0f
                start(row)
                rig.leaveStand(0.999f) shouldBe "fall_flash"
                rig.lastPickTotal() shouldBe (farTotal(row.tuning) plusOrMinus 0.001f)
            }
        }

        PHASE_2_ROWS.forEach { row ->
            it("${row.name}: a roll in the icicleRainWeight share starts Icicle Rain with pound and no tell") {
                (row.tuning.icicleRainWeight > 0f) shouldBe true
                start(row)
                pound(row.tuning)
                rig.lastPickTotal() shouldBe (farTotal(row.tuning) plusOrMinus 0.001f)
            }
        }
    }

    describe("the pound") {
        it("pound (5 x 0.1 s); at 0.2 s one SHAKE_CAM and the first icicle") {
            start()
            val shakes = rig.eventsOf(EventType.SHAKE_CAM)
            val begin = pound()
            val strip = rig.animator().animations["pound"] as Animation
            strip.size() shouldBe 5
            strip.getDuration() shouldBe (0.5f plusOrMinus 0.0001f)
            strip.isLooping() shouldBe false
            rig.secondsWhile(2f) { rig.animKey == "pound" } shouldBe (0.5f plusOrMinus TOLERANCE)
            rig.ticks(1f)
            rig.eventsOf(EventType.SHAKE_CAM) shouldBe shakes + 1
            val shakeTime = rig.eventTimes[rig.events.indexOfLast { it.key == EventType.SHAKE_CAM }]
            (shakeTime + STEP - begin) shouldBe (0.2f plusOrMinus TOLERANCE)
            (icicles().first().time + STEP - begin) shouldBe (0.2f plusOrMinus TOLERANCE)
        }
    }

    describe("the wave") {
        PHASE_2_ROWS.forEach { row ->
            val tuning = row.tuning
            it("${row.name}: icicleCount = ${tuning.icicleCount} icicles in 1-tile columns between the walls, " +
                "icicle i at 0.2 s + i x icicleStagger, with FORM, DELAY, DURATION and the owner") {
                start(row)
                val begin = pound(tuning)
                waitIcicles(tuning.icicleCount)
                rig.ticks(0.5f)
                val records = icicles()
                records.size shouldBe tuning.icicleCount
                val columns = columns()
                columns.toSet().size shouldBe tuning.icicleCount
                columns.forEach { (it in 0 until 24) shouldBe true }
                records.forEachIndexed { i, record ->
                    withClue("icicle $i") {
                        (record.time + STEP - begin) shouldBe (0.2f + i * tuning.icicleStagger plusOrMinus TOLERANCE)
                        record.props.get(ConstKeys.STATE) shouldBe FallingIcicleState.FORM
                        record.props.get(ConstKeys.DELAY, Float::class) shouldBe tuning.icicleStillDur
                        record.props.get(ConstKeys.DURATION, Float::class) shouldBe tuning.icicleShakeDur
                        record.props.get(ConstKeys.OWNER) shouldBe rig.boss
                    }
                }
            }

            it("${row.name}: an arena of 7 columns gives 1 lead icicle + 4 (the columns that are not the gap)") {
                start(row, narrow = true)
                rig.place(2.5f)
                pound(tuning)
                rig.ticks(2f)
                val columns = columns()
                columns.size shouldBe 5
                columns.toSet().size shouldBe 5
                columns.forEach { (it in 0 until 7) shouldBe true }
            }
        }
    }

    describe("the lead icicle") {
        listOf(
            "a running player" to (6f to 4f),
            "a lead point past the wall (clamped to the last column)" to (24f to 10f)
        ).forEach { (name, setup) ->
            it("$name: the first icicle is in the column of the OrbAim.LEAD point (icicleLeadTime)") {
                start()
                rig.place(setup.first)
                rig.playerBody.physics.velocity.x = setup.second * T
                pound()
                waitIcicles(1)
                val tuning = GlacierManTuning.NORMAL_PHASE_2
                val leadX = rig.playerBody.getCenter(Vector2()).x + setup.second * T * tuning.icicleLeadTime
                val expected = ((leadX.coerceIn(LEFT_INNER, RIGHT_INNER) - LEFT_INNER) / T).toInt().coerceIn(0, 23)
                columnOf(icicles().first()) shouldBe expected
            }
        }
    }

    describe("the safe gap") {
        listOf(0 to 1, 1 to 2, 2 to 3).forEach { (pick, gapStart) ->
            it("an arena of 7 columns, gap pick $pick: the only free columns are $gapStart and ${gapStart + 1}, " +
                "not the lead column 0 and not the boss columns 5 and 6") {
                start(narrow = true)
                rig.place(2.5f)
                rig.random.ints.addLast(pick)
                pound()
                rig.ticks(2f)
                val gap = gapPicks().first()
                gap.min shouldBe 0f
                gap.max shouldBe 2f
                val columns = columns()
                columns.first() shouldBe 0
                (0 until 7).filter { it !in columns } shouldBe listOf(gapStart, gapStart + 1)
            }
        }

        listOf(0 to 0, 13 to 15, 17 to 22).forEach { (pick, gapStart) ->
            it("an arena of 24 columns, gap pick $pick of 18 safe starts: columns $gapStart and ${gapStart + 1} " +
                "have no icicle") {
                start()
                rig.place(6f)
                rig.random.ints.addLast(pick)
                pound()
                rig.ticks(2f)
                val gap = gapPicks().first()
                gap.max shouldBe 17f
                val columns = columns()
                columns.first() shouldBe 4
                columns.contains(gapStart) shouldBe false
                columns.contains(gapStart + 1) shouldBe false
            }
        }
    }

    describe("the ceiling line") {
        it("each icicle forms with its top on the lowest block bottom over its column at or above the top of " +
            "Glacier Man's body; 14 tiles above his feet with none") {
            start(narrow = true)
            rig.addBlock(2f * T, FLOOR + 6f * T, 3f * T, T)
            rig.addBlock(3f * T, FLOOR + 4f * T, T, T)
            rig.addBlock(7f * T, rig.bossBody.getMaxY(), T, T)
            rig.addBlock(8f * T, rig.bossBody.getMaxY() - 0.25f * T, T, T)
            rig.place(2.5f)
            rig.random.ints.addLast(2)
            pound()
            rig.ticks(2f)
            val expected = mapOf(
                0 to FLOOR + 6f * T,
                1 to FLOOR + 4f * T,
                2 to FLOOR + 6f * T,
                5 to FLOOR + 1.75f * T,
                6 to FLOOR + 14f * T
            )
            val records = icicles()
            records.map { columnOf(it) }.toSet() shouldBe expected.keys
            records.forEach { record ->
                withClue("column ${columnOf(record)}") {
                    record.position.y shouldBe (expected[columnOf(record)]!! plusOrMinus 0.01f)
                }
            }
        }
    }

    describe("the double rain") {
        it("phase 2 under 25 % health: 2 pounds, each wave with its own lead icicle and gap; the second pound " +
            "starts only when no icicle of the first wave is alive") {
            start()
            rig.boss.setHealth(7)
            val tuning = GlacierManTuning.NORMAL_PHASE_2
            val shakes = rig.eventsOf(EventType.SHAKE_CAM)
            rig.place(6f)
            pound()
            waitIcicles(tuning.icicleCount)
            rig.ticks(waveDur(tuning) + 0.5f)
            val stands = standCalls(tuning)
            val first = icicles().map { it.entity as FallingIcicle }
            first.size shouldBe tuning.icicleCount
            columnOf(icicles().first()) shouldBe 4
            rig.place(14f)
            for (i in 0 until first.size - 1) {
                first[i].destroy()
                rig.ticks(0.2f)
            }
            rig.eventsOf(EventType.SHAKE_CAM) shouldBe shakes + 1
            icicles().size shouldBe tuning.icicleCount
            (rig.animKey == "pound") shouldBe false
            first.last().destroy()
            withClue("the second pound") { rig.ticksUntil(0.2f) { rig.animKey == "pound" } shouldBe true }
            val begin = rig.time
            waitIcicles(2 * tuning.icicleCount)
            rig.eventsOf(EventType.SHAKE_CAM) shouldBe shakes + 2
            columnOf(icicles()[tuning.icicleCount]) shouldBe 12
            gapPicks().size shouldBe 2 * 17
            gapPicks()[17].max shouldBe 17f
            columns(tuning.icicleCount).toSet().size shouldBe tuning.icicleCount
            untilRealStand(tuning)
            (rig.time - begin) shouldBe (waveDur(tuning) plusOrMinus TOLERANCE)
            standCalls(tuning) shouldBe stands + 1
            icicles().size shouldBe 2 * tuning.icicleCount
        }

        listOf(8 to 1, 30 to 1).forEach { (health, waves) ->
            it("phase 2 at health $health: $waves wave") {
                start()
                rig.boss.setHealth(health)
                val tuning = GlacierManTuning.NORMAL_PHASE_2
                val begin = pound()
                untilRealStand(tuning)
                (rig.time - begin) shouldBe (waveDur(tuning) plusOrMinus TOLERANCE)
                icicles().size shouldBe tuning.icicleCount
            }
        }

        it("phase 1 under 25 % health has no Icicle Rain (weight 0)") {
            start(PHASE_1_ROWS[0])
            rig.boss.setHealth(5)
            rig.leaveStand(0.5f)
            rig.lastPickTotal() shouldBe (farTotal(GlacierManTuning.NORMAL) plusOrMinus 0.001f)
        }
    }

    describe("the end of the move") {
        PHASE_2_ROWS.forEach { row ->
            val tuning = row.tuning
            it("${row.name}: STAND at 0.2 s + (icicleCount - 1) x icicleStagger + icicleWaveRest") {
                start(row)
                val begin = pound(tuning)
                untilRealStand(tuning)
                (rig.time - begin) shouldBe (waveDur(tuning) plusOrMinus TOLERANCE)
            }
        }
    }

    describe("the repeat rule") {
        it("Icicle Rain gets x lastMoveScale after one rain and weight 0 after two") {
            start()
            val row = GlacierManTuning.NORMAL_PHASE_2
            pound()
            untilRealStand(row)
            val second = farTotal(row) - row.icicleRainWeight + row.icicleRainWeight * row.lastMoveScale
            pound(row, rainScale = row.lastMoveScale, total = second)
            rig.lastPickTotal() shouldBe (second plusOrMinus 0.001f)
            untilRealStand(row)
            rig.leaveStand(0.5f)
            rig.lastPickTotal() shouldBe (farTotal(row) - row.icicleRainWeight plusOrMinus 0.001f)
        }
    }

    describe("a FIRED stun") {
        it("stops the spawns of the wave; the spawned icicles still drop") {
            start()
            pound()
            waitIcicles(2)
            val spawned = icicles().map { it.entity as FallingIcicle }
            spawned.forEach { rig.alsoTick.add(it) }
            rig.boss.takeDamageFrom(Fireball(rig.services)) shouldBe true
            rig.tick()
            rig.animKey shouldBe "fired"
            rig.secondsWhile(2f) { rig.animKey == "fired" }
            rig.ticks(0.4f)
            icicles().size shouldBe 2
            spawned.forEach { icicle ->
                icicle.existenceState.liveNow shouldBe true
                icicle.body.physics.gravityOn shouldBe true
            }
        }
    }

    describe("the end of the fight") {
        fun foreignIcicle(): FallingIcicle {
            val icicle = MegaEntityFactory.fetch(FallingIcicle::class)!!
            icicle.spawn(props(ConstKeys.POSITION pairTo Vector2(10f * T, 10f * T))) shouldBe true
            rig.processSpawns()
            return icicle
        }

        it("the defeat shatters the boss's live icicles (5 shards each) and no other icicle") {
            start()
            pound()
            waitIcicles(3)
            val own = icicles().map { it.entity as FallingIcicle }
            val other = foreignIcicle()
            repeat(15) { rig.boss.takeDamageFrom(rig.bullet()) }
            rig.checkPoints()
            rig.boss.defeated shouldBe true
            own.forEach { rig.destroyed.contains(it, true) shouldBe true }
            rig.destroyed.contains(other, true) shouldBe false
            rig.spawnsOf<IceShard>().count { it.props.get(ConstKeys.TAG) == FallingIcicle.TAG } shouldBe 5 * own.size
            val count = icicles().size
            rig.ticks(1f)
            icicles().size shouldBe count
        }

        it("a destroy of the boss destroys its icicles, with no shards, and no other icicle") {
            start()
            pound()
            waitIcicles(3)
            val own = icicles().map { it.entity as FallingIcicle }
            val other = foreignIcicle()
            rig.boss.destroy()
            rig.processDestroys()
            own.forEach { rig.destroyed.contains(it, true) shouldBe true }
            rig.destroyed.contains(other, true) shouldBe false
            rig.spawnsOf<IceShard>().size shouldBe 0
        }
    }
})
