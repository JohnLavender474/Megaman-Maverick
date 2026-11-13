package com.megaman.maverick.game.entities.bosses.d1000

import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.world.body.IBody
import com.maverick.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.decorations.D1000DebrisWarning
import com.megaman.maverick.game.entities.hazards.D1000CeilingDebris
import com.megaman.maverick.game.entities.hazards.D1000Pillar
import com.megaman.maverick.game.entities.hazards.D1000Shockwave
import com.megaman.maverick.game.entities.projectiles.D1000Bomb
import com.megaman.maverick.game.entities.projectiles.EnemyShot
import com.megaman.maverick.game.world.body.getCenter
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import com.megaman.maverick.game.entities.bosses.D1000
import com.megaman.maverick.game.entities.bosses.D1000Core

// TODO: the D-1000 fight (its behaviors, mechanics, tuning and anti-trap rules) was AI-generated from owner play tests; it needs more scrutiny and refinement (see the branch NOTES of 081f, section "AI-generated fight")
class D1000TrapGuard(private val boss: D1000) {

    companion object {
        const val SLICE_STEPS = 6
        const val PILLAR_SLACK = 0.05f
        val SHOT_RADIUS = EnemyShot.Kind.CORE.width * ConstVals.PPM / 2f
    }

    val escape = D1000Escape()
    val refused = HashSet<D1000Action>()
    private val eyeFixtures = Array<IFixture>()
    private val band = GameRectangle()

    val arena = GameRectangle()
    var moves = D1000Moves.NORMAL
        private set
    var playerX = 0f
        private set
    var bandBottom = 0f
        private set
    var bandTop = 0f
        private set
    var shotsAirUntil = 0f
        private set

    fun begin(arena: GameRectangle, player: IBody, moves: D1000Moves, seen: (MegaGameEntity) -> Boolean = { true }) {
        this.arena.set(arena)
        this.moves = moves
        playerX = player.getCenter().x
        bandBottom = player.getY()
        bandTop = player.getMaxY()
        escape.clear()
        shotsAirUntil = 0f
        addLive(seen)
    }

    fun escapes() = escapesFrom(playerX)

    fun escapesFrom(x: Float, start: Float = 0f) = escape.hasEscape(x, arena.getX(), arena.getMaxX(), moves, start)

