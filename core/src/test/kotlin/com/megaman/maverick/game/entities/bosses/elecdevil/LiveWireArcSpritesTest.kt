package com.megaman.maverick.game.entities.bosses.elecdevil

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.extensions.objectSetOf
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.entities.EntityExistenceState
import com.maverick.game.engine.world.body.BodyComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.bosses.BossRig
import com.megaman.maverick.game.entities.bosses.ElecDevilBodyPiece
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.floats.shouldBeLessThanOrEqual
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlin.math.abs

private const val ARC = "arc"
private const val ART_PIXEL = 2f
private const val TOLERANCE = 0.01f

private class TileSpan(val start: Float, val end: Float, val offLine: Float)

class LiveWireArcSpritesTest : DescribeSpec({

    lateinit var rig: BossRig

    afterEach { rig.dispose() }

    fun piece(): ElecDevilBodyPiece {
        val piece = MegaEntityFactory.fetch(ElecDevilBodyPiece::class)!!
        piece.spawn(
            props(
                ConstKeys.END pairTo {},
                ConstKeys.OWNER pairTo rig.player,
                ConstKeys.SPEED pairTo 0f,
                ConstKeys.START pairTo Vector2(),
                ConstKeys.TARGET pairTo Vector2(10f * T, 0f),
                "${ConstKeys.LIGHT}_${ConstKeys.SOURCE}_${ConstKeys.KEYS}" pairTo objectSetOf(1)
            )
        ) shouldBe true
        rig.processSpawns()
        return piece
    }

    fun linkedPair(from: Vector2, to: Vector2): Pair<ElecDevilBodyPiece, ElecDevilBodyPiece> {
        val lead = piece()
        val trail = piece()
        trail.linkedPiece = lead
        trail.getComponent(BodyComponent::class)!!.body.setCenter(from)
        lead.getComponent(BodyComponent::class)!!.body.setCenter(to)
        return trail to lead
    }

    fun sprites(piece: ElecDevilBodyPiece): SpritesComponent {
        val sprites = piece.getComponent(SpritesComponent::class)!!
        sprites.preProcess(0f)
        return sprites
    }

    fun arcSprites(sprites: SpritesComponent) =
        sprites.sprites.entries().filter { it.key.toString().startsWith("${ARC}_") }.map { it.value }

    fun center(sprite: GameSprite) = Vector2(sprite.x + sprite.width / 2f, sprite.y + sprite.height / 2f)

    fun tiles(sprites: SpritesComponent, from: Vector2, to: Vector2): List<TileSpan> {
        val direction = Vector2(to).sub(from).nor()
        val out = mutableListOf<TileSpan>()
        var tile = 0
        while (sprites.sprites.containsKey("${ARC}_$tile")) {
            val sprite = sprites.sprites["${ARC}_$tile"]
            if (!sprite.hidden) {
                val offset = center(sprite).sub(from)
                val along = offset.dot(direction)
                out.add(TileSpan(along - sprite.width / 2f, along + sprite.width / 2f, abs(direction.crs(offset))))
            }
            tile++
        }
        return out
    }

    beforeEach { rig = BossRig() }

    describe("the Live Wire arc") {
        listOf(
            Vector2(0f, 0f) to Vector2(10f, 0f),
            Vector2(0f, 0f) to Vector2(64f, 0f),
            Vector2(100f, 50f) to Vector2(30f, 120.3f),
            Vector2(32f, 40f) to Vector2(330f, 61f)
        ).forEach { (from, to) ->
            it("covers the line from $from to $to with no gap, no overlap past the far center and a cap at each end") {
                val (trail, _) = linkedPair(from, to)
                val sprites = sprites(trail)
                val length = from.dst(to)
                val spans = tiles(sprites, from, to)
                withClue("tiles ${spans.map { it.start to it.end }} on a line of $length") {
                    spans.size shouldBeGreaterThan 0
                    spans.first().start shouldBe (0f plusOrMinus TOLERANCE)
                    spans.zipWithNext().forEach { (a, b) -> b.start shouldBe (a.end plusOrMinus TOLERANCE) }
                    spans.last().end shouldBeLessThanOrEqual length + TOLERANCE
                    spans.last().end shouldBeGreaterThan length - ART_PIXEL
                    spans.forEach { it.offLine shouldBeLessThan TOLERANCE }
                }
                val start = sprites.sprites["${ARC}_cap_0"]
                val end = sprites.sprites["${ARC}_cap_1"]
                start.hidden shouldBe false
                end.hidden shouldBe false
                center(start).dst(from) shouldBeLessThan TOLERANCE
                center(end).dst(to) shouldBeLessThan TOLERANCE
            }
        }

        it("hides the tiles and the caps when the link is gone") {
            val (trail, lead) = linkedPair(Vector2(0f, 0f), Vector2(100f, 0f))
            arcSprites(sprites(trail)).count { !it.hidden } shouldBeGreaterThan 0
            lead.existenceState = EntityExistenceState.UNSPAWNED
            arcSprites(sprites(trail)).count { !it.hidden } shouldBe 0
            lead.existenceState = EntityExistenceState.LIVE
            arcSprites(sprites(trail)).count { !it.hidden } shouldBeGreaterThan 0
            trail.linkedPiece = null
            arcSprites(sprites(trail)).count { !it.hidden } shouldBe 0
            arcSprites(sprites(lead)).count { !it.hidden } shouldBe 0
        }
    }
})
