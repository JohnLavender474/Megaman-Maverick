package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.maverick.game.engine.common.UtilMethods
import com.maverick.game.engine.common.extensions.objectSetOf
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.events.IEventListener
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.difficulty.DifficultyMode
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.hazards.D1000CeilingDebris
import com.megaman.maverick.game.entities.hazards.D1000Pillar
import com.megaman.maverick.game.entities.hazards.D1000Shockwave
import com.megaman.maverick.game.entities.megamanx.MegamanXValues
import com.megaman.maverick.game.entities.projectiles.D1000Bomb
import com.megaman.maverick.game.entities.projectiles.JunkShot
import com.megaman.maverick.game.entities.state.BossPhase
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.world.body.FixtureType
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.unmockkAll
import kotlin.math.abs
import com.megaman.maverick.game.entities.bosses.d1000.D1000Moves
import com.megaman.maverick.game.entities.bosses.d1000.D1000XMoves
import com.megaman.maverick.game.entities.bosses.d1000.D1000Escape
import com.megaman.maverick.game.entities.bosses.d1000.D1000Tier
import com.megaman.maverick.game.entities.bosses.d1000.D1000Action
import com.megaman.maverick.game.entities.bosses.d1000.D1000Director
import com.megaman.maverick.game.entities.bosses.d1000.D1000Tuning
import com.megaman.maverick.game.entities.bosses.d1000.D1000Orbs
import com.megaman.maverick.game.entities.bosses.d1000.D1000Ceiling

private const val TOLERANCE = 0.01f
private const val REACTION = 0.25f
private const val LEFT_RAM_STOP = RIG_ARENA_X + 2.1875f * RIG_PPM
private const val ROOM_SECONDS = 1f
private const val ROOM_LOOK = 4f * RIG_PPM
private const val ROOM_TIE = 0.01f * RIG_PPM

private class D1000Dodger(val rig: D1000Rig, val hard: Boolean) {

    val hits = ArrayList<String>()
    val stunHits = ArrayList<String>()
    val stuns = ArrayList<FloatArray>()
    var time = 0f
    var moving = true
    var bodyCounts = true
    var shotsOnlyInStun = false
    private var info = ""

    private val firstSeen = HashMap<IGameEntity, Float>()
    private val hitBy = HashSet<IGameEntity>()
    private val moves = D1000Moves(MegamanXValues.WALK_SPEED * RIG_PPM, 0f, 0f, 0f)
    private val step = MegamanXValues.WALK_SPEED * RIG_PPM * RIG_DELTA
    private val fixtures = Array<IFixture>()

    init {
        rig.flyBombs = true
        rig.flyShots = true
        rig.tickWaves = true
        rig.services.events.addListener(object : IEventListener {
            override val eventKeyMask: ObjectSet<Any> = objectSetOf(EventType.STUN_PLAYER)
            override fun onEvent(event: Event) {
                stuns.add(floatArrayOf(time, time + event.getProperty(ConstKeys.DURATION, Float::class)!!))
            }
        })
    }

    private fun stunLeft() = stuns.maxOfOrNull { it[1] - time }?.coerceAtLeast(0f) ?: 0f

    private fun seen(entity: MegaGameEntity): Boolean {
        if (entity is D1000Pillar || entity is D1000CeilingDebris) return true
        return time - firstSeen.getOrPut(entity) { time } >= REACTION
    }

    private fun bodyOf(entity: IGameEntity): Body = entity.getComponent(BodyComponent::class)!!.body

    private fun damagers(entity: IGameEntity): List<GameRectangle> {
        fixtures.clear()
        bodyOf(entity).getFixtures(fixtures, FixtureType.DAMAGER)
        return fixtures.filter { it.isActive() }.map { GameRectangle(it.getShape() as GameRectangle) }
    }

    private fun bossBoxes(): Array<GameRectangle> {
        val out = Array<GameRectangle>()
        damagers(rig.boss).forEach { out.add(it) }
        return out
    }

    private fun safeNow(x: Float): Boolean {
        val escape = rig.boss.guard.escape
        val half = D1000XMoves.HALF_WIDTH
        for (i in 0 until escape.count) {
            val danger = escape.danger(i)
            if (danger.stun || danger.jumpable || danger.wallOnly || danger.from > RIG_DELTA) continue
            if (x + half > danger.left && x - half < danger.right) return false
        }
        return true
    }

    private fun room(x: Float): Float {
        val escape = rig.boss.guard.escape
        var room = ROOM_LOOK
        for (i in 0 until escape.count) {
            val danger = escape.danger(i)
            if (danger.stun || danger.jumpable || danger.wallOnly || danger.from > ROOM_SECONDS) continue
            val gap = if (x < danger.left) danger.left - x else if (x > danger.right) x - danger.right else 0f
            room = minOf(room, gap)
        }
        return room
    }

