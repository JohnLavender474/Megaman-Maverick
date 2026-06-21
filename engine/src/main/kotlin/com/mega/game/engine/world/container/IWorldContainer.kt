package com.mega.game.engine.world.container

import com.mega.game.engine.common.interfaces.IClearable
import com.mega.game.engine.common.interfaces.ICopyable
import com.mega.game.engine.world.body.IBody
import com.mega.game.engine.world.body.IFixture

interface IWorldContainer : IClearable, ICopyable<IWorldContainer> {

    fun addBody(body: IBody): Boolean

    fun addFixture(fixture: IFixture): Boolean

    fun forEachBody(x: Int, y: Int, action: (body: IBody, container: IWorldContainer) -> Unit)

    fun forEachBody(
        minX: Int,
        minY: Int,
        maxX: Int,
        maxY: Int,
        action: (body: IBody, container: IWorldContainer) -> Unit
    )

    fun forEachFixture(x: Int, y: Int, action: (fixture: IFixture, container: IWorldContainer) -> Unit)

    fun forEachFixture(
        minX: Int,
        minY: Int,
        maxX: Int,
        maxY: Int,
        action: (fixture: IFixture, container: IWorldContainer) -> Unit
    )

    fun getBodies(
        x: Int,
        y: Int,
        out: MutableCollection<IBody>,
        filter: ((body: IBody, container: IWorldContainer) -> Boolean)? = null
    ) = forEachBody(x, y) { body, container ->
        if (filter == null || filter(body, container)) out.add(body)
    }

    fun getBodies(
        minX: Int,
        minY: Int,
        maxX: Int,
        maxY: Int,
        out: MutableCollection<IBody>,
        filter: ((body: IBody, container: IWorldContainer) -> Boolean)? = null
    ) = forEachBody(minX, minY, maxX, maxY) { body, container ->
        if (filter == null || filter(body, container)) out.add(body)
    }

    fun getFixtures(
        x: Int,
        y: Int,
        out: MutableCollection<IFixture>,
        filter: ((fixture: IFixture, container: IWorldContainer) -> Boolean)? = null
    ) = forEachFixture(x, y) { fixture, container ->
        if (filter == null || filter(fixture, container)) out.add(fixture)
    }

    fun getFixtures(
        minX: Int,
        minY: Int,
        maxX: Int,
        maxY: Int,
        out: MutableCollection<IFixture>,
        filter: ((fixture: IFixture, container: IWorldContainer) -> Boolean)? = null
    ) = forEachFixture(minX, minY, maxX, maxY) { fixture, container ->
        if (filter == null || filter(fixture, container)) out.add(fixture)
    }
}
