package com.megaman.maverick.game.entities.bosses.elecdevil

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.common.extensions.getTextureRegion
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponentBuilder
import com.maverick.game.engine.drawables.sprites.setSize
import com.megaman.maverick.game.assets.TextureAsset
import kotlin.math.max
import kotlin.math.min

internal class LiveWireArcSprites(
    private val keyPrefix: String,
    private val priority: Int,
    private val fillEnds: (from: Vector2, to: Vector2) -> Boolean
) {

    companion object {
        private const val ARC_REGION = "ElecDevilLiveWireArc"
        private const val ARC_CAP_REGION = "ElecDevilLiveWireCap"
        private const val ARC_FRAMES = 4
        private const val ARC_FRAME_DUR = 0.06f
        private const val ART_SCALE = 2
        private const val ARC_TILE_ART_LENGTH = 16
        private const val ARC_TILE_ART_THICKNESS = 8
        private const val ARC_CAP_ART_SIZE = 8
        private const val MAX_ARC_TILES = 20

        private val arcFrames = Array<TextureRegion>()
        private val arcCapFrames = Array<TextureRegion>()

        fun loadRegions(assets: AssetManager) {
            if (!arcFrames.isEmpty) return
            assets.getTextureRegion(TextureAsset.PROJECTILES_1.source, ARC_REGION)
                .split(ARC_TILE_ART_LENGTH, ARC_TILE_ART_THICKNESS)[0].forEach { arcFrames.add(it) }
            assets.getTextureRegion(TextureAsset.PROJECTILES_1.source, ARC_CAP_REGION)
                .split(ARC_CAP_ART_SIZE, ARC_CAP_ART_SIZE)[0].forEach { arcCapFrames.add(it) }
        }
    }

    var time = 0f
    var alpha = 1f

    private val tileRegions = Array<TextureRegion>().also { regions ->
        repeat(MAX_ARC_TILES) { regions.add(TextureRegion()) }
    }
    private val from = Vector2()
    private val to = Vector2()

    fun addTo(builder: SpritesComponentBuilder): SpritesComponentBuilder {
        for (tile in 0 until MAX_ARC_TILES) {
            builder.sprite("${keyPrefix}_$tile", GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, priority)))
            builder.preProcess { _, sprite -> drawTile(tile, sprite) }
        }
        for (end in 0 until 2) {
            builder.sprite("${keyPrefix}_cap_$end", GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, priority)))
            builder.preProcess { _, sprite -> drawCap(end, sprite) }
        }
        return builder
    }

    private fun frameIndex(offset: Int) = ((time / ARC_FRAME_DUR).toInt() + offset) % ARC_FRAMES

    private fun drawTile(tile: Int, sprite: GameSprite) {
        val drawn = fillEnds(from, to)
        val length = if (drawn) from.dst(to) else 0f
        val fullTileLength = (ARC_TILE_ART_LENGTH * ART_SCALE).toFloat()
        val tileStart = tile * fullTileLength
        sprite.hidden = !drawn || tileStart >= length
        if (sprite.hidden) return
        val artLength = max(1, (min(fullTileLength, length - tileStart) / ART_SCALE).toInt())
        val tileLength = (artLength * ART_SCALE).toFloat()
        val tileRegion = tileRegions[tile]
        tileRegion.setRegion(arcFrames[frameIndex(tile)], 0, 0, artLength, ARC_TILE_ART_THICKNESS)
        sprite.setRegion(tileRegion)
        sprite.setSize(tileLength, (ARC_TILE_ART_THICKNESS * ART_SCALE).toFloat())
        sprite.setOriginCenter()
        val direction = to.sub(from).nor()
        val along = tileStart + tileLength / 2f
        sprite.setCenter(from.x + direction.x * along, from.y + direction.y * along)
        sprite.rotation = direction.angleDeg()
        sprite.setAlpha(alpha)
    }

    private fun drawCap(end: Int, sprite: GameSprite) {
        sprite.hidden = !fillEnds(from, to)
        if (sprite.hidden) return
        sprite.setRegion(arcCapFrames[frameIndex(end * 2)])
        sprite.setSize((ARC_CAP_ART_SIZE * ART_SCALE).toFloat())
        val center = if (end == 0) from else to
        sprite.setCenter(center.x, center.y)
        sprite.setAlpha(alpha)
    }
}