    private fun addLive(seen: (MegaGameEntity) -> Boolean) {
        MegaGameEntities.getOfTag(D1000Pillar.TAG).forEach { pillar ->
            if (pillar !is D1000Pillar || pillar.horizontal || !seen(pillar) || !pillar.existenceState.liveAfterUpdate) return@forEach
            if (pillar.dangerUntil >= 0f) addPillar(pillar.baseX, pillar.dangerIn, pillar.dangerUntil)
        }
        MegaGameEntities.getOfTag(D1000Bomb.TAG).forEach { bomb ->
            if (bomb !is D1000Bomb || bomb.horizontal || !seen(bomb) || !bomb.existenceState.liveAfterUpdate || bomb.owner != boss) return@forEach
            addPillar(bomb.targetX, bomb.landsIn() + D1000Pillar.START_DUR, bomb.landsIn() + D1000Pillar.START_DUR + D1000Pillar.ACTIVE_DUR)
        }
        MegaGameEntities.getOfTag(D1000DebrisWarning.TAG).forEach { warning ->
            if (warning !is D1000DebrisWarning || !seen(warning) || !warning.existenceState.liveAfterUpdate) return@forEach
            warning.pieces.forEach { piece ->
                if (piece.dropped) return@forEach
                addPiece(piece.startX, warning.hangPoint.y, piece.velocity.x, piece.velocity.y, piece.radius, warning.dropsIn(piece))
            }
        }
        MegaGameEntities.getOfTag(D1000CeilingDebris.TAG).forEach { piece ->
            if (piece !is D1000CeilingDebris || !seen(piece) || !piece.existenceState.liveAfterUpdate) return@forEach
            val body = piece.body
            val center = body.getCenter()
            addPiece(center.x, center.y, body.physics.velocity.x, body.physics.velocity.y, D1000CeilingDebris.radius(piece.key), 0f)
        }
        MegaGameEntities.getOfTag(EnemyShot.TAG).forEach { shot ->
            if (shot !is EnemyShot || !seen(shot) || !shot.existenceState.liveAfterUpdate || shot.owner != boss) return@forEach
            val body = shot.body
            val center = body.getCenter()
            val first = escape.count
            addBallistic(
                center.x, center.y, body.physics.velocity.x, body.physics.velocity.y, body.physics.gravity.y,
                shot.kind.width * ConstVals.PPM / 2f, 0f
            )
            if (shot.kind == EnemyShot.Kind.CORE) for (i in first until escape.count)
                shotsAirUntil = max(shotsAirUntil, escape.danger(i).to)
        }
        MegaGameEntities.getOfTag(D1000Core.TAG).forEach { eye ->
            if (eye !is D1000Core || !seen(eye) || !eye.existenceState.liveAfterUpdate) return@forEach
            eyeFixtures.clear()
            eye.body.getFixtures(eyeFixtures, FixtureType.DAMAGER)
            eyeFixtures.forEach { fixture ->
                if (!fixture.isActive()) return@forEach
                val box = fixture.getShape() as GameRectangle
                if (box.getY() >= bandTop || box.getMaxY() <= bandBottom) return@forEach
                escape.add(box.getX(), box.getMaxX(), 0f, D1000Escape.HORIZON)
            }
        }
        MegaGameEntities.getOfTag(D1000Shockwave.TAG).forEach { wave ->
            if (wave !is D1000Shockwave || !seen(wave) || !wave.existenceState.liveAfterUpdate) return@forEach
            if (wave.onFloor) addShockwave(wave.center.x, wave.direction, 0f)
            else addWallWave(wave.direction, wave.center.y - D1000Shockwave.LENGTH / 2f, 0f)
        }
    }

    fun addPillar(x: Float, from: Float, to: Float) =
        escape.add(
            x - D1000Pillar.WIDTH / 2f, x + D1000Pillar.WIDTH / 2f, from - PILLAR_SLACK, to + PILLAR_SLACK, fullHeight = true
        )

    fun addDebris(x: Float, y: Float, spread: Float, from: Float) {
        D1000CeilingDebris.landingBand(band, x, spread, arena.getX() + D1000Ceiling.CEILING_POCKET, arena.getMaxX() - D1000Ceiling.CEILING_POCKET)
        val radius = D1000CeilingDebris.MAX_RADIUS
        val fast = D1000CeilingDebris.fallTime(y - radius - bandTop, D1000CeilingDebris.MAX_START_SPEED)
        val slow = D1000CeilingDebris.fallTime(y - arena.getY(), 0f) + D1000CeilingDebris.MAX_LAST_DELAY
        escape.add(band.getX(), band.getMaxX(), from + fast, from + slow)
        escape.add(band.getX(), band.getMaxX(), from, from + slow, wallOnly = true)
    }

    private fun addPiece(x: Float, y: Float, vx: Float, vy: Float, radius: Float, from: Float) {
        addBallistic(x, y, vx, vy, -D1000CeilingDebris.GRAVITY, radius, from)
        val time = D1000CeilingDebris.fallTime(y - arena.getY(), -vy)
        val landing = x + vx * time
        escape.add(min(x, landing) - radius, max(x, landing) + radius, from, from + time, wallOnly = true)
    }

