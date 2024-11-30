package com.megaman.maverick.game.world.body

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys

object BodyComponentCreator {

    fun create(
        entity: IBodyEntity,
        body: Body,
        bodyFixtureDefs: Array<BodyFixtureDef> = Array(),
        debugShapes: Array<() -> IDrawableShape?>? = null
    ): BodyComponent {
        bodyFixtureDefs.forEach { t ->
            val shape = t.shape ?: GameRectangle(body)
            val fixture = Fixture(
                body = body,
                type = t.type,
                rawShape = shape,
                active = t.active,
                attachedToBody = t.attached,
                bodyAttachmentPosition = t.attachment,
                offsetFromBodyAttachment = t.offset.cpy(),
                properties = t.props
            )
            fixture.addFixtureLabels(t.labels)
            body.addFixture(fixture)
            if (debugShapes != null) t.drawingColor.let { color ->
                debugShapes.add { fixture }
            }
        }
        body.fixtures.forEach { (_, fixture) -> fixture.setEntity(entity) }
        body.setEntity(entity)
        body.preProcess.put(ConstKeys.DELTA) { body.putProperty(ConstKeys.PRIOR, body.getPosition()) }
        body.onReset = { body.resetBodySenses() }
        return BodyComponent(body)
    }
}

data class BodyFixtureDef(
    val type: FixtureType,
    val shape: IGameShape2D? = null,
    val offset: Vector2 = Vector2(),
    val attachment: Position = Position.CENTER,
    val active: Boolean = true,
    val attached: Boolean = true,
    val props: Properties = props(),
    val labels: ObjectSet<FixtureLabel> = ObjectSet(),
    val drawingColor: Color? = null
) {

    companion object {

        fun of(vararg types: FixtureType): Array<BodyFixtureDef> {
            val array = Array<BodyFixtureDef>()
            types.forEach { type -> array.add(BodyFixtureDef(type)) }
            return array
        }
    }
}
