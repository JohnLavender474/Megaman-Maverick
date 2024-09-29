package com.megaman.maverick.game.utils

import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.props
import com.megaman.maverick.game.entities.EntityType

object StringTo_EntityType_EntityName_Props_Parser {

    data class ParsedResult(
        val entityType: EntityType,
        val entityName: String,
        val properties: Properties
    )

    fun parse(input: String): ParsedResult {
        val lastIndexForEntityType = input.indexOf(",")
        if (lastIndexForEntityType == -1) throw IllegalArgumentException("Invalid input format: $input")
        val entityType = EntityType.valueOf(input.substring(0, lastIndexForEntityType))

        var lastIndexForEntityName = input.indexOf(",", lastIndexForEntityType + 1)
        if (lastIndexForEntityName == -1) lastIndexForEntityName = input.length
        val entityName = input.substring(lastIndexForEntityType + 1, lastIndexForEntityName)

        var properties = props()
        if (input.length >= lastIndexForEntityName + 1) {
            val propertiesString = input.substring(lastIndexForEntityName + 1)
            properties = StringToPropertiesParser.parse(propertiesString)
        }

        return ParsedResult(entityType, entityName, properties)
    }
}
