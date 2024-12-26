package com.mega.game.engine.common.interfaces

import com.mega.game.engine.common.enums.Facing


interface IFaceable {


    var facing: Facing


    fun isFacing(facing: Facing) = this.facing == facing


    fun swapFacing() {
        facing = if (facing == Facing.LEFT) Facing.RIGHT else Facing.LEFT
    }
}
