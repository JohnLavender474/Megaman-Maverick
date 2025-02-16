package com.mega.game.engine.common.objects.table

import com.badlogic.gdx.utils.Array

class TableBuilder<T> {

    private val rows = Array<Array<T>>()

    fun row(elements: Iterable<T>): TableBuilder<T> {
        val row = Array<T>()
        elements.forEach { row.add(it) }
        rows.add(row)
        return this
    }

    fun build() = Table(rows)

    override fun toString() = StringBuilder()
        .append("TableBuilder{\n")
        .also { builder -> rows.forEach { row -> builder.append("\t$row\n") } }
        .append("}")
        .toString()
}
