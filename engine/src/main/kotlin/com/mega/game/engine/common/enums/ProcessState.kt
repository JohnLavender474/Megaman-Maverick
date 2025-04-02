package com.mega.game.engine.common.enums

import com.mega.game.engine.common.extensions.equalsAny

enum class ProcessState {
    BEGIN, CONTINUE, END;

    fun isRunning() = this.equalsAny(BEGIN, CONTINUE)
}
