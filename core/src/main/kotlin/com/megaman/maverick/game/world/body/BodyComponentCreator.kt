package com.megaman.maverick.game.world.body

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.objects.GamePair
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
import com.megaman.maverick.game.entities.contracts.MegaGameEntity

object BodyComponentCreator {

    fun create(
        entity: IBodyEntity,
        body: Body,
        bodyFixtureDefs: Array<BodyFixtureDef> = Array(),
        debugShapes: Array<() -> IDrawableShape?>? = null
    ): BodyComponent {
        val component = BodyComponent(body)
        return amend(entity, component, bodyFixtureDefs, debugShapes)
    }

    fun amend(
        entity: IBodyEntity,
        component: BodyComponent,
        bodyFixtureDefs: Array<BodyFixtureDef> = Array(),
        debugShapes: Array<() -> IDrawableShape?>? = null
    ): BodyComponent {
        val body = component.body

        handleBodyFixtureDefs(body, bodyFixtureDefs, debugShapes)

        body.setEntity(entity)
        body.forEachFixture { fixture -> fixture.setEntity(entity) }
        body.preProcess.put(ConstKeys.PRIOR) { body.putProperty(ConstKeys.PRIOR, body.getPosition()) }
        body.onReset.put(ConstKeys.BODY_SENSES) { body.resetBodySenses() }

        if (entity is MegaGameEntity)
            entity.runnablesOnDestroy.put(ConstKeys.CLEAR_FEET_BLOCKS) { body.clearFeetBlocks() }

        return component
    }

    private fun handleBodyFixtureDefs(
        body: Body,
        bodyFixtureDefs: Array<BodyFixtureDef>,
        debugShapes: Array<() -> IDrawableShape?>? = null
    ) = bodyFixtureDefs.forEach { t ->
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

        if (debugShapes != null) {
            t.drawingColor?.let { color -> fixture.drawingColor = color }
            debugShapes.add { fixture }
        }
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

        fun of(vararg entries: GamePair<FixtureType, IGameShape2D>): Array<BodyFixtureDef> {
            val array = Array<BodyFixtureDef>()
            entries.forEach { (type, shape) -> array.add(create(type, shape)) }
            return array
        }

        fun create(type: FixtureType, shape: IGameShape2D) = BodyFixtureDef(type = type, shape = shape)
    }
}
