package com.mega.game.engine.drawables.sorting

data class DrawingPriority(
    var section: DrawingSection = DrawingSection.PLAYGROUND,
    var value: Int = 0
) : Comparable<DrawingPriority> {

    override fun compareTo(other: DrawingPriority): Int {
        val sectionCompare = section.compareTo(other.section)
        return if (sectionCompare == 0) value.compareTo(other.value) else sectionCompare
    }

    override fun equals(other: Any?) =
        other is DrawingPriority && section == other.section && value == other.value

    override fun hashCode() = 31 * section.hashCode() + value
}
