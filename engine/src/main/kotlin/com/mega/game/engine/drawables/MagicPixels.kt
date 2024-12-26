package com.mega.game.engine.drawables

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.ObjectSet
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.common.objects.IntPair

/**
 * Utility object for magic pixels.
 */
object MagicPixels {

    /**
     * This method consumes the [frame] and checks if any of its pixels has a magic color where the color's int bits is
     * the same as a value contained in the [colors] set. For each pixel that has a magic color, the position of the
     * pixel and its color is added as an entry to the [out] map. The method returns true if any of the pixels has a
     * magic color, otherwise false.
     *
     * @param frame The region to scan for magic pixels
     * @param colors The set of magic colors
     * @param out The map to contain the position-color entries of magic pixel
     * @throws IllegalArgumentException If the [colors] set is empty
     */
    fun get(
        frame: TextureRegion,
        colors: ObjectSet<Color>,
        out: OrderedMap<IntPair, Color>
    ): Boolean {
        if (colors.isEmpty) throw IllegalArgumentException("The colors set should not be empty.")

        val data = frame.texture.textureData
        if (!data.isPrepared) data.prepare()

        val pixmap = data.consumePixmap()

        assert(pixmap.width == frame.regionWidth)
        assert(pixmap.height == frame.regionHeight)

        return get(pixmap, colors, out)
    }

    /**
     * This method consumes the [pixmap and checks if any of its pixels has a magic color where the color's int bits is
     * the same as a value contained in the [colors] set. For each pixel that has a magic color, the position of the
     * pixel and its color is added as an entry to the [out] map. The method returns true if any of the pixels has a
     * magic color, otherwise false.
     *
     * @param pixmap The pixmap to scan for magic pixels
     * @param colors The set of magic colors
     * @param out The map to contain the position-color entries of magic pixel
     * @throws IllegalArgumentException If the [colors] set is empty
     */
    fun get(
        pixmap: Pixmap,
        colors: ObjectSet<Color>,
        out: OrderedMap<IntPair, Color>
    ): Boolean {
        val allColors = ObjectSet<Color>()

        for (x in 0 until pixmap.width) for (y in 0 until pixmap.height) {
            val pixel = pixmap.getPixel(x, y)
            val color = Color(pixel)

            allColors.add(color)

            if (colors.contains(color)) out.put(IntPair(x, y), color)
        }

        pixmap.dispose()

        println("All colors: $allColors")

        return !out.isEmpty
    }
}
