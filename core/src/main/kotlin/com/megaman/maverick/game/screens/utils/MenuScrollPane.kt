package com.megaman.maverick.game.screens.utils

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.enums.Direction
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.drawables.fonts.MegaFontHandle

/**
 * A vertical list of menu buttons that shows a maximum of [maxVisibleRows] rows at the same time. When the selected
 * button is out of view, the pane scrolls to show it.
 *
 * When there are rows out of view above the topmost visible row, an arrow that points up shows above the list. When
 * there are rows out of view below the bottommost visible row, an arrow that points down shows below the list.
 *
 * The pane does not keep the selected key. The menu screen keeps it, and gives it to [update] and [draw].
 *
 * @param textX the x position of the text, in tiles
 * @param topRow the y position of the topmost visible row, in tiles
 * @param maxVisibleRows the maximum number of rows that show at the same time
 * @param wrap if true, DOWN on the bottommost button goes to the topmost button, and UP on the topmost button goes to
 * the bottommost button; if false, the selection stops at the two ends of the list
 * @param rowSpacing the distance between two rows, in tiles
 * @param arrowOffsetX the x offset of the selection arrow from [textX], in tiles
 * @param indicatorOffsetX the x offset of the "more above" and "more below" arrows from [textX], in tiles
 */
class MenuScrollPane(
    assMan: AssetManager,
    private val textX: Float,
    private val topRow: Float,
    val maxVisibleRows: Int,
    var wrap: Boolean = true,
    private val rowSpacing: Float = ConstVals.TEXT_ROW_DECREMENT * ConstVals.PPM,
    private val arrowOffsetX: Float = -0.5f,
    private val indicatorOffsetX: Float = 0.25f
) {

    companion object {
        // the arrow texture points to the right; a positive rotation turns it counterclockwise
        private const val UP_ROTATION = 90f
        private const val DOWN_ROTATION = 270f
    }

    val keys: Array<String>
        get() = _keys

    private val _keys = Array<String>()
    private val fontHandles = ObjectMap<String, MegaFontHandle>()
    private val arrow = BlinkingArrow(assMan)
    private val moreAboveArrow = BlinkingArrow(assMan, rotation = UP_ROTATION)
    private val moreBelowArrow = BlinkingArrow(assMan, rotation = DOWN_ROTATION)

    // the index of the topmost visible row
    private var scrollIndex = 0

    init {
        require(maxVisibleRows > 0) { "maxVisibleRows must be greater than 0: $maxVisibleRows" }
    }

    fun addItem(key: String, text: String = key) {
        require(!fontHandles.containsKey(key)) { "Key is already in the scroll pane: $key" }
        _keys.add(key)
        fontHandles.put(key, MegaFontHandle(text = text, centerX = false, centerY = false))
    }

    /**
     * Returns the key of the button that is next to [key] in [direction]. LEFT and RIGHT return [key].
     */
    fun navigate(key: String, direction: Direction): String {
        val index = _keys.indexOf(key, false)
        if (index == -1) return key

        val size = _keys.size
        val next = when (direction) {
            Direction.UP -> if (wrap) (index - 1 + size) % size else maxOf(index - 1, 0)
            Direction.DOWN -> if (wrap) (index + 1) % size else minOf(index + 1, size - 1)
            else -> index
        }
        return _keys[next]
    }

    /**
     * Scrolls to the topmost row. Call this when the menu screen shows.
     */
    fun reset() {
        scrollIndex = 0
    }

    fun update(delta: Float, selectedKey: String?) {
        if (selectedKey != null) scrollTo(selectedKey)
        arrow.update(delta)
        moreAboveArrow.update(delta)
        moreBelowArrow.update(delta)
    }

    fun draw(drawer: Batch, selectedKey: String?) {
        val lastIndex = minOf(scrollIndex + maxVisibleRows, _keys.size)
        for (index in scrollIndex until lastIndex) {
            val key = _keys[index]
            val row = getRowY(index - scrollIndex)

            val fontHandle = fontHandles[key]
            fontHandle.positionX = textX * ConstVals.PPM
            fontHandle.positionY = row * ConstVals.PPM
            fontHandle.draw(drawer)

            if (key == selectedKey) {
                arrow.centerX = (textX + arrowOffsetX) * ConstVals.PPM
                arrow.centerY = (row - ConstVals.ARROW_CENTER_ROW_DECREMENT) * ConstVals.PPM
                arrow.draw(drawer)
            }
        }

        // the indicators take the positions of the rows directly above and below the visible rows
        if (scrollIndex > 0) {
            moreAboveArrow.centerX = (textX + indicatorOffsetX) * ConstVals.PPM
            moreAboveArrow.centerY = (getRowY(-1) - ConstVals.ARROW_CENTER_ROW_DECREMENT) * ConstVals.PPM
            moreAboveArrow.draw(drawer)
        }
        if (lastIndex < _keys.size) {
            moreBelowArrow.centerX = (textX + indicatorOffsetX) * ConstVals.PPM
            moreBelowArrow.centerY = (getRowY(maxVisibleRows) - ConstVals.ARROW_CENTER_ROW_DECREMENT) * ConstVals.PPM
            moreBelowArrow.draw(drawer)
        }
    }

    // returns the y position (in tiles) of the row at the given index in the visible part of the list
    private fun getRowY(visibleIndex: Int) = topRow - visibleIndex * rowSpacing

    private fun scrollTo(key: String) {
        val index = _keys.indexOf(key, false)
        if (index == -1) return

        if (index < scrollIndex) scrollIndex = index
        else if (index >= scrollIndex + maxVisibleRows) scrollIndex = index - maxVisibleRows + 1

        scrollIndex = scrollIndex.coerceIn(0, maxOf(_keys.size - maxVisibleRows, 0))
    }
}
