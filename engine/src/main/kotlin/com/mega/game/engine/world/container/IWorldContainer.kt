package com.mega.game.engine.world.container

import com.mega.game.engine.common.interfaces.IClearable
import com.mega.game.engine.common.interfaces.ICopyable
import com.mega.game.engine.world.body.IBody
import com.mega.game.engine.world.body.IFixture

interface IWorldContainer : IClearable, ICopyable<IWorldContainer> {

    fun addBody(body: IBody): Boolean

    fun addFixture(fixture: IFixture): Boolean

    fun getBodies(x: Int, y: Int, out: MutableCollection<IBody>)

    fun getBodies(minX: Int, minY: Int, maxX: Int, maxY: Int, out: MutableCollection<IBody>)

    fun getFixtures(x: Int, y: Int, out: MutableCollection<IFixture>)

    fun getFixtures(minX: Int, minY: Int, maxX: Int, maxY: Int, out: MutableCollection<IFixture>)

    fun getObjects(x: Int, y: Int, out: MutableCollection<Any>)

    fun getObjects(minX: Int, minY: Int, maxX: Int, maxY: Int, out: MutableCollection<Any>)
}
