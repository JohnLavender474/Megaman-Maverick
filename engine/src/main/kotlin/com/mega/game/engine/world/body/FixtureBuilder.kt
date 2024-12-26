package com.mega.game.engine.world.body

import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.IGameShape2D

class FixtureBuilder(
    private var body: Body,
    private var type: Any,
    private var rawShape: IGameShape2D = GameRectangle(),
    private var active: Boolean = true,
    private var attachedToBody: Boolean = true,
    private var bodyAttachmentPosition: Position = Position.CENTER,
    private var offsetFromBodyAttachment: Vector2 = Vector2(),
    private var properties: Properties = Properties()
) {

    fun type(type: Any): FixtureBuilder {
        this.type = type
        return this
    }

    fun shape(rawShape: IGameShape2D): FixtureBuilder {
        this.rawShape = rawShape
        return this
    }

    fun active(active: Boolean): FixtureBuilder {
        this.active = active
        return this
    }

    fun attachedToBody(attached: Boolean): FixtureBuilder {
        this.attachedToBody = attached
        return this
    }

    fun attachmentPosition(position: Position): FixtureBuilder {
        this.bodyAttachmentPosition = position
        return this
    }

    fun offsetFromBodyAttachment(offset: Vector2): FixtureBuilder {
        this.offsetFromBodyAttachment = offset
        return this
    }

    fun properties(properties: Properties): FixtureBuilder {
        this.properties = properties
        return this
    }

    fun build() =
        Fixture(
            body = body,
            type = type,
            rawShape = rawShape,
            active = active,
            attachedToBody = attachedToBody,
            bodyAttachmentPosition = bodyAttachmentPosition,
            offsetFromBodyAttachment = offsetFromBodyAttachment,
            properties = properties
        )
}
