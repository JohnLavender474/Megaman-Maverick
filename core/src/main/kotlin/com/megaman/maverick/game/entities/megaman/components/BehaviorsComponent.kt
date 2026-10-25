package com.megaman.maverick.game.entities.megaman.components

import com.maverick.game.engine.behaviors.BehaviorsComponent
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.behaviors.*

// TODO: the behavior factories check weapons inside their bodies (NEEDLE_SPIN, RUSH_JET, RODENT_CLAWS); these
//  checks belong to the loadout and move with item 024
internal fun Megaman.defineBehaviorsComponent(): BehaviorsComponent {
    val behaviorsComponent = BehaviorsComponent()

    behaviorsComponent.addBehavior(BehaviorType.WALL_SLIDING, wallSlideBehavior())
    behaviorsComponent.addBehavior(BehaviorType.SWIMMING, swimBehavior())
    behaviorsComponent.addBehavior(BehaviorType.JUMPING, jumpBehavior())
    behaviorsComponent.addBehavior(BehaviorType.AIR_DASHING, airDashBehavior())
    behaviorsComponent.addBehavior(BehaviorType.GROUND_SLIDING, groundSlideBehavior())
    behaviorsComponent.addBehavior(BehaviorType.CROUCHING, crouchBehavior())
    behaviorsComponent.addBehavior(BehaviorType.CLIMBING, climbBehavior())
    behaviorsComponent.addBehavior(BehaviorType.JETPACKING, jetpackBehavior())

    return behaviorsComponent
}
