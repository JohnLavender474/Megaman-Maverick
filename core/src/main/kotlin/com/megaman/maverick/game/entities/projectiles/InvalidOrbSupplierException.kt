package com.megaman.maverick.game.entities.projectiles

import com.megaman.maverick.game.ConstKeys

class InvalidOrbSupplierException(value: Any, cause: Throwable? = null) : IllegalArgumentException(
    "The ${ConstKeys.SUPPLIER} spawn prop of ${BigAssMaverickRobotOrb.TAG} is a ${value::class.qualifiedName}, " +
        "not a trajectory function. Pass a (from: Vector2, out: Vector2) -> Vector2 that sets out to the " +
        "trajectory from the point from and returns out, or leave the prop out to keep the spawn trajectory.",
    cause
)
