package com.mega.game.engine.world.body

import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.OrderedSet
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.putIfAbsentAndGet
import com.mega.game.engine.common.objects.Properties

class BodyBuilder(
    private var type: BodyType? = null,
    private var x: Float = 0f,
    private var y: Float = 0f,
    private var width: Float = 0f,
    private var height: Float = 0f,
    private var physics: PhysicsData = PhysicsData(),
    private var fixtures: OrderedMap<Any, OrderedSet<IFixture>> = OrderedMap(),
    private var preProcess: OrderedMap<Any, () -> Unit> = OrderedMap(),
    private var postProcess: OrderedMap<Any, () -> Unit> = OrderedMap(),
    private var onReset: OrderedMap<Any, () -> Unit> = OrderedMap(),
    private var direction: Direction = Direction.UP,
    private var properties: Properties = Properties()
) {

    fun type(type: BodyType): BodyBuilder {
        this.type = type
        return this
    }

    fun position(x: Float, y: Float): BodyBuilder {
        this.x = x
        this.y = y
        return this
    }

    fun size(width: Float, height: Float): BodyBuilder {
        this.width = width
        this.height = height
        return this
    }

    fun physics(physics: PhysicsData): BodyBuilder {
        this.physics = physics
        return this
    }

    fun fixture(fixture: IFixture): BodyBuilder {
        fixtures.putIfAbsentAndGet(fixture.getType(), OrderedSet()).add(fixture)
        return this
    }

    fun preProcess(key: Any, action: () -> Unit): BodyBuilder {
        preProcess.put(key, action)
        return this
    }

    fun postProcess(key: Any, action: () -> Unit): BodyBuilder {
        postProcess.put(key, action)
        return this
    }

    fun onReset(key: Any, onReset: () -> Unit): BodyBuilder {
        this.onReset.put(key, onReset)
        return this
    }

    fun direction(direction: Direction): BodyBuilder {
        this.direction = direction
        return this
    }

    fun properties(properties: Properties): BodyBuilder {
        this.properties = properties
        return this
    }

    fun build(): Body {
        if (type == null) throw IllegalArgumentException("Body type must be specified.")
        return Body(
            type = type!!,
            x = x,
            y = y,
            width = width,
            height = height,
            physics = physics,
            fixtures = fixtures,
            preProcess = preProcess,
            postProcess = postProcess,
            onReset = onReset,
            direction = direction,
            properties = properties
        )
    }
}
