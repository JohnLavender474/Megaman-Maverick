package com.megaman.maverick.game.screens.levels.camera

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.maverick.game.engine.camera.CameraView
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.entities.facing
import com.maverick.game.engine.entities.isBehaviorActive
import com.maverick.game.engine.players.IPlayer
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.getCenter
import com.megaman.maverick.game.world.body.isSensing

class PartnerSpot(private val spots: WarpSpotTest, private val view: CameraView, private val viewCenter: Vector3) {

    private val tryBounds = GameRectangle()

    fun find(player: IPlayer, partner: IPlayer, best: Vector2, fallback: Vector2): Boolean {
        val x = partner.body.getCenter().x
        val y = partner.body.getY() + player.body.getHeight() / 2f
        val side = if (partner.facing == Facing.LEFT) -1f else 1f
        fallback.set(x, y)
        best.set(x + side * ConstVals.PPM, y)

        return stands(partner) && (
            tryAt(player, x + side * ConstVals.PPM, y, best, true) ||
                tryAt(player, x - side * ConstVals.PPM, y, best, true) ||
                tryAt(player, x, y, best, false))
    }

    fun landing(player: IPlayer, ready: Boolean, pastCap: Boolean, best: Vector2, fallback: Vector2) = when {
        ready -> best
        pastCap && spots.isFree(player, fallback.x, fallback.y) -> fallback
        else -> null
    }

    fun isInView(player: IPlayer): Boolean {
        val center = player.body.getCenter()
        return center.x > viewCenter.x - view.viewWidth / 2f && center.x < viewCenter.x + view.viewWidth / 2f &&
            center.y > viewCenter.y - view.viewHeight / 2f && center.y < viewCenter.y + view.viewHeight / 2f
    }

    private fun tryAt(player: IPlayer, x: Float, y: Float, out: Vector2, needsGround: Boolean): Boolean {
        val size = player.body
        tryBounds.setSize(size.getWidth(), size.getHeight()).setCenter(x, y)
        if (!isFullyInView(tryBounds) || !spots.isSafe(player, x, y)) return false
        if (needsGround && !spots.hasGround(player, x, y)) return false
        out.set(x, y)
        return true
    }

    // TODO: a partner on a ladder stands, but its side spots have no ground, so the player lands on the partner's spot
    //  in the ladder without a climb and can fall and warp again (item 144)
    private fun stands(player: IPlayer): Boolean {
        val body = player.body
        return body.isSensing(BodySense.FEET_ON_GROUND) || body.isSensing(BodySense.IN_WATER) ||
            player.isBehaviorActive(BehaviorType.CLIMBING)
    }

    private fun isFullyInView(area: GameRectangle) =
        area.getX() >= viewCenter.x - view.viewWidth / 2f && area.getMaxX() <= viewCenter.x + view.viewWidth / 2f &&
            area.getY() >= viewCenter.y - view.viewHeight / 2f && area.getMaxY() <= viewCenter.y + view.viewHeight / 2f
}