    fun tick() {
        val x = rig.xBody.getCenter(Vector2()).x
        val stunned = stunLeft() > 0f
        if (moving && !stunned) {
            val guard = rig.boss.guard
            guard.begin(rig.arena, rig.xBody, if (hard) D1000Moves.HARD else D1000Moves.NORMAL) { seen(it) }
            guard.escape.stun(0f, stunLeft())
            rig.boss.forecastCurrentAction()
            guard.addFootprint(bossBoxes(), 0f, 0f, D1000Escape.HORIZON, true)
            val next = listOf(x, x - step, x + step).filter { c ->
                c - D1000XMoves.HALF_WIDTH >= rig.arena.getX() && c + D1000XMoves.HALF_WIDTH <= rig.arena.getMaxX() &&
                    safeNow(c) && guard.escape.hasEscape(c, rig.arena.getX(), rig.arena.getMaxX(), moves)
            }.maxByOrNull { room(it) + if (it == x) ROOM_TIE else 0f }
            info = if (next != null) "" else {
                val escape = guard.escape
                "STUCK " + (0 until escape.count).joinToString(" ") { i ->
                    val d = escape.danger(i)
                    "[%.2f..%.2f %.2f..%.2f%s]".format(
                        (d.left - RIG_ARENA_X) / RIG_PPM, (d.right - RIG_ARENA_X) / RIG_PPM, d.from, d.to,
                        if (d.stun) "S" else if (d.jumpable) "J" else if (d.wallOnly) "W" else ""
                    )
                }
            }
            rig.placeX(((next ?: x) - RIG_ARENA_X) / RIG_PPM, (rig.xBody.getY() - RIG_FLOOR) / RIG_PPM)
        } else info = if (stunned) "stunned" else "still"
        rig.tick()
        rig.services.events.run()
        time += RIG_DELTA
        checkHits()
    }

    private fun checkHits() {
        val box = GameRectangle(rig.xBody.getX(), rig.xBody.getY(), rig.xBody.getWidth(), rig.xBody.getHeight())
        val stunned = stunLeft() > 0f
        fun hit(entity: IGameEntity, label: String, boxes: List<GameRectangle>) {
            if (hitBy.contains(entity) || boxes.none { it.overlaps(box) }) return
            hitBy.add(entity)
            if (stunned && entity is JunkShot && entity.type == JunkShot.Type.CORE) stunHits.add("%.2f s".format(time))
            hits.add("$label at %.2f s, X at %.2f tiles $info".format(time, (box.getX() + box.getWidth() / 2f - RIG_ARENA_X) / RIG_PPM).take(1500))
        }
        rig.spawned.forEach { entity ->
            if (rig.destroyed.contains(entity, true)) return@forEach
            when (entity) {
                is D1000Pillar -> hit(entity, "pillar %.2f".format((entity.baseX - RIG_ARENA_X) / RIG_PPM), damagers(entity))
                is JunkShot -> {
                    val velocity = bodyOf(entity).physics.velocity
                    val core = entity.type == JunkShot.Type.CORE
                    val flat = core && abs(velocity.y) < abs(velocity.x)
                    val dodged = core && (flat || shotsOnlyInStun)
                    if (!dodged || stunned) hit(entity, "junk ${entity.type}", damagers(entity))
                }
                is D1000CeilingDebris -> hit(entity, "debris ${entity.key}", damagers(entity))
                is D1000Shockwave -> if (stunned) hit(entity, "shockwave", damagers(entity))
                is D1000Core -> hit(entity, "eye", damagers(entity))
            }
        }
        if (bodyCounts) {
            val boxes = damagers(rig.boss)
            if (boxes.any { it.overlaps(box) }) hits.add("body at %.2f s".format(time))
        }
    }

    fun run(seconds: Float) {
        val end = time + seconds
        while (time < end) tick()
    }
}

