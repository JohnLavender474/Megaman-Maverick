package com.mega.game.engine.world.body

import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.interfaces.*
import com.mega.game.engine.common.shapes.GameRectangle

interface IBody : IRectangle, IProcessable, IDirectional, IPropertizable, Resettable {

    var type: BodyType
    var physics: PhysicsData

    fun addFixture(fixture: IFixture): IBody

    fun hasFixture(fixture: IFixture): Boolean

    fun removeFixture(fixture: IFixture): Boolean

    fun getFixtures(out: Array<IFixture>, vararg params: Any): Array<IFixture>

    fun forEachFixture(action: (IFixture) -> Unit): IBody

    fun getBounds(out: GameRectangle): GameRectangle
}
