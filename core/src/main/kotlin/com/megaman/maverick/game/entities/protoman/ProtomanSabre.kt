package com.megaman.maverick.game.entities.protoman

import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.objectMapOf
import com.maverick.game.engine.common.objects.pairTo
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.players.SabreParams
import com.megaman.maverick.game.players.SabreState
import com.megaman.maverick.game.players.bodyOverGround

internal fun protomanSabreParams(proto: Protoman) = SabreParams(
    moves = objectMapOf(
        SabreState.FLOOR pairTo gdxArrayOf(
            ProtomanSabreMoves.SLASH_1,
            ProtomanSabreMoves.SLASH_2,
            ProtomanSabreMoves.SLASH_3
        ),
        SabreState.CROUCH pairTo gdxArrayOf(ProtomanSabreMoves.CROUCH),
        SabreState.AIR pairTo gdxArrayOf(ProtomanSabreMoves.AIR)
    ),
    atlas = TextureAsset.PROTOMAN,
    spriteSize = ProtomanValues.SPRITE_SIZE,
    artPxPerBodyFrame = 48,
    sound = SoundAsset.PROTO_SABRE_SOUND,
    canStart = { !proto.buster.shooting },
    chooseState = {
        when {
            !proto.bodyOverGround -> SabreState.AIR
            proto.sabre.state == SabreState.CROUCH -> SabreState.CROUCH
            else -> SabreState.FLOOR
        }
    },
    spriteOffsetY = { proto.spriteOffsetY() },
    endOnStateChange = true,
    landingMoves = objectMapOf(ProtomanSabreMoves.AIR pairTo ProtomanSabreMoves.AIR_LAND)
)