class D1000TrapTest : DescribeSpec({

    var rig: D1000Rig? = null

    afterEach {
        rig?.dispose()
        rig = null
        MegaEntityFactory.reset()
        unmockkAll()
    }

    fun newRig(arenaTiles: Float = RIG_ARENA_WIDTH / RIG_PPM, hard: Boolean = false) =
        D1000Rig(arenaWidth = arenaTiles * RIG_PPM).also {
            rig = it
            if (hard) it.services.gameState.setDifficultyMode(DifficultyMode.HARD)
        }

    fun overdrive(rig: D1000Rig) {
        rig.spawnBoss()
        rig.hitEye(3)
        rig.ticks(ENTRY_SECONDS)
        rig.boss.bossComponent.phase = BossPhase.FIGHT
        rig.ticks(1.15f)
    }

    fun tiles(rig: D1000Rig, px: Float) = (px - rig.arena.getX()) / RIG_PPM

    val opening = d1000Context(cyclesSinceCombo = D1000Director.comboGap(D1000Tier.NORMAL, false))

    describe("the ram") {
        it("is refused when X is pinned at the wall with too short a run, and the body never rams the wall") {
            val rig = newRig(9.5f)
            rig.placeX(1f, 0f)
            rig.startFight()
            rig.rollFor(D1000Action.WALK, opening)
            var last = rig.bossCenter().x
            rig.ticksUntil(3f) {
                val now = rig.bossCenter().x
                val stopped = now == last && now < RIG_ARENA_X + 9f * RIG_PPM
                last = now
                stopped
            } shouldBe true
            rig.rollFor(D1000Action.RAM, d1000Context(last = D1000Action.WALK, cyclesSinceCombo = 5))
            rig.ticksUntil(2f) { rig.boss.guard.refused.contains(D1000Action.RAM) } shouldBe true
            rig.ticksUntil(4f) { rig.bossCenter().x <= LEFT_RAM_STOP + TOLERANCE } shouldBe false
        }

        it("is allowed with a long run, and the eye holds its shots from the wind-up to the wall") {
            val rig = newRig()
            rig.placeX(1f, 0f)
            val dodger = D1000Dodger(rig, false)
            rig.startFight()
            rig.rollFor(D1000Action.RAM, opening)
            dodger.moving = false
            dodger.bodyCounts = false
            dodger.run(0.45f)
            rig.boss.guard.refused.contains(D1000Action.RAM) shouldBe false
            val shots = rig.spawnedOf<JunkShot>().count { it.type == JunkShot.Type.CORE }
            var hitWall = false
            while (!hitWall && dodger.time < 5f) {
                dodger.tick()
                hitWall = rig.bossCenter().x <= LEFT_RAM_STOP + TOLERANCE
            }
            hitWall shouldBe true
            rig.spawnedOf<JunkShot>().count { it.type == JunkShot.Type.CORE } shouldBe shots
        }
    }

    describe("the owner's case: X at the wall while the body rams, the eye fires and debris falls") {
        listOf(false, true).forEach { hard ->
            it("X drops under the ram, and then escapes the stun, the junk and the eye (${if (hard) "Hard" else "Normal"})") {
                val rig = newRig(hard = hard)
                rig.placeX(1f, 4.45f)
                val dodger = D1000Dodger(rig, hard)
                rig.startFight()
                rig.rollFor(D1000Action.RAM, opening.copy(hard = hard))
                dodger.moving = false
                val start = rig.bossCenter().x
                while (rig.bossCenter().x == start && dodger.time < 4f) dodger.tick()
                var speed = 0f
                while (rig.xBody.getY() > RIG_FLOOR) {
                    speed -= MegamanXValues.FALL_GRAVITY * RIG_PPM * RIG_DELTA
                    val feet = (rig.xBody.getY() - RIG_FLOOR - speed * RIG_DELTA).coerceAtLeast(0f)
                    rig.placeX(1f, feet / RIG_PPM)
                    dodger.tick()
                }
                dodger.moving = true
                dodger.run(3f)
                withClue("hits: ${dodger.hits}") { dodger.hits.isEmpty() shouldBe true }
                rig.spawnedOf<D1000CeilingDebris>().isNotEmpty() shouldBe true
                dodger.stuns.isNotEmpty() shouldBe true
            }
        }
    }

    describe("the Ceiling Crash") {
        it("tracks X only as far as a 1.25-tile pocket at the wall, so the drop never pins X") {
            val rig = newRig(18.5f)
            rig.placeX(0.5f, 0f)
            rig.startFight()
            rig.rollFor(D1000Action.CEILING, opening)
            rig.ticks(4f)
            val left = rig.bossBody().let { body ->
                val out = Array<IFixture>()
                body.getFixtures(out, FixtureType.DAMAGER)
                out.filter { it.isActive() }.minOf { (it.getShape() as GameRectangle).getX() }
            }
            (left - rig.arena.getX() >= D1000Ceiling.CEILING_POCKET - TOLERANCE) shouldBe true
        }

        listOf(false, true).forEach { hard ->
            it("in Overdrive, an X that stands in the wall pocket is not hit by the junk lanes, the drop, the waves or an eye shot in its stun (${if (hard) "Hard" else "Normal"})") {
                val rig = newRig(hard = hard)
                rig.placeX(0.6f, 0f)
                val dodger = D1000Dodger(rig, hard)
                dodger.moving = false
                dodger.shotsOnlyInStun = true
                overdrive(rig)
                rig.rollFor(
                    D1000Action.CEILING,
                    opening.copy(tier = D1000Tier.OVERDRIVE, cyclesSinceCombo = D1000Director.comboGap(D1000Tier.OVERDRIVE, hard), hard = hard)
                )
                dodger.run(4.5f)
                withClue("hits: ${dodger.hits}") { dodger.hits.isEmpty() shouldBe true }
                dodger.stuns.isNotEmpty() shouldBe true
            }
        }
    }

    describe("the director with live hazards") {
        it("plans a Bracket on an X at the wall; the slower orbs outlast a live pillar, so the check is at the release") {
            val rig = newRig()
            rig.placeX(1f, 0f)
            rig.startFight()
            rig.rollFor(D1000Action.BRACKET, opening)
            rig.ticksUntil(6f) { rig.spawnedOf<D1000Bomb>().isNotEmpty() } shouldBe true
            rig.boss.guard.refused.contains(D1000Action.BRACKET) shouldBe false
        }

        it("checks each orb again at its release: an orb that would close X's last gap is held, a later one is thrown") {
            val rig = newRig()
            rig.flyBombs = true
            rig.placeX(1f, 0f)
            rig.startFight()
            rig.rollFor(D1000Action.BRACKET, opening)
            rig.ticksUntil(3f) { rig.boss.chargeProgress() != null } shouldBe true
            rig.boss.guard.refused.contains(D1000Action.BRACKET) shouldBe false
            val lane = RIG_ARENA_X + 3f * RIG_PPM
            MegaEntityFactory.fetch(D1000Bomb::class)!!.spawn(
                props(
                    ConstKeys.OWNER pairTo rig.boss,
                    ConstKeys.POSITION pairTo Vector2(lane, RIG_FLOOR + RIG_ARENA_HEIGHT - 0.5f * RIG_PPM),
                    ConstKeys.TARGET pairTo lane,
                    ConstKeys.BOUNDS pairTo rig.arena
                )
            )
            fun onX() = rig.spawnedOf<D1000Bomb>().count { abs(it.targetX - (RIG_ARENA_X + RIG_PPM)) < 0.2f * RIG_PPM }
            rig.ticksUntil(1f) { rig.boss.chargeProgress() == null } shouldBe true
            rig.tick()
            onX() shouldBe 0
            rig.ticksUntil(D1000Tuning.GUIDE_SIGNAL_DUR + D1000Orbs.GUIDE_FLIGHT_MAX + D1000Orbs.MARK_DUR + 0.1f) {
                rig.spawnedOf<D1000Bomb>().size >= 2
            } shouldBe true
        }
    }

    describe("the walk") {
        it("stops 1.5 tiles before X, so it never pins X at the wall") {
            val rig = newRig(18.5f)
            rig.placeX(1f, 0f)
            rig.startFight()
            rig.rollFor(D1000Action.WALK, opening)
            rig.ticks(2.5f)
            val front = rig.bossBody().let { body ->
                val out = Array<IFixture>()
                body.getFixtures(out, FixtureType.DAMAGER)
                out.filter { it.isActive() }.minOf { (it.getShape() as GameRectangle).getX() }
            }
            (front - rig.xBody.getMaxX() >= D1000.WALK_GAP - 0.2f * RIG_PPM) shouldBe true
        }
    }

    describe("a whole fight with a dodging X") {
        listOf(false, true).forEach { hard ->
            listOf(D1000Tier.NORMAL, D1000Tier.OVERDRIVE).forEach { tier ->
                it("no eye shot lands while X is stunned, with an X that walks out of what it sees (${if (hard) "Hard" else "Normal"}, $tier)") {
                    val rig = newRig(18.5f, hard)
                    rig.placeX(3f, 0f)
                    val dodger = D1000Dodger(rig, hard)
                    dodger.bodyCounts = false
                    if (tier == D1000Tier.OVERDRIVE) overdrive(rig) else rig.startFight()
                    UtilMethods.random.setSeed(if (hard) 11L else if (tier == D1000Tier.NORMAL) 5L else 7L)
                    dodger.run(45f)
                    dodger.stuns.isNotEmpty() shouldBe true
                    withClue("eye shots that hit X in a stun: ${dodger.stunHits}") { dodger.stunHits.isEmpty() shouldBe true }
                }
            }
        }
    }
})