    fun addBallistic(x: Float, y: Float, vx: Float, vy: Float, gravity: Float, radius: Float, from: Float) {
        val flat = gravity == 0f && abs(vy) < abs(vx)
        var time = 0f
        var sliceStart = -1f
        var sliceLeft = 0f
        var sliceRight = 0f
        var steps = 0
        while (from + time <= D1000Escape.HORIZON) {
            val px = x + vx * time
            val py = y + vy * time + 0.5f * gravity * time * time
            val inBand = py - radius < bandTop && py + radius > bandBottom
            if (inBand) {
                if (sliceStart < 0f) {
                    sliceStart = time
                    sliceLeft = px
                    sliceRight = px
                }
                sliceLeft = min(sliceLeft, px)
                sliceRight = max(sliceRight, px)
                steps++
            }
            if (sliceStart >= 0f && (!inBand || steps >= SLICE_STEPS)) {
                escape.add(sliceLeft - radius, sliceRight + radius, from + sliceStart, from + time, flat)
                sliceStart = -1f
                steps = 0
            }
            val falling = vy + gravity * time < 0f
            if (py + radius < bandBottom && falling) break
            if (px < arena.getX() - radius || px > arena.getMaxX() + radius) break
            time += D1000Escape.STEP
        }
        if (sliceStart >= 0f) escape.add(sliceLeft - radius, sliceRight + radius, from + sliceStart, from + time, flat)
    }

    fun stunsBetween(from: Float, to: Float, first: Int = 0): Boolean {
        for (i in first until escape.count) {
            val danger = escape.danger(i)
            if (danger.stun && danger.from < to && danger.to > from) return true
        }
        return false
    }

    fun addShockwave(x: Float, direction: Facing, from: Float) {
        val wall = if (direction == Facing.RIGHT) arena.getMaxX() - D1000Shockwave.THICKNESS / 2f
        else arena.getX() + D1000Shockwave.THICKNESS / 2f
        val half = D1000Shockwave.LENGTH / 2f
        val floorHit = bandBottom < arena.getY() + D1000Shockwave.THICKNESS
        val slice = D1000Escape.STEP * SLICE_STEPS
        var time = 0f
        var center = x
        while (from + time <= D1000Escape.HORIZON) {
            val next = if (direction == Facing.RIGHT) min(center + D1000Shockwave.SPEED * slice, wall)
            else max(center - D1000Shockwave.SPEED * slice, wall)
            if (floorHit) escape.add(min(center, next) - half, max(center, next) + half, from + time, from + time + slice, true)
            time += slice
            center = next
            if (center == wall) break
        }
        addWallWave(direction, arena.getY(), from + time)
    }

    private fun addWallWave(direction: Facing, bottom: Float, from: Float) {
        val left = if (direction == Facing.RIGHT) arena.getMaxX() - D1000Shockwave.THICKNESS else arena.getX()
        val enter = max(0f, (bandBottom - bottom - D1000Shockwave.LENGTH) / D1000Shockwave.SPEED)
        val leave = (bandTop - bottom) / D1000Shockwave.SPEED
        if (leave < 0f) return
        escape.add(left, left + D1000Shockwave.THICKNESS, from + enter, from + leave, fullHeight = true)
    }

    fun touches(boxes: Array<GameRectangle>): Boolean {
        val half = D1000XMoves.HALF_WIDTH
        return boxes.any { box ->
            box.getY() < bandTop && box.getMaxY() > bandBottom && playerX + half > box.getX() && playerX - half < box.getMaxX()
        }
    }

    fun addFootprint(boxes: Array<GameRectangle>, shift: Float, from: Float, to: Float, skipWhenInside: Boolean) {
        var left = Float.POSITIVE_INFINITY
        var right = Float.NEGATIVE_INFINITY
        boxes.forEach { box ->
            if (box.getY() >= bandTop || box.getMaxY() <= bandBottom) return@forEach
            left = min(left, box.getX() + shift)
            right = max(right, box.getMaxX() + shift)
        }
        if (left > right) return
        val half = D1000XMoves.HALF_WIDTH
        if (skipWhenInside && playerX + half > left && playerX - half < right) return
        escape.add(left, right, from, to)
    }
}
